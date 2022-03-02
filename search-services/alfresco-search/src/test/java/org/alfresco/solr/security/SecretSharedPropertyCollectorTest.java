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

import static org.alfresco.solr.security.SecretSharedPropertyCollector.ALLOW_UNAUTHENTICATED_SOLR_PROPERTY;
import static org.alfresco.solr.security.SecretSharedPropertyCollector.PROPS_CACHE;
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
    private static final String SECURE_COMMS_NONE = "none";
    private static final String TRUE = "true";
    private static final String FALSE = "false";

    private static final Set<String> PROPS_TO_CLEAR = Set.of(SHARED_SECRET, SECURE_COMMS_PROPERTY, ALLOW_UNAUTHENTICATED_SOLR_PROPERTY);

    @Before
    public void setUp()
    {
        PROPS_CACHE.clear();

        for (String property : PROPS_TO_CLEAR)
        {
            assertNull(System.getProperty(property));
            assertNull(AlfrescoSolrDataModel.getCommonConfig().getProperty(property));
        }
    }

    @After
    public void tearDown()
    {
        for (String property : PROPS_TO_CLEAR)
        {
            System.clearProperty(property);
            AlfrescoSolrDataModel.getCommonConfig().remove(property);
        }
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
    public void allowUnauthenticatedSolrIsNotSet_shouldReturnFalse()
    {
        try(MockedStatic<SecretSharedPropertyHelper> mock = mockStatic(SecretSharedPropertyHelper.class))
        {
            mock.when(() -> SecretSharedPropertyHelper.getPropertyFromCores(ALLOW_UNAUTHENTICATED_SOLR_PROPERTY, FALSE))
                .thenReturn(emptySet());
            assertFalse(SecretSharedPropertyCollector.isAllowUnauthenticatedSolrEndpoint());
        }
    }

    @Test
    public void allowUnauthenticatedSolrIsTrueThroughSystemProperty_shouldReturnTrue()
    {
        System.setProperty(ALLOW_UNAUTHENTICATED_SOLR_PROPERTY, TRUE);
        assertTrue(SecretSharedPropertyCollector.isAllowUnauthenticatedSolrEndpoint());
    }

    @Test
    public void allowUnauthenticatedSolrIsFalseThroughSystemProperty_shouldReturnFalse()
    {
        System.setProperty(ALLOW_UNAUTHENTICATED_SOLR_PROPERTY, FALSE);
        assertFalse(SecretSharedPropertyCollector.isAllowUnauthenticatedSolrEndpoint());
    }

    @Test
    public void allowUnauthenticatedSolrIsTrueThroughAlfrescoProperties_shouldReturnTrue()
    {
        try(MockedStatic<AlfrescoSolrDataModel> mock = mockStatic(AlfrescoSolrDataModel.class))
        {
            var alfrescoCommonConfig = new Properties();
            alfrescoCommonConfig.setProperty(ALLOW_UNAUTHENTICATED_SOLR_PROPERTY, TRUE);

            mock.when(AlfrescoSolrDataModel::getCommonConfig).thenReturn(alfrescoCommonConfig);

            assertTrue(SecretSharedPropertyCollector.isAllowUnauthenticatedSolrEndpoint());
        }
    }

    @Test
    public void allowUnauthenticatedSolrIsFalseThroughAlfrescoProperties_shouldReturnFalse()
    {
        try(MockedStatic<AlfrescoSolrDataModel> mock = mockStatic(AlfrescoSolrDataModel.class))
        {
            var alfrescoCommonConfig = new Properties();
            alfrescoCommonConfig.setProperty(ALLOW_UNAUTHENTICATED_SOLR_PROPERTY, FALSE);

            mock.when(AlfrescoSolrDataModel::getCommonConfig).thenReturn(alfrescoCommonConfig);

            assertFalse(SecretSharedPropertyCollector.isAllowUnauthenticatedSolrEndpoint());
        }
    }

    @Test
    public void allowUnauthenticatedSolrIsTrueThroughSolrCores_shouldReturnTrue()
    {
        try(MockedStatic<SecretSharedPropertyHelper> mock = mockStatic(SecretSharedPropertyHelper.class))
        {
            mock.when(() -> SecretSharedPropertyHelper.getPropertyFromCores(ALLOW_UNAUTHENTICATED_SOLR_PROPERTY, FALSE))
                .thenReturn(Set.of(TRUE));

            assertTrue(SecretSharedPropertyCollector.isAllowUnauthenticatedSolrEndpoint());
        }
    }

    @Test
    public void allowUnauthenticatedSolrIsFalseThroughSolrCores_shouldReturnFalse()
    {
        try(MockedStatic<SecretSharedPropertyHelper> mock = mockStatic(SecretSharedPropertyHelper.class))
        {
            mock.when(() -> SecretSharedPropertyHelper.getPropertyFromCores(ALLOW_UNAUTHENTICATED_SOLR_PROPERTY, FALSE))
                .thenReturn(Set.of(FALSE));

            assertFalse(SecretSharedPropertyCollector.isAllowUnauthenticatedSolrEndpoint());
        }
    }

    @Test
    public void commsMethodIsNotNull_shouldReturnThatValue()
    {
        PROPS_CACHE.put(SECURE_COMMS_PROPERTY, A_COMMS_METHOD);
        assertEquals(A_COMMS_METHOD, SecretSharedPropertyCollector.getCommsMethod());

        assertFalse(SecretSharedPropertyCollector.isCommsSecretShared());
    }

    @Test
    public void commsMethodIsNotNullAndIsSecret_shouldReturnThatValue()
    {
        PROPS_CACHE.put(SECURE_COMMS_PROPERTY, SECRET_SHARED_METHOD_KEY);
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
            mock.when(() -> SecretSharedPropertyHelper.getPropertyFromCores(SECURE_COMMS_PROPERTY, SECURE_COMMS_NONE))
                .thenReturn(Set.of(COMMS_METHOD_FROM_SOLRCORE));
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
            mock.when(() -> SecretSharedPropertyHelper.getPropertyFromCores(SECURE_COMMS_PROPERTY, SECURE_COMMS_NONE))
                .thenReturn(emptySet());
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
            mock.when(() -> SecretSharedPropertyHelper.getPropertyFromCores(SECURE_COMMS_PROPERTY, SECURE_COMMS_NONE))
                .thenReturn(Set.of(COMMS_METHOD_FROM_SOLRCORE, SECRET_SHARED_METHOD_KEY));

            SecretSharedPropertyCollector.getCommsMethod();
        }
    }

}
