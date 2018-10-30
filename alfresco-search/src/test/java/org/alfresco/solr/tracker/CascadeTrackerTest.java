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
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.client.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.alfresco.solr.AlfrescoSolrUtils.*;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class CascadeTrackerTest extends AbstractAlfrescoSolrTests
{
    private static long MAX_WAIT_TIME = 80000;
    @BeforeClass
    public static void beforeClass() throws Exception 
    {
        initAlfrescoCore("schema.xml");
    }

    @After
    public void clearQueue() {
        SOLRAPIQueueClient.nodeMetaDataMap.clear();
        SOLRAPIQueueClient.transactionQueue.clear();
        SOLRAPIQueueClient.aclChangeSetQueue.clear();
        SOLRAPIQueueClient.aclReadersMap.clear();
        SOLRAPIQueueClient.aclMap.clear();
        SOLRAPIQueueClient.nodeMap.clear();
    }


    @Test 
    public void solrTracking_folderUpdate_shouldReIndexFolderAndChildren() throws Exception
    {
        /* Init Alfresco instance with acl, folder and files */
        BooleanQuery.Builder builder;
        BooleanQuery waitForQuery;

        Acl acl = initAcls();

        Transaction txn = getTransaction(0, 3);

        Node folderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node childFolderNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

        NodeMetaData folderMetaData = getNodeMetaData(folderNode, txn, acl, "mike", null, false);
        folderMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("folder1"));
        NodeMetaData childFolderMetaData = getNodeMetaData(childFolderNode, txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
        childFolderMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("folder2"));
        NodeMetaData fileMetaData = getNodeMetaData(fileNode, txn, acl, "mike", ancestors(folderMetaData.getNodeRef(),childFolderMetaData.getNodeRef()),
            false);
        fileMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("file1"));

        indexTransaction(txn, list(folderNode,childFolderNode, fileNode), list(folderMetaData, childFolderMetaData, fileMetaData));

        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")),
            BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery
            .newLongRange(QueryConstants.FIELD_S_TXID, txn.getId(), txn.getId() + 1, true, false),
            BooleanClause.Occur.MUST));
        waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);

        /* Update the folder */
        Transaction txn1 = getTransaction(0, 1);
        folderMetaData.getProperties()
            .put(ContentModel.PROP_CASCADE_TX, new StringPropertyValue(Long.toString(txn1.getId())));
        folderMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("folder2"));
        folderNode.setTxnId(txn1.getId());
        folderMetaData.setTxnId(txn1.getId());

        //Change the ancestor on the file just to see if it's been updated
        NodeRef nodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        childFolderMetaData.setAncestors(ancestors(nodeRef));
        fileMetaData.setAncestors(ancestors(nodeRef));

        indexTransaction(txn1, list(folderNode), list(folderMetaData));

        //Check for the TXN state stamp.
        builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!TX")),
            BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery
            .newLongRange(QueryConstants.FIELD_S_TXID, txn1.getId(), txn1.getId() + 1, true, false),
            BooleanClause.Occur.MUST));
        waitForDocCount(builder.build(), 1, MAX_WAIT_TIME);

        TermQuery termQuery1 = new TermQuery(new Term(QueryConstants.FIELD_ANCESTOR, nodeRef.toString()));

        waitForDocCount(termQuery1, 2, MAX_WAIT_TIME);

        //child folder and grandchild document must be updated
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", QueryConstants.FIELD_ANCESTOR + ":\"" + nodeRef.toString() + "\"");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        params.add("sort", "id asc");
        params.add("fq", "{!afts}AUTHORITY_FILTER_FROM_JSON");
        SolrServletRequest req = areq(params,
            "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}], \"authorities\": [ \"mike\"], \"tenants\": [ \"\" ]}");
        assertQ(req, "*[count(//doc)=2]", "//result/doc[1]/long[@name='DBID'][.='" + childFolderNode.getId() + "']", "//result/doc[2]/long[@name='DBID'][.='" + fileNode.getId() + "']");
    }

    private Acl initAcls() throws Exception
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet, Long.MAX_VALUE - 10); // Test with long value

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, list("jim"), list("phil"), null);

        indexAclChangeSet(aclChangeSet, list(acl, acl2), list(aclReaders, aclReaders2));

        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")),
            BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery
            .newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false),
            BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);
        return acl;
    }
}
