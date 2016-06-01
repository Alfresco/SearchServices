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

import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Locale;

import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.apache.commons.io.FileUtils;

/**
 * Bare-bones implementation of the writer for SOLR purposes
 * 
 * @author Derek Hulley
 * @since 5.0
 */
public class SolrFileContentWriter implements ContentWriter
{
    private final File file;
    private final String contentUrl;
    private boolean written;
    
    /**
     * @param file          the file to write to
     * @param contentUrl    the content URL for information purposes
     */
    protected SolrFileContentWriter(File file, String contentUrl)
    {
        this.file = file;
        this.contentUrl = contentUrl;
        this.written = false;
    }
    
    @Override
    public String toString()
    {
        return "SolrFileContentWriter [file=" + file + "]";
    }

    @Override
    public long getSize()
    {
        if (file.exists())
        {
            return file.length();
        }
        else
        {
            return 0L;
        }
    }

    @Override
    public final ContentReader getReader() throws ContentIOException
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public synchronized final boolean isClosed()
    {
        throw new UnsupportedOperationException();
    }

    public synchronized boolean isChannelOpen()
    {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public synchronized final WritableByteChannel getWritableChannel() throws ContentIOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public FileChannel getFileChannel(boolean truncate) throws ContentIOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized OutputStream getContentOutputStream() throws ContentIOException
    {
        if (written == true)
        {
            throw new IllegalStateException("The writer has already been used: " + file);
        }
        else if (file.exists())
        {
            throw new IllegalStateException("The file already exists: " + file);
        }
        try
        {
            OutputStream is = new BufferedOutputStream(FileUtils.openOutputStream(file));
            written = true;
            // done
            return is;
        }
        catch (Throwable e)
        {
            throw new ContentIOException("Failed to open stream onto file: " + file, e);
        }
    }

    @Override
    public void putContent(ContentReader reader) throws ContentIOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public synchronized void putContent(InputStream is) throws ContentIOException
    {
        if (written == true)
        {
            throw new IllegalStateException("The writer has already been used: " + file);
        }
        else if (file.exists())
        {
            throw new IllegalStateException("The file already exists: " + file);
        }
        try
        {
            FileUtils.copyInputStreamToFile(is, file);
            written = true;
        }
        catch (Throwable e)
        {
            throw new ContentIOException("Failed to copy stream onto file: " + file, e);
        }
    }
    
    @Override
    public synchronized void putContent(File sourceFile) throws ContentIOException
    {
        if (written == true)
        {
            throw new IllegalStateException("The writer has already been used: " + this.file);
        }
        else if (this.file.exists())
        {
            throw new IllegalStateException("The file already exists: " + this.file);
        }
        else if (!sourceFile.exists())
        {
            throw new IllegalStateException("The source file does not exist: " + sourceFile);
        }
        try
        {
            FileUtils.copyFile(sourceFile, this.file, false);
            written = true;
        }
        catch (Throwable e)
        {
            throw new ContentIOException("Failed to copy file onto file: " + sourceFile, e);
        }
    }
    
    @Override
    public synchronized void putContent(String content) throws ContentIOException
    {
        try
        {
            // attempt to use the correct encoding
            String encoding = "UTF-8";
            byte[] bytes = content.getBytes(encoding);

            // get the stream
            ByteArrayInputStream is = new ByteArrayInputStream(bytes);
            putContent(is);
            // done
        }
        catch (IOException e)
        {
            throw new ContentIOException("Failed to copy content from string: \n" +
                    "   writer: " + this +
                    "   content length: " + content.length(),
                    e);
        }
    }
    
    @Override
    public void guessEncoding()
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void guessMimetype(String filename)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addListener(ContentStreamListener listener)
    {
        throw new UnsupportedOperationException("Auto-created method not implemented.");
    }

    @Override
    public ContentData getContentData()
    {
        throw new UnsupportedOperationException("Auto-created method not implemented.");
    }

    @Override
    public String getContentUrl()
    {
        return contentUrl;
    }

    @Override
    public String getMimetype()
    {
        throw new UnsupportedOperationException("Auto-created method not implemented.");
    }

    @Override
    public void setMimetype(String mimetype)
    {
        throw new UnsupportedOperationException("Auto-created method not implemented.");
    }

    @Override
    public String getEncoding()
    {
        throw new UnsupportedOperationException("Auto-created method not implemented.");
    }

    @Override
    public void setEncoding(String encoding)
    {
        throw new UnsupportedOperationException("Auto-created method not implemented.");
    }

    @Override
    public Locale getLocale()
    {
        throw new UnsupportedOperationException("Auto-created method not implemented.");
    }

    @Override
    public void setLocale(Locale locale)
    {
        throw new UnsupportedOperationException("Auto-created method not implemented.");
    }
}
