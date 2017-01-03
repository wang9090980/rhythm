/*
 * Copyright (c) 2010-2017, b3log.org & hacpai.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.b3log.rhythm.processor;

import java.net.URL;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.Transaction;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.DoNothingRenderer;
import org.b3log.latke.thread.ThreadService;
import org.b3log.latke.thread.ThreadServiceFactory;
import org.b3log.latke.urlfetch.HTTPHeader;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.HTTPResponse;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.latke.util.CollectionUtils;
import org.b3log.latke.util.Strings;
import org.b3log.rhythm.model.Sym;
import org.b3log.rhythm.repository.SymRepository;
import org.b3log.rhythm.util.Rhythms;
import org.json.JSONObject;

/**
 * Checks accessibility of Syms.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.2, Oct 29, 2016
 * @since 1.2.0
 */
@RequestProcessor
public class SymAccessibilityChecker {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(SymAccessibilityChecker.class.getName());

    /**
     * Sym repository.
     */
    @Inject
    private SymRepository symRepository;

    /**
     * Check timeout.
     */
    private static final long CHECK_TIMEOUT = 10000;

    /**
     * URL fetch service.
     */
    private URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    /**
     * Thread service.
     */
    private ThreadService threadService = ThreadServiceFactory.getThreadService();

    /**
     * Checks.
     *
     * @param context the specified context
     * @throws Exception exception
     */
    @RequestProcessing(value = "/syms/accessibility", method = HTTPRequestMethod.GET)
    public void checkAccessibility(final HTTPRequestContext context) throws Exception {
        final DoNothingRenderer renderer = new DoNothingRenderer();
        context.setRenderer(renderer);

        final HttpServletRequest request = context.getRequest();
        final String key = request.getParameter("key");
        if (Strings.isEmptyOrNull(key) || !key.equals(Rhythms.CFG.getString("key"))) {
            return;
        }

        try {
            final List<JSONObject> syms
                    = CollectionUtils.jsonArrayToList(symRepository.get(new Query()).optJSONArray(Keys.RESULTS));
            for (final JSONObject sym : syms) {
                threadService.submit(new CheckTask(sym), CHECK_TIMEOUT);
            }
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Check accessibility syms failed", e);
        }
    }

    /**
     * Sym accessibility check task.
     *
     * @author <a href="http://88250.b3log.org">Liang Ding</a>
     * @version 1.0.0.1, Oct 29, 2016
     */
    private class CheckTask implements Runnable {

        /**
         * Sym to check.
         */
        private final JSONObject sym;

        /**
         * Constructs a check task with the specified sym.
         *
         * @param sym the specified sym
         */
        public CheckTask(final JSONObject sym) {
            this.sym = sym;
        }

        @Override
        public void run() {
            final String symURL = sym.optString(Sym.SYM_URL);

            LOGGER.debug("Checks sym [url=" + symURL + "] accessibility");
            final long start = System.currentTimeMillis();

            int responseCode = 0;

            try {
                final HTTPRequest request = new HTTPRequest();
                request.addHeader(new HTTPHeader("User-Agent", "B3log Rhythm/" + Rhythms.RHYTHM_VERSION));
                request.setURL(new URL(symURL));

                final HTTPResponse response = urlFetchService.fetch(request);
                responseCode = response.getResponseCode();
                if (HttpServletResponse.SC_OK == responseCode) {
                    final String html = new String(response.getContent(), "UTF-8");
                    final String favicon
                            = StringUtils.substringBetween(html, "<link rel=\"icon\" type=\"image/png\" href=\"", "\"");
                    sym.put(Sym.SYM_ICON, StringUtils.trim(favicon));
                    final String desc = StringUtils.substringBetween(html, "<meta name=\"description\" content=\"", "\"");
                    sym.put(Sym.SYM_DESC, StringUtils.trim(desc));
                }

                LOGGER.log(Level.INFO, "Accesses sym [url=" + symURL + "] response [code={0}]", responseCode);
            } catch (final Exception e) {
                LOGGER.warn("Sym [url=" + symURL + "] accessibility check failed [msg=" + e.getMessage() + "]");
            } finally {
                final long elapsed = System.currentTimeMillis() - start;
                LOGGER.log(Level.DEBUG, "Accesses sym [url=" + symURL + "] response [code=" + responseCode + "], "
                        + "elapsed [" + elapsed + ']');

                sym.put(Sym.SYM_ACCESSIBILITY_CHECK_CNT, sym.optInt(Sym.SYM_ACCESSIBILITY_CHECK_CNT) + 1);
                if (HttpServletResponse.SC_OK != responseCode) {
                    sym.put(Sym.SYM_ACCESSIBILITY_NOT_200_CNT, sym.optInt(Sym.SYM_ACCESSIBILITY_NOT_200_CNT) + 1);
                }

                final Transaction transaction = symRepository.beginTransaction();
                try {
                    symRepository.update(sym.optString(Keys.OBJECT_ID), sym);

                    transaction.commit();
                } catch (final RepositoryException e) {
                    if (null != transaction && transaction.isActive()) {
                        transaction.rollback();
                    }

                    LOGGER.log(Level.ERROR, "Updates sym failed", e);
                }
            }
        }
    }
}
