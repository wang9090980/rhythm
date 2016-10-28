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

import javax.inject.Inject;
import org.b3log.latke.Keys;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.annotation.Transactional;
import org.b3log.latke.service.annotation.Service;
import org.b3log.rhythm.model.Sym;
import org.b3log.rhythm.repository.SymRepository;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Sym service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Oct 28, 2016
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
     * Adds or updates a sym.
     *
     * @param sym the specified sym
     */
    @Transactional
    public void addSym(final JSONObject sym) {
        final Query query = new Query().
                setFilter(new PropertyFilter(Sym.SYM_URL, FilterOperator.EQUAL, sym.optString(Sym.SYM_URL)));

        try {
            final JSONObject result = symRepository.get(query);
            final JSONArray syms = result.optJSONArray(Keys.RESULTS);
            if (syms.length() > 0) {
                // Update
                final JSONObject existSym = syms.optJSONObject(0);

                existSym.put(Sym.SYM_TITLE, sym.optString(Sym.SYM_TITLE));

                symRepository.update(existSym.optString(Keys.OBJECT_ID), existSym);
            } else {
                // Add
                sym.put(Sym.SYM_ACCESSIBILITY_CHECK_CNT, 0);
                sym.put(Sym.SYM_ACCESSIBILITY_NOT_200_CNT, 0);
                sym.put(Sym.SYM_STATUS, Sym.SYM_STATUS_C_VALID);
                sym.put(Sym.SYM_TITLE, sym.optString(Sym.SYM_TITLE));

                symRepository.add(sym);
            }

            LOGGER.info("Added a sym [symURL=" + sym.optString(Sym.SYM_URL) + ']');
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Adds sym failed", e);
        }
    }

}
