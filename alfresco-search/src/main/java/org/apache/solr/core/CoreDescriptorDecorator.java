/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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
package org.apache.solr.core;

import com.google.common.collect.ImmutableList;
import org.alfresco.solr.config.ConfigUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Properties;

/**
 * This class was created solely for the purpose of exposing the coreProperties of the CoreDescriptor.
 * It is now possible to substitute a sub-set of properties using the rules specified here @see ConfigUtil#locateProperty()
 *
 * The Substitutable Properties are defined in the substitutableProperties list.
 * @author Ahmed Owian
 * @author Gethin James
 */
public class CoreDescriptorDecorator 
{
    private static Log log = LogFactory.getLog(CoreDescriptorDecorator.class); 
    private final Properties properties = new Properties();

    public static ImmutableList<String> substitutableProperties = 
            ImmutableList.of(
                    "alfresco.host",
                    "alfresco.port",
                    "alfresco.baseUrl",
                    "alfresco.port.ssl",
                    "alfresco.secureComms",
                    "alfresco.encryption.ssl.keystore.passwordFileLocation",
                    "alfresco.encryption.ssl.truststore.passwordFileLocation",
                    "alfresco.encryption.ssl.keystore.location",
                    "alfresco.encryption.ssl.truststore.location",
                    "alfresco.encryption.ssl.truststore.provider",
                    "alfresco.encryption.ssl.keystore.type",
                    "alfresco.encryption.ssl.keystore.provider",
                    "alfresco.encryption.ssl.truststore.type");

    public CoreDescriptorDecorator(CoreDescriptor descriptor)
    {
        properties.putAll(descriptor.coreProperties);
        try
        {
            substitutableProperties.forEach(prop ->
                 properties.put(prop, ConfigUtil.locateProperty(prop,properties.getProperty(prop)))
            );
        }
        catch(Exception e)
        {
            log.warn("Unable to locate alfresco host|port|baseUrl|ssl properties");
        }
    }

    public Properties getProperties()
    {
        return this.properties;
    }
}
