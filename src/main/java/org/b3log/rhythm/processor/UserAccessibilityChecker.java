/*
 * Copyright (c) 2015, b3log.org
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
import org.b3log.latke.Keys;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.model.User;
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
import org.b3log.latke.util.Strings;
import org.b3log.rhythm.service.UserService;
import org.b3log.rhythm.util.Rhythms;
import org.json.JSONObject;

/**
 * Checks accessibility of users.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.1.0.0, Jun 20, 2014
 * @since 0.2.0
 */
@RequestProcessor
public class UserAccessibilityChecker {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(UserAccessibilityChecker.class.getName());

    /**
     * User service.
     */
    @Inject
    private UserService userService;

    /**
     * Check count.
     */
    private static final int CHECK_CNT = 200;

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
    @RequestProcessing(value = "/users/accessibility", method = HTTPRequestMethod.GET)
    public void checkAccessibility(final HTTPRequestContext context) throws Exception {
        final DoNothingRenderer renderer = new DoNothingRenderer();
        context.setRenderer(renderer);

        final HttpServletRequest request = context.getRequest();
        final String key = request.getParameter("key");
        if (Strings.isEmptyOrNull(key) || !key.equals(Rhythms.CFG.getString("key"))) {
            return;
        }

        final List<JSONObject> users = userService.getUsersRandomly(CHECK_CNT);

        for (final JSONObject user : users) {
            threadService.submit(new CheckTask(user), CHECK_TIMEOUT);
        }
    }

    /**
     * User accessibility check task.
     *
     * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
     * @version 1.0.1.0, Jun 17, 2014
     */
    private class CheckTask implements Runnable {

        /**
         * User to check.
         */
        private JSONObject user;

        /**
         * Constructs a check task with the specified user.
         *
         * @param user the specified user
         */
        public CheckTask(final JSONObject user) {
            this.user = user;
        }

        @Override
        public void run() {
            final String userUrl = user.optString(User.USER_URL);

            LOGGER.debug("Checks user[url=" + userUrl + "] accessibility");

            int responseCode = 0;

            try {
                final HTTPRequest request = new HTTPRequest();
                request.addHeader(new HTTPHeader("User-Agent", "B3log Rhythm/" + Rhythms.RHYTHM_VERSION));
                request.setURL(new URL(userUrl));

                final HTTPResponse response = urlFetchService.fetch(request);

                responseCode = response.getResponseCode();
                LOGGER.log(Level.INFO, "Accesses user[url=" + userUrl + "] response[code={0}]", responseCode);
            } catch (final Exception e) {
                LOGGER.warn("User[url=" + userUrl + "] accessibility check failed [msg=" + e.getMessage() + "]");
            } finally {
                if (HttpServletResponse.SC_OK != responseCode) {
                    userService.removeUser(user.optString(Keys.OBJECT_ID));
                }
            }
        }
    }
}
