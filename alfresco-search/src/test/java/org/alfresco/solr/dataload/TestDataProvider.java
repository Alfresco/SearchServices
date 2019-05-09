package org.alfresco.solr.dataload;

import static java.util.stream.IntStream.range;
import static org.alfresco.solr.AlfrescoSolrUtils.getNode;
import static org.alfresco.solr.AlfrescoSolrUtils.getNodeMetaData;

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.client.Transaction;

import java.util.AbstractMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class TestDataProvider {

    public static Map.Entry<List<Node>, List<NodeMetaData>> nSampleNodesWithSampleContent(Acl acl, Transaction txn, int howManyNodes)
    {
        List<Node> nodes = range(0, howManyNodes).mapToObj(index -> getNode(txn, acl, Node.SolrApiNodeStatus.UPDATED)).collect(Collectors.toList());
        List<NodeMetaData> metadata = nodes.stream().map(node -> getNodeMetaData(node, txn, acl, "mike", null, false)).collect(Collectors.toList());

        return new AbstractMap.SimpleImmutableEntry<>(nodes, metadata);
    }
}
