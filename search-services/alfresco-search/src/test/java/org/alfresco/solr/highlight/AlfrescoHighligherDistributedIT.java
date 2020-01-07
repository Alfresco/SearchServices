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
import org.junit.Test;

import java.util.List;

@SolrTestCaseJ4.SuppressSSL
public class AlfrescoHighligherDistributedIT extends AbstractAlfrescoDistributedIT
{
    @BeforeClass
    public static void initData() throws Throwable
    {
        initSolrServers(2, "DistributedAlfrescoSolrFacetingIT", DEFAULT_CORE_PROPS);
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }

    public void makeSureHighlightingIsProperlyConfigured()
    {
        SolrHighlighter highlighter = HighlightComponent.getHighlighter(defaultCore);
        assertTrue(
                "Wrong highlighter: " + highlighter.getClass(),
                highlighter instanceof AlfrescoSolrHighlighter);
    }

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
                        HighlightParams.FIELDS, "",
                        HighlightParams.SNIPPETS, String.valueOf(4),
                        HighlightParams.FRAGSIZE, String.valueOf(40)));

        assertTrue(response.getResults().getNumFound() > 0);
    }
}
