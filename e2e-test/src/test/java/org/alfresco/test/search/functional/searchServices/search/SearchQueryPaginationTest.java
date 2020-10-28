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

package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.rest.search.SearchResponse;
import org.alfresco.search.TestGroup;
import org.alfresco.test.search.functional.searchServices.cmis.AbstractCmisE2ETest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * The purpose of this test is to test search query pagination using cmis and afts query
 * 
 * @author Meenal Bhave
 */
public class SearchQueryPaginationTest extends AbstractCmisE2ETest
{
    private UserModel testUser2;
    private FolderModel testFolder ;
    private FileModel testFile;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        // Create testUser2: This user does not have access to the testSite
        testUser2 = dataUser.createRandomTestUser("testUser2");

        // Create a new folder and 10 files inside the folder
        testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();
        
        for (int i = 0; i < 10; i++)
        {
            testFile = new FileModel(i + "-File.txt", unique_searchString, "", FileType.TEXT_PLAIN);
            dataContent.usingUser(testUser).usingSite(testSite).usingResource(testFolder).createContent(testFile);
        }
        
        waitForMetadataIndexing(testFile.getName(), true);
    }
    
    @Test(priority = 1, groups = { TestGroup.ACS_62n})
    public void testCmisSearchWithPagination()
    {
       // Search for the cmis documents using cmis query
        String query = "select * from cmis:document";

        //  Set skipCount = 0, maxItems = 1000
        SearchResponse response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(0, 1000));

        // Get getTotalItems, Expect hasModeItems = false
        Integer totalDocuments = response.getPagination().getTotalItems();
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        // Set skipCount = totalDocument, maxItems = 1000
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(totalDocuments, 1000));

        // Check getTotalItems is the same, hasMoreItems = false
        Assert.assertEquals(response.getPagination().getTotalItems(), totalDocuments, "Total Document Count doesn't match");
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        // Set skipCount = 0, maxItems = totalDocuments/2
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(0, totalDocuments/2));

        // Check getTotalItems is the same, hasMoreItems = True
        Assert.assertEquals(response.getPagination().getTotalItems(), totalDocuments, "Total Document Count doesn't match");
        Assert.assertTrue(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        // Set skipCount = 1, maxItems = totalDocuments-2
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(1, totalDocuments-2));

        // Check getTotalItems is the same, hasMoreItems = True
        Assert.assertEquals(response.getPagination().getTotalItems(), totalDocuments, "Total Document Count doesn't match");
        Assert.assertTrue(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        // Set skipCount = totalDocuments, maxItems = totalDocuments/2
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(totalDocuments, totalDocuments/2));

        // Check getTotalItems is the same, hasMoreItems = false
        Assert.assertEquals(response.getPagination().getTotalItems(), totalDocuments, "Total Document Count doesn't match");
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        // Set skipCount = 0, maxItems = totalDocuments
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(0, totalDocuments));

        // Check getTotalItems is the same, hasMoreItems = false
        Assert.assertEquals(response.getPagination().getTotalItems(), totalDocuments, "Total Document Count doesn't match");
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        // Set skipCount = totalDocuments/2+1, maxItems = totalDocuments
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(totalDocuments/2+1, totalDocuments));

        // Check getTotalItems is the same, hasMoreItems = false
        Assert.assertEquals(response.getPagination().getTotalItems(), totalDocuments, "Total Document Count doesn't match");
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        // Set skipCount = totalDocuments, maxItems = totalDocuments
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(totalDocuments, totalDocuments));

        // Check getTotalItems is the same, hasMoreItems = false
        Assert.assertEquals(response.getPagination().getTotalItems(), totalDocuments, "Total Document Count doesn't match");
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        // Set skipCount = 0, maxItems = 0
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(0, 0));
        Assert.assertTrue(response.isEmpty(), "Empty Response, Error is expected when maxItems is <= 0");

        // Set skipCount = totalDocuments, maxItems = 0
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(totalDocuments, 0));
        Assert.assertTrue(response.isEmpty(), "Empty Response, Error is expected when maxItems <= 0");

        // Set skipCount = -1, maxItems = 1
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(-1, 1));
        Assert.assertTrue(response.isEmpty(), "Empty Response, Error is expected when skipCount < 0");
    }

    @Test(priority = 2, groups = { TestGroup.ACS_62n})
    public void testPagination()
    {
       // Search for the files under the testFolder using cmis query
        String parentId = testFolder.getNodeRefWithoutVersion();
        String query = "select * from cmis:document where IN_FOLDER('" + parentId + "')";

        //  Set skipCount = 0, maxItems = 100
        SearchResponse response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(0, 100));

        // Check getTotalItems = 10, Expect hasModeItems = false
        testPaginationDetails(response, 10, 0, 100);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        //  Set skipCount = 0, maxItems = 10: Expect hasModeItems = false
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(0, 10));

        testPaginationDetails(response, 10, 0, 10);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");
 
        //  Set skipCount = 0, maxItems = 5: Expect hasModeItems = true
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(0, 5));

        testPaginationDetails(response, 10, 0, 5);
        Assert.assertTrue(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        //  Set skipCount = 2, maxItems = 10: Expect hasModeItems = false
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(2, 10));

        testPaginationDetails(response, 10, 2, 10);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        //  Set skipCount = 2, maxItems = 7: Expect hasModeItems = true
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(2, 7));

        testPaginationDetails(response, 10, 2, 7);
        Assert.assertTrue(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        //  Set skipCount = 2, maxItems = 8: Expect hasModeItems = false
        response = performSearch(testUser, query, SearchLanguage.CMIS, setPaging(2, 8));

        testPaginationDetails(response, 10, 2, 8);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");
    }

    @Test(priority = 3)
    public void testPaginationRespectsACLs()
    {
       // Search for the files under the testFolder using cmis query
        String parentId = testFolder.getNodeRefWithoutVersion();
        String query = "select * from cmis:document where IN_FOLDER('" + parentId + "')";

        //  Set skipCount = 0, maxItems = 100
        SearchResponse response = performSearch(testUser2, query, SearchLanguage.CMIS, setPaging(0, 100));

        // Get getTotalItems, Expect hasModeItems = false
        testPaginationDetails(response, 0, 0, 100);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        //  Set skipCount = 1, maxItems = 1
        response = performSearch(testUser2, query, SearchLanguage.CMIS, setPaging(1, 1));

        // Get getTotalItems, Expect hasModeItems = false
        testPaginationDetails(response, 0, 1, 1);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");
    }

    @Test(priority = 4, enabled = false)
    // https://issues.alfresco.com/jira/browse/SEARCH-2541
    public void testSearchApiPagination()
    {
        // Search for the files with specific title
        String query = "cm:title:'" + unique_searchString + "'";

        //  Set skipCount = 0, maxItems = 100
        SearchResponse response = performSearch(testUser, query, SearchLanguage.AFTS, setPaging(0, 100));

        // Get getTotalItems, Expect hasModeItems = false
        testPaginationDetails(response, 10, 0, 100);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        //  Set skipCount = 1, maxItems = 1
        response = performSearch(testUser, query, SearchLanguage.AFTS, setPaging(1, 1));

        // Get getTotalItems, Expect hasModeItems = true
        testPaginationDetails(response, 10, 1, 1);
        Assert.assertTrue(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        //  Set skipCount = 9, maxItems = 10
        response = performSearch(testUser, query, SearchLanguage.AFTS, setPaging(9, 10));

        // Get getTotalItems, Expect hasModeItems = false
        testPaginationDetails(response, 10, 9, 10);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        //  Set skipCount = 10, maxItems = 10
        response = performSearch(testUser, query, SearchLanguage.AFTS, setPaging(10, 10));

        // Get getTotalItems, Expect hasModeItems = false
        testPaginationDetails(response, 10, 10, 10);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");
    }

    @Test(priority = 5)
    public void testSearchApiPaginationRespectsACLs()
    {
       // Search for the files with specific title
        String query = "cm:title:'" + unique_searchString + "'";

        //  Set skipCount = 0, maxItems = 100
        SearchResponse response = performSearch(testUser2, query, SearchLanguage.AFTS, setPaging(0, 100));

        // Get getTotalItems, Expect hasModeItems = false
        testPaginationDetails(response, 0, 0, 100);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");

        //  Set skipCount = 1, maxItems = 1
        response = performSearch(testUser2, query, SearchLanguage.AFTS, setPaging(1, 1));

        // Get getTotalItems, Expect hasModeItems = false
        testPaginationDetails(response, 0, 1, 1);
        Assert.assertFalse(response.getPagination().isHasMoreItems(), "Incorrect: hasMoreItems");
    }

    private void testPaginationDetails(SearchResponse response, int expectedTotalCount, int skipCount, int maxItems)
    {
        Assert.assertEquals(response.getPagination().getTotalItems().intValue(), expectedTotalCount, "Unexpected document count");

        Assert.assertEquals(response.getPagination().getSkipCount(), skipCount, "Unexpected skip count returned");
        Assert.assertEquals(response.getPagination().getMaxItems(), maxItems, "Unexpected maxItems returned");

        // count = (total-skipCount) < maxItems ? total-skipCount: maxItems
        int expectedCount = (expectedTotalCount < skipCount) ? 0 : (expectedTotalCount-skipCount) < maxItems ? expectedTotalCount-skipCount : maxItems;
        Assert.assertEquals(response.getPagination().getCount(), expectedCount, "Unexpected document count");
    }
}
