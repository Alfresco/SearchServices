/*
 * #%L
 * Alfresco Search Services E2E Test
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

package org.alfresco.test.search.functional.searchServices.search.crosslocale;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.rest.search.SearchResponse;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Tests including all different tokenization (false, true, both) modes with Exact Term queries.
 * Search Services must be configured with Cross Locale enabled in order to run these tests.
 * These tests are based in AFTSDefaultTextQueryIT class, but an additional type of property 
 * has been added (tok:true) in order to provide full coverage for the available options.
 */
public class SearchExactTermCrossLocaleTest extends AbstractE2EFunctionalTest
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;
    
    private static final DateFormat QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private String fromDate;
    private String toDate;
    
    private void prepareExactSearchData(FolderModel testFolder) throws Exception {

        List<Map<String, String>> exactSearchData = asList(
                    of("name", "Running",
                            "description", "Running is a sport is a nice activity",
                            "content", "when you are running you are doing an amazing sport",
                            "title", "Running jumping"),
                    of("name", "Run",
                            "description", "you are supposed to run jump",
                            "content", "after many runs you are tired and if you jump it happens the same",
                            "title", "Run : a philosophy"),
                    of("name", "Poetry",
                            "description", "a document about poetry and jumpers",
                            "content", "poetry is unrelated to sport",
                            "title", "Running jumping twice jumpers"),
                    of("name", "Jump",
                            "description", "a document about jumps",
                            "content", "runnings jumpings",
                            "title", "Running"),
                    of("name", "Running jumping",
                            "description", "runners jumpers runs everywhere",
                            "content", "run is Good as jump",
                            "title", "Running the art of jumping"));

        List<FileModel> createdFileModels = new ArrayList<>();
        range(0, exactSearchData.size())
                .forEach(id -> {

                    Map<String, String> record = exactSearchData.get(id);

                    Map<String, Object> properties = new HashMap<>();
                    properties.put(PropertyIds.OBJECT_TYPE_ID, "D:tok:document");
                    properties.put(PropertyIds.NAME, record.get("name"));
                    properties.put("cm:title", record.get("title"));
                    properties.put("cm:description", record.get("description"));
                    properties.put("tok:true", record.get("title"));
                    properties.put("tok:false", record.get("title"));
                    properties.put("tok:both", record.get("title"));
                    properties.put(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, List.of("P:cm:titled"));
                    
                    FileModel fileModel = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, record.get("content"));
                    fileModel.setName(record.get("name"));

                    cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
                                .createFile(fileModel, properties, VersioningState.MAJOR)
                                .assertThat().existsInRepo();
                    
                    createdFileModels.add(fileModel);
                    
                });
        
        waitForContentIndexing(createdFileModels.get(createdFileModels.size() - 1).getName(), true);
        
    }
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();
        
        deployCustomModel("model/tokenised-model.xml");
        
        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);

        FolderModel testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();
        prepareExactSearchData(testFolder);
        
        // Calculate time query range, required for conjunction queries
        Date today = new Date();
        
        LocalDateTime yesterday = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        yesterday = yesterday.plusDays(-1);
        fromDate = QUERY_DATE_FORMAT.format(Date.from(yesterday.atZone(ZoneId.systemDefault()).toInstant()));
        
        LocalDateTime tomorrow = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        tomorrow = tomorrow.plusDays(1);
        toDate = QUERY_DATE_FORMAT.format(Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant()));
        
    }
    
    protected void assertResponseCardinality(String query, int num)
    {
        SearchResponse response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), num, query);
    }
    
    @Test
    public void exactSearch_singleTerm_shouldReturnResultsContainingExactTerm() throws Exception
    {
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
         * Note that we are not using 'running' this time as 'Milestone' wiki page (coming from ootb content)
         * is including "running" in the content
         * 3 results are expected, only two docs are not fit for the query
         * 
         * - "name", "Running",
         * "description", "Running is a sport is a nice activity",
         * "content", "when you are running you are doing an amazing sport",
         * "title", "Running jumping"
         *
         * - "name", "Poetry",
         * "description", "a document about poetry and jumpers",
         * "content", "poetry is unrelated to sport",
         * "title", "Running jumping twice jumpers"
         */
        assertResponseCardinality("=jump", 3);
        
    }
    
    @Test
    public void exactSearch_singleTermInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception 
    {
        
        /**
         * tok:false is a copy field un-tokenized of tok:both/tok:true, so it has the exact same content but not analysed.
         * This means we produce just a token in the index, exactly as the full content.
         * We can't expect any search to work except full exact value search.
         * 
         * Since REST API is getting the results from DB or Search Services, using single term expressions is always
         * retrieved from DB. Combining this single term with range queries (like cm:created) will ensure the results
         * are coming from SOLR. 
         */
        
        /*
         * Following queries will get results directly from DB
         * As there is no "running" value, 0 results are expected
         */
        assertResponseCardinality("=tok:false:running", 0);
        assertResponseCardinality("=tok:true:running", 0);
        assertResponseCardinality("=tok:both:running", 0);
        
        /*
         * Following queries will get results from SOLR
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * there is no result that have tok_false:"running"
         *
         */
        assertResponseCardinality("=tok:false:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 0);
        assertResponseCardinality("=tok:true:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 4);
        assertResponseCardinality("=tok:both:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 4);
        
        /*
         * Following queries will get results directly from DB
         * 1 result is expected
         */
        assertResponseCardinality("=tok:false:Running", 1);
        assertResponseCardinality("=tok:true:Running", 1);
        assertResponseCardinality("=tok:both:Running", 1);
        
        /*
         * Following queries will get results from SOLR
         * Out of the 5 'run corpus' documents
         * 1 result is expected:
         * 
         * - "name", "Jump",
         *  ...
         *  "title", "Running"
         *
         */
        assertResponseCardinality("=tok:false:Running AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 1);
        assertResponseCardinality("=tok:true:Running AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 4);
        assertResponseCardinality("=tok:both:Running AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 4);
        
        /*
         * Following queries will get results directly from DB
         * As there is no "Run" value, 0 results are expected
         */
        assertResponseCardinality("=tok:false:Run", 0);
        assertResponseCardinality("=tok:true:Run", 0);
        assertResponseCardinality("=tok:both:Run", 0);


        /*
         * Following queries will get results from SOLR
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * there is no result that have exactly tok:false:"Run"
         * The closest we have is record Run (tok:false:"Run : a philosophy")
         * As you can see we don't have a full match, so it's not in the results.
         *
         */
        assertResponseCardinality("=tok:false:Run AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 0);
        assertResponseCardinality("=tok:true:Run AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 1);
        assertResponseCardinality("=tok:both:Run AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 1);


    }
    
    @Test
    public void exactSearch_multiTerm_shouldReturnResultsContainingExactTerm() throws Exception 
    {
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
         *  "creator", "Alex"
         *  
         * Since 'Milestone' wiki page (coming from ootb content) is including "running" in the content,
         * we are checking for 5 results instead of 4
         *               
         */
        assertResponseCardinality("=running =jumping", 5);
    }
    
    @Test
    public void exactSearch_multiTermInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception 
    {
        /**
         * tok:false is a copy field un-tokenized of tok:both/tok:true, so it has the exact same content but not analysed.
         * This means we produce just a token in the index, exactly as the full content.
         * We can't expect any search to work except full exact value search.
         */

        /*
         * Following queries will get results directly from DB
         * As there is no "running" or "jumper" value, 0 results are expected
         */
        assertResponseCardinality("=tok:false:running =tok:false:jumpers", 0);
        assertResponseCardinality("=tok:both:running =tok:both:jumpers", 0);
        assertResponseCardinality("=tok:true:running =tok:true:jumpers", 0);
        
        /*
         * Following queries will get results from SOLR
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * there is no result that have tok:false:"running" or "jumpers"
         *
         */
        assertResponseCardinality("=tok:false:running =tok:false:jumpers AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 0);
        assertResponseCardinality("=tok:both:running =tok:both:jumpers AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 4);
        assertResponseCardinality("=tok:true:running =tok:true:jumpers AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 4);
    }
    
    @Test
    public void exactSearch_exactPhrase_shouldReturnResultsContainingExactPhrase() throws Exception 
    {
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
    public void exactSearch_phraseInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception 
    {
        /**
         * tok:false is a copy field un-tokenized of tok:both/tok:true, so it has the exact same content but not analysed.
         * This means we produce just a token in the index, exactly as the full content.
         * We can't expect any search to work except full exact value search.
         */
        
        /*
         * Following queries will get results directly from DB
         * As there is no "running jumping" value, 0 results are expected
         */
        assertResponseCardinality("=tok:false:\"running jumping\"", 0);
        assertResponseCardinality("=tok:true:\"running jumping\"", 0);
        assertResponseCardinality("=tok:both:\"running jumping\"", 0);

        /*
         * Following queries will get results from SOLR
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * the closest we got was this one, but it is uppercase
         * - "name", "Running",
         * "title", "Running jumping",
         *
         */
        assertResponseCardinality("=tok:false:\"running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 0);
        assertResponseCardinality("=tok:true:\"running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 2);
        assertResponseCardinality("=tok:both:\"running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 2);

        /*
         * Following queries will get results directly from DB
         * As there is one "running jumping" value, 1 result are expected
         */
        assertResponseCardinality("=tok:false:\"Running jumping\"", 1);
        assertResponseCardinality("=tok:true:\"running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 2);
        assertResponseCardinality("=tok:both:\"Running jumping\"", 1);
        
        /*
         * Following queries will get results from SOLR
         * Out of the 5 'run corpus' documents
         * 1 results are expected:
         * - "name", "Running",
         * "title", "Running jumping",
         *
         */
        assertResponseCardinality("=tok:false:\"Running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 1);
        assertResponseCardinality("=tok:true:\"Running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 2);
        assertResponseCardinality("=tok:both:\"Running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 2);


        /*
         * Following queries will get results directly from DB
         * As there is none "Running jumping twice" value, 0 results are expected
         */
        assertResponseCardinality("=tok:false:\"Running jumping twice\"", 0);
        assertResponseCardinality("=tok:true:\"Running jumping twice\"", 0);
        assertResponseCardinality("=tok:both:\"Running jumping twice\"", 0);
        
        /*
         * Following queries will get results from SOLR
         * Out of the 5 'run corpus' documents
         * 0 results are expected:
         * the closest we got was this one, but it is uppercase
         * - "name", "Poetry",
         * "title", "Running jumping twice jumpers",
         *
         */
        assertResponseCardinality("=tok:false:\"Running jumping twice\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 0);
        assertResponseCardinality("=tok:true:\"Running jumping twice\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 1);
        assertResponseCardinality("=tok:both:\"Running jumping twice\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']", 1);
    }
    
}
