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
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.junit.Before;
import org.junit.Test;
/**
 * Ported tests relating to cmis from AlfrescoCoreAdminTester (Legacy embedded
 * tests).
 * @author Michael Suzuki
 *
 */
public class SortCMISTest extends LoadCMISData
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
        addTypeSortTestData(testCMISFolder00NodeRef, 
                testCMISRootNodeRef, 
                testCMISBaseFolderNodeRef, 
                testCMISBaseFolderQName,
                testCMISFolder00QName,
                testCMISDate00);
    }
    
    protected void addTypeSortTestData(NodeRef folder00NodeRef, NodeRef rootNodeRef, NodeRef baseFolderNodeRef, Object baseFolderQName, Object folder00QName, Date date1)
            throws IOException
    {
        addSortableNull(folder00NodeRef, rootNodeRef, baseFolderNodeRef, baseFolderQName,
                folder00QName, date1, "start", 0);
        for (int i = 0; i < 10; i++)
        {
            addSortableNode(folder00NodeRef, rootNodeRef, baseFolderNodeRef, baseFolderQName,
                        folder00QName, date1, i);
            if (i == 5)
            {
                addSortableNull(folder00NodeRef, rootNodeRef, baseFolderNodeRef, baseFolderQName,
                            folder00QName, date1, "mid", 1);
            }
        }

        addSortableNull(folder00NodeRef, rootNodeRef, baseFolderNodeRef, baseFolderQName,
                    folder00QName, date1, "end", 2);
    }
    private void addSortableNull(NodeRef folder00NodeRef,
            NodeRef rootNodeRef, NodeRef baseFolderNodeRef, Object baseFolderQName, Object folder00QName,
            Date date1, String id, int offset) throws IOException
    {
        HashMap<QName, PropertyValue> content00Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc00 = new MLTextPropertyValue();
        desc00.addValue(Locale.ENGLISH, "Test null");
        content00Properties.put(ContentModel.PROP_DESCRIPTION, desc00);
        content00Properties.put(ContentModel.PROP_TITLE, desc00);
        content00Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Test null"));
        content00Properties.put(ContentModel.PROP_CREATED,
                    new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date1)));
    
        NodeRef content00NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content00QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Test null");
        ChildAssociationRef content00CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder00NodeRef,
                    content00QName, content00NodeRef, true, 0);
        addNode(h.getCore(), dataModel, 1, 200 + offset, 1, extendedContent, new QName[] { ContentModel.ASPECT_OWNABLE,
                    ContentModel.ASPECT_TITLED }, content00Properties, null, "andy",
                    new ChildAssociationRef[] { content00CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef,
                                folder00NodeRef }, new String[] { "/" + baseFolderQName.toString() + "/"
                                + folder00QName.toString() + "/" + content00QName.toString() }, content00NodeRef, true);
    }
    @Test
    public void checkOrder() throws Exception
    {

        NamedList<Object> report = new SimpleOrderedMap<Object>();
//        rsp.add("CMIS order", report);

        Integer[] asc = new Integer[] { 200, 201, 202, 1008, 1005, 1004, 1009, 1001, 1007, 1006, 1003, 1002, 100, 1000 };
        Integer[] desc = new Integer[] { 1000, 100, 1002, 1003, 1006, 1007, 1001, 1009, 1004, 1005, 1008, 202, 201, 200 };

        checkOrderableProperty( report, "cmistest:singleTextUntokenised", asc, desc);
        // checkOrderableProperty( report, "cmistest:singleTextTokenised");
        checkOrderableProperty( report, "cmistest:singleTextBoth", asc, desc);

        // testOrderablePropertyFail("test:multipleTextUntokenised");
        // testOrderablePropertyFail("test:multipleTextTokenised");
        // testOrderablePropertyFail("test:multipleTextBoth");

        asc = new Integer[] { 200, 201, 202, 1009, 100, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008 };
        desc = new Integer[] { 1008, 1007, 1006, 1005, 1004, 1003, 1002, 1001, 1000, 100, 1009, 202, 201, 200 };

        checkOrderableProperty( report, "cmistest:singleMLTextUntokenised", asc, desc);
        // testOrderablePropertyFail("cmistest:singleMLTextTokenised");
        checkOrderableProperty( report, "cmistest:singleMLTextBoth", asc, desc);

        // testOrderablePropertyFail("cmistest:multipleMLTextUntokenised");
        // testOrderablePropertyFail("cmistest:multipleMLTextTokenised");
        // testOrderablePropertyFail("cmistest:multipleMLTextBoth");

        asc = new Integer[] { 200, 1000, 201, 202, 100, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009  };     
        desc = new Integer[] { 1009, 1008, 1007, 1006, 1005, 1004, 1003, 1002, 1001, 100, 202, 201, 1000, 200 };

        checkOrderableProperty( report, "cmistest:singleFloat", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleFloat");

        checkOrderableProperty( report, "cmistest:singleDouble", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleDouble");

        asc = new Integer[] { 200, 1000, 201, 202, 100, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009  };     
        desc = new Integer[] { 1009, 1008, 1007, 1006, 1005, 1004, 1003, 1002, 1001, 100, 202, 201, 1000, 200 };

        checkOrderableProperty( report, "cmistest:singleInteger", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleInteger");

        checkOrderableProperty( report, "cmistest:singleLong", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleLong");

        asc = new Integer[] { 200, 201, 202, 100, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009  };     
        desc = new Integer[] { 1009, 1008, 1007, 1006, 1005, 1004, 1003, 1002, 1001, 1000, 100, 202, 201, 200 };

        checkOrderableProperty( report, "cmistest:singleDate", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleDate");

        checkOrderableProperty( report, "cmistest:singleDatetime", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleDatetime");

        asc = new Integer[] { 1001, 1003, 1005, 1007, 1009, 100, 1000, 1002, 1004, 1006, 1008, 200, 201, 202};
        desc = new Integer[] { 1008, 1006, 1004, 1002, 1000, 100, 1009, 1007, 1005, 1003, 1001, 202, 201, 200 }; 

        checkOrderableProperty( report, "cmistest:singleBoolean", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleBoolean");

    }
    private void checkOrderableProperty(NamedList<Object> report, String propertyQueryName, Integer[] asc, Integer[] desc) throws Exception
    {
        String queryASC = String.format(
                "SELECT %1$s FROM cmistest:extendedContent ORDER BY %1$s ASC, cmis:objectId ASC",
                propertyQueryName);
        assertQueryCollection(queryASC, asc);

        String queryDesc = String.format(
                "SELECT %1$s FROM cmistest:extendedContent ORDER BY %1$s DESC, cmis:objectId DESC",
                propertyQueryName);
        assertQueryCollection(queryDesc, desc);
    }
   
    
}
