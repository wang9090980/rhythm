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
package org.b3log.rhythm.util;

import java.net.URL;
import org.apache.commons.validator.routines.InetAddressValidator;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.util.Strings;
import static org.b3log.rhythm.model.Article.ARTICLE_TAGS_REF;
import static org.b3log.rhythm.model.Article.ARTICLE_TITLE;
import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.safety.Whitelist;
import org.jsoup.select.Elements;

/**
 * Security utilities.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @author <a href="mailto:echowdx@gmail.com">Dongxu Wang</a>
 * @version 1.0.0.3, Feb 25, 2016
 * @since 0.1.6
 */
public final class Securities {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(Securities.class.getName());
    /**
     * IP address validator.
     */
    private static final InetAddressValidator ADDRESS_VALIDATOR = InetAddressValidator.getInstance();

    /**
     * Security processing for the specified HTML content.
     *
     * <p>
     * <ul>
     * <li>Removes all event properties (onclick, onblur, etc.) in a tag, for example,
     * <pre>&lt;a href='http://google.com' onclick='xxxx'&gt;a link&lt;/a&gt;</pre> produce to
     * <pre>&lt;a href='http://google.com'&gt;a link&lt;/a&gt;</pre></li>
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
                "", Whitelist.relaxed().
                addAttributes(":all", "id", "target", "class", "style").
                addTags("span", "hr").
                addAttributes("iframe", "src", "width", "height"), outputSettings);
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
     * Security process for the specified article.
     *
     * @param article the specified article
     * @throws JSONException json exception
     */
    public static void securityProcess(final JSONObject article) throws JSONException {
        //String content = article.getString(ARTICLE_CONTENT);
        //content = Securities.securedHTML(content);
        //article.put(ARTICLE_CONTENT, content);

        String title = article.getString(ARTICLE_TITLE);
        title = securedHTML(title);
        article.put(ARTICLE_TITLE, title);

        String tagString = article.getString(ARTICLE_TAGS_REF);
        tagString = securedHTML(tagString);
        article.put(ARTICLE_TAGS_REF, tagString);
    }

    /**
     * Checks the specified host is valid.
     *
     * @param host the specified host
     * @return {@code true} if valid, returns {@code false} otherwise
     */
    public static boolean validHost(final String host) {
        if (!Strings.isURL(host)) {
            return false;
        }

        try {
            final URL url = new URL(host);
            final String hostPart = url.getHost();

            if (ADDRESS_VALIDATOR.isValid(hostPart)) {
                // not allow IP address

                LOGGER.warn("Invalid host [" + host + "]");

                return false;
            }

            if ("localhost".equals(hostPart)) {
                // not allow localhost

                LOGGER.warn("Invalid host [" + host + "]");

                return false;
            }
        } catch (final Exception e) {
            return false;
        }

        return true;
    }

    /**
     * Checks the specified title is valid.
     *
     * @param title the specified title
     * @return {@code true} if valid, returns {@code false} otherwise
     */
    public static boolean validTitle(final String title) {
        if (Strings.isEmptyOrNull(title)) {
            return false;
        }

        if ("Solo 示例".equals(title)) {
            return false;
        }

        return true;
    }

    /**
     * Private securities.
     */
    private Securities() {
    }
}
