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
package org.b3log.rhythm.service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import javax.inject.Inject;
import javax.servlet.http.HttpServletResponse;
import org.b3log.latke.Keys;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.model.User;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.Transaction;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.service.annotation.Service;
import org.b3log.latke.util.Stopwatchs;
import org.b3log.rhythm.model.Article;
import org.b3log.rhythm.model.Blog;
import org.b3log.rhythm.model.Common;
import org.b3log.rhythm.model.Tag;
import org.b3log.rhythm.repository.ArticleRepository;
import org.b3log.rhythm.repository.TagArticleRepository;
import org.b3log.rhythm.repository.TagRepository;
import org.b3log.rhythm.repository.UserRepository;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Article service.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.6, Mar 12, 2016
 * @since 0.1.5
 */
@Service
public class ArticleService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ArticleService.class.getName());

    /**
     * Article repository.
     */
    @Inject
    private ArticleRepository articleRepository;

    /**
     * User repository.
     */
    @Inject
    private UserRepository userRepository;

    /**
     * Tag-Article repository.
     */
    @Inject
    private TagArticleRepository tagArticleRepository;

    /**
     * Tag repository.
     */
    @Inject
    private TagRepository tagRepository;

    /**
     * Default article batch size.
     */
    private static final int BATCH_SIZE = 50;

    /**
     * Tags the specified article with the specified tag titles.
     *
     * @param tagTitles the specified tag titles
     * @param article the specified article
     * @return an array of tags
     * @throws RepositoryException repository exception
     * @throws JSONException json exception
     */
    public JSONArray tag(final String[] tagTitles, final JSONObject article) throws RepositoryException, JSONException {
        final JSONArray ret = new JSONArray();
        for (int i = 0; i < tagTitles.length; i++) {
            final String tagTitle = tagTitles[i].trim();
            JSONObject tag = tagRepository.getByTitle(tagTitle);
            String tagId;
            if (null == tag) {
                LOGGER.log(Level.TRACE, "Found a new tag[title={0}] in article[title={1}]",
                        new Object[]{tagTitle, article.getString(Article.ARTICLE_TITLE)});
                tag = new JSONObject();
                tag.put(Tag.TAG_TITLE_LOWER_CASE, tagTitle.toLowerCase());
                tag.put(Tag.TAG_REFERENCE_COUNT, 1);

                tagId = tagRepository.add(tag);
                tag.put(Keys.OBJECT_ID, tagId);
            } else {
                tagId = tag.getString(Keys.OBJECT_ID);
                LOGGER.log(Level.TRACE, "Found a existing tag[title={0}, oId={1}] in article[title={2}]",
                        new Object[]{tag.getString(Tag.TAG_TITLE_LOWER_CASE), tag.getString(Keys.OBJECT_ID),
                            article.getString(Article.ARTICLE_TITLE)});
                final int refCnt = tag.getInt(Tag.TAG_REFERENCE_COUNT);
                final JSONObject tagTmp = new JSONObject(tag, JSONObject.getNames(tag));
                tagTmp.put(Tag.TAG_REFERENCE_COUNT, refCnt + 1);
                tagRepository.update(tagId, tagTmp);
            }

            ret.put(tag);
        }

        return ret;
    }

    /**
     * Decrements reference count of every tag of an article specified by the given article id.
     *
     * @param articleId the given article id
     * @throws JSONException json exception
     * @throws RepositoryException repository exception
     */
    public void decTagRefCount(final String articleId) throws JSONException, RepositoryException {
        final List<JSONObject> tags = tagRepository.getByArticleId(articleId);

        for (final JSONObject tag : tags) {
            final String tagId = tag.getString(Keys.OBJECT_ID);
            final int refCnt = tag.getInt(Tag.TAG_REFERENCE_COUNT);
            tag.put(Tag.TAG_REFERENCE_COUNT, refCnt - 1);

            tagRepository.update(tagId, tag);
            LOGGER.log(Level.TRACE, "Deced tag[tagTitle={0}] reference count[{1}] of article[oId={2}]",
                    new Object[]{tag.getString(Tag.TAG_TITLE_LOWER_CASE), tag.getInt(Tag.TAG_REFERENCE_COUNT), articleId});
        }

        LOGGER.log(Level.DEBUG, "Deced all tag reference count of article[oId={0}]", articleId);
    }

    /**
     * Removes tag-article relations by the specified article id.
     *
     * @param articleId the specified article id
     * @throws JSONException json exception
     * @throws RepositoryException repository exception
     */
    public void removeTagArticleRelations(final String articleId)
            throws JSONException, RepositoryException {
        final List<JSONObject> tagArticleRelations = tagArticleRepository.getByArticleId(articleId);
        for (int i = 0; i < tagArticleRelations.size(); i++) {
            final JSONObject tagArticleRelation = tagArticleRelations.get(i);
            final String relationId = tagArticleRelation.getString(Keys.OBJECT_ID);
            tagArticleRepository.remove(relationId);
        }
    }

    /**
     * Adds relation of the specified tags and article.
     *
     * @param tags the specified tags
     * @param article the specified article
     * @throws JSONException json exception
     * @throws RepositoryException repository exception
     */
    public void addTagArticleRelation(final JSONArray tags, final JSONObject article)
            throws JSONException, RepositoryException {
        for (int i = 0; i < tags.length(); i++) {
            final JSONObject tag = tags.getJSONObject(i);
            final JSONObject tagArticleRelation = new JSONObject();

            tagArticleRelation.put(Tag.TAG + "_" + Keys.OBJECT_ID, tag.getString(Keys.OBJECT_ID));
            tagArticleRelation.put(Article.ARTICLE + "_" + Keys.OBJECT_ID, article.getString(Keys.OBJECT_ID));

            tagArticleRepository.add(tagArticleRelation);
        }
    }

    /**
     * Gets articles randomly with the specified fetch size.
     *
     * @param fetchSize the specified fetch size
     * @return a list of json objects, its size less or equal to the specified fetch size, returns an empty list if not
     * found
     */
    public List<JSONObject> getArticlesRandomly(final int fetchSize) {
        Stopwatchs.start("Gets Articles Randomly");

        try {
            return articleRepository.getRandomly(fetchSize);
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets articles ranomly failed", e);

            return Collections.<JSONObject>emptyList();
        } finally {
            Stopwatchs.end();
        }
    }

    /**
     * Gets a list of articles specified by the given greater/less operation and the specified accessibility check
     * count.
     *
     * @param greaterOrLess the given greater/less operation, '>' or '<'
     * @param checkCnt the specified accessibility check count
     * @return a list of articles, returns an empty list if not found by the specified condition
     */
    public Set<String> getArticleIdsByAccessibilityCheckCnt(final char greaterOrLess, final int checkCnt) {
        final Set<String> ret = new HashSet<String>();

        final FilterOperator operator = ('>' == greaterOrLess) ? FilterOperator.GREATER_THAN : FilterOperator.LESS_THAN;

        final Query query = new Query().setFilter(
                new PropertyFilter(Article.ARTICLE_ACCESSIBILITY_NOT_200_CNT, operator, checkCnt)).
                setPageSize(BATCH_SIZE).setPageCount(1).
                addProjection(Keys.OBJECT_ID, String.class);

        try {
            final JSONObject result = articleRepository.get(query);
            final JSONArray articles = result.getJSONArray(Keys.RESULTS);

            for (int i = 0; i < articles.length(); i++) {
                ret.add(articles.getJSONObject(i).getString(Keys.OBJECT_ID));
            }

            LOGGER.log(Level.DEBUG, "Article Ids[{0}]", ret.toString());
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Gets article ids by accessibility check count[greaterOrLess=" + greaterOrLess
                    + ", checkCnt=" + checkCnt + "] failed", e);
        }

        return ret;
    }

    /**
     * Updates the accessibility of the specified article.
     *
     * @param article the specified article
     * @param statusCode the specified HTTP status code
     */
    public void updateAccessibility(final JSONObject article, final int statusCode) {
        if (null == article) {
            return;
        }

        final Transaction transaction = articleRepository.beginTransaction();

        try {
            final int checkCnt = article.optInt(Article.ARTICLE_ACCESSIBILITY_CHECK_CNT);
            article.put(Article.ARTICLE_ACCESSIBILITY_CHECK_CNT, checkCnt + 1);

            if (HttpServletResponse.SC_OK != statusCode) {
                final int not200Cnt = article.optInt(Article.ARTICLE_ACCESSIBILITY_NOT_200_CNT);
                article.put(Article.ARTICLE_ACCESSIBILITY_NOT_200_CNT, not200Cnt + 1);
            }

            articleRepository.update(article.getString(Keys.OBJECT_ID), article);

            transaction.commit();

            LOGGER.log(Level.INFO, "Updated accessibility of article[{0}]", article.toString());
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            LOGGER.log(Level.ERROR, "Updates accessibility of article[" + article.toString() + "] failed", e);
        }
    }

    /**
     * Removes an article specified by the given article id.
     *
     * @param articleId the given article id
     */
    public void removeArticle(final String articleId) {
        final Transaction transaction = articleRepository.beginTransaction();

        try {
            articleRepository.remove(articleId);
            decTagRefCount(articleId);
            removeTagArticleRelations(articleId);

            transaction.commit();
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            LOGGER.log(Level.ERROR, "Removes an article[id=" + articleId + "] failed", e);
        }
    }

    /**
     * Adds the specified article.
     *
     * @param article the specified article, for example,      <pre>
     * {
     *     "articleOriginalId": "",
     *     "articleTitle": "",
     *     "articleAuthorEmail": "",
     *     "articleTags": "",
     *     "articlePermalink": "",
     *     "blogHost": "",
     *     "blog": "",
     *     "blogVersion": "",
     *     "blogTitle": ""
     * }
     * </pre>
     */
    public void addArticle(final JSONObject article) {
        final Transaction transaction = articleRepository.beginTransaction();

        try {
            article.put(Article.ARTICLE_ACCESSIBILITY_CHECK_CNT, 0);
            article.put(Article.ARTICLE_ACCESSIBILITY_NOT_200_CNT, 0);

            articleRepository.add(article);

            String articleTags = article.getString(Article.ARTICLE_TAGS_REF);
            articleTags = Tag.formatTags(articleTags);
            final String[] tagTitles = articleTags.split(",");

            final JSONArray tags = tag(tagTitles, article);
            addTagArticleRelation(tags, article);

            updateRecentPostTime(article);

            transaction.commit();
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            LOGGER.log(Level.ERROR, "Adds article[" + article.toString() + "] failed", e);
        }
    }

    /**
     * Updates the specified article.
     *
     * @param article the specified article, for example,      <pre>
     * {
     *     "articleOriginalId": "",
     *     "articleTitle": "",
     *     "articleAuthorEmail": "",
     *     "articleTags": "",
     *     "articlePermalink": "",
     *     "blogHost": "",
     *     "blog": "",
     *     "blogVersion": "",
     *     "blogTitle": ""
     * }
     * </pre>
     */
    public void updateByOriginalId(final JSONObject article) {
        final Transaction transaction = articleRepository.beginTransaction();

        final String originalId = article.optString(Article.ARTICLE_ORIGINAL_ID);

        final Query query = new Query().setFilter(new PropertyFilter(Article.ARTICLE_ORIGINAL_ID, FilterOperator.EQUAL, originalId));

        try {
            final JSONObject result = articleRepository.get(query);
            final JSONArray array = result.optJSONArray(Keys.RESULTS);
            if (0 == array.length()) {
                LOGGER.log(Level.WARN, "Not found article by original id [{0}]", originalId);

                return;
            }

            final JSONObject old = array.getJSONObject(0);
            final String id = old.getString(Keys.OBJECT_ID);
            article.put(Keys.OBJECT_ID, id);

            article.put(Article.ARTICLE_ACCESSIBILITY_CHECK_CNT, old.getInt(Article.ARTICLE_ACCESSIBILITY_CHECK_CNT));
            article.put(Article.ARTICLE_ACCESSIBILITY_NOT_200_CNT, old.getInt(Article.ARTICLE_ACCESSIBILITY_NOT_200_CNT));

            articleRepository.update(id, article);

            transaction.commit();
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }
            LOGGER.log(Level.ERROR, "Updates article by original id [" + originalId + "] failed", e);
        }
    }

    /**
     * Updates the author's recent post time with the specified article.
     *
     * @param article the specified article
     * @throws ServiceException service exception
     */
    private void updateRecentPostTime(final JSONObject article) throws ServiceException {
        final long currentTimeMillis = System.currentTimeMillis();

        try {
            final String authorEmail = article.getString(Article.ARTICLE_AUTHOR_EMAIL);
            final String authorURL = article.getString(Blog.BLOG_HOST);

            JSONObject user = userRepository.getByEmail(authorEmail);
            if (null == user) {
                // This author is a new user
                user = new JSONObject();

                user.put(Keys.OBJECT_ID, String.valueOf(currentTimeMillis));
                user.put(User.USER_EMAIL, authorEmail);
                user.put(Common.RECENT_POST_TIME, currentTimeMillis);
                user.put(User.USER_URL, authorURL);

                userRepository.add(user);
            } else {
                user.put(Common.RECENT_POST_TIME, currentTimeMillis);
                user.put(User.USER_URL, authorURL);

                userRepository.update(user.getString(Keys.OBJECT_ID), user);
            }
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Updates recent post time of failed", e);

            throw new ServiceException(e);
        }
    }
}
