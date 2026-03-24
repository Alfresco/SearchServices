/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
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
 * #L%
 */

package org.alfresco.solr;

import static java.util.Collections.singletonList;
import static org.alfresco.solr.AlfrescoSolrUtils.ancestors;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;
import static org.carrot2.shaded.guava.common.collect.ImmutableList.of;

import java.lang.reflect.Field;
import java.util.HashSet;

import org.alfresco.service.namespace.QName;
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
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Verifies that the alfresco.tokenise.property.* override forces
 * tokenised=TRUE at index time, regardless of the model's own tokenisation setting.
 */
@SolrTestCaseJ4.SuppressSSL
public class TokenisationOverrideIT extends AbstractAlfrescoDistributedIT
{
    private static final QName PROP = QName.createQName("custom", "untokenisedProp");
    private static final QName PROP_OVERRIDE = QName.createQName("custom", "untokenisedPropOverride");
    private static final QName ASPECT = QName.createQName("custom", "tokenisationOverrideTestAspect");

    private static final String TOKENISED_FIELD_OVERRIDE = "text@s__lt@{custom}untokenisedPropOverride";
    private static final String TOKENISED_FIELD_BASE     = "text@s__lt@{custom}untokenisedProp";

    static final String PROP_VALUE = "The sky is very blue";
    private static final int TIMEOUT = 100000;

    private static HashSet<QName> savedTokeniseProperties;

    @BeforeClass
    public static void initData() throws Throwable
    {
        initSolrServers(1, TokenisationOverrideIT.class.getSimpleName(), null);
        modifyAlfrescoSolrDataModel();
        indexNodes();
    }

    private static void modifyAlfrescoSolrDataModel() throws NoSuchFieldException, IllegalAccessException
    {
        Field tokenisePropertiesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("tokeniseProperties");

        tokenisePropertiesField.setAccessible(true);

        HashSet<QName> tokeniseProperties = (HashSet<QName>) tokenisePropertiesField.get(AlfrescoSolrDataModel.getInstance());
        savedTokeniseProperties = (HashSet<QName>) tokeniseProperties.clone();
        tokeniseProperties.add(PROP_OVERRIDE);
    }

    private static void indexNodes() throws Exception
    {
        AclChangeSet aclChangeSet = getAclChangeSet(1);

        Acl acl = getAcl(aclChangeSet);
        AclReaders aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);

        indexAclChangeSet(aclChangeSet,
                of(acl),
                of(aclReaders));

        Transaction txn = getTransaction(0, 2);

        Node parentFolder = getNode(0, txn, acl, Node.SolrApiNodeStatus.UPDATED);
        NodeMetaData parentFolderMetadata = getNodeMetaData(parentFolder, txn, acl, "joel", null, false);

        Node testNode = getNode(1, txn, acl, Node.SolrApiNodeStatus.UPDATED);
        NodeMetaData testNodeMetadata = getNodeMetaData(testNode, txn, acl, "joel",
                ancestors(parentFolderMetadata.getNodeRef()), false);

        testNodeMetadata.getAspects().add(ASPECT);
        testNodeMetadata.getProperties().put(PROP, new StringPropertyValue(PROP_VALUE));
        testNodeMetadata.getProperties().put(PROP_OVERRIDE, new StringPropertyValue(PROP_VALUE));

        indexTransaction(txn,
                of(parentFolder, testNode),
                of(parentFolderMetadata, testNodeMetadata));
    }

    @AfterClass
    public static void destroyData() throws NoSuchFieldException, IllegalAccessException
    {
        restoreAlfrescoSolrDataModel();
        dismissSolrServers();
    }

    private static void restoreAlfrescoSolrDataModel() throws NoSuchFieldException, IllegalAccessException
    {
        Field tokenisePropertiesField = AlfrescoSolrDataModel
                .getInstance()
                .getClass()
                .getDeclaredField("tokeniseProperties");

        tokenisePropertiesField.setAccessible(true);
        tokenisePropertiesField.set(AlfrescoSolrDataModel.getInstance(), savedTokeniseProperties);
    }

    @Test
    public void tokenisationOverride_overriddenPropertyIsTokenised_basePropertyIsNot() throws Exception
    {
        waitForDocCount(new TermQuery(new Term(TOKENISED_FIELD_OVERRIDE, "blue")), 1, TIMEOUT);
        waitForDocCount(new TermQuery(new Term(TOKENISED_FIELD_BASE, "blue")), 0, TIMEOUT);
    }
}
