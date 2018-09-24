/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
 *
 * This file is part of Alfresco
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
 */
package org.alfresco.solr;

import static org.alfresco.solr.AlfrescoCoreAdminHandler.ARG_TXID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.alfresco.solr.adapters.IOpenBitSet;
import org.alfresco.solr.tracker.AclTracker;
import org.alfresco.solr.tracker.IndexHealthReport;
import org.alfresco.solr.tracker.MetadataTracker;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

/** Unit tests for {@link org.alfresco.solr.AlfrescoCoreAdminHandler}. */
@RunWith(MockitoJUnitRunner.class)
public class AlfrescoCoreAdminHandlerTest
{
    /** The string representing a transaction report. */
    private static final String TXREPORT = "TXREPORT";
    /** A transaction id. */
    private static final String TX_ID = "123";
    /** A core name. */
    private static final String CORE_NAME = "CORE_NAME";

    /** The class under test. */
    @InjectMocks
    private AlfrescoCoreAdminHandler alfrescoCoreAdminHandler;
    @Mock
    private TrackerRegistry trackerRegistry;
    @Mock
    private AclTracker aclTracker;
    @Mock
    private MetadataTracker metadataTracker;
    @Mock
    private IndexHealthReport indexHealthReport;
    @Mock
    private IndexHealthReport metaReport;
    @Mock
    private IOpenBitSet iOpenBitSet;
    @Mock
    private TrackerState trackerState;
    @Mock
    private InformationServer informationServer;
    @Mock
    private SolrQueryRequest req;
    @Mock
    private SolrQueryResponse rsp;
    @Mock
    private SolrParams params;

    @Before
    public void setUp()
    {
        // Wire the mocks into the class under test.
        alfrescoCoreAdminHandler.setTrackerRegistry(trackerRegistry);
        alfrescoCoreAdminHandler.getInformationServers().put(CORE_NAME, informationServer);

        when(req.getParams()).thenReturn(params);
    }

    /** Check that a transaction report can be generated. */
    @Test
    public void handleCustomActionTXReportSuccess() throws Exception
    {
        // Set up the parameters being passed in.
        when(params.get(CoreAdminParams.ACTION)).thenReturn(TXREPORT);
        when(params.get(CoreAdminParams.CORE)).thenReturn(CORE_NAME);
        when(params.get(ARG_TXID)).thenReturn(TX_ID);
        // Set up the mock ACL tracker.
        when(trackerRegistry.getTrackerForCore(CORE_NAME, AclTracker.class)).thenReturn(aclTracker);
        when(aclTracker.checkIndex(Long.valueOf(TX_ID), 0L, null, null)).thenReturn(indexHealthReport);
        when(indexHealthReport.getDuplicatedAclTxInIndex()).thenReturn(iOpenBitSet);
        when(indexHealthReport.getAclTxInIndexButNotInDb()).thenReturn(iOpenBitSet);
        when(indexHealthReport.getMissingAclTxFromIndex()).thenReturn(iOpenBitSet);
        when(aclTracker.getTrackerState()).thenReturn(trackerState);
        // Set up the mock metadata tracker.
        when(trackerRegistry.getTrackerForCore(CORE_NAME, MetadataTracker.class)).thenReturn(metadataTracker);
        when(metadataTracker.checkIndex(Long.valueOf(TX_ID), 0L, null, null)).thenReturn(metaReport);
        when(metaReport.getDuplicatedTxInIndex()).thenReturn(iOpenBitSet);
        when(metaReport.getTxInIndexButNotInDb()).thenReturn(iOpenBitSet);
        when(metaReport.getMissingTxFromIndex()).thenReturn(iOpenBitSet);
        when(metaReport.getDuplicatedLeafInIndex()).thenReturn(iOpenBitSet);
        when(metaReport.getDuplicatedErrorInIndex()).thenReturn(iOpenBitSet);
        when(metaReport.getDuplicatedUnindexedInIndex()).thenReturn(iOpenBitSet);
        when(metadataTracker.getTrackerState()).thenReturn(trackerState);

        // Call the method under test.
        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);

        // Check that a report was generated (don't look at the contents of the report though).
        verify(rsp).add(eq("report"), any(NamedList.class));
    }

    /** Check that when the transaction id is missing we get an exception. */
    @Test(expected = SolrException.class)
    public void handleCustomActionTXReportMissingTXId()
    {
        when(params.get(CoreAdminParams.ACTION)).thenReturn(TXREPORT);
        when(params.get(ARG_TXID)).thenReturn(null);

        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);

        verify(rsp, never()).add(anyString(), any());
    }

    /** Check that when the core name is missing we get an exception. */
    @Test(expected = SolrException.class)
    public void handleCustomActionTXReportMissingCoreName()
    {
        when(params.get(CoreAdminParams.ACTION)).thenReturn(TXREPORT);
        when(params.get(CoreAdminParams.CORE)).thenReturn(null);
        when(params.get(ARG_TXID)).thenReturn(TX_ID);

        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);

        verify(rsp, never()).add(anyString(), any());
    }

    /** Check that when an unknown action is provided we don't generate a report. */
    @Test(expected = SolrException.class)
    public void handleCustomActionUnknownAction()
    {
        when(params.get(CoreAdminParams.ACTION)).thenReturn("Unknown");

        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);

        verify(rsp, never()).add(anyString(), any());
    }

    /** Check that when the action is missing we don't generate a report. */
    @Test(expected = SolrException.class)
    public void handleCustomActionMissingAction()
    {
        when(params.get(CoreAdminParams.ACTION)).thenReturn(null);

        alfrescoCoreAdminHandler.handleCustomAction(req, rsp);

        verify(rsp, never()).add(anyString(), any());
    }
}
