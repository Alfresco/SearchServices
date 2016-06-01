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
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests {@link SolrContentStoreTest}
 * 
 * @author Derek Hulley
 * @since 5.0
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrContentStoreTest
{
    private String rootStr;
    
    @Before
    public void setUp() throws IOException
    {
        File tempFile = File.createTempFile("SolrContentStoreTest-", ".bin");
        File tempFolder = tempFile.getParentFile();
        rootStr = tempFolder.getAbsolutePath() + "/" + System.currentTimeMillis();
        rootStr = new File(rootStr).getAbsolutePath();          // Ensure we handle separator char for this test
    }
    
    @After
    public void tearDown() throws IOException
    {
        if (rootStr != null)
        {
            File rootDir = new File(rootStr);
            FileUtils.deleteDirectory(rootDir);
        }
    }
    
    /**
     * Generates a content context using a string.  The URL part will be the same if the
     * data provided is the same.
     */
    private ContentContext createContentContext(String data)
    {
        return SolrContentUrlBuilder.start().add("data", data).getContentContext();
    }
    
    @Test
    public void rootLocation()
    {
        SolrContentStore store = new SolrContentStore(rootStr);
        File rootDir = new File(rootStr);
        Assert.assertTrue(rootDir.exists());
        Assert.assertTrue(rootDir.isDirectory());
        
        Assert.assertEquals(rootStr, store.getRootLocation());
    }
    
    @Test
    public void failedRootLocation() throws IOException
    {
        File rootFile = new File(rootStr);
        rootFile.createNewFile();
        try
        {
            new SolrContentStore(rootStr);
            Assert.fail("Failed to handle file in root location.");
        }
        catch (RuntimeException e)
        {
            // Expected
        }
        rootFile.delete();
    }
    
    @Test
    public void reconstruct()
    {
        new SolrContentStore(rootStr);
        new SolrContentStore(rootStr);
    }
    
    @Test
    public void getWriter()
    {
        SolrContentStore store = new SolrContentStore(rootStr);
        
        ContentContext ctx = createContentContext("abc");
        ContentWriter writer = store.getWriter(ctx);
        String url = writer.getContentUrl();
        
        Assert.assertNotNull(url);
        Assert.assertEquals("URL of the context does not match the writer URL. ", ctx.getContentUrl(), url);
    }
    
    @Test
    public void contentByString()
    {
        SolrContentStore store = new SolrContentStore(rootStr);
        
        ContentContext ctx = createContentContext("abc");
        ContentWriter writer = store.getWriter(ctx);
        
        File file = new File(rootStr + "/" + writer.getContentUrl().replace("solr://", ""));
        Assert.assertFalse("File was created before anything was written", file.exists());

        String content = "Quick brown fox jumps over the lazy dog.";
        writer.putContent(content);
        Assert.assertTrue("File was not created.", file.exists());
        
        try
        {
            writer.putContent("Should not work");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
        
        // Now get the reader
        ContentReader reader = store.getReader(ctx.getContentUrl());
        Assert.assertNotNull(reader);
        Assert.assertTrue(reader.exists());
        
        Assert.assertEquals(content, reader.getContentString());
    }
    
    @Test
    public void contentByStream() throws Exception
    {
        SolrContentStore store = new SolrContentStore(rootStr);
        
        ContentContext ctx = createContentContext("abc");
        ContentWriter writer = store.getWriter(ctx);
        
        byte[] bytes = new byte[] {1, 7, 13};
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        writer.putContent(bis);
        
        // Now get the reader
        ContentReader reader = store.getReader(ctx.getContentUrl());
        
        ByteArrayOutputStream bos = new ByteArrayOutputStream(3);
        reader.getContent(bos);
        Assert.assertEquals(bytes[0], bos.toByteArray()[0]);
        Assert.assertEquals(bytes[1], bos.toByteArray()[1]);
        Assert.assertEquals(bytes[2], bos.toByteArray()[2]);
    }
    
    @Test
    public void delete() throws Exception
    {
        SolrContentStore store = new SolrContentStore(rootStr);
        
        ContentContext ctx = createContentContext("abc");
        String url = ctx.getContentUrl();
        ContentWriter writer = store.getWriter(ctx);
        writer.putContent("Content goes here.");
        
        // Check the reader
        ContentReader reader = store.getReader(url);
        Assert.assertNotNull(reader);
        Assert.assertTrue(reader.exists());
        
        // Delete
        store.delete(url);
        reader = store.getReader(url);
        Assert.assertNotNull(reader);
        Assert.assertFalse(reader.exists());
        
        // Delete when already gone; should just not fail
        store.delete(url);
    }
//
//    See ACE-2896.  There is actually no way of ensuring that the cached document is latest or perfect.
//    /**
//     * This store allows the same URL to be used but does redirection to the latest version under the covers
//     */
//    @Test
//    public void rewrite() throws Exception
//    {
//        SolrContentStore store = new SolrContentStore(rootStr);
//        
//        ContentContext ctx = createContentContext("abc");
//        ContentWriter writer1 = store.getWriter(ctx);
//        ContentWriter writer2 = store.getWriter(ctx);
//        assertNotEquals(
//                "Different writers should use different URLs: writer1=" + writer1 + ", writer2=" + writer2,
//                writer1.getContentUrl(), writer2.getContentUrl());
//        assertTrue(
//                "Second URL must be 'greater' than first: writer1=" + writer1 + ", writer2=" + writer2,
//                writer1.getContentUrl().compareTo(writer2.getContentUrl()) < 0);
//        
//        writer1.putContent("Text1");
//        writer2.putContent("Text2");
//        
//        // Now get the reader
//        ContentReader reader = store.getReader(ctx.getContentUrl());
//        assertEquals("Text2", reader.getContentString());
//    }
    
    /**
     * A demonstration of how the store might be used.
     */
    @Test
    public void exampleUsage()
    {
        SolrContentStore store = new SolrContentStore(rootStr);
        
        String tenant = "alfresco.com";
        long dbId = 12345;
        String otherData = "sdfklsfdl";
        
        ContentContext ctxWrite = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_DB_ID, String.valueOf(dbId))
                .add(SolrContentUrlBuilder.KEY_TENANT, tenant)
                .add("otherData", otherData)
                .getContentContext();
        ContentWriter writer = store.getWriter(ctxWrite);
        writer.putContent("a document in plain text");
        
        // The URL can be reliably rebuilt in any order
        String urlRead = SolrContentUrlBuilder
                .start()
                .add("otherData", otherData)
                .add(SolrContentUrlBuilder.KEY_TENANT, tenant)
                .add(SolrContentUrlBuilder.KEY_DB_ID, String.valueOf(dbId))
                .get();
        ContentReader reader = store.getReader(urlRead);
        String documentText = reader.getContentString();
        
        Assert.assertEquals("a document in plain text", documentText);
    }
}
