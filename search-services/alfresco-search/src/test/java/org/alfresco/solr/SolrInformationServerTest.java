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

package org.alfresco.solr;

import static java.util.Optional.ofNullable;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.ANY;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.ASSOC_REF;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.BOOLEAN;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.CATEGORY;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.CHILD_ASSOC_REF;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.CONTENT;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.DOUBLE;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.ENCRYPTED;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.FLOAT;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.INT;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.LOCALE;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.LONG;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.MLTEXT;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.NODE_REF;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.PATH;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.PERIOD;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.QNAME;
import static org.alfresco.service.cmr.dictionary.DataTypeDefinition.TEXT;
import static org.alfresco.solr.SolrInformationServer.REQUEST_HANDLER_GET;
import static org.alfresco.solr.SolrInformationServer.REQUEST_HANDLER_NATIVE;
import static org.alfresco.solr.SolrInformationServer.RESPONSE_DEFAULT_ID;
import static org.alfresco.solr.SolrInformationServer.RESPONSE_DEFAULT_IDS;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_DAY_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_HOUR_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_MINUTE_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_MONTH_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_QUARTER_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_SECOND_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_YEAR_FIELD_SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.QueryConstants;
import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.NodeMetaDataParameters;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.client.StringPropertyValue;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.BasicResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocSlice;
import org.apache.solr.update.AddUpdateCommand;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

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

    @Mock
    private UpdateRequestProcessorChain updateRequestProcessorChain;

    @Mock
    private UpdateRequestProcessor updateRequestProcessor;

    @Before
    public void setUp()
    {
        when(core.getResourceLoader()).thenReturn(resourceLoader);
        when(core.getRequestHandler(REQUEST_HANDLER_GET)).thenReturn(handler);
        when(core.getRequestHandler(REQUEST_HANDLER_NATIVE)).thenReturn(handler);
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

        // mock updateProcessingChain -> createProcessor -> processAdd method calls
        when(core.getUpdateProcessingChain(null)).thenReturn(updateRequestProcessorChain);
        when(updateRequestProcessorChain.createProcessor(any(),any())).thenReturn(updateRequestProcessor);
    }

    @Test
    public void testGetStateOk()
    {
        String id = String.valueOf(System.currentTimeMillis());

        SolrDocument state = new SolrDocument();

        SimpleOrderedMap<SolrDocument> responseContent = new SimpleOrderedMap<>();
        responseContent.add(RESPONSE_DEFAULT_ID, state);

        when(response.getValues()).thenReturn(responseContent);

        SolrDocument document = infoServer.getState(core, request, id);

        assertEquals(id, request.getParams().get(CommonParams.ID));
        verify(core).getRequestHandler(REQUEST_HANDLER_GET);
        verify(response).getValues();

        assertSame(state, document);
    }

    @Test
    public void setUnitOfTimeFieldsWithDatetimeField_shouldSetDateAndTimeFields()
    {
        DataTypeDefinition datatype = mock(DataTypeDefinition.class);
        when(datatype.getName()).thenReturn(DataTypeDefinition.DATETIME);

        SolrInputDocument document = new SolrInputDocument();

        String fieldPrefix = "datetime@sd@";
        String fieldSuffix = "{http://www.alfresco.org/model/content/1.0}created";
        String sourceFieldName = fieldPrefix + fieldSuffix;

        infoServer.setUnitOfTimeFields(document::setField, sourceFieldName,  "1972-09-16T17:33:18Z", datatype);

        assertEquals(1972,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_YEAR_FIELD_SUFFIX));
        assertEquals(3,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_QUARTER_FIELD_SUFFIX));
        assertEquals(9,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_MONTH_FIELD_SUFFIX));
        assertEquals(16,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_DAY_FIELD_SUFFIX));
        assertEquals(17,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_HOUR_FIELD_SUFFIX));
        assertEquals(33,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_MINUTE_FIELD_SUFFIX));
        assertEquals(18,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_SECOND_FIELD_SUFFIX));
    }

    @Test
    public void setUnitOfTimeFieldsWithDatetimeField_shouldSetOnlyDateFields()
    {
        DataTypeDefinition datatype = mock(DataTypeDefinition.class);
        when(datatype.getName()).thenReturn(DataTypeDefinition.DATE);

        SolrInputDocument document = new SolrInputDocument();

        String fieldPrefix = "datetime@sd@";
        String fieldSuffix = "{http://www.alfresco.org/model/content/1.0}created";
        String sourceFieldName = fieldPrefix + fieldSuffix;

        infoServer.setUnitOfTimeFields(document::setField, sourceFieldName,  "1972-09-16T17:33:18Z", datatype);

        assertEquals(1972,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_YEAR_FIELD_SUFFIX));
        assertEquals(3,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_QUARTER_FIELD_SUFFIX));
        assertEquals(9,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_MONTH_FIELD_SUFFIX));
        assertEquals(16,
                unitOfTimeFieldValue(document, AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_DAY_FIELD_SUFFIX));

        assertFalse(document.containsKey(AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_HOUR_FIELD_SUFFIX));
        assertFalse(document.containsKey(AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_MINUTE_FIELD_SUFFIX));
        assertFalse(document.containsKey(AlfrescoSolrDataModel.PART_FIELDNAME_PREFIX + fieldSuffix + UNIT_OF_TIME_SECOND_FIELD_SUFFIX));
    }

    @Test
    public void destructuringCannotBeAppliedToMultivaluedFields()
    {
        PropertyDefinition multiValuedProperty = mock(PropertyDefinition.class);
        when(multiValuedProperty.isMultiValued()).thenReturn(true);

        assertFalse(infoServer.canBeDestructured(multiValuedProperty, "datetime@sd@{http://www.alfresco.org/model/content/1.0}created"));
    }

    /**
     * Destructuring cannot only be applied to those fields matching a dynamic field in the schema.
     * The dynamic fields defined in the schema follow the naming:
     *
     * [datatype]@[options]@[namespace][local name]
     *
     * For example
     *
     * datetime@sd@{http://www.alfresco.org/model/content/1.0}created
     *
     * SearchServices calls those fields "PROPERTIES", while the static fields (even belonging to a Node) are called
     * "FIELDS".
     */
    @Test
    public void destructuringCannotBeAppliedToProperties()
    {
        PropertyDefinition multiValuedProperty = mock(PropertyDefinition.class);
        when(multiValuedProperty.isMultiValued()).thenReturn(false);

        assertFalse(
                "Destructuring can only be applied to Node properties (e.g. dynamic fields in Solr schema)",
                infoServer.canBeDestructured(multiValuedProperty, "this_is_a_field_that_doesnt_follow_the_dynamic_naming"));
    }

    @Test
    public void destructuringCanBeAppliedToDateFields()
    {
        PropertyDefinition propertyThatCanBeDestructured = mock(PropertyDefinition.class);
        when(propertyThatCanBeDestructured.isMultiValued()).thenReturn(false);

        DataTypeDefinition date = mock(DataTypeDefinition.class);
        when(date.getName()).thenReturn(DataTypeDefinition.DATE);
        when(propertyThatCanBeDestructured.getDataType()).thenReturn(date);

        assertTrue(
                "Destructuring must be supported in Date fields!",
                infoServer.canBeDestructured(propertyThatCanBeDestructured,"date@sd@{http://www.alfresco.org/model/content/1.0}created"));
    }

    @Test
    public void destructuringCanBeAppliedToDateTimeFields()
    {
        PropertyDefinition propertyThatCanBeDestructured = mock(PropertyDefinition.class);
        when(propertyThatCanBeDestructured.isMultiValued()).thenReturn(false);

        DataTypeDefinition datetime = mock(DataTypeDefinition.class);
        when(datetime.getName()).thenReturn(DataTypeDefinition.DATETIME);
        when(propertyThatCanBeDestructured.getDataType()).thenReturn(datetime);

        assertTrue(
                "Destructuring must be supported in Datetime fields!",
                infoServer.canBeDestructured(propertyThatCanBeDestructured, "datetime@sd@{http://www.alfresco.org/model/content/1.0}created"));
    }

    @Test
    public void destructuringCanBeAppliedOnlyToDateOrDatetimeFields()
    {
        Stream.of(ANY,ENCRYPTED,TEXT,MLTEXT,CONTENT,INT,LONG,FLOAT,DOUBLE,
                BOOLEAN,QNAME,CATEGORY,NODE_REF,CHILD_ASSOC_REF,ASSOC_REF,
                PATH,LOCALE,PERIOD)
                .map(qname -> {
                    PropertyDefinition propertyThatCannotBeDestructured = mock(PropertyDefinition.class);
                    DataTypeDefinition def = Mockito.mock(DataTypeDefinition.class);
                    when(def.getName()).thenReturn(qname);
                    when(propertyThatCannotBeDestructured.getDataType()).thenReturn(def);
                    return propertyThatCannotBeDestructured;})
                .forEach(property -> assertFalse(
                                "Destructuring must be supported only on Date or Datetime fields!",
                                infoServer.canBeDestructured(property, "somedatatype@sd@{http://www.alfresco.org/model/content/1.0}somefield")));
    }

    private int unitOfTimeFieldValue(SolrInputDocument doc, String fieldName) {
        return ofNullable(doc.getFieldValue(fieldName))
                    .map(Number.class::cast)
                    .map(Number::intValue)
                    .orElseThrow(() -> new IllegalArgumentException(fieldName + " hasn't been set."));
    }

    /**
     * GetState returns null in case the given id doesn't correspond to an existing state document.
     */
    @Test
    public void testGetStateWithStateNotFound_returnsNull()
    {
        String id = String.valueOf(System.currentTimeMillis());

        SimpleOrderedMap<Object> responseContent = new SimpleOrderedMap<>();
        responseContent.add(RESPONSE_DEFAULT_ID, null);

        when(response.getValues()).thenReturn(responseContent);

        SolrDocument document = infoServer.getState(core, request, id);

        assertEquals(id, request.getParams().get(CommonParams.ID));
        verify(core).getRequestHandler(REQUEST_HANDLER_GET);
        verify(response).getValues();

        assertNull(document);
    }
    
    /**
     * When storing ANAME and APATH fields, skipping to store duplicated entries should be granted.
     * 
     * Test data has been simplified from a living input use case:
     * 
     * {
     * "apath": "/9ea65c3c/1f5eebed/d657ec29/7c7da7c4/3ca56633/85f3f802/5c3a9e15/da781274",
     * "path": "/company_home/user_homes/user1/taskers/Tasker-1418058127641/responseSummary-1418332928505/responseSummary-1418332928552/response"
     * },
     * {
     * "apath": "/9ea65c3c/1f5eebed/d657ec29/572c38fc/4ff94a6e/85f3f802/5c3a9e15/da781274",
     * "path": "/company_home/user_homes/user2/taskers/tasker/responseSummary-1418332928505/responseSummary-1418332928552/response"
     * },
     * {
     * "apath": "/9ea65c3c/1f5eebed/d657ec29/cebd969b/0decd203/85f3f802/5c3a9e15/da781274",
     * "path": "/company_home/user_homes/user3/taskers/tasker/responseSummary-1418332928505/responseSummary-1418332928552/response"
     * }
     */
    @Test
    public void testPathsFieldStorage()
    {

        SolrInputDocument doc = new SolrInputDocument();
        NodeMetaData nodeMetaData = new NodeMetaData();
        nodeMetaData.setAncestorPaths(List.of("/1/2/4/7/10", "/1/2/5/8/10", "/1/2/6/9/10"));
        nodeMetaData.setPaths(List.of());
        
        List<String> expectedANames = Stream.of("0/10", 
                "1/7/10", "1/8/10", "1/9/10", 
                "2/4/7/10", "2/5/8/10", "2/6/9/10", 
                "3/2/4/7/10", "3/2/5/8/10", "3/2/6/9/10", 
                "4/1/2/4/7/10", "4/1/2/5/8/10", "4/1/2/6/9/10", 
                "F/1/2/4/7/10", "F/1/2/5/8/10", "F/1/2/6/9/10")
                .collect(Collectors.toCollection(ArrayList::new));
        
        List<String> expectedAPaths = Stream.of("0/1", 
                "1/1/2", 
                "2/1/2/4", "2/1/2/5", "2/1/2/6", 
                "3/1/2/4/7", "3/1/2/5/8", "3/1/2/6/9", 
                "4/1/2/4/7/10", "4/1/2/5/8/10", "4/1/2/6/9/10", 
                "F/1/2/4/7/10", "F/1/2/5/8/10", "F/1/2/6/9/10")
                .collect(Collectors.toCollection(ArrayList::new));
        
        infoServer.updatePathRelatedFields(nodeMetaData, doc);
        
        List<String> anames = doc.getFieldValues(QueryConstants.FIELD_ANAME).stream()
                .map(aname -> aname.toString())
                .collect(Collectors.toList())
                .stream()
                .sorted()
                .collect(Collectors.toList());
        
        assertEquals(expectedANames, anames);
                
        List<String> apaths = doc.getFieldValues(QueryConstants.FIELD_APATH).stream()
                .map(aname -> aname.toString())
                .collect(Collectors.toList())
                .stream()
                .sorted()
                .collect(Collectors.toList());
        
        assertEquals(expectedAPaths, apaths);


    }
    
    /**
     * Repeat the indexing operation 2 times to verify that updating and existing document 
     * removes previous information in ANAME and APATH fields
     */
    @Test
    public void testPathsFieldStorageIterations()
    {

        SolrInputDocument doc = new SolrInputDocument();
        NodeMetaData nodeMetaData = new NodeMetaData();
        nodeMetaData.setAncestorPaths(List.of("/1/2/4/7/10", "/1/2/5/8/10", "/1/2/6/9/10"));
        nodeMetaData.setPaths(List.of());
        
        IntStream.range(0, 2).forEach(i -> 
        {

            infoServer.updatePathRelatedFields(nodeMetaData, doc);
            
            assertEquals(doc.getFieldValues(QueryConstants.FIELD_ANAME).size(), 16);
            assertEquals(doc.getFieldValues(QueryConstants.FIELD_APATH).size(), 14);

        });

    }

    /** Check that the FTS report is derived from the correct parts of the Solr response. */
    @Test
    public void testAddContentOutdatedAndUpdatedCounts()
    {
        // Pretend that there are three documents in total.
        NamedList<Object> responseContent = new SimpleOrderedMap<>();
        DocList docList = mock(DocList.class);
        when(docList.matches()).thenReturn(3);
        BasicResultContext basicResultContext = mock(BasicResultContext.class);
        when(basicResultContext.getDocList()).thenReturn(docList);
        responseContent.add(RESPONSE_DEFAULT_IDS, basicResultContext);

        // Set the facet to say one document is outdated.
        NamedList<Number> facetQueries = new SimpleOrderedMap<>();
        facetQueries.add("OUTDATED", 1);
        NamedList<NamedList<Number>> facetCounts = new SimpleOrderedMap<>();
        facetCounts.add("facet_queries", facetQueries);
        responseContent.add("facet_counts", facetCounts);

        // Set up the request handler to return the fake response.
        doAnswer(invocation -> {
            SolrQueryResponse solrQueryResponse = invocation.getArgument(1);
            solrQueryResponse.setAllValues(responseContent);
            return null;
        }).when(handler).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));

        // Call the method under test.
        NamedList<Object> report = new NamedList<>();
        infoServer.addContentOutdatedAndUpdatedCounts(report);

        // Check the report.
        assertEquals("Expected two content nodes to be in sync.", report.get("Node count whose content is in sync"), 2L);
        assertEquals("Expected one content node to need an update.", report.get("Node count whose content needs to be updated"), 1L);
    }

    /** Test if a node with "cm:isIndexed"=false is ignored */
    @Test
    public void testUnindexedNode() throws IOException, AuthenticationException
    {
        // mocked node metadata
        NodeMetaData nodeMetaData = new NodeMetaData();
        nodeMetaData.setProperties(Map.of(ContentModel.PROP_IS_INDEXED, new StringPropertyValue("false")));
        nodeMetaData.setAspects(Set.of(ContentModel.ASPECT_INDEX_CONTROL));
        nodeMetaData.setType(ContentModel.TYPE_CONTENT);
        nodeMetaData.setNodeRef(new NodeRef("workspace://SpacesStore/f7c71f35-b592-40e2-a15f-fccbadd6b4d3"));

        when(client.getNodesMetaData(any(NodeMetaDataParameters.class))).thenReturn(Arrays.asList(nodeMetaData));

        // when calling ".indexNode" method, ".deleteErrorNode" private method will need some resultContext, preferably with docListSize=0
        // to ensure that, we need a "resultContext" with "matches"=0, which is created by having a "SolrQueryResponse" correctly setup,
        // when the requestHandler handles the request
        doAnswer(invocationOnMock -> {
            SolrQueryRequest request = invocationOnMock.getArgument(0);
            SolrQueryResponse response = invocationOnMock.getArgument(1);

            NamedList<Object> namedList = new NamedList<>();
            BasicResultContext rc = new BasicResultContext(new DocSlice(1, 1, new int[] {}, new float[] {}, 0, 0), null, null, null, request);
            namedList.add("response", rc);
            response.setAllValues(namedList);
            return null;
        }).when(handler).handleRequest(any(), any());

        // checks if "UpdateRequestProcessor#processAdd" arguments contains DOC_TYPE as UnindexedNode
        doAnswer(invocationOnMock -> {
            AddUpdateCommand cmd = invocationOnMock.getArgument(0);
            assertEquals("UnindexedNode", cmd.solrDoc.get("DOC_TYPE").getValue());
            return null;
        }).when(updateRequestProcessor).processAdd(any());

        // Node to be fed to Solr
        Node node = mock(Node.class);
        when(node.getStatus()).thenReturn(Node.SolrApiNodeStatus.UPDATED);
        when(node.getId()).thenReturn(865l);

        // method to be tested - when we index a node with "cm:isIndexed", the node should not be indexed
        infoServer.indexNode(node, false);

        // verifies if the method was called
        verify(updateRequestProcessor).processAdd(any());
    }
    
	@Test
	public void testGetFacets() 
	{
		SimpleOrderedMap<Object> responseContent = new SimpleOrderedMap<>();
		// Create facet_fields (TXID) as SimpleOrderedMap with Integer as value type
		SimpleOrderedMap<Object> txidFacet = new SimpleOrderedMap<>();
		txidFacet.add("1", 1);
		txidFacet.add("2", 1);
		txidFacet.add("3", 1);
		// Create and populate the NamedList to simulate facet_counts
		NamedList<Object> facetCounts = new NamedList<>();
		facetCounts.add("facet_queries", new SimpleOrderedMap<>());
		facetCounts.add("facet_fields", new SimpleOrderedMap<>());
		// Add TXID facet to facet_fields
		SimpleOrderedMap<Object> facetFields = (SimpleOrderedMap<Object>) facetCounts.get("facet_fields");
		facetFields.add("TXID", txidFacet);
		// Add the facet_counts to the main facetMap
		responseContent.add("facet_counts", facetCounts);
		// Set up the request handler to return the fake response.
		doAnswer(invocation -> {
			SolrQueryResponse solrQueryResponse = invocation.getArgument(1);
			solrQueryResponse.setAllValues(responseContent);
			return null;
		}).when(handler).handleRequest(any(SolrQueryRequest.class), any(SolrQueryResponse.class));
		NamedList<Integer> actualResult = infoServer.getFacets(request, "TXID:[1 TO 3]", "TXID", 1, 3);
		NamedList<Integer> expectedResult = new NamedList<Integer>() {
			{
				add("1", 1);
				add("2", 1);
				add("3", 1);
			}
		};
		assertEquals(expectedResult, actualResult);
	}
}
