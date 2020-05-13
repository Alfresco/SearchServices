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

import java.util.Properties;
import java.util.stream.Stream;

import org.alfresco.service.cmr.dictionary.DataTypeDefinition;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
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
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

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
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_DAY_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_HOUR_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_MINUTE_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_MONTH_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_SECOND_FIELD_SUFFIX;
import static org.alfresco.solr.SolrInformationServer.UNIT_OF_TIME_YEAR_FIELD_SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
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