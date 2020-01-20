/*
 * #%L
 * Alfresco Data model classes
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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

package org.alfresco.solr.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.alfresco.encryption.KeyResourceLoader;
import org.alfresco.encryption.KeyStoreParameters;
import org.alfresco.encryption.ssl.SSLEncryptionParameters;
import org.alfresco.httpclient.AlfrescoHttpClient;
import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.httpclient.HttpClientFactory.SecureCommsType;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.apache.commons.httpclient.params.DefaultHttpParams;

/**
 * This factory encapsulates the creation of a SOLRAPIClient and the management of that resource.
 * 
 * @author Ahmed Owian
 */
public class SOLRAPIClientFactory
{
    /*
     * Pool of cached client resources keyed on alfresco instances
     */
    private static Map<String, SOLRAPIClient> clientsPerAlfresco = new HashMap<>();

    // encryption related parameters
    private String secureCommsType; // "none", "https"

    // ssl
    private String sslKeyStoreType;
    private String sslKeyStoreProvider;
    private String sslKeyStoreLocation;
    private String sslKeyStorePasswordFileLocation;
    private String sslTrustStoreType;
    private String sslTrustStoreProvider;
    private String sslTrustStoreLocation;
    private String sslTrustStorePasswordFileLocation;
    private String alfrescoHost;
    private int alfrescoPort;
    private int alfrescoPortSSL;
    private String baseUrl;

    // http client
    private int maxTotalConnections = 40;
    private int maxHostConnections = 40;
    private int socketTimeout = 120000;


    public static void close() {
        for(SOLRAPIClient client : clientsPerAlfresco.values()) {
            client.close();
        }
    }

    /**
     * Gets the client resource from the pool.
     * 
     * @param alfrescoHost String
     * @param alfrescoPort int
     * @param alfrescoPortSSL int
     * @return SOLRAPIClient
     */
    private SOLRAPIClient getCachedClient(String alfrescoHost, int alfrescoPort, int alfrescoPortSSL)
    {
        String key = constructKey(alfrescoHost, alfrescoPort, alfrescoPortSSL);
        return clientsPerAlfresco.get(key);
    }

    /**
     * Constructs a key to identify a unique alfresco instance to which the client will connect.
     * 
     * @param alfrescoHost String
     * @param alfrescoPort int
     * @param alfrescoPortSSL int
     * @return the key to get a client
     */
    private String constructKey(String alfrescoHost, int alfrescoPort, int alfrescoPortSSL)
    {
        return alfrescoHost + alfrescoPort + alfrescoPortSSL;
    }

    /**
     * Sets the client in the resource pool.
     * 
     * @param alfrescoHost String
     * @param alfrescoPort int
     * @param alfrescoPortSSL int
     * @param client SOLRAPIClient
     */
    private void setCachedClient(String alfrescoHost, int alfrescoPort, int alfrescoPortSSL, SOLRAPIClient client)
    {
        String key = constructKey(alfrescoHost, alfrescoPort, alfrescoPortSSL);
        clientsPerAlfresco.put(key, client);
    }

    /**
     * Creates the SOLRAPIClient or gets it from a pool
     * 
     * @param props solrcore.properties in the <coreName>/conf directory
     * @param keyResourceLoader reads encryption key resources
     * @param dictionaryService represents the Repository Data Dictionary
     * @param namespaceDAO allows retrieving and creating Namespace definitions
     * @return an instance of SOLRAPIClient
     */
    public SOLRAPIClient getSOLRAPIClient(Properties props, KeyResourceLoader keyResourceLoader,
                DictionaryService dictionaryService, NamespaceDAO namespaceDAO)
    {

        if(Boolean.parseBoolean(System.getProperty("alfresco.test", "false")))
        {
            return new SOLRAPIQueueClient(namespaceDAO);
        }

        alfrescoHost = props.getProperty("alfresco.host", "localhost");
        alfrescoPort = Integer.parseInt(props.getProperty("alfresco.port", "8080"));
        alfrescoPortSSL = Integer.parseInt(props.getProperty("alfresco.port.ssl", "8443"));

        SOLRAPIClient client = getCachedClient(alfrescoHost, alfrescoPort, alfrescoPortSSL);
        if (client == null)
        {
            baseUrl = props.getProperty("alfresco.baseUrl", "/alfresco");
            // Load SSL settings only when using HTTPs protocol
            secureCommsType = props.getProperty("alfresco.secureComms", "none");
            if (secureCommsType.equals("https"))
            {
	            sslKeyStoreType = getProperty(props, "alfresco.encryption.ssl.keystore.type", "JCEKS");
	            sslKeyStoreProvider = getProperty(props, "alfresco.encryption.ssl.keystore.provider", "");
	            sslKeyStoreLocation = getProperty(props, "alfresco.encryption.ssl.keystore.location",
	                        "ssl.repo.client.keystore");
	            sslKeyStorePasswordFileLocation = getProperty(props, 
	                        "alfresco.encryption.ssl.keystore.passwordFileLocation", 
	                        "ssl-keystore-passwords.properties");
	            sslTrustStoreType = getProperty(props, "alfresco.encryption.ssl.truststore.type", "JCEKS");
	            sslTrustStoreProvider = getProperty(props, "alfresco.encryption.ssl.truststore.provider", "");
	            sslTrustStoreLocation = getProperty(props, "alfresco.encryption.ssl.truststore.location",
	                        "ssl.repo.client.truststore");
	            sslTrustStorePasswordFileLocation = getProperty(props, 
	                        "alfresco.encryption.ssl.truststore.passwordFileLocation",
	                        "ssl-truststore-passwords.properties");
            }
            maxTotalConnections = Integer.parseInt(props.getProperty("alfresco.maxTotalConnections", "40"));
            maxHostConnections = Integer.parseInt(props.getProperty("alfresco.maxHostConnections", "40"));
            socketTimeout = Integer.parseInt(props.getProperty("alfresco.socketTimeout", "60000"));

            client = new SOLRAPIClient(getRepoClient(keyResourceLoader), dictionaryService, namespaceDAO);
            setCachedClient(alfrescoHost, alfrescoPort, alfrescoPortSSL, client);
        }

        return client;
    }

    protected AlfrescoHttpClient getRepoClient(KeyResourceLoader keyResourceLoader)
    {
    	HttpClientFactory httpClientFactory = null;
    	
    	if (secureCommsType.equals("https"))
    	{  
	        KeyStoreParameters keyStoreParameters = new KeyStoreParameters("SSL Key Store", sslKeyStoreType,
	                    sslKeyStoreProvider, sslKeyStorePasswordFileLocation, sslKeyStoreLocation);
	        KeyStoreParameters trustStoreParameters = new KeyStoreParameters("SSL Trust Store", sslTrustStoreType,
	                    sslTrustStoreProvider, sslTrustStorePasswordFileLocation, sslTrustStoreLocation);
	        SSLEncryptionParameters sslEncryptionParameters = new SSLEncryptionParameters(keyStoreParameters,
	                    trustStoreParameters);
	        httpClientFactory = new HttpClientFactory(SecureCommsType.getType(secureCommsType),
                    sslEncryptionParameters, keyResourceLoader, null, null, alfrescoHost, alfrescoPort,
                    alfrescoPortSSL, maxTotalConnections, maxHostConnections, socketTimeout);
    	}
    	else
    	{
    		httpClientFactory = new PlainHttpClientFactory(alfrescoHost, alfrescoPort, maxTotalConnections, maxHostConnections);
    	}
    	
        AlfrescoHttpClient repoClient = httpClientFactory.getRepoClient(alfrescoHost, alfrescoPortSSL);
        repoClient.setBaseUrl(baseUrl);
        return repoClient;
        
    }
    
    /**
     * Return property value from system (passed as -D argument).
     * If the system property does not exists, return local value from solrcore.properties
     * If the local property does not exists, return default value
     * 
     * @param props Local properties file (solrcore.properties)
     * @param key The property key
     * @return The value
     */
    private String getProperty(Properties props, String key, String defaultValue) 
    {
    	String value = System.getProperties().getProperty(key);
    	if (value == null)
    	{
    		value = props.getProperty(key);
    	}
    	if (value == null)
    	{
    		value = defaultValue;
    	}
    	return value;
    }
    
    /**
     * Local class to avoid loading sslEntryptionParameters for plain http connections. 
     * 
     * @author aborroy
     *
     */
    class PlainHttpClientFactory extends HttpClientFactory
    {
        public PlainHttpClientFactory(String host, int port, int maxTotalConnections, int maxHostConnections)
        {
            setSecureCommsType("none");
            setHost(host);
            setPort(port);
            setMaxTotalConnections(maxTotalConnections);
            setMaxHostConnections(maxHostConnections);
            init();
        }

        @Override
        public void init()
        {
            DefaultHttpParams.setHttpParamsFactory(new NonBlockingHttpParamsFactory());
        }

    }
    
}

