package org.alfresco.solr.tracker;

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes a document only if the explicitShardId matches the provided shardId
 *
 * @author Elia
 */
public class LastRegisteredShardRouter implements DocRouter {

    protected final static Logger log = LoggerFactory.getLogger(ExplicitRouter.class);

    public LastRegisteredShardRouter() {
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl) {
        //all acls go to all shards.
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node) {

        Integer explicitShardId = node.getExplicitShardId();

        if (explicitShardId == null) {
            log.error("explicitShardId is not set for node " + node.getNodeRef());
            return false;
        }

        return explicitShardId.equals(shardInstance);

    }
}
