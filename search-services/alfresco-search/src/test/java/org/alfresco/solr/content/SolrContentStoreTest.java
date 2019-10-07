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
package org.alfresco.solr.content;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

import org.alfresco.repo.content.ContentContext;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.solr.client.NodeMetaData;
import org.apache.commons.io.FileUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;

/**
 * Tests {@link SolrContentStoreTest}
 *
 * @author Derek Hulley
 * @since 5.0
 */
@RunWith(MockitoJUnitRunner.class) 
public class SolrContentStoreTest
{

    private static final String DEFAULT_TENANT = "_DEFAULT_";
    private String solrHome = new File("./target/contentstoretest/").getAbsolutePath();
    private long dbid = 111;
    private String tenant = "me";

    @After 
    public void tearDown() throws IOException
    {
        File rootDir = new File(new SolrContentStore(solrHome).getRootLocation());
        FileUtils.deleteDirectory(rootDir);
    }

    @Test(expected = RuntimeException.class) 
    public void contentStoreCreation_solrHomeNull_shouldThrowException()
    {
        new SolrContentStore(null);
    }

    @Test(expected = RuntimeException.class) 
    public void contentStoreCreation_solrHomeEmpty_shouldThrowException()
    {
        new SolrContentStore("");
    }

    @Test 
    public void contentStoreCreation_solrHomeNotExistSolrContentDirNotDefined_shouldUseDefaultContentStore()
    {
        SolrContentStore solrContentStore = new SolrContentStore(solrHome + "/notExist");

        Assert.assertThat(solrContentStore.getRootLocation(), is(solrHome + "/" + SolrContentStore.CONTENT_STORE));
    }

    @Test 
    public void contentStoreCreation_solrHomeNotExistSolrContentDirDefined_shouldCreateContentStore()
    {
        String testContentDir = solrHome + "/test/content/dir";
        System.setProperty(SolrContentStore.SOLR_CONTENT_DIR, testContentDir);

        SolrContentStore solrContentStore = new SolrContentStore(solrHome + "/notExist");

        Assert.assertThat(solrContentStore.getRootLocation(), is(testContentDir));

        System.clearProperty(SolrContentStore.SOLR_CONTENT_DIR);
    }

    @Test 
    public void contentStoreCreation_solrHomeExistSolrContentDirDefined_shouldCreateContentStore()
    {
        String testContentDir = solrHome + "/test/content/dir";
        System.setProperty(SolrContentStore.SOLR_CONTENT_DIR, testContentDir);

        SolrContentStore solrContentStore = new SolrContentStore(solrHome);

        Assert.assertThat(solrContentStore.getRootLocation(), is(testContentDir));

        System.clearProperty(SolrContentStore.SOLR_CONTENT_DIR);
    }

    @Test 
    public void contentStoreCreation_solrHomeExistSolrContentDirNotDefined_shouldUseDefaultContentStore()
    {
        String existSolrHomePath = solrHome + "/exist";
        File existSolrHome = new File(existSolrHomePath);
        existSolrHome.mkdir();

        SolrContentStore solrContentStore = new SolrContentStore(existSolrHomePath);

        Assert.assertThat(solrContentStore.getRootLocation(), is(solrHome + "/" + SolrContentStore.CONTENT_STORE));
    }


    @Test 
    public void rootLocation()
    {
        SolrContentStore store = new SolrContentStore(solrHome);
        File rootDir = new File(store.getRootLocation());
        Assert.assertTrue(rootDir.exists());
        Assert.assertTrue(rootDir.isDirectory());
    }


    @Test
    public void storeDocOnSolrContentStore() throws IOException
    {
        SolrContentStore solrContentStore = new SolrContentStore(solrHome);
        solrContentStore.enableMasterMode();
        SolrInputDocument doc = Mockito.mock(SolrInputDocument.class);
        SolrInputDocument document = solrContentStore.retrieveDocFromSolrContentStore(tenant, dbid);
        Assert.assertNull(document);
        solrContentStore.storeDocOnSolrContentStore(tenant, dbid, doc);
        document = solrContentStore.retrieveDocFromSolrContentStore(tenant, dbid);
        Assert.assertNotNull(document);
        solrContentStore.close();
    }

    @Test
    public void storeDocOnSolrContentStoreNodeMetaData() throws IOException
    {
        SolrContentStore solrContentStore = new SolrContentStore(solrHome);
        solrContentStore.enableMasterMode();
        SolrInputDocument doc = Mockito.mock(SolrInputDocument.class);
        NodeMetaData nodeMetaData = Mockito.mock(NodeMetaData.class);
        SolrInputDocument document = solrContentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNull(document);
        solrContentStore.storeDocOnSolrContentStore(nodeMetaData, doc);
        document = solrContentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNotNull(document);
        solrContentStore.close();
    }

    @Test
    public void removeDocFromContentStore() throws IOException
    {
        SolrContentStore solrContentStore = new SolrContentStore(solrHome);
        solrContentStore.enableMasterMode();
        SolrInputDocument doc = Mockito.mock(SolrInputDocument.class);
        NodeMetaData nodeMetaData = Mockito.mock(NodeMetaData.class);
        solrContentStore.storeDocOnSolrContentStore(nodeMetaData, doc);
        SolrInputDocument document = solrContentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNotNull(document);
        solrContentStore.removeDocFromContentStore(nodeMetaData);
        document = solrContentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNull(document);
        solrContentStore.close();
    }
}
