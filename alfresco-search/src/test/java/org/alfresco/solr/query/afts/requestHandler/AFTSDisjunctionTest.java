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
package org.alfresco.solr.query.afts.requestHandler;

import static java.util.stream.IntStream.range;
import static org.alfresco.model.ContentModel.PROP_NAME;
import static org.alfresco.model.ContentModel.TYPE_CONTENT;
import static org.alfresco.solr.AlfrescoSolrUtils.addNode;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.query.afts.TestDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * https://issues.alfresco.com/jira/browse/SEARCH-556
 *
 * @author msuzuki
 */
public class AFTSDisjunctionTest extends AbstractRequestHandlerTest
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        TestDataProvider dataProvider = new TestDataProvider(h);
        String [] names = {"test1", "test2", "test3" };

        TEST_ROOT_NODEREF = dataProvider.getRootNode();

        range(0, names.length)
                .forEach(index -> {
                    String name = names[index];
                    int dbId = index + 2; // we explicitly skip id 0 and 1

                    Map<QName, PropertyValue> properties = new HashMap<>();
                    properties.put(PROP_NAME, new StringPropertyValue(name));

                    addNode(h.getCore(),
                            dataModel, 1, dbId, 1,
                            TYPE_CONTENT, null, properties, null,
                            "the_owner_of_this_node_is" + name,
                            null,
                            new NodeRef[]{ TEST_ROOT_NODEREF },
                            new String[]{ "/" + dataProvider.qName("a_qname_for_node_" + name) },
                            dataProvider.newNodeRef(), true);
                });    }
    @Test
    public void testDisjunction()
    {
        assertResponseCardinality("*", 4);
        assertResponseCardinality("cm:name:test*", 3);
        assertResponseCardinality("cm:name:test* AND NOT id:1", 3);
        assertResponseCardinality("cm:name:test* AND NOT (id:1)", 3);
        assertResponseCardinality("cm:name:test* AND (NOT id:1)", 3);
    }
}