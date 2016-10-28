/*
 * Copyright (c) 2010-2016, b3log.org & hacpai.com
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
package org.b3log.rhythm.service;

import java.net.URL;
import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.SortDirection;
import org.b3log.latke.repository.annotation.Transactional;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.urlfetch.HTTPHeader;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.HTTPResponse;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.latke.util.CollectionUtils;
import org.b3log.rhythm.model.Sym;
import org.b3log.rhythm.repository.SymRepository;
import org.b3log.rhythm.util.Rhythms;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Sym service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Oct 29, 2016
 * @since 1.2.0
 */
@Service
public class SymService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(SymService.class.getName());

    /**
     * Sym repository.
     */
    @Inject
    private SymRepository symRepository;

    /**
     * Gets syms.
     *
     * @return a list of syms :p
     */
    public List<JSONObject> getSyms() {
        final Query query = new Query().
                setFilter(new PropertyFilter(Sym.SYM_STATUS, FilterOperator.EQUAL, Sym.SYM_STATUS_C_VALID)).
                addSort(Keys.OBJECT_ID, SortDirection.ASCENDING).
                addProjection(Sym.SYM_URL, String.class).
                addProjection(Sym.SYM_TITLE, String.class).
                addProjection(Sym.SYM_ICON, String.class).
                addProjection(Sym.SYM_DESC, String.class);
        try {
            return CollectionUtils.jsonArrayToList(symRepository.get(query).optJSONArray(Keys.RESULTS));
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets syms failed", e);

            return Collections.emptyList();
        }
    }

    /**
     * Updates the specified sym.
     *
     * @param sym the specified sym
     */
    @Transactional
    public void updateSym(final JSONObject sym) {
        try {
            symRepository.update(sym.optString(Keys.OBJECT_ID), sym);
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Updates sym failed", e);
        }
    }

    /**
     * Adds or updates a sym.
     *
     * @param sym the specified sym
     */
    @Transactional
    public void addSym(final JSONObject sym) {
        final String symURL = sym.optString(Sym.SYM_URL);

        try {
            String favicon = "";
            String desc = "";
            try {
                final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();
                final HTTPRequest request = new HTTPRequest();
                request.addHeader(new HTTPHeader("User-Agent", "B3log Rhythm/" + Rhythms.RHYTHM_VERSION));
                request.setURL(new URL(symURL));

                final HTTPResponse response = urlFetchService.fetch(request);
                if (HttpServletResponse.SC_OK == response.getResponseCode()) {
                    final String html = new String(response.getContent(), "UTF-8");
                    favicon = StringUtils.substringBetween(html, "<link rel=\"icon\" type=\"image/png\" href=\"", "\"");
                    desc = StringUtils.substringBetween(html, "<meta name=\"description\" content=\"", "\"");
                }
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Gets favicon failed [symURL=" + symURL + "]", e);
            }

            final Query query = new Query().
                    setFilter(new PropertyFilter(Sym.SYM_URL, FilterOperator.EQUAL, symURL));
            final JSONObject result = symRepository.get(query);
            final JSONArray syms = result.optJSONArray(Keys.RESULTS);

            String symTitle = sym.optString(Sym.SYM_TITLE);
            symTitle = StringUtils.trim(symTitle);
            favicon = StringUtils.trim(favicon);
            desc = StringUtils.trim(desc);

            if (syms.length() > 0) {
                // Update
                final JSONObject existSym = syms.optJSONObject(0);
                existSym.put(Sym.SYM_TITLE, symTitle);
                existSym.put(Sym.SYM_ICON, favicon);
                existSym.put(Sym.SYM_DESC, desc);

                symRepository.update(existSym.optString(Keys.OBJECT_ID), existSym);
            } else {
                // Add
                sym.put(Sym.SYM_ACCESSIBILITY_CHECK_CNT, 0);
                sym.put(Sym.SYM_ACCESSIBILITY_NOT_200_CNT, 0);
                sym.put(Sym.SYM_STATUS, Sym.SYM_STATUS_C_VALID);
                sym.put(Sym.SYM_TITLE, symTitle);
                sym.put(Sym.SYM_ICON, favicon);
                sym.put(Sym.SYM_DESC, desc);

                symRepository.add(sym);
            }

            LOGGER.info("Added a sym [symURL=" + symURL + ']');
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Adds sym failed", e);
        }
    }

}
