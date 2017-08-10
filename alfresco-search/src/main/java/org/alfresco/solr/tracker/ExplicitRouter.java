package org.alfresco.solr.tracker;

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;

/**
 * Routes a document only if the shardInstance matches the provided shardId
 */
public class ExplicitRouter implements DocRouter {

    private final int shardId;

    public ExplicitRouter(int shardId) {
        this.shardId = shardId;
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl) {
        //all acls go to all shards.
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node) {
        return shardId == shardInstance;
    }
}
