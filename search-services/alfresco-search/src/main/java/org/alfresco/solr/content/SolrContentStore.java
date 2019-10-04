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

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.alfresco.solr.content.SolrContentUrlBuilder.FILE_EXTENSION;
import static org.alfresco.solr.content.SolrContentUrlBuilder.logger;

import org.alfresco.repo.content.ContentContext;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.config.ConfigUtil;
import org.alfresco.solr.handler.ReplicationHandler;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.util.JavaBinCodec;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.SnapShooter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * A content store specific to SOLR's requirements: The URL is generated from a
 * set of properties such as:
 * <ul>
 *  <li>ACL ID</li>
 *  <li>DB ID</li>
 *  <li>Other metadata</li>
 * </ul>
 *
 * The URL, if not known, can be reliably regenerated using the {@link SolrContentUrlBuilder}.
 * 
 * @author Derek Hulley
 * @author Michael Suzuki
 * @author Andrea Gazzarini
 * @since 5.0
 */
public class SolrContentStore implements Closeable, ReplicationRole
{
    protected final static Logger log = LoggerFactory.getLogger(SolrContentStore.class);

    static final long NO_VERSION_AVAILABLE = -1L;
    public static final long NO_CONTENT_STORE_REPLICATION_REQUIRED = -2L;

    static final String CONTENT_STORE = "contentstore";
    static final String SOLR_CONTENT_DIR = "solr.content.dir";
    private static final String VERSION_FILE = ".version";

    public static final String DELETES = "deletes";
    public static final String ADDS = "adds";

    private final Predicate<File> onlyDatafiles = file -> file.isFile() && file.getName().endsWith(FILE_EXTENSION);
    private final String root;

    private final ReplicationRole slave = new ReplicationRole()
    {
        @Override
        public ReplicationRole enableMasterMode()
        {
            return master.enableMasterMode();
        }

        @Override
        public long getLastCommittedVersion()
        {
            try
            {
                return Files.lines(Paths.get(root, VERSION_FILE))
                        .map(Long::parseLong)
                        .findFirst()
                        .orElse(NO_VERSION_AVAILABLE);
            }
            catch (IOException e)
            {
                return NO_VERSION_AVAILABLE;
            }
        }

        @Override
        public void setLastCommittedVersion(long version)
        {
            try
            {
                File tmpFile = new File(root, ".version-" + new SimpleDateFormat(SnapShooter.DATE_FMT, Locale.ROOT).format(new Date()));
                FileWriter wr = new FileWriter(tmpFile);
                wr.write(Long.toString(version));
                wr.close();

                tmpFile.renameTo(new File(root, ".version"));
            }
            catch (IOException exception)
            {
                logger.error("Unable to persist the last committed content store version {}. See the stacktrace below for furtger details.", version, exception);
            }
        }

        @Override
        public Map<String, List<Map<String, Object>>> getChanges(long version)
        {
            return emptyMap();
        }

        @Override
        public void storeDocOnSolrContentStore(String tenant, long dbId, SolrInputDocument doc)
        {
            logger.warn("NoOp SolrContentStore write call on slave side: this shouldn't happen because the ContentStore is in read-only mode when the hosting node is a slave.");
        }

        @Override
        public void storeDocOnSolrContentStore(NodeMetaData nodeMetaData, SolrInputDocument doc)
        {
            logger.warn("NoOp SolrContentStore write call on slave side: this shouldn't happen because the ContentStore is in read-only mode when the hosting node is a slave.");
        }

        @Override
        public void removeDocFromContentStore(NodeMetaData nodeMetaData)
        {
            logger.warn("NoOp SolrContentStore write call on slave side: this shouldn't happen because the ContentStore is in read-only mode when the hosting node is a slave.");
        }

        @Override
        public void flushChangeSet()
        {
            logger.warn("NoOp ChangeSet tracking call on slave side: this shouldn't happen because the ContentStore is in read-only mode when the hosting node is a slave.");
        }

        @Override
        public void close()
        {
            // There's nothing to close on slave side
        }
    };

    private final ReplicationRole master = new ReplicationRole()
    {
        private ChangeSet changeSet;

        @Override
        public ReplicationRole enableMasterMode()
        {
            changeSet = ofNullable(changeSet).orElseGet(() -> new ChangeSet.Builder().withContentStoreRoot(root).build());
            return master;
        }

        @Override
        public long getLastCommittedVersion()
        {
            return changeSet.getLastCommittedVersion();
        }

        @Override
        public void setLastCommittedVersion(long version)
        {
            // Do nothing here, as we are on master-side
        }

        @Override
        public Map<String, List<Map<String, Object>>> getChanges(long version)
        {
            if (version == NO_VERSION_AVAILABLE)
            {
                return Map.of(
                        "adds", fullContentStore(),
                        "deletes", emptyList());
            }

            ChangeSet requestedVersion = changeSet.since(version);

            return Map.of(
                    DELETES,
                    requestedVersion.deletes.stream()
                            .map(path -> Map.<String, Object>of("name", path))
                            .collect(toList()),
                    ADDS,
                    requestedVersion.adds.stream()
                            .map(relativePath -> root + relativePath)
                            .map(File::new)
                            .map(file -> new ReplicationHandler.FileInfo(file, file.getAbsolutePath().replace(root, "")))
                            .map(ReplicationHandler.FileInfo::getAsMap)
                            .collect(toList()));
        }

        @Override
        public void storeDocOnSolrContentStore(String tenant, long dbId, SolrInputDocument doc)
        {
            ContentContext contentContext =
                    of(SolrContentUrlBuilder
                            .start()
                            .add(SolrContentUrlBuilder.KEY_TENANT, tenant)
                            .add(SolrContentUrlBuilder.KEY_DB_ID, String.valueOf(dbId)))
                            .map(SolrContentUrlBuilder::getContentContext)
                            .orElseThrow(() -> new IllegalArgumentException("Unable to build a Content Context from tenant " + tenant + " and DBID " + dbId));

            this.delete(contentContext.getContentUrl());

            ContentWriter writer = this.getWriter(contentContext);

            log.debug("Writing {}/{} to {}", tenant, dbId, contentContext.getContentUrl());

            try (OutputStream contentOutputStream = writer.getContentOutputStream();
                 GZIPOutputStream gzip = new GZIPOutputStream(contentOutputStream))
            {
                JavaBinCodec codec = new JavaBinCodec(resolver);
                codec.marshal(doc, gzip);

                File file = getFileFromUrl(contentContext.getContentUrl());
                changeSet.addOrReplace(relativePath(file));
            }
            catch (Exception exception)
            {
                log.warn("Unable to write to Content Store using URL: {}", contentContext.getContentUrl(), exception);
            }
        }

        @Override
        public void storeDocOnSolrContentStore(NodeMetaData nodeMetaData, SolrInputDocument doc) {
            String fixedTenantDomain = AlfrescoSolrDataModel.getTenantId(nodeMetaData.getTenantDomain());
            storeDocOnSolrContentStore(fixedTenantDomain, nodeMetaData.getId(), doc);
        }

        @Override
        public void removeDocFromContentStore(NodeMetaData nodeMetaData)
        {
            String fixedTenantDomain = AlfrescoSolrDataModel.getTenantId(nodeMetaData.getTenantDomain());
            String contentUrl = SolrContentUrlBuilder
                    .start()
                    .add(SolrContentUrlBuilder.KEY_TENANT, fixedTenantDomain)
                    .add(SolrContentUrlBuilder.KEY_DB_ID, String.valueOf(nodeMetaData.getId()))
                    .getContentContext()
                    .getContentUrl();
            delete(contentUrl);
        }

        @Override
        public void flushChangeSet() throws IOException {
            changeSet.flush();
        }

        @Override
        public void close()
        {
            changeSet.close();
        }

        private List<Map<String, Object>> fullContentStore()
        {
            try
            {
                return Files.walk(Paths.get(root))
                        .map(Path::toFile)
                        .filter(onlyDatafiles)
                        .map(file -> new ReplicationHandler.FileInfo(file, file.getAbsolutePath().replace(root, "")))
                        .map(ReplicationHandler.FileInfo::getAsMap)
                        .collect(toList());
            }
            catch (Exception e)
            {
                log.error("An exception occurred while retrieving the whole ContentStore filelist. " +
                        "As consequence of that an empty list will be returned (i.e. no ContentStore synch will happen).");
                return emptyList();
            }
        }

        private boolean delete(String contentUrl)
        {
            File file = getFileFromUrl(contentUrl);
            boolean deleted = file.delete();

            if (deleted) changeSet.delete(relativePath(file));

            return deleted;
        }

        private ContentWriter getWriter(ContentContext context)
        {
            String url = context.getContentUrl();
            File file = getFileFromUrl(url);
            return new SolrFileContentWriter(file, url);
        }
    };

    private ReplicationRole currentRole = ReplicationRole.NO_OP;

    private final JavaBinCodec.ObjectResolver resolver = (o, codec) -> {
        if(o instanceof BytesRef)
        {
            BytesRef br = (BytesRef)o;
            codec.writeByteArray(br.bytes,br.offset,br.length);
            return null;
        }
        return o;
    };

    /**
     * Builds a new {@link SolrContentStore} instance with the given SOLR HOME.
     *
     * @param solrHome the Solr HOME.
     */
    public SolrContentStore(String solrHome)
    {
        if (solrHome == null || solrHome.isEmpty())
        {
            throw new RuntimeException("Path to SOLR_HOME is required");
        }

        File solrHomeFile = new File(SolrResourceLoader.normalizeDir(solrHome));
        if (!solrHomeFile.exists())
        {
            //Its very unlikely that solrHome would not exist so we will log an error
            //but continue because solr.content.dir may be specified, so it keeps working
            log.error(solrHomeFile.getAbsolutePath() + " does not exist.");
        }

        String path = solrHomeFile.getParent() + "/" + CONTENT_STORE;
        log.warn(path + " will be used as a default path if " + SOLR_CONTENT_DIR + " property is not defined");
        File rootFile = new File(ConfigUtil.locateProperty(SOLR_CONTENT_DIR, path));

        try
        {
            FileUtils.forceMkdir(rootFile);
        }
        catch (Exception e) {
            throw new RuntimeException("Failed to create directory for content store: " + rootFile, e);
        }

        this.root = rootFile.getAbsolutePath();
    }

    /**
     * Returns the content store changes since the given (requestor) version.
     *
     * @param version the requestor version.
     * @return the content store changes since the given (requestor) version.
     */
    @Override
    public Map<String, List<Map<String, Object>>> getChanges(long version)
    {
        return currentRole.getChanges(version);
    }

    /**
     * Retrieve document from SolrContentStore.
     *
     * @param tenant identifier
     * @param dbId identifier
     * @return {@link SolrInputDocument} searched document
     */
    public SolrInputDocument retrieveDocFromSolrContentStore(String tenant, long dbId)
    {
        String contentUrl =
                SolrContentUrlBuilder.start()
                        .add(SolrContentUrlBuilder.KEY_TENANT, tenant)
                        .add(SolrContentUrlBuilder.KEY_DB_ID, String.valueOf(dbId))
                        .get();

        ContentReader reader = this.getReader(contentUrl);
        if (!reader.exists())
        {
            return null;
        }

        try (InputStream contentInputStream = reader.getContentInputStream();
             InputStream gzip = new GZIPInputStream(contentInputStream))
        {
            return (SolrInputDocument) new JavaBinCodec(resolver).unmarshal(gzip);
        }
        catch (Exception exception)
        {
            // Don't fail for this
            log.warn("Failed to get doc from store using URL: " + contentUrl, exception);
            return null;
        }
    }

    @Override
    public long getLastCommittedVersion()
    {
        return currentRole.getLastCommittedVersion();
    }

    @Override
    public void setLastCommittedVersion(long version)
    {
        currentRole.setLastCommittedVersion(version);
    }

    /**
     * Returns the absolute path of the content store root folder.
     *
     * @return the absolute path of the content store root folder.
     */
    public String getRootLocation()
    {
        return root;
    }

    public boolean exists(String contentUrl)
    {
        File file = getFileFromUrl(contentUrl);
        return file.exists();
    }

    @Override
    public void storeDocOnSolrContentStore(String tenant, long dbId, SolrInputDocument doc)
    {
        currentRole.storeDocOnSolrContentStore(tenant, dbId, doc);
    }

    /**
     * Store {@link SolrInputDocument} in to Alfresco solr content store.
     *
     * @param nodeMetaData the incoming node metadata.
     * @param doc the document itself.
     */
    @Override
    public void storeDocOnSolrContentStore(NodeMetaData nodeMetaData, SolrInputDocument doc)
    {
        currentRole.storeDocOnSolrContentStore(nodeMetaData, doc);
    }

    /**
     * Removes {@link SolrInputDocument} from Alfresco solr content store.
     *
     * @param nodeMetaData the incoming node metadata.
     */
    @Override
    public void removeDocFromContentStore(NodeMetaData nodeMetaData)
    {
        currentRole.removeDocFromContentStore(nodeMetaData);
    }

    @Override
    public void flushChangeSet() throws IOException
    {
        currentRole.flushChangeSet();
    }

    @Override
    public void close() throws IOException
    {
        currentRole.close();
    }

    @Override
    public ReplicationRole enableMasterMode()
    {
        return currentRole.enableMasterMode();
    }

    /**
     * Assuming the input file belongs to the content store, it returns the corresponding relative path.
     *
     * @param file the content store file.
     * @return the relative file path.
     */
    private String relativePath(File file)
    {
        return file.getAbsolutePath().replace(root, "");
    }

    /**
     * Convert a content URL into a File, whether it exists or not
     */
    private File getFileFromUrl(String contentUrl)
    {
        return new File(contentUrl.replace(SolrContentUrlBuilder.SOLR_PROTOCOL_PREFIX, root + "/"));
    }

    private ContentReader getReader(String contentUrl)
    {
        File file = getFileFromUrl(contentUrl);
        return new SolrFileContentReader(file, contentUrl);
    }
}