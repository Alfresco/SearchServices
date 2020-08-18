/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail. Otherwise, the software is
 * provided under the following open source license terms:
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package org.alfresco.test.search.functional.searchServices.search;

import static org.testng.Assert.assertTrue;

import java.util.List;

import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test class tests aspects are added and removed from Solr Documents
 * Created for Search-2379
 */
public class SearchAspectTest extends AbstractE2EFunctionalTest
{
    private FolderModel folder;
    private FileModel file;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation()
    {
        folder = new FolderModel("folder-aspect");

        file = new FileModel("file-aspect.txt");
        file.setContent("content file aspect");

        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(folder, "cmis:folder",
                new CustomObjectTypeProperties());

        dataContent.usingUser(testUser).usingResource(folder).createCustomContent(file, "cmis:document",
                new CustomObjectTypeProperties());

        waitForMetadataIndexing(file.getName(), true);
    }

    @Test(priority = 1)
    public void testAspectIsRemoved() throws Exception
    {

        // When checking out a file, cm:checkedOut aspect is added
        cmisApi.authenticateUser(testUser).usingResource(file).checkOut();

        String queryFile = "cm:name:'" + file.getName() + "'";

        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setQuery(queryFile);
        queryModel.setLanguage(SearchLanguage.AFTS.toString());
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(queryModel);
        searchRequest.setInclude(List.of("aspectNames"));

        SearchResponse response = restClient.authenticateUser(testUser).withSearchAPI().search(searchRequest);
        assertTrue(response.getEntries().get(0).getModel().getAspectNames().contains("cm:checkedOut"),
                "checkedOut aspect expected");

        // When cancelling the check out of a file, cm:checkedOut aspect is removed
        cmisApi.authenticateUser(testUser).usingResource(file).cancelCheckOut();
        cmisApi.authenticateUser(testUser).usingResource(file).updateProperty(PropertyIds.NAME,
                "file-aspect-random.txt");

        waitForMetadataIndexing("file-aspect-random.txt", true);

        queryModel.setQuery("cm:name:'file-aspect-random.txt'");
        searchRequest.setQuery(queryModel);
        response = restClient.authenticateUser(testUser).withSearchAPI().search(searchRequest);

        assertTrue(!response.getEntries().get(0).getModel().getAspectNames().contains("cm:checkedOut"),
                "checkedOut aspect was NOT expected");

    }
}
