/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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
package org.alfresco.solr.tracker;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Rule;
import org.junit.Test;

import java.util.*;

import static org.alfresco.solr.AlfrescoSolrUtils.*;

/**
 * Tests sharding by quarter
 * @author Gethin James
 */

@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedDateQuarterAlfrescoSolrTrackerTest extends DistributedDateAbstractSolrTrackerTest
{
    @Rule
    public JettyServerRule jetty = new JettyServerRule(this.getClass().getSimpleName(), 6, getShardMethod(), new String[]{DEFAULT_TEST_CORENAME});

    @Override
    protected void assertCorrect(int numNodes) throws Exception {
        //We should expect roughly 25% on each of the 4 cores
        int shardHits = assertNodesPerShardGreaterThan((int)((numNodes)*.22), true);
        //We have 6 shards but we are sharding by quarter so only 4 of the shards should be used.
        assertEquals(4, shardHits);
    }

    @Override
    protected Properties getShardMethod()
    {
        Properties prop = new Properties();
        prop.put("shard.method", ShardMethodEnum.DATE.toString());
        prop.put("shard.date.grouping", "3");
        return prop;
    }
}

