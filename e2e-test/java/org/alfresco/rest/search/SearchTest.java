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
import org.alfresco.utility.model.ContentModel;
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
    UserModel userModel, adminUserModel;
    SiteModel siteModel, privateSiteModel, moderatedSiteModel;
    UserModel searchedUser;
    NodesBuilder nodesBuilder;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        moderatedSiteModel = dataSite.usingUser(userModel).createModeratedRandomSite();
        
        /*
         * Create the following file structure for preconditions : 
         *   - sourceFolder
         *     - suzuki
         */
        nodesBuilder = restClient.authenticateUser(userModel).withCoreAPI().usingNode(ContentModel.my()).defineNodes();
        
        nodesBuilder.folder("source")
                      .file("suzuki")
                      .file("suzuki-2");
        
        //just for showing available properties
        System.out.println(String.format("Source Folder Full Name [%s ] withd ID: %s",nodesBuilder.getNode("source").getName(), nodesBuilder.getNode("source").getId()));
        System.out.println(String.format("Source File Full Name [%s ] withd ID: %s",nodesBuilder.getNode("suzuki").getName(), nodesBuilder.getNode("suzuki").getId()));
    }
    
    @Test
    public void searchCreatedData() throws Exception
    {        
        RestNodeModelsCollection nodes =  restClient.authenticateUser(userModel)
                                                .withCoreAPI()
                                                .usingParams("term=suzuki")
                                                .usingQueries()
                                                .findNodes();
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        nodes.assertThat()
             .entriesListIsNotEmpty()
             .and().entriesListCountIs(2);                            
    }
}
