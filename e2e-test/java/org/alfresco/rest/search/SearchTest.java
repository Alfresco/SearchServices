/*
 * Copyright (C) 2017 Alfresco Software Limited.
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
package org.alfresco.rest.search;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestNodeModelsCollection;
import org.alfresco.rest.model.builder.NodesBuilder;
import org.alfresco.rest.model.builder.NodesBuilder.NodeDetail;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Search end point Public API test.
 * @author Michael Suzuki
 *
 */
public class SearchTest extends RestTest
{
    private static final String SEARCH_DATA_SAMPLE_FOLDER = "folder";
    UserModel userModel, adminUserModel;
    SiteModel siteModel;
    UserModel searchedUser;
    NodesBuilder nodesBuilder;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        /*
         * Create the following file structure for preconditions : 
         *   |- folder
         *        |-- pangram.txt
         *        |-- cars.pdf
         */
        nodesBuilder = restClient.authenticateUser(userModel).withCoreAPI().usingNode(ContentModel.my()).defineNodes();
        FolderModel folder = new FolderModel(SEARCH_DATA_SAMPLE_FOLDER);
        dataContent.usingSite(siteModel).createFolder(folder);
        //Create files
        FileModel file = new FileModel("pangram.txt", FileType.TEXT_PLAIN, "The qucik brown fox jumps over the lazy dog");
        FileModel file2 = new FileModel("cars.txt", FileType.TEXT_PLAIN, "The landrover discovery is not a sports car");
        ContentModel cm = new ContentModel();
        cm.setCmisLocation(folder.getCmisLocation());
        cm.setName(folder.getName());
        dataContent.usingSite(siteModel).usingResource(cm).createContent(file);
        dataContent.usingSite(siteModel).usingResource(cm).createContent(file2);
        
        }
    
    @Test
    public void searchCreatedData() throws Exception
    {        
        RestNodeModelsCollection nodes =  restClient.authenticateUser(userModel)
                                                .withCoreAPI()
                                                .usingParams("term=cars")
                                                .usingQueries()
                                                .findNodes();
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsNotEmpty();
       
        nodes =  restClient.authenticateUser(userModel)
                .withCoreAPI()
                .usingParams("term=fox")
                .usingQueries()
                .findNodes();
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsNotEmpty();
    }
}
