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
package org.alfresco.solr;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.alfresco.solr.client.SOLRAPIClient;
import org.apache.commons.lang.reflect.FieldUtils;
import org.apache.solr.core.*;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.request.SolrRequestHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.IndexSchemaFactory;
import org.apache.solr.update.UpdateHandler;
import org.apache.solr.update.processor.RunUpdateProcessorFactory;
import org.apache.solr.update.processor.UpdateRequestProcessor;
import org.apache.solr.update.processor.UpdateRequestProcessorChain;
import org.apache.solr.update.processor.UpdateRequestProcessorFactory;
import org.apache.solr.util.TestHarness;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * This base class sets up Solr Core and related objects for unit tests.
 * @author Ahmed Owian
 */
@RunWith(MockitoJUnitRunner.class)
public abstract class SolrCoreTestBase implements SolrTestFiles
{
    protected @Mock
    AlfrescoCoreAdminHandler adminHandler;
    protected @Mock
    UpdateHandler updateHandler;
    protected @Mock
    SolrResourceLoader resourceLoader;
    protected @Mock
    SOLRAPIClient solrAPIClient;
    protected @Mock
    RunUpdateProcessorFactory runUpdateProcessorFactory;
    protected @Mock
    UpdateRequestProcessor processor;
    protected SolrCore core;
    protected CoreDescriptor coreDescriptor;
    protected CoreContainer coreContainer;
    protected RequestHandlers reqHandlers;
    protected @Mock SolrRequestHandler selectRequestHandler;
    protected @Mock SolrRequestHandler aftsRequestHandler;
    protected Map<String, SolrInfoMBean> infoRegistry;
    
    @Before
    public void setUpBase() throws Exception
    {
    	  Properties properties = new Properties();
          properties.put("solr.tests.maxBufferedDocs", "1000");
          properties.put("solr.tests.maxIndexingThreads", "10");
          properties.put("solr.tests.ramBufferSizeMB", "1024");
          properties.put("solr.tests.mergeScheduler", "org.apache.lucene.index.ConcurrentMergeScheduler");
          properties.put("solr.tests.mergePolicy", "org.apache.lucene.index.TieredMergePolicy");
          
          coreContainer = new CoreContainer(TEST_FILES_LOCATION);
          resourceLoader = new SolrResourceLoader(Paths.get(TEST_SOLR_CONF), null, properties);
          SolrConfig solrConfig = new SolrConfig(resourceLoader, "solrconfig-afts.xml", null);
          IndexSchema schema =  IndexSchemaFactory.buildIndexSchema("schema-afts.xml", solrConfig);
          coreDescriptor = new CoreDescriptor("test_core_name",  Paths.get(TEST_SOLR_COLLECTION),
                  coreContainer.getContainerProperties(), false,
                  CoreDescriptor.CORE_TRANSIENT, "true",
                  CoreDescriptor.CORE_LOADONSTARTUP, "true");

        // SolrCore is final, we can't mock with mockito
        core = new SolrCore(coreContainer, coreDescriptor, new ConfigSet("testConfigset", solrConfig, schema, null, true));
        
        FieldUtils.writeField(core, "updateHandler", updateHandler, true);
        FieldUtils.writeField(core, "resourceLoader", resourceLoader, true);
        infoRegistry = new HashMap<String, SolrInfoMBean>();
        FieldUtils.writeField(core, "infoRegistry", infoRegistry, true);
        reqHandlers = new RequestHandlers(core);
        reqHandlers.register("/select", selectRequestHandler);
        reqHandlers.register("/afts", aftsRequestHandler);
        FieldUtils.writeField(core, "reqHandlers", reqHandlers, true);

        Map<String, UpdateRequestProcessorChain> map = new HashMap<>();
        List<UpdateRequestProcessorFactory> factories = new ArrayList<UpdateRequestProcessorFactory>(1);
        factories.add(runUpdateProcessorFactory);
        when(runUpdateProcessorFactory.getInstance(any(SolrQueryRequest.class), any(SolrQueryResponse.class),
                                any(UpdateRequestProcessor.class))).thenReturn(processor);
        UpdateRequestProcessorChain def = new UpdateRequestProcessorChain(factories, core);
        map.put(null, def);
        map.put("", def);
        FieldUtils.writeField(core, "updateProcessorChains", map, true);
    }
}
