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

import java.io.File;
import java.io.IOException;

import org.alfresco.solr.client.NodeMetaData;
import org.apache.commons.io.FileUtils;
import org.apache.solr.common.SolrInputDocument;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertSame;

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

    private String solrHome;
    private SolrContentStore contentStore;

    @Before
    public void setUp()
    {
        solrHome = new File("./target/contentstoretest/" + System.currentTimeMillis()).getAbsolutePath();
        contentStore = new SolrContentStore(solrHome);
    }

    @After 
    public void tearDown() throws IOException
    {
        contentStore.close();

        File rootDir = new File(new SolrContentStore(solrHome).getRootLocation());
        FileUtils.deleteDirectory(rootDir);
    }

    @Test
    public void atVeryBeginningAccessModeIsNotSet()
    {
        assertSame(contentStore.notYetSet, contentStore.currentAccessMode);
    }

    @Test
    public void transitionFromNotSetToReadOnlyMode()
    {
        assertSame(contentStore.notYetSet, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(true);

        assertSame(contentStore.readOnly, contentStore.currentAccessMode);
    }

    @Test
    public void transitionFromNotSetToReadWriteMode()
    {
        assertSame(contentStore.notYetSet, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(false);

        assertSame(contentStore.readWrite, contentStore.currentAccessMode);
    }

    @Test
    public void transitionFromReadOnlyToReadWriteMode()
    {
        assertSame(contentStore.notYetSet, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(true);

        assertSame(contentStore.readOnly, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(false);

        assertSame(contentStore.readWrite, contentStore.currentAccessMode);
    }

    @Test
    public void transitionFromReadOnlyToReadOnlyHasNoEffect()
    {
        assertSame(contentStore.notYetSet, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(true);

        assertSame(contentStore.readOnly, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(true);

        assertSame(contentStore.readOnly, contentStore.currentAccessMode);
    }

    @Test
    public void transitionFromReadWriteToReadOnlyModeHasNoEffect()
    {
        assertSame(contentStore.notYetSet, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(false);

        assertSame(contentStore.readWrite, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(true);

        assertSame(contentStore.readWrite, contentStore.currentAccessMode);
    }

    @Test
    public void transitionFromReadWriteToReadWriteHasNoEffect()
    {
        assertSame(contentStore.notYetSet, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(false);

        assertSame(contentStore.readWrite, contentStore.currentAccessMode);

        contentStore.toggleReadOnlyMode(false);

        assertSame(contentStore.readWrite, contentStore.currentAccessMode);
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
        File rootDir = new File(contentStore.getRootLocation());

        Assert.assertTrue(rootDir.exists());
        Assert.assertTrue(rootDir.isDirectory());
    }


    @Test
    public void storeDocOnSolrContentStore()
    {
        contentStore.toggleReadOnlyMode(false);

        SolrInputDocument doc = Mockito.mock(SolrInputDocument.class);
        long dbid = 111;
        String tenant = "me";
        SolrInputDocument document = contentStore.retrieveDocFromSolrContentStore(tenant, dbid);
        Assert.assertNull(document);

        contentStore.storeDocOnSolrContentStore(tenant, dbid, doc);
        document = contentStore.retrieveDocFromSolrContentStore(tenant, dbid);
        Assert.assertNotNull(document);
    }

    @Test
    public void storeDocOnSolrContentStoreNodeMetaData()
    {
        contentStore.toggleReadOnlyMode(false);
        SolrInputDocument doc = Mockito.mock(SolrInputDocument.class);
        NodeMetaData nodeMetaData = Mockito.mock(NodeMetaData.class);
        SolrInputDocument document = contentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNull(document);

        contentStore.storeDocOnSolrContentStore(nodeMetaData, doc);
        document = contentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNotNull(document);
    }

    @Test
    public void removeDocFromContentStore()
    {
        contentStore.toggleReadOnlyMode(false);
        SolrInputDocument doc = Mockito.mock(SolrInputDocument.class);
        NodeMetaData nodeMetaData = Mockito.mock(NodeMetaData.class);
        contentStore.storeDocOnSolrContentStore(nodeMetaData, doc);
        SolrInputDocument document = contentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNotNull(document);

        contentStore.removeDocFromContentStore(nodeMetaData);
        document = contentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNull(document);
    }
}
