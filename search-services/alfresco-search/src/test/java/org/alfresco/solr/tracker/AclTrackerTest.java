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
package org.alfresco.solr.tracker;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

import static org.alfresco.solr.tracker.AclTracker.INITIAL_MAX_ACL_CHANGE_SET_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.openMocks;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.client.AclChangeSet;
import org.alfresco.solr.client.AclChangeSets;
import org.alfresco.solr.client.SOLRAPIClient;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

/** Unit tests for the {@link AclTracker}. */
public class AclTrackerTest
{
    /** The class under test. */
    @InjectMocks
    private AclTracker aclTracker;
    /** The class that gets information from the Repository. */
    @Mock
    private SOLRAPIClient repositoryClient;
    /** The class that gets information from Solr. */
    @Mock
    private InformationServer solrInformationServer;

    @Before
    public void setUp()
    {
        aclTracker = new AclTracker();
        openMocks(this);
    }

    /** Check that during the first run (with an empty repository and index) then verification is successful. */
    @Test
    public void testCheckRepoAndIndexConsistency_firstRun_success() throws Exception
    {
        TrackerState trackerState = new TrackerState();
        AclChangeSets firstChangeSets = new AclChangeSets(emptyList());
        when(repositoryClient.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1)).thenReturn(firstChangeSets);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);

        assertTrue("Expected first ACL transaction to have been checked.", trackerState.isCheckedFirstAclTransactionTime());
        assertTrue("Expected last ACL transaction to have been checked.", trackerState.isCheckedLastAclTransactionTime());
    }

    /** Check that subsequent checks of a running index don't make expensive requests. */
    @Test
    public void testCheckRepoAndIndexConsistency_alreadyInitialised_success() throws Exception
    {
        TrackerState trackerState = new TrackerState();
        trackerState.setLastGoodChangeSetCommitTimeInIndex(8000L);
        trackerState.setCheckedFirstAclTransactionTime(true);
        trackerState.setCheckedLastAclTransactionTime(true);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);

        // Check that we don't make any expensive calls to the index or the repo.
        verifyNoInteractions(repositoryClient, solrInformationServer);
    }

    /** Check that during the first run (with data in the repo but an empty index) then verification is successful. */
    @Test
    public void testCheckRepoAndIndexConsistency_populatedRepoEmptyIndex_success() throws Exception
    {
        TrackerState trackerState = new TrackerState();
        AclChangeSets firstChangeSets = new AclChangeSets(asList(new AclChangeSet(1, 1000, 2)), 8000L, 8L);
        when(repositoryClient.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1)).thenReturn(firstChangeSets);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);

        assertTrue("Expected first ACL transaction to have been checked.", trackerState.isCheckedFirstAclTransactionTime());
        assertTrue("Expected last ACL transaction to have been checked.", trackerState.isCheckedLastAclTransactionTime());
        assertEquals("Expected last good change set commit time to be loaded from repository.",
                trackerState.getLastGoodChangeSetCommitTimeInIndex(), 1000L);
        assertEquals("Expected last change set commit time to be loaded from repository.",
                trackerState.getLastChangeSetCommitTimeOnServer(), 1000L);
        assertEquals("Expected last change set id to be loaded from repository.",
                trackerState.getLastChangeSetIdOnServer(), 1);
    }

    /** Check that after downtime the validation is successful. */
    @Test
    public void testCheckRepoAndIndexConsistency_afterRestart_success() throws Exception
    {
        TrackerState trackerState = new TrackerState();
        trackerState.setLastGoodChangeSetCommitTimeInIndex(8000L);
        AclChangeSets firstChangeSets = new AclChangeSets(asList(new AclChangeSet(1, 1000, 2)), 8000L, 8L);
        when(repositoryClient.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1)).thenReturn(firstChangeSets);
        when(solrInformationServer.getAclTxDocsSize("1", "1000")).thenReturn(1);

        // The index is behind the repo.
        AclChangeSet lastIndexedChangeSet = new AclChangeSet(7, 7000, 7);
        when(solrInformationServer.getMaxAclChangeSetIdAndCommitTimeInIndex()).thenReturn(lastIndexedChangeSet);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);

        assertTrue("Expected first ACL transaction to have been checked.", trackerState.isCheckedFirstAclTransactionTime());
        assertTrue("Expected last ACL transaction to have been checked.", trackerState.isCheckedLastAclTransactionTime());
    }

    /** Check that if the index is populated but missing the first ACL transaction then we get an exception. */
    @Test(expected = AlfrescoRuntimeException.class)
    public void testCheckRepoAndIndexConsistency_indexMissingFirstACLTx_runtimeException() throws Exception
    {
        TrackerState trackerState = new TrackerState();
        trackerState.setLastGoodChangeSetCommitTimeInIndex(8000L);
        AclChangeSets firstChangeSets = new AclChangeSets(asList(new AclChangeSet(1, 1000, 2)), 8000L, 8L);
        when(repositoryClient.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1)).thenReturn(firstChangeSets);
        // The first ACL transaction was not found in Solr.
        when(solrInformationServer.getAclTxDocsSize("1", "1000")).thenReturn(0);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);
    }

    /** Check that if the last ACL in the index is after the last ACL in the repository then we get an exception. */
    @Test (expected = AlfrescoRuntimeException.class)
    public void testCheckRepoAndIndexConsistency_indexAheadOfRepo_runtimeException() throws Exception
    {
        TrackerState trackerState = new TrackerState();
        trackerState.setLastGoodChangeSetCommitTimeInIndex(8000L);
        AclChangeSets firstChangeSets = new AclChangeSets(asList(new AclChangeSet(1, 1000, 2)), 8000L, 8L);
        when(repositoryClient.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1)).thenReturn(firstChangeSets);
        when(solrInformationServer.getAclTxDocsSize("1", "1000")).thenReturn(1);

        // The index contains an ACL after the last one from the server (id 8 at time 8000L).
        AclChangeSet lastIndexedChangeSet = new AclChangeSet(9, 9000, 9);
        when(solrInformationServer.getMaxAclChangeSetIdAndCommitTimeInIndex()).thenReturn(lastIndexedChangeSet);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);
    }
}
