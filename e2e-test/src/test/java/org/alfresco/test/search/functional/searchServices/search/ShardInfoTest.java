/*
 * Copyright (C) 2005-2018 Alfresco Software Limited.
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
package org.alfresco.test.search.functional.searchServices.search;

import org.testng.annotations.Test;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

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
    @Test(groups = { TestGroup.ACS_60n, TestGroup.ASS_MASTER })
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
            assertEquals(model.getShardMethod(), "DB_ID");
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
            assertEquals(instance.getHost(), "search");
            assertEquals(instance.getPort().intValue(), 8983);
            assertEquals(instance.getState(), "ACTIVE");
            assertEquals(instance.getMode(), "MASTER");
        }
    }

    /* The test that will be run when in master slave setup by including the ASS_MASTER_SLAVE test group. */
    @Test(groups = {TestGroup.ACS_60n, TestGroup.ASS_MASTER_SLAVE })
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
}
