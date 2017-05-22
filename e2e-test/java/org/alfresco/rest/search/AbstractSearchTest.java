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
import org.alfresco.rest.model.builder.NodesBuilder;
import org.alfresco.utility.Utility;
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
    protected static final String SEARCH_DATA_SAMPLE_FOLDER = "folder";
    UserModel userModel, adminUserModel;
    SiteModel siteModel;
    UserModel searchedUser;
    NodesBuilder nodesBuilder;
    protected FileModel file,file2;
    
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
        dataContent.usingUser(userModel).usingSite(siteModel).createFolder(folder);
        //Create files
        file = new FileModel("pangram.txt", FileType.TEXT_PLAIN, "The quick brown fox jumps over the lazy dog");
        file2 = new FileModel("cars.txt", FileType.TEXT_PLAIN, "The landrover discovery is not a sports car");
        ContentModel cm = new ContentModel();
        cm.setCmisLocation(folder.getCmisLocation());
        cm.setName(folder.getName());
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(cm).createContent(file);
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(cm).createContent(file2);
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
        return restClient.authenticateUser(dataUser.getAdminUser()).withSearchAPI().search(query);
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
}
