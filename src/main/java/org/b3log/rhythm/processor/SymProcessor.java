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
package org.b3log.rhythm.processor;

import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
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
 * @version 1.0.0.0, Oct 28, 2016
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
    public void addArticle(final HTTPRequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        final JSONObject jsonObject = new JSONObject();

        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);
        renderer.setJSONObject(jsonObject);

        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

            final JSONObject sym = new JSONObject();
            sym.put(Sym.SYM_URL, requestJSONObject.optString(Sym.SYM_URL));
            sym.put(Sym.SYM_TITLE, requestJSONObject.optString(Sym.SYM_TITLE));

            symService.addSym(sym);

            jsonObject.put(Keys.STATUS_CODE, true);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Can not add sym", e);

            jsonObject.put(Keys.STATUS_CODE, e.getMessage());
        }
    }
}
