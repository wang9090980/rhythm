/*
 * Copyright (c) 2009, 2010, 2011, 2012, 2013, 2014, B3log Team
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

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.b3log.latke.util.Strings;

/**
 * Rhythm utilities.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.1.11, Oct 16, 2014
 * @since 0.1.4
 */
public final class Rhythms {

    /**
     * Version.
     */
    public static final String RHYTHM_VERSION = "0.2.0";

    /**
     * Indent factor.
     */
    public static final int INDENT_FACTOR = 4;

    /**
     * Key of symphony.
     */
    public static final String KEY_OF_SYMPHONY;

    /**
     * Number of broadcast chance.
     */
    public static final int BROADCAST_CHANCE_NUM;

    /**
     * Minimum article post period in milliseconds.
     */
    public static final long MIN_STEP_POST_TIME;

    /**
     * Configurations.
     */
    public static final ResourceBundle CFG = ResourceBundle.getBundle("rhythm");

    /**
     * Released B3log Solo versions.
     */
    public static final List<String> RELEASED_SOLO_VERSIONS = new ArrayList<String>();
 
    /**
     * The latest B3log Solo download URL.
     */
    public static final String LATEST_SOLO_DL_URL 
            = "http://pan.baidu.com/share/link?shareid=541735&uk=3255126224#dir/path=%2Fb3log-solo%2F0.6.7";
    /**
     * The latest development B3log Solo version.
     */
    public static final String SNAPSHOT_SOLO_VERSION = "0.6.8";
   

    static {
        RELEASED_SOLO_VERSIONS.add("0.5.5");
        RELEASED_SOLO_VERSIONS.add("0.5.6");
        RELEASED_SOLO_VERSIONS.add("0.6.0");
        RELEASED_SOLO_VERSIONS.add("0.6.1");
        RELEASED_SOLO_VERSIONS.add("0.6.5");
        RELEASED_SOLO_VERSIONS.add("0.6.6");
        RELEASED_SOLO_VERSIONS.add("0.6.7");

        KEY_OF_SYMPHONY = CFG.getString("keyOfSymphony");
        MIN_STEP_POST_TIME = Long.valueOf(CFG.getString("minStepPostTime"));
        BROADCAST_CHANCE_NUM = Integer.valueOf(CFG.getString("broadcastChanceNum"));
    }

    /**
     * Checks whether the specified client name is valid.
     * 
     * @param clientName the specified client name
     * @return {@code true} if it is valid, returns {@code false} otherwise
     */
    public static boolean isValidClient(final String clientName) {
        // TODO: HARD-CODING
        return "B3log Solo".equals(clientName) || "Cat".equals(clientName);
    }

    /**
     * Gets the latest Solo version with the specified current versions.
     * 
     * @param currentVersion the specified current version
     * @return the latest Solo version
     */
    public static String getLatestSoloVersion(final String currentVersion) {
        final String latest = RELEASED_SOLO_VERSIONS.get(RELEASED_SOLO_VERSIONS.size() - 1);
        if (Strings.isEmptyOrNull(currentVersion)) {
            return latest;
        }

        if (currentVersion.compareTo(latest) > 0) {
            return currentVersion;
        }

        return latest;
    }

    /**
     * Private default constructor.
     */
    private Rhythms() {
    }
}
