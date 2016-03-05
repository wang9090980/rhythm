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
package org.b3log.rhythm.model;

/**
 * This class defines all article model relevant keys.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.1.0.5, Mar 5, 2016
 * @since 0.1.4
 */
public final class Article {

    /**
     * Article.
     */
    public static final String ARTICLE = "article";
    /**
     * Articles.
     */
    public static final String ARTICLES = "articles";
    /**
     * Key of title.
     */
    public static final String ARTICLE_TITLE = "articleTitle";
    /**
     * Key of tags.
     */
    public static final String ARTICLE_TAGS_REF = "articleTags";
    /**
     * Key of permalink.
     */
    public static final String ARTICLE_PERMALINK = "articlePermalink";
    /**
     * Key of author email.
     */
    public static final String ARTICLE_AUTHOR_EMAIL = "articleAuthorEmail";
    /**
     * Key original article id.
     */
    public static final String ARTICLE_ORIGINAL_ID = "articleOriginalId";
    /**
     * Key of accessibility check count.
     */
    public static final String ARTICLE_ACCESSIBILITY_CHECK_CNT = "articleAccessibilityCheckCnt";
    /**
     * Key of accessibility check not HTTP 200 count.
     */
    public static final String ARTICLE_ACCESSIBILITY_NOT_200_CNT = "articleAccessibilityNot200Cnt";
    //// Transient ////
    /**
     * Key of article content.
     */
    public static final String ARTICLE_CONTENT = "articleContent";

    /**
     * Key of article create date.
     */
    public static final String ARTICLE_CREATE_DATE = "articleCreateDate";

    /**
     * Private default constructor.
     */
    private Article() {
    }
}
