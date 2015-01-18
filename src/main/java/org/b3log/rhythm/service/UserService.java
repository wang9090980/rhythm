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
package org.b3log.rhythm.service;

import java.util.Collections;
import java.util.List;
import javax.inject.Inject;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.RepositoryException;
import org.b3log.latke.repository.annotation.Transactional;
import org.b3log.latke.service.annotation.Service;
import org.b3log.rhythm.repository.UserRepository;
import org.json.JSONObject;

/**
 * User service.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.0, May 30, 2014
 * @since 0.2.0
 */
@Service
public class UserService {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(UserService.class.getName());

    /**
     * User repository.
     */
    @Inject
    private UserRepository userRepository;

    /**
     * Removes the specified user by the given user id.
     *
     * @param userId the given user id
     */
    @Transactional
    public void removeUser(final String userId) {
        try {
            userRepository.remove(userId);

            LOGGER.info("Removed user [id=" + userId + ']');
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Removes user [id=" + userId + "] failed", e);
        }
    }

    /**
     * Gets users randomly with the specified fetch size.
     *
     * @param fetchSize the specified fetch size
     * @return a list of json objects, its size less or equal to the specified fetch size, returns an empty list if not found
     */
    public List<JSONObject> getUsersRandomly(final int fetchSize) {
        try {
            return userRepository.getRandomly(fetchSize);
        } catch (final RepositoryException e) {
            LOGGER.log(Level.ERROR, "Gets users ranomly failed", e);

            return Collections.<JSONObject>emptyList();
        }
    }
}
