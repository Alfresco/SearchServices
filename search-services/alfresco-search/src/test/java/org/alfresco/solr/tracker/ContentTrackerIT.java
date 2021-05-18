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

import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alfresco.solr.AlfrescoSolrDataModel.TenantDbId;
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
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ContentTrackerIT
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

    @Before
    public void setUp() throws Exception
    {
        doReturn("workspace://SpacesStore").when(props).getProperty(eq("alfresco.stores"), anyString());
        doReturn("" + UPDATE_BATCH).when(props).getProperty(eq("alfresco.contentUpdateBatchSize"), anyString());
        when(srv.getTrackerStats()).thenReturn(trackerStats);
        this.contentTracker = new ContentTracker(props, repositoryClient, coreName, srv);
       
    }

    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")

    public void doTrackWithNoContentDoesNothing() throws Exception
    {
        this.contentTracker.doTrack("anIterationId");
        verify(srv, never()).updateContent(any());
        verify(srv, never()).commit();
    }

    @Test
    @Ignore("Superseded by AlfrescoSolrTrackerTest")
    public void doTrackWithContentUpdatesContent() throws Exception
    {
        List<TenantDbId> docs1 = new ArrayList<>();
        List<TenantDbId> docs2 = new ArrayList<>();
        List<TenantDbId> emptyList = new ArrayList<>();
        // Adds one more than the UPDATE_BATCH
        for (int i = 0; i <= UPDATE_BATCH; i++)
        {
            TenantDbId doc = new TenantDbId();
            doc.dbId = 1l;
            doc.tenant = "1";
            docs1.add(doc);
        }
        TenantDbId thirdDoc = docs1.get(UPDATE_BATCH);
        thirdDoc.dbId = 3l;
        thirdDoc.tenant = "3";

        // Adds UPDATE_BATCH
        for (long i = 0; i < UPDATE_BATCH; i++)
        {
            TenantDbId doc = new TenantDbId();
            doc.dbId = 2l;
            doc.tenant = "2";
            docs2.add(doc);
        }
        when(this.srv.getDocsWithUncleanContent())
                .thenReturn(docs1)
                .thenReturn(docs2)
            .thenReturn(emptyList);
        this.contentTracker.doTrack("anIterationId");
        
        InOrder order = inOrder(srv);
        order.verify(srv).getDocsWithUncleanContent();
        
        /*
         * I had to make each bunch of calls have different parameters to prevent Mockito from incorrectly failing
         * because it was finding 5 calls instead of finding the first two calls, then the commit, then the rest...
         * It seems that Mockito has a bug with verification in order.
         * See https://code.google.com/p/mockito/issues/detail?id=296
         */

        // From docs1
        TenantDbId docRef = new TenantDbId();
        docRef.dbId = 1L;
        docRef.tenant = "1";

        order.verify(srv, times(UPDATE_BATCH)).updateContent(docRef);
        order.verify(srv).commit();

        // The one extra doc should be processed and then committed
        order.verify(srv).updateContent(thirdDoc);
        order.verify(srv).commit();
        
        order.verify(srv).getDocsWithUncleanContent();
        
        // From docs2
        docRef = new TenantDbId();
        docRef.dbId = 2L;
        docRef.tenant = "2";
        order.verify(srv, times(UPDATE_BATCH)).updateContent(docRef);
        order.verify(srv).commit();
        
        order.verify(srv).getDocsWithUncleanContent();
    }
    @Test
    public void typeCheck()
    {
        Assert.assertEquals(contentTracker.getType(), Tracker.Type.CONTENT);
    }
}
