/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail. Otherwise, the software is
 * provided under the following open source license terms:
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.apache.solr.handler.component;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.FacetComponent.FacetBase;
import org.apache.solr.handler.component.FacetComponent.FacetContext;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.BasicResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

/**
 * Unit tests for the {@link AlfrescoSearchHandler} for Facet Queries requests.
 */
public class AlfrescoSearchHandlerFacetQueryIT
{

    @Mock
    NamedList<NamedList<Object>> mockParams;
    @Mock
    SolrQueryResponse mockResponse;
    @Mock
    SolrQueryRequest mockRequest;
    @Mock
    BasicResultContext mockResultContext;
    @Mock
    FacetContext mockFacetContext;
    @Mock
    Map<Object, Object> mockContext;

    @Before
    public void setUp()
    {
        initMocks(this);
        when(mockResponse.getValues()).thenReturn(mockParams);
        when(mockResponse.getResponse()).thenReturn(mockResultContext);
        when(mockResultContext.getRequest()).thenReturn(mockRequest);
        when(mockRequest.getContext()).thenReturn(mockContext);
        when(mockContext.get(AlfrescoSearchHandler.FACET_CONTEXT_KEY)).thenReturn(mockFacetContext);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testKeysWithCountZeroAreRemoved()
    {

        NamedList<Object> facetQueries = new NamedList<>();
        facetQueries.add("small", 1);
        facetQueries.add("medium", 0);
        facetQueries.add("big", 0);

        NamedList<Object> facetCounts = new NamedList<>();
        facetCounts.add(FacetComponent.FACET_QUERY_KEY, facetQueries);

        when(mockParams.get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).thenReturn(facetCounts);

        List<FacetBase> queryFacets = new ArrayList<>();
        when(mockFacetContext.getAllQueryFacets()).thenReturn(queryFacets);

        AlfrescoSearchHandler.removeFacetQueriesWithCountZero(mockResponse);

        assertEquals(((NamedList<Object>) ((NamedList<Object>) mockResponse.getValues()
                .get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).get(FacetComponent.FACET_QUERY_KEY)).size(), 1);
        assertEquals(((NamedList<Object>) ((NamedList<Object>) mockResponse.getValues()
                .get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).getVal(0)).get("small"), 1);

    }

    @SuppressWarnings("unchecked")
    @Test
    public void testKeysWithCountNonZeroArePresent()
    {

        NamedList<Object> facetQueries = new NamedList<>();
        facetQueries.add("small", 1);
        facetQueries.add("medium", 2);
        facetQueries.add("big", 10);

        NamedList<Object> facetCounts = new NamedList<>();
        facetCounts.add(FacetComponent.FACET_QUERY_KEY, facetQueries);

        when(mockParams.get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).thenReturn(facetCounts);

        List<FacetBase> queryFacets = new ArrayList<>();
        when(mockFacetContext.getAllQueryFacets()).thenReturn(queryFacets);

        AlfrescoSearchHandler.removeFacetQueriesWithCountZero(mockResponse);

        assertEquals(((NamedList<Object>) ((NamedList<Object>) mockResponse.getValues()
                .get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).get(FacetComponent.FACET_QUERY_KEY)).size(), 3);
        assertEquals(((NamedList<Object>) ((NamedList<Object>) mockResponse.getValues()
                .get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).getVal(0)).get("small"), 1);
        assertEquals(((NamedList<Object>) ((NamedList<Object>) mockResponse.getValues()
                .get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).getVal(0)).get("medium"), 2);
        assertEquals(((NamedList<Object>) ((NamedList<Object>) mockResponse.getValues()
                .get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).getVal(0)).get("big"), 10);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEmptyFacetQueries()
    {

        NamedList<Object> facetQueries = new NamedList<>();

        NamedList<Object> facetCounts = new NamedList<>();
        facetCounts.add(FacetComponent.FACET_QUERY_KEY, facetQueries);

        when(mockParams.get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).thenReturn(facetCounts);

        List<FacetBase> queryFacets = new ArrayList<>();
        when(mockFacetContext.getAllQueryFacets()).thenReturn(queryFacets);

        AlfrescoSearchHandler.removeFacetQueriesWithCountZero(mockResponse);

        assertEquals(((NamedList<Object>) ((NamedList<Object>) mockResponse.getValues()
                .get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).get(FacetComponent.FACET_QUERY_KEY)).size(), 0);
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testEmptyFacetCount()
    {

        NamedList<Object> facetCounts = new NamedList<>();

        when(mockParams.get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).thenReturn(facetCounts);

        List<FacetBase> queryFacets = new ArrayList<>();
        when(mockFacetContext.getAllQueryFacets()).thenReturn(queryFacets);

        AlfrescoSearchHandler.removeFacetQueriesWithCountZero(mockResponse);

        assertEquals(((NamedList<Object>) mockResponse.getValues().get(AlfrescoSearchHandler.FACET_COUNTS_KEY)).size(),
                0);
    }

}
