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
package org.alfresco.solr.query.afts.requestHandler;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.query.afts.TestDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static java.util.stream.IntStream.range;
import static junit.framework.TestCase.assertEquals;
import static org.alfresco.model.ContentModel.*;
import static org.alfresco.solr.AlfrescoSolrUtils.addNode;


/**
 * @author elia
 */
public class AFTSDefaultTextQueryTest extends AbstractRequestHandlerTest
{

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        TestDataProvider dataProvider = new TestDataProvider(h);

        String[] names = {
                "test1",
                "test2",
                "file3",
                "name4"};

        String[] descriptions = {
                "description of test 1",
                "description 2",
                "this is not a description of test 1 and 2",
                "other description"};

        String[] contents = {
                "test",
                "content of test 2",
                "other contents",
                "content of file number 4"};

        String[] titles = {
                "Title1",
                "Other Title",
                "Third",
                "Forth"};

        // Creators should not be searched in default queries
        String[] creators = {
                "Luca",
                "Mario",
                "Giovanni",
                "Giuseppe"};

        // Make sure the arrays of properties have the same cardinality.
        assertEquals(names.length, descriptions.length);
        assertEquals(names.length, contents.length);
        assertEquals(names.length, titles.length);
        assertEquals(names.length, creators.length);

        TEST_ROOT_NODEREF = dataProvider.getRootNode();

        range(0, names.length)
                .forEach(dbId -> {
                    String name = names[dbId];
                    String description = descriptions[dbId];
                    String content = contents[dbId];
                    String title = titles[dbId];
                    String creator = creators[dbId];

                    Map<QName, PropertyValue> properties = new HashMap<>();
                    properties.put(PROP_NAME, new StringPropertyValue(name));
                    properties.put(PROP_DESCRIPTION, new StringPropertyValue(description));
                    properties.put(PROP_CONTENT, new StringPropertyValue(content));
                    properties.put(PROP_TITLE, new StringPropertyValue(title));
                    properties.put(PROP_CREATOR, new StringPropertyValue(creator));

                    addNode(getCore(),
                            dataModel, 1, dbId, 1,
                            TYPE_CONTENT, null, properties, null,
                            "the_owner_of_this_node_is" + name,
                            null,
                            new NodeRef[]{ TEST_ROOT_NODEREF },
                            new String[]{ "/" + dataProvider.qName("a_qname_for_node_" + name) },
                            dataProvider.newNodeRef(), true);
                });
    }

    @Test
    public void defaultExactQueryTest()
    {
        assertResponseCardinality("test", 3);
        assertResponseCardinality("Giovanni", 0);
    }

    @Test
    public void defaultRangeQueryTest()
    {
        assertResponseCardinality("[te to test]", 3);
    }

    @Test
    public void defaultWildCardQueryTest()
    {
        assertResponseCardinality("?est", 3);
    }

    @Test
    public void defaultFuzzyQueryTest()
    {
        assertResponseCardinality("content~1.0", 3);
    }

    @Test
    public void defaultSpanQueryTest()
    {
        assertResponseCardinality("Other * title", 1);
        assertResponseCardinality("description * of", 2);
    }

    @Test
    public void defaultPrefixQueryTest()
    {
        assertResponseCardinality("te*", 3);
        assertResponseCardinality("fil*", 2);
    }

    /**
     * Check that creators are correctly inserted
     */
    @Test
    public void CreatorQueryTest()
    {
        assertResponseCardinality("cm:creator:Giovanni", 1);
        assertResponseCardinality("cm:creator:[L TO Z]", 2);
    }

}
