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

package org.alfresco.solr.query.afts.requestHandler;

import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.MLTextPropertyValue;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.dataload.TestDataProvider;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
@SolrTestCaseJ4.SuppressSSL
public class AFTSDefaultTextQueryIT extends AbstractRequestHandlerIT
{
    // Following data is used for restoring alfrescoSolrDataModel after text execution
    private HashSet<QName> oldCrossLocaleProperties;
    private HashSet<QName> oldCrossLocaleTypes;

    static TestDataProvider dataProvider;
   
    
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        dataProvider = new TestDataProvider(h);

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
                    properties.put(PROP_DESCRIPTION, new MLTextPropertyValue(Map.of(Locale.getDefault(), description)));
                    properties.put(PROP_TITLE, new MLTextPropertyValue(Map.of(Locale.getDefault(), title)));
                    properties.put(PROP_CREATOR, new StringPropertyValue(creator));

                    Map<QName, String> contents = Map.of(PROP_CONTENT, content);

                    addNode(getCore(),
                            dataModel, 1, dbId, 1,
                            TYPE_CONTENT, null, properties, contents,
                            "the_owner_of_this_node_is" + name,
                            null,
                            new NodeRef[]{ TEST_ROOT_NODEREF },
                            new String[]{ "/" + dataProvider.qName("a_qname_for_node_" + name) },
                            dataProvider.newNodeRef(), true);
                });
    }

    private void prepareExactSearchData() throws Exception {
        TestDataProvider dataProvider = AFTSDefaultTextQueryIT.dataProvider;

        List<Map<String, String>> exactSearchData = asList(
                of("name", "Running",
                        "description", "Running is a sport is a nice activity",
                        "content", "when you are running you are doing an amazing sport",
                        "title", "Running jumping",
                        "creator", "Alex"),
                of("name", "Run",
                        "description", "you are supposed to run jump",
                        "content", "after many runs you are tired and if you jump it happens the same",
                        "title", "Run : a philosophy",
                        "creator", "Alex"),
                of("name", "Poetry",
                        "description", "a document about poetry and jumpers",
                        "content", "poetry is unrelated to sport",
                        "title", "Running jumping twice jumpers",
                        "creator", "Alex"),
                of("name", "Jump",
                        "description", "a document about jumps",
                        "content", "runnings jumpings",
                        "title", "Running",
                        "creator", "Alex"),
                of("name", "Running jumping",
                        "description", "runners jumpers runs everywhere",
                        "content", "run is Good as jump",
                        "title", "Running the art of jumping",
                        "creator", "Alex"));

        range(0, exactSearchData.size())
                .forEach(dbId -> {

                    Map<String, String> record = exactSearchData.get(dbId);

                    String name = record.get("name");
                    String description = record.get("description");
                    String content = record.get("content");
                    String title = record.get("title");
                    String creator = record.get("creator");

                    Map<QName, PropertyValue> properties = new HashMap<>();
                    properties.put(PROP_NAME, new StringPropertyValue(name));
                    properties.put(PROP_DESCRIPTION, new MLTextPropertyValue(Map.of(Locale.getDefault(), description)));
                    properties.put(PROP_TITLE, new MLTextPropertyValue(Map.of(Locale.getDefault(), title)));
                    properties.put(PROP_CREATOR, new StringPropertyValue(creator));
                    properties.put(PROP_RATING_SCHEME, new StringPropertyValue(title));

                    Map<QName, String> contents = Map.of(PROP_CONTENT, content);

                    addNode(getCore(),
                            dataModel, 2, dbId+10, 1,
                            TYPE_CONTENT, null, properties, contents,
                            "the_owner_of_this_node_is" + name,
                            null,
                            new NodeRef[]{ TEST_ROOT_NODEREF },
                            new String[]{ "/" + dataProvider.qName("a_qname_for_node_" + name) },
                            dataProvider.newNodeRef(), true);
                });
    }
    
    private void changeCrossLocaleEnabledFields() throws NoSuchFieldException, IllegalAccessException {
        Field crossLocalePropertiesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("crossLocaleSearchProperties");

        crossLocalePropertiesField.setAccessible(true);

        Field crossLocaleTypesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("crossLocaleSearchDataTypes");

        crossLocaleTypesField.setAccessible(true);

        HashSet<QName> crossLocaleProperties = (HashSet<QName>) crossLocalePropertiesField.get(AlfrescoSolrDataModel.getInstance());
        HashSet<QName> crossLocaleTypes = (HashSet<QName>) crossLocaleTypesField.get(AlfrescoSolrDataModel.getInstance());

        oldCrossLocaleTypes = (HashSet<QName>) crossLocaleTypes.clone();
        oldCrossLocaleProperties = (HashSet<QName>) crossLocaleProperties.clone();

        crossLocaleTypes.clear();
        crossLocaleProperties.clear();
        crossLocaleProperties.add(PROP_TITLE);
    }

    @After
    public void restoreAlfrescoSolrDataModel() throws NoSuchFieldException, IllegalAccessException {
        if (oldCrossLocaleTypes != null) {
            Field crossLocaleTypesField = AlfrescoSolrDataModel
                    .getInstance()
                    .getClass()
                    .getDeclaredField("crossLocaleSearchDataTypes");

            crossLocaleTypesField.setAccessible(true);
            crossLocaleTypesField.set(AlfrescoSolrDataModel.getInstance(), oldCrossLocaleTypes);
        }
        if (oldCrossLocaleProperties != null) {
            Field crossLocalePropertiesField = AlfrescoSolrDataModel
                    .getInstance()
                    .getClass()
                    .getDeclaredField("crossLocaleSearchProperties");
            crossLocalePropertiesField.setAccessible(true);
            crossLocalePropertiesField.set(AlfrescoSolrDataModel.getInstance(), oldCrossLocaleProperties);
        }
    }
    
    /**
     * Test exact search is working.
     */
    @Test
    public void exactSearch_singleTerm_shouldReturnResultsContainingExactTerm() throws Exception {
        prepareExactSearchData();
        /*
         * Out of the 5 'run corpus' documents 
         * 2 results are expected:
         * 
         * - "name", "Run",
         * "description", "you are supposed to run jump"
         * 
         * - "name", "Running jumping",
         * "content", "run is Good as jump",
         */
        assertResponseCardinality("=run", 2);

        /*
         * No result for runner, one record has runners,
         * you can see the difference between exact search and not
         */
        assertResponseCardinality("=runner", 0);
        assertResponseCardinality("runner", 1);

        /*
         * Out of the 5 'run corpus' documents
         * 4 results are expected, only one doc is no fit for the query
         * 
         *              "name", "Run",
         *              "description", "you are supposed to run jump",
         *              "content", "after many runs you are tired and if you jump it happens the same",
         *              "title", "Run : a philosophy",
         *              "creator", "Alex"
         */
        assertResponseCardinality("=running", 4);
    }

    @Test
    public void exactSearch_singleTermInFieldWithNoCrossLocaleEnabled_shouldReturnException() throws Exception {
        /*
         * Cross Locale is Disabled for: content, cm_description, name
         * Cross Locale is Enabled for : cm_title
         */
        changeCrossLocaleEnabledFields();
        prepareExactSearchData();
        
        assertResponseCardinality("=cm_title:run", 1); 
        assertResponseException("=content:run", "Exact Term search is not supported unless you configure the field <{http://www.alfresco.org/model/content/1.0}content> for cross locale search");
        assertResponseException("=cm_description:run", "Exact Term search is not supported unless you configure the field <{http://www.alfresco.org/model/content/1.0}description> for cross locale search");
        assertResponseCardinality("=run", 1);
        
        restoreAlfrescoSolrDataModel();
    }

    @Test
    public void exactSearch_singleTermInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception {
        /**
         * cm_ratingScheme is a copy field un-tokenized of Title, so it has the exact same content but not analysed.
         * This means we produce just a token in the index, exactly as the full content.
         * We can't expect any search to work except full exact value search
         */
        prepareExactSearchData();
        /*
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * there is no result that have cm_ratingScheme:"running"
         *
         */
        assertResponseCardinality("=cm_ratingScheme:running", 0);
        assertResponseCardinality("=cm_title:running", 4);
        
        /*
         * Out of the 5 'run corpus' documents
         * 1 result is expected:
         * 
         * - "name", "Jump",
         *  ...
         *  "title", "Running"
         *
         */
        assertResponseCardinality("=cm_ratingScheme:Running", 1);
        assertResponseCardinality("=cm_title:Running", 4);

        /*
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * there is no result that have exactly cm_ratingScheme:"Run"
         * The closest we have is record Run (cm_ratingScheme:"Run : a philosophy")
         * As you can see we don't have a full match, so it's not in the results.
         *
         */
        assertResponseCardinality("=cm_ratingScheme:Run", 0);
        assertResponseCardinality("=cm_title:Run", 1);


    }

    /**
     * Test exact search is working.
     */
    @Test
    public void exactSearch_multiTerm_shouldReturnResultsContainingExactTerm() throws Exception {
        prepareExactSearchData();
        /*
         * Out of the 5 'run corpus' documents
         * 3 results are expected:
         *
         * - "name", "Run",
         * "description", "you are supposed to run jump",
         * 
         * - "name", "Running jumping",
         * "description", "runners jumpers runs everywhere",
         * "content", "run is Good as jump",
         * 
         * - "name", "Jump",
         * "description", "a document about jumps",
         * 
         */
        assertResponseCardinality("=run =jump", 3);

        /*
         * No result for runner or jumper, one record has runners,
         * and another record has jumpers
         * 
         * - "name", "Poetry",
         *              "description", "a document about poetry and jumpers",
         * - "name", "Running jumping",
         *              "description", "runners jumpers run everywhere",
         * 
         * you can see the difference between exact search and not
         */
        assertResponseCardinality("=runner =jumper", 0);
        assertResponseCardinality("runner jumper", 2);

        /*
         * Out of the 5 'run corpus' documents
         * 4 results are expected:
         * Only one doc does't fit:
         * - "name", "Run",
         *  "description", "you are supposed to run jump",
         *  "content", "after many runs you are tired and if you jump it happens the same",
         *  "title", "Run : a philosophy",
         *  "creator", "Alex"),
         * 
         *               
         */
        assertResponseCardinality("=running =jumping", 4);
    }



    @Test
    public void exactSearch_multiTermInFieldWithNoCrossLocaleEnabled_shouldReturnException() throws Exception {
        /*
         * Cross Locale is Disabled for: content, cm_description, name
         * Cross Locale is Enabled for : cm_title
         */
        changeCrossLocaleEnabledFields();
        prepareExactSearchData();

        assertResponseCardinality("=cm_title:run =cm_title:jump", 1);
        assertResponseException("=content:run =content:jump", "Exact Term search is not supported unless you configure the field <{http://www.alfresco.org/model/content/1.0}content> for cross locale search");
        assertResponseException("=cm_description:run =cm_description:jump", "Exact Term search is not supported unless you configure the field <{http://www.alfresco.org/model/content/1.0}description> for cross locale search");
        assertResponseCardinality("=run =jump", 1);

        restoreAlfrescoSolrDataModel();
    }

    @Test
    public void exactSearch_multiTermInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception {
        /**
         * cm_ratingScheme is a copy field un-tokenized of Title, so it has the exact same content but not analysed.
         * This means we produce just a token in the index, exactly as the full content.
         * We can't expect any search to work except full exact value search
         */
        prepareExactSearchData();
        /*
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * there is no result that have cm_ratingScheme:"running" or "jumpers"
         *
         */
        assertResponseCardinality("=cm_ratingScheme:running =cm_ratingScheme:jumpers", 0);
        assertResponseCardinality("=cm_title:running =cm_title:jumpers", 4);
    }

    /**
     * Test exact search is working.
     */
    @Test
    public void exactSearch_exactPhrase_shouldReturnResultsContainingExactPhrase() throws Exception {
        prepareExactSearchData();
        /*
         * Out of the 5 'run corpus' documents
         * 1 results are expected:
         *
         * - "name", "Run",
         * "description", "you are supposed to run jump",
         * 
         */
        assertResponseCardinality("=\"run jump\"", 1);

        /*
         * No result for runner jumper, one record has runners jumpers,
         * you can see the difference between exact search and not
         * 
         * "name", "Running jumping",
         * "description", "runners jumpers run everywhere",
         */
        assertResponseCardinality("=\"runner jumper\"", 0);
        assertResponseCardinality("\"runner jumper\"", 1);

        /*
         * Out of the 5 'run corpus' documents
         * 3 results are expected:
         *
         * - "name", "Running",
         *  ...
         *   "title", "Running jumping",
         * 
         * - "name", "Poetry",
         * "title", "Running jumping twice jumpers"
         * 
         * - "name", "Running jumping",
         */
        assertResponseCardinality("=\"running jumping\"", 3);
        assertResponseCardinality("\"running jumping\"", 5);
    }



    @Test
    public void exactSearch_phraseInFieldWithNoCrossLocaleEnabled_shouldReturnException() throws Exception {
        /*
         * Cross Locale is Disabled for: content, cm_description, name
         * Cross Locale is Enabled for : cm_title
         */
        changeCrossLocaleEnabledFields();
        prepareExactSearchData();

        assertResponseCardinality("=cm_title:\"running jumping\"", 2);
        assertResponseException("=content:\"running jumping\"", "Exact Term search is not supported unless you configure the field <{http://www.alfresco.org/model/content/1.0}content> for cross locale search");
        assertResponseException("=cm_description:\"running jumping\"", "Exact Term search is not supported unless you configure the field <{http://www.alfresco.org/model/content/1.0}description> for cross locale search");
        assertResponseCardinality("=\"running jumping\"", 2);

        restoreAlfrescoSolrDataModel();
    }

    @Test
    public void exactSearch_phraseInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception {
        /**
         * cm_ratingScheme is a copy field un-tokenized of Title, so it has the exact same content but not analysed.
         * This means we produce just a token in the index, exactly as the full content.
         * We can't expect any search to work except full exact value search
         */
        prepareExactSearchData();
        /*
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * the closest we got was this one, but it is uppercase
         * - "name", "Running",
         * "title", "Running jumping",
         *
         */
        assertResponseCardinality("=cm_ratingScheme:\"running jumping\"", 0);
        assertResponseCardinality("=cm_title:\"running jumping\"", 2);

        /*
         * Out of the 5 'run corpus' documents
         * 1 results are expected:
         * - "name", "Running",
         * "title", "Running jumping",
         *
         */
        assertResponseCardinality("=cm_ratingScheme:\"Running jumping\"", 1);
        assertResponseCardinality("=cm_title:\"Running jumping\"", 2);

        /*
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * the closest we got was this one, but it is uppercase
         * - "name", "Poetry",
         * "title", "Running jumping twice jumpers",
         *
         */
        assertResponseCardinality("=cm_ratingScheme:\"Running jumping twice\"", 0);
        assertResponseCardinality("=cm_title:\"Running jumping twice\"", 1);
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
        assertResponseCardinality("[other to otherz]", 3);

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
