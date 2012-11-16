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
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.rhythm.event.EventTypes;
import org.b3log.rhythm.model.Article;
import org.b3log.rhythm.model.Blog;
import org.b3log.rhythm.util.Rhythms;
import org.json.JSONObject;

/**
 * This listener is responsible for sending article to B3log Symphony.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.1.2, Nov 16, 2012
 * @since 0.1.4
 */
public final class ArticleSender
        extends AbstractEventListener<JSONObject> {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ArticleSender.class.getName());
    /**
     * URL fetch service.
     */
    private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();
    /**
     * URL of adding article to Rhythm.
     */
    private static final URL ADD_ARTICLE_URL;

    static {
        try {
            ADD_ARTICLE_URL = new URL("http://symphony.b3log.org:80/rhythm/article");
        } catch (final MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void action(final Event<JSONObject> event) throws EventException {
        final JSONObject data = event.getData();
        LOGGER.log(Level.FINER, "Processing an event[type={0}, data={1}] in listener[className={2}]",
                   new Object[]{event.getType(), data, ArticleSender.class.getName()});
        try {
            final JSONObject article = data.getJSONObject(Article.ARTICLE);

            final String clientHost = data.getString(Blog.BLOG_HOST);
            final String clientVersion = data.getString(Blog.BLOG_VERSION);
            final String clientName = data.getString(Blog.BLOG);
            final String clientRuntimeEnv = data.getString("clientRuntimeEnv");
            final String userB3Key = data.getString("userB3Key");
            final String clientAdminEmail = data.getString("clientAdminEmail");

            final HTTPRequest httpRequest = new HTTPRequest();
            httpRequest.setURL(ADD_ARTICLE_URL);
            httpRequest.setRequestMethod(HTTPRequestMethod.POST);
            article.put("clientArticleId", article.getString(Keys.OBJECT_ID));

            httpRequest.addPayloadEntry("symphonyKey", Rhythms.KEY_OF_SYMPHONY);
            httpRequest.addPayloadEntry("userB3Key", userB3Key);
            httpRequest.addPayloadEntry("clientName", clientName);
            httpRequest.addPayloadEntry("clientVersion", clientVersion);
            httpRequest.addPayloadEntry("clientHost", clientHost);
            httpRequest.addPayloadEntry("clientRuntimeEnv", clientRuntimeEnv);
            httpRequest.addPayloadEntry("clientAdminEmail", clientAdminEmail);
            httpRequest.addPayloadEntry(Article.ARTICLE, article.toString());

            urlFetchService.fetchAsync(httpRequest);
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
        return EventTypes.ADD_ARTICLE_TO_SYMPHONY;
    }
}
