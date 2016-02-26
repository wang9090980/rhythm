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
import org.b3log.latke.Keys;
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
import static org.b3log.rhythm.model.Article.ARTICLE;
import static org.b3log.rhythm.model.Article.ARTICLE_AUTHOR_EMAIL;
import static org.b3log.rhythm.model.Article.ARTICLE_ORIGINAL_ID;
import static org.b3log.rhythm.model.Article.ARTICLE_PERMALINK;
import static org.b3log.rhythm.model.Article.ARTICLE_TAGS_REF;
import static org.b3log.rhythm.model.Article.ARTICLE_TITLE;
import org.b3log.rhythm.model.Blog;
import org.b3log.rhythm.model.Common;
import org.b3log.rhythm.processor.StatusCodes;
import org.b3log.rhythm.service.ArticleService;
import org.b3log.rhythm.util.Rhythms;
import org.b3log.rhythm.util.Securities;
import org.json.JSONObject;

/**
 * Article processor.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Feb 25, 2016
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
     * Adds an article.
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
    @RequestProcessing(value = "/article", method = HTTPRequestMethod.POST)
    public void addArticle(final HTTPRequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        final JSONObject jsonObject = new JSONObject();

        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);
        renderer.setJSONObject(jsonObject);

        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

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
                    new String[]{originalArticle.getString(ARTICLE_TITLE), blogHost, blogVersion});
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

            articleService.addArticle(article);

            if (originalArticle.optBoolean(Common.POST_TO_COMMUNITY, true)) {
                try {
                    originalArticle.remove(Common.POST_TO_COMMUNITY);

                    eventManager.fireEventSynchronously(new Event<JSONObject>(EventTypes.ADD_ARTICLE_TO_SYMPHONY, requestJSONObject));
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
    @RequestProcessing(value = "/article", method = HTTPRequestMethod.PUT)
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
