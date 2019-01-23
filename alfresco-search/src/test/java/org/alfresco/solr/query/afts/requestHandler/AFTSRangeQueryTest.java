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
import static org.alfresco.model.ContentModel.PROP_RATING_SCHEME;
import static org.alfresco.model.ContentModel.TYPE_CONTENT;
import static org.alfresco.solr.AlfrescoSolrUtils.addNode;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.query.afts.SharedTestDataProvider;
import org.alfresco.util.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * https://issues.alfresco.com/jira/browse/SEARCH-1359
 *
 * @author elia
 */
public class AFTSRangeQueryTest extends AbstractRequestHandlerTest
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        SharedTestDataProvider dataProvider = new SharedTestDataProvider(h);

        @SuppressWarnings("unchecked")
        Pair<String, String> [] ratingsAndCreators = new Pair[] {
                new Pair<>("someTest", "Mario"),
                new Pair<>("test", "Luigi"),
                new Pair<>("firstString", "Wario")
        };

        TEST_ROOT_NODEREF = dataProvider.getRootNode();

        range(0, ratingsAndCreators.length)
                .forEach(dbId -> {
                    String rating = ratingsAndCreators[dbId].getFirst();
                    String creator = ratingsAndCreators[dbId].getSecond();

                    Map<QName, PropertyValue> properties = new HashMap<>();
                    properties.put(PROP_CREATOR, new StringPropertyValue(creator));
                    properties.put(PROP_RATING_SCHEME, new StringPropertyValue(rating));

                    addNode(h.getCore(),
                            dataModel, 1, dbId, 1,
                            TYPE_CONTENT, null, properties, null,
                            "the_owner_of_this_node_is" + creator,
                            null,
                            new NodeRef[]{ TEST_ROOT_NODEREF },
                            new String[]{ "/" + dataProvider.qName("a_qname_for_node_" + creator) },
                            dataProvider.newNodeRef(), true);
                });
    }

    @Test
    public void testRangeQueriesNonTokenised()
    {
        assertResponseCardinality("cm:ratingScheme:[n TO *]", 2);
        assertResponseCardinality("cm:ratingScheme:[* TO n]", 1);
        assertResponseCardinality("cm:ratingScheme:[first TO *]", 3);
        assertResponseCardinality("cm:ratingScheme:[firstt TO *]", 2);
        assertResponseCardinality("cm:ratingScheme:[* TO someTest]", 2);
        assertResponseCardinality("cm:ratingScheme:[kk TO someTest]", 1);
    }

    @Test
    public void testRangeQueriesTokenised()
    {
        assertResponseCardinality("cm:creator:[Mario TO *]", 2);
        assertResponseCardinality("cm:creator:[Lu TO *]", 3);
        assertResponseCardinality("cm:creator:[Luigi2 TO Mario]", 1);
        assertResponseCardinality("cm:creator:[* TO Mario]", 2);
    }
}