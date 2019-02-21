package org.alfresco.service.search;

/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import org.alfresco.cmis.CmisWrapper;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.core.RestProperties;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.TasProperties;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.network.ServerHealth;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import lombok.Getter;
import static lombok.AccessLevel.PROTECTED;

import java.util.List;

/**
 * @author meenal bhave
 */

@ContextConfiguration("classpath:alfresco-search-e2e-context.xml")
public abstract class AbstractSearchServiceE2E extends AbstractTestNGSpringContextTests
{
    private static Logger LOG = LogFactory.getLogger();

    @Autowired
    protected RestProperties restProperties;

    @Autowired
    protected TasProperties properties;

    @Autowired
    protected ServerHealth serverHealth;

    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    @Autowired
    protected RestWrapper restClient;

    @Autowired
    protected CmisWrapper cmisApi;

    @Autowired
    @Getter(value = PROTECTED)
    protected DataUser dataUser;

    @Autowired
    @Getter(value = PROTECTED)
    private ContentService contentService;
    
    protected UserModel testUser, adminUserModel;
    protected SiteModel testSite;
    protected UserModel searchedUser;
    
    protected static String unique_searchString;

    public static final String NODE_PREFIX = "workspace/SpacesStore/";

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() throws Exception
    {
        super.springTestContextPrepareTestInstance();

        try
        {
            deployCustomModel("model/music-model.xml");
            deployCustomModel("model/finance-model.xml");
        }
        catch (Exception e)
        {
            LOG.warn("Error Loading Custom Model", e);
        }
    }
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();

        adminUserModel = dataUser.getAdminUser();
        testUser = dataUser.createRandomTestUser("UserSearch");
                
        testSite = new SiteModel(RandomData.getRandomName("SiteSearch"));
        testSite.setVisibility(Visibility.PRIVATE);
        
        testSite = dataSite.usingUser(testUser).createSite(testSite);
        
        unique_searchString = testSite.getTitle().replace("SiteSearch", "Unique");
    }

    public boolean deployCustomModel(String path)
    {
        Boolean modelDeployed = false;

        if ((path != null) && (path.endsWith("-model.xml")))
        {
            try
            {
                dataContent.usingAdmin().deployContentModel(path);
                modelDeployed = true;
            }
            catch (Exception e)
            {
                LOG.warn("Error Loading Custom Model", e);
            }
        }
        return modelDeployed;
    }
    
    public boolean deactivateCustomModel(String fileName)
    {
        Boolean modelDeactivated = false;

        try
        {
            FileModel customModel = getCustomModel(fileName);

            // Deactivate the model if found
            if (customModel != null)
            {

                cmisApi.authenticateUser(dataUser.getAdminUser()).usingResource(customModel).updateProperty("cm:modelActive", false);

                modelDeactivated = true;
            }
        }
        catch (Exception e)
        {
            LOG.warn("Error Deactivating Custom Model", e);
        }
        return modelDeactivated;
    }

    public boolean deleteCustomModel(String fileName)
    {
        Boolean modelDeleted = false;

        try
        {
            FileModel customModel = getCustomModel(fileName);

            // Delete the model if found
            if (customModel != null)
            {
                // cmisApi.authenticateUser(dataUser.getAdminUser()).usingResource(customModel).deleteContent();
                dataContent.usingAdmin().usingResource(customModel).deleteContent();
                restClient.authenticateUser(dataContent.getAdminUser()).withCoreAPI().usingTrashcan().deleteNodeFromTrashcan(customModel);

                modelDeleted = true;
            }
            else
            {
                LOG.error("Custom Content Model [{}] is not available under [/Data Dictionary/Models/] location", fileName);
            }
        }
        catch (Exception e)
        {
            LOG.error("Custom Content Model [{}] is not available under [/Data Dictionary/Models/] location", fileName);
        }

        return modelDeleted;
    }

    private FileModel getCustomModel(String fileName)
    {
        FileModel customModel = null;

        try
        {
            if ((fileName != null) && (fileName.endsWith("-model.xml")))

            {
                Session session = contentService.getCMISSession(dataUser.getAdminUser().getUsername(), dataUser.getAdminUser().getPassword());

                CmisObject modelInRepo = session.getObjectByPath(String.format("/Data Dictionary/Models/%s", fileName));

                if (modelInRepo != null)
                {
                    customModel = new FileModel(modelInRepo.getName());
                    customModel.setNodeRef(modelInRepo.getId());
                    customModel.setNodeRef(customModel.getNodeRefWithoutVersion());
                    customModel.setCmisLocation(String.format("/Data Dictionary/Models/%s", fileName));
                    LOG.info("Custom Model file: " + customModel.getCmisLocation());
                }
                else
                {
                    LOG.info("Custom Content Model [{}] is not available under [/Data Dictionary/Models/] location", fileName);
                }
            }
        }
        catch (Exception e)
        {
            LOG.warn("Error Getting Custom Model: " + fileName, e);
        }

        return customModel;
    }

    protected SearchRequest createQuery(String term)
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery(term);
        query.setQuery(queryReq);
        return query;
    }

    /**
     * 
     * Helper method which create an http post request to Search API end point.
     * Executes the given search request without throwing checked exceptions (a {@link RuntimeException} will be thrown in case).
     * @param query the search request.
     * @return {@link SearchResponse} response.
     * 
     */
    protected SearchResponse query(SearchRequest query) throws Exception
    {
        try
        {
            return restClient.authenticateUser(testUser).withSearchAPI().search(query);
        }
        catch (final Exception exception)
        {
            throw new RuntimeException(exception);
        }
    }

    /**
     * Wait for Solr to finish indexing and search to return appropriate results
     * 
     * @param userQuery: Search Query
     * @param contentName that's expected to be included / excluded from the results
     * @param expectedInResults
     * @return true if search returns expected results, i.e. is given content is found or excluded from the results
     * @throws Exception
     */
    public boolean waitForContent(String userQuery, String contentName, boolean expectedInResults) throws Exception
    {
        boolean resultAsExpected = false;
        boolean found = !expectedInResults;
        String expectedStatusCode = HttpStatus.OK.toString();

        // Repeat search until the query results are as expected or Search Retry count is hit
        for (int searchCount = 1; searchCount <= 6; searchCount++)
        {
            SearchRequest searchRequest = createQuery(userQuery);
            SearchResponse response = query(searchRequest);

            if (restClient.getStatusCode().matches(expectedStatusCode))
            {
                List<SearchNodeModel> entries = response.getEntries();
                if (!entries.isEmpty())
                {
                    for (SearchNodeModel entry : entries)
                    {
                        found = (contentName.equalsIgnoreCase(entry.getModel().getName()));
                    }
                }
                // Loop again if result is not as expected: To cater for solr lag: eventual consistency
                resultAsExpected = (expectedInResults == found);
                if (resultAsExpected)
                {
                    break;
                }
                else
                {
                    // Wait for the solr indexing.
                    Utility.waitToLoopTime(properties.getSolrWaitTimeInSeconds(), "Wait For Indexing");
                }
            }
            else
            {
                throw new RuntimeException("API returned status code:" + restClient.getStatusCode() + " Expected: " + expectedStatusCode);
            }
        }

        return resultAsExpected;
    }

    /**
     * Wait for Solr to finish indexing: Indexing has caught up = true if search returns appropriate results
     * 
     * @param userQuery: search query, this can include the fieldname, unique search string will guarantee accurate results
     * @param expectedInResults, true if entry is expected in the results set
     * @return true (indexing is finished) if search returns appropriate results
     * @throws Exception
     */
    public boolean waitForIndexing(String userQuery, boolean expectedInResults) throws Exception
    {
        // Use the search query as is: fieldname(s) may or may not be specified within the userQuery
        return waitForIndexing(null, userQuery, expectedInResults);
    }
    
    /**
     * waitForIndexing method that matches / waits for filename, metadata to be indexed.
     * @param userQuery
     * @param expectedInResults
     * @return
     * @throws Exception
     */
    public boolean waitForMetadataIndexing(String userQuery, boolean expectedInResults) throws Exception
    {
        return waitForIndexing("name", userQuery, expectedInResults);
    }
    
    /**
     * waitForIndexing method that matches / waits for content to be indexed, this can take longer than metadata indexing.
     * Since Metadata is indexed first, use this method where tests, queries need content to be indexed too.
     * @param userQuery
     * @param expectedInResults
     * @return
     * @throws Exception
     */
    public boolean waitForContentIndexing(String userQuery, boolean expectedInResults) throws Exception
    {
        return waitForIndexing("cm:content", userQuery, expectedInResults);
    }

    /**
     * Wait for Solr to finish indexing: Indexing has caught up = true if search returns appropriate results
     * 
     * @param fieldName: specific field to search for, e.g. name. When specified, the query will become: name:'userQuery'
     * @param userQuery: search string, unique search string will guarantee accurate results
     * @param expectedInResults, true if entry is expected in the results set
     * @return true (indexing is finished) if search returns appropriate results
     * @throws Exception
     */
    private boolean waitForIndexing(String fieldName, String userQuery, boolean expectedInResults) throws Exception
    {
        boolean resultAsExpected = false;
        String expectedStatusCode = HttpStatus.OK.toString();
        String query = (fieldName == null)? userQuery: String.format("%s:'%s'", fieldName, userQuery); 

        // Repeat search until the query results are as expected or Search Retry count is hit
        for (int searchCount = 1; searchCount <= 3; searchCount++)
        {
            SearchRequest searchRequest = createQuery(query);
            SearchResponse response = query(searchRequest);

            if (restClient.getStatusCode().matches(expectedStatusCode))
            {
                boolean found = !response.getEntries().isEmpty();

                // Loop again if result is not as expected: To cater for solr lag: eventual consistency
                resultAsExpected = (expectedInResults == found);
                if (resultAsExpected)
                {
                    break;
                }
                else
                {
                 // Wait for the solr indexing.
                    Utility.waitToLoopTime(properties.getSolrWaitTimeInSeconds(), "Wait For Indexing");
                }
            }
            else
            {
                throw new RuntimeException("API returned status code:" + restClient.getStatusCode() + " Expected: " + expectedStatusCode);
            }
        }

        return resultAsExpected;
    }

    /**
     * Run a search as admin user and return the response
     * 
     * @param queryString: string to search for, unique search string will guarantee accurate results
     * @return the search response from the API
     * @throws Exception
     */
    public SearchResponse query(String queryString) throws Exception
    {
        return queryAsUser(dataUser.getAdminUser(), queryString);
    }

    /**
     * Run a search as given user and return the response
     * 
     * @param user: UserModel for the user you wish to run the query as
     * @param queryString: string to search for, unique search string will guarantee accurate results
     * @return the search response from the API
     * @throws Exception
     */
    public SearchResponse queryAsUser(UserModel user, String queryString) throws Exception
    {
        SearchRequest searchRequest = new SearchRequest();
        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setQuery(queryString);
        searchRequest.setQuery(queryModel);
        return restClient.authenticateUser(user).withSearchAPI().search(searchRequest);
    }
}
