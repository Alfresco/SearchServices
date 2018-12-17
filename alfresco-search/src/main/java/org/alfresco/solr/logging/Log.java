/*
 * Copyright (C) 2015 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.solr.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Logging utility.
 * It is a simple wrapper around a logger implementation, which allows a more concise code.
 *
 * @author Andrea Gazzarini
 * @since 1.3
 */
public class Log
{
    private final Logger logger;

    public Log(Class owner)
    {
        this.logger = LoggerFactory.getLogger(owner);
    }

    /**
     * Logs a given message with DEBUG level.
     *
     * @param message the message template (with placeholders)
     * @param params the optional array of values.
     */
    public void debug(String message, Object ... params)
    {
        if(logger.isDebugEnabled())
        {
            logger.debug(message, params);
        }
    }

    /**
     * Logs a given message with WARNING level.
     *
     * @param message the message template (with placeholders)
     * @param params the optional array of values.
     */
    public void warning(String message, Object ... params)
    {
        if(logger.isWarnEnabled())
        {
            logger.warn(message, params);
        }
    }

    /**
     * Logs a given message with ERROR level.
     *
     * @param message the message template (with placeholders)
     * @param params the optional array of values.
     */
    public void error(String message, Object ... params)
    {
        logger.error(message, params);
    }

    /**
     * Logs a given message with INFO level.
     *
     * @param message the message template (with placeholders)
     * @param params the optional array of values.
     */
    public void info(String message, Object ... params)
    {
        logger.info(message, params);
    }
}
