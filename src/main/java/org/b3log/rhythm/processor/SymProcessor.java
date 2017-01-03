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

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.JSONRenderer;
import org.b3log.latke.util.Requests;
import org.b3log.rhythm.model.Sym;
import org.b3log.rhythm.service.SymService;
import org.json.JSONObject;

/**
 * Sym processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.3, Nov 29, 2016
 * @since 1.2.0
 */
@RequestProcessor
public class SymProcessor {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(SymProcessor.class.getName());

    /**
     * Sym service.
     */
    @Inject
    private SymService symService;

    /**
     * Gets syms.
     *
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean,
     *     "syms": []
     * }
     * </pre>
     * </p>
     *
     * @param context the specified context
     */
    @RequestProcessing(value = "/syms", method = HTTPRequestMethod.GET)
    public void getSyms(final HTTPRequestContext context) {
        final JSONObject ret = new JSONObject();
        context.renderJSON(ret);

        try {
            final List<JSONObject> syms = symService.getSyms();
            ret.put(Sym.SYMS, syms);

            ret.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Can not add sym", e);

            ret.put(Keys.STATUS_CODE, false);
        }
    }

    /**
     * Adds a sym.
     *
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": boolean
     * }
     * </pre>
     * </p>
     *
     * @param context the specified context, including a request json object, for example,      <pre>
     * {
     *     "symURL": "",
     *     "symTitle": ""
     * }
     * </pre>
     */
    @RequestProcessing(value = "/sym", method = HTTPRequestMethod.POST)
    public void addSym(final HTTPRequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put(Keys.STATUS_CODE, false);

        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);
        renderer.setJSONObject(jsonObject);

        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

            final String url = requestJSONObject.optString(Sym.SYM_URL);
            if (StringUtils.isBlank(url)) {
                return;
            }

            String host = StringUtils.substringAfter(url, "://");
            host = StringUtils.substringBefore(host, ":");
            if (isIPv4(host)) {
                return;
            }

            if (StringUtils.contains(host, "localhost")) {
                return;
            }

            final JSONObject sym = new JSONObject();
            sym.put(Sym.SYM_URL, url);
            sym.put(Sym.SYM_TITLE, requestJSONObject.optString(Sym.SYM_TITLE));

            symService.addSym(sym);

            jsonObject.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Can not add sym", e);

            jsonObject.put(Keys.STATUS_CODE, e.getMessage());
        }
    }

    /**
     * Is IPv4.
     *
     * @param ip ip
     * @return {@code true} if it is, returns {@code false} otherwise
     */
    public static boolean isIPv4(final String ip) {
        if (StringUtils.isBlank(ip)) {
            return false;
        }

        final String regex = "^(1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|[1-9])\\."
                + "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)\\."
                + "(00?\\d|1\\d{2}|2[0-4]\\d|25[0-5]|[1-9]\\d|\\d)$";

        final Pattern pattern = Pattern.compile(regex);
        final Matcher matcher = pattern.matcher(ip);

        return matcher.matches();
    }
}
