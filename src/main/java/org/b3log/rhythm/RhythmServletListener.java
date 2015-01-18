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
package org.b3log.rhythm;

import java.util.ResourceBundle;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSessionEvent;
import org.b3log.latke.event.EventManager;
import org.b3log.latke.ioc.Lifecycle;
import org.b3log.latke.logging.Level;
import org.b3log.latke.logging.Logger;
import org.b3log.latke.servlet.AbstractServletListener;
import org.b3log.latke.util.Stopwatchs;
import org.b3log.latke.util.Strings;
import org.b3log.rhythm.event.symphony.ArticleSender;
import org.b3log.rhythm.event.symphony.ArticleUpdater;

/**
 * B3log Rhythm servlet listener.
 *
 * @author <a href="mailto:DL88250@gmail.com">Liang Ding</a>
 * @version 1.0.0.6, Apr 26, 2013
 * @since 0.1.4
 */
public final class RhythmServletListener extends AbstractServletListener {

    /**
     * Logger.
     */
    private static final Logger LOGGER = Logger.getLogger(RhythmServletListener.class.getName());

    /**
     * B3log Symphony address.
     */
    public static final String B3LOG_SYMPHONY_SERVE_PATH;

    static {
        final ResourceBundle b3log = ResourceBundle.getBundle("b3log");

        B3LOG_SYMPHONY_SERVE_PATH = b3log.getString("symphony.servePath");
    }

    @Override
    public void contextInitialized(final ServletContextEvent servletContextEvent) {
        super.contextInitialized(servletContextEvent);

        registerEventProcessor();

        LOGGER.info("Initialized the context");
    }

    @Override
    public void contextDestroyed(final ServletContextEvent servletContextEvent) {
        super.contextDestroyed(servletContextEvent);

        LOGGER.info("Destroyed the context");
    }

    @Override
    public void sessionCreated(final HttpSessionEvent httpSessionEvent) {
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent httpSessionEvent) {
    }

    @Override
    public void requestInitialized(final ServletRequestEvent servletRequestEvent) {
        final HttpServletRequest servletRequest =
                (HttpServletRequest) servletRequestEvent.getServletRequest();
        Stopwatchs.start("Request Initialized[requestURI=" + servletRequest.
                getRequestURI() + "]");
    }

    @Override
    public void requestDestroyed(final ServletRequestEvent servletRequestEvent) {
        Stopwatchs.end();

        LOGGER.log(Level.DEBUG, "Stopwatch: {0}{1}", new Object[]{Strings.LINE_SEPARATOR, Stopwatchs.getTimingStat()});
        Stopwatchs.release();
    }

    /**
     * Register event processors.
     */
    private void registerEventProcessor() {
        try {
            final EventManager eventManager = Lifecycle.getBeanManager().getReference(EventManager.class);

            eventManager.registerListener(new ArticleSender());
            eventManager.registerListener(new ArticleUpdater());

        } catch (final Exception e) {
            LOGGER.log(Level.ERROR, "Register event processors error", e);
            throw new RuntimeException(e);
        }
    }
}
