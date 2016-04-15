/*
 * #%L
 * Alfresco Solr Client
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

import static org.mockito.Mockito.*;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;

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
        when(props.getProperty(eq("alfresco.host"), anyString())).thenReturn("localhost");
        when(props.getProperty(eq("alfresco.port"), anyString())).thenReturn("8080");
        when(props.getProperty(eq("alfresco.port.ssl"), anyString())).thenReturn("8443");
        when(props.getProperty(eq("alfresco.maxTotalConnections"), anyString())).thenReturn("40");
        when(props.getProperty(eq("alfresco.maxHostConnections"), anyString())).thenReturn("40");
        when(props.getProperty(eq("alfresco.socketTimeout"), anyString())).thenReturn("0");
        when(props.getProperty(eq("alfresco.secureComms"), anyString())).thenReturn("none");
        when(props.getProperty(eq("alfresco.encryption.ssl.keystore.location"), anyString()))
            .thenReturn("ssl.repo.client.keystore");
        when(props.getProperty(eq("alfresco.encryption.ssl.truststore.location"), anyString()))
            .thenReturn("ssl.repo.client.truststore");

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
