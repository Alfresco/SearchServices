package org.alfresco.solr.tracker;

import org.alfresco.service.namespace.QName;
import org.alfresco.solr.client.Acl;
import org.alfresco.solr.client.Node;

import java.util.Objects;

import static java.util.Optional.ofNullable;

import java.util.Map;

/**
 * A composable {@link DocRouter} which consists of
 *
 * <ul>
 *     <li>A primary routing strategy</li>
 *     <li>A fallback strategy used in case of failure of the strategy above</li>
 * </ul>
 *
 * @author agazzarini
 */
public class DocRouterWithFallback implements DocRouter
{

    private final DocRouter primaryStrategy;
    private final DocRouter fallbackStrategy;

    public DocRouterWithFallback(DocRouter primaryStrategy, DocRouter fallbackStrategy)
    {
        this.primaryStrategy = Objects.requireNonNull(primaryStrategy);
        this.fallbackStrategy = Objects.requireNonNull(fallbackStrategy);
    }

    @Override
    public Boolean routeAcl(int shardCount, int shardInstance, Acl acl)
    {
        return primaryStrategy.routeAcl(shardCount, shardInstance, acl);
    }

    @Override
    public Boolean routeNode(int shardCount, int shardInstance, Node node)
    {
        return ofNullable(primaryStrategy.routeNode(shardCount, shardInstance, node))
                .orElseGet(() -> ofNullable(fallbackStrategy.routeNode(shardCount, shardInstance, node))
                                    .orElse(false));
    }
    
    @Override
    public Map<String, String> getProperties(QName shardProperty)
    {
        return primaryStrategy.getProperties(shardProperty);
    }

}
