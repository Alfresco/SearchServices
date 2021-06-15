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
 * This class is capturing expected results that are not working currently.
 * This test should be addressed by https://alfresco.atlassian.net/browse/SEARCH-2461
 */
@Test(enabled=false)
public class SearchExactTermTestFailing extends AbstractSearchExactTermTest
{
    
    @Test
    public void exactSearch_singleTermInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception 
    {
        
        /*
         * Following queries will get results directly from DB
         * 1 result is expected (Running)
         */
        assertResponseCardinality("=tok:false:running", 1);
        assertException("=tok:true:running");
        assertException("=tok:both:running");
        
        /*
         * Following queries will get results directly from DB
         * 1 result is expected (Running)
         */
        assertResponseCardinality("=tok:false:Running", 1);
        assertException("=tok:true:Running");
        assertException("=tok:both:Running");
        
        /*
         * Following queries will get results directly from DB
         * No results are expected, as there is no "Run" value
         */
        assertResponseCardinality("=tok:false:Run", 0);
        assertException("=tok:true:Run");
        assertException("=tok:both:Run");

    }
    
    @Test
    public void exactSearch_multiTermInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception 
    {
        /*
         * Following queries will get results directly from DB
         * As there is no "running" or "jumper" value, 0 results are expected
         */
        assertResponseCardinality("=tok:false:running =tok:false:jumpers", 0);
        assertException("=tok:both:running =tok:both:jumpers");
        assertException("=tok:true:running =tok:true:jumpers");
        
    }
    
    @Test
    public void exactSearch_phraseInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception 
    {
        
        /*
         * Following queries will get results directly from DB
         * As there is one "running jumping" value, 1 result are expected
         */
        assertResponseCardinality("=tok:false:\"running jumping\"", 1);
        assertException("=tok:true:\"running jumping\"");
        assertException("=tok:both:\"running jumping\"");

        /*
         * Following queries will get results directly from DB
         * As there is one "running jumping" value, 1 result are expected
         */
        assertResponseCardinality("=tok:false:\"Running jumping\"", 1);
        assertException("=tok:true:\"Running jumping\"");
        assertException("=tok:both:\"Running jumping\"");
        
        /*
         * Following queries will get results directly from DB
         * As there is none "Running jumping twice" value, 0 results are expected
         */
        assertResponseCardinality("=tok:false:\"Running jumping twice\"", 0);
        assertException("=tok:true:\"Running jumping twice\"");
        assertException("=tok:both:\"Running jumping twice\"");
        
    }
    
}
