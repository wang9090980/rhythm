/*
 * Copyright (c) 2010-2015, b3log.org
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

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

/**
 * Security utilities.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @author <a href="mailto:echowdx@gmail.com">Dongxu Wang</a>
 * @version 1.0.0.2, Feb 27, 2014
 * @since 0.1.6
 */
public final class Securities {

    /**
     * Security processing for the specified HTML content.
     *
     * <p>
     * <ul>
     * <li>Removes all event properties (onclick, onblur, etc.) in a tag, for example,
     * <pre>&lt;a href='google.com' onclick='xxxx'&gt;a link&lt;/a&gt;</pre> produce to
     * <pre>&lt;a href='google.com'&gt;a link&lt;/a&gt;</pre></li>
     * <li>Escapes
     * <pre>&lt;script&gt;&lt;/script&gt;</pre></li>
     * <li>Matches the tag start and end, for example,
     * <pre>&lt;div&gt;content</pre> produce to
     * <pre>&lt;div&gt;content&lt;/div&gt;</pre></li>
     * </ul>
     * </p>
     *
     * @param html the specified HTML content
     * @return secured HTML content
     */
    public static String securedHTML(final String html) {
        final Document.OutputSettings outputSettings = new Document.OutputSettings();
        outputSettings.prettyPrint(false);

        final String tmp = Jsoup.clean(html.replace("<script>", "&lt;script&gt;").replace("</script>", "&lt;/script&gt;"),
                                       "", Whitelist.relaxed().addAttributes(":all", "id", "target", "class", "style").addTags("span").
                                       addTags("hr").addTags("iframe").addAttributes("iframe", "src", "width", "height"),
                                       outputSettings);
        final Document doc = Jsoup.parse(tmp, "", Parser.xmlParser());
        final Elements iframes = doc.getElementsByTag("iframe");

        for (final Element iframe : iframes) {
            final String src = iframe.attr("src");
            if (!src.startsWith("https://wide.b3log.org")) {
                iframe.remove();
            }
        }

        return doc.html();
    }

    /**
     * Private securities.
     */
    private Securities() {
    }
}
