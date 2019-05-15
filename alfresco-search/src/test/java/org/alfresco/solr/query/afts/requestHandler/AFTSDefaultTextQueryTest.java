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
import org.alfresco.solr.dataload.TestDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;


import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;
import static org.alfresco.model.ContentModel.*;
import static org.alfresco.solr.AlfrescoSolrUtils.addNode;
import static com.google.common.collect.ImmutableMap.of;


/**
 * @author elia
 *
 * The following test set needs to test that default text queries actually work by searching
 * in cm:name, cm:title, cm:description and cm:content fields.
 *
 * THe default queries can be of the following types:
 * exact, prefix, wildcard, fuzzy, span and range.
 * This test set checks that in all these query types the default fields are involved in the search.
 *
 */
public class AFTSDefaultTextQueryTest extends AbstractRequestHandlerTest
{

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        TestDataProvider dataProvider = new TestDataProvider(h);

        List<Map<String, String>> data = asList(
                of("name", "test1",
                        "description", "description of test 1",
                        "content", "test",
                        "title", "TITLE1",
                        "creator", "Luca"),
                of("name", "test2",
                        "description", "description 2",
                        "content", "content of test 2",
                        "title", "Other Title",
                        "creator", "Mario"),
                of("name", "file3",
                        "description", "this is not a description of test 1 and 2",
                        "content", "other content here ",
                        "title", "Third",
                        "creator", "Giovanni"),
                of("name", "name of record 4",
                        "description", "other description right here",
                        "content", "content of file number 4",
                        "title", "Forth",
                        "creator", "Giuseppe"));


        TEST_ROOT_NODEREF = dataProvider.getRootNode();

        range(0, data.size())
                .forEach(dbId -> {

                    Map<String, String> record = data.get(dbId);

                    String name = record.get("name");
                    String description = record.get("description");
                    String content = record.get("content");
                    String title = record.get("title");
                    String creator = record.get("creator");

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


    /**
     * Test exact search is working.
     */
    @Test
    public void defaultExactQueryTest()
    {

        /*
         * 3 results expected:
         * record 1 ("test" in name, description and content)
         * record 2 ("test" in name, and content)
         * record 3 ("test" in description)
         */
        assertResponseCardinality("test", 3);

        /*
         * 3 results expected:
         * record 2 ("Other" in title )
         * record 3 ("other" in content)
         * record 4 ("other" in description)
         */
        assertResponseCardinality("Other", 3);

        /*
         * No results expected because creator should not be considered in default text search.
         */
        assertResponseCardinality("Giovanni", 0);
    }


    /**
     * Test range queries.
     */
    @Test
    public void defaultRangeQueryTest()
    {
        /*
         * 3 results expected:
         * record 1 ("test" in name, description and content)
         * record 2 ("test" in name, and content)
         * record 3 ("test" in description)
         */
        assertResponseCardinality("[te to test]", 3);

        /*
         * 3 results expected:
         * record 2 ("Other" in title )
         * record 3 ("other" in content)
         * record 4 ("other" in description)
         */
        assertResponseCardinality("other to otherz ", 3);

    }

    /**
     * Test wildcard queries.
     */
    @Test
    public void defaultWildCardQueryTest()
    {
        /*
         * 3 results expected:
         * record 1 ("test" in name, description and content)
         * record 2 ("test" in name, and content)
         * record 3 ("test" in description)
         */
        assertResponseCardinality("?est", 3);

        /*
         * 3 results expected:
         * record 2 ("Other" in title )
         * record 3 ("other" in content)
         * record 4 ("other" in description)
         */
        assertResponseCardinality("?ther", 3);

    }


    /**
     * Thes span queries
     */
    @Test
    public void defaultSpanQueryTest()
    {
        /*
         * 1 result expected
         * record 1 ("Other title" in title)
         */
        assertResponseCardinality("Other *(0) title", 1);

        /*
         * 2 results expected
         * record 1 ("description of test" in description)
         * record 3 ("description of test" in description)
         */
        assertResponseCardinality("description *(1) test ", 2);

        /*
         * No results expected.
         * There is no "description test" in any field.
         */
        assertResponseCardinality("description *(0) test ", 0);

        /*
         * 1 result expected
         * record 4 ("name of record" in name)
         */
        assertResponseCardinality("name *(1) record ", 1);

        /*
         * 2 results expected
         * record 2 ("other content here" in content)
         */
        assertResponseCardinality("other *(1) here ", 1);

    }

    /**
     * Test prefix queries
     */
    @Test
    public void defaultPrefixQueryTest()
    {
        /*
         * 3 results expected
         * record 1 ("test" in name, description and content)
         * record 2 ("test" in name, and content)
         * record 3 ("test" in description)
         */
        assertResponseCardinality("te*", 3);

        /*
         * 3 results expected
         * record 1 ("file" in name, description and content)
         * record 2 ("file" in name, and content)
         */
        assertResponseCardinality("fil*", 2);

        /*
         * 3 results expected:
         * record 2 ("Other" in title )
         * record 3 ("other" in content)
         * record 4 ("other" in description)
         */
        assertResponseCardinality("oth*", 3);
    }


}
