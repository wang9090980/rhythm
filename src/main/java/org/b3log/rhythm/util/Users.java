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

import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.b3log.latke.user.UserService;
import org.b3log.latke.user.gae.GAEUserService;

/**
 * User utilities.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.1, Sep 30, 2011
 * @since 0.1.4
 */
public final class Users {

    /**
     * Logger.
     */
    private static final Logger LOGGER =
            Logger.getLogger(Users.class.getName());
    /**
     * User service.
     */
    private UserService userService = new GAEUserService();

    /**
     * Checks whether the current request is made by logged in administrator.
     *
     * @param request the specified request
     * @return {@code true} if the current request is made by logged in
     * administrator, returns {@code false} otherwise
     */
    public boolean isAdminLoggedIn(final HttpServletRequest request) {
        return userService.isUserLoggedIn(request)
               && userService.isUserAdmin(request);
    }

    /**
     * Gets the {@link Users} singleton.
     *
     * @return the singleton
     */
    public static Users getInstance() {
        return SingletonHolder.SINGLETON;
    }

    /**
     * Private default constructor.
     */
    private Users() {
    }

    /**
     * Singleton holder.
     *
     * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
     * @version 1.0.0.0, Jan 12, 2011
     */
    private static final class SingletonHolder {

        /**
         * Singleton.
         */
        private static final Users SINGLETON = new Users();

        /**
         * Private default constructor.
         */
        private SingletonHolder() {
        }
    }
}
