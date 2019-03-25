/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.service.search.e2e.rm.searchServices;

import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.service.search.e2e.AbstractRMSearchServiceE2E;
import org.alfresco.utility.model.TestGroup;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * The purpose of this test is to create basic queries using a RM site and a basic file plan.
 * 
 * @author Cristina Diaconu
 */
public class SearchServicesRME2ETest extends AbstractRMSearchServiceE2E
{

    @Test(priority = 1, groups = { TestGroup.ASS_14 })
    public void testBasicSearch() throws Exception
    {

        // Search for a folder name
        String query = "select * from cmis:folder where cmis:name='" + FOLDER1 + "'";

        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setQuery(query);
        queryModel.setLanguage(SEARCH_LANGUAGE);

        SearchResponse response = queryAsUser(dataUser.getAdminUser(), queryModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        Assert.assertEquals(response.getPagination().getCount(), 2,
                    "Wrong number of search elements, expected two folders.");

        // Search for a file name
        query = "select * from cmis:document where cmis:name like '" + NON_ELECTRONIC_FILE + "%'";

        queryModel = new RestRequestQueryModel();
        queryModel.setQuery(query);
        queryModel.setLanguage(SEARCH_LANGUAGE);

        response = queryAsUser(dataUser.getAdminUser(), queryModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        Assert.assertEquals(response.getPagination().getCount(), 1,
                    "Wrong number of search elements, expected 1 non electronic file");
    }

}
