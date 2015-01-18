/*
 * Copyright (c) 2015, b3log.org
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
import java.util.Set;
import javax.inject.Inject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.mail.MailService;
import org.b3log.latke.mail.MailService.Message;
import org.b3log.latke.mail.MailServiceFactory;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.servlet.renderer.DoNothingRenderer;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.HTTPResponse;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.rhythm.service.DNSPodService;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Sub-domains (*.b3log.org) monitor.
 * 
 * <p>
 * Checks with <a href="https://www.dnspod.cn/client/user_api_doc.pdf">DNSPod API</a>.
 * </p>
 * 
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.2, Aug 24, 2013
 * @since 0.1.6
 */
@RequestProcessor
public class SubdomainMonitor {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(SubdomainMonitor.class.getName());
    /**
     * DNSPod service.
     */
    @Inject
    private DNSPodService dnsPodService;
    /**
     * URL fetch service.
     */
    private URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    /**
     * Ping all sub-domains.
     * 
     * @param context the specified context
     * @param request the specified request
     * @throws Exception exception 
     */
    @RequestProcessing(value = "/dns/monitor", method = HTTPRequestMethod.GET)
    public void ping(final HTTPRequestContext context, final HttpServletRequest request) throws Exception {
        final Set<String> subdomains = dnsPodService.getSubdomains();

        final JSONArray errorSCSubdomains = new JSONArray(); // HTTP SC 404, 500, (^200)

        for (final String subdomain : subdomains) {
            String url = null;

            try {
                final HTTPRequest pingRequest = new HTTPRequest();
                url = "http://" + subdomain;

                pingRequest.setURL(new URL(url));

                final HTTPResponse response = urlFetchService.fetch(pingRequest);
                final int sc = response.getResponseCode();

                LOGGER.log(Level.INFO, "Ping URL[{0}, sc={1}]", new Object[]{url, sc});

                if (HttpServletResponse.SC_OK != sc) {
                    final JSONObject errorURL = new JSONObject();
                    errorURL.put("URL", url);
                    errorURL.put("SC", sc);
                    errorSCSubdomains.put(errorURL);
                }

            } catch (final Exception e) {
                LOGGER.log(Level.ERROR, "Ping URL[" + url + "] failed", e);
            }
        }

        if (errorSCSubdomains.length() > 0) {
            // Sends admin an email SC ^200
            final MailService mailService = MailServiceFactory.getMailService();
            final Message message = new MailService.Message();
            message.addRecipient("DL88250@gmail.com");
            message.setFrom("DL88250@gmail.com");
            message.setSubject("B3log.org Sub-domains Monitor Report");

            final StringBuilder contentBuilder = new StringBuilder();
            contentBuilder.append("<h2>Ping Failed URLs: </h2>");
            for (int i = 0; i < errorSCSubdomains.length(); i++) {
                final JSONObject errorURL = errorSCSubdomains.getJSONObject(i);
                contentBuilder.append(errorURL.getString("URL")).append("    ").append(errorURL.getInt("SC")).append("<br/>");
            }
            message.setHtmlBody(contentBuilder.toString());

            mailService.send(message);
        }

        final DoNothingRenderer renderer = new DoNothingRenderer();
        context.setRenderer(renderer);
    }
}
