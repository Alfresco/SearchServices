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

package org.alfresco.solr.writer;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.client.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.internal.csv.CSVParser;
import org.apache.solr.internal.csv.CSVStrategy;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.StringReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import static org.alfresco.solr.AlfrescoSolrUtils.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AlfrescoCSVWriterTest extends AbstractAlfrescoSolrTests
{
    private static Log logger = LogFactory.getLog(AlfrescoCSVWriterTest.class);
    private static long MAX_WAIT_TIME = 80000;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema-afts.xml");
    }

    @After
    public void clearQueue() throws Exception {
        SOLRAPIQueueClient.nodeMetaDataMap.clear();
        SOLRAPIQueueClient.transactionQueue.clear();
        SOLRAPIQueueClient.aclChangeSetQueue.clear();
        SOLRAPIQueueClient.aclReadersMap.clear();
        SOLRAPIQueueClient.aclMap.clear();
        SOLRAPIQueueClient.nodeMap.clear();
        SOLRAPIQueueClient.nodeContentMap.clear();
    }


    @Test
    public void testWriting() throws Exception
    {
        /*
        * Create and index an AclChangeSet.
        */

        logger.info("######### Starting Writer test ###########");
        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);
        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, list("jim"), list("phil"), null);

        indexAclChangeSet(aclChangeSet,
                list(acl, acl2),
                list(aclReaders, aclReaders2));


        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);

        logger.info("#################### Passed First Test ##############################");

        //First create a transaction.
        Transaction foldertxn = getTransaction(0, 1);
        Transaction txn = getTransaction(0, 2);

        //Next create two nodes to update for the transaction
        Node folderNode = getNode(foldertxn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode2 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

        String testUser = "mike"+generateId();
        NodeMetaData folderMetaData = getNodeMetaData(folderNode, foldertxn, acl, testUser, null, false);
        NodeMetaData fileMetaData   = getNodeMetaData(fileNode,  txn, acl, testUser, ancestors(folderMetaData.getNodeRef()), false);
        NodeMetaData fileMetaData2  = getNodeMetaData(fileNode2, txn, acl, testUser, ancestors(folderMetaData.getNodeRef()), false);

        fileMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("my file1 Name"));
        HashMap<Locale, String> title = new HashMap<Locale, String> ();
        title.put(Locale.ENGLISH, "title1");
        fileMetaData.getProperties().put(ContentModel.PROP_TITLE, new  MLTextPropertyValue(title));
        HashMap<Locale, String> desc = new HashMap<Locale, String> ();
        desc.put(Locale.ENGLISH, "file desc");
        fileMetaData.getProperties().put(ContentModel.PROP_DESCRIPTION, new  MLTextPropertyValue(desc));

        fileMetaData2.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("my file2 name"));
        HashMap<Locale, String> title2 = new HashMap<Locale, String> ();
        title2.put(Locale.ENGLISH, "file2");
        fileMetaData2.getProperties().put(ContentModel.PROP_TITLE,  new  MLTextPropertyValue(title2));

        //Index the transaction, nodes, and nodeMetaDatas.
        indexTransaction(foldertxn, list(folderNode), list(folderMetaData));
        indexTransaction(txn,
                list(fileNode, fileNode2),
                list(fileMetaData, fileMetaData2));
        logger.info("######### Waiting for Doc Count ###########");

        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_OWNER, testUser)), 3, 10000);

        logger.info("######### Testing CSV writer ###########");

        SolrServletRequest req = areq(params( "q", "OWNER:"+testUser, "qt", "/afts",
                "wt", "csv", "fl", "*"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        String response = queryRequest(req);
        String[][] data = new CSVParser(new StringReader(response)).getAllValues();
        assertTrue("There should be 4 rows, 1 header and 3 rows of data", data.length == 4);
        List<String> headers = Arrays.asList(data[0]);
        //All fields (although I want do them all)
        assertTrue(headers.contains("DBID"));
        assertTrue(headers.contains("id"));
        assertTrue(headers.contains("OWNER"));
        assertTrue(headers.contains("ACLID"));
        assertTrue(headers.contains("content@s__size@{http://www.alfresco.org/model/content/1.0}content"));

        //Change the field list
        req = areq(params( "q", "OWNER:"+testUser, "qt", "/afts",
                "wt", "csv", "fl", "id,ACLID"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        response = queryRequest(req);
        data = new CSVParser(new StringReader(response)).getAllValues();
        assertTrue("There should be 4 rows, 1 header and 3 rows of data", data.length == 4);
        headers = Arrays.asList(data[0]);
        assertFalse(headers.contains("DBID"));
        assertTrue(headers.contains("id"));
        assertFalse(headers.contains("OWNER"));
        assertTrue(headers.contains("ACLID"));

        req = areq(params( "q", "OWNER:"+testUser, "qt", "/afts",
                "wt", "csv", "fl", "id,content@s__size@{http://www.alfresco.org/model/content/1.0}content,cm:name"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        response = queryRequest(req);
        data = new CSVParser(new StringReader(response)).getAllValues();
        assertTrue("There should be 4 rows, 1 header and 3 rows of data", data.length == 4);
        headers = Arrays.asList(data[0]);
        assertFalse(headers.contains("DBID"));
        assertTrue(headers.contains("id"));
        assertFalse(headers.contains("OWNER"));
        assertTrue(headers.contains("cm:name"));



    }

    private String queryRequest(SolrServletRequest req) {

        try {
            String response = h.query(req);
            System.out.println(response);
            System.out.println("");
            logger.debug("Query response is "+ response);
            return response;
        } catch (Exception e) {
            throw new RuntimeException("Exception during query", e);
        }
    }

}
