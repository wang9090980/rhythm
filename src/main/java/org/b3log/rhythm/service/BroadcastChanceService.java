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
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.b3log.latke.Keys;
import org.b3log.latke.model.User;
import org.b3log.latke.repository.FilterOperator;
import org.b3log.latke.repository.PropertyFilter;
import org.b3log.latke.repository.Query;
import org.b3log.latke.repository.Transaction;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.urlfetch.HTTPRequest;
import org.b3log.latke.urlfetch.URLFetchService;
import org.b3log.latke.urlfetch.URLFetchServiceFactory;
import org.b3log.latke.util.Strings;
import org.b3log.rhythm.model.BroadcastChance;
import org.b3log.rhythm.repository.BroadcastChanceRepository;
import org.b3log.rhythm.repository.UserRepository;
import org.b3log.rhythm.repository.impl.BroadcastChanceRepositoryImpl;
import org.b3log.rhythm.repository.impl.UserRepositoryImpl;
import org.json.JSONArray;
import org.json.JSONObject;

/**
 * Broadcast chance service.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.0, Apr 15, 2013
 * @since 0.1.6
 */
public final class BroadcastChanceService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(BroadcastChanceService.class.getName());

    /**
     * Broadcast chance repository.
     */
    private BroadcastChanceRepository broadcastChanceRepository = BroadcastChanceRepositoryImpl.getInstance();

    /**
     * User repository.
     */
    private UserRepository userRepository = UserRepositoryImpl.getInstance();

    /**
     * URL fetch service.
     */
    private URLFetchService urlFetchService = URLFetchServiceFactory.getURLFetchService();

    /**
     * Max size of active broadcast chances.
     */
    private static final int MAX_SIZE = 5;

    /**
     * Cycle time (active time).
     */
    private static final long CYCLE_TIME = 1000 * 60 * 30; // 30 minutes

    /**
     * Determines whether the specified email has a broadcast chance.
     * 
     * @param email the specified email
     * @return {@code true} if it has, returns {@code false} otherwise
     */
    public boolean hasBroadcastChance(final String email) {
        final Query query = new Query().setFilter(new PropertyFilter(BroadcastChance.BROADCAST_CHANCE_EMAIL, FilterOperator.EQUAL, email));

        try {
            final JSONObject result = broadcastChanceRepository.get(query);

            return 0 != result.optJSONArray(Keys.RESULTS).length();
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Determines broadcast chance failed", e);

            return false;
        }
    }

    /**
     * Generates broadcast chances.
     */
    public void generateBroadcastChances() {
        try {
            final long count = broadcastChanceRepository.count();
            if (0 == count) {
                gen(MAX_SIZE);

                return;
            }

            final long expirationTime = System.currentTimeMillis() - CYCLE_TIME;

            final Query query = new Query().setPageCount(1);
            query.setFilter(new PropertyFilter(BroadcastChance.BROADCAST_CHANCE_POST_TIME, FilterOperator.LESS_THAN, expirationTime));

            final JSONObject result = broadcastChanceRepository.get(query);
            final JSONArray array = result.getJSONArray(Keys.RESULTS);
            if (0 == array.length()) { // All broadcast chances are active
                return;
            }

            gen(array.length());
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Generates broadcast chances failed", e);
        }
    }

    /**
     * Sends all generated broadcast chances to clients.
     */
    public void sendBroadcastChances() {
        try {
            final Query query = new Query().setPageCount(1);
            final JSONObject result = broadcastChanceRepository.get(query);
            final JSONArray array = result.getJSONArray(Keys.RESULTS);
            for (int i = 0; i < array.length(); i++) {
                final JSONObject broadcastChance = array.getJSONObject(i);
                String clientURL = broadcastChance.getString(BroadcastChance.BROADCAST_CHANCE_HOST);
                if (!clientURL.endsWith("/")) {
                    clientURL += "/";
                }

                clientURL += "console/plugins/b3log-broadcast/chance";

                final HTTPRequest request = new HTTPRequest();
                request.setURL(new URL(clientURL));
                request.setRequestMethod(HTTPRequestMethod.POST);

                final long expiration = broadcastChance.getLong(BroadcastChance.BROADCAST_CHANCE_POST_TIME)
                        + broadcastChance.getLong(BroadcastChance.BROADCAST_CHANCE_CYCLE_TIME);

                request.addPayloadEntry(BroadcastChance.BROADCAST_CHANCE_T_EXPIRATION_TIME, Long.toString(expiration));

                urlFetchService.fetchAsync(request);

                LOGGER.log(Level.INFO, "Sent a broadcast chance to client[{0}]", clientURL);
            }
        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Generates broadcast chances failed", e);
        }
    }

    /**
     * Generates broadcast chances with the specified size.
     * 
     * @param size the specified size
     */
    private void gen(final int size) {
        final Transaction transaction = broadcastChanceRepository.beginTransaction();

        try {
            final List<JSONObject> users = userRepository.getRandomly(size);
            for (final JSONObject user : users) {
                String userURL = user.getString(User.USER_URL);

                if (!Strings.isURL(userURL)) { // For the legacy data 
                    userURL = "http://" + userURL;

                    if (!Strings.isURL(userURL)) {
                        LOGGER.log(Level.WARNING, "User URL [{0}] is invalid", userURL);

                        continue;
                    }
                }

                final JSONObject broadcastChance = new JSONObject();
                broadcastChance.put(BroadcastChance.BROADCAST_CHANCE_HOST, userURL);
                broadcastChance.put(BroadcastChance.BROADCAST_CHANCE_EMAIL, user.getString(User.USER_EMAIL));
                broadcastChance.put(BroadcastChance.BROADCAST_CHANCE_POST_TIME, System.currentTimeMillis());
                broadcastChance.put(BroadcastChance.BROADCAST_CHANCE_CYCLE_TIME, CYCLE_TIME);

                broadcastChanceRepository.add(broadcastChance);

                LOGGER.log(Level.INFO, "Generated broadcast chance [host={0}]", userURL);
            }

            transaction.commit();
        } catch (final Exception e) {
            if (transaction.isActive()) {
                transaction.rollback();
            }

            LOGGER.log(Level.SEVERE, "Adds broadcast chances failed", e);
        }
    }

    /**
     * Gets the {@link BroadcastChanceService} singleton.
     *
     * @return the singleton
     */
    public static BroadcastChanceService getInstance() {
        return BroadcastChanceService.SingletonHolder.SINGLETON;
    }

    /**
     * Private constructor.
     */
    private BroadcastChanceService() {
    }

    /**
     * Singleton holder.
     *
     * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
     * @version 1.0.0.0, Apr 15, 2013
     */
    private static final class SingletonHolder {

        /**
         * Singleton.
         */
        private static final BroadcastChanceService SINGLETON = new BroadcastChanceService();

        /**
         * Private default constructor.
         */
        private SingletonHolder() {
        }
    }
}
