/*
 * Copyright (C) 2005-2017 Alfresco Software Limited.
 * This file is part of Alfresco
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
 */

package org.alfresco.rest.trashcan;


import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestNodeModelsCollection;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.springframework.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


/**
 * Tests for /api-explorer/#!/trashcan/listDeletedNodes
 * 
 * @author jcule
 *
 */
public class GetDeletedNodesTests extends RestTest 
{
	private UserModel adminUserModel;
	private SiteModel deleteNodesSiteModel;
	private FolderModel deleteNodesFolder1, deleteNodesFolder2, deleteNodesFolder3;
	
	@BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);        
        deleteNodesSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
            
    }
	
    @AfterClass(alwaysRun=true)
    public void cleanup() throws Exception
    {
        dataSite.usingAdmin().deleteSite(deleteNodesSiteModel);
    }
	
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void testDeletedNodesBiggerThanMaxCount() throws Exception
    {
		//get the number of item in the trashcan 
		RestNodeModelsCollection deletedNodes = restClient.withCoreAPI().usingQueries().findDeletedNodes();
		int count = deletedNodes.getPagination().getCount();
		int totalItems = deletedNodes.getPagination().getTotalItems();
		
        //create folders
    	deleteNodesFolder1 = dataContent.usingUser(adminUserModel).usingSite(deleteNodesSiteModel).createFolder();
    	deleteNodesFolder2 = dataContent.usingUser(adminUserModel).usingSite(deleteNodesSiteModel).createFolder();
    	deleteNodesFolder3 = dataContent.usingUser(adminUserModel).usingSite(deleteNodesSiteModel).createFolder();
    	
        //delete folders
        dataContent.usingUser(adminUserModel).usingResource(deleteNodesFolder1).deleteContent();
        dataContent.usingUser(adminUserModel).usingResource(deleteNodesFolder2).deleteContent();
        dataContent.usingUser(adminUserModel).usingResource(deleteNodesFolder3).deleteContent();
        
        int maxItems = count + 1;
        RestNodeModelsCollection deletedNodesMaxItem = restClient.withCoreAPI().usingQueries().usingParams(String.format("maxItems=%s", maxItems)).findDeletedNodes();
         
		String countMaxItem = Integer.toString(maxItems);
		String totalItemsMaxItem = Integer.toString(totalItems + 3);
		String hasMoreItemsMaxItem = Boolean.toString(deletedNodesMaxItem.getPagination().isHasMoreItems());
		String skipCount = Integer.toString(deletedNodesMaxItem.getPagination().getSkipCount());
		
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deletedNodesMaxItem.getPagination().assertThat().field("totalItems").is(totalItemsMaxItem).and().field("count").is(countMaxItem);
        deletedNodesMaxItem.getPagination().assertThat().field("hasMoreItems").is(hasMoreItemsMaxItem).and().field("skipCount").is(skipCount);        
           	
    }
	
	
}
