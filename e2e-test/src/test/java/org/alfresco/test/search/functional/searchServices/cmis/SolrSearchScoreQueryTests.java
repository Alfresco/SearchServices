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

package org.alfresco.test.search.functional.searchServices.cmis;

import java.math.BigDecimal;

import org.alfresco.search.TestGroup;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.data.provider.XMLTestDataProvider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.Assert;
import org.testng.annotations.AfterClass;
import org.testng.annotations.Test;

/**
 * Testing SCORE function queries to solve issues related to
 * https://issues.alfresco.com/jira/browse/ACE-2479
 * 
 * @author aborroy
 *
 */
public class SolrSearchScoreQueryTests extends AbstractCmisE2ETest
{
	
    private static Logger LOGGER = LoggerFactory.getLogger(SolrSearchScoreQueryTests.class);
    private XMLTestData testData;
    
    @AfterClass(alwaysRun = true)
    public void cleanupEnvironment()
    {
        if (testData != null)
        {
            testData.cleanup(dataContent);
        }
        else
        {
            LOGGER.warn("testData is inexplicably null - skipping clean up.");
        }
    }
    
    /* These tests does not require common data preparation from AbstractE2EFunctionalTest
     * as it's including every test data required in search-score-funtion.xml
     * @see org.alfresco.test.search.functional.AbstractE2EFunctionalTest#dataPreparation()
     */
    @Override
    public void setup()
    {
    }
    
    @Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getAllData")
    @XMLDataConfig(file = "src/test/resources/testdata/search-score-function.xml")
    public void prepareDataForScoreSearch(XMLTestData testData) throws Exception
    {
        this.testData = testData;
        this.testData.createUsers(dataUser);
        this.testData.createSitesStructure(dataSite, dataContent, dataUser);
        testUser = dataUser.getCurrentUser();
        cmisApi.authenticateUser(testUser);
    }

    /**
     * Verify that results are ordered
     * @throws Exception
     */
    @Test(dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryOrdered() throws Exception
    {
        
        String query = "SELECT cmis:objectId, SCORE() AS orderCriteria "
                + "FROM cmis:document "
                + "WHERE CONTAINS('Quidditch') " 
                + "ORDER BY orderCriteria";

        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));

        cmisApi.withQuery(query).assertColumnIsOrdered().isOrderedAsc("orderCriteria");

    }

    /**
     * Verify that results are inverse ordered
     * 
     * @throws Exception
     */
    @Test(dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryOrderedDesc() throws Exception
    {

        String query = "SELECT cmis:objectId, SCORE() AS orderCriteria "
                + "FROM cmis:document "
                + "WHERE CONTAINS('Quidditch') "
                + "ORDER BY orderCriteria DESC";

        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));

        cmisApi.withQuery(query).assertColumnIsOrdered().isOrderedDesc("orderCriteria");

    }

    /**
     * Verify that all SCORE results are between 0 and 1
     * 
     * @throws Exception
     */
    @Test(groups = { TestGroup.ACS_62n }, dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryInRange() throws Exception
    {

        String query = "SELECT cmis:objectId, SCORE() " 
                     + "FROM cmis:document " 
                     + "WHERE CONTAINS('Quidditch')";
    	
        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));

        cmisApi.withQuery(query).assertColumnValuesRange().isReturningValuesInRange("SEARCH_SCORE", BigDecimal.ZERO, BigDecimal.ONE);

    }
    
    /**
     * Verify that all SCORE results are between 0 and 1
     * @throws Exception
     */
    @Test(groups = { TestGroup.ACS_62n }, dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryAliasInRange() throws Exception
    {
        
        String query = "SELECT cmis:objectId, SCORE() AS orderCriteria "
                    + "FROM cmis:document "
                    + "WHERE CONTAINS('Quidditch')";

        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));

        cmisApi.withQuery(query).assertColumnValuesRange().isReturningValuesInRange("orderCriteria", BigDecimal.ZERO, BigDecimal.ONE);        

    }

    /**
     * Verify that SCORE is valid name for an alias
     * Currently only supported with double quotes
     * 
     * @throws Exception
     */
    @Test(dependsOnMethods = "prepareDataForScoreSearch")
    public void scoreQueryScoreAsAlias() throws Exception
    {

        String query = "SELECT cmis:objectId, SCORE() AS \"score\" " 
                    + "FROM cmis:document " 
                    + "WHERE CONTAINS('Quidditch')";

        Assert.assertTrue(waitForIndexing(query, 3), String.format("Result count not as expected for query: %s", query));
 
        cmisApi.withQuery(query).assertResultsCount().equals(3);

    }	

}
