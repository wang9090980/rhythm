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
package org.b3log.rhythm.model;

/**
 * This class defines all Sym model relevant keys.
 *
 * @author <a href="http://88250.b3log.org">Liang Ding</a>
 * @version 1.0.0.1, Oct 29, 2016
 * @since 1.2.0
 */
public final class Sym {

    /**
     * Key of sym.
     */
    public static final String SYM = "sym";

    /**
     * Key of syms.
     */
    public static final String SYMS = "syms";

    /**
     * Key of sym URL.
     */
    public static final String SYM_URL = "symURL";

    /**
     * Key of sym title.
     */
    public static final String SYM_TITLE = "symTitle";

    /**
     * Key of sym icon.
     */
    public static final String SYM_ICON = "symIcon";

    /**
     * Key of sym description.
     */
    public static final String SYM_DESC = "symDesc";

    /**
     * Key of sym accessibility check count.
     */
    public static final String SYM_ACCESSIBILITY_CHECK_CNT = "symAccessibilityCheckCnt";

    /**
     * Key of sym accessibility check count.
     */
    public static final String SYM_ACCESSIBILITY_NOT_200_CNT = "symAccessibilityNot200Cnt";

    /**
     * Key of sym status.
     */
    public static final String SYM_STATUS = "symStatus";

    //// Status Constants
    /**
     * Sym status - valid.
     */
    public static final int SYM_STATUS_C_VALID = 0;

    /**
     * Sym status - invalid.
     */
    public static final int SYM_STATUS_C_INVALID = 1;

    /**
     * Private constructor.
     */
    private Sym() {
    }
}
