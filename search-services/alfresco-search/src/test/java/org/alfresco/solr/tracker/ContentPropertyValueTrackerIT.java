/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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
import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclReaders;
import org.alfresco.solr.client.ContentPropertyValue;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

import static org.alfresco.solr.AlfrescoSolrUtils.MAX_WAIT_TIME;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.list;

/**
 * @author Elia Porciani
 */
@SolrTestCaseJ4.SuppressSSL
public class ContentPropertyValueTrackerIT extends AbstractAlfrescoDistributedIT
{
    final private String authorField = "text@s__lt@{http://www.alfresco.org/model/content/1.0}author";

    @BeforeClass
    public static void initData() throws Throwable
    {
        initSolrServers(1, ContentPropertyValueTrackerIT.class.getSimpleName(), null);
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }

    /**
     *
     * MNT-21076 (SEARCH-1906)
     *
     * This test aims to test that a document with more than one (not indexed) ContentPropertyValue
     * is not removed from the index after any update. The fix prevents an NPE during an update because the
     * not indexed ContentPropertyValue is searched and not found in the cached document.
     *
     * The second ContentPropertyValue must not be indexed.
     */
    @Test
    public void testDataWithMultipleContentValueIsIndexedAfterUpdateTest() throws Exception
    {
        putHandleDefaults();
        AclChangeSet aclChangeSet = getAclChangeSet(1, 1);
        Acl acl = getAcl(aclChangeSet);

        // Arbitrary acl data.
        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, list("joel"), list("phil"), null);
        indexAclChangeSet(aclChangeSet,
                list(acl),
                list(aclReaders));

        Transaction txn = getTransaction(0, 1);
        Node fileNode = getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED);
        NodeMetaData fileMetaData   = getNodeMetaData(fileNode, txn, acl, "mike", null, false);
        String author = "Mario";

        // Here we set the PROP_TITLE as a ContentPropertyValue.
        // The value is not indexed because the related indexedField is not found (it does not exist).
        fileMetaData.getProperties()
                .put(ContentModel.PROP_TITLE, new ContentPropertyValue(Locale.CANADA, 100, "UTF8", "txt", 10l));
        fileMetaData.getProperties().put(ContentModel.PROP_AUTHOR, new StringPropertyValue(author));
        indexTransaction(txn,
                list(fileNode),
                list(fileMetaData));

        // Check the document is correctly indexed
        waitForDocCount(new TermQuery(new Term(authorField, author)), 1, MAX_WAIT_TIME);

        // Update the author.
        Transaction txn1 = getTransaction(0, 1);
        String authorAfterUpdate = "Luigi";
        fileMetaData.getProperties().put(ContentModel.PROP_AUTHOR, new StringPropertyValue(authorAfterUpdate));
        indexTransaction(txn1,
                list(fileNode),
                list(fileMetaData));

        // Check the document is still indexed after the update.
        waitForDocCount(new TermQuery(new Term(authorField, authorAfterUpdate)), 1, MAX_WAIT_TIME);
    }
}