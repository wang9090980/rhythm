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
 * This class defines all broadcast chance model relevant keys.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.0, Apr 15, 2013
 * @since 0.1.6
 */
public final class BroadcastChance {

    /**
     * Broadcast chance.
     */
    public static final String BROADCAST_CHANCE = "broadcastChance";

    /**
     * Broadcast chances.
     */
    public static final String BROADCAST_CHANCES = "broadcastChances";

    /**
     * Key of host.
     */
    public static final String BROADCAST_CHANCE_HOST = "broadcastChanceHost";

    /**
     * Key of email.
     */
    public static final String BROADCAST_CHANCE_EMAIL = "broadcastChanceEmail";

    /**
     * Key of post time.
     */
    public static final String BROADCAST_CHANCE_POST_TIME = "broadcastChancePostTime";

    /**
     * Key of live time.
     */
    public static final String BROADCAST_CHANCE_CYCLE_TIME = "broadcastChanceCycleTime";

    //// Transient ////
    /**
     * Key of expiration time.
     */
    public static final String BROADCAST_CHANCE_T_EXPIRATION_TIME = "broadcastChanceExpirationTime";

    /**
     * Private default constructor.
     */
    private BroadcastChance() {
    }
}
