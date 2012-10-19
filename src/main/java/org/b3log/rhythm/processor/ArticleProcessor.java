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
package org.b3log.rhythm.processor;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.b3log.latke.Keys;
import org.b3log.latke.cache.Cache;
import org.b3log.latke.cache.CacheFactory;
import org.b3log.latke.event.Event;
import org.b3log.latke.event.EventException;
import org.b3log.latke.event.EventManager;
import org.b3log.latke.model.Pagination;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.DoNothingRenderer;
import org.b3log.latke.servlet.renderer.JSONRenderer;
import org.b3log.latke.util.Requests;
import org.b3log.latke.util.Strings;
import org.b3log.rhythm.event.EventTypes;
import org.b3log.rhythm.model.Article;
import static org.b3log.rhythm.model.Article.*;
import org.b3log.rhythm.model.Blog;
import org.b3log.rhythm.model.Common;
import org.b3log.rhythm.model.Tag;
import org.b3log.rhythm.repository.ArticleRepository;
import org.b3log.rhythm.repository.TagArticleRepository;
import org.b3log.rhythm.repository.TagRepository;
import org.b3log.rhythm.repository.impl.ArticleRepositoryImpl;
import org.b3log.rhythm.repository.impl.TagArticleRepositoryImpl;
import org.b3log.rhythm.repository.impl.TagRepositoryImpl;
import org.b3log.rhythm.service.ArticleService;
import org.b3log.rhythm.util.Rhythms;
import org.b3log.rhythm.util.Securities;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Article processor.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.9, Aug 10, 2012
 * @since 0.1.4
 */
@RequestProcessor
public final class ArticleProcessor {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ArticleProcessor.class.getName());
    /**
     * Article service.
     */
    private ArticleService articleService = ArticleService.getInstance();
    /**
     * Article repository.
     */
    private ArticleRepository articleRepository = ArticleRepositoryImpl.getInstance();
    /**
     * Tag repository.
     */
    private TagRepository tagRepository = TagRepositoryImpl.getInstance();
    /**
     * Tag-Article repository.
     */
    private TagArticleRepository tagArticleRepository = TagArticleRepositoryImpl.getInstance();
    /**
     * Cache.
     */
    @SuppressWarnings("unchecked")
    private Cache<String, Serializable> cache = (Cache<String, Serializable>) CacheFactory.getCache("RhythmCache");
    /**
     * Event manager.
     */
    private EventManager eventManager = EventManager.getInstance();

    /**
     * Index.
     * 
     * @param context the specified context
     * @throws IOException io exception 
     */
    @RequestProcessing(value = "/", method = HTTPRequestMethod.GET)
    public void index(final HTTPRequestContext context) throws IOException {
        context.getResponse().sendRedirect("http://www.b3log.org");

        context.setRenderer(new DoNothingRenderer());
    }

    /**
     * Adds article.
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
     * @param context the specified context, 
     * including a request json object, for example,
     * <pre>
     * {
     *     "article": {
     *         "oId": "", 
     *         "articleTitle": "",
     *         "articlePermalink": "",
     *         "articleTags": "tag1, tag2, ....",
     *         "articleAuthorEmail": "",
     *         "articleContent": "",
     *         "articleCreateDate": long,
     *         "postToCommunity": boolean
     *     },
     *     "blogTitle": "",
     *     "blogHost": "", // clientHost
     *     "blogVersion": "", // clientVersion
     *     "blog": "", // clientName
     *     "userB3Key": ""
     *     "clientRuntimeEnv": "",
     *     "clientAdminEmail": ""
     * }
     * </pre>
     */
    @RequestProcessing(value = "/add-article.do", method = HTTPRequestMethod.POST)
    public void addArticle(final HTTPRequestContext context) {
        final HttpServletRequest request = context.getRequest();
        final HttpServletResponse response = context.getResponse();

        final JSONObject jsonObject = new JSONObject();

        final JSONRenderer renderer = new JSONRenderer();
        context.setRenderer(renderer);
        renderer.setJSONObject(jsonObject);

        try {
            final JSONObject requestJSONObject = Requests.parseRequestJSONObject(request, response);

            LOGGER.log(Level.FINEST, "Request[data={0}]", requestJSONObject);
            final String blog = requestJSONObject.optString(Blog.BLOG);
            if (!"B3log Solo".equals(blog)) {
                jsonObject.put(Keys.STATUS_CODE, "Unsupported Client");

                return;
            }

            final String blogHost = requestJSONObject.getString(Blog.BLOG_HOST);
            final String blogVersion = requestJSONObject.optString(Blog.BLOG_VERSION);

            if (!Rhythms.RELEASED_SOLO_VERSIONS.contains(blogVersion) && !Rhythms.SNAPSHOT_SOLO_VERSION.equals(blogVersion)) {
                LOGGER.log(Level.WARNING, "Version of Solo[host={0}] is [{1}], so ignored this request",
                        new String[]{blogHost, blogVersion});
                jsonObject.put(Keys.STATUS_CODE, StatusCodes.IGNORE_REQUEST);

                return;
            }

            final JSONObject originalArticle = requestJSONObject.getJSONObject(ARTICLE);
            securityProcess(originalArticle);

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
                LOGGER.log(Level.SEVERE, e.getMessage(), e);
                return;
            }

            latestPostTime = currentPostTime;

            final String blogTitle = requestJSONObject.getString(Blog.BLOG_TITLE);

            final JSONObject article = new JSONObject();

            final String id = originalArticle.getString(Keys.OBJECT_ID);
            article.put(ARTICLE_ORIGINAL_ID, id);
            article.put(ARTICLE_TITLE, originalArticle.getString(ARTICLE_TITLE));

            article.put(ARTICLE_AUTHOR_EMAIL, authorEmail);
            final String tagString = originalArticle.getString(ARTICLE_TAGS_REF);
            article.put(ARTICLE_TAGS_REF, tagString);
            final String permalink = "http://" + blogHost + originalArticle.getString(ARTICLE_PERMALINK);

            article.put(ARTICLE_PERMALINK, permalink);
            article.put(Blog.BLOG_HOST, blogHost);
            article.put(Blog.BLOG, blog);
            article.put(Blog.BLOG_VERSION, blogVersion);
            article.put(Blog.BLOG_TITLE, blogTitle);

            article.put(Article.ARTICLE_ACCESSIBILITY_CHECK_CNT, 0);
            article.put(Article.ARTICLE_ACCESSIBILITY_NOT_200_CNT, 0);

            articleService.addArticle(article);

            if (originalArticle.optBoolean(Common.POST_TO_COMMUNITY, true)) {
                try {
                    originalArticle.remove(Common.POST_TO_COMMUNITY);

                    eventManager.fireEventSynchronously(new Event<JSONObject>(EventTypes.ADD_ARTICLE_TO_SYMPHONY, requestJSONObject));
                } catch (final EventException e) {
                    LOGGER.log(Level.SEVERE, e.getMessage(), e);
                }
            }

            jsonObject.put(Keys.STATUS_CODE, StatusCodes.ADD_ARTICLE_SUCC);

            cache.put(authorEmail + ".lastPostTime", latestPostTime);
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Can not add article", e);

            try {
                context.getResponse().sendError(
                        HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Gets articles by tags.
     * 
     * @param context the specified context
     * @throws IOException io exception 
     */
    @RequestProcessing(value = "/get-articles-by-tags.do", method = HTTPRequestMethod.GET)
    public void getArticlesByTags(final HTTPRequestContext context) throws IOException {
        final HttpServletRequest request = context.getRequest();
        final String tagParam = request.getParameter(Tag.TAGS);
        if (Strings.isEmptyOrNull(tagParam)) {
            context.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        final String tagString = tagParam.toLowerCase();
        String soloHost = request.getParameter(Blog.BLOG_HOST);
        if (Strings.isEmptyOrNull(soloHost)) {
            context.getResponse().sendError(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        soloHost = soloHost.split(":")[0];
        final int pageSize = Integer.valueOf(request.getParameter(Pagination.PAGINATION_PAGE_SIZE));
        String callbackFuncName = request.getParameter("callback");
        if (Strings.isEmptyOrNull(callbackFuncName)) {
            callbackFuncName = "callback";
        }

        final JSONObject jsonObject = new JSONObject();

        final JSONRenderer renderer = new JSONRenderer();
        renderer.setCallback(callbackFuncName); // Sets JSONP
        context.setRenderer(renderer);
        renderer.setJSONObject(jsonObject);

        jsonObject.put(Keys.STATUS_CODE, StatusCodes.GET_ARTICLES_SUCC);

        LOGGER.log(Level.INFO, "Getting articles by tags[{0}]....", tagString);
        try {
            final String[] tags = tagString.split(",");

            final List<JSONObject> articles = new ArrayList<JSONObject>();
            for (int i = 0; i < tags.length; i++) {
                final String tagTitle = tags[i];
                final JSONObject tag = tagRepository.getByTitle(tagTitle);

                if (null != tag) {
                    LOGGER.log(Level.FINER, "Tag Title[{0}]", tag.getString(Tag.TAG_TITLE_LOWER_CASE));

                    final String tagId = tag.getString(Keys.OBJECT_ID);
                    final JSONObject result = tagArticleRepository.getByTagId(tagId, 1, pageSize);
                    final JSONArray tagArticleRelations = result.getJSONArray(Keys.RESULTS);
                    final int relationSize = pageSize < tagArticleRelations.length() ? pageSize : tagArticleRelations.length();
                    LOGGER.log(Level.FINEST, "Relation size[{0}]", relationSize);

                    for (int j = 0; j < relationSize; j++) {
                        final JSONObject tagArticleRelation = tagArticleRelations.getJSONObject(j);
                        LOGGER.log(Level.FINEST, "Relation[{0}]", tagArticleRelation.toString());
                        final String relatedArticleId = tagArticleRelation.getString(Article.ARTICLE + "_" + Keys.OBJECT_ID);
                        final JSONObject article = articleRepository.get(relatedArticleId);
                        if (article.getString(Blog.BLOG_HOST).split(":")[0].equalsIgnoreCase(soloHost)) {
                            continue; // Excludes articles from requested host
                        }

                        boolean existed = false;
                        for (final JSONObject relevantArticle : articles) {
                            if (relevantArticle.getString(Keys.OBJECT_ID).equals(article.getString(Keys.OBJECT_ID))) {
                                existed = true;
                            }
                        }

                        if (!existed) {
                            articles.add(article);
                        }

                        if (articles.size() == pageSize) {
                            break; // Got enough
                        }
                    }
                }

                if (articles.size() == pageSize) {
                    break; // Got enough
                }
            }

            removeUnusedProperties(articles);

            jsonObject.put(Article.ARTICLES, articles);

            jsonObject.put(Keys.STATUS_CODE, StatusCodes.GET_ARTICLES_SUCC);

            LOGGER.log(Level.FINE, "Got articles[{0}] by tag[{1}]", new Object[]{articles, tagString});
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Can not get articles", e);

            try {
                context.getResponse().sendError(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
            } catch (final IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Removes unused properties of each article in the specified articles.
     * 
     * <p>
     * Remains the following properties:
     * <ul>
     *   <li>{@link Article#ARTICLE_TITLE article title}</li>
     *   <li>{@link Article#ARTICLE_PERMALINK article permalink}</li>
     * </ul>
     * </p>
     * 
     * @param articles the specified articles
     */
    private void removeUnusedProperties(final List<JSONObject> articles) {
        for (final JSONObject article : articles) {
            article.remove(Keys.OBJECT_ID);
            article.remove(ARTICLE_ORIGINAL_ID);
            article.remove(ARTICLE_AUTHOR_EMAIL);
            article.remove(ARTICLE_TAGS_REF);
            article.remove(Blog.BLOG_HOST);
            article.remove(Blog.BLOG_TITLE);
            article.remove(Blog.BLOG_VERSION);
            article.remove(Blog.BLOG);
        }
    }

    /**
     * Security process.
     * 
     * @param article the specified article
     * @throws JSONException json exception
     */
    private void securityProcess(final JSONObject article) throws JSONException {
        String content = article.getString(ARTICLE_CONTENT);
        content = Securities.securedHTML(content);
        article.put(ARTICLE_CONTENT, content);

        String title = article.getString(ARTICLE_TITLE);
        title = Securities.securedHTML(title);
        article.put(ARTICLE_TITLE, title);

        String tagString = article.getString(ARTICLE_TAGS_REF);
        tagString = Securities.securedHTML(tagString);
        article.put(ARTICLE_TAGS_REF, tagString);
    }
}
