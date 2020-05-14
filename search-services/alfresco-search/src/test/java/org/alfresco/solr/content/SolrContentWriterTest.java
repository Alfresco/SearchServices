/*-
 * #%L
 * Alfresco Solr Search
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
package org.alfresco.solr.content;

import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;

public class SolrContentWriterTest
{
    private String solrHome = new File("./target/contentwriter/").getAbsolutePath();

    @After
    public void tearDown() throws IOException
    {
        File rootDir = new File(solrHome);
        FileUtils.deleteDirectory(rootDir);
    }

    private ContentWriter getContentWriter(String name)
    {
        return new SolrFileContentWriter(new File(solrHome, name), solrHome + "/" + name);
    }

    private ContentReader getContentReader(String name)
    {
        return new SolrFileContentReader(new File(solrHome, name), solrHome + "/" + name);
    }

    @Test
    public void contentByString()
    {
        String filename = "abc";
        ContentWriter writer = getContentWriter(filename);

        File file = new File(solrHome, filename);
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
        ContentReader reader = getContentReader(filename);
        Assert.assertNotNull(reader);
        Assert.assertTrue(reader.exists());

        Assert.assertEquals(content, reader.getContentString());
    }

    @Test
    public void contentByStream()
    {

        String filename = "cbs";
        ContentWriter writer = getContentWriter(filename);

        byte[] bytes = new byte[] { 1, 7, 13 };
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        writer.putContent(bis);

        // Now get the reader
        ContentReader reader = getContentReader(filename);

        ByteArrayOutputStream bos = new ByteArrayOutputStream(3);
        reader.getContent(bos);
        Assert.assertEquals(bytes[0], bos.toByteArray()[0]);
        Assert.assertEquals(bytes[1], bos.toByteArray()[1]);
        Assert.assertEquals(bytes[2], bos.toByteArray()[2]);
    }
}
