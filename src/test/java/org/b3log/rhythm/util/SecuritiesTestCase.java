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

import junit.framework.Assert;
import org.testng.annotations.Test;

/**
 * Security utilities test case.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @author <a href="mailto:echowdx@gmail.com">Dongxu Wang</a>
 * @version 1.1.0.1, Apr 8, 2015
 * @since 0.1.6
 */
public class SecuritiesTestCase {

    /**
     * Tests {@link Securities#securedHTML(java.lang.String)} for event properties processing.
     */
    @Test
    public void securedHTML() {
        final String html = "<a href='google.com' onclick='test'>a link</a><script>alert(1);</script><p>test";

        final String securedHTML = Securities.securedHTML(html);

        Assert.assertFalse(securedHTML.contains("onclick"));
        Assert.assertFalse(securedHTML.contains("<script>"));
        Assert.assertTrue(securedHTML.contains("</p>"));
    }

    /**
     * Tests {@link Securities#securedHTML(java.lang.String)} for {@code iframe} processing.
     */
    @Test
    public void securedHTMLIFrame() {
        // secured 
        final String html = "<iframe style=\"border:1px solid\" "
                      + "src=\"https://wide.b3log.org/playground/8b7cc38b4c12e6fde5c4d15a4f2f32e5.go?embed=true\" "
                      + "width=\"100%\" height=\"600\"></iframe>";

        final String securedHTML = Securities.securedHTML(html);

        Assert.assertEquals(html, securedHTML);

        // insecured
        
        final String securedPart = "<iframe style=\"border:1px solid\" "
                      + "src=\"https://wide.b3log.org/playground/8b7cc38b4c12e6fde5c4d15a4f2f32e5.go?embed=true\" "
                      + "width=\"100%\" height=\"600\"></iframe>";
        final String inscuredPart = "<iframe style=\"border:1px solid\" src=\"https://insecured.com\"</iframe>";

        final String filtered = Securities.securedHTML(securedPart + inscuredPart);

        Assert.assertEquals(securedHTML, filtered);
    }
}
