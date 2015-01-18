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
package org.b3log.rhythm.dev;

import java.io.IOException;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.b3log.latke.Latkes;
import org.b3log.latke.RuntimeMode;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.repository.jdbc.util.JdbcRepositories;
import org.b3log.latke.servlet.HTTPRequestContext;
import org.b3log.latke.servlet.HTTPRequestMethod;
import org.b3log.latke.servlet.annotation.RequestProcessing;
import org.b3log.latke.servlet.annotation.RequestProcessor;
import org.b3log.latke.util.Stopwatchs;

/**
 * Initializes database.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.0, Sep 3, 2012
 * @since 0.2.0
 */
@RequestProcessor
public class InitProcessor {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(InitProcessor.class.getName());

    /**
     * Generates tables.
     *
     * @param context the specified context
     * @param request the specified request
     * @param response the specified response
     * @throws IOException io exception
     */
    @RequestProcessing(value = "/dev/db/table/gen", method = HTTPRequestMethod.GET)
    public void genTables(final HTTPRequestContext context, final HttpServletRequest request, final HttpServletResponse response)
            throws IOException {
        if (RuntimeMode.PRODUCTION == Latkes.getRuntimeMode()) {
            return;
        }

        Stopwatchs.start("Gen Tables");

        try {
            LOGGER.log(Level.INFO, "Database [{0}], creates all tables", Latkes.getRuntimeDatabase());
            final List<JdbcRepositories.CreateTableResult> createTableResults = JdbcRepositories.initAllTables();
            for (final JdbcRepositories.CreateTableResult createTableResult : createTableResults) {
                LOGGER.log(Level.INFO, "Creates table result[tableName={0}, isSuccess={1}]",
                           new Object[]{createTableResult.getName(), createTableResult.isSuccess()});
            }
        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Creates database tables failed", e);
        }

        Stopwatchs.end();
    }
}
