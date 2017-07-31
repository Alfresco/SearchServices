/*
 * Copyright (C) 2005-2017 Alfresco Software Limited.
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
package org.alfresco.solr.query;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.*;
import org.alfresco.solr.stream.AlfrescoSolrStream;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.HttpSolrClient;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.SolrParams;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_DOC_TYPE;
import static org.alfresco.solr.AlfrescoSolrUtils.*;

@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedStatsStreamTest extends AbstractAlfrescoDistributedTest {

    static final QName PROP_RATING = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "fiveStarRatingSchemeTotal");
    static final QName PROP_TRACK  = QName.createQName(NamespaceService.AUDIO_MODEL_1_0_URI, "trackNumber");

    @Rule
    public JettyServerRule jetty = new JettyServerRule(2, this);

    @Test
    public void testStats() throws Exception {

        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);

        indexAclChangeSet(aclChangeSet,
                list(acl),
                list(aclReaders));

        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCountAllCores(waitForQuery, 1, 80000);

        /*
        * Create and index a Transaction
        */

        //First create a transaction.
        int numNodes = 10;
        List<Node> nodes = new ArrayList();
        List<NodeMetaData> nodeMetaDatas = new ArrayList();

        Transaction bigTxn = getTransaction(0, numNodes);

        for (int i = 0; i < numNodes; i++) {
            Node node = getNode(bigTxn, acl, Node.SolrApiNodeStatus.UPDATED);
            nodes.add(node);
            NodeMetaData nodeMetaData = getNodeMetaData(node, bigTxn, acl, "mike", null, false);
            nodeMetaData.getProperties().put(ContentModel.PROP_TITLE, new StringPropertyValue("statsworld"));
            nodeMetaData.getProperties().put(PROP_RATING, new StringPropertyValue(""+(i+1)));
            nodeMetaData.getProperties().put(PROP_TRACK, new StringPropertyValue(""+(i*2)));
            nodeMetaDatas.add(nodeMetaData);
        }

        indexTransaction(bigTxn, nodes, nodeMetaDatas);
        waitForDocCount(new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "world")), numNodes, 100000);

        putHandleDefaults();

        List<SolrClient> clusterClients = getClusterClients();
        String shards = getShardsString(clusterClients);

        String alfrescoJson = "{ \"authorities\": [ \"mike\"], \"tenants\": [ \"\" ] }";
        String expr = "alfrescoStats(stats( myCollection, q=*.*, sum(audio:trackNumber), sum(cm:fiveStarRatingSchemeTotal), min(audio:trackNumber), min(cm:fiveStarRatingSchemeTotal), max(audio:trackNumber), max(cm:fiveStarRatingSchemeTotal), avg(audio:trackNumber), avg(cm:fiveStarRatingSchemeTotal), count(*)))";

        SolrParams params = params("expr", expr, "qt", "/stream", "myCollection.shards", shards);

        AlfrescoSolrStream tupleStream = new AlfrescoSolrStream(((HttpSolrClient)clusterClients.get(0)).getBaseURL(), params);
        tupleStream.setJson(alfrescoJson);
        List<Tuple> tuples = getTuples(tupleStream);

        assert (tuples.size() == 1);
        Tuple tuple = tuples.get(0);

        Double sumi = tuple.getDouble("sum(audio:trackNumber)");
        Double sumf = tuple.getDouble("sum(cm:fiveStarRatingSchemeTotal)");
        Double mini = tuple.getDouble("min(audio:trackNumber)");
        Double minf = tuple.getDouble("min(cm:fiveStarRatingSchemeTotal)");
        Double maxi = tuple.getDouble("max(audio:trackNumber)");
        Double maxf = tuple.getDouble("max(cm:fiveStarRatingSchemeTotal)");
        Double avgi = tuple.getDouble("avg(audio:trackNumber)");
        Double avgf = tuple.getDouble("avg(cm:fiveStarRatingSchemeTotal)");
        Double count = tuple.getDouble("count(*)");

        assertTrue(sumi.longValue() == 90);
        assertTrue(sumf.doubleValue() == 55.0D);
        assertTrue(mini.doubleValue() == 0.0D);
        assertTrue(minf.doubleValue() == 1.0D);
        assertTrue(maxi.doubleValue() == 18.0D);
        assertTrue(maxf.doubleValue() == 10.0D);
        assertTrue(avgi.doubleValue() == 9.0D);
        assertTrue(avgf.doubleValue() == 5.5D);
        assertTrue(count.doubleValue() == 10);
    }
}