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

package org.alfresco.test.search.functional.searchServices.search;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.common.collect.Sets;

import org.alfresco.rest.search.RestInstanceModel;
import org.alfresco.rest.search.RestShardInfoModel;
import org.alfresco.rest.search.RestShardInfoModelCollection;
import org.alfresco.rest.search.RestShardModel;
import org.alfresco.search.TestGroup;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.springframework.http.HttpStatus;

/**
 * Shard info end point REST API test.
 *
 * @author Tuna Aksoy
 */
public class ShardInfoTest extends AbstractE2EFunctionalTest
{
    /* The test that will be excluded when running master slave setup, excluding the ASS_MASTER test group. */
    @Test(groups = { TestGroup.ACS_60n, TestGroup.CONFIG_MASTER })
    public void getShardInfoWithAdminAuthority() throws JsonProcessingException
    {
        RestShardInfoModelCollection info = restClient.authenticateUser(dataUser.getAdminUser()).withShardInfoAPI()
                .getInfo();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        info.assertThat().entriesListIsNotEmpty();
        assertEquals(info.getPagination().getTotalItems().intValue(), 2);

        List<String> stores = Arrays.asList("workspace://SpacesStore", "archive://SpacesStore");
        List<String> baseUrls = Arrays.asList("/solr/alfresco", "/solr/archive");

        List<RestShardInfoModel> entries = info.getEntries();
        for (RestShardInfoModel shardInfoModel : entries)
        {
            RestShardInfoModel model = shardInfoModel.getModel();
            assertEquals(model.getTemplate(), "rerank");
            assertEquals(model.getMode(), "MASTER");
            List<String> shardingMethods = Arrays.asList("DB_ID", "DB_ID_RANGE", "EXPLICIT_ID", "ACL_ID", "MOD_ACL_ID", "DATE", "PROPERTY");
            String shardingMethod = model.getShardMethod();
            assertTrue(shardingMethods.contains(shardingMethod), "Unexpected Sharding Method Found: " + shardingMethod);
            assertTrue(model.getHasContent());

            assertTrue(stores.contains(model.getStores()));

            List<RestShardModel> shards = model.getShards();
            assertNotNull(shards);
            RestShardModel shard = shards.iterator().next();
            assertNotNull(shard);
            List<RestInstanceModel> instances = shard.getInstances();
            assertNotNull(instances);
            RestInstanceModel instance = instances.iterator().next();
            assertNotNull(instance);

            assertTrue(baseUrls.contains(instance.getBaseUrl()));

            // TODO: Ideally Solr Host and Port should be Parameterised
            assertNotNull(instance.getHost(), "The solr host is not present");
            assertEquals(instance.getPort().intValue(), 8983);
            assertEquals(instance.getState(), "ACTIVE");
            assertEquals(instance.getMode(), "MASTER");
        }
    }

    /* The test that will be run when in master slave setup by including the ASS_MASTER_SLAVE test group. */
    @Test(groups = {TestGroup.ACS_60n, TestGroup.CONFIG_MASTER_SLAVE })
    public void getShardInfoWithAdminAuthorityMasterSlaveConfig() throws JsonProcessingException
    {
        RestShardInfoModelCollection info = restClient.authenticateUser(dataUser.getAdminUser()).withShardInfoAPI()
                .getInfo();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        info.assertThat().entriesListIsNotEmpty();
        assertEquals(info.getPagination().getTotalItems().intValue(), 2);

        List<String> stores = Arrays.asList("workspace://SpacesStore", "archive://SpacesStore");
        List<String> baseUrls = Arrays.asList("/solr/alfresco", "/solr/archive");

        List<RestShardInfoModel> entries = info.getEntries();
        for (RestShardInfoModel shardInfoModel : entries)
        {
            RestShardInfoModel model = shardInfoModel.getModel();
            assertEquals(model.getTemplate(), "rerank");
            assertEquals(model.getShardMethod(), "DB_ID");
            assertEquals(model.getMode(), "MIXED");
            assertTrue(model.getHasContent());

            assertTrue(stores.contains(model.getStores()));

            List<RestShardModel> shards = model.getShards();
            assertNotNull(shards);
            RestShardModel shard = shards.iterator().next();
            assertNotNull(shard);
            List<RestInstanceModel> instances = shard.getInstances();
            assertNotNull(instances);
            RestInstanceModel instance = instances.iterator().next();
            assertNotNull(instance);

            assertTrue(baseUrls.contains(instance.getBaseUrl()));
            
            assertEquals(instance.getState(), "ACTIVE");
        }
    }

    @Test(groups = { TestGroup.ACS_60n })
    public void getShardInfoWithoutAdminAuthority() throws Exception
    {
        restClient.authenticateUser(dataUser.createRandomTestUser()).withShardInfoAPI().getInfo();
        restClient.assertStatusCodeIs(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    /**
     * This is a test to check that sharding is correctly working on bamboo and locally. 
     * Include test group 'sharding' on bamboo to enable this test to run
     * @throws JsonProcessingException
     */
    
    @Test(groups = { TestGroup.ACS_60n, TestGroup.CONFIG_SHARDING })
    public void getShardInfoWith2OrMoreShards() throws JsonProcessingException
    {
        RestShardInfoModelCollection info = restClient.authenticateUser(dataUser.getAdminUser()).withShardInfoAPI().getInfo();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        info.assertThat().entriesListIsNotEmpty();
        
        assertEquals(info.getPagination().getTotalItems().intValue(), 2, "Pagination is: " + info.getPagination().getTotalItems().intValue() + " not expected value of 2");
     
        Set<String> stores = Sets.newHashSet("workspace://SpacesStore", "archive://SpacesStore");
        List<String> baseUrls = Arrays.asList("/solr/alfresco", "/solr/archive");
        List<RestShardInfoModel> entries = info.getEntries();
        
        Set<String> actualStores = entries.stream().map(shardInfoModel -> shardInfoModel.getModel().getStores()).collect(Collectors.toSet());
        assertEquals(actualStores, stores, "The number of stores do not match the expected number of stores");

        for (RestShardInfoModel shardInfoModel : entries)
        {
            RestShardInfoModel model = shardInfoModel.getModel();
            assertEquals(model.getTemplate(), "rerank", "Template is not rerank, template found: "+ model.getTemplate());
            assertEquals(model.getMode(), "MASTER", "Mode is not MASTER, mode found: "+ model.getMode());
            assertTrue(model.getHasContent(), "There is no content on the shards");
            assertTrue(model.getNumberOfShards()>=2, "Number of shards is not equal to or greater than 2");
            
            List<String> shardingMethods = Arrays.asList("DB_ID", "DB_ID_RANGE", "EXPLICIT_ID", "ACL_ID", "MOD_ACL_ID", "DATE", "PROPERTY");
            String shardingMethod = model.getShardMethod();
            assertTrue(shardingMethods.contains(shardingMethod), "Unexpected Sharding Method Found: " + shardingMethod);
            
            List<RestShardModel> shards = model.getShards();
            assertNotNull(shards, "There are no shards present");         
            for (RestShardModel shardInstance : shards)
            {
                List<RestInstanceModel> instanceList = shardInstance.getInstances();
                for (RestInstanceModel instanceX : instanceList)
                {
                    assertTrue(baseUrls.contains(instanceX.getBaseUrl()), "The baseUrl is not present, baseUrl found is: " + instanceX.getBaseUrl());
                    assertEquals(instanceX.getState(), "ACTIVE", "Shard state is not ACTIVE, shard state is: " + instanceX.getState());
                    assertNotNull(instanceX.getPort(), "There is not port found for the instance");
                    assertEquals(instanceX.getMode(), "MASTER", "Mode is not MASTER, mode found: "+ instanceX.getMode());
                    assertTrue(instanceX.getTransactionsRemaining() >= 0, "Transactions remaining is not more than 0, transactions remaining: " + instanceX.getTransactionsRemaining());
                    String shardParams = (instanceX).getShardParams();            
                    switch (shardingMethod)            
                    {
        	            case "MOD_ACL_ID":
        	            	break;
        	            case "ACL_ID":
        	            	break;
        	            case "DB_ID":
        	            	break;
        	            case "DB_ID_RANGE":
                        	assertTrue(shardParams.contains("shard.range="), "Shard Parameters Not as expected for the Shard Method: DB_ID_RANGE");
                        	break;
        	            case "DATE":
                        	assertTrue(shardParams.contains("shard.key="), "Shard Parameters Not as expected for the Shard Method: DATE");
                        	assertTrue(shardParams.contains("shard.date.grouping="), "Shard Parameters Not as expected for the Shard Method: DATE");
                        	break;
                        case "PROPERTY":
                        	assertTrue(shardParams.contains("shard.key="), "Shard Parameters Not as expected for the Shard Method: PROPERTY"); 
                        	assertTrue(shardParams.contains("shard.regex="), "Shard Parameters Not as expected for the Shard Method: PROPERTY");
                        	break;
                        case "EXPLICIT_ID":
                        	assertTrue(shardParams.contains("shard.key="), "Shard Parameters Not as expected for the Shard Method: EXPLICIT_ID");  
                        	break;
                        default:
                            throw new AssertionError("Shard Method Not expected: " + model.getShardMethod());
                    }                    
                }
            }           
        }
    }
}
