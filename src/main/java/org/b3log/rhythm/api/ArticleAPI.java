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
package org.b3log.rhythm.api;

import java.io.IOException;
import java.io.Serializable;
import java.net.URL;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.b3log.latke.Keys;
import org.b3log.latke.RuntimeEnv;
import org.b3log.latke.cache.Cache;
import org.b3log.latke.cache.CacheFactory;
import org.b3log.latke.event.Event;
import org.b3log.latke.event.EventException;
import org.b3log.latke.event.EventManager;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.JSONRenderer;
import org.b3log.latke.util.Requests;
import org.b3log.latke.util.Strings;
import org.b3log.rhythm.event.EventTypes;
import org.b3log.rhythm.model.Article;

import static org.b3log.rhythm.model.Article.ARTICLE_AUTHOR_EMAIL;
import static org.b3log.rhythm.model.Article.ARTICLE_ORIGINAL_ID;
import static org.b3log.rhythm.model.Article.ARTICLE_PERMALINK;
import static org.b3log.rhythm.model.Article.ARTICLE_TAGS_REF;
import static org.b3log.rhythm.model.Article.ARTICLE_TITLE;

import org.b3log.rhythm.model.Blog;
import org.b3log.rhythm.model.Common;
import org.b3log.rhythm.model.Tag;
import org.b3log.rhythm.service.ArticleService;
import org.b3log.rhythm.util.Rhythms;
import org.b3log.rhythm.util.Securities;
import org.json.JSONObject;

/**
 * Article API processor. Please visit <a href="https://hacpai.com/article/1457158841475">社区内容 API 开放，欢迎各位独立博客主进行连接</a>
 * for more details.
 * <p>
 * <ul>
 * <li>Posts (adds/updates) an article (/api/article), POST</li>
 * </ul>
 * </p>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.0.5, Jan 3, 2017
 * @since 1.1.0
 */
@RequestProcessor
public class ArticleAPI {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ArticleAPI.class.getName());

    /**
     * Article service.
     */
    @Inject
    private ArticleService articleService;

    /**
     * Cache.
     */
    @SuppressWarnings("unchecked")
    private Cache<String, Serializable> cache = (Cache<String, Serializable>) CacheFactory.getCache("RhythmCache");

    /**
     * Event manager.
     */
    @Inject
    private EventManager eventManager;

    /**
     * Client title max length.
     */
    public static final int CLIENT_TITLE_MAX_LENGTH = 32;

    /**
     * Article id max length.
     */
    public static final int ARTICLE_ID_MAX_LENGTH = 32;

    /**
     * Comment id max length.
     */
    public static final int COMMENT_ID_MAX_LENGTH = 32;

    /**
     * Article title max length.
     */
    public static final int ARTICLE_TITLE_MAX_LENGTH = 128;

    /**
     * Article permalink max length.
     */
    public static final int ARTICLE_PERMALINK_MAX_LENGTH = 64;

    /**
     * Shows post article.
     *
     * @param context the specified context
     * @throws IOException exception
     */
    @RequestProcessing(value = "/api/article", method = HTTPRequestMethod.GET)
    public void showPostArticle(final HTTPRequestContext context) throws IOException {
        context.getResponse().sendRedirect("https://hacpai.com/article/1457158841475");
    }

    /**
     * Posts an article.
     * <p>
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
     *                               {
     *                                   "article": {
     *                                       "id": "1165070220000",
     *                                       "title": "这是一篇测试文章",
     *                                       "permalink": "/test-post",
     *                                       "tags": "tag1, tag2",
     *                                       "content": "Test"
     *                                   },
     *                                   "client": {
     *                                       "title": "我的个人博客",
     *                                       "host": "http://xxx.com",
     *                                       "email": "test@hacpai.com",
     *                                       "key": "xxxx"
     *                                   }
     *                               }
     *                               </pre>
     */
    @RequestProcessing(value = "/api/article", method = HTTPRequestMethod.POST)
    public void postArticle(final HTTPRequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put(Common.SUCC, true);
        jsonObject.put(Keys.MSG, "Post an article successfully");

        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);
        renderer.setJSONObject(jsonObject);

        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

            final JSONObject client = requestJSONObject.optJSONObject(Common.CLIENT);
            if (null == client) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.MSG, "[client] is null");

                return;
            }

            final String clientTitle = client.optString(Common.TITLE);
            if (StringUtils.isBlank(clientTitle)) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.MSG, "[client] is null");

                return;
            }
            if (StringUtils.length(clientTitle) > CLIENT_TITLE_MAX_LENGTH) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[client.title] length should be [1, 32]");

                return;
            }

            String clientHost = client.optString(Common.HOST);
            if (!Strings.isURL(clientHost)) {
                clientHost = "http://" + clientHost;

                if (!Securities.validHost(clientHost)) {
                    jsonObject.put(Common.SUCC, false);
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
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "Invalid [client.email" + clientEmail + "]");

                return;
            }

            final String clientKey = client.optString(Common.KEY);
            if (StringUtils.isBlank(clientKey)) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "Invalid [client.key=" + clientKey + "]");

                return;
            }

            final JSONObject article = requestJSONObject.optJSONObject(Article.ARTICLE);
            if (null == article) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[article] is null");

                return;
            }

            final String articleId = article.optString(Common.ID);
            if (StringUtils.isBlank(articleId)) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[article.id] is null");

                return;
            }

            if (StringUtils.length(articleId) > ARTICLE_ID_MAX_LENGTH) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[article.id] length should be [1, " + ARTICLE_ID_MAX_LENGTH + "]");

                return;
            }

            String articleTitle = article.optString(Common.TITLE);
            if (StringUtils.isBlank(articleTitle)) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[article.title] is null");

                return;
            }

            if (StringUtils.length(articleTitle) > ARTICLE_TITLE_MAX_LENGTH) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[article.title] length should be [1, " + ARTICLE_TITLE_MAX_LENGTH + "]");

                return;
            }

            articleTitle = Securities.securedHTML(articleTitle);

            final String articlePermalink = article.optString(Common.PERMALINK);
            if (StringUtils.isBlank(articlePermalink)) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[article.permalink] is null");

                return;
            }

            if (StringUtils.length(articlePermalink) > ARTICLE_PERMALINK_MAX_LENGTH) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[article.permalink] length should be [1, " + ARTICLE_PERMALINK_MAX_LENGTH + "]");

                return;
            }

            if (!StringUtils.startsWith(articlePermalink, "/")) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[article.permalink] should start with /, for example, /hello-world");

                return;
            }

            final String articleContent = article.optString(Common.CONTENT);

            if (StringUtils.length(articleContent) < Article.MIN_CONTENT_LENGTH) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "[article.content] length too short");

                return;
            }

            String articleTags = article.optString(Tag.TAGS);
            articleTags = Securities.securedHTML(articleTags);
            articleTags = "B3log," + articleTags;
            articleTags = Tag.formatTags(articleTags);


            LOGGER.log(Level.INFO, "Data [{0}]", requestJSONObject.toString(Rhythms.INDENT_FACTOR));

            Long latestPostTime = (Long) cache.get(clientEmail + ".lastPostTime");
            final Long currentPostTime = System.currentTimeMillis();
            if (null == latestPostTime) {
                latestPostTime = 0L;
            }

            if (latestPostTime > (currentPostTime - Rhythms.MIN_STEP_POST_TIME)) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "Too Frequent");

                return;
            }

            latestPostTime = currentPostTime;

            final JSONObject postArticle = new JSONObject();

            postArticle.put(ARTICLE_ORIGINAL_ID, articleId);
            postArticle.put(ARTICLE_TITLE, articleTitle);

            postArticle.put(ARTICLE_AUTHOR_EMAIL, clientEmail);
            if (articleTags.contains("B3log广播") || articleTags.contains("B3log Broadcast")) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "Invalid [tags]");

                return;
            }

            postArticle.put(ARTICLE_TAGS_REF, articleTags);

            if ("aBroadcast".equals(articlePermalink)) {
                jsonObject.put(Common.SUCC, false);
                jsonObject.put(Keys.STATUS_CODE, "Invalid [permalink]");

                return;
            }

            postArticle.put(ARTICLE_PERMALINK, clientHost + articlePermalink);
            postArticle.put(Blog.BLOG_HOST, clientHost);
            final String clientName = "Other";
            postArticle.put(Blog.BLOG, clientName);
            final String clientVer = "1.0.0";
            postArticle.put(Blog.BLOG_VERSION, clientVer);
            postArticle.put(Blog.BLOG_TITLE, clientTitle);

            articleService.addArticle(postArticle);

            try {
                final JSONObject data = new JSONObject();
                /*
                {
                    "article": {
                        "oId": "",
                        "articleTitle": "",
                        "articlePermalink": "/test",
                        "articleTags": "tag1, tag2, ....",
                        "articleAuthorEmail": "",
                        "articleContent": "",
                        "articleCreateDate": long,
                        "postToCommunity": boolean
                    },
                    "blogTitle": "",
                    "blogHost": "http://xxx.com", // clientHost
                    "blogVersion": "", // clientVersion
                    "blog": "", // clientName
                    "userB3Key": ""
                    "clientRuntimeEnv": "",
                    "clientAdminEmail": ""
                }
                 */

                final JSONObject dataArticle = new JSONObject();
                dataArticle.put(Keys.OBJECT_ID, articleId);
                dataArticle.put(Article.ARTICLE_TITLE, articleTitle);
                dataArticle.put(Article.ARTICLE_PERMALINK, articlePermalink);
                dataArticle.put(Article.ARTICLE_TAGS_REF, articleTags);
                dataArticle.put(Article.ARTICLE_AUTHOR_EMAIL, clientEmail);
                dataArticle.put(Article.ARTICLE_CONTENT, articleContent);
                dataArticle.put(Article.ARTICLE_CREATE_DATE, System.currentTimeMillis());
                dataArticle.put(Common.POST_TO_COMMUNITY, true);
                data.put(Article.ARTICLE, dataArticle);

                data.put(Blog.BLOG_TITLE, clientTitle);
                data.put(Blog.BLOG_HOST, clientHost);
                data.put(Blog.BLOG_VERSION, clientVer);
                data.put(Blog.BLOG, clientName);
                data.put(Common.USER_B3_KEY, clientKey);
                data.put(Common.CLIENT_RUNTIME_ENV, RuntimeEnv.LOCAL.toString());
                data.put(Common.CLIENT_ADMIN_EMAIL, clientEmail);

                eventManager.fireEventAsynchronously(new Event<JSONObject>(EventTypes.ADD_ARTICLE_TO_SYMPHONY, data));
            } catch (final EventException e) {
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
