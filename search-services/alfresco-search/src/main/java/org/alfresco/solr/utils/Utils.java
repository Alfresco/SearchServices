/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.alfresco.solr.utils;

import static java.util.Collections.emptyList;
import static java.util.Optional.ofNullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.Collection;
import java.util.List;

/**
 * SearchServices Booch utilities.
 *
 * @author Andrea Gazzarini
 */
public abstract class Utils
{
    private static final Logger LOGGER = LoggerFactory.getLogger(Utils.class);

    /**
     * Returns the same input collection if that is not null, otherwise a new empty collection.
     * Provides a safe way for iterating over a returned collection (which could be null).
     *
     * @param values the collection.
     * @param <T> the collection type.
     * @return the same input collection if that is not null, otherwise a new empty collection.
     */
    public static <T> Collection<T> notNullOrEmpty(Collection<T> values)
    {
        return values != null ? values : emptyList();
    }

    /**
     * Returns the same input list if that is not null, otherwise a new empty list.
     * Provides a safe way for iterating over a returned list (which could be null).
     *
     * @param values the input list.
     * @param <T> the list elements type.
     * @return the same input list if that is not null, otherwise a new empty list.
     */
    public static <T> List<T> notNullOrEmpty(List<T> values)
    {
        return values != null ? values : emptyList();
    }

    /**
     * Makes sure we are not dealing with a null array.
     *
     * @param values the input array.
     * @param <T> the array type.
     * @return the input array if it is not null, an empty array otherwise.
     */
    public static <T> T[] notNullOrEmpty(final T[] values)
    {
        return values != null ? values : emptyList().toArray(values);
    }

    /**
     * Converts the given input in an Integer, otherwise it returns null.
     *
     * @param value the numeric string.
     * @return the corresponding Integer or null in case the input is NaN.
     */
    public static Integer toIntOrNull(String value)
    {
        try
        {
            return Integer.valueOf(value);
        }
        catch(NumberFormatException nfe)
        {
            return null;
        }
    }

    /**
     * Silently closes the given {@link Closeable} resource without raising any exception.
     * This utility method is specifically useful when we have to close a resource in a lamba statement: since the
     * close() method could throw an {@link IOException} the compiler requires an enclosing try / catch block which
     * makes the code less readable.
     *
     * <br/><br/>
     * <p>
     *     <code>
     *          try { if (resource != null) resource.close } catch (IOException exception) { ... }
     *      </code>
     * </p>
     * <br/>
     *
     * In these contexts a call to this method reduces the amount of code needed:
     *
     * <br/><br/>
     * <p>
     *  <code>
     *      silentlyClose(resource);
     *  </code>
     * </p>
     *
     * @param resource the {@link Closeable} resource we want to silently close.
     */
    public static void silentyClose(Closeable resource)
    {
        try
        {
            if (resource != null) resource.close();
        }
        catch(IOException ignore)
        {
            LOGGER.warn("Unable to properly close the resource instance {}. See the stacktrace below for further details.", resource, ignore);
        }
    }

    /**
     * Returns true if the input string is null or is empty.
     * Note whitespaces are not considered, so if a string contains only whitespaces, it is considered empty.
     *
     * @param value the input string.
     * @return true if the input string is null or is empty.
     */
    public static boolean isNullOrEmpty(String value)
    {
        return value == null || value.trim().length() == 0;
    }

    /**
     * Returns true if the input string is not null and it is not empty.
     * Note whitespaces are not considered, so if a string contains only whitespaces, it is considered empty.
     *
     * @param value the input string.
     * @return true if the input string is not null and it is not empty.
     */
    public static boolean isNotNullAndNotEmpty(String value)
    {
        return value != null && value.trim().length() != 0;
    }

    /**
     * Returns true if the first character of the input string is the locale marker character.
     *
     * @param value the input string.
     * @return true true if the first character of the input string is the locale marker character.
     */
    public static boolean startsWithLanguageMarker(String value)
    {
        return ofNullable(value)
                    .map(v -> v.charAt(0) == '\u0000')
                    .orElse(false);
    }

    public static Double doubleOrNull(String value)
    {
        try
        {
            return Double.parseDouble(value);
        }
        catch(Exception exception)
        {
            LOGGER.error("Input string >{}< cannot be converted in a valid double ", value);
            return null;
        }
    }
}
