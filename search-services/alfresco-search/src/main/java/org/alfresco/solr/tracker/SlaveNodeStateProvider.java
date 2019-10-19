package org.alfresco.solr.tracker;

import static org.alfresco.solr.tracker.Tracker.Type.NODE_STATE_PUBLISHER;

import org.alfresco.httpclient.AuthenticationException;
import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.commons.codec.EncoderException;

import java.io.IOException;
import java.util.Properties;

/**
 * Despite belonging to the Tracker ecosystem, this component is actually a publisher, which periodically informs
 * Alfresco about the state of the hosting slave node.
 * As the name suggests, this worker is scheduled only when the hosting node acts as a slave.
 * It allows Solr's master/slave setup to be used with dynamic shard registration.
 *
 * In this scenario the slave is polling a "tracking" Solr node. The tracker below calls
 * the repo to register the state of the node without pulling any real transactions from the repo.
 *
 * This allows the repo to register the replica so that it will be included in queries. But the slave Solr node
 * will pull its data from a "tracking" Solr node using Solr's master/slave replication, rather then tracking the repository.
 *
 * @author Andrea Gazzarini
 * @since 1.5
 */
public class SlaveNodeStateProvider extends NodeStateProvider
{
    public SlaveNodeStateProvider(Properties coreProperties, SOLRAPIClient repositoryClient, String name, SolrInformationServer informationServer)
    {
        super(coreProperties, repositoryClient, name, informationServer, NODE_STATE_PUBLISHER);
    }

    @Override
    protected void doTrack()
    {
        try
        {
            ShardState shardstate = getShardState();
            client.getTransactions(0L, null, 0L, null, 0, shardstate);
        }
        catch (EncoderException exception )
        {
            log.error("Unable to publish this node state. " +
                    "A failure condition has been met during the outbound subscription message encoding process. " +
                    "See the stacktrace below for further details.", exception);
        }
        catch (IOException exception )
        {
            log.error("Unable to publish this node state. " +
                    "Detected an I/O failure while sending the subscription state message. " +
                    "See the stacktrace below for further details.", exception);
        }
        catch (AuthenticationException exception)
        {
            log.error("Unable to publish this node state. " +
                    "Authentication failure detected while sending the subscription state message. " +
                    "See the stacktrace below for further details.", exception);
        }
    }

    @Override
    public void maintenance()
    {
        // Do nothing here
    }

    @Override
    public boolean hasMaintenance()
    {
        return false;
    }
}