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

import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import org.b3log.latke.util.Strings;

/**
 * Rhythm utilities.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.1.3, Nov 24, 2012
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
     * The latest development B3log Solo version.
     */
    public static final String SNAPSHOT_SOLO_VERSION = "0.5.6";

    static {
        RELEASED_SOLO_VERSIONS.add("0.4.6");
        RELEASED_SOLO_VERSIONS.add("0.5.0");
        RELEASED_SOLO_VERSIONS.add("0.5.5");

        KEY_OF_SYMPHONY = CFG.getString("keyOfSymphony");
        MIN_STEP_POST_TIME = Long.valueOf(CFG.getString("minStepPostTime"));
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
