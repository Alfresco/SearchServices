/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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

package org.alfresco.solr.tracker;

import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.IntStream.range;
import static org.alfresco.solr.AlfrescoSolrUtils.MAX_WAIT_TIME;
import static org.alfresco.solr.AlfrescoSolrUtils.getAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclChangeSet;
import static org.alfresco.solr.AlfrescoSolrUtils.getAclReaders;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;
import static org.alfresco.solr.AlfrescoSolrUtils.getTransaction;
import static org.alfresco.solr.AlfrescoSolrUtils.indexAclChangeSet;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Stream;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AbstractAlfrescoDistributedIT;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.ContentPropertyValue;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.Transaction;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.BooleanQuery.Builder;
import org.apache.lucene.search.TermQuery;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

@SolrTestCaseJ4.SuppressSSL
public class DistributedContentPropertiesIT extends AbstractAlfrescoDistributedIT
{
    @BeforeClass
    public static void initData() throws Throwable
    {
        var solrCoreProperties = DEFAULT_CORE_PROPS;
        solrCoreProperties.setProperty("solr.enableIndexingCustomContent", "true");

        initSolrServers(3, DistributedContentPropertiesIT.class.getSimpleName(), solrCoreProperties);
    }

    @After
    public void clearData() throws Exception
    {
        deleteByQueryAllClients("*:*");
        explicitCommitOnAllClients();
    }

    @AfterClass
    public static void destroyData()
    {
        dismissSolrServers();
    }

    /**
     * In this scenario we have n nodes.
     * Among them:
     *
     * <ul>
     *     <li>n-m have one default cm:content field </li>
     *     <li>the m do not have a value for the content field (i.e. the value of the content field is empty) </li>
     * </ul>
     */
    @Test
    public void eachNodeHasAtMaximumOneCmContentField() throws Exception
    {
        putHandleDefaults();

        var aclChangeSet = getAclChangeSet(1, 1);
        var acl = getAcl(aclChangeSet);

        var aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        indexAclChangeSet(aclChangeSet, singletonList(acl), singletonList(aclReaders));

        var howManyTestNodes = 10;

        var transaction = getTransaction(0, howManyTestNodes);

        var nodes = nodes(howManyTestNodes, transaction, acl);
        var metadata = metadata(nodes, transaction, acl);

        var howManyNodesWithContent = howManyTestNodes - 3;
        indexTransaction(transaction, nodes, metadata, textContent(nodes, "Lorem ipsum dolor sit amet", howManyNodesWithContent));

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "ipsum")),
                howManyNodesWithContent, MAX_WAIT_TIME);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}" + ContentModel.PROP_PERSONDESC.getLocalName(), "ipsum")),
                0, MAX_WAIT_TIME);
    }

    /**
     * In this scenario we have n nodes.
     * Among them:
     *
     * <ul>
     *     <li>n-m have one custom content field (i.e. a field different from cm:content).</li>
     *     <li>the m do not have a value for the content field (i.e. the value of the content field is empty) </li>
     * </ul>
     */
    @Test
    public void eachNodeHasAtMaximumOneCustomContentField() throws Exception
    {
        putHandleDefaults();

        var aclChangeSet = getAclChangeSet(1, 1);
        var acl = getAcl(aclChangeSet);

        // Arbitrary acl data.
        var aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        indexAclChangeSet(aclChangeSet, singletonList(acl), singletonList(aclReaders));

        var howManyTestNodes = 10;

        var transaction = getTransaction(0, howManyTestNodes);

        var nodes = nodes(howManyTestNodes, transaction, acl);
        var metadata =
                metadata(nodes, transaction, acl).stream()
                                .peek(nodeMetadata -> {
                                    nodeMetadata.getProperties().remove(ContentModel.PROP_CONTENT);
                                    nodeMetadata.getProperties().put(ContentModel.PROP_PERSONDESC,
                                            new ContentPropertyValue(Locale.US, 0L, "UTF-8", "text/plain", null));})
                                .collect(toList());

        var howManyNodesWithContent = howManyTestNodes - 4;
        var howManyNodesWithoutContent = howManyTestNodes - howManyNodesWithContent;

        var textContents =
                Stream.concat(
                        range(0, howManyNodesWithContent)
                                .mapToObj(index -> Map.of(ContentModel.PROP_PERSONDESC, "consectetur Adipiscing elit " + System.currentTimeMillis())),
                        range(0, howManyNodesWithoutContent)
                                .mapToObj(index -> Map.of(ContentModel.PROP_PERSONDESC, "")))
                    .collect(toList());

        indexTransactionWithMultipleContentFields(transaction, nodes, metadata, textContents);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}" + ContentModel.PROP_PERSONDESC.getLocalName(), "adipiscing")),
                howManyNodesWithContent,
                MAX_WAIT_TIME);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "adipiscing")),
                0, MAX_WAIT_TIME);
    }

    /**
     * In this scenario we have n nodes.
     * Among them:
     *
     * <ul>
     *     <li>n-m have one default and one custom content field (i.e. a field different from cm:content).</li>
     *     <li>the m do not have a value for those content fields (i.e. the value of the content fields is empty) </li>
     * </ul>
     */
    @Test
    public void eachNodeHasOneCustomAndOneDefaultContentField() throws Exception
    {
        putHandleDefaults();

        var aclChangeSet = getAclChangeSet(1, 1);
        var acl = getAcl(aclChangeSet);

        var aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        indexAclChangeSet(aclChangeSet, singletonList(acl), singletonList(aclReaders));

        var howManyTestNodes = 10;

        var transaction = getTransaction(0, howManyTestNodes);

        var nodes = nodes(howManyTestNodes, transaction, acl);
        var metadata =
                metadata(nodes, transaction, acl).stream()
                        .peek(nodeMetadata ->
                            nodeMetadata.getProperties().put(ContentModel.PROP_PERSONDESC,
                                    new ContentPropertyValue(Locale.US, 0L, "UTF-8", "text/plain", null)))
                        .collect(toList());

        var howManyNodesWithContent = howManyTestNodes - 2;

        indexTransactionWithMultipleContentFields(
                transaction,
                nodes,
                metadata,
                textContentWithMultipleContentFields(
                        nodes,
                        "Lorem ipsum dolor sit amet",
                        "consectetur Adipiscing elit",
                        howManyNodesWithContent));

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}" + ContentModel.PROP_PERSONDESC.getLocalName(), "consectetur")),
                howManyNodesWithContent,
                MAX_WAIT_TIME);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "ipsum")),
                howManyNodesWithContent, MAX_WAIT_TIME);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}" + ContentModel.PROP_PERSONDESC.getLocalName(), "ipsum")),
                0,
                MAX_WAIT_TIME);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "elit")),
                0, MAX_WAIT_TIME);
    }

    /**
     * In this scenario we have n + m + y nodes.
     * Among them:
     *
     * <ul>
     *     <li>n nodes have one custom content field (i.e. a field different from cm:content).</li>
     *     <li>m nodes have one default content field (i.e. cm:content).</li>
     *     <li>y nodes do not have a value for the content field </li>
     * </ul>
     */
    @Test
    public void someNodeHasOneCustomAndSomeNodeHasOneDefaultContentField() throws Exception
    {
        putHandleDefaults();

        var aclChangeSet = getAclChangeSet(1, 1);
        var acl = getAcl(aclChangeSet);

        var aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        indexAclChangeSet(aclChangeSet, singletonList(acl), singletonList(aclReaders));

        var howManyTestNodes = 10;

        var transaction = getTransaction(0, howManyTestNodes);

        var nodes = nodes(howManyTestNodes, transaction, acl);
        var metadata =
                metadata(nodes, transaction, acl).stream()
                        .peek(nodeMetadata ->
                            nodeMetadata.getProperties().put(ContentModel.PROP_PERSONDESC,
                                    new ContentPropertyValue(Locale.US, 0L, "UTF-8", "text/plain", null)))
                        .collect(toList());

        var howManyNodesWithContent = howManyTestNodes - 2;
        var howManyNodesWithDefaultContentField = 3;
        var howManyNodesWithCustomContentField = howManyNodesWithContent - howManyNodesWithDefaultContentField;
        var howManyNodesWithoutContent = howManyTestNodes - howManyNodesWithContent;

        var baseCmContentText = "Lorem ipsum dolor sit amet";
        var basePersonDescriptionText = "consectetur Adipiscing elit";

        var texts =
                Stream.concat(
                    range(0, howManyNodesWithDefaultContentField)
                        .mapToObj(index -> Map.of(
                                            ContentModel.PROP_CONTENT, baseCmContentText + " " + System.currentTimeMillis(),
                                            ContentModel.PROP_PERSONDESC, "")),
                    range(0, howManyNodesWithCustomContentField)
                        .mapToObj(index -> Map.of(
                                            ContentModel.PROP_CONTENT, "",
                                            ContentModel.PROP_PERSONDESC, basePersonDescriptionText + " " + System.currentTimeMillis())));

        var textContents =
                Stream.concat(
                    texts,
                    range(0, howManyNodesWithoutContent)
                        .mapToObj(index -> Map.of(
                                ContentModel.PROP_CONTENT, "",
                                ContentModel.PROP_PERSONDESC, ""))).collect(toList());

        indexTransactionWithMultipleContentFields(
                transaction,
                nodes,
                metadata,
                textContents);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}" + ContentModel.PROP_PERSONDESC.getLocalName(), "consectetur")),
                howManyNodesWithCustomContentField,
                MAX_WAIT_TIME);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "ipsum")),
                howManyNodesWithDefaultContentField, MAX_WAIT_TIME);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}" + ContentModel.PROP_PERSONDESC.getLocalName(), "ipsum")),
                0,
                MAX_WAIT_TIME);

        waitForDocCount(
                new TermQuery(
                        new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}content", "elit")),
                0, MAX_WAIT_TIME);
    }

    @Test
    public void testMNT23669() throws Exception
    {
        putHandleDefaults();

        // ACL
        var aclChangeSet = getAclChangeSet(1, 1);
        var acl = getAcl(aclChangeSet);
        var aclReaders = getAclReaders(aclChangeSet, acl, singletonList("joel"), singletonList("phil"), null);
        indexAclChangeSet(aclChangeSet, singletonList(acl), singletonList(aclReaders));

        // Nodes
        var howManyTestNodes = 2;
        var transaction = getTransaction(0, howManyTestNodes);
        var nodes = nodes(howManyTestNodes, transaction, acl);
        var metadata =
                metadata(nodes, transaction, acl)
                    .stream()
                    .peek(nodeMetadata -> nodeMetadata.getProperties().put(ContentModel.PROP_PERSONDESC, new ContentPropertyValue(Locale.US, 0L, "UTF-8", "text/plain", null)))
                    .collect(toList());

        var howManyNodesWithContent = howManyTestNodes;
        var baseCmContentText = "Test default content property";
        var basePersonDescriptionText = "custom content property text";

        // Index for the first time
        indexTransactionWithMultipleContentFields(
                transaction,
                nodes,
                metadata,
                textContentWithMultipleContentFields(nodes, baseCmContentText, basePersonDescriptionText, howManyNodesWithContent));

        // Ensure nodes are searchable using the current value
        waitForDocCount(
                new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}" + ContentModel.PROP_PERSONDESC.getLocalName(), "custom")),
                howManyNodesWithContent,
                MAX_WAIT_TIME);

        // New transaction
        transaction = getTransaction(0, howManyTestNodes);
        for (var node : nodes)
        {
            node.setTxnId(transaction.getId());
        }

        // Reindex nodes with a new custom content property value
        basePersonDescriptionText = "changing value for person description";
        indexTransactionWithMultipleContentFields(
                transaction,
                nodes,
                metadata,
                textContentWithMultipleContentFields(nodes, baseCmContentText, basePersonDescriptionText, howManyNodesWithContent));

        // Build query that will search for old OR new value
        Builder builder = new BooleanQuery.Builder();
        Builder customContentBuilder = new BooleanQuery.Builder();
        TermQuery oldValueQuery = new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}" + ContentModel.PROP_PERSONDESC.getLocalName(), "custom"));
        TermQuery newValueQuery = new TermQuery(new Term("content@s___t@{http://www.alfresco.org/model/content/1.0}" + ContentModel.PROP_PERSONDESC.getLocalName(), "changing"));
        customContentBuilder.add(new BooleanClause(oldValueQuery, BooleanClause.Occur.SHOULD));
        customContentBuilder.add(new BooleanClause(newValueQuery, BooleanClause.Occur.SHOULD));
        builder.add(new BooleanClause(customContentBuilder.build(), BooleanClause.Occur.MUST));

        // The query always has to return the expected number of results which means node are always searchable, either by the old OR new value
        SolrClient client = getStandaloneClients().get(0);
        int totalHits = 0;
        for (int i = 0; i < 15; i++)
        {
            QueryResponse response = client.query(luceneToSolrQuery(builder.build()));
            totalHits = (int) response.getResults().getNumFound();
            if (totalHits != howManyNodesWithContent)
            {
                fail("(" + i + ") Expecting " + howManyNodesWithContent + " results but " + totalHits + " have been returned");
            }
            Thread.sleep(1000);
        }
    }

    private List<Node> nodes(int howMany, Transaction transaction, Acl acl)
    {
        return range(0, howMany)
                .mapToObj(index -> getNode(transaction, acl, Node.SolrApiNodeStatus.UPDATED))
                .collect(toList());
    }

    private List<NodeMetaData> metadata(List<Node> nodes, Transaction transaction, Acl acl)
    {
        return nodes.stream()
                .map(node -> getNodeMetaData(node, transaction, acl, "mike", null, false))
                .collect(toList());
    }

    private List<String> textContent(List<Node> nodes, String baseText, int limit)
    {
        return Stream.concat(
                nodes.stream()
                        .map(node -> baseText + " " + System.currentTimeMillis())
                        .limit(limit),
                range(0, nodes.size() - limit)
                        .mapToObj(index -> "")).collect(toList());
    }

    private List<Map<QName, String>> textContentWithMultipleContentFields(List<Node> nodes, String baseCmContentText, String basePersonDescriptionText, int limit)
    {
        var texts = nodes.stream()
                        .map(node -> Map.of(
                                        ContentModel.PROP_CONTENT, baseCmContentText + " " + System.currentTimeMillis(),
                                        ContentModel.PROP_PERSONDESC, basePersonDescriptionText + " " + System.currentTimeMillis()))
                        .limit(limit);

        return Stream.concat(
                texts,
                range(0, nodes.size() - limit)
                        .mapToObj(index -> Map.of(
                                ContentModel.PROP_CONTENT, "",
                                ContentModel.PROP_PERSONDESC, ""))).collect(toList());
    }
}