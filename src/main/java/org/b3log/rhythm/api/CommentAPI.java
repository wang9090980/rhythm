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
package org.b3log.rhythm.api;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.RuntimeEnv;
import org.b3log.latke.cache.Cache;
import org.b3log.latke.cache.CacheFactory;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.JSONRenderer;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.latke.util.Requests;
import org.b3log.latke.util.Strings;
import org.b3log.rhythm.RhythmServletListener;
import org.b3log.rhythm.model.Article;
import org.b3log.rhythm.model.Common;
import org.b3log.rhythm.util.Rhythms;
import org.b3log.rhythm.util.Securities;
import org.json.JSONObject;

/**
 * Comment API processor. Please visit <a href="https://hacpai.com/article/1457158841475">社区内容 API 开放，欢迎各位独立博客主进行连接</a>
 * for more details.
 *
 * <ul>
 * <li>Adds a comment (/api/comment), POST</li>
 * </ul>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.2, Mar 7, 2016
 * @since 1.1.0
 */
@RequestProcessor
public class CommentAPI {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(CommentAPI.class.getName());

    /**
     * Cache.
     */
    @SuppressWarnings("unchecked")
    private Cache<String, Serializable> cache = (Cache<String, Serializable>) CacheFactory.getCache("RhythmCache");

    /**
     * URL fetch service.
     */
    private final URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    /**
     * Shows add comment.
     *
     * @param context the specified context
     * @throws IOException exception
     */
    @RequestProcessing(value = "/api/comment", method = HTTPRequestMethod.GET)
    public void showAddComment(final HTTPRequestContext context) throws IOException {
        context.getResponse().sendRedirect("https://hacpai.com/article/1457158841475");
    }

    /**
     * Adds a comment.
     *
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": true,
     *     "msg": ""
     * }
     * </pre>
     * </p>
     *
     * @param context the specified context, including a request json object, for example,      <pre>
     * {
     *     "comment": {
     *         "id": "1165070220000",
     *         "articleId": "1164070220000",
     *         "content": "Test comment",
     *         "authorName": "Daniel",            // optional
     *         "authorEmail": "dl88250@gmail.com" // optional
     *     },
     *     "client": {
     *         "title": "我的个人博客",
     *         "host": "http://xxx.com",
     *         "email": "test@hacpai.com",
     *         "key": "xxxx"
     *     }
     * }
     * </pre>
     */
    @RequestProcessing(value = "/api/comment", method = HTTPRequestMethod.POST)
    public void addComment(final HTTPRequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put(Common.SUCC, true);
        jsonObject.put(Keys.MSG, "Add a comment successfully");

        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);
        renderer.setJSONObject(jsonObject);

        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

            final JSONObject client = requestJSONObject.optJSONObject(Common.CLIENT);
            if (null == client) {
                jsonObject.put(Common.SUCC, true);
                jsonObject.put(Keys.MSG, "[client] is null");

                return;
            }

            final String clientTitle = client.optString(Common.TITLE);
            if (StringUtils.isBlank(clientTitle)) {
                jsonObject.put(Common.SUCC, true);
                jsonObject.put(Keys.MSG, "[client] is null");

                return;
            }
            if (StringUtils.length(clientTitle) > ArticleAPI.CLIENT_TITLE_MAX_LENGTH) {
                jsonObject.put(Keys.STATUS_CODE, "[client.title] length should be [1, 32]");

                return;
            }

            String clientHost = client.optString(Common.HOST);
            if (!Strings.isURL(clientHost)) {
                clientHost = "http://" + clientHost;

                if (!Securities.validHost(clientHost)) {
                    jsonObject.put(Keys.STATUS_CODE, "Invalid [client.host=" + clientHost + "]");

                    return;
                }
            }

            final URL hostURL = new URL(clientHost);
            clientHost = hostURL.getProtocol() + "://" + hostURL.getHost();
            if (-1 != hostURL.getPort()) {
                clientHost += ":" + hostURL.getPort();
            }

            final String clientEmail = client.optString(Common.EMAIL);
            if (!Strings.isEmail(clientEmail)) {
                jsonObject.put(Keys.STATUS_CODE, "Invalid [client.email" + clientEmail + "]");

                return;
            }

            final String clientKey = client.optString(Common.KEY);
            if (StringUtils.isBlank(clientKey)) {
                jsonObject.put(Keys.STATUS_CODE, "Invalid [client.key=" + clientKey + "]");

                return;
            }

            final JSONObject comment = requestJSONObject.optJSONObject(Common.COMMENT);
            if (null == comment) {
                jsonObject.put(Keys.STATUS_CODE, "[comment] is null");

                return;
            }

            final String commentId = comment.optString(Common.ID);
            if (StringUtils.isBlank(commentId)) {
                jsonObject.put(Keys.STATUS_CODE, "[comment.id] is null");

                return;
            }

            if (StringUtils.length(commentId) > ArticleAPI.COMMENT_ID_MAX_LENGTH) {
                jsonObject.put(Keys.STATUS_CODE, "[comment.id] length should be [1, " + ArticleAPI.COMMENT_ID_MAX_LENGTH + "]");

                return;
            }

            final String articleId = comment.optString(Article.ARTICLE_ID);
            if (StringUtils.isBlank(articleId)) {
                jsonObject.put(Keys.STATUS_CODE, "[comment.articleId] is null");

                return;
            }

            if (StringUtils.length(articleId) > ArticleAPI.ARTICLE_ID_MAX_LENGTH) {
                jsonObject.put(Keys.STATUS_CODE, "[comment.articleId] length should be [1, " + ArticleAPI.ARTICLE_ID_MAX_LENGTH + "]");

                return;
            }

            String commentContent = comment.optString(Common.CONTENT);
            // TODO: check comment.content
            commentContent = Securities.securedHTML(commentContent);

            LOGGER.log(Level.INFO, "Data [{0}]", requestJSONObject.toString(Rhythms.INDENT_FACTOR));

            Long latestPostTime = (Long) cache.get(clientEmail + ".lastPostTime");
            final Long currentPostTime = System.currentTimeMillis();
            if (null == latestPostTime) {
                latestPostTime = 0L;
            }
            try {
                if (latestPostTime > (currentPostTime - Rhythms.MIN_STEP_POST_TIME)) {
                    jsonObject.put(Keys.STATUS_CODE, "Too Frequent");

                    return;
                }
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Invalid request [clientHost=" + clientHost + "]", e);

                return;
            }

            latestPostTime = currentPostTime;

            try {
                final JSONObject data = new JSONObject();
                /*
                {
                    "comment": {
                        "commentId": "", // client comment id
                        "articleId": "",
                        "commentContent": "",
                        "commentAuthorName": "", // optional, 'default commenter'
                        "commentAuthorEmail": "" // optional, 'default commenter'
                },
                    "clientName": "",
                    "clientVersion": "",
                    "clientHost": "",
                    "clientRuntimeEnv": "" // LOCAL
                    "clientAdminEmail": "",
                    "userB3Key": ""
                }
                 */

                final JSONObject dataComment = new JSONObject();
                dataComment.put(Common.COMMENT_ID, commentId);
                dataComment.put(Article.ARTICLE_ID, articleId);
                dataComment.put(Common.COMMENT_CONTENT, commentContent);
                dataComment.put(Common.COMMENT_AUTHOR_NAME, comment.optString(Common.AUTHOR_NAME));
                dataComment.put(Common.COMMENT_AUTHOR_EMAIL, comment.optString(Common.AUTHOR_EMAIL));
                data.put(Common.COMMENT, dataComment);

                data.put(Common.CLIENT_NAME, "Other");
                data.put(Common.CLIENT_VERSION, "1.0.0");
                data.put(Common.CLIENT_HOST, clientHost);
                data.put(Common.CLIENT_RUNTIME_ENV, RuntimeEnv.LOCAL.toString());
                data.put(Common.CLIENT_ADMIN_EMAIL, clientEmail);
                data.put(Common.USER_B3_KEY, clientKey);

                final HTTPRequest httpRequest = new HTTPRequest();
                httpRequest.setURL(new URL(RhythmServletListener.B3LOG_SYMPHONY_SERVE_PATH + "/solo/comment"));
                httpRequest.setRequestMethod(HTTPRequestMethod.POST);
                httpRequest.setPayload(data.toString().getBytes("UTF-8"));

                urlFetchService.fetchAsync(httpRequest);
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, e.getMessage(), e);
            }

            jsonObject.put(Common.SUCC, true);

            cache.put(clientEmail + ".lastPostTime", latestPostTime);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Can not add article", e);

            jsonObject.put(Keys.STATUS_CODE, e.getMessage());
        }
    }
}
