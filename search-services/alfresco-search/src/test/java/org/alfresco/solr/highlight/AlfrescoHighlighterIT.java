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

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;

import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.*;

import java.util.Map;
import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AbstractAlfrescoSolrIT;
import org.alfresco.solr.client.*;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.LegacyNumericRangeQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.HighlightParams;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

@SolrTestCaseJ4.SuppressSSL
public class AlfrescoHighlighterIT extends AbstractAlfrescoSolrIT
{
    private static Log logger = LogFactory.getLog(AlfrescoHighlighterIT.class);
    private static long MAX_WAIT_TIME = 80000;

    private final static String NAME_METADATA_ATTRIBUTE = "name";
    private final static String TITLE_METADATA_ATTRIBUTE = "title";
    private final static String DESCRIPTION_METADATA_ATTRIBUTE = "description";

    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");

        String long_text = "this is some long text.  It has the word long in many places.  " +
                "In fact, it has long on some different fragments.  " +
                "Let us see what happens to long in this case.";

        List<Map<String, String>> data = asList(
                of(NAME_METADATA_ATTRIBUTE, "some very long name",
                        DESCRIPTION_METADATA_ATTRIBUTE, "mydesc",
                        TITLE_METADATA_ATTRIBUTE, "title1 is very long"),
                of(NAME_METADATA_ATTRIBUTE, long_text,
                        TITLE_METADATA_ATTRIBUTE, "title2"),
                of(NAME_METADATA_ATTRIBUTE, "MixedCabbageString and plurals and discussion",
                        TITLE_METADATA_ATTRIBUTE, "title2"));

        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        Acl acl2 = getAcl(aclChangeSet);
        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        AclReaders aclReaders2 = getAclReaders(aclChangeSet, acl2, singletonList("jim"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet,
                asList(acl, acl2),
                asList(aclReaders, aclReaders2));


        //Check for the ACL state stamp.
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        builder.add(new BooleanClause(new TermQuery(new Term(QueryConstants.FIELD_SOLR4_ID, "TRACKER!STATE!ACLTX")), BooleanClause.Occur.MUST));
        builder.add(new BooleanClause(LegacyNumericRangeQuery.newLongRange(QueryConstants.FIELD_S_ACLTXID, aclChangeSet.getId(), aclChangeSet.getId() + 1, true, false), BooleanClause.Occur.MUST));
        BooleanQuery waitForQuery = builder.build();
        waitForDocCount(waitForQuery, 1, MAX_WAIT_TIME);

        String owner = "mike";

        //First create a transaction.
        Transaction foldertxn = getTransaction(0, 1);
        Transaction txn = getTransaction(0, 2);

        // Create folder
        Node folderNode = getNode(foldertxn, acl, Node.SolrApiNodeStatus.UPDATED);
        NodeMetaData folderMetaData = getNodeMetaData(folderNode, foldertxn, acl, owner, null, false);

        List<Node> nodeList = new ArrayList<>();
        List<NodeMetaData> metadataList = new ArrayList<>();

        data.forEach(entry ->
        {
            Node node = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
            nodeList.add(node);
            NodeMetaData fileMetaData = getNodeMetaData(node, txn, acl, owner, ancestors(folderMetaData.getNodeRef()), false);
            Map<QName, PropertyValue> properties = fileMetaData.getProperties();
            properties.put(ContentModel.PROP_NAME, new StringPropertyValue(entry.get(NAME_METADATA_ATTRIBUTE)));

            HashMap<Locale, String> titleProp = new HashMap<>();
            titleProp.put(Locale.ENGLISH, entry.get(TITLE_METADATA_ATTRIBUTE));

            properties.put(ContentModel.PROP_TITLE, new MLTextPropertyValue(titleProp));

            String description = entry.get(DESCRIPTION_METADATA_ATTRIBUTE);
            if (description != null) {
                HashMap<Locale, String> descProp = new HashMap<>();
                descProp.put(Locale.ENGLISH, description);
                properties.put(ContentModel.PROP_DESCRIPTION, new MLTextPropertyValue(descProp));
            }

            metadataList.add(fileMetaData);
        });

        indexTransaction(foldertxn, singletonList(folderNode), singletonList(folderMetaData));
        indexTransaction(txn, nodeList, metadataList);

        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_READER, "jim")), 1, MAX_WAIT_TIME);
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_OWNER, owner)), 4, MAX_WAIT_TIME);
    }

    @AfterClass
    public static void clearQueue()
    {
        SOLRAPIQueueClient.nodeMetaDataMap.clear();
        SOLRAPIQueueClient.transactionQueue.clear();
        SOLRAPIQueueClient.aclChangeSetQueue.clear();
        SOLRAPIQueueClient.aclReadersMap.clear();
        SOLRAPIQueueClient.aclMap.clear();
        SOLRAPIQueueClient.nodeMap.clear();
        SOLRAPIQueueClient.nodeContentMap.clear();
    }

    @Test
    public void highlightingSnippetsFragSizeTest()
    {
        SolrServletRequest req = areq(params("q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "long",
                HighlightParams.FIELDS, "content,name,title",
                HighlightParams.SNIPPETS, "4",
                HighlightParams.FRAGSIZE, "40"),
                "{" +
                        "\"locales\":[\"en\"], " +
                        "\"tenants\": [ \"\" ]" +
                        "}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=2]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='some very <em>long</em> name']",
                "//lst[@name='highlighting']/lst[1]/arr[@name='title']/str[.='title1 is very <em>long</em>']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.='this is some <em>long</em> text.  It has the']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.=' word <em>long</em> in many places.  In fact, it has']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.=' <em>long</em> on some different fragments.  Let us']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.=' see what happens to <em>long</em> in this case.']");
    }

    @Test
    public void highlightingPhraseQueriesTest()
    {
        //Phrase hightling is on by default
        SolrServletRequest req = areq(params("q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "\"some long\"",
                HighlightParams.FIELDS, NAME_METADATA_ATTRIBUTE,
                HighlightParams.SIMPLE_PRE, "(",
                HighlightParams.SIMPLE_POST, ")",
                HighlightParams.SNIPPETS, String.valueOf(1),
                HighlightParams.FRAGSIZE, String.valueOf(100)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "//lst[@name='highlighting']/lst/arr/str[.='this is (some) (long) text.  It has the word long in many places.  In fact, it has long on some']");

        req = areq(params("q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "\"some long\"",
                HighlightParams.FIELDS, NAME_METADATA_ATTRIBUTE,
                HighlightParams.USE_PHRASE_HIGHLIGHTER, "false",
                HighlightParams.SIMPLE_PRE, "(",
                HighlightParams.SIMPLE_POST, ")",
                HighlightParams.SNIPPETS, String.valueOf(1),
                HighlightParams.FRAGSIZE, String.valueOf(100)),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "//lst[@name='highlighting']/lst/arr/str[.='(some) very (long) name']",
                "//lst[@name='highlighting']/lst/arr/str[.='this is (some) (long) text.  It has the word (long) in many places.  In fact, it has (long) on (some)']");
    }

    @Test
    public void highlightingMaxAnalyzedCharsTest()
    {
        SolrServletRequest req = areq(params("q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
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
    }

    @Test
    public void highlightingMergeContinuousFragmentsTest()
    {
        SolrServletRequest req = areq(params("q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "'some long'",
                HighlightParams.FIELDS, NAME_METADATA_ATTRIBUTE,
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
    }

    @Test
    public void highlightingLocalConfigurationsTest()
    {
        SolrServletRequest req = areq(params("q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.Q, "long",
                HighlightParams.FIELDS, "name,title",
                "f.title." + HighlightParams.SIMPLE_PRE, "(",
                "f.title." + HighlightParams.SIMPLE_POST, ")",
                "f.name." + HighlightParams.SIMPLE_PRE, "[",
                "f.name." + HighlightParams.SIMPLE_POST, "]",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst/arr[@name='name'])=2]",
                "*[count(//lst[@name='highlighting']/lst/str[@name='DBID'])=2]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='some very [long] name']",
                "//lst[@name='highlighting']/lst[1]/arr[@name='title']/str[.='title1 is very (long)']",
                "//lst[@name='highlighting']/lst[2]/arr[@name='name']/str[.='this is some [long] text.  It has the word [long] in many places.  In fact, it has [long] on some']");
    }

    @Test
    public void highlightingRequiredFieldsTest()
    {
        SolrServletRequest req = areq(params("q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
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

        req = areq(params("q", "name:long OR title:long", "qt", "/afts", "start", "0", "rows", "5",
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
    }

    @Test
    public void highlightingPrePostTest()
    {
        SolrServletRequest req = areq(params("q", "name:long", "qt", "/afts", "start", "0", "rows", "5",
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
    }

    @Test
    public void highlightingCamelCaseTest()
    {
        SolrServletRequest req = areq(params("q", "name:cabbage", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                //HighlightParams.Q, "lon*",
                HighlightParams.FIELDS, NAME_METADATA_ATTRIBUTE,
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
    }

    @Test
    public void highlightingPluralsTest()
    {
        SolrServletRequest req = areq(params("q", "name:plural", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                //HighlightParams.Q, "lon*",
                HighlightParams.FIELDS, NAME_METADATA_ATTRIBUTE,
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
    }

    @Test
    public void highlightingStemmingTest()
    {
        SolrServletRequest req = areq(params("q", "name:discuss", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                //HighlightParams.Q, "lon*",
                HighlightParams.FIELDS, NAME_METADATA_ATTRIBUTE,
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

    @Test
    public void highlightingBooleanConjunctionTest()
    {
        SolrServletRequest req = areq(params("q", "title:(is AND long)", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.FIELDS, TITLE_METADATA_ATTRIBUTE,
                HighlightParams.HIGHLIGHT_MULTI_TERM, "false",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=1]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='title'])=1]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='title']/str[.='title1 {is} very {long}']"
        );
    }


    @Test
    public void highlightingBooleanConjunctionGenericTextTest()
    {
        SolrServletRequest req = areq(params("q", "(very AND name)", "qt", "/afts", "start", "0", "rows", "5",
                HighlightParams.HIGHLIGHT, "true",
                HighlightParams.FIELDS, NAME_METADATA_ATTRIBUTE,
                HighlightParams.HIGHLIGHT_MULTI_TERM, "false",
                HighlightParams.SIMPLE_PRE, "{",
                HighlightParams.SIMPLE_POST, "}"),
                "{\"locales\":[\"en\"], \"tenants\": [ \"\" ]}");

        assertQ(req,
                "*[count(//lst[@name='highlighting']/lst)=1]",
                "*[count(//lst[@name='highlighting']/lst/arr[@name='name'])=1]",
                "//lst[@name='highlighting']/lst[1]/arr[@name='name']/str[.='some {very} long {name}']"
        );
    }
}