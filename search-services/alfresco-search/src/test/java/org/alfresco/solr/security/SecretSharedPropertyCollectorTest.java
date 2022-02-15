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

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.MockedStatic;

import java.util.Properties;
import java.util.Set;

import static java.util.Collections.emptySet;
import static org.alfresco.solr.security.SecretSharedPropertyCollector.SECRET_SHARED_METHOD_KEY;
import static org.alfresco.solr.security.SecretSharedPropertyCollector.SECURE_COMMS_PROPERTY;
import static org.alfresco.solr.security.SecretSharedPropertyCollector.SHARED_SECRET;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mockStatic;

public class SecretSharedPropertyCollectorTest
{
    private static final String A_COMMS_METHOD = "aCommsMethod";
    private static final String SET_THROUGH_SYSTEM_PROPERTY = "aCommsMethod_SetThroughSystemProperty";
    private static final String SET_THROUGH_ALFRESCO_COMMON_CONFIG = "aCommsMethod_SetThroughAlfrescoCommonConfig";
    private static final String COMMS_METHOD_FROM_SOLRCORE = "aCommsMethod_FromSolrCore";
    private static final String SECRET_VALUE = "my-secret";

    @Before
    public void setUp()
    {
        SecretSharedPropertyCollector.commsMethod = null;
        assertNull(System.getProperty(SHARED_SECRET));
        assertNull(System.getProperty(SECURE_COMMS_PROPERTY));
        assertNull(AlfrescoSolrDataModel.getCommonConfig().getProperty(SECURE_COMMS_PROPERTY));
    }

    @After
    public void tearDown()
    {
        System.clearProperty(SHARED_SECRET);
        System.clearProperty(SECURE_COMMS_PROPERTY);
        AlfrescoSolrDataModel.getCommonConfig().remove(SECURE_COMMS_PROPERTY);
    }

    @Test
    public void getSecret_shouldReturnTheSecretValue()
    {
        System.setProperty(SecretSharedPropertyCollector.SHARED_SECRET, SECRET_VALUE);
        assertEquals(SECRET_VALUE, SecretSharedPropertyCollector.getSecret());
    }

    @Test(expected = RuntimeException.class)
    public void getSecretWithMissingSecretValue_shouldThrowException()
    {
        SecretSharedPropertyCollector.getSecret();
    }

    @Test
    public void commsMethodIsNotNull_shouldReturnThatValue()
    {
        SecretSharedPropertyCollector.commsMethod = A_COMMS_METHOD;
        assertEquals(A_COMMS_METHOD, SecretSharedPropertyCollector.getCommsMethod());

        assertFalse(SecretSharedPropertyCollector.isCommsSecretShared());
    }

    @Test
    public void commsMethodIsNotNullAndIsSecret_shouldReturnThatValue()
    {
        SecretSharedPropertyCollector.commsMethod = SECRET_SHARED_METHOD_KEY;
        assertEquals(SECRET_SHARED_METHOD_KEY, SecretSharedPropertyCollector.getCommsMethod());

        assertTrue(SecretSharedPropertyCollector.isCommsSecretShared());
    }

    @Test
    public void commsMethodThroughSystemProperty()
    {
        System.setProperty(SECURE_COMMS_PROPERTY, SET_THROUGH_SYSTEM_PROPERTY);
        assertEquals(SET_THROUGH_SYSTEM_PROPERTY, SecretSharedPropertyCollector.getCommsMethod());

        assertFalse(SecretSharedPropertyCollector.isCommsSecretShared());
    }

    @Test
    public void commsMethodSetToSecretThroughSystemProperty()
    {
        System.setProperty(SECURE_COMMS_PROPERTY, SECRET_SHARED_METHOD_KEY);
        assertEquals(SECRET_SHARED_METHOD_KEY, SecretSharedPropertyCollector.getCommsMethod());

        assertTrue(SecretSharedPropertyCollector.isCommsSecretShared());
    }

    @Test
    public void commsMethodThroughAlfrescoProperties()
    {
        try(MockedStatic<AlfrescoSolrDataModel> mock = mockStatic(AlfrescoSolrDataModel.class))
        {
            var alfrescoCommonConfig = new Properties();
            alfrescoCommonConfig.setProperty(SECURE_COMMS_PROPERTY, SET_THROUGH_ALFRESCO_COMMON_CONFIG);

            mock.when(AlfrescoSolrDataModel::getCommonConfig).thenReturn(alfrescoCommonConfig);
            assertEquals(SET_THROUGH_ALFRESCO_COMMON_CONFIG, SecretSharedPropertyCollector.getCommsMethod());

            assertFalse(SecretSharedPropertyCollector.isCommsSecretShared());
        }
    }

    @Test
    public void commsMethodThroughSolrCores()
    {
        try(MockedStatic<SecretSharedPropertyHelper> mock = mockStatic(SecretSharedPropertyHelper.class))
        {
            mock.when(SecretSharedPropertyHelper::getCommsFromCores).thenReturn(Set.of(COMMS_METHOD_FROM_SOLRCORE));
            assertEquals(COMMS_METHOD_FROM_SOLRCORE, SecretSharedPropertyCollector.getCommsMethod());

            assertFalse(SecretSharedPropertyCollector.isCommsSecretShared());
        }
    }

    /**
     * In case no core has been defined in the Solr instance, no comms method
     * can be defined (assuming that value cannot be retrieved from configuration
     * or system properties).
     *
     * @see <a href="https://alfresco.atlassian.net/browse/SEARCH-2985">SEARCH-2985</a>
     */
    @Test
    public void commsMethodThroughSolrCoresReturnEmptySet()
    {
        try(MockedStatic<SecretSharedPropertyHelper> mock = mockStatic(SecretSharedPropertyHelper.class))
        {
            mock.when(SecretSharedPropertyHelper::getCommsFromCores).thenReturn(emptySet());
            assertNull(SecretSharedPropertyCollector.getCommsMethod());

            assertFalse(SecretSharedPropertyCollector.isCommsSecretShared());
        }
    }

    /**
     * When multiple solr cores are defined and they are using the shared secret,
     * they are all supposed to use the same communication method.
     */
    @Test(expected = RuntimeException.class)
    public void commsMethodThroughSolrCoresReturnsMoreThanOneValue()
    {
        try(MockedStatic<SecretSharedPropertyHelper> mock = mockStatic(SecretSharedPropertyHelper.class))
        {
            mock.when(SecretSharedPropertyHelper::getCommsFromCores)
                    .thenReturn(Set.of(COMMS_METHOD_FROM_SOLRCORE, SECRET_SHARED_METHOD_KEY));

            SecretSharedPropertyCollector.getCommsMethod();
        }
    }

}
