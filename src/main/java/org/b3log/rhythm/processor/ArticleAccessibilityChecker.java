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

import java.net.URL;
import java.util.List;
import java.util.Set;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.DoNothingRenderer;
import org.b3log.latke.urlfetch.HTTPHeader;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.HTTPResponse;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.latke.util.Stopwatchs;
import org.b3log.rhythm.model.Article;
import org.b3log.rhythm.service.ArticleService;
import org.b3log.rhythm.util.Rhythms;
import org.json.JSONObject;

/**
 * Checks and removes whether the articles indexed by Rhythm can accessibility (HTTP status code 200) with the 
 * permalink of an article.
 * 
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.6, Dec 26, 2012
 * @since 0.1.5
 */
@RequestProcessor
public final class ArticleAccessibilityChecker {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(ArticleAccessibilityChecker.class.getName());
    /**
     * Article service.
     */
    private ArticleService articleService = ArticleService.getInstance();
    /**
     * URL fetch service.
     */
    private URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();
    /**
     * Check count.
     */
    private static final int CHECK_CNT = 10;
    /**
     * Threshold of not 200.
     */
    private static final int NOT_200_THRESHOLD = 20;

    /**
     * Checks and saves the check results.
     * 
     * @param context the specified context
     * @throws Exception exception 
     */
    @RequestProcessing(value = "/articles/accessibility", method = HTTPRequestMethod.GET)
    public void checkAccessibility(final HTTPRequestContext context) throws Exception {
        final List<JSONObject> articles = articleService.getArticlesRandomly(CHECK_CNT);

        for (final JSONObject article : articles) {
            final String articlePermalink = article.getString(Article.ARTICLE_PERMALINK);

            Stopwatchs.start("Checks Article[permalink=" + articlePermalink + "] Accessibility");

            try {
                final HTTPRequest request = new HTTPRequest();
                request.addHeader(new HTTPHeader("User-Agent", "B3log Rhythm/" + Rhythms.RHYTHM_VERSION));
                request.setURL(new URL(articlePermalink));

                final HTTPResponse response = urlFetchService.fetch(request);

                final int responseCode = response.getResponseCode();
                LOGGER.log(Level.INFO, "Accesses article[permalink=" + articlePermalink + "] response[code={0}]", responseCode);

                articleService.updateAccessibility(article, responseCode);
            } catch (final Exception e) {
                LOGGER.warn(e.getMessage());
            } finally {
                Stopwatchs.end();
            }
        }

        final DoNothingRenderer renderer = new DoNothingRenderer();
        context.setRenderer(renderer);
    }

    /**
     * Removes articles if they are not found.
     * 
     * <p>
     * A 'not found' article is that {@link Article#ARTICLE_ACCESSIBILITY_NOT_200_CNT not 200 count} greater than
     * {@value #NOT_200_THRESHOLD}.
     * </p>
     * 
     * @param context the specified context
     * @throws Exception exception 
     */
    @RequestProcessing(value = "/articles/accessibility/remove", method = HTTPRequestMethod.GET)
    public void removeNotFoundArticles(final HTTPRequestContext context) throws Exception {
        final Set<String> articleIds = articleService.getArticleIdsByAccessibilityCheckCnt('>', NOT_200_THRESHOLD);

        for (final String articleId : articleIds) {
            ArticleService.getInstance().removeArticle(articleId);
        }

        final DoNothingRenderer renderer = new DoNothingRenderer();
        context.setRenderer(renderer);
    }
}
