/*
 * #%L
 * Alfresco Solr Client
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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

import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.alfresco.repo.index.shard.ShardState;
import org.alfresco.repo.index.shard.ShardStateBuilder;
import org.alfresco.repo.search.impl.QueryParserUtils;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.commons.lang3.StringUtils;

import java.util.HashMap;
import java.util.Optional;
import java.util.Properties;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static org.alfresco.solr.tracker.DocRouterFactory.SHARD_KEY_KEY;

/**
 * Superclass for all components which are able to inform Alfresco about the hosting node state.
 * This has been introduced in SEARCH-1752 for splitting the dual responsibility of the {@link MetadataTracker}.
 * As consequence of that, this class contains all the members needed for obtaining a valid
 * {@link org.alfresco.repo.index.shard.ShardState} that can be periodically communicated to Alfresco.
 *
 * @author Andrea Gazzarini
 * @since 1.5
 * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-1752">SEARCH-1752</a>
 */
public abstract class NodeStateProvider extends AbstractTracker
{
    DocRouter docRouter;

    /** The string representation of the shard key. */
    private Optional<String> shardKey;

    /** The property to use for determining the shard. */
    protected Optional<QName> shardProperty = Optional.empty();

    NodeStateProvider(
            Properties p,
            SOLRAPIClient client,
            String coreName,
            InformationServer informationServer,
            Type type)
    {
        super(p, client, coreName, informationServer, type);
        shardMethod = p.getProperty("shard.method", SHARD_METHOD_DBID);
        shardKey = ofNullable(p.getProperty(SHARD_KEY_KEY));
        firstUpdateShardProperty();
        docRouter = DocRouterFactory.getRouter(p, ShardMethodEnum.getShardMethod(shardMethod));
    }

    NodeStateProvider(Type type)
    {
        super(type);
    }

    private void firstUpdateShardProperty()
    {
        shardKey.ifPresent( shardKeyName -> {
            updateShardProperty();
            if (shardProperty.isEmpty())
            {
                log.warn("Sharding property " + SHARD_KEY_KEY + " was set to " + shardKeyName + ", but no such property was found.");
            }
        });
    }

    /**
     * Set the shard property using the shard key.
     */
    void updateShardProperty()
    {
        shardKey.ifPresent(shardKeyName -> {
            Optional<QName> updatedShardProperty = getShardProperty(shardKeyName);
            if (!shardProperty.equals(updatedShardProperty))
            {
                if (updatedShardProperty.isEmpty())
                {
                    log.warn("The model defining " + shardKeyName + " property has been disabled");
                }
                else
                {
                    log.info("New " + SHARD_KEY_KEY + " property found for " + shardKeyName);
                }
            }
            shardProperty = updatedShardProperty;
        });
    }

    /**
     * Given the field name, returns the name of the property definition.
     * If the property definition is not found, Empty optional is returned.
     *
     * @param field the field name.
     * @return the name of the associated property definition if present, Optional.Empty() otherwise
     */
    static Optional<QName> getShardProperty(String field)
    {
        if (StringUtils.isBlank(field))
        {
            throw new IllegalArgumentException("Sharding property " + SHARD_KEY_KEY + " has not been set.");
        }

        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        NamespaceDAO namespaceDAO = dataModel.getNamespaceDAO();
        DictionaryService dictionaryService = dataModel.getDictionaryService(CMISStrictDictionaryService.DEFAULT);
        PropertyDefinition propertyDef = QueryParserUtils.matchPropertyDefinition("http://www.alfresco.org/model/content/1.0",
                namespaceDAO,
                dictionaryService,
                field);
        if (propertyDef == null)
        {
            return Optional.empty();
        }
        return of(propertyDef.getName());
    }

    /**
     * The {@link ShardState}, as the name suggests, encapsulates/stores the state of the shard which hosts this
     * {@link MetadataTracker} instance.
     *
     * The {@link ShardState} is primarily used in two places:
     *
     * <ul>
     *     <li>Transaction tracking: (see {@link MetadataTracker#trackTransactions()}): for pulling/tracking transactions from Alfresco</li>
     *     <li>
     *         DynamicSharding: when the {@link MetadataTracker} is running on a slave instance it doesn't actually act
     *         as a tracker, it calls Alfresco to register the state of the node (the shard) without pulling any transactions.
     *         As consequence of that, Alfresco will be aware about the shard which will be included in subsequent queries.
     *     </li>
     * </ul>
     *
     * @return the {@link ShardState} instance which stores the current state of the hosting shard.
     */
    ShardState getShardState()
    {
        TrackerState transactionsTrackerState = super.getTrackerState();
        TrackerState changeSetsTrackerState =
                of(infoSrv.getAdminHandler())
                        .map(AlfrescoCoreAdminHandler::getTrackerRegistry)
                        .map(registry -> registry.getTrackerForCore(coreName, AclTracker.class))
                        .map(Tracker::getTrackerState)
                        .orElse(transactionsTrackerState);

        HashMap<String, String> propertyBag = new HashMap<>();
        propertyBag.put("coreName", coreName);
        HashMap<String, String> extendedPropertyBag = new HashMap<>(propertyBag);
        updateShardProperty();

        shardProperty.ifPresent(p -> extendedPropertyBag.putAll(docRouter.getProperties(p)));

        return ShardStateBuilder.shardState()
                .withMaster(isMaster)
                .withLastUpdated(System.currentTimeMillis())
                .withLastIndexedChangeSetCommitTime(changeSetsTrackerState.getLastIndexedChangeSetCommitTime())
                .withLastIndexedChangeSetId(changeSetsTrackerState.getLastIndexedChangeSetId())
                .withLastIndexedTxCommitTime(transactionsTrackerState.getLastIndexedTxCommitTime())
                .withLastIndexedTxId(transactionsTrackerState.getLastIndexedTxId())
                .withPropertyBag(extendedPropertyBag)
                    .withShardInstance()
                        .withBaseUrl(infoSrv.getBaseUrl())
                        .withPort(infoSrv.getPort())
                        .withHostName(infoSrv.getHostName())
                        .withShard()
                            .withInstance(shardInstance)
                            .withFloc()
                                .withNumberOfShards(shardCount)
                                .withAddedStoreRef(storeRef)
                                .withTemplate(shardTemplate)
                                .withHasContent(transformContent)
                                .withShardMethod(ShardMethodEnum.getShardMethod(shardMethod))
                                .withPropertyBag(propertyBag)
                            .endFloc()
                        .endShard()
                    .endShardInstance()
                .build();
    }

    /**
     * Returns the {@link DocRouter} instance in use on this node.
     *
     * @return the {@link DocRouter} instance in use on this node.
     */
    public DocRouter getDocRouter()
    {
        return this.docRouter;
    }
}
