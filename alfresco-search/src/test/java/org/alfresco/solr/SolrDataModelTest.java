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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.InputStream;

import org.alfresco.repo.dictionary.M2Model;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldUse;
import org.alfresco.solr.AlfrescoSolrDataModel.TenantAclIdDbId;
import org.junit.Test;

/**
 * @author Andy
 *
 */
public class SolrDataModelTest
{
    private static final QName CONTENT_STREAM_LENGTH = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}contentStreamLength");
    private static final QName IS_PRIVATE_WOKING_COPY = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}isPrivateWorkingCopy");
    private static final QName IS_IMMUTABLE = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}isImmutable");
    private static final QName CREATION_DATE = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}creationDate");
    private static final QName NAME = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}name");
    private static QName OBJECT_ID = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}objectId");

    @Test
    public void testDecodeSolr4id()
    {
        String tenant = "TheTenant";
        Long aclId = 987698769860l;
        Long dbId = 9879987l;
        String id = AlfrescoSolrDataModel.getNodeDocumentId(tenant, aclId, dbId);
        TenantAclIdDbId ids = AlfrescoSolrDataModel.decodeNodeDocumentId(id);
        assertEquals(tenant,ids.tenant);
        assertEquals(aclId, ids.alcId);
        assertEquals(dbId, ids.dbId);
    }
    
    @Test
    public void testParseAclChangeSetId()
    {
        Long expectedAclChangeSetId = 94032903249l;
        String aclChangeSetDocumentId = AlfrescoSolrDataModel.getAclChangeSetDocumentId(expectedAclChangeSetId);
        Long actualAclChangeSetId = AlfrescoSolrDataModel.parseAclChangeSetId(aclChangeSetDocumentId);
        assertEquals(expectedAclChangeSetId, actualAclChangeSetId);
    }
    
    @Test
    public void testParseTransactionId()
    {
        Long expectedId = 94032903249l;
        String id = AlfrescoSolrDataModel.getTransactionDocumentId(expectedId);
        Long actualId = AlfrescoSolrDataModel.parseTransactionId(id);
        assertEquals(expectedId, actualId);
    }
    
    @Test
    public void smokeTestCMISModel()
    {
        AlfrescoSolrDataModel dataModel = new AlfrescoSolrDataModel();
        
        // load test model containing content properties multiple
        ClassLoader cl = SolrDataModelTest.class.getClassLoader();
        InputStream modelStream = cl.getResourceAsStream("alfresco/model/dictionaryModel.xml");
        
        assertNotNull(modelStream);
        M2Model model = M2Model.createModel(modelStream);
        dataModel.putModel(model);
        
        modelStream = cl.getResourceAsStream("alfresco/model/cmisModel.xml");
        assertNotNull(modelStream);
        model = M2Model.createModel(modelStream);
        dataModel.putModel(model);
        
        assertEquals(2, dataModel.getAlfrescoModels().size());
        
        assertEquals(1, dataModel.getIndexedFieldNamesForProperty(OBJECT_ID).getFields().size());
        assertEquals(4, dataModel.getIndexedFieldNamesForProperty(NAME).getFields().size());
        assertEquals(1, dataModel.getIndexedFieldNamesForProperty(CREATION_DATE).getFields().size());
        assertEquals(0, dataModel.getIndexedFieldNamesForProperty(IS_IMMUTABLE).getFields().size());
        assertEquals(0, dataModel.getIndexedFieldNamesForProperty(IS_PRIVATE_WOKING_COPY).getFields().size());
        assertEquals(1, dataModel.getIndexedFieldNamesForProperty(CONTENT_STREAM_LENGTH).getFields().size());
        
        assertEquals(1, dataModel .getQueryableFields(OBJECT_ID, null, FieldUse.FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(OBJECT_ID, null, FieldUse.COMPLETION).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(OBJECT_ID, null, FieldUse.FTS).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(OBJECT_ID, null, FieldUse.ID).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(OBJECT_ID, null, FieldUse.MULTI_FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(OBJECT_ID, null, FieldUse.SORT).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(OBJECT_ID, null, FieldUse.STATS).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(OBJECT_ID, null, FieldUse.SUGGESTION).getFields().size());
        
        assertEquals(1, dataModel .getQueryableFields(NAME, null, FieldUse.FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(NAME, null, FieldUse.COMPLETION).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(NAME, null, FieldUse.FTS).getFields().size());
        assertEquals(2, dataModel .getQueryableFields(NAME, null, FieldUse.ID).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(NAME, null, FieldUse.MULTI_FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(NAME, null, FieldUse.SORT).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(NAME, null, FieldUse.STATS).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(NAME, null, FieldUse.SUGGESTION).getFields().size());
        
        assertEquals(1, dataModel .getQueryableFields(CREATION_DATE, null, FieldUse.FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CREATION_DATE, null, FieldUse.COMPLETION).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CREATION_DATE, null, FieldUse.FTS).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CREATION_DATE, null, FieldUse.ID).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CREATION_DATE, null, FieldUse.MULTI_FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CREATION_DATE, null, FieldUse.SORT).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CREATION_DATE, null, FieldUse.STATS).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CREATION_DATE, null, FieldUse.SUGGESTION).getFields().size());
        
        assertEquals(1, dataModel .getQueryableFields(IS_PRIVATE_WOKING_COPY, null, FieldUse.FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(IS_PRIVATE_WOKING_COPY, null, FieldUse.COMPLETION).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(IS_PRIVATE_WOKING_COPY, null, FieldUse.FTS).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(IS_PRIVATE_WOKING_COPY, null, FieldUse.ID).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(IS_PRIVATE_WOKING_COPY, null, FieldUse.MULTI_FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(IS_PRIVATE_WOKING_COPY, null, FieldUse.SORT).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(IS_PRIVATE_WOKING_COPY, null, FieldUse.STATS).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(IS_PRIVATE_WOKING_COPY, null, FieldUse.SUGGESTION).getFields().size());
        
        assertEquals(1, dataModel .getQueryableFields(CONTENT_STREAM_LENGTH, null, FieldUse.FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CONTENT_STREAM_LENGTH, null, FieldUse.COMPLETION).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CONTENT_STREAM_LENGTH, null, FieldUse.FTS).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CONTENT_STREAM_LENGTH, null, FieldUse.ID).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CONTENT_STREAM_LENGTH, null, FieldUse.MULTI_FACET).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CONTENT_STREAM_LENGTH, null, FieldUse.SORT).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CONTENT_STREAM_LENGTH, null, FieldUse.STATS).getFields().size());
        assertEquals(1, dataModel .getQueryableFields(CONTENT_STREAM_LENGTH, null, FieldUse.SUGGESTION).getFields().size());
        
        
    }
}
