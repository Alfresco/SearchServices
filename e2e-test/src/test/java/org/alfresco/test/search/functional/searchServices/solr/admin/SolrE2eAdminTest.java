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
 * http://<server>:<port>/solr/admin/cores?action=(actionName)
 * 
 * @author aborroy
 *
 */
@Configuration
public class SolrE2eAdminTest extends AbstractE2EFunctionalTest
{
    
    
    // SOLR default response status codes (returned in responseHeader.status)
    private static final String SOLR_RESPONSE_STATUS_OK = "0";
    
    // Alfresco SOLR action response status identifiers
    private static final String ACTION_RESPONSE_REPORT = "report";
    
    // Default Alfresco SOLR Core Names
    List<String> defaultCoreNames = new ArrayList<>(List.of("alfresco", "archive"));

    /**
     * Node Report for every core.
     * @throws Exception
     */
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
     * Node Report for an specific core.
     * @throws Exception
     */
    @Test(priority = 2)
    public void testNodeReportCore() throws Exception
    {
        final String nodeid = "200";
        
        defaultCoreNames.forEach(core -> {
            
            try
            {
            
                RestResponse response = restClient.withParams("nodeid=" + nodeid, "core=" + core).withSolrAdminAPI().getAction("nodeReport");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
                
                String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core).toString();
                Assert.assertNotNull(report);
                
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
    }
    
    /**
     * Node Report requires "nodeid" parameter.
     * This test will return an error as we are missing to pass the parameter. 
     * @throws Exception
     */
    @Test(priority = 3)
    public void testNodeReportError() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("nodeReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String reportError = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + ".error").toString();
        Assert.assertNotNull(reportError);
        
    }
    
    /**
     * ACL Report for every core.
     * @throws Exception
     */
    @Test(priority = 4)
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
     * ACL Report for an specific core.
     * @throws Exception
     */
    @Test(priority = 5)
    public void testAclReportCore() throws Exception
    {
        final String aclid = "1";
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("aclid=" + aclid, "core=" + core).withSolrAdminAPI().getAction("aclReport");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
                String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core).toString();
                Assert.assertNotNull(report);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
        
    }
    
    /**
     * ACL Report requires "aclid" parameter.
     * This test will fail as we are missing to pass the parameter.
     * @throws Exception
     */
    @Test(priority = 6)
    public void testAclReportError() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("aclReport");
        
        String reportError = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + ".error").toString();
        Assert.assertNotNull(reportError);
    }
    
    /**
     * TX Report for every core.
     * @throws Exception
     */
    @Test(priority = 7)
    public void testTxReport() throws Exception
    {
        String txid = "1";
        
        RestResponse response = restClient.withParams("txid=" + txid).withSolrAdminAPI().getAction("txReport");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);

        String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT).toString();
        Assert.assertNotNull(report);
    }
    
    /**
     * TX Report for an specific core.
     * @throws Exception
     */
    @Test(priority = 8)
    public void testTxReportCore() throws Exception
    {
        final String txid = "1";
        defaultCoreNames.forEach(core -> {
            
            try
            {
        
                RestResponse response = restClient.withParams("coreName=" + core, "txid=" + txid).withSolrAdminAPI().getAction("txReport");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
                String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core).toString();
                Assert.assertNotNull(report);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
    }
    
    /**
     * Transaction report requires "txid" parameter.
     * This test will fail as we are missing to pass the parameter.
     * @throws Exception
     */
    @Test(priority = 9)
    public void testTxReportError() throws Exception
    {
        String coreName = "alfresco";
        
        RestResponse response = restClient.withParams("coreName=" + coreName).withSolrAdminAPI().getAction("txReport");
        
        String reportError = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + ".error").toString();
        Assert.assertNotNull(reportError);
    }
    
    /**
     * ACL TX Report for every core.
     * @throws Exception
     */
    @Test(priority = 10)
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
     * ACL TX Report for specific core.
     * @throws Exception
     */
    @Test(priority = 11)
    public void testAclTxReportCore() throws Exception
    {
        final String acltxid = "1";
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("acltxid=" + acltxid, "core=" + core).withSolrAdminAPI().getAction("aclTxReport");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
                String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core).toString();
                Assert.assertNotNull(report);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
        
    }
    
    /**
     * AclTx report requires "acltxid" parameter.
     * This test will fail as we are missing to pass the parameter.
     * @throws Exception
     */
    @Test(priority = 12)
    public void testAclTxReportError() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("aclTxReport");
        
        String reportError = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + ".error").toString();
        Assert.assertNotNull(reportError);
    }
    
    /**
     * Report for every core.
     * @throws Exception
     */
    @Test(priority = 13)
    public void testReport() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction(ACTION_RESPONSE_REPORT);
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);

        String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT).toString();
        Assert.assertNotNull(report);
    }
    
    /**
     * Report for specific core.
     * @throws Exception
     */
    @Test(priority = 14)
    public void testReportCore() throws Exception
    {
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("coreName=" + core).withSolrAdminAPI().getAction(ACTION_RESPONSE_REPORT);
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
                String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core).toString();
                Assert.assertNotNull(report);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });        
    }
    
    /**
     * Report using params.
     * @throws Exception
     */
    @Test(priority = 15)
    public void testReportWithParams() throws Exception
    {
        Long fromTime = 0l;
        Long toTime = 0l;
        
        RestResponse response = restClient.withParams("fromTime=" + fromTime, "toTime=" + toTime).withSolrAdminAPI()
                .getAction(ACTION_RESPONSE_REPORT);
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);

        String report = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT).toString();
        Assert.assertNotNull(report);
    }
    
    /**
     * Summary for every core.
     * @throws Exception
     */
    @Test(priority = 16)
    public void testSummary() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("summary");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String report = response.getResponse().body().jsonPath().get("Summary").toString();
        Assert.assertNotNull(report);
    }
    
    /**
     * Summary for specific core.
     * @throws Exception
     */
    @Test(priority = 17)
    public void testSummaryCore() throws Exception
    {
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("summary");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
                
                String report = response.getResponse().body().jsonPath().get("Summary." + core).toString();
                Assert.assertNotNull(report);
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
    }
    
    /**
     * Check every core.
     * @throws Exception
     */
    @Test(priority = 18)
    public void testCheck() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("check");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "success");
    }
    
    /**
     * Check specific core.
     * @throws Exception
     */
    @Test(priority = 19)
    public void testCheckCore() throws Exception
    {
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("check");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
                Assert.assertEquals(actionStatus, "success");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
    }
    
    /**
     * This action only applies to DB_ID_RANGE Sharding method.
     * This test verifies expected result when using another deployment
     * @throws Exception
     */
    @Test(priority = 20)
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
    @Test(priority = 21, groups = { TestGroup.ASS_SHARDING_DB_ID_RANGE })
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
    @Test(priority = 22)
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
    @Test(priority = 23, groups = { TestGroup.ASS_SHARDING_DB_ID_RANGE })
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
    
    /**
     * Purge TX in every core.
     * @throws Exception
     */
    @Test(priority = 24)
    public void testPurge() throws Exception
    {
        String txid = "1";
        
        RestResponse response = restClient.withParams("txid=" + txid).withSolrAdminAPI().getAction("purge");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
    }
    
    /**
     * Purge TX in specific core.
     * @throws Exception
     */
    @Test(priority = 25)
    public void testPurgeCore() throws Exception
    {
        final String txid = "1";
        
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core, "txid=" + txid).withSolrAdminAPI().getAction("purge");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
                Assert.assertEquals(actionStatus, "scheduled");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
    }
    
    /**
     * Purge with no params produces an empty response.
     * @throws Exception
     */
    @Test(priority = 26)
    public void testPurgeEmpty() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("purge");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
    }
    
    /**
     * FIX for every core.
     * @throws Exception
     */
    @Test(priority = 27)
    public void testFix() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("fix");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        defaultCoreNames.forEach(core -> {
            Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core +".txToReindex"));
            Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core + ".aclChangeSetToReindex"));
        });
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
    }
    
    /**
     * FIX for specific core.
     * @throws Exception
     */
    @Test(priority = 28)
    public void testFixCore() throws Exception
    {
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("fix");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
                
                Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core +".txToReindex"));
                Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core + ".aclChangeSetToReindex"));
                
                String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
                Assert.assertEquals(actionStatus, "scheduled");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
    }
    
    /**
     * REINDEX for every core.
     * @throws Exception
     */
    @Test(priority = 29)
    public void testReindex() throws Exception
    {
        String txid = "1";
        
        RestResponse response = restClient.withParams("txid=" + txid).withSolrAdminAPI().getAction("reindex");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");        
    }
    
    /**
     * REINDEX for specific core.
     * @throws Exception
     */
    @Test(priority = 30)
    public void testReindexCore() throws Exception
    {
        String txid = "1";
        
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core, "txid=" + txid).withSolrAdminAPI().getAction("reindex");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
                Assert.assertEquals(actionStatus, "scheduled");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
    }
    
    /**
     * RETRY for every core.
     * @throws Exception
     */
    @Test(priority = 31)
    public void testRetry() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("retry");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
        
        defaultCoreNames.forEach(core -> {
            Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core));
            Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core));
        });
    }
    
    /**
     * RETRY for specific core.
     * @throws Exception
     */
    @Test(priority = 32)
    public void testRetryCore() throws Exception
    {
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("retry");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
                Assert.assertEquals(actionStatus, "scheduled");
                
                Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core));
                Assert.assertNotNull(response.getResponse().body().jsonPath().get("action." + core));
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
    }
    
    /**
     * INDEX for every core.
     * @throws Exception
     */
    @Test(priority = 33)
    public void testIndex() throws Exception
    {
        String txid = "1";
        
        RestResponse response = restClient.withParams("txid=" + txid).withSolrAdminAPI().getAction("index");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "scheduled");
    }
    
    /**
     * INDEX for specific core.
     * @throws Exception
     */
    @Test(priority = 34)
    public void testIndexCore() throws Exception
    {
        final String txid = "1";
        
        defaultCoreNames.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core, "txid=" + txid).withSolrAdminAPI().getAction("index");
                
                String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
                Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
                Assert.assertEquals(actionStatus, "scheduled");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
            
        });
    }
    
    /**
     * Reloads default log4j properties into memory.
     * @throws Exception
     */
    @Test(priority = 35)
    public void testLog4J() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("log4j");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "success");
    }
    
    /**
     * This test will fail as specified resource to reload doesn't exist.
     * @throws Exception
     */
    @Test(priority = 36)
    public void testLog4JError() throws Exception
    {
        RestResponse response = restClient.withParams("resource=log4j-unexisting.properties").withSolrAdminAPI().getAction("log4j");
        
        String status = response.getResponse().body().jsonPath().get("responseHeader.status").toString();             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status").toString();
        Assert.assertEquals(actionStatus, "error");
    }
    
    /**
     * This test will fail if it's executed twice
     * @throws Exception
     */
    @Test(priority = 37)
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
    @Test(priority = 38)
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
    
    /**
     * Reloads core configuration in memory.
     * @throws Exception
     */
    @Test(priority = 39)
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
    @Test(priority = 40)
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
    @Test(priority = 41)
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
    @Test(priority = 42)
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
    @Test(priority = 43)
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
