/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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

package org.alfresco.solr.highlight;

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
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.response.BasicResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;

import static junit.framework.TestCase.assertTrue;
import static org.alfresco.solr.AlfrescoSolrUtils.*;
import static org.apache.solr.SolrJettyTestBase.jetty;
import static org.junit.Assert.assertNotNull;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AlfrescoHighlighterTest extends AbstractAlfrescoSolrTests
{
    private static Log logger = LogFactory.getLog(AlfrescoHighlighterTest.class);
    private static long MAX_WAIT_TIME = 80000;

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");
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
    public void testHighlighting() throws Exception
    {
        /*
        * Create and index an AclChangeSet.
        */

        logger.info("######### Starting Highlight test ###########");
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
        Node fileNode3 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

        NodeMetaData folderMetaData = getNodeMetaData(folderNode, foldertxn, acl, "mike", null, false);
        NodeMetaData fileMetaData   = getNodeMetaData(fileNode,  txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
        NodeMetaData fileMetaData2  = getNodeMetaData(fileNode2, txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
        NodeMetaData fileMetaData3  = getNodeMetaData(fileNode3, txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);

        String LONG_TEXT = "this is some long text.  It has the word long in many places.  In fact, it has long on some different fragments.  " +
                "Let us see what happens to long in this case.";

        fileMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("some very long name"));
        HashMap<Locale, String> title = new HashMap<Locale, String> ();
        title.put(Locale.ENGLISH, "title1 is very long");
        fileMetaData.getProperties().put(ContentModel.PROP_TITLE, new  MLTextPropertyValue(title));
        HashMap<Locale, String> desc = new HashMap<Locale, String> ();
        desc.put(Locale.ENGLISH, "mydesc");
        fileMetaData.getProperties().put(ContentModel.PROP_DESCRIPTION, new  MLTextPropertyValue(desc));

        fileMetaData2.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue(LONG_TEXT));
        HashMap<Locale, String> title2 = new HashMap<Locale, String> ();
        title2.put(Locale.ENGLISH, "title2");
        fileMetaData2.getProperties().put(ContentModel.PROP_TITLE,  new  MLTextPropertyValue(title2));
        
        fileMetaData3.getProperties().put(ContentModel.PROP_NAME,  new StringPropertyValue("MixedCabbageString and plurals and discussion"));
        fileMetaData3.getProperties().put(ContentModel.PROP_TITLE,  new  MLTextPropertyValue(title2));

       // List<String> content = Arrays.asList(LONG_TEXT, LONG_TEXT);

        //Index the transaction, nodes, and nodeMetaDatas.
        indexTransaction(foldertxn, list(folderNode), list(folderMetaData));
        indexTransaction(txn,
                list(fileNode, fileNode2, fileNode3),
                list(fileMetaData, fileMetaData2, fileMetaData3));
        logger.info("######### Waiting for Doc Count ###########");

        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_OWNER, "mike")), 4, 10000);

        logger.info("######### Testing SNIPPETS / FRAGSIZE ###########");
        SolrServletRequest req = areq(params( "q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "long",
                HighlightParams.FIELDS, "content,name,title",
                HighlightParams.SNIPPETS, String.valueOf(4),
                HighlightParams.FRAGSIZE, String.valueOf(40)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=2]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='some very <em>long</em> name']",
                "//lst[@name='highlighting']/lst[1]/arr[@name='title']/str[.='title1 is very <em>long</em>']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.='this is some <em>long</em> text.  It has the']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.=' word <em>long</em> in many places.  In fact, it has']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.=' <em>long</em> on some different fragments.  Let us']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.=' see what happens to <em>long</em> in this case.']");

        logger.info("######### Testing PRE / POST ###########");
        req = areq(params( "q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "long",
                HighlightParams.FIELDS, "content,name,title",
                HighlightParams.SIMPLE_PRE, "<al>",
                HighlightParams.SIMPLE_POST, "<fresco>",
                HighlightParams.SNIPPETS, String.valueOf(4),
                HighlightParams.FRAGSIZE, String.valueOf(40)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=2]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='some very <al>long<fresco> name']",
                "//lst[@name='highlighting']/lst[1]/arr[@name='title']/str[.='title1 is very <al>long<fresco>']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.='this is some <al>long<fresco> text.  It has the']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.=' word <al>long<fresco> in many places.  In fact, it has']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.=' <al>long<fresco> on some different fragments.  Let us']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.=' see what happens to <al>long<fresco> in this case.']");

        logger.info("######### Testing PHRASE QUERIES ###########");

        //Phrase hightling is on by default
        req = areq(params( "q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "\"some long\"",
                HighlightParams.FIELDS, "name",
                HighlightParams.SIMPLE_PRE, "(",
                HighlightParams.SIMPLE_POST, ")",
                HighlightParams.SNIPPETS, String.valueOf(1),
                HighlightParams.FRAGSIZE, String.valueOf(100)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "//lst[@name='highlighting']/lst/arr/str[.='this is (some) (long) text.  It has the word long in many places.  In fact, it has long on some']");

        req = areq(params( "q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "\"some long\"",
                HighlightParams.FIELDS, "name",
                HighlightParams.USE_PHRASE_HIGHLIGHTER, "false",
                HighlightParams.SIMPLE_PRE, "(",
                HighlightParams.SIMPLE_POST, ")",
                HighlightParams.SNIPPETS, String.valueOf(1),
                HighlightParams.FRAGSIZE, String.valueOf(100)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "//lst[@name='highlighting']/lst/arr/str[.='(some) very (long) name']",
                "//lst[@name='highlighting']/lst/arr/str[.='this is (some) (long) text.  It has the word (long) in many places.  In fact, it has (long) on (some)']");

        logger.info("######### MergeContiguous ###########");

        req = areq(params( "q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "'some long'",
                HighlightParams.FIELDS, "name",
                HighlightParams.MERGE_CONTIGUOUS_FRAGMENTS, "true",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}",
                HighlightParams.SNIPPETS, String.valueOf(4),
                HighlightParams.FRAGSIZE, String.valueOf(40)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst/arr)=1]",
                "//lst[@name='highlighting']/lst/arr[@name='name']/str[.='this is {some} {long} text.  It has the word long in many places.  In fact, it has long on some different fragments.  Let us see what happens to long in this case.']"
        );

        logger.info("######### maxAnalyzedChars ###########");

        req = areq(params( "q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "long",
                HighlightParams.FIELDS, "name,title",
                HighlightParams.MAX_CHARS, "18",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=2]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='name'])=2]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='some very {long} name']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.='this is some {long}']");

        logger.info("######### testLocal ###########");

        req = areq(params( "q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "long",
                HighlightParams.FIELDS, "name,title",
                "f.title."+HighlightParams.SIMPLE_PRE, "{",
                "f.title."+HighlightParams.SIMPLE_POST, "}",
                "f.name."+HighlightParams.SIMPLE_PRE, "[",
                "f.name."+HighlightParams.SIMPLE_POST, "]",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");
        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst/arr[@name='name'])=2]",
                "*[count(//lst[@name='highlighting']/lst/str[@name='DBID'])=2]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='some very [long] name']",
                "//lst[@name='highlighting']/lst[1]/arr[@name='title']/str[.='title1 is very {long}']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.='this is some [long] text.  It has the word [long] in many places.  In fact, it has [long] on some']");
        
        logger.info("######### requireFieldMatch ###########");

        req = areq(params( "q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "long",
                HighlightParams.FIELDS, "name,title",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=2]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='title'])=1]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='title']/str[.='title1 is very {long}']");
        //add name

        req = areq(params( "q", "name:long OR title:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "title:long",
                HighlightParams.FIELDS, "name,title",
                HighlightParams.FIELD_MATCH, "true",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=2]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='title'])=1]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='name'])=0]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='title']/str[.='title1 is very {long}']");


        logger.info("######### MultiTerm ###########");

        req = areq(params( "q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "lon*",
                HighlightParams.FIELDS, "name",
                HighlightParams.HIGHLIGHT_MULTI_TERM, "false",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}",
                HighlightParams.SNIPPETS, String.valueOf(1),
                HighlightParams.FRAGSIZE, String.valueOf(100)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=2]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='title'])=0]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='name'])=0]");
        
   
        logger.info("######### CamelCase ###########");

        req = areq(params( "q", "name:cabbage", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                //HighlightParams.Q, "lon*",
                HighlightParams.FIELDS, "name",
                HighlightParams.HIGHLIGHT_MULTI_TERM, "false",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}",
                HighlightParams.SNIPPETS, String.valueOf(1),
                HighlightParams.FRAGSIZE, String.valueOf(100)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=1]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='name'])=1]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='Mixed{Cabbage}String and plurals and discussion']"
        		);
        
        logger.info("######### Plurals ###########");

        req = areq(params( "q", "name:plural", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                //HighlightParams.Q, "lon*",
                HighlightParams.FIELDS, "name",
                HighlightParams.HIGHLIGHT_MULTI_TERM, "false",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}",
                HighlightParams.SNIPPETS, String.valueOf(1),
                HighlightParams.FRAGSIZE, String.valueOf(100)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=1]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='name'])=1]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='MixedCabbageString and {plurals} and discussion']"
        		);
        
        logger.info("######### stemming ###########");

        req = areq(params( "q", "name:discuss", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                //HighlightParams.Q, "lon*",
                HighlightParams.FIELDS, "name",
                HighlightParams.HIGHLIGHT_MULTI_TERM, "false",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}",
                HighlightParams.SNIPPETS, String.valueOf(1),
                HighlightParams.FRAGSIZE, String.valueOf(100)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=1]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='name'])=1]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='MixedCabbageString and plurals and {discussion}']"
        		);
        
    }

}
