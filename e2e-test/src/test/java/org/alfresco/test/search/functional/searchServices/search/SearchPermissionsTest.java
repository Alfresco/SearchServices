/*
 * Copyright (C) 2020 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.alfresco.test.search.functional.searchServices.search;

import javax.json.Json;
import javax.json.JsonObject;

import org.alfresco.rest.search.SearchResponse;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Search end point Public API test with Permission checks
 * 
 * @author Meenal Bhave
 */
public class SearchPermissionsTest extends AbstractE2EFunctionalTest
{
    private FileModel file1, file2;
    private FolderModel parentFolder, folder1, folder2;
    private UserModel testUser1, testUser2, testUser3; 

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        /*
         * Create the following file structure in the same Site  : In addition to the preconditions created in dataPreparation
         * |- permGrandParent
         *    |-- permChild1
         *    |------ permFile1 
         *    |-- permChild2
         *    |------ permFile2 
         */

        parentFolder = new FolderModel("permGrandParent");
        dataContent.usingUser(testUser).usingSite(testSite).createFolder(parentFolder);
        folder1 = dataContent.usingUser(testUser).usingSite(testSite).usingResource(parentFolder).createFolderCmisApi("permChild1");
        folder2 = dataContent.usingUser(testUser).usingSite(testSite).usingResource(parentFolder).createFolderCmisApi("permChild2");

        file1 = new FileModel("permFile1", FileType.TEXT_PLAIN, "File1 with inherited permissions");
        file2 = new FileModel("permFile2", FileType.TEXT_PLAIN, "File2 with inherited permissions");
        
        // Create test users
        testUser1 = dataUser.createRandomTestUser("UserSiteMember");
        testUser2 = dataUser.createRandomTestUser("UserNotASiteMemeber");
        testUser3 = dataUser.createRandomTestUser("UserWithoutContentAccess");

        dataUser.addUserToSite(testUser1, testSite, UserRole.SiteCollaborator);

        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder1).createContent(file1);
        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder2).createContent(file2);
        
        // Deny testUser1 - access to file1
        JsonObject userPermission = Json
                .createObjectBuilder()
                .add("permissions",
                        Json.createObjectBuilder()
                                .add("isInheritanceEnabled", false)
                                .add("locallySet",
                                        Json.createObjectBuilder()
                                            .add("authorityId", testUser1.getUsername())
                                            .add("name", "SiteCollaborator")
                                            .add("accessStatus", "DENIED")
                                        )).build();
        String putBody = userPermission.toString();
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(file1).updateNode(putBody);

        // Allow testUser2 - access to file2, user is not a site member
        userPermission = Json
                .createObjectBuilder()
                .add("permissions",
                        Json.createObjectBuilder()
                                .add("isInheritanceEnabled", false)
                                .add("locallySet",
                                        Json.createObjectBuilder()
                                            .add("authorityId", testUser2.getUsername())
                                            .add("name", "SiteContributor")
                                            .add("accessStatus", "ALLOWED")
                                        )).build();
        putBody = userPermission.toString();
        restClient.authenticateUser(testUser).withCoreAPI().usingNode(file2).updateNode(putBody);

        waitForContentIndexing(file2.getContent(), true);
    }
    
    @Test
    public void searchResultsRespectInheritedPermissions()
    {
        // Search as testUser: expect all: 5 results: When user is a Site Manager
        SearchResponse response = queryAsUser(testUser, "cm:name:perm*");
        int resultCount = response.getPagination().getCount();
        Assert.assertTrue(resultCount == 5, "Unexpected Result count for testUser: Expected 5, received: " + resultCount);

        // Search as testUser1: expect 3 results: when user is a site member but without permission to a content
        response = queryAsUser(testUser1, "cm:name:perm*");
        resultCount = response.getPagination().getCount();
        Assert.assertTrue(resultCount == 3, "Unexpected Result count for testUser1: Expected 3, received: " + resultCount);

        // Search as testUser2: expect 1 result: When user isn't a site member but has granular permissions to a content
        response = queryAsUser(testUser2, "cm:name:perm*");
        resultCount = response.getPagination().getCount();
        Assert.assertTrue(resultCount == 1, "Unexpected Result count for testUser2: Expected 1, received: " + resultCount);

        // Search as testUser3: expect none: 0 results: When user isn't a site member / does not have granular permissions to the content
        response = queryAsUser(testUser3, "cm:name:perm*");
        resultCount = response.getPagination().getCount();
        Assert.assertTrue(resultCount == 0, "Unexpected Result count for testUser3: Expected 0, received: " + resultCount);
    }
}
