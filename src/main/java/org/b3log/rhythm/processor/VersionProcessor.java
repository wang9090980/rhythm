/*
 * Copyright (c) 2009, 2010, 2011, 2012, 2013, B3log Team
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

import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang.StringUtils;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.JSONRenderer;
import org.b3log.latke.util.Strings;
import org.b3log.rhythm.model.Solo;
import org.b3log.rhythm.util.Rhythms;
import org.json.JSONObject;

/**
 * Version processor.
 * 
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.4, Dec 28, 2012
 * @since 0.1.4
 */
@RequestProcessor
public final class VersionProcessor {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(VersionProcessor.class.getName());

    /**
     * Gets the latest version of the B3log Solo.
     * 
     * @param context the specified context
     * @throws Exception exception 
     */
    @RequestProcessing(value = {"/version/solo/latest/*", "/version/solo/latest"}, method = HTTPRequestMethod.GET)
    public void getLatestSoloVersion(final HTTPRequestContext context) throws Exception {
        final HttpServletRequest request = context.getRequest();
        String callbackFuncName = request.getParameter("callback");
        if (Strings.isEmptyOrNull(callbackFuncName)) {
            callbackFuncName = "callback";
        }

        final JSONObject jsonObject = new JSONObject();

        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);

        renderer.setCallback(callbackFuncName); // Sets JSONP
        renderer.setJSONObject(jsonObject);

        String currentVersion = request.getRequestURI();
        currentVersion = StringUtils.substringAfter(currentVersion, "/version/solo/latest");
        if (currentVersion.startsWith("/")) {
            currentVersion = currentVersion.substring(1);
        }

        final String latestVersion = Rhythms.getLatestSoloVersion(currentVersion);

        LOGGER.log(Level.DEBUG, "Version[client={0}, latest={1}]", new Object[]{currentVersion, latestVersion});

        jsonObject.put(Solo.SOLO_VERSION, latestVersion);

        jsonObject.put(Solo.SOLO_DOWNLOAD, "http://pan.baidu.com/share/link?shareid=541735&uk=3255126224#dir/path=%2Fb3log-solo%2F0.6.0");
    }
}
