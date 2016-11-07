/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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
package org.alfresco.solr.query.cmis;

import static org.alfresco.solr.AlfrescoSolrUtils.addNode;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.MLTextPropertyValue;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.junit.Before;
import org.junit.Test;
/**
 * Ported tests relating to cmis from AlfrescoCoreAdminTester (Legacy embedded
 * tests).
 * @author Michael Suzuki
 *
 */
public class TrieFieldsCMISTest extends LoadCMISData
{
    @Before
    public void setup() throws Exception
    {
        /******** Load record ************/

        addTypeTestData(testCMISFolder00NodeRef,
                testCMISRootNodeRef,
                testCMISBaseFolderNodeRef,
                testCMISBaseFolderQName,
                testCMISFolder00QName,
                testCMISDate00);

        addTrieTypeTestData(testCMISFolder00NodeRef, 
                testCMISRootNodeRef, 
                testCMISBaseFolderNodeRef, 
                testCMISBaseFolderQName,
                testCMISFolder00QName,
                testCMISDate00);

    }
    private void addTrieTypeTestData(NodeRef folder00NodeRef, NodeRef rootNodeRef, NodeRef baseFolderNodeRef,
            QName baseFolderQName, QName folder00QName, Date date00) throws IOException
    {
        HashMap<QName, PropertyValue> content00Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc00 = new MLTextPropertyValue();
        desc00.addValue(Locale.ENGLISH, "Trie test1");
        content00Properties.put(ContentModel.PROP_DESCRIPTION, desc00);
        content00Properties.put(ContentModel.PROP_TITLE, desc00);
        content00Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Trie test1"));
        content00Properties.put(ContentModel.PROP_CREATED,
                    new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date00)));

        StringPropertyValue firstIntValue = new StringPropertyValue("98198");
        content00Properties.put(singleInteger,firstIntValue);
        
        StringPropertyValue firstLongValue = new StringPropertyValue("3956650");
        content00Properties.put(singleLong,firstLongValue);
        
        NodeRef content00NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content00QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Trie test1");
        ChildAssociationRef content00CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder00NodeRef,
                    content00QName, content00NodeRef, true, 0);
        addNode(h.getCore(), dataModel, 1, 300, 1, extendedContent, new QName[] { ContentModel.ASPECT_OWNABLE,
                    ContentModel.ASPECT_TITLED }, content00Properties, null, "andy",
                    new ChildAssociationRef[] { content00CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef,
                                folder00NodeRef }, new String[] { "/" + baseFolderQName.toString() + "/"
                                + folder00QName.toString() + "/" + content00QName.toString() }, content00NodeRef, true);
        
        HashMap<QName, PropertyValue> content01Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc01 = new MLTextPropertyValue();
        desc01.addValue(Locale.ENGLISH, "Trie test2");
        content01Properties.put(ContentModel.PROP_DESCRIPTION, desc01);
        content01Properties.put(ContentModel.PROP_TITLE, desc01);
        content01Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Trie test2"));
        content01Properties.put(ContentModel.PROP_CREATED,
                    new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date00)));

        StringPropertyValue secondIntValue = new StringPropertyValue("98200");
        content01Properties.put(singleInteger,secondIntValue);
        
        StringPropertyValue secondLongValue = new StringPropertyValue("3956651");
        content01Properties.put(singleLong,secondLongValue);
        
        NodeRef content01NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content01QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Trie test1");
        ChildAssociationRef content01CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder00NodeRef,
                    content01QName, content01NodeRef, true, 0);
        addNode(h.getCore(), dataModel, 1, 301, 1, extendedContent, new QName[] { ContentModel.ASPECT_OWNABLE,
                    ContentModel.ASPECT_TITLED }, content01Properties, null, "andy",
                    new ChildAssociationRef[] { content01CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef,
                                folder00NodeRef }, new String[] { "/" + baseFolderQName.toString() + "/"
                                + folder00QName.toString() + "/" + content01QName.toString() }, content01NodeRef, true);
        
    }
   
    @Test
    public void checkOrder() throws IOException
    {
      //See MNT-14322 for the origin of the numbers
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger = 98218"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger = 98198"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger = 98200"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong = 3956650"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong = 3956651"),expectedDocCount(1));
    }
    
}
