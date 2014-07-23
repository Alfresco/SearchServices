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
package org.alfresco.solr.client;

import java.util.Properties;

import org.alfresco.encryption.KeyResourceLoader;
import org.alfresco.encryption.KeyStoreParameters;
import org.alfresco.encryption.ssl.SSLEncryptionParameters;
import org.alfresco.httpclient.AlfrescoHttpClient;
import org.alfresco.httpclient.HttpClientFactory;
import org.alfresco.httpclient.HttpClientFactory.SecureCommsType;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.service.cmr.dictionary.DictionaryService;

public class SOLRAPIClientFactory
{

    // encryption related parameters
    private String secureCommsType; // "none", "https"
    private String keyStoreType;
    private String keyStoreProvider;
    private String passwordFileLocation;
    private String keyStoreLocation;
    
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
    
    
    public SOLRAPIClient getSOLRAPIClient(Properties p, KeyResourceLoader keyResourceLoader, DictionaryService dictionaryService, NamespaceDAO namespaceDAO)
    {
        alfrescoHost = p.getProperty("alfresco.host", "localhost");
        alfrescoPort = Integer.parseInt(p.getProperty("alfresco.port", "8080"));
        alfrescoPortSSL = Integer.parseInt(p.getProperty("alfresco.port.ssl", "8443"));
        baseUrl = p.getProperty("alfresco.baseUrl", "/alfresco");
        keyStoreType = p.getProperty("alfresco.encryption.keystore.type", "JCEKS");
        keyStoreProvider = p.getProperty("alfresco.encryption.keystore.provider");
        passwordFileLocation = p.getProperty("alfresco.encryption.keystore.passwordFileLocation");
        keyStoreLocation = p.getProperty("alfresco.encryption.keystore.location");
        sslKeyStoreType = p.getProperty("alfresco.encryption.ssl.keystore.type");
        sslKeyStoreProvider = p.getProperty("alfresco.encryption.ssl.keystore.provider", "");
        sslKeyStoreLocation = p.getProperty("alfresco.encryption.ssl.keystore.location", "ssl.repo.client.keystore");
        sslKeyStorePasswordFileLocation = p.getProperty("alfresco.encryption.ssl.keystore.passwordFileLocation", "ssl-keystore-passwords.properties");
        sslTrustStoreType = p.getProperty("alfresco.encryption.ssl.truststore.type", "JCEKS");
        sslTrustStoreProvider = p.getProperty("alfresco.encryption.ssl.truststore.provider", "");
        sslTrustStoreLocation = p.getProperty("alfresco.encryption.ssl.truststore.location", "ssl.repo.client.truststore");
        sslTrustStorePasswordFileLocation = p.getProperty("alfresco.encryption.ssl.truststore.passwordFileLocation", "ssl-truststore-passwords.properties");
        secureCommsType = p.getProperty("alfresco.secureComms", "https");
        maxTotalConnections = Integer.parseInt(p.getProperty("alfresco.maxTotalConnections", "40"));
        maxHostConnections = Integer.parseInt(p.getProperty("alfresco.maxHostConnections", "40"));
        socketTimeout = Integer.parseInt(p.getProperty("alfresco.socketTimeout", "0"));
        
        return new SOLRAPIClient(getRepoClient(keyResourceLoader), dictionaryService, namespaceDAO);
    }
    
    protected AlfrescoHttpClient getRepoClient(KeyResourceLoader keyResourceLoader)
    {
        // TODO i18n
        KeyStoreParameters keyStoreParameters = new KeyStoreParameters("SSL Key Store", sslKeyStoreType, sslKeyStoreProvider, sslKeyStorePasswordFileLocation, sslKeyStoreLocation);
        KeyStoreParameters trustStoreParameters = new KeyStoreParameters("SSL Trust Store", sslTrustStoreType, sslTrustStoreProvider, sslTrustStorePasswordFileLocation, sslTrustStoreLocation);
        SSLEncryptionParameters sslEncryptionParameters = new SSLEncryptionParameters(keyStoreParameters, trustStoreParameters);
        
        HttpClientFactory httpClientFactory = new HttpClientFactory(SecureCommsType.getType(secureCommsType),
                sslEncryptionParameters, keyResourceLoader, null, null, alfrescoHost, alfrescoPort, alfrescoPortSSL, maxTotalConnections, maxHostConnections, socketTimeout);
        // TODO need to make port configurable depending on secure comms, or just make redirects
        // work
        AlfrescoHttpClient repoClient = httpClientFactory.getRepoClient(alfrescoHost, alfrescoPortSSL);
        repoClient.setBaseUrl(baseUrl);
        return repoClient;
    }
}
