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

package org.alfresco.test.search.functional.searchServices.solr.admin;

import static org.testng.Assert.assertEquals;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.springframework.context.annotation.Configuration;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests validating the results of SOLR REST API Actions
 */
@Configuration
public class SolrE2eActionTest extends AbstractE2EFunctionalTest
{

    // DBID (sys:node-dbid) value for the document
    Integer dbId;

    /**
     * Create a new document and get the DBID value for it
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {

        // Create a new document
        FolderModel folder = new FolderModel("folder-aspect");
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(folder, "cmis:folder",
                    new CustomObjectTypeProperties());

        FileModel file = new FileModel("file-aspect-" + System.currentTimeMillis() + ".txt");
        file.setContent("content file aspect");
        dataContent.usingUser(testUser).usingResource(folder).createCustomContent(file, "cmis:document",
                    new CustomObjectTypeProperties());

        waitForMetadataIndexing(file.getName(), true);

        // Get the DBID for the create document
        String queryFile = "cm:name:" + file.getName();
        restClient.authenticateUser(dataContent.getAdminUser()).withParams(queryFile).withSolrAPI()
                    .getSelectQueryJson();
        dbId = Integer.valueOf(
                    restClient.onResponse().getResponse().body().jsonPath().get("response.docs[0].DBID").toString());

    }

    /**
     * REINDEX for specific core using DBID
     * 
     * @throws Exception
     */
    @Test
    public void testReindexNodeId()
    {

        final String deleteQueryBody = "{\"delete\":{\"query\": \"DBID:" + dbId + "\"}}";

        try
        {
            // Remove document from SOLR
            restClient.withSolrAPI().postAction("delete", deleteQueryBody);

            // Re-index document using nodeId
            RestResponse response = restClient.withParams("core=alfresco", "nodeId=" + dbId).withSolrAdminAPI()
                        .getAction("reindex");
            String actionStatus = response.getResponse().body().jsonPath().get("action.alfresco.status");
            Assert.assertEquals(actionStatus, "scheduled");
            waitForMetadataIndexing("DBID:" + dbId, true);

            // Verify the node has been re-indexed to its original type "cm:content"
            String queryFile = "DBID:" + dbId;
            RestRequestQueryModel queryModel = new RestRequestQueryModel();
            queryModel.setQuery(queryFile);
            queryModel.setLanguage(SearchLanguage.AFTS.toString());
            SearchRequest searchRequest = new SearchRequest();
            searchRequest.setQuery(queryModel);
            SearchResponse searchResponse = restClient.authenticateUser(testUser).withSearchAPI().search(searchRequest);
            assertEquals(searchResponse.getEntries().get(0).getModel().getNodeType(), "cm:content");

        }
        catch (Exception e)
        {
            throw new RuntimeException(e);
        }
    }

}
