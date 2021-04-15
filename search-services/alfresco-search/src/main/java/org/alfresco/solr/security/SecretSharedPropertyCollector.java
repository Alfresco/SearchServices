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

package org.alfresco.solr.security;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.config.ConfigUtil;
import org.apache.solr.core.SolrResourceLoader;

/**
 * Provides property values for Alfresco Communication using "secret" method:
 * 
 * - "alfresco.secureComms" is the commsMethod (none, https or secret)
 * - "alfresco.secureComms.secret" is the word used as shared secret for "secret" method
 * - "alfresco.secureComms.secret.header" is the request header name used for "secret" method
 *
 */
public class SecretSharedPropertyCollector
{

    public final static String SECRET_SHARED_METHOD_KEY = "secret";

    // Property names for "secret" communication method
    private static String SECURE_COMMS_PROPERTY = "alfresco.secureComms";
    private static String SHARED_SECRET = "alfresco.secureComms.secret";
    private static String SHARED_SECRET_HEADER = "alfresco.secureComms.secret.header";

    // Save communication method as static value in order to improve performance 
    private static String commsMethod;

    /**
     * Check if communications method is "secret"
     * @return true when communications method is "secret"
     */
    public static boolean isCommsSecretShared()
    {
        return Objects.equals(SecretSharedPropertyCollector.getCommsMethod(),
                    SecretSharedPropertyCollector.SECRET_SHARED_METHOD_KEY);
    }

    /**
     * Get communication method from environment variables, shared properties or core properties.
     * @return Communication method: none, https, secret
     */
    private static String getCommsMethod()
    {

        if (commsMethod == null)
        {

            // Environment variable
            commsMethod = ConfigUtil.locateProperty(SECURE_COMMS_PROPERTY, null);

            if (commsMethod == null)
            {
                // Shared configuration (shared.properties file)
                commsMethod = AlfrescoSolrDataModel.getCommonConfig().getProperty(SECURE_COMMS_PROPERTY);

                if (commsMethod == null)
                {
                    // Get configuration from deployed SOLR Cores
                    Set<String> secureCommsSet = getCommsFromCores();
                    if (secureCommsSet.size() > 1 && secureCommsSet.contains(SECRET_SHARED_METHOD_KEY))
                    {
                        throw new RuntimeException(
                                    "No valid secure comms values: all the cores must be using \"secret\" communication method but found: "
                                                + secureCommsSet);
                    }
                    commsMethod = secureCommsSet.stream().findFirst().get();

                }
            }
        }

        return commsMethod;

    }

    /**
     * Read different values of "alfresco.secureComms" property from every "solrcore.properties" files.
     * @return List of different communication methods declared in SOLR Cores.
     */
    private static Set<String> getCommsFromCores()
    {

        Set<String> secureCommsSet = new HashSet<>();

        try (Stream<Path> walk = Files.walk(Paths.get(SolrResourceLoader.locateSolrHome().toString())))
        {

            List<String> coreFiles = walk.map(x -> x.toString())
                        .filter(f -> f.contains("solrcore.properties") && !f.contains("templates"))
                        .collect(Collectors.toList());

            coreFiles.forEach(coreFile -> {

                Properties props = new Properties();
                try
                {
                    props.load(new FileReader(coreFile));
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
                String prop = props.getProperty(SECURE_COMMS_PROPERTY);
                if (prop != null)
                {
                    secureCommsSet.add(prop);
                }
                else
                {
                    secureCommsSet.add("none");
                }

            });

        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }

        return secureCommsSet;

    }

    /**
     * Read "secret" word from Java environment variable "alfresco.secureComms.secret"
     * 
     * It can be set from command line invocation using default "-D" parameter:
     * 
     * solr start -a "-Dcreate.alfresco.defaults=alfresco -Dalfresco.secureComms.secret=secret"
     * 
     * @return value for the "secret" word
     */
    public static String getSecret()
    {
        String secret = ConfigUtil.locateProperty(SHARED_SECRET, null);

        if (secret == null || secret.length() == 0)
        { 
            throw new RuntimeException("Missing value for " + SHARED_SECRET + " configuration property"); 
        }

        return secret;
    }

    /**
     * Read secret request header name from Java environment variable "alfresco.secureComms.secret.header".
     * If it's not specified, used default value "X-Alfresco-Search-Secret"
     * 
     * @return value for the secret request header
     */
    public static String getSecretHeader()
    {
        String secretHeader = ConfigUtil.locateProperty(SHARED_SECRET_HEADER, null);
        if (secretHeader != null)
        {
            return secretHeader;
        }
        else
        {
            return HttpClientFactory.DEFAULT_SHAREDSECRET_HEADER;
        }
    }
    
    /**
     * Add secret shared properties to original core properties read from "solrcore.properties"
     * @param properties Read properties from "solrcore.properties"
     * @return when "secret" communication method is configured additional properties are set in original parameter
     */
    public static Properties completeCoreProperties(Properties properties)
    {
        if (isCommsSecretShared())
        {
            properties.setProperty(SECURE_COMMS_PROPERTY, getCommsMethod());
            properties.setProperty(SHARED_SECRET, getSecret());
            properties.setProperty(SHARED_SECRET_HEADER, getSecretHeader());
        }
        return properties;
    }

}
