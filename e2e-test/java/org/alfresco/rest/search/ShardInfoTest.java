/*
 * Copyright (C) 2005-2018 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.alfresco.rest.search;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;

import org.alfresco.rest.exception.EmptyRestModelCollectionException;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.report.Bug;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

/**
 * Shard info end point REST API test.
 *
 * @author Tuna Aksoy
 */
public class ShardInfoTest extends AbstractSearchTest
{
    @Bug(id="DELENG-1", status=Bug.Status.FIXED)
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ACS_60n})
    public void getShardInfoWithAdminAuthority() throws JsonProcessingException, EmptyRestModelCollectionException
    {
        RestShardInfoModelCollection info = restClient.authenticateUser(dataUser.getAdminUser()).withShardInfoAPI().getInfo();
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
            stores.contains(model.getStores());
            List<RestShardModel> shards = model.getShards();
            assertNotNull(shards);
            RestShardModel shard = shards.iterator().next();
            assertNotNull(shard);
            List<RestInstanceModel> instances = shard.getInstances();
            assertNotNull(instances);
            RestInstanceModel instance = instances.iterator().next();
            assertNotNull(instance);
            baseUrls.contains(instance.getBaseUrl());
            // TODO: Ideally Solr Host and Port should be Parameterised
            assertEquals(instance.getHost(), "search");
            assertEquals(instance.getPort().intValue(), 8983);
            assertEquals(instance.getState(), "ACTIVE");
            assertEquals(instance.getMode(), "MASTER");
        }
    }

    @Bug(id="DELENG-1", status=Bug.Status.FIXED)
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ACS_60n})
    public void getShardInfoWithoutAdminAuthority() throws Exception
    {
        restClient.authenticateUser(dataUser.createRandomTestUser()).withShardInfoAPI().getInfo();
        restClient.assertStatusCodeIs(HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
