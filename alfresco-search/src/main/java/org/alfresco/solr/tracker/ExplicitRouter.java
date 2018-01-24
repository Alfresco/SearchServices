package org.alfresco.solr.tracker;

import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Routes a document only if the shardInstance matches the provided shardId
 */
public class ExplicitRouter implements DocRouter {

    protected final static Logger log = LoggerFactory.getLogger(ExplicitRouter.class);
    private final DBIDRouter fallback = new DBIDRouter();

    public ExplicitRouter() {
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl) {
        //all acls go to all shards.
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node, long dbidCap) {

        if(shardCount <= 1)
        {
            return true;
        }

        String shardBy = node.getShardPropertyValue();

        if (shardBy != null && !shardBy.isEmpty())
        {
            try
            {
                int shardid = Integer.parseInt(shardBy);
                return shardid == shardInstance;
            }
            catch (NumberFormatException e)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Shard "+shardInstance+" EXPLICIT_ID routing specified but failed to parse a shard property value ("+shardBy+") for node "+node.getNodeRef());
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("Shard "+shardInstance+" EXPLICIT_ID routing specified but no shard id property found for node "+node.getNodeRef());
            }
        }

        if (log.isDebugEnabled())
        {
            log.debug("Shard "+shardInstance+" falling back to DBID routing for node "+node.getNodeRef());
        }
        return fallback.routeNode(shardCount, shardInstance, node, dbidCap);
    }
}
