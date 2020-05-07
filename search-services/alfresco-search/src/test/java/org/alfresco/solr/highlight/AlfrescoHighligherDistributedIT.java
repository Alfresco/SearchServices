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

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.ancestors;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.handler.component.AlfrescoSolrHighlighter;
import org.apache.solr.handler.component.HighlightComponent;
import org.apache.solr.highlight.SolrHighlighter;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

/**
 * This Highlighter Integration test is marked has been marked as ignored.
 * While working on the SolrContentStore removal epic (SEARCH-1687) and specifically one of its subtasks (SEARCH-1693)
 * we found this test completely commented. After refactoring the Alfresco Highlighter we tried to re-enable the test method
 * and we found a major incompatibility between the response returned from Solr and the Solrj "javabin" writer used
 * for interacting with Solr.
 *
 * The default Solr highlighting section appears like this (using the XML response writer):
 *
 * <pre>
 *  &lt;lst name="_DEFAULT_!8000016f66a1a298!8000016f66a1a29e">
 *      &lt;arr name="name">
 *          &lt;str>some very &lt;em&gt;long&lt;/em&gt; name&lt;/str>
 *      &lt;/arr>
 *      &lt;arr name="title">
 *          &lt;str>This the &lt;em&gt;long&lt;/em&gt; french version of of the&lt;/str>
 *          &lt;str>This the &lt;em&gt;long&lt;/em&gt; english version of the&lt;/str>
 *      &lt;/arr>
 *  &lt;/lst>
 *  ... (other highlighting snippets belonging to different documents)
 * </pre>
 *
 * As you can see, for each document, there's a "lst" section where the name attribute contains the Solr document ID.
 * Within that section, we have one array for each attribute ("name" and "title" in the example above) which contains
 * the highlighting snippets.
 *
 * Following that structure, the "javabin" (default) response writer of Solrj assumes there can be only array members
 * within each document highlight section and a consequence of that it casts the NamedList entry value as a List (the
 * following is an extract from QueryResponse.java):
 *
 * <pre>
 *   private void extractHighlightingInfo( NamedList<Object> info )
 *   {
 *     _highlighting = new HashMap<>();
 *     for( Map.Entry<String, Object> doc : info ) {
 *       Map<String,List<String>> fieldMap = new HashMap<>();
 *       _highlighting.put( doc.getKey(), fieldMap );
 *
 *       NamedList<List<String>> fnl = (NamedList<List<String>>)doc.getValue();
 *       for( Map.Entry<String, List<String>> field : fnl ) {
 *         fieldMap.put( field.getKey(), field.getValue() );
 *       }
 *     }
 *   }
 * </pre>
 *
 * The AlfrescoHighlighter adds within that section an additional element which consists of the document DBID.
 *
 * <pre>
 *  &lt;lst name="_DEFAULT_!8000016f66a1a298!8000016f66a1a29e">
 *      &lt;str name="DBID">1577974866590&lt;/str>
 *      &lt;arr name="name">
 *          &lt;str>some very &lt;em&gt;long&lt;/em&gt; name&lt;/str>
 *      &lt;/arr>
 *      &lt;arr name="title">
 *          &lt;str>This the &lt;em&gt;long&lt;/em&gt; french version of of the&lt;/str>
 *          &lt;str>This the &lt;em&gt;long&lt;/em&gt; english version of the&lt;/str>
 *      &lt;/arr>
 *  &lt;/lst>
 * </pre>
 *
 * Unfortunately that additional element is not an array so at the end, if we query Solr
 *
 * <ul>
 *     <li>using Solrj</li>
 *     <li>with highlighting enabled</li>
 *     <li>with "javabin" response writer (which is the default)</li>
 * </ul>
 *
 * a ClassCastException is thrown:
 *
 * <pre>
 *  java.lang.ClassCastException: class java.lang.String cannot be cast to class java.util.List
 * </pre>
 *
 * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-2033">SEARCH-2033</a>
 */
@Ignore
@SolrTestCaseJ4.SuppressSSL
public class AlfrescoHighligherDistributedIT extends AbstractAlfrescoDistributedIT
{
    //@BeforeClass
//    public static void initData() throws Throwable
//    {
//        initSolrServers(2, AlfrescoHighligherDistributedIT.class.getSimpleName(), DEFAULT_CORE_PROPS);
//    }

    //@AfterClass
//    public static void destroyData()
//    {
//        dismissSolrServers();
//    }

//    public void makeSureHighlightingIsProperlyConfigured()
//    {
//        SolrHighlighter highlighter = HighlightComponent.getHighlighter(defaultCore);
//        assertTrue(
//                "Wrong highlighter: " + highlighter.getClass(),
//                highlighter instanceof AlfrescoSolrHighlighter);
//    }

    @Ignore
    @Test
    public void testHighlight() throws Exception
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);
        Acl acl = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("mike"), singletonList("mike"), null);

        indexAclChangeSet(aclChangeSet, singletonList(acl), singletonList(aclReaders));

        //First create a transaction.
        Transaction foldertxn = getTransaction(0, 1);
        Transaction txn = getTransaction(0, 2);

        //Next create two nodes to update for the transaction
        Node folderNode = getNode(foldertxn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        Node fileNode2 = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);

        NodeMetaData folderMetaData = getNodeMetaData(folderNode, foldertxn, acl, "mike", null, false);
        NodeMetaData fileMetaData   = getNodeMetaData(fileNode,  txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);
        NodeMetaData fileMetaData2  = getNodeMetaData(fileNode2, txn, acl, "mike", ancestors(folderMetaData.getNodeRef()), false);

        fileMetaData.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("some very long name"));
        fileMetaData.getProperties().put(ContentModel.PROP_TITLE, new StringPropertyValue("title1"));
        fileMetaData.getProperties().put(ContentModel.PROP_DESCRIPTION, new StringPropertyValue("mydesc"));

        fileMetaData2.getProperties().put(ContentModel.PROP_NAME, new StringPropertyValue("some name"));
        fileMetaData2.getProperties().put(ContentModel.PROP_TITLE, new StringPropertyValue("title2"));

        String LONG_TEXT = "this is some long text.  " +
                "It has the word long in many places.  " +
                "In fact, it has long on some different fragments.  " +
                "Let us see what happens to long in this case.";

        List<String> content = asList(LONG_TEXT, LONG_TEXT);

        indexTransaction(foldertxn, singletonList(folderNode), singletonList(folderMetaData));
        indexTransaction(txn,
                asList(fileNode, fileNode2),
                asList(fileMetaData, fileMetaData2),
                content);

        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_OWNER, "mike")), 3, 80000);

        //name, title, description, content
        //up to 3 matches in the content  (needs to be big enough)
        QueryResponse response = query(getDefaultTestClient(), true,
                "{\"locales\":[\"en\"], \"authorities\": [\"mike\"], \"tenants\": [ \"\" ]}",
                params( "q", "name:some very long name",
                        "qt", "/afts", "start", "0", "rows", "5",
                        HighlightParams.HIGHLIGHT, "true",
                        HighlightParams.FIELDS, "name",
                        HighlightParams.SNIPPETS, String.valueOf(4),
                        HighlightParams.FRAGSIZE, String.valueOf(40)));

        assertTrue(response.getResults().getNumFound() > 0);
    }
}
