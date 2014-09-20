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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.alfresco.encryption.KeyResourceLoader;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.solr.client.SOLRAPIClient;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class SOLRAPIClientFactoryTest
{
    @Mock
    private Properties props;
    @Mock
    private KeyResourceLoader keyResourceLoader;
    @Mock
    private DictionaryService dictionaryService;
    @Mock
    private NamespaceDAO namespaceDAO;
    private SOLRAPIClientFactory factory;

    @Before
    public void setUp() throws Exception
    {
        when(props.getProperty("alfresco.host", "localhost")).thenReturn("localhost");
        when(props.getProperty("alfresco.port", "8080")).thenReturn("8080");
        when(props.getProperty("alfresco.port.ssl", "8443")).thenReturn("8443");
        when(props.getProperty("alfresco.maxTotalConnections", "40")).thenReturn("40");
        when(props.getProperty("alfresco.maxHostConnections", "40")).thenReturn("40");
        when(props.getProperty("alfresco.socketTimeout", "60000")).thenReturn("0");
        when(props.getProperty("alfresco.secureComms", "none")).thenReturn("none");
        when(props.getProperty("alfresco.encryption.ssl.keystore.location",
                        "ssl.repo.client.keystore")).thenReturn("ssl.repo.client.keystore");
        when(props.getProperty("alfresco.encryption.ssl.truststore.location",
                        "ssl.repo.client.truststore")).thenReturn("ssl.repo.client.truststore");

        this.factory = new SOLRAPIClientFactory();
    }

    @Test
    public void getsSameSOLRAPIClientForSameAlfresco()
    {
        SOLRAPIClient solrapiClient = factory.getSOLRAPIClient(props, keyResourceLoader, dictionaryService,
                    namespaceDAO);
        assertNotNull(solrapiClient);
        
        SOLRAPIClient solrapiClient2 = factory.getSOLRAPIClient(props, keyResourceLoader, dictionaryService,
                    namespaceDAO);
        assertNotNull(solrapiClient2);
        
        assertSame(solrapiClient, solrapiClient2);
    }

    @Test
    public void getsDifferentSOLRAPIClientForDifferentAlfresco()
    {
        SOLRAPIClient solrapiClient = factory.getSOLRAPIClient(props, keyResourceLoader, dictionaryService,
                    namespaceDAO);
        assertNotNull(solrapiClient);
        
        when(props.getProperty("alfresco.port.ssl", "8443")).thenReturn("8444");
        SOLRAPIClient solrapiClient2 = factory.getSOLRAPIClient(props, keyResourceLoader, dictionaryService,
                    namespaceDAO);
        assertNotNull(solrapiClient2);
        
        assertNotSame(solrapiClient, solrapiClient2);
    }
}
