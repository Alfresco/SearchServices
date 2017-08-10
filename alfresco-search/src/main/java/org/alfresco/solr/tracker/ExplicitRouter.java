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

    public ExplicitRouter() {
    }

    @Override
    public boolean routeAcl(int shardCount, int shardInstance, Acl acl) {
        //all acls go to all shards.
        return true;
    }

    @Override
    public boolean routeNode(int shardCount, int shardInstance, Node node) {

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
                    log.debug("EXPLICIT_ID routing specified but failed to parse a shard property value of "+shardBy);
                }
            }
        }
        else
        {
            if (log.isDebugEnabled())
            {
                log.debug("EXPLICIT_ID routing specified but no shard id property found ");
            }
        }

        return false;
    }
}
