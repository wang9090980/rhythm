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

import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.urlfetch.HTTPHeader;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.HTTPResponse;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.latke.util.Strings;
import org.b3log.rhythm.util.Rhythms;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * DNSPod service.
 * 
 * <p>
 * See <a href="https://www.dnspod.cn/client/user_api_doc.pdf">DNSPod API</a> for more details.
 * </p>
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.1, Sep 4, 2012
 * @since 0.1.6
 */
public final class DNSPodService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(DNSPodService.class.getName());
    /**
     * URL fetch service.
     */
    private static final URLFetchService URL_FETCH_SVC = URLFetchServiceFactory.getURLFetchService();
    /**
     * DNSPod API URL.
     */
    private static final String DNSPOD_API = "https://dnsapi.cn";
    /**
     * b3log.org domain id.
     */
    private static final String B3LOGORG_DOMAIN_ID = "612290";
    /**
     * Exclusions of records.
     */
    private static final Set<String> EXCLUDES = new HashSet<String>();

    static {
        EXCLUDES.add("@");
        EXCLUDES.add("ghs");
    }

    /**
     * Gets sub-domains of b3log.org.
     * 
     * @return a set of sub-domains, returns an empty set if not found or error
     */
    public Set<String> getSubdomains() {
        final Set<String> ret = new HashSet<String>();

        try {
            final HTTPRequest httpRequest = new HTTPRequest();
            httpRequest.setRequestMethod(HTTPRequestMethod.POST);
            httpRequest.setURL(new URL(DNSPOD_API + "/Record.List"));
            httpRequest.addHeader(new HTTPHeader("User-Agent", "B3log Rhythm/0.1.6 (DL88250gmail.com)"));
            
            httpRequest.addPayloadEntry("login_email", Rhythms.CFG.getString("dnspod.username"));
            httpRequest.addPayloadEntry("login_password", Rhythms.CFG.getString("dnspod.password"));
            httpRequest.addPayloadEntry("format", "json");
            httpRequest.addPayloadEntry("lang", "cn");
            httpRequest.addPayloadEntry("error_on_empty", "no");
            httpRequest.addPayloadEntry("domain_id", B3LOGORG_DOMAIN_ID);
            httpRequest.addPayloadEntry("offset", "0");
            httpRequest.addPayloadEntry("length", "3000");
            
            final HTTPResponse response = URL_FETCH_SVC.fetch(httpRequest);

            final JSONObject content = new JSONObject(new String(response.getContent()));

            LOGGER.log(Level.INFO, "Response[sc={0}, content={1}]",
                       new Object[]{response.getResponseCode(), content.toString(Rhythms.INDENT_FACTOR)});

            final JSONArray records = content.getJSONArray("records");

            final StringBuilder domainsBuilder = new StringBuilder("Sub-domains[").append(Strings.LINE_SEPARATOR);

            for (int i = 0; i < records.length(); i++) {
                final JSONObject record = records.getJSONObject(i);

                final String name = record.getString("name");
                if (EXCLUDES.contains(name)) {
                    LOGGER.log(Level.FINE, "Skips record[name={0}]", name);
                    continue;
                }

                final String subdomain = name + ".b3log.org";

                domainsBuilder.append("    ").append(subdomain);

                if (i < records.length() - 1) {
                    domainsBuilder.append(Strings.LINE_SEPARATOR);
                }

                ret.add(subdomain);
            }

            domainsBuilder.append(Strings.LINE_SEPARATOR).append("]");

            LOGGER.log(Level.FINEST, domainsBuilder.toString());
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Gets sub-domains failed", e);
        }

        LOGGER.log(Level.INFO, "Got [{0}] sub-domains", ret.size());

        return ret;
    }

    /**
     * Gets the {@link DNSPodService} singleton.
     *
     * @return the singleton
     */
    public static DNSPodService getInstance() {
        return DNSPodService.SingletonHolder.SINGLETON;
    }

    /**
     * Private constructor.
     */
    private DNSPodService() {
    }

    /**
     * Singleton holder.
     *
     * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
     * @version 1.0.0.0, May 18, 2012
     */
    private static final class SingletonHolder {

        /**
         * Singleton.
         */
        private static final DNSPodService SINGLETON = new DNSPodService();

        /**
         * Private default constructor.
         */
        private SingletonHolder() {
        }
    }
}
