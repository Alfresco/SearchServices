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
import static org.alfresco.model.ContentModel.PROP_CREATOR;
import static org.alfresco.model.ContentModel.TYPE_CONTENT;
import static org.alfresco.solr.AlfrescoSolrUtils.addNode;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.query.afts.SharedTestDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests the behaviour of properties marked as identifiers.
 * The test dataset is very simple: it is composed by few nodes where the cm:creator property, contains a username,
 * which is supposed to be managed as a whole keyword.
 *
 * @see <a href="https://issues.alfresco.com/jira/browse/MNT-18693">MNT-18693</a>
 * @author eporciani
 * @author agazzarini
 */
public class AFTSIdentifierFieldsTest extends AbstractRequestHandlerTest
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        SharedTestDataProvider dataProvider = new SharedTestDataProvider(h);
        String [] usernames = {"Daniel", "Daniel2", "Daniele", "daniel.armstrong" };

        TEST_ROOT_NODEREF = dataProvider.getRootNode();

        range(0, usernames.length)
                .forEach(dbId -> {
                    String username = usernames[dbId];

                    Map<QName, PropertyValue> properties = new HashMap<>();
                    properties.put(PROP_CREATOR, new StringPropertyValue(username));

                    addNode(h.getCore(),
                        dataModel, 1, dbId, 1,
                        TYPE_CONTENT, null, properties, null,
                        "the_owner_is_" + username,
                        null,
                        new NodeRef[]{ TEST_ROOT_NODEREF },
                        new String[]{ "/" + dataProvider.qName("test_" + username) },
                        dataProvider.newNodeRef(), true);
                });
    }

    @Test
    public void testIdentifierProperty()
    {
        assertResponseCardinality("creator:'Daniel'", 1);
        assertResponseCardinality("creator:'daniel'", 0);

        assertResponseCardinality("creator:'Daniel2'", 1);
        assertResponseCardinality("creator:'Danie*'", 3);
        assertResponseCardinality("creator:'Daniel*'", 3);
        assertResponseCardinality("creator:'Daniela'", 0);

        assertResponseCardinality("creator:'daniel*'", 1);
        assertResponseCardinality("creator:'daniel.armstrong'", 1);
    }
}