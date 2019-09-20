/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.test.search.functional.gs.search;

import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.test.search.functional.gs.AbstractGSE2ETest;
import org.alfresco.utility.model.FolderModel;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The purpose of this test is to create basic queries using a RM site and a basic file plan.
 * 
 * @author Cristina Diaconu
 * @author Meenal Bhave
 */
public class SearchServicesGSE2ETest extends AbstractGSE2ETest
{

    @Test(priority = 1)
    public void testBasicSearch()
    {
        // Create a new folder
        FolderModel testFolder = dataContent.usingUser(testUser).usingSite(testSite).createFolder();

        // Search for the new folder using folder name
        boolean indexingInProgress = isContentInSearchResults(testFolder.getName(), testFolder.getName(), true);

        Assert.assertTrue(indexingInProgress, "Expected folder is not found: " + testFolder.getName());

        // Search for a folder name using cmis query
        String query = "select * from cmis:folder where cmis:name='" + testFolder.getName() + "'";

        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setQuery(query);
        queryModel.setLanguage(SEARCH_LANGUAGE_CMIS);

        SearchResponse response = queryAsUser(dataUser.getAdminUser(), queryModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, "Expected folder is not found: " + testFolder.getName());

        // Search for a file name
        query = "select * from cmis:document where cmis:name like '" + NON_ELECTRONIC_FILE + "%'";

        queryModel = new RestRequestQueryModel();
        queryModel.setQuery(query);
        queryModel.setLanguage(SEARCH_LANGUAGE_CMIS);

        response = queryAsUser(dataUser.getAdminUser(), queryModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        Assert.assertEquals(response.getPagination().getCount(), 1, "Expected file is not found: " + NON_ELECTRONIC_FILE);
    }

}
