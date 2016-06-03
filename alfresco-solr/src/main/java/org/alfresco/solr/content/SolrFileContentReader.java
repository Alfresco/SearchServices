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

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.util.Locale;

import org.alfresco.service.cmr.repository.ContentData;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentStreamListener;
import org.apache.commons.io.FileUtils;
import org.springframework.util.FileCopyUtils;

/**
 * Bare-bones implementation of the reader for SOLR purposes
 * 
 * @author Derek Hulley
 * @since 5.0
 */
public class SolrFileContentReader implements ContentReader
{
    private final File file;
    private final String contentUrl;
    
    /**
     * @param file          the file to write to
     * @param contentUrl    the content URL for information purposes
     */
    protected SolrFileContentReader(File file, String contentUrl)
    {
        this.file = file;
        this.contentUrl = contentUrl;
    }

    @Override
    public String toString()
    {
        return "SolrFileContentReader [file=" + file + "]";
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
    public FileChannel getFileChannel() throws ContentIOException
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean exists()
    {
        return file.exists();
    }

    @Override
    public ReadableByteChannel getReadableChannel() throws ContentIOException
    {
        throw new UnsupportedOperationException("Auto-created method not implemented.");
    }

    @Override
    public synchronized InputStream getContentInputStream() throws ContentIOException
    {
        if (!file.exists())
        {
            throw new IllegalStateException("The file does not exist: " + file);
        }
        try
        {
            InputStream is = new BufferedInputStream(new FileInputStream(file));
            // done
            return is;
        }
        catch (Throwable e)
        {
            throw new ContentIOException("Failed to open stream onto file: " + file, e);
        }
    }

    @Override
    public synchronized void getContent(OutputStream os) throws ContentIOException
    {
        if (!file.exists())
        {
            throw new IllegalStateException("The file does not exist: " + file);
        }
        try
        {
            FileUtils.copyFile(file, os);
        }
        catch (Throwable e)
        {
            throw new ContentIOException("Failed to copy stream onto file: " + file, e);
        }
    }
    
    @Override
    public synchronized void getContent(File targetFile) throws ContentIOException
    {
        if (!this.file.exists())
        {
            throw new IllegalStateException("The file does not exist: " + this.file);
        }
        else if (targetFile.exists())
        {
            throw new IllegalStateException("The target file already exists: " + targetFile);
        }
        try
        {
            FileUtils.copyFile(this.file, targetFile, false);
        }
        catch (Throwable e)
        {
            throw new ContentIOException("Failed to copy stream onto file: " + targetFile, e);
        }
    }
    
    @Override
    public String getContentString(int length) throws ContentIOException
    {
        String str = getContentString();
        if (str.length() > length)
        {
            return str.substring(0, length - 1);
        }
        else
        {
            return str;
        }
    }

    @Override
    public final String getContentString() throws ContentIOException
    {
        try
        {
            // read from the stream into a byte[]
            InputStream is = getContentInputStream();
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            FileCopyUtils.copy(is, os);  // both streams are closed
            byte[] bytes = os.toByteArray();
            // get the encoding for the string
            String encoding = "UTF-8";
            // create the string from the byte[] using encoding if necessary
            String content = (encoding == null) ? new String(bytes) : new String(bytes, encoding);
            // done
            return content;
        }
        catch (IOException e)
        {
            throw new ContentIOException("Failed to copy content to string: \n" +
                    "   accessor: " + this,
                    e);
        }
    }
    
    @Override
    public long getLastModified()
    {
        return file.lastModified();
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
