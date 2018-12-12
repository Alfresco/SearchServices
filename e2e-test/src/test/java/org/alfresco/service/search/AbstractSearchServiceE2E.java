package org.alfresco.service.search;

/*
 * Copyright 2018 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import org.alfresco.cmis.CmisWrapper;
import org.alfresco.dataprep.ContentService;
import org.alfresco.rest.core.RestProperties;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.LogFactory;
import org.alfresco.utility.TasProperties;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.network.ServerHealth;
import org.apache.chemistry.opencmis.client.api.CmisObject;
import org.apache.chemistry.opencmis.client.api.Session;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeSuite;

import lombok.Getter;
import static lombok.AccessLevel.PROTECTED;

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

    /**
     * Wait for Solr to finish indexing: Indexing has caught up = true if search returns appropriate results
     * 
     * @param userQuery: string to search for, unique search string will guarantee accurate results
     * @param expectedInResults, true if entry is expected in the results set
     * @return true (indexing is finished) if search returns appropriate results
     * @throws Exception
     */
    public boolean waitForIndexing(String userQuery, boolean expectedInResults) throws Exception
    {
        boolean found = false;
        boolean resultAsExpected = false;
        String expectedStatusCode = HttpStatus.OK.toString();
        Integer retryCount = 3;
        
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery(userQuery);
        query.setQuery(queryReq);

        // Repeat search until the query results are as expected or Search Retry count is hit
        for (int searchCount = 1; searchCount <= retryCount; searchCount++)
        {
            // Using adminUser just to confirm that the content is indexed
            SearchResponse response = restClient.authenticateUser(dataUser.getAdminUser()).withSearchAPI().search(query);

            if (restClient.getStatusCode().matches(expectedStatusCode))
            {
                if (response.getEntries().size() >= 1)
                {
                    found = true;
                }
                else
                {
                    found = false;
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
     * Run a search and return the response
     * @param queryString: string to search for, unique search string will guarantee accurate results
     * @return the search response from the API
     * @throws Exception
     */
    public SearchResponse query(String queryString) throws Exception
    {
        SearchRequest searchRequest = new SearchRequest();
        RestRequestQueryModel queryModel = new RestRequestQueryModel();
        queryModel.setQuery(queryString);
        searchRequest.setQuery(queryModel);
        return restClient.authenticateUser(dataUser.getAdminUser()).withSearchAPI().search(searchRequest);
    }
}
