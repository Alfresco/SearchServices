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
package org.alfresco.solr.lifecycle;

import org.apache.solr.core.SolrConfig;
import org.apache.solr.core.SolrCore;
import org.junit.Test;
import org.xml.sax.InputSource;

import static org.alfresco.solr.lifecycle.SolrCoreLoadRegistration.isContentStoreInReadOnlyModeFor;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * {@link SolrCoreLoadRegistration} test case.
 *
 * @author Andrea Gazzarini
 * @since 1.5
 */
public class SolrCoreLoadRegistrationTest
{
    private SolrCore core;

    @Test
    public void noReplicationHandlerDefined_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_no_replication_handler_defined.xml");
        assertFalse("If no replication handler is defined, then we expect to run a RW content store.", isContentStoreInReadOnlyModeFor(core));
    }

    @Test
    public void emptyReplicationHandlerDefined_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_empty_replication_handler.xml");
        assertFalse("If an empty replication handler is defined, then we expect to run a RW content store.", isContentStoreInReadOnlyModeFor(core));
    }

    @Test
    public void slaveReplicationHandlerDefinedButDisabled_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_slave_disabled_replication_handler.xml");
        assertFalse("If a slave replication handler is defined but disabled, then we expect to run a RW content store.", isContentStoreInReadOnlyModeFor(core));
    }

    @Test
    public void masterReplicationHandlerDefined_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_master_replication_handler.xml");
        assertFalse("If a master replication handler is defined but disabled, then we expect to run a RW content store.", isContentStoreInReadOnlyModeFor(core));
    }

    @Test
    public void masterReplicationHandlerDefinedButDisabled_thenContentStoreIsInReadWriteMode() throws Exception
    {
        prepare("solrconfig_master_disabled_replication_handler.xml");
        assertFalse("If a master replication handler is defined but disabled, then we expect to run a RW content store.", isContentStoreInReadOnlyModeFor(core));
    }

    @Test
    public void slaveReplicationHandlerDefined_thenContentStoreIsInReadOnlyMode() throws Exception
    {
        prepare("solrconfig_slave_replication_handler.xml");
        assertTrue("If a slave replication handler is defined, then we expect to run a RO content store.", isContentStoreInReadOnlyModeFor(core));
    }

    private void prepare(String configName) throws Exception
    {
        core = mock(SolrCore.class);
        SolrConfig solrConfig = new SolrConfig(configName, new InputSource(getClass().getResourceAsStream("/test-files/" + configName)));
        when(core.getSolrConfig()).thenReturn(solrConfig);
    }
}
