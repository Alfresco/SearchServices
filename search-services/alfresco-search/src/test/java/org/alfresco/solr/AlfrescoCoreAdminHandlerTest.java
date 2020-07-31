/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

package org.alfresco.solr;

import static java.util.Optional.of;
import static java.util.stream.IntStream.range;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ACL_TX_IN_INDEX_NOT_IN_DB;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ACTION_ERROR_MESSAGE_LABEL;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ACTION_STATUS_LABEL;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ACTION_STATUS_NOT_SCHEDULED;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ACTION_STATUS_SCHEDULED;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ADDITIONAL_INFO;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ALFRESCO_CORE_NAME;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ARCHIVE_CORE_NAME;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ARG_ACLID;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ARG_ACLTXID;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ARG_NODEID;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.ARG_TXID;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.CORE_PARAMETER_NAMES;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.DRY_RUN_PARAMETER_NAME;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.DUPLICATED_ACL_TX_IN_INDEX;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.DUPLICATED_TX_IN_INDEX;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.FROM_TX_COMMIT_TIME_PARAMETER_NAME;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.MAX_TRANSACTIONS_TO_SCHEDULE_PARAMETER_NAME;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.MAX_TRANSACTIONS_TO_SCHEDULE_CONF_PROPERTY_NAME;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.MISSING_ACL_TX_IN_INDEX;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.MISSING_TX_IN_INDEX;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.TO_TX_COMMIT_TIME_PARAMETER_NAME;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.TX_IN_INDEX_NOT_IN_DB;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.UNKNOWN_CORE_MESSAGE;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.UNPROCESSABLE_REQUEST_ON_SLAVE_NODES;
import static org.alfresco.solr.AlfrescoCoreAdminHandler.VERSION_CORE_NAME;
import static org.apache.solr.common.params.CoreAdminParams.ACTION;
import static org.apache.solr.common.params.CoreAdminParams.CORE;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.adapters.SolrOpenBitSetAdapter;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.IndexHealthReport;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

@RunWith(MockitoJUnitRunner.class)
public class AlfrescoCoreAdminHandlerTest
{
    private AlfrescoCoreAdminHandler admin;

    @Mock
    TrackerRegistry registry;

    private ModifiableSolrParams params;

    @Before
    public void setUp()
    {
        admin = new AlfrescoCoreAdminHandler();
        admin.trackerRegistry = registry;
        when(registry.getCoreNames()).thenReturn(Set.of(ALFRESCO_CORE_NAME, ARCHIVE_CORE_NAME));

        params = new ModifiableSolrParams();
    }

    @Test
    public void noTargetCoreToFixInParams()
    {
        assertEquals(0, params.size());

        NamedList<Object> actionResponse = admin.actionFIX(params);
        assertEquals(0, actionResponse.size());
    }

    @Test
    public void unknownTargetCoreToFixInParams()
    {
        String invalidCoreName = "thisIsAnInvalidOrAtLeastUnknownCoreName";
        params.set(CORE, invalidCoreName);

        NamedList<Object> actionResponse = admin.actionFIX(params);
        assertEquals( 1, actionResponse.size());
        assertEquals(UNKNOWN_CORE_MESSAGE + invalidCoreName, actionResponse.get(ACTION_ERROR_MESSAGE_LABEL));
    }

    @Test
    public void fixOnSlaveNodeHasNoEffect()
    {
        params.set(CORE, ALFRESCO_CORE_NAME);

        assertFalse(admin.isMasterOrStandalone(ALFRESCO_CORE_NAME));

        NamedList<Object> actionResponse = admin.actionFIX(params);
        assertEquals( 1, actionResponse.size());
        assertEquals(UNPROCESSABLE_REQUEST_ON_SLAVE_NODES, actionResponse.get(ACTION_ERROR_MESSAGE_LABEL));
    }

    @Test
    public void maxTransactionScheduledParameterIsNotNull()
    {
        int expectedMaxTransactionToSchedule = 12876;

        params.set(CORE, ALFRESCO_CORE_NAME);
        params.set(MAX_TRANSACTIONS_TO_SCHEDULE_PARAMETER_NAME, expectedMaxTransactionToSchedule);

        admin = new AlfrescoCoreAdminHandler();

        assertEquals(expectedMaxTransactionToSchedule, admin.getMaxTransactionToSchedule(params));
    }

    @Test
    public void maxTransactionScheduledIsNull_shouldBeGatheredFromCoreProperties()
    {
        params.set(CORE, ALFRESCO_CORE_NAME);

        int expectedMaxTransactionToSchedule = 17892;
        Properties coreProperties = new Properties();
        coreProperties.setProperty(
                MAX_TRANSACTIONS_TO_SCHEDULE_CONF_PROPERTY_NAME,
                String.valueOf(expectedMaxTransactionToSchedule));
        CoreContainer coreContainer = mock(CoreContainer.class);
        SolrCore core = mock(SolrCore.class);
        SolrResourceLoader resourceLoader = mock(SolrResourceLoader.class);
        String solrHome = "./";
        when(coreContainer.getCore(ALFRESCO_CORE_NAME)).thenReturn(core);
        when(core.getResourceLoader()).thenReturn(resourceLoader);
        when(coreContainer.getSolrHome()).thenReturn(solrHome);
        when(resourceLoader.getCoreProperties()).thenReturn(coreProperties);

        admin = new AlfrescoCoreAdminHandler(coreContainer);

        assertEquals(expectedMaxTransactionToSchedule, admin.getMaxTransactionToSchedule(params));
    }

    @Test
    public void maxTransactionScheduledParameterAndConfigurationIsNull_shouldGetTheHardCodedDefault()
    {
        params.set(CORE, ALFRESCO_CORE_NAME);

        Properties coreProperties = new Properties();

        CoreContainer coreContainer = mock(CoreContainer.class);
        SolrCore core = mock(SolrCore.class);
        SolrResourceLoader resourceLoader = mock(SolrResourceLoader.class);
        String solrHome = "./";
        when(coreContainer.getCore(ALFRESCO_CORE_NAME)).thenReturn(core);
        when(core.getResourceLoader()).thenReturn(resourceLoader);
        when(coreContainer.getSolrHome()).thenReturn(solrHome);
        when(resourceLoader.getCoreProperties()).thenReturn(coreProperties);

        admin = new AlfrescoCoreAdminHandler(coreContainer);

        assertEquals(Integer.MAX_VALUE, admin.getMaxTransactionToSchedule(params));
    }

    @Test
    public void masterOrStandaloneNode_implicitDryRunParameterIsEchoed()
    {
        admin = new AlfrescoCoreAdminHandler() {
            @Override
            NamedList<Object> fixOnSpecificCore(
                    String coreName,
                    Long fromTxCommitTime,
                    Long toTxCommitTime,
                    boolean dryRun,
                    int maxTransactionsToSchedule) {
                return new NamedList<>(); // dummy entry
            }

            @Override
            boolean isMasterOrStandalone(String coreName)
            {
                return true;
            }
        };

        admin.trackerRegistry = registry;

        params.set(CORE, ALFRESCO_CORE_NAME);

        NamedList<Object> actionResponse = admin.actionFIX(params);
        assertEquals(true, actionResponse.get(DRY_RUN_PARAMETER_NAME));
        assertEquals(ACTION_STATUS_NOT_SCHEDULED, actionResponse.get(ACTION_STATUS_LABEL));
    }

    @Test
    public void masterOrStandaloneNodeWithTrackersDisabled_DryRunParameterShouldBeForcedToTrue()
    {
        class TestMetadataTracker extends MetadataTracker {

            protected TestMetadataTracker() {
                super(new Properties(), mock(SOLRAPIClient.class), ALFRESCO_CORE_NAME, mock(InformationServer.class));
                this.state = new TrackerState();
            }

            @Override
            protected void doTrack(String iterationId) {
                // Nothing to be done here, it's a fake implementation.
            }
        }

        class TestAclTracker extends AclTracker {

            protected TestAclTracker() {
                super(new Properties(), mock(SOLRAPIClient.class), ALFRESCO_CORE_NAME, mock(InformationServer.class));
                this.state = new TrackerState();
            }

            @Override
            protected void doTrack(String iterationId) {
                // Nothing to be done here, it's a fake implementation.
            }
        }

        admin = new AlfrescoCoreAdminHandler() {
            @Override
            NamedList<Object> fixOnSpecificCore(
                    String coreName,
                    Long fromTxCommitTime,
                    Long toTxCommitTime,
                    boolean dryRun,
                    int maxTransactionsToSchedule) {
                return new NamedList<>(); // dummy entry
            }

            @Override
            boolean isMasterOrStandalone(String coreName)
            {
                return true;
            }
        };

        admin.trackerRegistry = registry;
        final MetadataTracker metadataTracker = new TestMetadataTracker();
        final AclTracker aclTracker = new TestAclTracker();

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);
        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, AclTracker.class)).thenReturn(aclTracker);
        when(registry.getTrackersForCore(ALFRESCO_CORE_NAME)).thenReturn(List.of(metadataTracker, aclTracker));

        params.set(CORE, ALFRESCO_CORE_NAME);

        // Let's disable tracking on Alfresco
        admin.actionDisableIndexing(params);

        params.set(DRY_RUN_PARAMETER_NAME, false);

        NamedList<Object> actionResponse = admin.actionFIX(params);
        assertEquals(false, actionResponse.get(DRY_RUN_PARAMETER_NAME));
        assertEquals(ACTION_STATUS_NOT_SCHEDULED, actionResponse.get(ACTION_STATUS_LABEL));
        assertNotNull(
                "There should be a message which informs the requestor about the actual dryRun execution",
                actionResponse.get(ADDITIONAL_INFO));
    }

    @Test
    public void masterOrStandaloneNode_explicitDryRunParameterIsEchoed()
    {
        assertThatExplicitParameterIsEchoed(
                DRY_RUN_PARAMETER_NAME,
                true);

        assertThatExplicitParameterIsEchoed(
                DRY_RUN_PARAMETER_NAME,
                false);
    }

    @Test
    public void masterOrStandaloneNode_explicitFromCommitTimeParameterIsEchoed()
    {
        assertThatExplicitParameterIsEchoed(
                FROM_TX_COMMIT_TIME_PARAMETER_NAME,
                System.currentTimeMillis());
    }

    @Test
    public void masterOrStandaloneNode_explicitToCommitTimeParameterIsEchoed()
    {
        assertThatExplicitParameterIsEchoed(
                TO_TX_COMMIT_TIME_PARAMETER_NAME,
                System.currentTimeMillis());
    }

    @Test
    public void masterOrStandaloneNode_explicitMaxTransactionsToScheduleParameterIsEchoed()
    {
        assertThatExplicitParameterIsEchoed(
                MAX_TRANSACTIONS_TO_SCHEDULE_PARAMETER_NAME,
                Integer.MAX_VALUE);
    }

    @Test
    public void manageTransactionsToBeFixed_shouldRespectTheInputGlobalLimit()
    {
        AtomicInteger limit = new AtomicInteger(4);
        AtomicInteger transactionCount = new AtomicInteger();

        IOpenBitSet transactions = new SolrOpenBitSetAdapter();
        range(1, 7).forEach(transactions::set);

        Consumer<Long> counter = tx -> transactionCount.incrementAndGet();
        admin.manageTransactionsToBeFixed(transactions, tx -> 0, counter, limit);

        assertEquals(-1, limit.get());
        assertEquals(4, transactionCount.get());
    }

    @Test
    public void subsequentInvocationsToManageTransactionsToBeFixed_shouldRespectTheInputGlobalLimit()
    {
        // Limit is set to max 13 transactions
        AtomicInteger limit = new AtomicInteger(13);
        AtomicInteger transactionCount = new AtomicInteger();

        // First transaction set contains 6 transactions
        IOpenBitSet firstTransactionSet = new SolrOpenBitSetAdapter();
        range(1, 7).forEach(firstTransactionSet::set);

        Consumer<Long> counter = tx -> transactionCount.incrementAndGet();
        admin.manageTransactionsToBeFixed(firstTransactionSet, tx -> 0, counter, limit);

        assertEquals(7, limit.get());
        assertEquals(6, transactionCount.get());

        // Second transaction set contains 9 transactions (more than the remaining transactions to process)
        IOpenBitSet secondTransactionSet = new SolrOpenBitSetAdapter();
        range(10, 21).forEach(secondTransactionSet::set);

        admin.manageTransactionsToBeFixed(secondTransactionSet, tx -> 0, counter, limit);

        assertEquals("Global transaction limit should have been exceeded", -1, limit.get());
        assertEquals(13, transactionCount.get());

        // Third transaction set contains 10 transactions, it should be completely ignored as we already exceeded the
        // global limit above
        IOpenBitSet thirdTransactionSet = new SolrOpenBitSetAdapter();
        range(31, 42).forEach(thirdTransactionSet::set);

        Consumer<Long> thisShoulndtBeInvoked = tx -> { throw new RuntimeException("We should never be here, as the global limit has been already exceeded."); };

        admin.manageTransactionsToBeFixed(thirdTransactionSet, tx -> 0, thisShoulndtBeInvoked, limit);

        assertEquals( -2, limit.get());
        assertEquals(13, transactionCount.get());
    }

    @Test
    public void noAclTransactionToReindex_shouldReturnAnEmptyResponse()
    {
        IndexHealthReport emptyReport = mock(IndexHealthReport.class);
        when(emptyReport.getAclTxInIndexButNotInDb()).thenReturn(new SolrOpenBitSetAdapter());
        when(emptyReport.getDuplicatedAclTxInIndex()).thenReturn(new SolrOpenBitSetAdapter());
        when(emptyReport.getMissingAclTxFromIndex()).thenReturn(new SolrOpenBitSetAdapter());

        NamedList<Object> subReport = admin.aclTxToReindex(ALFRESCO_CORE_NAME, mock(AclTracker.class), emptyReport, tx -> {}, Integer.MAX_VALUE);

        assertEquals(
                Long.valueOf(0L),
                of(subReport.get(ACL_TX_IN_INDEX_NOT_IN_DB))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(ACL_TX_IN_INDEX_NOT_IN_DB + " section not found in response.")));

        assertEquals(
                Long.valueOf(0L),
                of(subReport.get(DUPLICATED_ACL_TX_IN_INDEX))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(DUPLICATED_ACL_TX_IN_INDEX + " section not found in response.")));

        assertEquals(
                Long.valueOf(0L),
                of(subReport.get(MISSING_ACL_TX_IN_INDEX))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(MISSING_ACL_TX_IN_INDEX + " section not found in response.")));
    }

    @Test
    public void noTransactionToReindex_shouldReturnAnEmptyResponse()
    {
        IndexHealthReport emptyReport = mock(IndexHealthReport.class);
        when(emptyReport.getTxInIndexButNotInDb()).thenReturn(new SolrOpenBitSetAdapter());
        when(emptyReport.getDuplicatedTxInIndex()).thenReturn(new SolrOpenBitSetAdapter());
        when(emptyReport.getMissingTxFromIndex()).thenReturn(new SolrOpenBitSetAdapter());

        NamedList<Object> subReport = admin.txToReindex(ALFRESCO_CORE_NAME, mock(MetadataTracker.class), emptyReport, tx -> {}, Integer.MAX_VALUE);

        assertEquals(
                Long.valueOf(0L),
                of(subReport.get(TX_IN_INDEX_NOT_IN_DB))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(TX_IN_INDEX_NOT_IN_DB + " section not found in response.")));

        assertEquals(
                Long.valueOf(0L),
                of(subReport.get(DUPLICATED_TX_IN_INDEX))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(DUPLICATED_TX_IN_INDEX + " section not found in response.")));

        assertEquals(
                Long.valueOf(0L),
                of(subReport.get(MISSING_TX_IN_INDEX))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(MISSING_TX_IN_INDEX + " section not found in response.")));
    }

    @Test
    public void maxTransactionsGlobalLimitShouldBeAppliedInCascade()
    {
        SolrInformationServer server = mock(SolrInformationServer.class);
        when(server.getDocListSize(anyString())).thenReturn(0);

        ConcurrentHashMap<String, InformationServer> informationServers = new ConcurrentHashMap<>();
        informationServers.put(ALFRESCO_CORE_NAME, server);
        admin.informationServers = informationServers;

        IOpenBitSet txInIndexButNotInDb = new SolrOpenBitSetAdapter();
        IOpenBitSet duplicatedTxInIndex = new SolrOpenBitSetAdapter();
        IOpenBitSet missingTxFromIndex = new SolrOpenBitSetAdapter();
        range(1, 10).forEach(txInIndexButNotInDb::set);
        range(21, 32).forEach(duplicatedTxInIndex::set);
        range(50, 61).forEach(missingTxFromIndex::set);

        int maxTransactionToSchedule = (int) (txInIndexButNotInDb.cardinality() +
                                                duplicatedTxInIndex.cardinality() +
                                                missingTxFromIndex.cardinality() -
                                                5);

        IndexHealthReport emptyReport = mock(IndexHealthReport.class);
        when(emptyReport.getTxInIndexButNotInDb()).thenReturn(txInIndexButNotInDb);
        when(emptyReport.getDuplicatedTxInIndex()).thenReturn(duplicatedTxInIndex);
        when(emptyReport.getMissingTxFromIndex()).thenReturn(missingTxFromIndex);

        NamedList<Object> subReport = admin.txToReindex(ALFRESCO_CORE_NAME, mock(MetadataTracker.class), emptyReport, tx -> {}, maxTransactionToSchedule);

        assertEquals(
                Long.valueOf(txInIndexButNotInDb.cardinality()),
                of(subReport.get(TX_IN_INDEX_NOT_IN_DB))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(TX_IN_INDEX_NOT_IN_DB + " section not found in response.")));

        assertEquals(
                Long.valueOf(duplicatedTxInIndex.cardinality()),
                of(subReport.get(DUPLICATED_TX_IN_INDEX))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(DUPLICATED_TX_IN_INDEX + " section not found in response.")));

        assertEquals(
                Long.valueOf(missingTxFromIndex.cardinality() - 5),
                of(subReport.get(MISSING_TX_IN_INDEX))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(MISSING_TX_IN_INDEX + " section not found in response.")));
    }

    @Test
    public void maxAclTransactionsGlobalLimitShouldBeAppliedInCascade()
    {
        SolrInformationServer server = mock(SolrInformationServer.class);
        when(server.getDocListSize(anyString())).thenReturn(0);

        ConcurrentHashMap<String, InformationServer> informationServers = new ConcurrentHashMap<>();
        informationServers.put(ALFRESCO_CORE_NAME, server);
        admin.informationServers = informationServers;

        IOpenBitSet txInIndexButNotInDb = new SolrOpenBitSetAdapter();
        IOpenBitSet duplicatedTxInIndex = new SolrOpenBitSetAdapter();
        IOpenBitSet missingTxFromIndex = new SolrOpenBitSetAdapter();
        range(1, 10).forEach(txInIndexButNotInDb::set);
        range(21, 32).forEach(duplicatedTxInIndex::set);
        range(50, 61).forEach(missingTxFromIndex::set);

        int maxTransactionToSchedule = (int) (txInIndexButNotInDb.cardinality() +
                duplicatedTxInIndex.cardinality() +
                missingTxFromIndex.cardinality() -
                5);

        IndexHealthReport emptyReport = mock(IndexHealthReport.class);
        when(emptyReport.getAclTxInIndexButNotInDb()).thenReturn(txInIndexButNotInDb);
        when(emptyReport.getDuplicatedAclTxInIndex()).thenReturn(duplicatedTxInIndex);
        when(emptyReport.getMissingAclTxFromIndex()).thenReturn(missingTxFromIndex);

        NamedList<Object> subReport = admin.aclTxToReindex(ALFRESCO_CORE_NAME, mock(AclTracker.class), emptyReport, tx -> {}, maxTransactionToSchedule);

        assertEquals(
                Long.valueOf(txInIndexButNotInDb.cardinality()),
                of(subReport.get(ACL_TX_IN_INDEX_NOT_IN_DB))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(ACL_TX_IN_INDEX_NOT_IN_DB + " section not found in response.")));

        assertEquals(
                Long.valueOf(duplicatedTxInIndex.cardinality()),
                of(subReport.get(DUPLICATED_ACL_TX_IN_INDEX))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(DUPLICATED_ACL_TX_IN_INDEX + " section not found in response.")));

        assertEquals(
                Long.valueOf(missingTxFromIndex.cardinality() - 5),
                of(subReport.get(MISSING_ACL_TX_IN_INDEX))
                        .map(NamedList.class::cast)
                        .map(NamedList::size)
                        .map(Number::longValue)
                        .orElseThrow(() -> new RuntimeException(MISSING_ACL_TX_IN_INDEX + " section not found in response.")));
    }

    @Test
    public void disableIndexingActionParameter_shouldTriggerTheDisableIndexingAction()
    {
        final AtomicBoolean invocationMarker = new AtomicBoolean();
        admin = new AlfrescoCoreAdminHandler() {
            @Override
            NamedList<Object> fixOnSpecificCore(
                    String coreName,
                    Long fromTxCommitTime,
                    Long toTxCommitTime,
                    boolean dryRun,
                    int maxTransactionsToSchedule) {
                return new NamedList<>(); // dummy entry
            }

            @Override
            NamedList<Object> actionDisableIndexing(SolrParams params) throws JSONException {
                invocationMarker.set(true);
                return new SimpleOrderedMap<>();
            }
        };

        params.set(ACTION, "DISABLE-INDEXING");

        SolrQueryRequest request = mock(SolrQueryRequest.class);
        when(request.getParams()).thenReturn(params);

        admin.handleCustomAction(request, mock(SolrQueryResponse.class));

        assertTrue(invocationMarker.get());
    }

    @Test
    public void enableIndexingActionParameter_shouldTriggerTheIndexingEnabling()
    {
        final AtomicBoolean invocationMarker = new AtomicBoolean();
        admin = new AlfrescoCoreAdminHandler() {
            @Override
            NamedList<Object> fixOnSpecificCore(
                    String coreName,
                    Long fromTxCommitTime,
                    Long toTxCommitTime,
                    boolean dryRun,
                    int maxTransactionsToSchedule) {
                return new NamedList<>(); // dummy entry
            }

            @Override
            NamedList<Object> actionEnableIndexing(SolrParams params) throws JSONException {
                invocationMarker.set(true);
                return new SimpleOrderedMap<>();
            }
        };

        params.set(ACTION, "ENABLE-INDEXING");

        SolrQueryRequest request = mock(SolrQueryRequest.class);
        when(request.getParams()).thenReturn(params);

        admin.handleCustomAction(request, mock(SolrQueryResponse.class));

        assertTrue(invocationMarker.get());
    }

    @Test
    public void unknownCoreNameInDisableIndexingCommand_shouldReturnAnErrorResponse()
    {
        String unknownCoreName = "ThisShouldBeAnInexistentCore";
        CORE_PARAMETER_NAMES.forEach(parameter -> {
            params.set(parameter, unknownCoreName);

            NamedList<?> response = admin.actionDisableIndexing(params);
            assertEquals(UNKNOWN_CORE_MESSAGE + unknownCoreName, response.get(ACTION_ERROR_MESSAGE_LABEL));
        });
    }

    @Test
    public void unknownCoreNameInEnableIndexingCommand_shouldReturnAnErrorResponse()
    {
        String unknownCoreName = "ThisShouldBeAnInexistentCore";
        CORE_PARAMETER_NAMES.forEach(parameter -> {
            params.set(parameter, unknownCoreName);

            NamedList<?> response = admin.actionEnableIndexing(params);
            assertEquals(UNKNOWN_CORE_MESSAGE + unknownCoreName, response.get(ACTION_ERROR_MESSAGE_LABEL));
        });
    }

    @Test
    public void disableIndexingOnSpecificSlaveCore_shouldReturnAnErrorResponse()
    {
        // The admin handler detects if a core is slave, master or standalone by checking
        // the trackers installed on it. If no trackers have been registered, then the core is considered a slave.
        assertFalse(admin.isMasterOrStandalone(ALFRESCO_CORE_NAME));
        CORE_PARAMETER_NAMES.forEach(parameter -> {
            params.set(parameter, ALFRESCO_CORE_NAME);

            NamedList<?> response = admin.actionDisableIndexing(params);
            assertEquals(UNPROCESSABLE_REQUEST_ON_SLAVE_NODES, response.get(ACTION_ERROR_MESSAGE_LABEL));
        });
    }

    @Test
    public void enableIndexingOnSpecificSlaveCore_shouldReturnAnErrorResponse()
    {
        // The admin handler detects if a core is slave, master or standalone by checking
        // the trackers installed on it. If no trackers have been registered, then the core is considered a slave.
        assertFalse(admin.isMasterOrStandalone(ALFRESCO_CORE_NAME));
        CORE_PARAMETER_NAMES.forEach(parameter -> {
            params.set(parameter, ALFRESCO_CORE_NAME);

            NamedList<?> response = admin.actionEnableIndexing(params);
            assertEquals(UNPROCESSABLE_REQUEST_ON_SLAVE_NODES, response.get(ACTION_ERROR_MESSAGE_LABEL));
        });
    }

    @Test
    public void disableIndexingWithoutIndicatingSpecificCore_shouldHaveNoEffectIfAllCoresAreSlave()
    {
        admin = spy(new AlfrescoCoreAdminHandler());
        admin.trackerRegistry = registry;
        when(registry.getCoreNames()).thenReturn(Set.of(ALFRESCO_CORE_NAME, ARCHIVE_CORE_NAME));
        admin.actionDisableIndexing(params);

        verify(admin, times(0)).disableIndexingOnSpecificCore(anyString());
    }

    @Test
    public void enableIndexingWithoutIndicatingSpecificCore_shouldHaveNoEffectIfAllCoresAreSlave()
    {
        admin = spy(new AlfrescoCoreAdminHandler());
        admin.trackerRegistry = registry;
        when(registry.getCoreNames()).thenReturn(Set.of(ALFRESCO_CORE_NAME, ARCHIVE_CORE_NAME));
        admin.actionEnableIndexing(params);

        verify(admin, times(0)).enableIndexingOnSpecificCore(anyString());
    }

    @Test
    public void disableIndexingWithoutIndicatingSpecificCore_shouldAffectOnlyMasterOrStandaloneCores()
    {
        admin = spy(new AlfrescoCoreAdminHandler());
        admin.trackerRegistry = registry;
        when(registry.getCoreNames()).thenReturn(Set.of(ALFRESCO_CORE_NAME, ARCHIVE_CORE_NAME, VERSION_CORE_NAME));

        // "alfresco" and "archive" are master/standalone cores, "version" is a slave core
        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(mock(MetadataTracker.class));
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(mock(MetadataTracker.class));
        when(registry.getTrackerForCore(VERSION_CORE_NAME, MetadataTracker.class)).thenReturn(null);

        admin.actionDisableIndexing(params);

        verify(admin, times(1)).disableIndexingOnSpecificCore(ALFRESCO_CORE_NAME);
        verify(admin, times(1)).disableIndexingOnSpecificCore(ARCHIVE_CORE_NAME);
    }

    @Test
    public void enableIndexingWithoutIndicatingSpecificCore_shouldAffectOnlyMasterOrStandaloneCores()
    {
        admin = spy(new AlfrescoCoreAdminHandler());
        admin.trackerRegistry = registry;
        when(registry.getCoreNames()).thenReturn(Set.of(ALFRESCO_CORE_NAME, ARCHIVE_CORE_NAME, VERSION_CORE_NAME));

        // "alfresco" and "archive" are master/standalone cores, "version" is a slave core
        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(mock(MetadataTracker.class));
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(mock(MetadataTracker.class));
        when(registry.getTrackerForCore(VERSION_CORE_NAME, MetadataTracker.class)).thenReturn(null);

        admin.actionEnableIndexing(params);

        verify(admin, times(1)).enableIndexingOnSpecificCore(ALFRESCO_CORE_NAME);
        verify(admin, times(1)).enableIndexingOnSpecificCore(ARCHIVE_CORE_NAME);
    }

    @Test
    public void retryActionOnSlaveNode_shouldReturnWarningMessage()
    {
        admin.coreNames().forEach(coreName -> assertFalse(admin.isMasterOrStandalone(coreName)));

        NamedList<Object> actionResponse = admin.actionRETRY(params);
        assertNotNull(actionResponse.get(AlfrescoCoreAdminHandler.WARNING));
    }

    @Test
    public void retryActionWhenIndexingIsDisabled_shouldReturnAnInfoMessage()
    {
        // That is not true: each core has an its own InformationServer instance
        // However for this specific test we don't care
        InformationServer srv = mock(InformationServer.class);
        admin.informationServers = new ConcurrentHashMap<>();
        admin.informationServers.put(ALFRESCO_CORE_NAME, srv);
        admin.informationServers.put(ARCHIVE_CORE_NAME, srv);

        // That is not true: each core has an its own MetadataTracker instance
        // However for this specific test we don't care
        MetadataTracker metadataTracker = mock(MetadataTracker.class);
        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);

        when(metadataTracker.isEnabled()).thenReturn(false);

        admin.coreNames().forEach(coreName -> assertTrue(admin.isMasterOrStandalone(coreName)));

        final NamedList<Object> actionResponse = admin.actionRETRY(params);
        admin.coreNames()
                .stream()
                .map(actionResponse::get)
                .map(NamedList.class::cast)
                .forEach(response -> {
                    assertEquals(ACTION_STATUS_NOT_SCHEDULED, response.get(ACTION_STATUS_LABEL));
                    assertNotNull(response.get(ADDITIONAL_INFO));
                });

        verifyNoInteractions(srv);
    }

    @Test
    public void retryActionWhenIndexingIsEnabled_shouldCollectThingsToReindex() throws Exception
    {
        final Set<Long> alfrescoErrorNodeIds = Set.of(123452L, 13579L, 24680L, 98765L);
        final Set<Long> archiveErrorNodeIds = Set.of(1234520L, 913579L, 124680L, 598765L);

        InformationServer alfrescoInformationServer = mock(InformationServer.class);
        InformationServer archiveInformationServer = mock(InformationServer.class);

        admin.informationServers = new ConcurrentHashMap<>();
        admin.informationServers.put(ALFRESCO_CORE_NAME, alfrescoInformationServer);
        admin.informationServers.put(ARCHIVE_CORE_NAME, archiveInformationServer);

        when(alfrescoInformationServer.getErrorDocIds()).thenReturn(alfrescoErrorNodeIds);
        when(archiveInformationServer.getErrorDocIds()).thenReturn(archiveErrorNodeIds);

        MetadataTracker alfrescoMetadataTracker = mock(MetadataTracker.class);
        MetadataTracker archiveMetadataTracker = mock(MetadataTracker.class);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(alfrescoMetadataTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(archiveMetadataTracker);

        when(alfrescoMetadataTracker.isEnabled()).thenReturn(true);
        when(archiveMetadataTracker.isEnabled()).thenReturn(true);

        admin.coreNames().forEach(coreName -> assertTrue(admin.isMasterOrStandalone(coreName)));

        final NamedList<Object> actionResponse = admin.actionRETRY(params);
        admin.coreNames()
                .stream()
                .map(actionResponse::get)
                .map(NamedList.class::cast)
                .forEach(response -> {
                    assertEquals(ACTION_STATUS_SCHEDULED, response.get(ACTION_STATUS_LABEL));
                });

        alfrescoErrorNodeIds.forEach(id -> verify(alfrescoMetadataTracker).addNodeToReindex(id));
        archiveErrorNodeIds.forEach(id -> verify(archiveMetadataTracker).addNodeToReindex(id));
    }

    @Test
    public void indexActionOnSlaveNode_shouldReturnWarningMessage()
    {
        admin.coreNames().forEach(coreName -> assertFalse(admin.isMasterOrStandalone(coreName)));

        NamedList<Object> actionResponse = admin.actionINDEX(params);
        assertNotNull(actionResponse.get(AlfrescoCoreAdminHandler.WARNING));
    }

    @Test
    public void indexActionWhenIndexingIsDisabled_shouldReturnAnInfoMessage()
    {
        MetadataTracker metadataTracker = mock(MetadataTracker.class);
        AclTracker aclTracker = mock(AclTracker.class);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, AclTracker.class)).thenReturn(aclTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, AclTracker.class)).thenReturn(aclTracker);

        when(metadataTracker.isEnabled()).thenReturn(false);
        when(aclTracker.isEnabled()).thenReturn(false);

        admin.coreNames().forEach(coreName -> assertTrue(admin.isMasterOrStandalone(coreName)));

        final NamedList<Object> actionResponse = admin.actionINDEX(params);
        admin.coreNames()
                .stream()
                .map(actionResponse::get)
                .map(NamedList.class::cast)
                .forEach(response -> {
                    assertEquals(ACTION_STATUS_NOT_SCHEDULED, response.get(ACTION_STATUS_LABEL));
                    assertNotNull(response.get(ADDITIONAL_INFO));
                });
    }

    @Test
    public void indexActionWhenIndexingIsEnabled_shouldCollectThingsToReindex()
    {
        final String txIdParam = "123452";
        final String aclTxIdParam = "13579";
        final String nodeIdParam = "24680";
        final String aclIdParam = "98765";

        params.set(ARG_TXID, txIdParam);
        params.set(ARG_ACLTXID, aclTxIdParam);
        params.set(ARG_NODEID, nodeIdParam);
        params.set(ARG_ACLID, aclIdParam);

        MetadataTracker alfrescoMetadataTracker = mock(MetadataTracker.class);
        AclTracker alfrescoAclTracker = mock(AclTracker.class);

        MetadataTracker archiveMetadataTracker = mock(MetadataTracker.class);
        AclTracker archiveAclTracker = mock(AclTracker.class);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(alfrescoMetadataTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(archiveMetadataTracker);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, AclTracker.class)).thenReturn(alfrescoAclTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, AclTracker.class)).thenReturn(archiveAclTracker);

        when(alfrescoMetadataTracker.isEnabled()).thenReturn(true);
        when(alfrescoAclTracker.isEnabled()).thenReturn(true);

        when(archiveMetadataTracker.isEnabled()).thenReturn(true);
        when(archiveAclTracker.isEnabled()).thenReturn(true);

        admin.coreNames().forEach(coreName -> assertTrue(admin.isMasterOrStandalone(coreName)));

        final NamedList<Object> actionResponse = admin.actionINDEX(params);
        admin.coreNames()
                .stream()
                .map(actionResponse::get)
                .map(NamedList.class::cast)
                .forEach(response -> {
                    assertEquals(ACTION_STATUS_SCHEDULED, response.get(ACTION_STATUS_LABEL));
                });

        verify(alfrescoMetadataTracker).addTransactionToIndex(Long.parseLong(txIdParam));
        verify(alfrescoMetadataTracker).addNodeToIndex(Long.parseLong(nodeIdParam));
        verify(alfrescoAclTracker).addAclChangeSetToIndex(Long.parseLong(aclTxIdParam));
        verify(alfrescoAclTracker).addAclToIndex(Long.parseLong(aclIdParam));

        verify(archiveMetadataTracker).addTransactionToIndex(Long.parseLong(txIdParam));
        verify(archiveMetadataTracker).addNodeToIndex(Long.parseLong(nodeIdParam));
        verify(archiveAclTracker).addAclChangeSetToIndex(Long.parseLong(aclTxIdParam));
        verify(archiveAclTracker).addAclToIndex(Long.parseLong(aclIdParam));
    }

    @Test
    public void reindexActionOnSlaveNode_shouldReturnWarningMessage()
    {
        admin.coreNames().forEach(coreName -> assertFalse(admin.isMasterOrStandalone(coreName)));

        NamedList<Object> actionResponse = admin.actionREINDEX(params);
        assertNotNull(actionResponse.get(AlfrescoCoreAdminHandler.WARNING));
    }

    @Test
    public void reindexActionWhenIndexingIsDisabled_shouldReturnAnInfoMessage()
    {
        MetadataTracker metadataTracker = mock(MetadataTracker.class);
        AclTracker aclTracker = mock(AclTracker.class);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, AclTracker.class)).thenReturn(aclTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, AclTracker.class)).thenReturn(aclTracker);

        when(metadataTracker.isEnabled()).thenReturn(false);
        when(aclTracker.isEnabled()).thenReturn(false);

        admin.coreNames().forEach(coreName -> assertTrue(admin.isMasterOrStandalone(coreName)));

        final NamedList<Object> actionResponse = admin.actionREINDEX(params);
        admin.coreNames()
                .stream()
                .map(actionResponse::get)
                .map(NamedList.class::cast)
                .forEach(response -> {
                    assertEquals(ACTION_STATUS_NOT_SCHEDULED, response.get(ACTION_STATUS_LABEL));
                    assertNotNull(response.get(ADDITIONAL_INFO));
                });
    }

    @Test
    public void reindexActionWhenIndexingIsEnabled_shouldCollectThingsToReindex()
    {
        final String txIdParam = "123452";
        final String aclTxIdParam = "13579";
        final String nodeIdParam = "24680";
        final String aclIdParam = "98765";

        params.set(ARG_TXID, txIdParam);
        params.set(ARG_ACLTXID, aclTxIdParam);
        params.set(ARG_NODEID, nodeIdParam);
        params.set(ARG_ACLID, aclIdParam);

        MetadataTracker alfrescoMetadataTracker = mock(MetadataTracker.class);
        AclTracker alfrescoAclTracker = mock(AclTracker.class);

        MetadataTracker archiveMetadataTracker = mock(MetadataTracker.class);
        AclTracker archiveAclTracker = mock(AclTracker.class);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(alfrescoMetadataTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(archiveMetadataTracker);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, AclTracker.class)).thenReturn(alfrescoAclTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, AclTracker.class)).thenReturn(archiveAclTracker);

        when(alfrescoMetadataTracker.isEnabled()).thenReturn(true);
        when(alfrescoAclTracker.isEnabled()).thenReturn(true);

        when(archiveMetadataTracker.isEnabled()).thenReturn(true);
        when(archiveAclTracker.isEnabled()).thenReturn(true);

        admin.coreNames().forEach(coreName -> assertTrue(admin.isMasterOrStandalone(coreName)));

        final NamedList<Object> actionResponse = admin.actionREINDEX(params);
        admin.coreNames()
                .stream()
                .map(actionResponse::get)
                .map(NamedList.class::cast)
                .forEach(response -> {
                    assertEquals(ACTION_STATUS_SCHEDULED, response.get(ACTION_STATUS_LABEL));
                });

        verify(alfrescoMetadataTracker).addTransactionToReindex(Long.parseLong(txIdParam));
        verify(alfrescoMetadataTracker).addNodeToReindex(Long.parseLong(nodeIdParam));
        verify(alfrescoAclTracker).addAclChangeSetToReindex(Long.parseLong(aclTxIdParam));
        verify(alfrescoAclTracker).addAclToReindex(Long.parseLong(aclIdParam));

        verify(archiveMetadataTracker).addTransactionToReindex(Long.parseLong(txIdParam));
        verify(archiveMetadataTracker).addNodeToReindex(Long.parseLong(nodeIdParam));
        verify(archiveAclTracker).addAclChangeSetToReindex(Long.parseLong(aclTxIdParam));
        verify(archiveAclTracker).addAclToReindex(Long.parseLong(aclIdParam));
    }

    @Test
    public void purgeActionOnSlaveNode_shouldReturnWarningMessage()
    {
        admin.coreNames().forEach(coreName -> assertFalse(admin.isMasterOrStandalone(coreName)));

        NamedList<Object> actionResponse = admin.actionPURGE(params);
        assertNotNull(actionResponse.get(AlfrescoCoreAdminHandler.WARNING));
    }

    @Test
    public void purgeActionWhenIndexingIsDisabled_shouldReturnAnInfoMessage()
    {
        MetadataTracker metadataTracker = mock(MetadataTracker.class);
        AclTracker aclTracker = mock(AclTracker.class);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, AclTracker.class)).thenReturn(aclTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, AclTracker.class)).thenReturn(aclTracker);

        when(metadataTracker.isEnabled()).thenReturn(false);
        when(aclTracker.isEnabled()).thenReturn(false);

        admin.coreNames().forEach(coreName -> assertTrue(admin.isMasterOrStandalone(coreName)));

        final NamedList<Object> actionResponse = admin.actionPURGE(params);
        admin.coreNames()
                .stream()
                .map(actionResponse::get)
                .map(NamedList.class::cast)
                .forEach(response -> {
            assertEquals(ACTION_STATUS_NOT_SCHEDULED, response.get(ACTION_STATUS_LABEL));
            assertNotNull(response.get(ADDITIONAL_INFO));
        });
    }

    @Test
    public void purgeActionWhenIndexingIsEnabled_shouldCollectTransactionsToPurge()
    {
        final String txIdParam = "123452";
        final String aclTxIdParam = "13579";
        final String nodeIdParam = "24680";
        final String aclIdParam = "98765";

        params.set(ARG_TXID, txIdParam);
        params.set(ARG_ACLTXID, aclTxIdParam);
        params.set(ARG_NODEID, nodeIdParam);
        params.set(ARG_ACLID, aclIdParam);

        MetadataTracker alfrescoMetadataTracker = mock(MetadataTracker.class);
        AclTracker alfrescoAclTracker = mock(AclTracker.class);

        MetadataTracker archiveMetadataTracker = mock(MetadataTracker.class);
        AclTracker archiveAclTracker = mock(AclTracker.class);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, MetadataTracker.class)).thenReturn(alfrescoMetadataTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, MetadataTracker.class)).thenReturn(archiveMetadataTracker);

        when(registry.getTrackerForCore(ALFRESCO_CORE_NAME, AclTracker.class)).thenReturn(alfrescoAclTracker);
        when(registry.getTrackerForCore(ARCHIVE_CORE_NAME, AclTracker.class)).thenReturn(archiveAclTracker);

        when(alfrescoMetadataTracker.isEnabled()).thenReturn(true);
        when(alfrescoAclTracker.isEnabled()).thenReturn(true);

        when(archiveMetadataTracker.isEnabled()).thenReturn(true);
        when(archiveAclTracker.isEnabled()).thenReturn(true);

        admin.coreNames().forEach(coreName -> assertTrue(admin.isMasterOrStandalone(coreName)));

        final NamedList<Object> actionResponse = admin.actionPURGE(params);
        admin.coreNames()
                .stream()
                .map(actionResponse::get)
                .map(NamedList.class::cast)
                .forEach(response -> {
                    assertEquals(ACTION_STATUS_SCHEDULED, response.get(ACTION_STATUS_LABEL));
                });

        verify(alfrescoMetadataTracker).addTransactionToPurge(Long.parseLong(txIdParam));
        verify(alfrescoMetadataTracker).addNodeToPurge(Long.parseLong(nodeIdParam));
        verify(alfrescoAclTracker).addAclChangeSetToPurge(Long.parseLong(aclTxIdParam));
        verify(alfrescoAclTracker).addAclToPurge(Long.parseLong(aclIdParam));

        verify(archiveMetadataTracker).addTransactionToPurge(Long.parseLong(txIdParam));
        verify(archiveMetadataTracker).addNodeToPurge(Long.parseLong(nodeIdParam));
        verify(archiveAclTracker).addAclChangeSetToPurge(Long.parseLong(aclTxIdParam));
        verify(archiveAclTracker).addAclToPurge(Long.parseLong(aclIdParam));
    }

    private <T> void assertThatExplicitParameterIsEchoed(String parameterName, T parameterValue)
    {
        admin = new AlfrescoCoreAdminHandler() {
            @Override
            NamedList<Object> fixOnSpecificCore(
                    String coreName,
                    Long fromTxCommitTime,
                    Long toTxCommitTime,
                    boolean dryRun,
                    int maxTransactionsToSchedule) {
                return new NamedList<>(); // dummy entry
            }

            @Override
            boolean isMasterOrStandalone(String coreName) {
                return true;
            }
        };

        admin.trackerRegistry = registry;

        params.set(CORE, ALFRESCO_CORE_NAME);
        params.set(parameterName, parameterValue.toString());

        NamedList<Object> actionResponse = admin.actionFIX(params);
        assertEquals(parameterValue, actionResponse.get(parameterName));
    }
}