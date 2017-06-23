/*
 * Copyright (C) 2014 Alfresco Software Limited.
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
package org.alfresco.solr.tracker;

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alfresco.solr.AlfrescoSolrDataModel.TenantAclIdDbId;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.client.SOLRAPIClient;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.runners.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContentTrackerTest
{
    private ContentTracker contentTracker;
    
    @Mock
    private SOLRAPIClient repositoryClient;
    private String coreName = "theCoreName";
    @Mock
    private SolrInformationServer srv;
    @Spy
    private Properties props;
    @Mock
    private TrackerStats trackerStats;

    private int UPDATE_BATCH = 2;
    private int READ_BATCH = 400;

    @Before
    public void setUp() throws Exception
    {
        doReturn("workspace://SpacesStore").when(props).getProperty(eq("alfresco.stores"), anyString());
        doReturn("" + UPDATE_BATCH).when(props).getProperty(eq("alfresco.contentUpdateBatchSize"), anyString());
        doReturn("" + READ_BATCH).when(props).getProperty(eq("alfresco.contentReadBatchSize"), anyString());
        when(srv.getTrackerStats()).thenReturn(trackerStats);
        this.contentTracker = new ContentTracker(props, repositoryClient, coreName, srv);
       
    }

    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")

    public void doTrackWithNoContentDoesNothing() throws Exception
    {
        this.contentTracker.doTrack();
        verify(srv, never()).updateContentToIndexAndCache(anyLong(), anyString());
        verify(srv, never()).commit();
    }

    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void doTrackWithContentUpdatesContent() throws Exception
    {
        List<TenantAclIdDbId> docs1 = new ArrayList<>();
        List<TenantAclIdDbId> docs2 = new ArrayList<>();
        List<TenantAclIdDbId> emptyList = new ArrayList<>();
        // Adds one more than the UPDATE_BATCH
        for (int i = 0; i <= UPDATE_BATCH; i++)
        {
            TenantAclIdDbId doc = new TenantAclIdDbId();
            doc.dbId = 1l;
            doc.tenant = "1";
            docs1.add(doc);
        }
        TenantAclIdDbId thirdDoc = docs1.get(UPDATE_BATCH);
        thirdDoc.dbId = 3l;
        thirdDoc.tenant = "3";

        // Adds UPDATE_BATCH
        for (long i = 0; i < UPDATE_BATCH; i++)
        {
            TenantAclIdDbId doc = new TenantAclIdDbId();
            doc.dbId = 2l;
            doc.tenant = "2";
            docs2.add(doc);
        }
        when(this.srv.getDocsWithUncleanContent(anyInt(), anyInt()))
                .thenReturn(docs1)
                .thenReturn(docs2)
            .thenReturn(emptyList);
        this.contentTracker.doTrack();
        
        InOrder order = inOrder(srv);
        order.verify(srv).getDocsWithUncleanContent(0, READ_BATCH);
        
        /*
         * I had to make each bunch of calls have different parameters to prevent Mockito from incorrectly failing
         * because it was finding 5 calls instead of finding the first two calls, then the commit, then the rest...
         * It seems that Mockito has a bug with verification in order.
         * See https://code.google.com/p/mockito/issues/detail?id=296
         */

        // From docs1
        order.verify(srv, times(UPDATE_BATCH)).updateContentToIndexAndCache(1l, "1");
        order.verify(srv).commit();
        // The one extra doc should be processed and then committed
        order.verify(srv).updateContentToIndexAndCache(thirdDoc.dbId, thirdDoc.tenant);
        order.verify(srv).commit();
        
        order.verify(srv).getDocsWithUncleanContent(0 + READ_BATCH, READ_BATCH);
        
        // From docs2
        order.verify(srv, times(UPDATE_BATCH)).updateContentToIndexAndCache(2l, "2");
        order.verify(srv).commit();
        
        order.verify(srv).getDocsWithUncleanContent(0 + READ_BATCH + READ_BATCH, READ_BATCH);
    }
    @Test
    public void typeCheck()
    {
        Assert.assertTrue(contentTracker.getType().equals(Tracker.Type.Content));
    }
    
}
