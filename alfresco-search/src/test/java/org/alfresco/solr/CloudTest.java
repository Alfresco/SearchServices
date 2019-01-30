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
package org.alfresco.solr;

public class CloudTest
{
    /*
    public final static String QUERY = "a query";
    private Cloud cloud = new Cloud();
    @Mock SolrQueryRequest request;
    
    @SuppressWarnings({ "rawtypes" })
    @Test
    public void testGetQueryWithZeroValues()
    {
        String fieldName = FIELD_DBID;
        String operator = SolrInformationServer.AND;
        Collection[] valueLists = new Collection[1];
        valueLists[0] = new ArrayList();
        String query = cloud.getQuery(fieldName, operator, valueLists);
        assertEquals("", query);
        
        valueLists = new Collection[0];
        query = cloud.getQuery(fieldName, operator, valueLists);
        assertEquals("", query);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testGetQueryWithOneValue()
    {
        String fieldName = FIELD_DBID;
        String operator = SolrInformationServer.AND;
        Collection[] valueLists = new Collection[1];
        valueLists[0] = new ArrayList();
        Object value = "value";
        valueLists[0].add(value);
        String query = cloud.getQuery(fieldName, operator, valueLists);
        assertEquals(FIELD_DBID + ":" + value, query);
    }

    @SuppressWarnings({ "rawtypes", "unchecked" })
    @Test
    public void testGetQueryWithManyValues()
    {
        String fieldName = FIELD_DBID;
        String operator = SolrInformationServer.AND;
        Collection[] valueLists = new Collection[2];
        valueLists[0] = new ArrayList();
        Object value1 = "value1";
        valueLists[0].add(value1);
        Object value2 = "value2";
        valueLists[0].add(value2);
        Object value3 = "value3";
        valueLists[0].add(value3);
        valueLists[1] = new ArrayList();
        Object value4 = "value4";
        valueLists[1].add(value4);
        String query = cloud.getQuery(fieldName, operator, valueLists);
        assertEquals(FIELD_DBID + ":" + value1 + operator + FIELD_DBID + ":" + value2 + operator
                    + FIELD_DBID + ":" + value3 + operator + FIELD_DBID + ":" + value4, query);
    }
    
    @Test
    public void testExists()
    {
        boolean exists = cloud.exists(super.selectRequestHandler, request, QUERY);
        assertFalse(exists);
    }

    @Test
    public void testGetDocList()
    {
        // The response is created in the getDocList method, and the aftsRequestHandler is simply mocked.
        // Therefore nothing is expected to be on the response for tests, so this verifies behavior.
        DocList docList = cloud.getDocList(super.aftsRequestHandler, request, QUERY);
        assertNull(docList);
        
        ArgumentCaptor<SolrQueryResponse> response = ArgumentCaptor.forClass(SolrQueryResponse.class);
        verify(super.aftsRequestHandler).handleRequest(eq(request), response.capture()); 
        assertNotNull(response.getValue());
    }

    @Test
    public void testSelect()
    {
        SolrParams params = new ModifiableSolrParams(request.getParams());
        ResultContext rc = cloud.getResultContext(super.aftsRequestHandler, request, params);
        assertNull(rc);
    }
    */

}
