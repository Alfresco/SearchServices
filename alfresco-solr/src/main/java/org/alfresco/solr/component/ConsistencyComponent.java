/*
 * #%L
 * Alfresco Solr
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
package org.alfresco.solr.component;

import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.InformationServer;
import org.alfresco.solr.TrackerState;
import org.alfresco.solr.tracker.MetadataTracker;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.Date;

/**
 * Adds consitency information to the search results
 */
public class ConsistencyComponent extends SearchComponent
{
    private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    @Override
    public void prepare(ResponseBuilder rb) throws IOException
    {
        // No preparation required.
    }

    @Override
    public void finishStage(ResponseBuilder rb)
    {
        super.finishStage(rb);
        if (rb.stage != ResponseBuilder.STAGE_GET_FIELDS)
            return;
        try {
            process(rb);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    @Override
    public void process(ResponseBuilder rb) throws IOException
    {
        SolrQueryRequest req = rb.req;
        AlfrescoCoreAdminHandler adminHandler = (AlfrescoCoreAdminHandler)
                    req.getCore().
                    getCoreDescriptor().
                    getCoreContainer().
                    getMultiCoreHandler();

        boolean isShard = rb.req.getParams().getBool(ShardParams.IS_SHARD, false);
        MetadataTracker metaTrkr = adminHandler.getTrackerRegistry().getTrackerForCore(req.getCore().getName(), MetadataTracker.class);
        if(metaTrkr != null && !isShard)
        {
            TrackerState metadataTrkrState = metaTrkr.getTrackerState();

            long lastIndexedTx = metadataTrkrState.getLastIndexedTxId();
            long lastIndexTxCommitTime = metadataTrkrState.getLastIndexedTxCommitTime();
            long lastTxIdOnServer = metadataTrkrState.getLastTxIdOnServer();
            long transactionsToDo = lastTxIdOnServer - lastIndexedTx;
            if (transactionsToDo < 0)
            {
                transactionsToDo = 0;
            }
            rb.rsp.add("lastIndexedTx", lastIndexedTx);
            rb.rsp.add("lastIndexedTxTime", lastIndexTxCommitTime);
            rb.rsp.add("txRemaining", transactionsToDo);
        }
    }

    @Override
    public String getDescription()
    {
        return "Adds consitency information to the search results.";
    }

}
