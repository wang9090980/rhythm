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
package org.b3log.rhythm.util;

/**
 * Security utilities.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @author <a href="mailto:echowdx@gmail.com">Dongxu Wang</a>
 * @version 1.0.0.0, Sep 26, 2012
 * @since 0.1.6
 */
public final class Securities {

    /**
     * Security processing for the specified HTML content.
     * 
     * <p>
     *   <ul>
     *     <li>Removes all event properties (onclick, onblur, etc.) in a tag, for example,  
     *     <pre>&lt;a href='google.com' onclick='xxxx'&gt;a link&lt;/a&gt;</pre> produce to
     *     <pre>&lt;a href='google.com'&gt;a link&lt;/a&gt;</pre></li>
     *     <li>Removes all <pre>&lt;script&gt;&lt;/script&gt;</pre> tag regions</li>
     *     <li>Matches the tag start and end, for example, 
     *     <pre>&lt;div&gt;content</pre> produce to
     *     <pre>&lt;div&gt;content&lt;/div&gt;</pre></li>
     *   </ul>
     * </p>
     * 
     * @param html the specified HTML content
     * @return secured HTML content 
     */
    public static String securedHTML(final String html) {
        // TODO: DX
        
    	return html.replace("onclick", "");
    }

    /**
     * Private securities.
     */
    private Securities() {
    }
}
