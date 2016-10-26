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

import org.alfresco.solr.AbstractAlfrescoDistributedTest;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.AlfrescoSolrHighlighter;
import org.apache.solr.handler.component.HighlightComponent;
import org.apache.solr.highlight.SolrHighlighter;
import org.junit.Rule;
import org.junit.Test;

/**
 * Tests Alfresco-specific logic.
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AlfrescoHighligherDistributedTest extends AbstractAlfrescoDistributedTest
{
    private static Log logger = LogFactory.getLog(AlfrescoHighligherDistributedTest.class);

    @Rule
    public DefaultAlrescoCoreRule jetty = new DefaultAlrescoCoreRule(this.getClass().getSimpleName(), DEFAULT_CORE_PROPS);

    @Test
    public void testHighlight() throws Exception {

        logger.info("######### Starting highlighter test ###########");
        SolrCore defaultCore = jetty.getDefaultCore();
        SolrHighlighter highlighter = HighlightComponent.getHighlighter(defaultCore);
        assertTrue("wrong highlighter: " + highlighter.getClass(), highlighter instanceof AlfrescoSolrHighlighter);
/**
        AclChangeSet aclChangeSet = getAclChangeSet(1);
        Acl acl = getAcl(aclChangeSet);

        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("mike"), list("mike"), null);

        indexAclChangeSet(aclChangeSet, list(acl), list(aclReaders));

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

        String LONG_TEXT = "this is some long text.  It has the word long in many places.  In fact, it has long on some different fragments.  " +
                "Let us see what happens to long in this case.";

        List<String> content = Arrays.asList(LONG_TEXT, LONG_TEXT);

        //Index the transaction, nodes, and nodeMetaDatas.
        indexTransaction(foldertxn, list(folderNode), list(folderMetaData));
        indexTransaction(txn,
                list(fileNode, fileNode2),
                list(fileMetaData, fileMetaData2),
                content);
        logger.info("######### Waiting for Doc Count ###########");
        waitForDocCount(new TermQuery(new Term(QueryConstants.FIELD_OWNER, "mike")), 3, 80000);
        waitForDocCount(new TermQuery(new Term(ContentModel.PROP_TITLE.toString(), "title1")), 1, 500);
        waitForDocCount(new TermQuery(new Term(ContentModel.PROP_DESCRIPTION.toString(), "mydesc")), 1, 500);

        //name, title, description, content
        //up to 3 matches in the content  (needs to be big enough)
        QueryResponse response = query(jetty.getDefaultClient(), false,
                "{\"locales\":[\"en\"], \"authorities\": [\"mike\"], \"tenants\": [ \"\" ]}",
                params( "q", ContentModel.PROP_NAME.toString()+":some very long name",
                        "qt", "/afts", "start", "0", "rows", "5",
                        HighlightParams.HIGHLIGHT, "true",
                        HighlightParams.FIELDS, "",
                        HighlightParams.SNIPPETS, String.valueOf(4),
                        HighlightParams.FRAGSIZE, String.valueOf(40)));

        assertTrue(response.getResults().getNumFound() > 0);

 **/
    }

}
