/*
 * Copyright (C) 2020 Alfresco Software Limited.
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
package org.alfresco.test.search.functional.searchServices.solr.admin;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.search.TestGroup;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.springframework.context.annotation.Configuration;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * End to end tests for SOLR Admin actions REST API, available from:
 * 
 * http://<server>:<port>/solr/admin/cores?action=*
 * 
 * @author aborroy
 *
 */
@Configuration
public class SolrE2eAdminTest extends AbstractE2EFunctionalTest
{
    
    
    // SOLR default response status codes (returned in responseHeader.status)
    private static final String SOLR_RESPONSE_STATUS_OK = "0";
    private static final String SOLR_RESPONSE_STATUS_INTERNAL_ERROR = "400";
    
    // Alfresco SOLR action response status identifiers
    private static final String ACTION_RESPONSE_REPORT = "report";
    
    // Default Alfresco SOLR Core Names
    List<String> defaultCoreNames = new ArrayList<>(List.of("alfresco", "archive"));

    @Test(priority = 1)
    public void testNodeReport() throws Exception
    {
        String nodeid = "200";
        RestResponse response = restClient.withParams("nodeid=" + nodeid).withSolrAdminAPI().getAction("nodeReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT).toString();
        Assert.assertNotNull(report);
        
        defaultCoreNames.forEach(core -> {
            Assert.assertNotNull(response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core));
        });
    }
    
    /**
     * Node Report requires "nodeid" parameter.
     * This test will fail as we are missing to pass the parameter. 
     * @throws Exception
     */
    @Test(priority = 2)
    public void testNodeReportError() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("nodeReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_INTERNAL_ERROR);
    }
    
    @Test(priority = 3)
    public void testAclReport() throws Exception
    {
        String aclid = "1";
        RestResponse response = restClient.withParams("aclid=" + aclid).withSolrAdminAPI().getAction("aclReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);

        String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT).toString();
        Assert.assertNotNull(report);
    }
    
    /**
     * Acl Report requires "aclid" parameter.
     * This test will fail as we are missing to pass the parameter.
     * @throws Exception
     */
    @Test(priority = 4)
    public void testAclReportError() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("aclReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_INTERNAL_ERROR);
    }
    
    @Test(priority = 5)
    public void testTxReport() throws Exception
    {
        String coreName = "alfresco";
        String txid = "1";
        
        RestResponse response = restClient.withParams("coreName=" + coreName, "txid=" + txid).withSolrAdminAPI().getAction("txReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);

        String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT).toString();
        Assert.assertNotNull(report);
    }
    
    /**
     * Transaction report requires "txid" parameter.
     * This test will fail as we are missing to pass the parameter.
     * @throws Exception
     */
    @Test(priority = 6)
    public void testTxReportError() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("txReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_INTERNAL_ERROR);
    }
    
    @Test(priority = 7)
    public void testAclTxReport() throws Exception
    {
        String acltxid = "1";
        
        RestResponse response = restClient.withParams("acltxid=" + acltxid).withSolrAdminAPI().getAction("aclTxReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);

        String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT).toString();
        Assert.assertNotNull(report);
    }
    
    /**
     * AclTx report requires "acltxid" parameter.
     * This test will fail as we are missing to pass the parameter.
     * @throws Exception
     */
    @Test(priority = 8)
    public void testAclTxReportError() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("aclTxReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_INTERNAL_ERROR);
    }
    
    @Test(priority = 9)
    public void testReport() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction(ACTION_RESPONSE_REPORT);
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);

        String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT).toString();
        Assert.assertNotNull(report);
    }
    
    @Test(priority = 10)
    public void testSummary() throws Exception
    {
        String core = "alfresco";

        RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("summary");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String report = response.getResponse().body().jsonPath().get("Summary").toString();
        Assert.assertNotNull(report);
    }
    
    @Test(priority = 11)
    public void testCheck() throws Exception
    {
        String core = "alfresco";
        
        RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("check");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "success");
    }
    
    /**
     * This action only applies to DB_ID_RANGE Sharding method.
     * This test verifies expected result when using another deployment
     * @throws Exception
     */
    @Test(priority = 12)
    public void testRangeCheck() throws Exception
    {
        String coreName = "alfresco";
        
        RestResponse response = restClient.withParams("coreName=" + coreName).withSolrAdminAPI().getAction("rangeCheck");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String expand = response.getResponse().body().jsonPath().get("expand").toString();             
        Assert.assertEquals(expand, "-1");
    }
    
    /**
     * When using DB_ID_RANGE Sharding method, expand param is including a number of nodes to be extended.
     * @throws Exception
     */
    @Test(priority = 13, groups = { TestGroup.ASS_SHARDING_DB_ID_RANGE })
    public void testRangeCheckSharding() throws Exception
    {
        String coreName = "alfresco";
        
        RestResponse response = restClient.withParams("coreName=" + coreName).withSolrAdminAPI().getAction("rangeCheck");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String expand = response.getResponse().body().jsonPath().get("expand").toString();             
        Assert.assertNotEquals(expand, "-1");        
    }

    /**
     * This action only applies to DB_ID_RANGE Sharding method.
     * This test verifies expected result when using another deployment
     * @throws Exception
     */
    @Test(priority = 14)
    public void testExpand() throws Exception
    {
        String coreName = "alfresco";
        String add = "1000";
        
        RestResponse response = restClient.withParams("coreName=" + coreName, "add=" + add).withSolrAdminAPI().getAction("expand");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        // This action only applies to DB_ID_RANGE Sharding method
        String expand = response.getResponse().body().jsonPath().get("expand").toString();             
        Assert.assertEquals(expand, "-1");
    }
    
    /**
     * When using DB_ID_RANGE Sharding method, expand param is including a number of nodes extended.
     * @throws Exception
     */
    @Test(priority = 15, groups = { TestGroup.ASS_SHARDING_DB_ID_RANGE })
    public void testExpandSharding() throws Exception
    {
        String coreName = "alfresco";
        String add = "1000";
        
        RestResponse response = restClient.withParams("coreName=" + coreName, "add=" + add).withSolrAdminAPI().getAction("expand");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        // This action only applies to DB_ID_RANGE Sharding method
        String expand = response.getResponse().body().jsonPath().get("expand").toString();             
        Assert.assertNotEquals(expand, "-1");
    }
    
    @Test(priority = 16)
    public void testPurge() throws Exception
    {
        String core = "alfresco";
        String txid = "1";
        
        RestResponse response = restClient.withParams("core=" + core, "txid=" + txid).withSolrAdminAPI().getAction("purge");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
    }
    
    @Test(priority = 17)
    public void testFix() throws Exception
    {
        String core = "alfresco";
        
        RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("fix");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core +".txToReindex"));
        Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core + ".aclChangeSetToReindex"));
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
    }
    
    @Test(priority = 18)
    public void testReindex() throws Exception
    {
        String core = "alfresco";
        String txid = "1";
        
        RestResponse response = restClient.withParams("core=" + core, "txid=" + txid).withSolrAdminAPI().getAction("reindex");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
    }
    
    @Test(priority = 19)
    public void testRetry() throws Exception
    {
        String core = "alfresco";
        
        RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("retry");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
    }
    
    @Test(priority = 20)
    public void testIndex() throws Exception
    {
        String core = "alfresco";
        String txid = "1";
        
        RestResponse response = restClient.withParams("core=" + core, "txid=" + txid).withSolrAdminAPI().getAction("index");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
    }
    
    @Test(priority = 21)
    public void testLog4J() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("log4j");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "success");
    }
    
    /**
     * This test will fail if it's executed twice
     * @throws Exception
     */
    @Test(priority = 22)
    public void testNewCore() throws Exception
    {
        String core = "newCore";
        String storeRef = "workspace://SpacesStore";
        String template = "rerank";

        RestResponse response = restClient.withParams("coreName=" + core, "storeRef=" + storeRef, "template=" + template)
                .withSolrAdminAPI().getAction("newCore");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "success");
    }

    /**
     * When creating a core that already exists, this action fails.
     * @throws Exception
     */   
    @Test(priority = 23)
    public void testNewCoreError() throws Exception
    {        
        String core = "alfresco";
        String template = "rerank";
        
        RestResponse response = restClient.withParams("coreName=" + core, "template=" + template)
                .withSolrAdminAPI().getAction("newCore");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "error");
    }
    
    @Test(priority = 24)
    public void testUpdateCore() throws Exception
    {
        String core = "alfresco";
        
        RestResponse response = restClient.withParams("coreName=" + core).withSolrAdminAPI().getAction("updateCore");

        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "success");
    }

    /**
     * When updating a core that doesn't exist, this action fails.
     * @throws Exception
     */   
    @Test(priority = 25)
    public void testUpdateCoreError() throws Exception
    {
        String core = "nonExistingCore";
        
        RestResponse response = restClient.withParams("coreName=" + core).withSolrAdminAPI().getAction("updateCore");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "error");
    }
   
    /**
     * This test updates "shared.properties" memory loading for every SOLR core.
     * @throws Exception
     */
    @Test(priority = 26)
    public void testUpdateShared() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("updateShared");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "success");
    }
    
    /**
     * This test will fail if it's executed twice
     * @throws Exception
     */
    @Test(priority = 27)
    public void testNewDefaultCore() throws Exception
    {
        String core = "newDefaultCore";
        String storeRef = "workspace://SpacesStore";
        String template = "rerank";
        
        RestResponse response = restClient
                .withParams("coreName=" + core, "storeRef=" + storeRef, "template=" + template)
                .withSolrAdminAPI().getAction("newDefaultIndex");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);

        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "success");
    }
    
    /**
     * When creating a core that already exists, this action fails.
     * @throws Exception
     */   
    @Test(priority = 28)
    public void testNewDefaultCoreError() throws Exception
    {
        String core = "alfresco";
        String template = "rerank";
        
        RestResponse response = restClient.withParams("coreName=" + core, "template=" + template)
                .withSolrAdminAPI().getAction("newDefaultIndex");

        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);

        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "error");
    }

    /**
     * This test has to be executed after "testNewCore" test, otherwise it will fail
     */    
    @Test(priority = 99, dependsOnMethods = ("testNewCore"))
    public void testRemoveCore() throws Exception
    {
        String core = "newCore";
        String storeRef = "workspace://SpacesStore";
        
        RestResponse response = restClient
                .withParams("coreName=" + core, "storeRef=" + storeRef)
                .withSolrAdminAPI().getAction("removeCore");
                
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
    }
   
}
