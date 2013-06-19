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
package org.b3log.rhythm.repository.impl;

import org.b3log.latke.repository.AbstractRepository;
import org.b3log.rhythm.model.Article;
import org.b3log.rhythm.repository.ArticleRepository;

/**
 * Article repository implementation.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.2, Jan 23, 2011
 * @since 0.1.4
 */
public final class ArticleRepositoryImpl extends AbstractRepository implements ArticleRepository {

    /**
     * Gets the {@link ArticleRepositoryImpl} singleton.
     *
     * @return the singleton
     */
    public static ArticleRepositoryImpl getInstance() {
        return SingletonHolder.SINGLETON;
    }

    /**
     * Private constructor.
     * 
     * @param name the specified name
     */
    private ArticleRepositoryImpl(final String name) {
        super(name);
    }

    /**
     * Singleton holder.
     *
     * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
     * @version 1.0.0.0, Jan 23, 2011
     */
    private static final class SingletonHolder {

        /**
         * Singleton.
         */
        private static final ArticleRepositoryImpl SINGLETON = new ArticleRepositoryImpl(Article.ARTICLE);

        /**
         * Private default constructor.
         */
        private SingletonHolder() {
        }
    }
}
