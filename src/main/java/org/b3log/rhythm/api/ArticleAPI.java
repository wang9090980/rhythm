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

import java.io.Serializable;
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
import static org.b3log.rhythm.model.Article.ARTICLE;
import static org.b3log.rhythm.model.Article.ARTICLE_AUTHOR_EMAIL;
import static org.b3log.rhythm.model.Article.ARTICLE_ORIGINAL_ID;
import static org.b3log.rhythm.model.Article.ARTICLE_PERMALINK;
import static org.b3log.rhythm.model.Article.ARTICLE_TAGS_REF;
import static org.b3log.rhythm.model.Article.ARTICLE_TITLE;
import org.b3log.rhythm.model.Blog;
import org.b3log.rhythm.model.Common;
import org.b3log.rhythm.model.Tag;
import org.b3log.rhythm.processor.StatusCodes;
import org.b3log.rhythm.service.ArticleService;
import org.b3log.rhythm.util.Rhythms;
import org.b3log.rhythm.util.Securities;
import org.json.JSONObject;

/**
 * Article API processor.
 *
 * <ul>
 * <li>Adds an article (/article/add), POST</li>
 * <li>Updates an article (/article/update), POST</li>
 * </ul>
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Mar 5, 2016
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
    private static final int CLIENT_TITLE_MAX_LENGTH = 32;

    /**
     * Article id max length.
     */
    private static final int ARTICLE_ID_MAX_LENGTH = 32;

    /**
     * Article title max length.
     */
    private static final int ARTICLE_TITLE_MAX_LENGTH = 32;

    /**
     * Article permalink max length.
     */
    private static final int ARTICLE_PERMALINK_MAX_LENGTH = 64;

    /**
     * Adds an article.
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
     *     "article": {
     *         "id": "1165070220000",                  // 客户端的文章 id，如果在社区重复了将不能发布成功
     *         "title": "这是一篇测试文章",               // 文章标题
     *         "permalink": "/test-post",              // 客户端固定链接
     *         "tags": "tag1, tag2, ....",             // 文章标签，英文输入状态的逗号分隔
     *         "content": "Test"                       // 文章内容，HTML 或 Markdown 格式
     *     },
     *     "client": {
     *         "title": "我的个人博客",                  // 客户端标题，比如博客的名字
     *         "host": "http://xxx.com",               // 客户端 URL
     *         "email": "test@hacpai.com",             // 账号邮箱，需要和社区注册的账号邮箱一致
     *         "key": "xxxx"                           // 账号同步 Key，需要和社区中配置的一致
     *     }
     * }
     * </pre>
     */
    @RequestProcessing(value = "/article/add", method = HTTPRequestMethod.POST)
    public void addArticle(final HTTPRequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        final JSONObject jsonObject = new JSONObject();
        jsonObject.put(Common.SUCC, true);
        jsonObject.put(Keys.MSG, "Publish an article successfully");

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
            if (StringUtils.length(clientTitle) > CLIENT_TITLE_MAX_LENGTH) {
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

            final JSONObject article = requestJSONObject.optJSONObject(Article.ARTICLE);
            if (null == article) {
                jsonObject.put(Keys.STATUS_CODE, "[article] is null");

                return;
            }

            final String articleId = article.optString(Common.ID);
            if (StringUtils.isBlank(articleId)) {
                jsonObject.put(Keys.STATUS_CODE, "[article.id] is null");

                return;
            }

            if (StringUtils.length(articleId) > ARTICLE_ID_MAX_LENGTH) {
                jsonObject.put(Keys.STATUS_CODE, "[article.id] length should be [1, 32]");

                return;
            }

            String articleTitle = article.optString(Common.TITLE);
            if (StringUtils.isBlank(articleTitle)) {
                jsonObject.put(Keys.STATUS_CODE, "[article.title] is null");

                return;
            }

            if (StringUtils.length(articleTitle) > ARTICLE_TITLE_MAX_LENGTH) {
                jsonObject.put(Keys.STATUS_CODE, "[article.title] length should be [1, 32]");

                return;
            }

            articleTitle = Securities.securedHTML(articleTitle);

            final String articlePermalink = article.optString(Common.PERMALINK);
            if (StringUtils.isBlank(articlePermalink)) {
                jsonObject.put(Keys.STATUS_CODE, "[article.permalink] is null");

                return;
            }

            if (StringUtils.length(articlePermalink) > ARTICLE_PERMALINK_MAX_LENGTH) {
                jsonObject.put(Keys.STATUS_CODE, "[article.permalink] length should be [1, 64]");

                return;
            }

            String articleTags = article.optString(Tag.TAGS);
            articleTags = Securities.securedHTML(articleTags);
            // TODO: check article.tags
            String articleContent = article.optString(Common.CONTENT);
            // TODO: check article.content
            articleContent = Securities.securedHTML(articleContent);

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

            final JSONObject postArticle = new JSONObject();

            postArticle.put(ARTICLE_ORIGINAL_ID, articleId);
            postArticle.put(ARTICLE_TITLE, articleTitle);

            postArticle.put(ARTICLE_AUTHOR_EMAIL, clientEmail);
            if (articleTags.contains("B3log Broadcast")) {
                jsonObject.put(Keys.STATUS_CODE, "Invalid [tags]");

                return;
            }

            articleTags = articleTags.replaceAll("，", ",");
            postArticle.put(ARTICLE_TAGS_REF, articleTags);

            if ("aBroadcast".equals(articlePermalink)) {
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

    /**
     * Updates an article.
     *
     * <p>
     * Renders the response with a json object, for example,
     * <pre>
     * {
     *     "sc": "ADD_ARTICLE_SUCC"
     * }
     * </pre>
     * </p>
     *
     * @param context the specified context, including a request json object, for example,      <pre>
     * {
     *     "article": {
     *         "oId": "",
     *         "articleTitle": "",
     *         "articlePermalink": "/test",
     *         "articleTags": "tag1, tag2, ....",
     *         "articleAuthorEmail": "",
     *         "articleContent": "",
     *         "articleCreateDate": long,
     *         "postToCommunity": boolean
     *     },
     *     "blogTitle": "",
     *     "blogHost": "http://xxx.com", // clientHost
     *     "blogVersion": "", // clientVersion
     *     "blog": "", // clientName
     *     "userB3Key": ""
     *     "clientRuntimeEnv": "",
     *     "clientAdminEmail": ""
     * }
     * </pre>
     */
    @RequestProcessing(value = "/article/update", method = HTTPRequestMethod.POST)
    public void updateArticle(final HTTPRequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        final JSONObject jsonObject = new JSONObject();

        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);
        renderer.setJSONObject(jsonObject);

        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

            LOGGER.log(Level.TRACE, "Request[data={0}]", requestJSONObject);
            final String blog = requestJSONObject.optString(Blog.BLOG);
            if (!Rhythms.isValidClient(blog)) {
                jsonObject.put(Keys.STATUS_CODE, "Unsupported Client");

                return;
            }

            String blogHost = requestJSONObject.getString(Blog.BLOG_HOST);
            if (!Strings.isURL(blogHost)) {
                blogHost = "http://" + blogHost;

                if (!Securities.validHost(blogHost)) {
                    jsonObject.put(Keys.STATUS_CODE, "Invalid Host");

                    return;
                }
            }

            final String blogTitle = requestJSONObject.getString(Blog.BLOG_TITLE);
            if (!Securities.validTitle(blogTitle)) {
                jsonObject.put(Keys.STATUS_CODE, "Invalid title");

                return;
            }

            final String blogVersion = requestJSONObject.optString(Blog.BLOG_VERSION);

            if (!Rhythms.RELEASED_SOLO_VERSIONS.contains(blogVersion) && !Rhythms.SNAPSHOT_SOLO_VERSION.equals(blogVersion)) {
                LOGGER.log(Level.WARN, "Version of Solo[host={0}] is [{1}], so ignored this request",
                        new String[]{blogHost, blogVersion});
                jsonObject.put(Keys.STATUS_CODE, StatusCodes.IGNORE_REQUEST);

                return;
            }

            final JSONObject originalArticle = requestJSONObject.getJSONObject(ARTICLE);
            Securities.securityProcess(originalArticle);

            LOGGER.log(Level.INFO, "Data[articleTitle={0}] come from Solo[host={1}, version={2}]",
                    new Object[]{originalArticle.getString(ARTICLE_TITLE), blogHost, blogVersion});
            final String authorEmail = originalArticle.getString(ARTICLE_AUTHOR_EMAIL);

            Long latestPostTime = (Long) cache.get(authorEmail + ".lastPostTime");
            final Long currentPostTime = System.currentTimeMillis();
            if (null == latestPostTime) {
                latestPostTime = 0L;
            }
            try {
                if (latestPostTime > (currentPostTime - Rhythms.MIN_STEP_POST_TIME)) {
                    jsonObject.put(Keys.STATUS_CODE, "Too Frequent");

                    return;
                }

                // TODO: check article
//                if (isInvalid(data)) {
//                    ret.put(Keys.STATUS_CODE, false);
//                    ret.put(Keys.MSG, Langs.get("badRequestLabel"));
//
//                    return ret;
//                }
            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Invalid request [blogHost=" + blogHost + "]", e);
                return;
            }

            latestPostTime = currentPostTime;

            final JSONObject article = new JSONObject();

            final String id = originalArticle.getString(Keys.OBJECT_ID);
            article.put(ARTICLE_ORIGINAL_ID, id);
            article.put(ARTICLE_TITLE, originalArticle.getString(ARTICLE_TITLE));

            article.put(ARTICLE_AUTHOR_EMAIL, authorEmail);
            String tagString = originalArticle.getString(ARTICLE_TAGS_REF);
            if (tagString.contains("B3log Broadcast")) {
                jsonObject.put(Keys.STATUS_CODE, "Invalid Tag");

                return;
            }

            tagString = tagString.replaceAll("，", ",");
            article.put(ARTICLE_TAGS_REF, tagString);

            String permalink = originalArticle.getString(ARTICLE_PERMALINK);
            if ("aBroadcast".equals(permalink)) {
                jsonObject.put(Keys.STATUS_CODE, "Invalid Permalink");

                return;
            }

            permalink = blogHost + originalArticle.getString(ARTICLE_PERMALINK);

            article.put(ARTICLE_PERMALINK, permalink);
            article.put(Blog.BLOG_HOST, blogHost);
            article.put(Blog.BLOG, blog);
            article.put(Blog.BLOG_VERSION, blogVersion);
            article.put(Blog.BLOG_TITLE, blogTitle);

            articleService.updateByOriginalId(article);

            if (originalArticle.optBoolean(Common.POST_TO_COMMUNITY, true)) {
                try {
                    originalArticle.remove(Common.POST_TO_COMMUNITY);

                    eventManager.fireEventSynchronously(new Event<JSONObject>(EventTypes.UPDATE_ARTICLE_TO_SYMPHONY, requestJSONObject));
                } catch (final EventException e) {
                    LOGGER.log(Level.ERROR, e.getMessage(), e);
                }
            }

            jsonObject.put(Keys.STATUS_CODE, StatusCodes.ADD_ARTICLE_SUCC);

            cache.put(authorEmail + ".lastPostTime", latestPostTime);
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Can not add article", e);

            jsonObject.put(Keys.STATUS_CODE, e.getMessage());
        }
    }

}
