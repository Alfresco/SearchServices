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

package org.alfresco.solr.client;

import java.util.*;

import org.alfresco.encryption.KeyResourceLoader;
import org.alfresco.encryption.KeyStoreParameters;
import org.alfresco.encryption.ssl.SSLEncryptionParameters;
import org.alfresco.httpclient.AlfrescoHttpClient;
import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.httpclient.HttpClientFactory.SecureCommsType;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.apache.commons.httpclient.Header;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.params.DefaultHttpParams;
import org.apache.commons.httpclient.params.HostParams;
import org.apache.commons.httpclient.params.HttpParams;

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
    private String secureCommsType; // "https", "apikey"

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

    // Alfresco ApiKey
    private String apiKeyHeader = DEFAULT_APIKEY_HEADER;
    private String alfrescoApiKey;
    private static final String DEFAULT_APIKEY_HEADER = "X-Alfresco-Search-ApiKey";

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
    public SOLRAPIClient getSOLRAPIClient(Properties props, KeyResourceLoader keyResourceLoader, DictionaryService dictionaryService, NamespaceDAO namespaceDAO)
    {

        if (Boolean.parseBoolean(System.getProperty("alfresco.test", "false")))
        {
            return new SOLRAPIQueueClient(namespaceDAO);
        }

        alfrescoHost = props.getProperty("alfresco.host", "localhost");
        alfrescoPort = Integer.parseInt(props.getProperty("alfresco.port", "8080"));
        alfrescoPortSSL = Integer.parseInt(props.getProperty("alfresco.port.ssl", "8443"));
        boolean compression = Boolean.parseBoolean(props.getProperty("solr.request.content.compress", "false"));

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
                        "alfresco.encryption.ssl.keystore.passwordFileLocation", "");
                sslTrustStoreType = getProperty(props, "alfresco.encryption.ssl.truststore.type", "JCEKS");
                sslTrustStoreProvider = getProperty(props, "alfresco.encryption.ssl.truststore.provider", "");
                sslTrustStoreLocation = getProperty(props, "alfresco.encryption.ssl.truststore.location",
                        "ssl.repo.client.truststore");
                sslTrustStorePasswordFileLocation = getProperty(props,
                        "alfresco.encryption.ssl.truststore.passwordFileLocation", "");
            }
            else if (secureCommsType.equals("apikey"))
            {
                alfrescoApiKey = props.getProperty("alfresco.apiKey", null);
                if(alfrescoApiKey == null || alfrescoApiKey.length()==0)
                {
                    throw new IllegalArgumentException("Missing value for alfresco.apiKey configuration property. If alfresco.secureComms is set to \"apikey\", a value for alfresco.apiKey is required. See https://docs.alfresco.com/search-enterprise/tasks/solr-install-withoutSSL.html");
                }
                apiKeyHeader = props.getProperty("alfresco.apiKeyHeader", DEFAULT_APIKEY_HEADER);
                if(apiKeyHeader == null || apiKeyHeader.length()==0)
                {
                    throw new IllegalArgumentException("Missing apiKeyHeader");
                }
            }
            else
            {
                throw new IllegalArgumentException("The only supported options for alfresco.secureComms are \"https\" and \"apikey\". Please see https://docs.alfresco.com/search-enterprise/tasks/solr-install-withoutSSL.html");
            }
            maxTotalConnections = Integer.parseInt(props.getProperty("alfresco.maxTotalConnections", "40"));
            maxHostConnections = Integer.parseInt(props.getProperty("alfresco.maxHostConnections", "40"));
            socketTimeout = Integer.parseInt(props.getProperty("alfresco.socketTimeout", "60000"));

            client = new SOLRAPIClient(getRepoClient(keyResourceLoader), dictionaryService, namespaceDAO, compression);
            setCachedClient(alfrescoHost, alfrescoPort, alfrescoPortSSL, client);
        }

        return client;
    }
    
    protected AlfrescoHttpClient getRepoClient(KeyResourceLoader keyResourceLoader)
    {
        HttpClientFactory httpClientFactory = null;

        if (secureCommsType.equals("https"))
        {
            KeyStoreParameters keyStoreParameters = new KeyStoreParameters("ssl-keystore", "SSL Key Store",
                    sslKeyStoreType, sslKeyStoreProvider, sslKeyStorePasswordFileLocation, sslKeyStoreLocation);
            KeyStoreParameters trustStoreParameters = new KeyStoreParameters("ssl-truststore", "SSL Trust Store",
                    sslTrustStoreType, sslTrustStoreProvider, sslTrustStorePasswordFileLocation, sslTrustStoreLocation);
            SSLEncryptionParameters sslEncryptionParameters = new SSLEncryptionParameters(keyStoreParameters,
                    trustStoreParameters);
            httpClientFactory = new HttpClientFactory(SecureCommsType.getType(secureCommsType), sslEncryptionParameters,
                    keyResourceLoader, null, null, alfrescoHost, alfrescoPort, alfrescoPortSSL, maxTotalConnections,
                    maxHostConnections, socketTimeout);
        }
        else if (secureCommsType.equals("apikey"))
        {
            httpClientFactory = new ApiKeyHttpClientFactory(alfrescoHost, alfrescoPort, alfrescoApiKey, apiKeyHeader,
                    maxTotalConnections, maxHostConnections, socketTimeout);
        }
        else
        {
            throw new IllegalArgumentException("Invalid value for alfresco.secureComms");
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
    class ApiKeyHttpClientFactory extends HttpClientFactory
    {

        private String apiKeyHeader;

        private String apiKey;

        public ApiKeyHttpClientFactory(String host, int port, String apiKeyHeader, String apiKey, int maxTotalConnections, int maxHostConnections, int socketTimeout)
        {
            setSecureCommsType("none");
            setHost(host);
            setPort(port);
            setMaxTotalConnections(maxTotalConnections);
            setMaxHostConnections(maxHostConnections);
            setSocketTimeout(socketTimeout);
            apiKeyHeader = apiKeyHeader;
            apiKey = apiKey;
            init();
        }

        @Override
        public void init()
        {
            DefaultHttpParams.setHttpParamsFactory(new NonBlockingHttpParamsFactory());
        }

        @Override
        protected HttpClient getDefaultHttpClient(String httpHost, int httpPort)
        {
            HttpClient result = super.getDefaultHttpClient(httpHost, httpPort);
            HostParams hostParams = result.getHostConfiguration().getParams();
            ArrayList<Header> defaultHeaders = new ArrayList<>();
            defaultHeaders.add(new Header(apiKeyHeader, apiKey));
            if(hostParams.getParameter(HostParams.DEFAULT_HEADERS) != null)
            {
                defaultHeaders.addAll((Collection)hostParams.getParameter(HostParams.DEFAULT_HEADERS));
            }
            hostParams.setParameter(HostParams.DEFAULT_HEADERS, defaultHeaders);
            return result;
        }
    }

}

