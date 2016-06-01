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

import java.io.UnsupportedEncodingException;
import java.util.Map;
import java.util.TreeMap;
import java.util.zip.CRC32;

import org.alfresco.repo.content.ContentContext;
import org.alfresco.repo.content.ContentStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility class that wraps up the creation of SOLR content URLs given arbitrary,
 * string-based metadata.
 * <p/>
 * The URL is constructed from a 19 digit number (zero-padded long), which is built from
 * the ACL ID, the DB ID or a CRC32 of the provided metadata, and a numerical version starting with "000".<br/>
 * For example, the DB ID "4775808" will generate
 * <tt>"someprefix://<tenant>/db/4775/808.gz"</tt><br/>
 * The 
 * 
 * @author Derek Hulley
 * @since 5.0
 */
public class SolrContentUrlBuilder
{
    /**
     * <b>solr</b> is the prefix for SOLR content URLs
     * @see #isContentUrlSupported(String)
     */
    public static final String SOLR_PROTOCOL = "solr";
    public static final String SOLR_PROTOCOL_PREFIX = SOLR_PROTOCOL + ContentStore.PROTOCOL_DELIMITER;
    public static final String FILE_EXTENSION = ".gz";

    /** The key for the tenant name */
    public static final String KEY_TENANT = "tenant";
    /** The key for the DB ID */
    public static final String KEY_DB_ID = "dbId";
    /** The key for the ACL ID */
    public static final String KEY_ACL_ID = "aclId";
    
    protected final static Logger logger = LoggerFactory.getLogger(SolrContentUrlBuilder.class);
    
    /** Metadata ordered by key */
    private final TreeMap<String, String> metadata;
    
    /**
     * Protected constructor used by {@link SolrContentUrlBuilder#start()}
     */
    protected SolrContentUrlBuilder()
    {
        this.metadata = new TreeMap<String, String>();
    }
    
    /**
     * Factory method to start building the SOLR content URL.
     * 
     * @return              an instance of the builder
     */
    public static SolrContentUrlBuilder start()
    {
        return new SolrContentUrlBuilder();
    }
    
    /**
     * Add some metadata to the URL generator.  The order in which metadata is added is irrelevant.
     * <p/>
     * Note that there are specific keys that are commonly used and, if provided, may not be null or empty.
     * <ul>
     *   <li><b>{@link #KEY_TENANT}:</b>    The name of the tenant or 'default' if missing.</li>
     *   <li><b>{@link #KEY_DB_ID}:</b>     The database ID.</li>
     *   <li><b>{@link #KEY_ACL_ID}:</b>    The ACL ID.</li>
     * </ul>
     * 
     * @param key           an arbitrary metadata key (never <tt>null</tt>>
     * @param value         some metadata value (<tt>null</tt> is supported)
     * @return              this builder for more building
     * 
     * @throws              IllegalArgumentException if the key is null
     * @throws              IllegalStateException if the key has been used already
     */
    public synchronized SolrContentUrlBuilder add(String key, String value)
    {
        if (key == null)
        {
            throw new IllegalArgumentException("The metadata 'key' may not be null.");
        }
        String previous = metadata.put(key, value);
        if (previous != null)
        {
            throw new IllegalStateException("The metadata key, '" + key + "', has already been used.");
        }
        // Check well-known keys
        if (key.equals(KEY_TENANT) || key.equals(KEY_DB_ID) || key.equals(KEY_ACL_ID))
        {
            if (value == null || value.length() == 0)
            {
                throw new IllegalArgumentException("Invalid value for key '" + key + "': " + value);
            }
        }
        // Store metadata
        metadata.put(key, value);
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Appending SOLR metadata: " + key + " - " + value);
        }
        return this;
    }
    
    /**
     * Get the final content URL using the {@link #add(String, String) supplied metadata}.
     * 
     * @return              the SOLR content URL
     * @throws              IllegalStateException if no metadata has been added
     */
    public synchronized String get()
    {
        if (metadata.size() == 0)
        {
            throw new IllegalStateException("No metadata added.  Usage add.");
        }
        
        StringBuilder sb = new StringBuilder(72);
        // Is there a 'tenant'?
        String tenant = metadata.get(KEY_TENANT);
        if (tenant == null)             // We checked it for length before
        {
            tenant = "default";
        }
        sb.append(SOLR_PROTOCOL_PREFIX).append(tenant).append("/");

        // Build a numeric value using the CRC and special IDs, if available
        StringBuilder numSb = new StringBuilder(52);
        if (metadata.containsKey(KEY_DB_ID))
        {
            sb.append("db/");
            // We have a unique DB ID, which can be used by itself
            numSb.append(metadata.get(KEY_DB_ID));
        }
        else if (metadata.containsKey(KEY_ACL_ID))
        {
            sb.append("acl/");
            // We have a unique ACL ID, which can be used completely
            numSb.append(metadata.get(KEY_ACL_ID));
        }
        else
        {
            sb.append("misc/");
            // Calculate the CRC
            CRC32 crc = new CRC32();
            try
            {
                for (Map.Entry<String, String> entry : metadata.entrySet())
                {
                    // This is ordered, so just add each entry as "key = value".
                    // DO NOT USE entry.toString() because the format is not a contract
                    // and we have to have the same string for the same metadata
                    String entryStr = entry.getKey() + "=" + entry.getValue() + "; ";
                    crc.update(entryStr.getBytes("UTF-8"));
                }
            }
            catch (UnsupportedEncodingException e)
            {
                // Yeah, right.
                throw new RuntimeException("UTF-8 is not supported.", e);
            }
            numSb.append(crc.getValue());
        }
        String numStr = numSb.toString();
        
        // We use 3 characters at a time from the CRC, which gives up to 999 entries per path element of the URL
        int pathCharCount = 0;
        for (int i = 0; i < numStr.length(); i++)
        {
            // If we have 4 chars in a path part (and we have more chars) then we add a separator
            if (pathCharCount == 4)
            {
                sb.append("/");
                pathCharCount = 0;
            }
            // Append the char
            sb.append(numStr.charAt(i));
            pathCharCount++;
        }
        // We always have a numeric value ending, never '/'.  That's it.  Just give it an extension.
        sb.append(FILE_EXTENSION);
        String url = sb.toString();
        
        // Done
        if (logger.isDebugEnabled())
        {
            logger.debug("Converted SOLR metadata to URL: " + url + "  -- " + metadata.toString());
        }
        return url;
    }
    
    /**
     * Helper method to retrieve a {@link ContentContext} constructed using the final {@link #get()} url.
     */
    public ContentContext getContentContext()
    {
        String url = get();
        return new ContentContext(null, url);
    }
}
