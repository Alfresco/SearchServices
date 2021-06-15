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

package org.alfresco.test.search.functional.searchServices.search.crosslocale;

import org.alfresco.test.search.functional.AbstractSearchExactTermTest;
import org.testng.annotations.Test;


/**
 * This class is capturing expected results that are not working currently.
 * This test should be addressed by https://alfresco.atlassian.net/browse/SEARCH-2461
 */
public class SearchExactTermCrossLocaleTestFailing extends AbstractSearchExactTermTest
{
    
    @Test(enabled=false)
    public void exactSearch_singleTermInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception 
    {
        
        /*
         * Following queries will get results directly from DB
         */
        assertResponseCardinality("=tok:false:running", 1);
        assertResponseCardinality("=tok:true:running", 4);
        assertResponseCardinality("=tok:both:running", 4);
        
        /*
         * Following queries will get results directly from DB
         */
        assertResponseCardinality("=tok:false:Running", 1);
        assertResponseCardinality("=tok:true:Running", 4);
        assertResponseCardinality("=tok:both:Running", 4);
        
        /*
         * Following queries will get results directly from DB
         */
        assertResponseCardinality("=tok:false:Run", 0);
        assertResponseCardinality("=tok:true:Run", 1);
        assertResponseCardinality("=tok:both:Run", 1);


    }
    
    @Test(enabled=false)
    public void exactSearch_multiTermInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception 
    {
        /*
         * Following queries will get results directly from DB
         */
        assertResponseCardinality("=tok:false:running =tok:false:jumpers", 0);
        assertResponseCardinality("=tok:both:running =tok:both:jumpers", 4);
        assertResponseCardinality("=tok:true:running =tok:true:jumpers", 4);
        
    }
    
    @Test(enabled=false)
    public void exactSearch_phraseInFieldWithOnlyUnTokenizedAnalysis_shouldReturnFullFieldValueMatch() throws Exception 
    {
        /*
         * Following queries will get results directly from DB
         */
        assertResponseCardinality("=tok:false:\"running jumping\"", 1);
        assertResponseCardinality("=tok:true:\"running jumping\"", 2);
        assertResponseCardinality("=tok:both:\"running jumping\"", 2);

        /*
         * Following queries will get results directly from DB
         */
        assertResponseCardinality("=tok:false:\"Running jumping\"", 1);
        assertResponseCardinality("=tok:true:\"Running jumping\"", 2);
        assertResponseCardinality("=tok:both:\"Running jumping\"", 2);
        
        /*
         * Following queries will get results directly from DB
         */
        assertResponseCardinality("=tok:false:\"Running jumping twice\"", 0);
        assertResponseCardinality("=tok:true:\"Running jumping twice\"", 1);
        assertResponseCardinality("=tok:both:\"Running jumping twice\"", 1);
        
    }
    
}
