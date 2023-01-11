/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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

package org.alfresco.test.search.functional;

import static java.util.Optional.ofNullable;

import static lombok.AccessLevel.PROTECTED;
import static org.testng.Assert.assertEquals;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import lombok.Getter;
import org.alfresco.cmis.CmisWrapper;
import org.alfresco.dataprep.ContentService;
import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.core.RestProperties;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.exception.EmptyJsonResponseException;
import org.alfresco.rest.exception.EmptyRestModelCollectionException;
import org.alfresco.rest.model.RestRequestSpellcheckModel;
import org.alfresco.rest.search.Pagination;
import org.alfresco.rest.search.RestRequestHighlightModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.RestShardInfoModel;
import org.alfresco.rest.search.RestShardInfoModelCollection;
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
import org.hamcrest.Matchers;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeSuite;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 * @author meenal bhave
 */
@ContextConfiguration ("classpath:alfresco-search-e2e-context.xml")
public abstract class AbstractE2EFunctionalTest extends AbstractTestNGSpringContextTests
{
    /** The number of retries that a query will be tried before giving up. */
    protected static final int SEARCH_MAX_ATTEMPTS = 120;

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
    //  @Getter(value = PROTECTED)
    protected DataUser dataUser;

    @Autowired
    @Getter (value = PROTECTED)
    private ContentService contentService;

    protected UserModel testUser, adminUserModel, testUser2;
    protected SiteModel testSite, testSite2;

    protected static String unique_searchString;

    protected static String shardingMethod = "DB_ID";
    protected int shardCount = 0;

    protected static final String SEARCH_LANGUAGE_CMIS = "cmis";

    protected enum SearchLanguage {
        CMIS,
        AFTS
    }

    protected enum ShardingMethod {
        DB_ID,
        DB_ID_RANGE,
        MOD_ACL_ID,
        ACL_ID,
        DATE,
        PROPERTY,
        EXPLICIT_ID
    }

    @BeforeSuite (alwaysRun = true)
    public void beforeSuite() throws Exception
    {
        super.springTestContextPrepareTestInstance();

        deployCustomModel("model/music-model.xml");
        deployCustomModel("model/finance-model.xml");
        deployCustomModel("model/sharding-content-model.xml");
    }

    @BeforeClass (alwaysRun = true)
    public void setup()
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
     * @param userQuery         Search Query
     * @param contentToFind     that's expected to be included / excluded from the results
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
            try
            {
                SearchResponse response = query(searchRequest);

                if (restClient.getStatusCode().matches(expectedStatusCode))
                {
                    boolean found = isContentInSearchResponse(response, contentName);

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
                    throw new RuntimeException("API returned status code:" + restClient.getStatusCode() + " Expected: " + expectedStatusCode + "; Response body: " + response);
                }
            }
            catch (EmptyJsonResponseException e)
            {
                // Ignore the empty JSON response and try again
            }
        }

        return false;
    }

    /**
     * Method to check if the contentName is returned in the SearchResponse.
     *
     * @param response    the search response
     * @param contentName the text we are using as matching/verifying criteria.
     * @return true if if the item with the contentName text is returned in the SearchResponse.
     */
    public boolean isContentInSearchResponse(SearchResponse response, String contentName)
    {
        return response.getEntries().stream()
                       .map(entry -> entry.getModel().getName())
                       .anyMatch(name -> name.equalsIgnoreCase(contentName) || contentName.isBlank());
    }

    /**
     * Wait for Solr to finish indexing: Indexing has caught up = true if search returns appropriate results
     *
     * @param userQuery:         search query, this can include the fieldname, unique search string will guarantee accurate results
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
     * @param fieldName:         specific field to search for, e.g. name. When specified, the query will become: name:'userQuery'
     * @param userQuery:         search string, unique search string will guarantee accurate results
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
     * @param user:        UserModel for the user you wish to run the query as
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
     * Run a search with Spellcheck as given user and return the response
     * @param user            UserModel for the user you wish to run the query as
     * @param queryModel      The queryModel to search for, containing the query
     * @param spellcheckQuery The Spellcheck Model containing the query
     * @return the search response from the API
     */
    protected SearchResponse queryAsUser(UserModel user, RestRequestQueryModel queryModel, RestRequestSpellcheckModel spellcheckQuery)
    {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(queryModel);
        searchRequest.setSpellcheck(spellcheckQuery);

        return restClient.authenticateUser(user).withSearchAPI().search(searchRequest);
    }

    protected SearchResponse queryAsUser(UserModel user, RestRequestQueryModel queryModel, Pagination paging)
    {
        SearchRequest searchRequest = new SearchRequest();
        searchRequest.setQuery(queryModel);

        if (ofNullable(paging).isPresent())
        {
            searchRequest.setPaging(paging);
        }

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

    protected DataUser getDataUser()
    {
        return dataUser;
    }

    /**
     * Helper method to test if the search query works and count matches where provided
     *
     * @param query:         AFTS or cmis query string
     * @param expectedCount: Only successful response is checked, when expectedCount is null (can not be exactly specified),
     * @return SearchResponse
     */
    protected SearchResponse testSearchQuery(String query, Integer expectedCount, SearchLanguage queryLanguage)
    {
        SearchResponse response = performSearch(testUser, query, queryLanguage, getDefaultPagingOptions());

        if (ofNullable(expectedCount).isPresent())
        {
            restClient.onResponse().assertThat().body("list.pagination.count", Matchers.equalTo(expectedCount));
        }

        return response;
    }

    /**
     * Helper method to test if the search query returns the expected results in the order given.
     *
     * @param query:         AFTS or cmis query string
     * @param expectedNames: The ordered list of names expected to be returned,
     * @return SearchResponse
     */
    protected SearchResponse testSearchQueryOrdered(String query, List<String> expectedNames, SearchLanguage queryLanguage)
    {
        SearchResponse response = performSearch(testUser, query, queryLanguage, getDefaultPagingOptions());

        List<String> names = response.getEntries().stream().map(s -> s.getModel().getName()).collect(Collectors.toList());

        // Include lists in failure message as TestNG won't do this for lists.
        assertEquals(names, expectedNames, "Unexpected results for query: " + query + " Expected: " + expectedNames + " but got " + names);

        return response;
    }

    /**
     * Helper method to test if the search query returns the expected set of results.
     *
     * @param query:         AFTS or cmis query string
     * @param expectedNames: The ordered list of names expected to be returned,
     * @return SearchResponse
     */
    protected SearchResponse testSearchQueryUnordered(String query, Set<String> expectedNames, SearchLanguage queryLanguage)
    {
        SearchResponse response = performSearch(testUser, query, queryLanguage, getDefaultPagingOptions());

        Set<String> names = response.getEntries().stream().map(s -> s.getModel().getName()).collect(Collectors.toSet());

        assertEquals(names, expectedNames, "Unexpected results for query: " + query);

        return response;
    }

    protected SearchResponse performSearch(UserModel asUser, String query, SearchLanguage queryLanguage, Pagination paging)
    {
        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setQuery(query);

        if (!ofNullable(asUser).isPresent())
        {
            asUser = testUser;
        }

        if (ofNullable(queryLanguage).isPresent())
        {
            queryModel.setLanguage(queryLanguage.toString());
        }

        SearchResponse response = queryAsUser(asUser, queryModel, paging);

        return response;
    }

    /**
     * Returns pagination object with alfresco default settings
     * Sets skipCount = 0, maxItems = 100
     *
     * @return
     */
    private Pagination getDefaultPagingOptions()
    {
        Pagination paging = new Pagination();
        paging.setSkipCount(0);
        paging.setMaxItems(100);

        return paging;
    }

    /**
     * Set the pagination options for the API query
     * @param skipCount Integer
     * @param maxItems Integer
     * @return
     */
    protected Pagination setPaging(Integer skipCount, Integer maxItems)
    {
        Pagination paging = new Pagination();

        if (ofNullable(skipCount).isPresent())
        {
            paging.setSkipCount(skipCount);
        }

        if (ofNullable(maxItems).isPresent())
        {
            paging.setMaxItems(maxItems);
        }

        return paging;
    }

    /**
     * Method to create and run a simple spellcheck query
     * When a spellcheck query is run a user, the query inputed and the user query is inputted
     * @param query
     * @param userQuery
     * @return
     */
    protected SearchResponse SearchSpellcheckQuery(UserModel user, String query, String userQuery)
    {
        RestRequestSpellcheckModel spellCheck = new RestRequestSpellcheckModel();
        spellCheck.setQuery(userQuery);

        UserModel searchUser = ofNullable(user).isPresent() ? user : testUser;
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery(query);
        queryReq.setUserQuery(userQuery);
        searchReq.setQuery(queryReq);
        searchReq.setSpellcheck(spellCheck);
        SearchResponse response = queryAsUser(searchUser, queryReq, spellCheck);
        return response;
    }

    /**
     * Method to check the spellcheck object returned in the Search Response
     * @param response             SearchResponse
     * @param spellCheckType       String Values: searchInsteadFor, didYouMean or null
     * @param spellCheckSuggestion String Values: suggestion string or null
     */
    public void testSearchSpellcheckResponse(SearchResponse response, String spellCheckType, String spellCheckSuggestion)
    {
        if (ofNullable(spellCheckType).isPresent())
        {
            response.getContext().assertThat().field("spellCheck").isNotEmpty();
            response.getContext().getSpellCheck().assertThat().field("type").is(spellCheckType);
        }
        else
        {
            response.getContext().assertThat().field("spellCheck").isNull();
        }

        if (ofNullable(spellCheckSuggestion).isPresent())
        {
            response.getContext().assertThat().field("spellCheck").isNotEmpty();
            response.getContext().getSpellCheck().assertThat().field("suggestions").contains(spellCheckSuggestion);
        }
    }

    /**
     * Method returns the sharding method used for the core by the 1st shard instance registered with ACS
     * @return String sharding Method
     * @throws JsonProcessingException
     * @throws EmptyRestModelCollectionException
     */
    public String getShardMethod() throws JsonProcessingException, EmptyRestModelCollectionException
    {
        RestShardInfoModelCollection info = getShardInfo();

        return shardingMethod = ofNullable(info)
                .map(RestShardInfoModelCollection::getEntries)
                .map(Collection::iterator).filter(Iterator::hasNext)
                .map(Iterator::next)
                .map(RestShardInfoModel::getModel)
                .map(RestShardInfoModel::getShardMethod)
                .orElseThrow(() -> new RuntimeException("Cannot retrieve the shard method in use."));
    }

    /**
     * Method returns the shardCount for the 1st core (of the shard instance) that registers with ACS
     * @return shard Count
     * @throws JsonProcessingException
     * @throws EmptyRestModelCollectionException
     */
    public int getShardCount() throws JsonProcessingException, EmptyRestModelCollectionException
    {
        RestShardInfoModelCollection info = getShardInfo();

        return shardCount = ofNullable(info)
                .map(RestShardInfoModelCollection::getEntries)
                .map(Collection::iterator)
                .filter(Iterator::hasNext)
                .map(Iterator::next)
                .map(RestShardInfoModel::getModel)
                .map(RestShardInfoModel::getNumberOfShards)
                .orElseThrow( () -> new RuntimeException("Cannot retrieve the number of shards registered."));
    }

    public RestShardInfoModelCollection getShardInfo() throws JsonProcessingException, EmptyRestModelCollectionException
    {
        RestShardInfoModelCollection info = restClient.authenticateUser(dataUser.getAdminUser()).withShardInfoAPI().getInfo();

        return info;
    }
}
