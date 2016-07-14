/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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
package org.alfresco.solr.component;

import java.io.IOException;
import java.util.Properties;

import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.SolrKeyResourceLoader;
import org.alfresco.solr.client.SOLRAPIClient;
import org.alfresco.solr.client.SOLRAPIClientFactory;
import org.alfresco.solr.tracker.ModelTracker;
import org.alfresco.solr.tracker.SolrTrackerScheduler;
import org.alfresco.solr.tracker.TrackerRegistry;
import org.apache.solr.core.CoreDescriptorDecorator;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Makes queries block wait until the first model sync is done to the repository.
 * This also ensures that module models are gotten before queries go through during installation.
 * 
 * @author Ahmed Owian
 */
public class EnsureModelsComponent extends SearchComponent
{
    private static final Logger log = LoggerFactory.getLogger(EnsureModelsComponent.class);

    /*
     * (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void prepare(ResponseBuilder rb) throws IOException
    {
    	SolrQueryRequest req = rb.req;
    	Properties props = new CoreDescriptorDecorator(req.getCore().getCoreDescriptor()).getProperties();

    	boolean isTrackingEnabled = Boolean.parseBoolean(props.getProperty("enable.alfresco.tracking", "false"));
    	
    	AlfrescoCoreAdminHandler adminHandler = (AlfrescoCoreAdminHandler) req.getCore().getCoreDescriptor()
    			.getCoreContainer().getMultiCoreHandler();
    	TrackerRegistry trackerRegistry = adminHandler.getTrackerRegistry();
    	ModelTracker modelTracker;
    	// Prevents other threads from registering the ModelTracker at the same time
    	synchronized (trackerRegistry)
    	{
    		modelTracker = trackerRegistry.getModelTracker();
    		if (modelTracker == null)
    		{
    			modelTracker = registerModelTracker(req.getCore(), adminHandler, isTrackingEnabled);
    		}
    	}
    	if (isTrackingEnabled)
    	{
    		log.info("Ensuring first model sync.");
    		modelTracker.ensureFirstModelSync();
    		log.info("Done ensuring first model sync.");
    	}
    }

    private ModelTracker registerModelTracker(SolrCore core, AlfrescoCoreAdminHandler adminHandler, boolean isTrackingEnabled)
    {
        SolrResourceLoader loader = core.getLatestSchema().getResourceLoader();
        SolrKeyResourceLoader keyResourceLoader = new SolrKeyResourceLoader(loader);
        SOLRAPIClientFactory clientFactory = new SOLRAPIClientFactory();
        Properties props = new CoreDescriptorDecorator(core.getCoreDescriptor()).getProperties();
        SOLRAPIClient repositoryClient = clientFactory.getSOLRAPIClient(props, keyResourceLoader,
                AlfrescoSolrDataModel.getInstance().getDictionaryService(CMISStrictDictionaryService.DEFAULT),
                AlfrescoSolrDataModel.getInstance().getNamespaceDAO());
        String solrHome = core.getCoreDescriptor().getCoreContainer().getSolrHome();
        SolrInformationServer srv = new SolrInformationServer(adminHandler, core, repositoryClient, null);

        ModelTracker mTracker = new ModelTracker(solrHome, props, repositoryClient, core.getName(), srv);
        TrackerRegistry trackerRegistry = adminHandler.getTrackerRegistry();
        trackerRegistry.setModelTracker(mTracker);
        SolrTrackerScheduler scheduler = adminHandler.getScheduler();
        if(isTrackingEnabled)
        {
            scheduler.schedule(mTracker, core.getName(), props);
        }
        return mTracker;
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#process(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void process(ResponseBuilder rb) throws IOException
    {
        // Nothing to do
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getDescription()
     */
    @Override
    public String getDescription()
    {
        return "ensureFirstModelSync";
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getSource()
     */
    @Override
    public String getSource()
    {
        return "";
    }

}
