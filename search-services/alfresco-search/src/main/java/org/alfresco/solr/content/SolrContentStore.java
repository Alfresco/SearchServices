/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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

import org.alfresco.repo.content.ContentContext;
import org.alfresco.service.cmr.repository.ContentReader;
import org.alfresco.service.cmr.repository.ContentWriter;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.NodeMetaData;
import org.alfresco.solr.config.ConfigUtil;
import org.alfresco.solr.handler.AlfrescoReplicationHandler;
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
import java.nio.file.StandardCopyOption;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonList;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.alfresco.solr.content.SolrContentUrlBuilder.FILE_EXTENSION;
import static org.alfresco.solr.content.SolrContentUrlBuilder.logger;

/**
 * A content store specific to SOLR's requirements: The URL is generated from a
 * set of properties such as:
 *
 * <ul>
 *  <li>ACL ID</li>
 *  <li>DB ID</li>
 *  <li>Other metadata</li>
 * </ul>
 *
 * The URL, if not known, can be reliably regenerated using the {@link SolrContentUrlBuilder}.
 * <br/>
 *
 * Since version 1.5 this class acts as a logical singleton: there will be only one instance per node.
 * That reflects exactly what we physically have in the filesystem (i.e. there's only one content store per node).
 *
 * That unique instance is created at startup in {@link org.alfresco.solr.AlfrescoCoreAdminHandler} and then passed to
 * each registered core (see {@link org.alfresco.solr.lifecycle.SolrCoreLoadListener})
 *
 * The State pattern implemented by means of the {@link AccessMode} interface allows for a given {@link SolrContentStore} instance to act:
 *
 * <ul>
 *     <li>in READ/WRITE mode: when <b>at least one core</b> of the hosting node is a master or it is a standalone shard/instance.</li>
 *     <li>in READ ONLY mode: when <b>all cores</b>  of the hosting node are slaves.</li>
 * </ul>
 *
 * The Finite State Machine (FST) provides three possible states: Initial, Read Only, Read/Write.
 * The allowed transitions are:
 *
 * <ul>
 *     <li>Initial -> ReadOnly: a slave core has been registered, the content store hasn't been yet initialised.</li>
 *     <li>
 *          ReadOnly -> Read/Write: this scenario is unusual because we should have on the same instance a mixed set of cores
 *          (some master/standalone, some slaves). It happens when a first slave core registers and causes the transition
 *          described in the first point (initial -> read only). Then a master or standalone core registers so we need to
 *          move from a read only to a complete readable/writable managed content store.
 *     </li>
 *     <li> Initial -> Read/Write: a master or standalone core has been registered, and the content store hasn't been yet initialised.</li>
 * </ul>
 *
 * Note the following transitions are not allowed:
 *
 * <ul>
 *     <li>Coming back to Initial state: once it has been initialised the Content Store cannot return back to the "Initial" state.</li>
 *     <li>
 *         Read/Write -> ReadOnly: if at least one master or standalone core registers, the content store is permanently moved in Read/Write mode.
 *         So even a further slave node registers (not very usually as described above) the content store remains in RW mode.
 *     </li>
 * </ul>
 *
 * @author Derek Hulley
 * @author Michael Suzuki
 * @author Andrea Gazzarini
 * @since 1.5
 * @see org.alfresco.solr.lifecycle.SolrCoreLoadListener
 * @see <a href="https://it.wikipedia.org/wiki/State_pattern">State Pattern</a>
 */
public final class SolrContentStore implements Closeable, AccessMode
{
    private final static Logger LOGGER = LoggerFactory.getLogger(SolrContentStore.class);

    public static final long NO_VERSION_AVAILABLE = -1L;
    public static final long NO_CONTENT_STORE_REPLICATION_REQUIRED = -2L;

    static final String CONTENT_STORE = "contentstore";
    static final String SOLR_CONTENT_DIR = "solr.content.dir";

    public static final String INFO = "info";
    public static final String FULL_REPLICATION = "full-replication";
    public static final String DELETES = "deletes";
    public static final String ADDS = "adds";

    private final Predicate<File> onlyDatafiles = file -> file.isFile() && file.getName().endsWith(FILE_EXTENSION);
    private final String root;

    /**
     * Used for denoting the very beginning state, when the {@link SolrContentStore} has been created
     * but we don't know (yet) which is the role played by the cores that will register on the hosting Solr node.
     */
    AccessMode notYetSet = new AccessMode()
    {
        @Override
        public long getLastCommittedVersion()
        {
            throw new IllegalStateException("ContentStore hasn't been properly initialised.");
        }

        @Override
        public void setLastCommittedVersion(long version)
        {
            throw new IllegalStateException("ContentStore hasn't been properly initialised.");
        }

        @Override
        public Map<String, List<Map<String, Object>>> getChanges(long version)
        {
            throw new IllegalStateException("ContentStore hasn't been properly initialised.");
        }

        @Override
        public void storeDocOnSolrContentStore(String tenant, long dbId, SolrInputDocument doc)
        {
            throw new IllegalStateException("ContentStore hasn't been properly initialised.");
        }

        @Override
        public void storeDocOnSolrContentStore(NodeMetaData nodeMetaData, SolrInputDocument doc)
        {
            throw new IllegalStateException("ContentStore hasn't been properly initialised.");
        }

        @Override
        public void removeDocFromContentStore(NodeMetaData nodeMetaData)
        {
            throw new IllegalStateException("ContentStore hasn't been properly initialised.");
        }

        @Override
        public void flushChangeSet()
        {
            throw new IllegalStateException("ContentStore hasn't been properly initialised.");
        }

        @Override
        public void switchOnReadOnlyMode()
        {
            logger.info("Switching the content store to ReadOnly mode.");
            currentAccessMode = readOnly;
        }

        @Override
        public void switchOnReadWriteMode()
        {
            logger.info("Switching the content store to Read/Write mode.");
            readWrite.init();
            currentAccessMode = readWrite;
        }

        @Override
        public void close()
        {
            // Nothing to close here
        }
    };

    final InitialisableAccessMode readOnly = new InitialisableAccessMode()
    {
        @Override
        public void init()
        {
            // Nothing to be done here...
        }

        @Override
        public void switchOnReadWriteMode()
        {
            logger.info("Switching from ReadOnly to Read/Write Content Store.");

            readWrite.init();
            currentAccessMode = readWrite;

            logger.info("Switching from ReadOnly to Read/Write Content Store.");
        }

        @Override
        public void switchOnReadOnlyMode()
        {
            logger.info("The content store is already in ReadOnly mode so this call won't have any effect.");
        }

        @Override
        public long getLastCommittedVersion()
        {
            try
            {
                return Files.readAllLines(Paths.get(root, ".version"))
                        .stream()
                        .map(Long::parseLong)
                        .findFirst()
                        .orElse(NO_VERSION_AVAILABLE);
            }
            catch (Exception e)
            {
                return NO_VERSION_AVAILABLE;
            }
        }

        @Override
        public void setLastCommittedVersion(long version)
        {

            File tmpFile = new File(root, ".version-" + new SimpleDateFormat(SnapShooter.DATE_FMT, Locale.ROOT).format(new Date()));
            try
            {
                FileWriter wr = new FileWriter(tmpFile);
                wr.write(Long.toString(version));
                wr.close();

                // file.renameTo(..) does not work on windows. Use Files.move instead.
                Files.move(tmpFile.toPath(), new File(root, ".version").toPath(), StandardCopyOption.ATOMIC_MOVE);

            }
            catch (IOException exception)
            {
                logger.error("Unable to persist the last committed content store version {}. See the stacktrace below for furtger details.", version, exception);
                try
                {
                    Files.delete(tmpFile.toPath());
                }
                catch (IOException e)
                {
                    logger.error("Unable to delete tmp contentstore version file {}.", version);
                }
            }
        }

        @Override
        public Map<String, List<Map<String, Object>>> getChanges(long version)
        {
            logger.warn("NoOp SolrContentStore changes call on slave side: this shouldn't happen because the ContentStore is in read-only mode when the hosting node is a slave.");
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

    final InitialisableAccessMode readWrite = new InitialisableAccessMode()
    {
        private ChangeSet changeSet;

        @Override
        public void init()
        {
            changeSet = ofNullable(changeSet).orElseGet(() -> new ChangeSet.Builder().withContentStoreRoot(root).build());
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
            // The slave doesn't have a version, we are listing the whole content store
            if (version <= NO_VERSION_AVAILABLE || changeSet.isUnknownVersion(version))
            {
                String message = "A slave requested the content store synchronization " +
                        ((version <= NO_VERSION_AVAILABLE)
                                ? " without providing any local version (actually {})."
                                : " with an invalid/unknown local version number ({}).") +
                        "As consequence of that this master will list the whole content store because a full replication is needed.";

                logger.info(message, version);

                return Map.of(
                        INFO, singletonList(Map.of(FULL_REPLICATION, true)),
                        ADDS, fullContentStore(),
                        DELETES, emptyList());
            }

            ChangeSet changes = changeSet.since(version);

            return Map.of(
                    INFO, singletonList(Map.of(FULL_REPLICATION, false)),
                    DELETES,
                    changes.deletes.stream()
                            .map(path -> Map.<String, Object>of("name", path))
                            .collect(toList()),
                    ADDS,
                    changes.adds.stream()
                            .map(relativePath -> root + relativePath)
                            .map(File::new)
                            .map(file -> new AlfrescoReplicationHandler.FileInfo(file, file.getAbsolutePath().replace(root, "")))
                            .map(AlfrescoReplicationHandler.FileInfo::getAsMap)
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

            LOGGER.debug("Writing {}/{} to {}", tenant, dbId, contentContext.getContentUrl());

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
                LOGGER.warn("Unable to write to Content Store using URL: {}", contentContext.getContentUrl(), exception);
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
        public void flushChangeSet() throws IOException
        {
            changeSet.flush();
        }

        @Override
        public void switchOnReadWriteMode()
        {
            logger.debug("The content store is already in ReadWrite mode; as consequence of that, the incoming \"SET-TO-RW-MODE\" call won't have any effect.");
        }

        @Override
        public void switchOnReadOnlyMode()
        {
            logger.debug("A writable content store cannot switch in ReadOnly mode. This could happen in an edge case where" +
                    " on the same Solr node we have masters and slaves nodes");
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
                        .map(file -> new AlfrescoReplicationHandler.FileInfo(file, file.getAbsolutePath().replace(root, "")))
                        .map(AlfrescoReplicationHandler.FileInfo::getAsMap)
                        .collect(toList());
            }
            catch (Exception e)
            {
                LOGGER.error("An exception occurred while retrieving the whole ContentStore filelist. " +
                        "As consequence of that an empty list will be returned (i.e. no ContentStore synch will happen).");
                return emptyList();
            }
        }

        private void delete(String contentUrl)
        {
            File file = getFileFromUrl(contentUrl);
            if (file.delete()) changeSet.delete(relativePath(file));
        }

        private ContentWriter getWriter(ContentContext context)
        {
            String url = context.getContentUrl();
            File file = getFileFromUrl(url);
            return new SolrFileContentWriter(file, url);
        }
    };

    AccessMode currentAccessMode = notYetSet;

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
            LOGGER.error(solrHomeFile.getAbsolutePath() + " does not exist.");
        }

        String path = solrHomeFile.getParent() + "/" + CONTENT_STORE;
        LOGGER.warn(path + " will be used as a default path if " + SOLR_CONTENT_DIR + " property is not defined");
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
        return currentAccessMode.getChanges(version);
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
            LOGGER.warn("Failed to get doc from store using URL: " + contentUrl, exception);
            return null;
        }
    }

    @Override
    public long getLastCommittedVersion()
    {
        return currentAccessMode.getLastCommittedVersion();
    }

    @Override
    public void setLastCommittedVersion(long version)
    {
        currentAccessMode.setLastCommittedVersion(version);
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
        currentAccessMode.storeDocOnSolrContentStore(tenant, dbId, doc);
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
        currentAccessMode.storeDocOnSolrContentStore(nodeMetaData, doc);
    }

    /**
     * Removes {@link SolrInputDocument} from Alfresco solr content store.
     *
     * @param nodeMetaData the incoming node metadata.
     */
    @Override
    public void removeDocFromContentStore(NodeMetaData nodeMetaData)
    {
        currentAccessMode.removeDocFromContentStore(nodeMetaData);
    }

    @Override
    public void flushChangeSet() throws IOException
    {
        currentAccessMode.flushChangeSet();
    }

    @Override
    public void close() throws IOException
    {
        currentAccessMode.close();
    }

    @Override
    public void switchOnReadWriteMode()
    {
        currentAccessMode = readWrite;
    }

    @Override
    public void switchOnReadOnlyMode()
    {
        currentAccessMode = readOnly;
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

    /**
     * Enables/disables the content store access mode.
     * The term "toggles" is just for indicating that this method will be called several times (one for each registered
     * core). The underlying FSM makes sure this call will be idempotent so in case of read/write content store the
     * data structure used for maintaining the content store versioning will be initialised only once, even if this
     * method is called repeatedly.
     *
     * @param enableReadOnlyMode a flag indicating if the requesting core requires a readOnly (true) or readWrite (false) content store.
     */
    public synchronized void toggleReadOnlyMode(boolean enableReadOnlyMode)
    {
        if (enableReadOnlyMode)
        {
            currentAccessMode.switchOnReadOnlyMode();
        }
        else
        {
            currentAccessMode.switchOnReadWriteMode();
        }
    }
}