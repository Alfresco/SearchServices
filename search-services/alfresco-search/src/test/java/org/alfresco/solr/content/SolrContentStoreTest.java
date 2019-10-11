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
import java.nio.file.Files;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

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

import static java.util.Collections.emptyMap;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

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
    public void whenAccessModeIsNotSetMethodCallsThrowsExceptionOrDoNothing()
    {
        assertSame(contentStore.notYetSet, contentStore.currentAccessMode);

        expectIllegalState(contentStore::getLastCommittedVersion);
        expectIllegalState(contentStore::setLastCommittedVersion, System.currentTimeMillis());
        expectIllegalState(contentStore::getChanges, System.currentTimeMillis());
        expectIllegalState(contentStore::removeDocFromContentStore, mock(NodeMetaData.class));
        expectIllegalState(contentStore::storeDocOnSolrContentStore, mock(NodeMetaData.class), mock(SolrInputDocument.class));

        try
        {
            contentStore.flushChangeSet();
            fail();
        }
        catch (IOException exception)
        {
            fail();
        }
        catch(IllegalStateException expected)
        {
            // Nothing to be done here
        }

        try
        {
            contentStore.storeDocOnSolrContentStore(DEFAULT_TENANT, System.currentTimeMillis(), mock(SolrInputDocument.class));
            fail();
        }
        catch(IllegalStateException expected)
        {
            // Nothing to be done here
        }
    }

    @Test
    public void lastCommittedVersionInReadOnlyModeNotFound()
    {
        contentStore.toggleReadOnlyMode(true);

        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, contentStore.getLastCommittedVersion());
    }

    @Test
    public void lastCommittedVersionInReadOnlyModeNotFoundBecauseException() throws IOException
    {
        contentStore.toggleReadOnlyMode(true);

        Files.write(new File(contentStore.getRootLocation(), ".version").toPath(), "NAN".getBytes());

        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, contentStore.getLastCommittedVersion());
    }

    @Test
    public void lastCommittedVersionInReadOnlyModeNotFoundBecauseFileIsEmpty() throws IOException
    {
        contentStore.toggleReadOnlyMode(true);

        File emptyVersionFile = new File(contentStore.getRootLocation(), ".version");
        emptyVersionFile.createNewFile();

        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, contentStore.getLastCommittedVersion());
    }

    @Test
    public void getLastCommittedVersionInReadOnlyMode() throws IOException
    {
        contentStore.toggleReadOnlyMode(true);

        long expectedLastCommittedVersion = System.currentTimeMillis();

        Files.write(new File(contentStore.getRootLocation(), ".version").toPath(), Long.toString(expectedLastCommittedVersion).getBytes());

        assertEquals(expectedLastCommittedVersion, contentStore.getLastCommittedVersion());
    }

    @Test
    public void setLastCommittedVersionInReadOnlyMode()
    {
        contentStore.toggleReadOnlyMode(true);

        long expectedLastCommittedVersion = System.currentTimeMillis();
        contentStore.setLastCommittedVersion(expectedLastCommittedVersion);

        File versionFile = new File(contentStore.getRootLocation(), ".version");
        assertTrue(versionFile.canRead());

        assertEquals(expectedLastCommittedVersion, contentStore.getLastCommittedVersion());
    }

    @Test
    public void getChangesInReadOnlyModeReturnsAnEmptyMap()
    {
        contentStore.toggleReadOnlyMode(true);
        assertEquals(Collections.<String, List<Map<String, Object>>>emptyMap(), contentStore.getChanges(System.currentTimeMillis()));
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

        assertTrue(rootDir.exists());
        assertTrue(rootDir.isDirectory());
    }


    @Test
    public void storeDocOnSolrContentStore()
    {
        contentStore.toggleReadOnlyMode(false);

        SolrInputDocument doc = mock(SolrInputDocument.class);
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
        SolrInputDocument doc = mock(SolrInputDocument.class);
        NodeMetaData nodeMetaData = mock(NodeMetaData.class);
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
        SolrInputDocument doc = mock(SolrInputDocument.class);
        NodeMetaData nodeMetaData = mock(NodeMetaData.class);
        contentStore.storeDocOnSolrContentStore(nodeMetaData, doc);
        SolrInputDocument document = contentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNotNull(document);

        contentStore.removeDocFromContentStore(nodeMetaData);
        document = contentStore.retrieveDocFromSolrContentStore(DEFAULT_TENANT, 0);
        Assert.assertNull(document);
    }

    private void expectIllegalState(Supplier<?> function)
    {
        try
        {
            function.get();
            fail();
        }
        catch (IllegalStateException expected)
        {
            // Nothing to do, this is expected
        }
    }

    private <T> void expectIllegalState(Consumer<T> function, T arg)
    {
        try
        {
            function.accept(arg);
            fail();
        }
        catch (IllegalStateException expected)
        {
            // Nothing to do, this is expected
        }
    }

    private <I,O> void expectIllegalState(Function<I,O> function, I arg)
    {
        try
        {
            function.apply(arg);
            fail();
        }
        catch (IllegalStateException expected)
        {
            // Nothing to do, this is expected
        }
    }

    private <A, B> void expectIllegalState(BiConsumer<A, B> function, A arg1, B arg2)
    {
        try
        {
            function.accept(arg1, arg2);
            fail();
        }
        catch (IllegalStateException expected)
        {
            // Nothing to do, this is expected
        }
    }
}
