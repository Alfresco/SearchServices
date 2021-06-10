/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.test.search.functional.AbstractSearchExactTermTest;
import org.testng.annotations.Test;

/**
 * Tests including all different tokenization (false, true, both) modes with Exact Term queries.
 * Since Search Services are not configured with Cross Locale enabled, some errors and omissions are expected.
 * These tests are based in AFTSDefaultTextQueryIT class, but an additional type of property 
 * has been added (tok:true) in order to provide full coverage for the available options.
 * 
 * Since tok:true and tok:both properties are not supported to be used with exact term search,
 * exception from this queries is expected.
 * 
 * SOLR log is dumping the cause of the error, for instance:
 *   java.lang.UnsupportedOperationException: Exact Term search is not supported unless you configure the field 
 *   <{http://www.alfresco.org/model/tokenised/1.0}true> for cross locale search
 */
public class SearchExactTermTest extends AbstractSearchExactTermTest
{
    
    @Test
    public void exactSearch_singleTerm_shouldReturnResultsContainingExactTerm() throws Exception
    {
        /*
         * Out of the 5 'run corpus' documents 
         * 1 result is expected:
         * 
         * - "name", "Run",
         * "description", "you are supposed to run jump"
         * 
         */
        assertResponseCardinality("=run", 1);
        
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
         * 1 result is expected
         * 
         * - "name", "Jump",
         * "description", "a document about jumps"
         */
        assertResponseCardinality("=jump", 1);
        
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
        assertException("=tok:true:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
        assertException("=tok:both:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
        
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
        assertException("=tok:true:Running AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
        assertException("=tok:both:Running AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
        
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
        assertException("=tok:true:Run AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
        assertException("=tok:both:Run AND cm:created:['" + fromDate + "' TO '" + toDate + "']");

    }
    
    @Test
    public void exactSearch_multiTerm_shouldReturnResultsContainingExactTerm() throws Exception 
    {
        /*
         * Out of the 5 'run corpus' documents
         * 2 results are expected:
         *
         * - "name", "Run",
         * "description", "you are supposed to run jump",
         * 
         * - "name", "Jump",
         * "description", "a document about jumps",
         * 
         */
        assertResponseCardinality("=run =jump", 2);

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
         * 2 results are expected:
         * Only one doc fits:
         * - "name", "Running jumping",
         * "description", "runners jumpers run everywhere",
         * - "name", "Running",
         * "title", "Running jumping"
         */
        assertResponseCardinality("=running =jumping", 2);
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
        assertException("=tok:both:running =tok:both:jumpers AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
        assertException("=tok:true:running =tok:true:jumpers AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
    }
    
    @Test
    public void exactSearch_exactPhrase_shouldReturnResultsContainingExactPhrase() throws Exception 
    {
        /*
         * Out of the 5 'run corpus' documents
         * 0 results are expected.
         */
        assertResponseCardinality("=\"run jump\"", 0);

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
         * 1 results is expected:
         *
         * - "name", "Running jumping",
         */
        assertResponseCardinality("=\"running jumping\"", 1);
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
        assertException("=tok:true:\"running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
        assertException("=tok:both:\"running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']");

        /*
         * Following queries will get results directly from DB
         * As there is one "running jumping" value, 1 result are expected
         */
        assertResponseCardinality("=tok:false:\"Running jumping\"", 1);
        assertResponseCardinality("=tok:true:\"Running jumping\"", 1);
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
        assertException("=tok:true:\"Running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
        assertException("=tok:both:\"Running jumping\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']");


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
        assertException("=tok:true:\"Running jumping twice\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
        assertException("=tok:both:\"Running jumping twice\" AND cm:created:['" + fromDate + "' TO '" + toDate + "']");
    }
    
}
