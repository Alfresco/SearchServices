/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.test.search.functional;

import org.alfresco.cmis.CmisWrapper;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.core.RestProperties;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestHighlightModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.TasProperties;
import org.alfresco.utility.Utility;
import org.alfresco.utility.constants.UserRole;
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

/**
 * @author meenal bhave
 */
@ContextConfiguration("classpath:alfresco-search-e2e-context.xml")
public abstract class AbstractE2EFunctionalTest extends AbstractTestNGSpringContextTests
{
    /** The number of retries that a query will be tried before giving up. */
    private static final int SEARCH_MAX_ATTEMPTS = 6;

    private static final Logger LOGGER = LogFactory.getLogger();

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

    protected static String unique_searchString;

    @BeforeSuite(alwaysRun = true)
    public void beforeSuite() throws Exception
    {
        super.springTestContextPrepareTestInstance();

        deployCustomModel("model/music-model.xml");
        deployCustomModel("model/finance-model.xml");
    }

    @BeforeClass(alwaysRun = true)
    public void setup()
    {
        serverHealth.assertServerIsOnline();

        adminUserModel = dataUser.getAdminUser();
        testUser = dataUser.createRandomTestUser("UserSearch");

        testSite = new SiteModel(RandomData.getRandomName("SiteSearch"));
        testSite.setVisibility(Visibility.PRIVATE);

        testSite = dataSite.usingUser(testUser).createSite(testSite);

        unique_searchString = testSite.getTitle().replace("SiteSearch", "Unique");

        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);
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
                LOGGER.warn("Error Loading Custom Model", e);
            }
        }
        return modelDeployed;
    }

    public boolean deactivateCustomModel(String fileName)
    {
        try
        {
            FileModel customModel = getCustomModel(fileName);

            // Deactivate the model if found
            if (customModel != null)
            {
                cmisApi.authenticateUser(dataUser.getAdminUser()).usingResource(customModel).updateProperty("cm:modelActive", false);

                return true;
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("Error Deactivating Custom Model", e);
        }
        return false;
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
                LOGGER.error("Custom Content Model [{}] is not available under [/Data Dictionary/Models/] location", fileName);
            }
        }
        catch (Exception e)
        {
            LOGGER.error("Custom Content Model [{}] is not available under [/Data Dictionary/Models/] location", fileName);
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
                LOGGER.info("Custom Model file: " + customModel.getCmisLocation());
                }
                else
                {
                    LOGGER.info("Custom Content Model [{}] is not available under [/Data Dictionary/Models/] location", fileName);
                }
            }
        }
        catch (Exception e)
        {
            LOGGER.warn("Error Getting Custom Model: " + fileName, e);
        }

        return customModel;
    }

    /**
     * Helper method which create an http post request to Search API end point.
     * Executes the given search request without throwing checked exceptions (a {@link RuntimeException} will be thrown in case).
     *
     * @param query the search request.
     * @return the query execution response.
     */
    protected SearchResponse query(SearchRequest query)
    {
        return restClient.authenticateUser(testUser).withSearchAPI().search(query);
    }

    /**
     * Wait for Solr to finish indexing and search to return appropriate results
     * 
     * @param userQuery Search Query
     * @param contentToFind that's expected to be included / excluded from the results
     * @param expectedInResults Whether we expect the content in the results or not.
     * @return true if search returns expected results, i.e. is given content is found or excluded from the results
     */
    public boolean isContentInSearchResults(String userQuery, String contentToFind, boolean expectedInResults) {

        String expectedStatusCode = HttpStatus.OK.toString();
        String contentName = (contentToFind == null) ? "" : contentToFind;

        SearchRequest searchRequest = createQuery(userQuery);

        // Repeat search until the query results are as expected or Search Retry count is hit
        for (int searchCount = 0; searchCount < SEARCH_MAX_ATTEMPTS; searchCount++)
        {
            SearchResponse response = query(searchRequest);

            if (restClient.getStatusCode().matches(expectedStatusCode))
            {
                boolean found = response.getEntries().stream()
                        .map(entry -> entry.getModel().getName())
                        .filter(name -> name.equalsIgnoreCase(contentName) || contentName.isBlank())
                        .count() > 0;

                // Exit loop if result is as expected.
                if (expectedInResults == found)
                {
                    return true;
                }
                // Wait for the solr indexing (eventual consistency).
                Utility.waitToLoopTime(properties.getSolrWaitTimeInSeconds(), "Wait For Indexing. Retry Attempt: " + (searchCount + 1));
            }
            else
            {
                throw new RuntimeException("API returned status code:" + restClient.getStatusCode() + " Expected: " + expectedStatusCode);
            }
        }

        return false;
    }

    /**
     * Wait for Solr to finish indexing: Indexing has caught up = true if search returns appropriate results
     * 
     * @param userQuery: search query, this can include the fieldname, unique search string will guarantee accurate results
     * @param expectedInResults, true if entry is expected in the results set
     * @return true (indexing is finished) if search returns appropriate results
     */
    public boolean waitForIndexing(String userQuery, boolean expectedInResults)
    {
        // Use the search query as is: fieldname(s) may or may not be specified within the userQuery
        return waitForIndexing(null, userQuery, expectedInResults);
    }

    /**
     * waitForIndexing method that matches / waits for filename, metadata to be indexed.
     * 
     * @param userQuery
     * @param expectedInResults
     * @return
     */
    public boolean waitForMetadataIndexing(String userQuery, boolean expectedInResults)
    {
        return waitForIndexing("name", userQuery, expectedInResults);
    }

    /**
     * waitForIndexing method that matches / waits for content to be indexed, this can take longer than metadata indexing.
     * Since Metadata is indexed first, use this method where tests, queries need content to be indexed too.
     * 
     * @param userQuery
     * @param expectedInResults
     * @return
     */
    public boolean waitForContentIndexing(String userQuery, boolean expectedInResults)
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
     */
    private boolean waitForIndexing(String fieldName, String userQuery, boolean expectedInResults)
    {
        String query = (fieldName == null) ? userQuery : String.format("%s:'%s'", fieldName, userQuery);

        return isContentInSearchResults(query, null, expectedInResults);
    }

    /**
     * Run a search as admin user and return the response
     * 
     * @param queryString: string to search for, unique search string will guarantee accurate results
     * @return the search response from the API
     */
    public SearchResponse query(String queryString)
    {
        return queryAsUser(dataUser.getAdminUser(), queryString);
    }

    /**
     * Run a search as given user and return the response
     * 
     * @param user: UserModel for the user you wish to run the query as
     * @param queryString: string to search for, unique search string will guarantee accurate results
     * @return the search response from the API
     */
    protected SearchResponse queryAsUser(UserModel user, String queryString)
    {
        SearchRequest searchRequest = new SearchRequest();
        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setQuery(queryString);
        searchRequest.setQuery(queryModel);
        return restClient.authenticateUser(user).withSearchAPI().search(searchRequest);
    }
    
    /**
     * Run a search as given user and return the response
     * 
     * @param user: UserModel for the user you wish to run the query as
     * @param queryModel: The queryModel to search for, containing the query
     * @return the search response from the API
     */
    protected SearchResponse queryAsUser(UserModel user, RestRequestQueryModel queryModel)
    {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(queryModel);

        return restClient.authenticateUser(user).withSearchAPI().search(searchRequest);
    }

    /**
     * Helper method which create an http post request to Search API end point.
     *
     * @return {@link SearchResponse} response.
     */
    protected SearchResponse query(RestRequestQueryModel queryReq, RestRequestHighlightModel highlight)
    {
        SearchRequest query = new SearchRequest(queryReq);
        query.setHighlight(highlight);
        return restClient.authenticateUser(testUser).withSearchAPI().search(query);
    }

    protected SearchRequest createQuery(String term)
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery(term);
        query.setQuery(queryReq);
        return query;
    }
}
