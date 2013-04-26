/*
 * Copyright (c) 2009, 2010, 2011, 2012, B3log Team
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
package org.b3log.rhythm.event.symphony;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.b3log.latke.Keys;
import org.b3log.latke.event.AbstractEventListener;
import org.b3log.latke.event.Event;
import org.b3log.latke.event.EventException;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.rhythm.RhythmServletListener;
import org.b3log.rhythm.event.EventTypes;
import org.b3log.rhythm.model.Article;
import org.b3log.rhythm.model.Blog;
import org.b3log.rhythm.util.Rhythms;
import org.json.JSONObject;

/**
 * This listener is responsible for updating article to B3log Symphony.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.0, Apr 26, 2013
 * @since 0.1.6
 */
public final class ArticleUpdater extends AbstractEventListener<JSONObject> {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ArticleUpdater.class.getName());

    /**
     * URL of adding article to Rhythm.
     */
    private static final URL UPDATE_ARTICLE_URL;

    static {
        try {
            UPDATE_ARTICLE_URL = new URL(RhythmServletListener.B3LOG_SYMPHONY_SERVE_PATH + "/rhythm/article");
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void action(final Event<JSONObject> event) throws EventException {
        final JSONObject data = event.getData();
        LOGGER.log(Level.INFO, "Processing an event[type={0}, data={1}] in listener[className={2}]",
                new Object[]{event.getType(), data, ArticleUpdater.class.getName()});
        try {
            final JSONObject article = data.getJSONObject(Article.ARTICLE);

            String clientHost = data.getString(Blog.BLOG_HOST);
            if (!clientHost.startsWith("http://") && !clientHost.startsWith("https://")) {
                clientHost = "http://" + clientHost;
            }

            final String clientVersion = data.getString(Blog.BLOG_VERSION);
            final String clientName = data.getString(Blog.BLOG);
            final String clientTitle = data.getString(Blog.BLOG_TITLE);
            final String clientRuntimeEnv = data.getString("clientRuntimeEnv");
            final String userB3Key = data.getString("userB3Key");
            final String clientAdminEmail = data.getString("clientAdminEmail");

            final JSONObject request = new JSONObject();
            request.put("article", article);
            request.put("userB3Key", userB3Key);
            request.put("clientName", clientName);
            request.put("clientTitle", clientTitle);
            request.put("clientVersion", clientVersion);
            request.put("clientHost", clientHost);
            request.put("clientRuntimeEnv", clientRuntimeEnv);
            request.put("clientAdminEmail", clientAdminEmail);

            updateArticleToSymphony(request);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Sends article to Symphony error: {0}", e.getMessage());
            throw new EventException(e);
        }
    }

    /**
     * Gets the event type {@linkplain EventTypes#ADD_ARTICLE_TO_SYMPHONY}.
     *
     * @return event type
     */
    @Override
    public String getEventType() {
        return EventTypes.UPDATE_ARTICLE_TO_SYMPHONY;
    }

    /**
     * Adds an article to B3log Symphony with the specified request.
     * 
     * @param request the specified request, for example, 
     * <pre>
     * {
     *     "article": {
     *         "articleAuthorEmail": "DL88250@gmail.com",
     *         "articleContent": "&lt;p&gt;test&lt;\/p&gt;",
     *         "articleCreateDate": 1350635469922,
     *         "articlePermalink": "/articles/2012/10/19/1350635469866.html",
     *         "articleTags": "test",
     *         "articleTitle": "test",
     *         "clientArticleId": "1350635469866",
     *         "oId": "1350635469866"
     *     },
     *     "userB3Key": "",
     *     "clientName": "",
     *     "clientTitle": "",
     *     "clientVersion": "",
     *     "clientHost": "",
     *     "clientRuntimeEnv": "",
     *     "clientAdminEmail": ""
     * }
     * </pre>
     * @throws Exception exception
     */
    private static void updateArticleToSymphony(final JSONObject request) throws Exception {
        final JSONObject article = request.getJSONObject("article");
        final String userB3Key = request.getString("userB3Key");
        final String clientName = request.getString("clientName");
        final String clientTitle = request.getString("clientTitle");
        final String clientVersion = request.getString("clientVersion");
        final String clientHost = request.getString("clientHost");
        final String clientRuntimeEnv = request.getString("clientRuntimeEnv");
        final String clientAdminEmail = request.getString("clientAdminEmail");

        final HTTPRequest httpRequest = new HTTPRequest();
        httpRequest.setURL(UPDATE_ARTICLE_URL);
        httpRequest.setRequestMethod(HTTPRequestMethod.PUT);
        article.put("clientArticleId", article.getString(Keys.OBJECT_ID));

        final JSONObject requestJSONObject = new JSONObject();

        requestJSONObject.put("symphonyKey", Rhythms.KEY_OF_SYMPHONY);
        requestJSONObject.put("userB3Key", userB3Key);
        requestJSONObject.put("clientName", clientName);
        requestJSONObject.put("clientTitle", clientTitle);
        requestJSONObject.put("clientVersion", clientVersion);
        requestJSONObject.put("clientHost", clientHost);
        requestJSONObject.put("clientRuntimeEnv", clientRuntimeEnv);
        requestJSONObject.put("clientAdminEmail", clientAdminEmail);
        requestJSONObject.put(Article.ARTICLE, article);

        httpRequest.setPayload(requestJSONObject.toString().getBytes("UTF-8"));

        URLFetchServiceFactory.getURLFetchService().fetchAsync(httpRequest);

        LOGGER.log(Level.INFO, "Sent an article to Symphony [articleTitle={0}]", article.optString(Article.ARTICLE_TITLE));
    }
}
