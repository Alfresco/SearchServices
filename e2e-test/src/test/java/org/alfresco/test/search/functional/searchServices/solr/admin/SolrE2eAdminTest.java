/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

package org.alfresco.test.search.functional.searchServices.solr.admin;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.search.TestGroup;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.springframework.context.annotation.Configuration;
import org.testng.Assert;
import org.testng.annotations.Test;

import static java.util.Collections.emptyList;

/**
 * End to end tests for SOLR Admin actions REST API, available from:
 * 
 * http://<server>:<port>/solr/admin/cores?action=(actionName)
 * 
 * @author aborroy
 * @author mbhave
 *
 */
@Configuration
public class SolrE2eAdminTest extends AbstractE2EFunctionalTest
{
    
    // SOLR default response status codes (returned in responseHeader.status)
    private static final Integer SOLR_RESPONSE_STATUS_OK = 0;
    
    // Alfresco SOLR action response status identifiers
    private static final String ACTION_RESPONSE_REPORT = "report";
    
    // Default Alfresco SOLR Core Names
    private static final List<String> DEFAULT_CORE_NAMES = new ArrayList<>(List.of("alfresco", "archive"));
    
    /**
     * Check that SOLR Response Header contains a Query Time (qtime) and status equals to 0
     * @param response SOLR REST API Response in JSON
     */
    private void checkResponseStatusOk(RestResponse response)
    {
        Integer qtime = response.getResponse().body().jsonPath().get("responseHeader.QTime");
        Assert.assertTrue(qtime >= 0, "Expeted responseHeader.QTime to be a positive number");
        Integer status = response.getResponse().body().jsonPath().get("responseHeader.status");             
        Assert.assertEquals(status, SOLR_RESPONSE_STATUS_OK, "Expected " + SOLR_RESPONSE_STATUS_OK + " in responseHeader.status,");
    }

    /**
     * FIX for specific core.
     * The test checks the response structure in order to make sure the expected sections are present.
     *
     * We are not testing the content of each section because due to the underlying E2E infrastructure, we cannot know
     * in advance the transactions that will be scheduled for reindexing.
     */
    @Test(priority = 0)
    public void testFixCore()
    {
        DEFAULT_CORE_NAMES.forEach(core -> {
            try
            {
                RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("fix");

                checkResponseStatusOk(response);

                Map<String, Object> txInIndexNotInDb = response.getResponse().body().jsonPath().get("action." + core +".txToReindex.txInIndexNotInDb");
                Assert.assertNotNull(txInIndexNotInDb, "Expected a list of transactions (even empty) that are in index but not in the database to be reindexed,");

                Map<String, Object> duplicatedTx = response.getResponse().body().jsonPath().get("action." + core +".txToReindex.duplicatedTxInIndex");
                Assert.assertNotNull(duplicatedTx, "Expected a list of duplicated transactions (even empty) to be reindexed,");

                Map<String, Object> missingTx = response.getResponse().body().jsonPath().get("action." + core +".txToReindex.missingTxInIndex");
                Assert.assertNotNull(missingTx, "Expected a list of missing transactions (or empty list) to be reindexed,");

                Map<String, Object> aclTxInIndexNotInDb = response.getResponse().body().jsonPath().get("action." + core + ".aclChangeSetToReindex.aclTxInIndexNotInDb");
                Assert.assertNotNull(aclTxInIndexNotInDb, "Expected a list of ACLs (or empty list) to be reindexed,");

                Map<String, Object> duplicatedAclTxInIndex = response.getResponse().body().jsonPath().get("action." + core + ".aclChangeSetToReindex.duplicatedAclTxInIndex");
                Assert.assertNotNull(duplicatedAclTxInIndex, "Expected a list of ACLs (or empty list) to be reindexed,");

                Map<String, Object> missingAclTxInIndex = response.getResponse().body().jsonPath().get("action." + core + ".aclChangeSetToReindex.missingAclTxInIndex");
                Assert.assertNotNull(missingAclTxInIndex, "Expected a list of ACLs (or empty list) to be reindexed,");

                String actionStatus = response.getResponse().body().jsonPath().get("action.status");
                Assert.assertEquals(actionStatus, "notScheduled");
            }
            catch (Exception e)
            {
                throw new RuntimeException(e);
            }
        });
    }

    /**
     * Node Report for every core.
     * @throws Exception
     */
    @Test(priority = 1)
    public void testNodeReport() throws Exception
    {
        Integer nodeid = 200;
        RestResponse response = restClient.withParams("nodeid=" + nodeid).withSolrAdminAPI().getAction("nodeReport");
        
        checkResponseStatusOk(response);
        
        DEFAULT_CORE_NAMES.forEach(core -> {
            Integer reportNodeid = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".'Node DBID'");
            Assert.assertEquals(reportNodeid, nodeid, "Expected " + nodeid + " in " + ACTION_RESPONSE_REPORT + "." + core + ".'Node DBID',");
        });
    }
    
    /**
     * Node Report for an specific core.
     * @throws Exception
     */
    @Test(priority = 2)
    public void testNodeReportCore() throws Exception
    {
        final Integer nodeid = 200;
        
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
            
                RestResponse response = restClient.withParams("nodeid=" + nodeid, "core=" + core).withSolrAdminAPI().getAction("nodeReport");
                
                checkResponseStatusOk(response);
                
                Integer reportNodeid = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".'Node DBID'");
                Assert.assertEquals(reportNodeid, nodeid, "Expected " + nodeid + " in " + ACTION_RESPONSE_REPORT + "." + core + ".'Node DBID',");
                
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
        
        checkResponseStatusOk(response);
        
        String reportError = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + ".error");
        Assert.assertEquals(reportError, "No nodeid parameter set.", "Unexpected message in " + ACTION_RESPONSE_REPORT + ".error,");
        
    }
    
    /**
     * ACL Report for every core.
     * @throws Exception
     */
    @Test(priority = 4)
    public void testAclReport() throws Exception
    {
        Integer aclid = 1;
        RestResponse response = restClient.withParams("aclid=" + aclid).withSolrAdminAPI().getAction("aclReport");
        
        checkResponseStatusOk(response);

        DEFAULT_CORE_NAMES.forEach(core -> {
            Integer reportAclid = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".'Acl Id'");
            Assert.assertEquals(reportAclid, aclid, "Expected " + aclid + " in " + ACTION_RESPONSE_REPORT + "." + core + ".'Acl Id',");
        });
    }
    
    /**
     * ACL Report for an specific core.
     * @throws Exception
     */
    @Test(priority = 5)
    public void testAclReportCore() throws Exception
    {
        final Integer aclid = 1;
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("aclid=" + aclid, "core=" + core).withSolrAdminAPI().getAction("aclReport");
                
                checkResponseStatusOk(response);
        
                Integer reportAclid = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".'Acl Id'");
                Assert.assertEquals(reportAclid, aclid, "Expected " + aclid + " in " + ACTION_RESPONSE_REPORT + "." + core + ".'Acl Id',");
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
        
        checkResponseStatusOk(response);
        
        String reportError = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + ".error");
        Assert.assertEquals(reportError, "No aclid parameter set.", "Unexpected message in " + ACTION_RESPONSE_REPORT + ".error,");
    }
    
    /**
     * TX Report for every core.
     * @throws Exception
     */
    @Test(priority = 7)
    public void testTxReport() throws Exception
    {
        Integer txid = 1;
        
        RestResponse response = restClient.withParams("txid=" + txid).withSolrAdminAPI().getAction("txReport");
        
        checkResponseStatusOk(response);

        DEFAULT_CORE_NAMES.forEach(core -> {
            Integer reportTxid = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".TXID");
            Assert.assertEquals(reportTxid, txid, "Expected " + txid + " in " + ACTION_RESPONSE_REPORT + "." + core + ".TXID,");
        });
    }
    
    /**
     * TX Report for an specific core.
     * @throws Exception
     */
    @Test(priority = 8)
    public void testTxReportCore() throws Exception
    {
        final Integer txid = 1;
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
        
                RestResponse response = restClient.withParams("coreName=" + core, "txid=" + txid).withSolrAdminAPI().getAction("txReport");
                
                checkResponseStatusOk(response);
        
                Integer reportTxid = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".TXID");
                Assert.assertEquals(reportTxid, txid, "Expected " + txid + " in " + ACTION_RESPONSE_REPORT + "." + core + ".TXID,");
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
        
        checkResponseStatusOk(response);
        
        String reportError = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + ".error");
        Assert.assertEquals(reportError, "No txid parameter set.", "Unexpected message in " + ACTION_RESPONSE_REPORT + ".error,");
    }
    
    /**
     * ACL TX Report for every core.
     * @throws Exception
     */
    @Test(priority = 10)
    public void testAclTxReport() throws Exception
    {
        Integer acltxid = 1;
        
        RestResponse response = restClient.withParams("acltxid=" + acltxid).withSolrAdminAPI().getAction("aclTxReport");
        
        checkResponseStatusOk(response);

        DEFAULT_CORE_NAMES.forEach(core -> {
            Integer reportAcltxidCount = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".aclTxDbAclCount");
            Assert.assertEquals(reportAcltxidCount, Integer.valueOf(2), "Expected 2 in " + ACTION_RESPONSE_REPORT + "." + core + ".aclTxDbAclCount,");
        });
    }
    
    /**
     * ACL TX Report for specific core.
     * @throws Exception
     */
    @Test(priority = 11)
    public void testAclTxReportCore() throws Exception
    {
        final Integer acltxid = 1;
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("acltxid=" + acltxid, "core=" + core).withSolrAdminAPI().getAction("aclTxReport");
                
                checkResponseStatusOk(response);
        
                Integer reportAcltxidCount = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".aclTxDbAclCount");
                Assert.assertEquals(reportAcltxidCount, Integer.valueOf(2), "Expected 2 in " + ACTION_RESPONSE_REPORT + "." + core + ".aclTxDbAclCount,");
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
        
        checkResponseStatusOk(response);
        
        String reportError = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + ".error");
        Assert.assertEquals(reportError, "No acltxid parameter set.", "Unexpected message in " + ACTION_RESPONSE_REPORT + ".error,");
    }
    
    /**
     * Report for every core.
     * @throws Exception
     */
    @Test(priority = 13)
    public void testReport() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction(ACTION_RESPONSE_REPORT);
        
        checkResponseStatusOk(response);

        DEFAULT_CORE_NAMES.forEach(core -> {
            Integer reportTxCount = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".'DB transaction count'");
            Assert.assertTrue(reportTxCount > 0, "Expecting a positive integer in " + ACTION_RESPONSE_REPORT + "." + core + ".'DB transaction count',");
        });
    }
    
    /**
     * Report for specific core.
     * @throws Exception
     */
    @Test(priority = 14)
    public void testReportCore() throws Exception
    {
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("coreName=" + core).withSolrAdminAPI().getAction(ACTION_RESPONSE_REPORT);
                
                checkResponseStatusOk(response);
        
                Integer reportTxCount = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".'DB transaction count'");
                Assert.assertTrue(reportTxCount > 0, "Expecting a positive integer in " + ACTION_RESPONSE_REPORT + "." + core + ".'DB transaction count',");
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
        
        checkResponseStatusOk(response);

        DEFAULT_CORE_NAMES.forEach(core -> {
            Integer reportTxCount = response.getResponse().body().jsonPath().get(ACTION_RESPONSE_REPORT + "." + core + ".'DB transaction count'");
            Assert.assertTrue(reportTxCount == 0, "Expecting 0 in " + ACTION_RESPONSE_REPORT + "." + core + ".'DB transaction count',");
        });
    }
    
    /**
     * Summary for every core.
     * @throws Exception
     */
    @Test(priority = 16)
    public void testSummary() throws Exception
    {
        RestResponse response = restClient.withSolrAdminAPI().getAction("summary");
        
        checkResponseStatusOk(response);
        
        DEFAULT_CORE_NAMES.forEach(core -> {
            Integer reportTxCount = response.getResponse().body().jsonPath().get("Summary." + core + ".'Alfresco Transactions in Index'");
            Assert.assertTrue(reportTxCount > 0, "Expecting a positive integer in Summary." + core + ".'Alfresco Transactions in Index',");
            
        });
    }
    
    /**
     * Summary for specific core.
     * @throws Exception
     */
    @Test(priority = 17)
    public void testSummaryCore() throws Exception
    {
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("summary");
                
                checkResponseStatusOk(response);
                
                Integer reportTxCount = response.getResponse().body().jsonPath().get("Summary." + core + ".'Alfresco Transactions in Index'");
                Assert.assertTrue(reportTxCount > 0, "Expecting a positive integer in Summary." + core + ".'Alfresco Transactions in Index',");
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
        
        checkResponseStatusOk(response);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
        Assert.assertEquals(actionStatus, "success");
    }
    
    /**
     * Check specific core.
     * @throws Exception
     */
    @Test(priority = 19)
    public void testCheckCore() throws Exception
    {
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("check");
                
                checkResponseStatusOk(response);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action.status");
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
    @Test(priority = 20, groups = {TestGroup.CONFIG_SHARDING})
    public void testRangeCheck() throws Exception
    {
        String coreName = "alfresco";
        
        RestResponse response = restClient.withParams("coreName=" + coreName).withSolrAdminAPI().getAction("rangeCheck");
        
        checkResponseStatusOk(response);
        
        Integer expand = response.getResponse().body().jsonPath().get("expand");   

        // RangeCheck action only applies to DB_ID_RANGE Sharding method, so expect error in other sharding methods and success for DB_ID_RANGE
        if (ShardingMethod.DB_ID_RANGE.toString().equalsIgnoreCase(getShardMethod()))
        {
            // This assertion replicates: testRangeCheckSharding, priority = 21, hence deleting that test as duplicate
            // Value -1 is expected when the next shard already has nodes indexed.
            Assert.assertTrue((expand == Integer.valueOf(-1) || expand == Integer.valueOf(0)), "RangeCheck should not have been allowed when not using Shard DB_ID_RANGE method,");
            
            Integer minDbid = response.getResponse().body().jsonPath().get("minDbid");
            Assert.assertTrue(minDbid > 0, "RangeCheck is not successful when not using Shard DB_ID_RANGE method,");
        }
        else
        {
            Assert.assertEquals(expand, Integer.valueOf(-1), "RangeCheck should not have been allowed when not using Shard DB_ID_RANGE method,");

            String exception = response.getResponse().body().jsonPath().get("exception");
            Assert.assertEquals(exception, "ERROR: Wrong document router type:DBIDRouter", "Expansion should not have been allowed when not using Shard DB_ID_RANGE method,");
        }
    }

    /**
     * This action only applies to DB_ID_RANGE Sharding method.
     * This test verifies expected result when using another deployment
     * @throws Exception
     */
    @Test(priority = 22, groups = { TestGroup.CONFIG_SHARDING})
    public void testExpand() throws Exception
    {
        String coreName = "alfresco";
        String add = "1000";
        
        RestResponse response = restClient.withParams("coreName=" + coreName, "add=" + add).withSolrAdminAPI().getAction("expand");
        
        checkResponseStatusOk(response);

        Integer expand = response.getResponse().body().jsonPath().get("expand");  

        // Expand action only applies to DB_ID_RANGE Sharding method, so expect error in other sharding methods and success for DB_ID_RANGE
        if (ShardingMethod.DB_ID_RANGE.toString().equalsIgnoreCase(getShardMethod()))
        {
            // This assertion replicates: testExpandSharding, priority = 23, hence deleting that test as duplicate
            Assert.assertTrue((expand == Integer.valueOf(-1) || expand == Integer.valueOf(0)), "Expansion is not successful when not using Shard DB_ID_RANGE method,");
            if (expand == Integer.valueOf(-1))
            {
                String exceptionExpected = "Expansion cannot occur if max DBID in the index is more then 75% of range.";
                String exception = response.getResponse().body().jsonPath().get("exception");
                Assert.assertEquals(exception, exceptionExpected, "Expansion failed with unexpected Exception while using DB_ID_RANGE sharding");
            }
        }
        else
        {
            Assert.assertEquals(expand, Integer.valueOf(-1), "Expansion should not have been allowed when not using Shard DB_ID_RANGE method");
        }
    }
    
    /**
     * Purge TX in every core.
     * @throws Exception
     */
    @Test(priority = 24)
    public void testPurge() throws Exception
    {
        Integer txid = 1;
        
        RestResponse response = restClient.withParams("txid=" + txid).withSolrAdminAPI().getAction("purge");
        
        checkResponseStatusOk(response);

        DEFAULT_CORE_NAMES.forEach(core -> {
            String actionStatus = response.getResponse().body().jsonPath().get("action." + core + ".status");
            Assert.assertEquals(actionStatus, "scheduled");
        });
    }
    
    /**
     * Purge TX in specific core.
     * @throws Exception
     */
    @Test(priority = 25)
    public void testPurgeCore()
    {
        final Integer txid = 1;
        
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core, "txid=" + txid).withSolrAdminAPI().getAction("purge");
                
                checkResponseStatusOk(response);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action." + core + ".status");
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
        
        checkResponseStatusOk(response);

        DEFAULT_CORE_NAMES.forEach(core -> {
            String actionStatus = response.getResponse().body().jsonPath().get("action." + core + ".status");
            Assert.assertEquals(actionStatus, "scheduled");
        });
    }
    
    /**
     * REINDEX for every core.
     * @throws Exception
     */
    @Test(priority = 29)
    public void testReindex() throws Exception
    {
        Integer txid = 1;
        
        RestResponse response = restClient.withParams("txid=" + txid).withSolrAdminAPI().getAction("reindex");
        
        checkResponseStatusOk(response);

        DEFAULT_CORE_NAMES.forEach(core -> {
            String actionStatus = response.getResponse().body().jsonPath().get("action." + core + ".status");
            Assert.assertEquals(actionStatus, "scheduled");
        });
    }
    
    /**
     * REINDEX for specific core.
     * @throws Exception
     */
    @Test(priority = 30)
    public void testReindexCore()
    {
        Integer txid = 1;
        
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core, "txid=" + txid).withSolrAdminAPI().getAction("reindex");
                
                checkResponseStatusOk(response);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action." + core + ".status");
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
        
        checkResponseStatusOk(response);
        
        DEFAULT_CORE_NAMES.forEach(core -> {
            String actionStatus = response.getResponse().body().jsonPath().get("action." + core + ".status");
            Assert.assertEquals(actionStatus, "scheduled");

            List<String> errorNodeList = response.getResponse().body().jsonPath().get("action." + core + "['Error Nodes']");
            Assert.assertEquals(errorNodeList, emptyList(), "Expected no error nodes,");
        });
    }
    
    /**
     * RETRY for specific core.
     * @throws Exception
     */
    @Test(priority = 32)
    public void testRetryCore()
    {
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core).withSolrAdminAPI().getAction("retry");
                
                checkResponseStatusOk(response);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action." + core + ".status");
                Assert.assertEquals(actionStatus, "scheduled");
                
                List<String> errorNodeList = response.getResponse().body().jsonPath().get("action." + core + "['Error Nodes']");
                Assert.assertEquals(errorNodeList, emptyList(), "Expected no error nodes,");
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
        Integer txid = 1;
        
        RestResponse response = restClient.withParams("txid=" + txid).withSolrAdminAPI().getAction("index");
        
        checkResponseStatusOk(response);
        DEFAULT_CORE_NAMES.forEach(core -> {
            String actionStatus = response.getResponse().body().jsonPath().get("action." + core + ".status");
            Assert.assertEquals(actionStatus, "scheduled");
        });
    }
    
    /**
     * INDEX for specific core.
     * @throws Exception
     */
    @Test(priority = 34)
    public void testIndexCore() throws Exception
    {
        final Integer txid = 1;
        
        DEFAULT_CORE_NAMES.forEach(core -> {
            
            try
            {
                RestResponse response = restClient.withParams("core=" + core, "txid=" + txid).withSolrAdminAPI().getAction("index");
                
                checkResponseStatusOk(response);
                
                String actionStatus = response.getResponse().body().jsonPath().get("action." + core + ".status");
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
        
        checkResponseStatusOk(response);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
        Assert.assertEquals(actionStatus, "success");
    }
    
    /**
     * This REST API call will fail as the specified resource to reload doesn't exist.
     * @throws Exception
     */
    @Test(priority = 36)
    public void testLog4JError() throws Exception
    {
        RestResponse response = restClient.withParams("resource=log4j-unexisting.properties").withSolrAdminAPI().getAction("log4j");
        
        checkResponseStatusOk(response);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
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
        
        checkResponseStatusOk(response);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
        Assert.assertEquals(actionStatus, "success");
        
        String actionCore = response.getResponse().body().jsonPath().get("action.core");
        Assert.assertEquals(actionCore, core, "Created core name is expected in action.core,");
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
        
        checkResponseStatusOk(response);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
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

        checkResponseStatusOk(response);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
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
        
        checkResponseStatusOk(response);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
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
        
        checkResponseStatusOk(response);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
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
        
        checkResponseStatusOk(response);

        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
        Assert.assertEquals(actionStatus, "success");
        
        String actionCore = response.getResponse().body().jsonPath().get("action.core");
        Assert.assertEquals(actionCore, core, "Created core name is expected in action.core,");     
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

        checkResponseStatusOk(response);

        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
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
                
        checkResponseStatusOk(response);
        
        String actionStatus = response.getResponse().body().jsonPath().get("action.status");
        Assert.assertEquals(actionStatus, "success");
        
    }
   
}
