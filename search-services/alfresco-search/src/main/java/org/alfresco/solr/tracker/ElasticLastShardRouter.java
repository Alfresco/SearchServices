package org.alfresco.solr.tracker;

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes a document only if the shardInstance matches the provided shardId
 */
public class ElasticLastShardRouter implements DocRouter {

    protected final static Logger log = LoggerFactory.getLogger(ExplicitRouter.class);
    private final DBIDRouter fallback = new DBIDRouter();

    public ElasticLastShardRouter() {
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl) {
        //all acls go to all shards.
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node) {


        int explicitShardId = node.getExplicitShardId();
        return explicitShardId == shardInstance;

    }
}
