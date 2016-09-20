/*
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 *
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
 */
package org.alfresco.solr;

import org.apache.commons.io.FileUtils;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.core.CoreContainer;

import java.io.*;
import java.util.Iterator;
import java.util.Properties;

/**
 * Methods taken from AlfrescoCoreAdminHandler that deal with I/O resources
 */
public class HandlerOfResources {

    /**
     * Opens an InputStream
     * @param solrHome
     * @param resource
     * @return InputStream
     */
    public static InputStream openResource(String solrHome, String resource)
    {
        InputStream is = null;
        try
        {
            File f0 = new File(resource);
            File f = f0;
            if (!f.isAbsolute())
            {
                // try $CWD/$configDir/$resource
                String path = solrHome;
                path = path.endsWith("/") ? path : path + "/";
                f = new File(path + resource);
            }
            if (f.isFile() && f.canRead())
            {
                return new FileInputStream(f);
            }
            else if (f != f0)
            { // no success with $CWD/$configDir/$resource
                if (f0.isFile() && f0.canRead()) return new FileInputStream(f0);
            }
            // delegate to the class loader (looking into $INSTANCE_DIR/lib jars)
            is = Thread.currentThread().getContextClassLoader().getResourceAsStream(resource);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Error opening " + resource, e);
        }
        if (is == null) { throw new RuntimeException("Can't find resource '" + resource + "' in classpath or '"
                + solrHome + "', cwd=" + System.getProperty("user.dir")); }
        return is;
    }


    /**
     * Updates a properties file using the SolrParams
     *
     * @param params
     * @param config
     * @throws IOException
     */
    public static void updatePropertiesFile(SolrParams params, File config) throws IOException {
        // fix configuration properties
        Properties properties = new Properties();
        properties.load(new FileInputStream(config));

        Properties extraProperties = extractCustomProperties(params);
        //Allow the properties to be overidden via url params
        if (extraProperties != null && !extraProperties.isEmpty())
        {
            properties.putAll(extraProperties);
        }

        properties.store(new FileOutputStream(config), null);
    }

    /**
     * Extracts Custom Properties from SolrParams
     * @param params
     * @return Properties
     */
    public static Properties extractCustomProperties(SolrParams params) {
        Properties properties = new Properties();
        //Add any custom properties.
        for (Iterator<String> it = params.getParameterNamesIterator(); it.hasNext(); /**/)
        {
            String paramName = it.next();
            if (paramName.startsWith("property."))
            {
                properties.setProperty(paramName.substring("property.".length()), params.get(paramName));
            }
        }
        return properties;
    }

    /**
     * Safely gets a boolean from SolrParams
     * @param params
     * @param paramName
     * @return boolean
     */
    public static boolean getSafeBoolean(SolrParams params, String paramName)
    {
        boolean paramValue = false;
        if (params.get(paramName) != null)
        {
            paramValue = Boolean.valueOf(params.get(paramName));
        }
        return paramValue;
    }

    /**
     * Safely gets a Long from SolrParams
     * @param params
     * @param paramName
     * @return Long
     */
    public static Long getSafeLong(SolrParams params, String paramName)
    {
        Long paramValue = null;
        if (params.get(paramName) != null)
        {
            paramValue = Long.valueOf(params.get(paramName));
        }
        return paramValue;
    }
}
