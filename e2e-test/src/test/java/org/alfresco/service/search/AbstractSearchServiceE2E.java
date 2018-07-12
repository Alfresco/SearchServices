package org.alfresco.service.search;

/*
 * Copyright 2018 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

import static lombok.AccessLevel.PROTECTED;


import javax.naming.AuthenticationException;

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
import org.alfresco.utility.network.ServerHealth;
import org.slf4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeSuite;

import lombok.Getter;

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
            dataContent.usingAdmin().deployContentModel("model/music-model.xml");
            dataContent.usingAdmin().deployContentModel("model/finance-model.xml");
        }
        catch (Exception e)
        {
            LOG.warn("Error Loading Custom Model", e);
        }
    }

    /**
     * Wait for Solr to finish indexing: Indexing has caught up = true if search returns appropriate results
     * 
     * @param userQuery: string to search for, unique search string will guarantee accurate results
     * @param expectedInResults, true if entry is expected in the results set
     * @return true (indexing is finished) if search returns appropriate results
     * @throws Exception
     */
    public boolean waitForIndexing(String userQuery, Boolean expectedInResults) throws Exception
    {
        Boolean found = false;
        Boolean resultAsExpected = false;
        String expectedStatusCode = HttpStatus.OK.toString();
        
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery(userQuery);
        query.setQuery(queryReq);

        // Using adminUser just to confirm that the content is indexed
        SearchResponse response = restClient.authenticateUser(dataUser.getAdminUser()).withSearchAPI().search(query);

        // Repeat search until the query results are as expected or Search Retry count is hit
        for (int searchCount = 1; searchCount <= 3; searchCount++)
        {
            if (searchCount > 1)
            {
                // Wait for the solr indexing.
                Utility.waitToLoopTime(properties.getSolrWaitTimeInSeconds(), "Wait For Indexing");
            }

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
                resultAsExpected = (expectedInResults.equals(found));
                if (resultAsExpected)
                {
                    break;
                }
            }
            else
            {
                throw new AuthenticationException("API returned status code:" + restClient.getStatusCode() + " Expected: " + expectedStatusCode);
            }
        }

        return resultAsExpected;
    }
}
