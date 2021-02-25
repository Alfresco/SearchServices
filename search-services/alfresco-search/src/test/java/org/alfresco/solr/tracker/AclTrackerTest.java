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
import static org.mockito.Mockito.verify;
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
    @InjectMocks
    private AclTracker aclTracker = new AclTracker();
    @Mock
    private SOLRAPIClient solrAPIClient;
    @Mock
    private InformationServer informationServer;
    @Mock
    private TrackerState trackerState;

    @Before
    public void setUp()
    {
        openMocks(this);
    }

    /** Check that during the first run (with an empty index) then verification is successful. */
    @Test
    public void testCheckRepoAndIndexConsistency_firstRun_success() throws Exception
    {
        AclChangeSets firstChangeSets = new AclChangeSets(emptyList());
        when(solrAPIClient.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1)).thenReturn(firstChangeSets);
        when(informationServer.getAclTxDocsSize("1", "1000")).thenReturn(1);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);

        verify(trackerState).setCheckedFirstAclTransactionTime(true);
        verify(trackerState).setCheckedLastAclTransactionTime(true);
    }

    /** Check that subsequent checks of a running index don't make expensive requests. */
    @Test
    public void testCheckRepoAndIndexConsistency_alreadyInitialised_success() throws Exception
    {
        when(trackerState.getLastGoodChangeSetCommitTimeInIndex()).thenReturn(8000L);
        when(trackerState.isCheckedFirstAclTransactionTime()).thenReturn(true);
        when(trackerState.isCheckedLastAclTransactionTime()).thenReturn(true);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);

        // Check that we don't make any expensive calls to the index or the repo.
        verifyNoInteractions(solrAPIClient, informationServer);
    }

    /** Check that after downtime the validation is successful. */
    @Test
    public void testCheckRepoAndIndexConsistency_afterRestart_success() throws Exception
    {
        when(trackerState.getLastGoodChangeSetCommitTimeInIndex()).thenReturn(8000L);
        AclChangeSets firstChangeSets = new AclChangeSets(asList(new AclChangeSet(1, 1000, 2)), 8000L, 8L);
        when(solrAPIClient.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1)).thenReturn(firstChangeSets);
        when(informationServer.getAclTxDocsSize("1", "1000")).thenReturn(1);

        // The index is behind the repo.
        AclChangeSet lastIndexedChangeSet = new AclChangeSet(7, 7000, 7);
        when(informationServer.getMaxAclChangeSetIdAndCommitTimeInIndex()).thenReturn(lastIndexedChangeSet);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);

        verify(trackerState).setCheckedFirstAclTransactionTime(true);
        verify(trackerState).setCheckedLastAclTransactionTime(true);
    }

    /** Check that if the index is populated but the repository is empty then we get an exception. */
    @Test(expected = AlfrescoRuntimeException.class)
    public void testCheckRepoAndIndexConsistency_populatedIndexEmptyRepo_runtimeException() throws Exception
    {
        when(trackerState.getLastGoodChangeSetCommitTimeInIndex()).thenReturn(8000L);
        AclChangeSets firstChangeSets = new AclChangeSets(asList(new AclChangeSet(1, 1000, 2)), 8000L, 8L);
        when(solrAPIClient.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1)).thenReturn(firstChangeSets);
        // The first ACL transaction was not found in the repository.
        when(informationServer.getAclTxDocsSize("1", "1000")).thenReturn(0);

        AclChangeSet lastIndexedChangeSet = new AclChangeSet(8, 8000, 8);
        when(informationServer.getMaxAclChangeSetIdAndCommitTimeInIndex()).thenReturn(lastIndexedChangeSet);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);
    }

    /** Check that if the last ACL in the index is after the last ACL in the repository then we get an exception. */
    @Test (expected = AlfrescoRuntimeException.class)
    public void testCheckRepoAndIndexConsistency_indexAheadOfRepo_runtimeException() throws Exception
    {
        when(trackerState.getLastGoodChangeSetCommitTimeInIndex()).thenReturn(8000L);
        AclChangeSets firstChangeSets = new AclChangeSets(asList(new AclChangeSet(1, 1000, 2)), 8000L, 8L);
        when(solrAPIClient.getAclChangeSets(null, 0L,
                null, INITIAL_MAX_ACL_CHANGE_SET_ID, 1)).thenReturn(firstChangeSets);
        when(informationServer.getAclTxDocsSize("1", "1000")).thenReturn(1);

        // The index contains an ACL after the last one from the server (id 8 at time 8000L).
        AclChangeSet lastIndexedChangeSet = new AclChangeSet(9, 9000, 9);
        when(informationServer.getMaxAclChangeSetIdAndCommitTimeInIndex()).thenReturn(lastIndexedChangeSet);

        // Call the method under test.
        aclTracker.checkRepoAndIndexConsistency(trackerState);
    }
}
