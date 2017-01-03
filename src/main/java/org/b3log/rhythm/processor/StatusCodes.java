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
package org.b3log.rhythm.processor;

/**
 * This enumeration defines all response status codes of actions.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Oct 18, 2010
 * @since 0.1.4
 */
public enum StatusCodes {

    /**
     * Indicates add an article successfully.
     */
    ADD_ARTICLE_SUCC,
    /**
     * Indicates get articles successfully.
     */
    GET_ARTICLES_SUCC,
    /**
     * Indicates ignore an request.
     */
    IGNORE_REQUEST,
}
