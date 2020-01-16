/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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

package org.alfresco.solr;

import java.util.Properties;

import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link SolrInformationServer} class.
 *
 * @author Matt Ward
 * @author Andrea Gazzarini
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrInformationServerTest
{
    private SolrInformationServer infoServer;

    @Mock
    private AlfrescoCoreAdminHandler adminHandler;

    @Mock
    private SolrResourceLoader resourceLoader;

    @Mock
    private SolrCore core;

    @Mock
    private SOLRAPIClient client;

    @Mock
    private SolrRequestHandler handler;

    @Mock
    private SolrQueryResponse response;

    private SolrQueryRequest request;

    @Before
    public void setUp()
    {
        when(core.getResourceLoader()).thenReturn(resourceLoader);
        when(resourceLoader.getCoreProperties()).thenReturn(new Properties());
        infoServer = new SolrInformationServer(adminHandler, core, client)
        {
            // @Override
            SolrQueryResponse newSolrQueryResponse()
            {
                return response;
            }
        };

        request = infoServer.newSolrQueryRequest();
    }

    @Test
    public void testGetStateOk()
    {
        String id = String.valueOf(System.currentTimeMillis());

        SolrDocument state = new SolrDocument();

        SimpleOrderedMap<SolrDocument> responseContent = new SimpleOrderedMap<>();
        responseContent.add(SolrInformationServer.RESPONSE_DEFAULT_ID, state);

        when(response.getValues()).thenReturn(responseContent);
        when(core.getRequestHandler(SolrInformationServer.REQUEST_HANDLER_GET)).thenReturn(handler);

        SolrDocument document = infoServer.getState(core, request, id);

        assertEquals(id, request.getParams().get(CommonParams.ID));
        verify(core).getRequestHandler(SolrInformationServer.REQUEST_HANDLER_GET);
        verify(response).getValues();

        assertSame(state, document);
    }

    /**
     * GetState returns null in case the given id doesn't correspond to an existing state document.
     */
    @Test
    public void testGetStateWithStateNotFound_returnsNull()
    {
        String id = String.valueOf(System.currentTimeMillis());

        SimpleOrderedMap<Object> responseContent = new SimpleOrderedMap<>();
        responseContent.add(SolrInformationServer.RESPONSE_DEFAULT_ID, null);

        when(response.getValues()).thenReturn(responseContent);
        when(core.getRequestHandler(SolrInformationServer.REQUEST_HANDLER_GET)).thenReturn(handler);

        SolrDocument document = infoServer.getState(core, request, id);

        assertEquals(id, request.getParams().get(CommonParams.ID));
        verify(core).getRequestHandler(SolrInformationServer.REQUEST_HANDLER_GET);
        verify(response).getValues();

        assertNull(document);
    }
}
