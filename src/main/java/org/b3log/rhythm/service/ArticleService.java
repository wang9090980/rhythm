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
package org.b3log.rhythm.service;

import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.Collections;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletResponse;
import org.b3log.latke.Keys;
import org.b3log.latke.model.User;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.Transaction;
import org.b3log.latke.service.ServiceException;
import org.b3log.latke.util.Stopwatchs;
import org.b3log.rhythm.model.Article;
import org.b3log.rhythm.model.Blog;
import org.b3log.rhythm.model.Common;
import org.b3log.rhythm.repository.ArticleRepository;
import org.b3log.rhythm.repository.UserRepository;
import org.b3log.rhythm.repository.impl.ArticleRepositoryImpl;
import org.b3log.rhythm.repository.impl.UserRepositoryImpl;
import org.b3log.rhythm.util.ArticleUtils;
import org.b3log.rhythm.util.TagUtils;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Article service.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.4, Sep 26, 2012
 * @since 0.1.5
 */
@SuppressWarnings("unchecked")
public final class ArticleService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ArticleService.class.getName());

    /**
     * Article repository.
     */
    private ArticleRepository articleRepository = ArticleRepositoryImpl.getInstance();

    /**
     * User repository.
     */
    private UserRepository userRepository = UserRepositoryImpl.getInstance();

    /**
     * Tag utilities.
     */
    private TagUtils tagUtils = TagUtils.getInstance();

    /**
     * Article utilities.
     */
    private ArticleUtils articleUtils = ArticleUtils.getInstance();

    /**
     * Default article batch size.
     */
    private static final int BATCH_SIZE = 50;

    /**
     * Gets article randomly with the specified fetch size.
     * 
     * @param fetchSize the specified fetch size
     * @return a list of json objects, its size less or equal to the specified 
     * fetch size, returns an empty list if not found
     */
    public List<JSONObject> getArticlesRandomly(final int fetchSize) {
        Stopwatchs.start("Gets Articles Randomly");

        try {
            return articleRepository.getRandomly(fetchSize);
        } catch (final RepositoryException e) {
            LOGGER.log(Level.SEVERE, "Gets articles ranomly failed", e);

            return Collections.<JSONObject>emptyList();
        } finally {
            Stopwatchs.end();
        }
    }

    /**
     * Gets a list of articles specified by the given greater/less operation and the specified accessibility check count. 
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

            LOGGER.log(Level.FINER, "Article Ids[{0}]", ret.toString());
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Gets article ids by accessibility check count[greaterOrLess=" + greaterOrLess
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

            LOGGER.log(Level.SEVERE, "Updates accessibility of article[" + article.toString() + "] failed", e);
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
            tagUtils.decTagRefCount(articleId);
            articleUtils.removeTagArticleRelations(articleId);

            transaction.commit();
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            LOGGER.log(Level.SEVERE, "Removes an article[id=" + articleId + "] failed", e);
        }
    }

    /**
     * Adds the specified article.
     * 
     * @param article the specified article, for example,
     * <pre>
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

            final String[] tagTitles = article.getString(Article.ARTICLE_TAGS_REF).split(",");
            final JSONArray tags = tagUtils.tag(tagTitles, article);
            articleUtils.addTagArticleRelation(tags, article);

            updateRecentPostTime(article);

            transaction.commit();
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            LOGGER.log(Level.SEVERE, "Adds article[" + article.toString() + "] failed", e);
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
            LOGGER.log(Level.SEVERE, "Updates recent post time of failed", e);

            throw new ServiceException(e);
        }
    }

    /**
     * Gets the {@link ArticleService} singleton.
     *
     * @return the singleton
     */
    public static ArticleService getInstance() {
        return SingletonHolder.SINGLETON;
    }

    /**
     * Private constructor.
     */
    private ArticleService() {
    }

    /**
     * Singleton holder.
     *
     * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
     * @version 1.0.0.0, Nov 3, 2011
     */
    private static final class SingletonHolder {

        /**
         * Singleton.
         */
        private static final ArticleService SINGLETON =
                new ArticleService();

        /**
         * Private default constructor.
         */
        private SingletonHolder() {
        }
    }
}
