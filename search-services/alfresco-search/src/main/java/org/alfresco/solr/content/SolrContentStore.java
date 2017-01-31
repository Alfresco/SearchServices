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
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import org.alfresco.repo.content.ContentContext;
import org.alfresco.repo.content.ContentStore;
import org.alfresco.service.cmr.repository.ContentIOException;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.config.ConfigUtil;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.core.SolrResourceLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A content store specific to SOLR's requirements: The URL is generated from a
 * set of properties such as:
 * <ul>
 * <li>ACL ID</li>
 * <li>DB ID</li>
 * <li>Other metadata</li>
 * </ul>
 * The URL, if not known, can be reliably regenerated using the
 * {@link SolrContentUrlBuilder}.
 * 
 * @author Derek Hulley
 * @author Michael Suzuki
 * @since 5.0
 */
public class SolrContentStore implements ContentStore
{
    protected final static Logger log = LoggerFactory.getLogger(SolrContentStore.class);
    private static final String CONTENT_STORE = "contentstore";
    /**
     * Constructor.
     * @param solrHome
     */
    public SolrContentStore(String solrHome)
    {
        if (solrHome == null || solrHome.isEmpty())
        {
            throw new RuntimeException("Path to SOLR_HOME is required");
        }

        File solrHomeFile = new File(SolrResourceLoader.normalizeDir(solrHome));
        if (!solrHomeFile.exists()) {
            //Its very unlikely that solrHome would not exist so we will log an error
            //but continue because solr.content.dir may be specified, so it keeps working
            log.error(solrHomeFile.getAbsolutePath()+ " does not exist.");
        }
        String path =  solrHomeFile.getParent()+"/"+CONTENT_STORE;
        File rootFile = new File(ConfigUtil.locateProperty("solr.content.dir", path));

        try
        {
            FileUtils.forceMkdir(rootFile);
        } 
        catch (Exception e)
        {
            throw new RuntimeException("Failed to create directory for content store: " + rootFile, e);
        }
        this.root = rootFile.getAbsolutePath();
    }

    // write a BytesRef as a byte array
    private JavaBinCodec.ObjectResolver resolver = new JavaBinCodec.ObjectResolver()
    {
        @Override public Object resolve(Object o,JavaBinCodec codec)throws IOException
        {
            if(o instanceof BytesRef)
            {
                BytesRef br=(BytesRef)o;
                codec.writeByteArray(br.bytes,br.offset,br.length);
                return null;
            }
            return o;
        }
    };

    /**
     * Retrieve document from SolrContentStore.
     * @param tenant identifier
     * @param dbId identifier
     * @return {@link SolrInputDocument} searched document
     * @throws IOException if error
     */
    public SolrInputDocument retrieveDocFromSolrContentStore(String tenant, long dbId) throws IOException
    {
        String contentUrl = SolrContentUrlBuilder.start().add(SolrContentUrlBuilder.KEY_TENANT, tenant)
                .add(SolrContentUrlBuilder.KEY_DB_ID, String.valueOf(dbId)).get();
        ContentReader reader = this.getReader(contentUrl);
        SolrInputDocument cachedDoc = null;
        if (reader.exists())
        {
            // try-with-resources statement closes all these InputStreams
            try (InputStream contentInputStream = reader.getContentInputStream();
                    // Uncompresses the document
                    GZIPInputStream gzip = new GZIPInputStream(contentInputStream);)
            {
                cachedDoc = (SolrInputDocument) new JavaBinCodec(resolver).unmarshal(gzip);
            } catch (Exception e)
            {
                // Don't fail for this
                log.warn("Failed to get doc from store using URL: " + contentUrl, e);
                return null;
            }
        }
        return cachedDoc;
    }

    private final String root;

    @Override
    public boolean isContentUrlSupported(String contentUrl)
    {
        return (contentUrl != null && contentUrl.startsWith(SolrContentUrlBuilder.SOLR_PROTOCOL_PREFIX));
    }


    /**
     * @return <tt>true</tt> always
     */
    @Override
    public boolean isWriteSupported()
    {
        return true;
    }

    /**
     * @return -1 always
     */
    @Override
    public long getSpaceFree()
    {
        return -1L;
    }

    /**
     * @return -1 always
     */
    @Override
    public long getSpaceTotal()
    {
        return -1L;
    }

    @Override
    public String getRootLocation()
    {
        return root;
    }

    /**
     * Convert a content URL into a File, whether it exists or not
     */
    private File getFileFromUrl(String contentUrl)
    {
        String path = contentUrl.replace(SolrContentUrlBuilder.SOLR_PROTOCOL_PREFIX, root + "/");
        return new File(path);
    }

    @Override
    public boolean exists(String contentUrl)
    {
        File file = getFileFromUrl(contentUrl);
        return file.exists();
    }

    @Override
    public ContentReader getReader(String contentUrl)
    {
        File file = getFileFromUrl(contentUrl);
        return new SolrFileContentReader(file, contentUrl);
    }

    @Override
    public ContentWriter getWriter(ContentContext context)
    {
        // Ensure that there is a context and that it has a URL
        if (context == null || context.getContentUrl() == null)
        {
            throw new IllegalArgumentException("Retrieve a writer with a URL-providing ContentContext.");
        }
        String url = context.getContentUrl();
        File file = getFileFromUrl(url);
        SolrFileContentWriter writer = new SolrFileContentWriter(file, url);
        // Done
        return writer;
    }

    @Override
    public boolean delete(String contentUrl)
    {
        File file = getFileFromUrl(contentUrl);
        return file.delete();
    }
    /**
     * Stores a {@link SolrInputDocument} into Alfresco solr content store.
     * @param tenant
     * @param dbId
     * @param doc
     * @throws IOException
     */
    public void storeDocOnSolrContentStore(String tenant, long dbId, SolrInputDocument doc) throws IOException
    {
        ContentContext contentContext = SolrContentUrlBuilder
                    .start()
                    .add(SolrContentUrlBuilder.KEY_TENANT, tenant)
                    .add(SolrContentUrlBuilder.KEY_DB_ID, String.valueOf(dbId))
                .getContentContext();
        this.delete(contentContext.getContentUrl());
        ContentWriter writer = this.getWriter(contentContext);
        if (log.isDebugEnabled())
        {
            log.debug("Writing doc to " + contentContext.getContentUrl());
        }
        try (
                    OutputStream contentOutputStream = writer.getContentOutputStream();
                    // Compresses the document
                    GZIPOutputStream gzip = new GZIPOutputStream(contentOutputStream);
            )
        {
            JavaBinCodec codec = new JavaBinCodec(resolver);
            codec.marshal(doc, gzip);
        }
        catch (Exception e)
        {
            // A failure to write to the store is acceptable as long as it's logged
            log.warn("Failed to write to store using URL: " + contentContext.getContentUrl(), e);
        }
    }
    /**
     * Store {@link SolrInputDocument} in to Alfresco solr content store.
     * @param nodeMetaData identifier
     * @param doc to store
     * @throws IOException if error
     */
    public void storeDocOnSolrContentStore(NodeMetaData nodeMetaData, SolrInputDocument doc) throws IOException
    {
        String fixedTenantDomain = AlfrescoSolrDataModel.getTenantId(nodeMetaData.getTenantDomain());
        storeDocOnSolrContentStore(fixedTenantDomain, nodeMetaData.getId(), doc);
    }
    /**
     * Removes {@link SolrInputDocument} from Alfresco solr content store.
     * @param nodeMetaData
     */
    public void removeDocFromContentStore(NodeMetaData nodeMetaData)
    {
        String fixedTenantDomain = AlfrescoSolrDataModel.getTenantId(nodeMetaData.getTenantDomain());
        String contentUrl = SolrContentUrlBuilder
                    .start()
                    .add(SolrContentUrlBuilder.KEY_TENANT, fixedTenantDomain)
                    .add(SolrContentUrlBuilder.KEY_DB_ID, String.valueOf(nodeMetaData.getId()))
                    .getContentContext()
                .getContentUrl();
        this.delete(contentUrl);
    }

}
