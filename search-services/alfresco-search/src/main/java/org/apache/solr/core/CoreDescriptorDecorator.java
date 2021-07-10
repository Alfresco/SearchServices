/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

package org.apache.solr.core;

import java.util.List;
import java.util.Properties;

import org.alfresco.solr.config.ConfigUtil;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.google.common.collect.ImmutableList;

/**
 * This class was created solely for the purpose of exposing the coreProperties of the CoreDescriptor.
 * It is now possible to substitute a sub-set of properties using the rules specified here @see ConfigUtil#locateProperty()
 *
 * The Substitutable Properties are defined in the substitutableProperties list.
 * @author Ahmed Owian
 * @author Gethin James
 * @author aborroy
 */
public class CoreDescriptorDecorator 
{
    private static Log log = LogFactory.getLog(CoreDescriptorDecorator.class); 
    private final Properties properties = new Properties();
    
    private static String SECURE_COMMS_PROPERTY =  "alfresco.secureComms";

    public static final ImmutableList<String> SUBSTITUTABLE_PROPERTIES_SECURE =
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

    public static final ImmutableList<String> SUBSTITUTABLE_PROPERTIES_NONE =
            ImmutableList.of(
                    "alfresco.host",
                    "alfresco.port",
                    "alfresco.baseUrl",
                    "alfresco.secureComms");
    
    public CoreDescriptorDecorator(CoreDescriptor descriptor)
    {
        properties.putAll(descriptor.coreProperties);
        
        List<String> coreProperties;
        String comms = ConfigUtil.locateProperty(SECURE_COMMS_PROPERTY, "none");
        if (comms.equals("https"))
        {
        	coreProperties = SUBSTITUTABLE_PROPERTIES_SECURE;
        }
        else 
        {
        	coreProperties = SUBSTITUTABLE_PROPERTIES_NONE;
        }
        
        try
        {
            for (String prop : coreProperties) {
                String value = ConfigUtil.locateProperty(prop, null);
                if (value != null) {
                    properties.put(prop, value);
                }
            }
        }
        catch(Exception e)
        {
            log.warn("Unable to locate alfresco host|port|baseUrl|ssl properties", e);
        }
    }

    public Properties getProperties()
    {
        return this.properties;
    }
}
