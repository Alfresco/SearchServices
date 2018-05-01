/*
 * Copyright (C) 2018 Alfresco Software Limited.
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

import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.builder.NodesBuilder;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.testng.annotations.BeforeClass;

/**
 * Abstract Search test class that contains useful methods
 * such as:
 *  <ul>
 *      <li>Preparing the data to index.
 *      <li>Preparing search requests.
 *
 * @author Michael Suzuki
 *
 */
public class AbstractSearchTest extends RestTest
{
    
    protected static final String SEARCH_DATA_SAMPLE_FOLDER = "FolderSearch";
    UserModel userModel, adminUserModel;
    SiteModel siteModel;
    UserModel searchedUser;
    NodesBuilder nodesBuilder;
    protected FileModel file, file2, file3, file4;

    protected static String unique_searchString;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser("UserSearch");
                
        siteModel = new SiteModel(RandomData.getRandomName("SiteSearch"));
        siteModel.setVisibility(Visibility.PRIVATE);
        
        siteModel = dataSite.usingUser(userModel).createSite(siteModel);
        
        unique_searchString = siteModel.getTitle();

        /*
         * Create the following file structure for preconditions : 
         *   |- folder
         *        |-- pangram.txt
         *        |-- cars.txt
         *        |-- alfresco.txt
         *        |-- <uniqueFileName>
         */
        nodesBuilder = restClient.authenticateUser(userModel).withCoreAPI().usingNode(ContentModel.my()).defineNodes();
        FolderModel folder = new FolderModel(SEARCH_DATA_SAMPLE_FOLDER);
        dataContent.usingUser(userModel).usingSite(siteModel).createFolder(folder);
        
        //Create files
        String title = "Title: " + unique_searchString;
        String description = "Description: File is created for search tests by Author: " + unique_searchString + " . ";
        
        file = new FileModel("pangram.txt", "pangram" + title, description, FileType.TEXT_PLAIN, description + " The quick brown fox jumps over the lazy dog");
        
        file2 = new FileModel("cars.txt", "cars" + title, description, FileType.TEXT_PLAIN, "The landrover discovery is not a sports car ");
        
        file3 = new FileModel("alfresco.txt", "alfresco", "alfresco", FileType.TEXT_PLAIN, "Alfresco text file for search ");
        
        file4 = new FileModel(unique_searchString, "unique" + title, description, FileType.TEXT_PLAIN, "Unique text file for search ");
        
        ContentModel cm = new ContentModel();
        cm.setCmisLocation(folder.getCmisLocation());
        cm.setName(folder.getName());
        
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(folder).createContent(file);
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(folder).createContent(file2);
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(folder).createContent(file3);
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(folder).createContent(file4);
        
        waitForIndexing(unique_searchString, true);
    }
    
    /**
     * Helper method which create an http post request to Search API end point.
     * @param term String search term
     * @return {@link SearchResponse} response.
     * @throws Exception if error
     * 
     */
    protected SearchResponse query(String term) throws Exception
    {
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setLanguage("afts");
        queryReq.setQuery(term);
        SearchRequest query = new SearchRequest(queryReq);
        return restClient.authenticateUser(dataUser.getAdminUser()).withSearchAPI().search(query);
    }
    /**
     * Helper method which create an http post request to Search API end point.
     * @param term String search term
     * @return {@link SearchResponse} response.
     * @throws Exception if error
     * 
     */
    protected SearchResponse query(RestRequestQueryModel queryReq,RestRequestHighlightModel highlight) throws Exception
    {
        SearchRequest query = new SearchRequest(queryReq);
        query.setHighlight(highlight);
        return restClient.authenticateUser(dataUser.getAdminUser()).withSearchAPI().search(query);
    }
    /**
     * Helper method which create an http post request to Search API end point.
     * @param term String search term
     * @return {@link SearchResponse} response.
     * @throws Exception if error
     * 
     */
    protected SearchResponse query(SearchRequest query) throws Exception
    {
        return restClient.authenticateUser(userModel).withSearchAPI().search(query);        
    }
    protected SearchRequest createQuery(String term)
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery(term);
        query.setQuery(queryReq);
        return query;
    }
    protected SearchRequest carsQuery()
    {
        return createQuery("cars");
    }
    
    /**
     * Wait for Solr to finish indexing: Indexing has caught up = true if search returns appropriate results
     * @param userQuery: string to search for, unique search string will guarantee accurate results
     * @param expectedInResults, true if entry is expected in the results set
     * @return true (indexing is finished) if search returns appropriate results
     * @throws Exception
     */
    public boolean waitForIndexing(String userQuery, Boolean expectedInResults) throws Exception
    {
        Boolean found = false;
        Boolean resultAsExpected = false;

        SearchRequest searchRequest = createQuery(userQuery);
        SearchResponse response = query(searchRequest);

        // Repeat search until the element is found or Timeout is hit
        for (int searchCount = 1; searchCount <= 3; searchCount++)
        {
            if (searchCount > 1)
            {
                // Wait for the solr indexing.
                Utility.waitToLoopTime(properties.getSolrWaitTimeInSeconds(), "Wait For Indexing");
            }

            if (response.getEntries().size() >= 1)
            {
                found = true;
            }
            else
            {
                found = false;
            }

            // Loop again if result is not as expected: To cater for solr lag: eventual consistency
            resultAsExpected = (expectedInResults.equals(found));
            if (resultAsExpected)
            {
                break;
            }
        }

        return resultAsExpected;
    }
}
