/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.Optional.ofNullable;

import org.alfresco.util.Pair;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.NumericDocValuesField;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.FieldDoc;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.SearcherManager;
import org.apache.lucene.search.Sort;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.SortedNumericSortField;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.BinaryOperator;

/**
 * Encapsulates changes occurred in the hosting content store since the last commit.
 *
 * @author Andrea Gazzarini
 * @since 1.5
 */
public class ChangeSet implements AutoCloseable
{
    private final static Logger LOGGER = LoggerFactory.getLogger(ChangeSet.class);
    final static ChangeSet EMPTY_CHANGESET = new ChangeSet.Builder().empty().build();

    /**
     * Builds class for creating {@link ChangeSet} instances.
     */
    public static class Builder
    {
        private String root;
        private boolean immutable;

        Builder withContentStoreRoot(String root)
        {
            if (root == null) throw new IllegalArgumentException("Unable to build the Changeset structures with a null content store root folder.");
            if (!new File(root).canWrite()) throw new IllegalArgumentException("Unable to build the Changeset structures with a non-writeable content store root folder.");

            this.root = root;
            return this;
        }

        Builder empty()
        {
            this.root = null;
            this.immutable = true;
            return this;
        }

        ChangeSet build()
        {
            if (root == null)
            {
                // Creates a transient changeset (no persistence); mainly used for reducing the changes during replication.
                return new ChangeSet(
                            immutable ? emptySet() : new HashSet<>(),
                            immutable ? emptySet() : new HashSet<>());
            }

            IndexWriter writer = null;
            try
            {
                File indexDirectory = new File(root, CHANGESETS_ROOT_FOLDER_NAME);

                writer = new IndexWriter(FSDirectory.open(indexDirectory.toPath()), new IndexWriterConfig());
                writer.commit();

                final SearcherManager searcher = new SearcherManager(writer, null);
                LOGGER.info("ContentStore Changeset index has been correctly mounted on {}", indexDirectory.getAbsolutePath());

                return new ChangeSet(
                        searcher,
                        writer,
                        immutable ? emptySet() : new HashSet<>(),
                        immutable ? emptySet() : new HashSet<>());
            }
            catch (Exception exception)
            {
                ofNullable(writer).ifPresent(ChangeSet::silentyClose);
                throw new IllegalArgumentException("Unable to create a ContentStore ChangeSet data structure. See further details in the stacktrack below.", exception);
            }
        }
    }

    final static String VERSION_FIELD_NAME = "version";
    final static String RVERSION_FIELD_NAME = "rversion";
    final static String ADDS_FIELD_NAME = "adds";
    final static String DELETES_FIELD_NAME = "deletes";
    final static String CHANGESETS_ROOT_FOLDER_NAME = "changeSets";

    Set<String> deletes;
    Set<String> adds;

    private final SearcherManager searcher;
    private final IndexWriter writer;

    Query selectEverything = new MatchAllDocsQuery();

    /**
     * Builds a new transient {@link ChangeSet} with the given Lucene facades.
     *
     * @param deletesContainer the container which will hold the deletes.
     * @param addsContainer the container which will hold the adds/updates.
     */
    private ChangeSet(
            final Set<String> deletesContainer,
            final Set<String> addsContainer)
    {
        this(null, null, deletesContainer, addsContainer);
    }

    /**
     * Builds a new {@link ChangeSet} with the given Lucene facades.
     *
     * @param searcher the searcher reference (actually a {@link SearcherManager} instance instead of dealing with {@link IndexSearcher} directly.
     * @param writer the {@link IndexWriter} instance used for persisting the content store changes.
     * @param deletesContainer the container which will hold the deletes.
     * @param addsContainer the container which will hold the adds/updates.
     */
    private ChangeSet(
            final SearcherManager searcher,
            final IndexWriter writer,
            final Set<String> deletesContainer,
            final Set<String> addsContainer)
    {
        this.searcher = searcher;
        this.writer = writer;
        this.deletes = deletesContainer;
        this.adds = addsContainer;
    }

    /**
     * Records a delete change.
     *
     * @param path the relative path of the file which has been deleted.
     */
    synchronized void delete(String path)
    {
        adds.remove(path);
        deletes.add(path);

        LOGGER.debug("ContentStore change recorded: item {} has been deleted.", path);

        debugPendingChanges();
    }

    /**
     * Records an add or update change.
     *
     * @param path the relative path of the file which has been updated or added.
     */
    synchronized void addOrReplace(String path)
    {
        deletes.remove(path);
        adds.add(path);

        LOGGER.debug("ContentStore change recorded: item {} has been added/updated.", path);

        debugPendingChanges();
    }

    /**
     * Flushes all pending collected content store changes.
     *
     * @throws IOException in case of I/O failure.
     */
    void flush() throws IOException
    {
        // No ops if this is a transient changeset
        if (searcher == null || writer == null) {
            return;
        }

        if (adds.isEmpty() && deletes.isEmpty())
        {
            LOGGER.debug("no changes in contentstore to flush");
            return;
        }

        final long version = System.currentTimeMillis();

        LOGGER.debug("About to add a new Changeset entry (version = {}, deletes = {}, adds = {})", version, deletes.size(), adds.size());

        final Document document = new Document();
        document.add(new NumericDocValuesField(VERSION_FIELD_NAME, version));
        document.add(new LongPoint(RVERSION_FIELD_NAME, version));

        Set<String> tmpDel;
        Set<String> tmpAdd;

        synchronized (this) {
            tmpDel = deletes;
            deletes = new HashSet<>();

            tmpAdd = adds;
            adds = new HashSet<>();
        }

        tmpDel.stream()
                .map(item -> new StoredField(DELETES_FIELD_NAME, item))
                .forEach(document::add);

        tmpAdd.stream()
                .map(item -> new StoredField(ADDS_FIELD_NAME, item))
                .forEach(document::add);

        writer.addDocument(document);
        writer.commit();
        searcher.maybeRefresh();

        LOGGER.debug("New Changeset entry have been added (version = {}, deletes = {}, adds = {})", version, deletes.size(), adds.size());
    }

    @Override
    public void close()
    {
        ofNullable(writer).ifPresent(ChangeSet::silentyClose);
        ofNullable(searcher).ifPresent(ChangeSet::silentyClose);
    }

    /**
     * Returns the last persisted content store version.
     *
     * @return the last persisted content store version, SolrContentStore#NO_VERSION_AVAILABLE in case the version isn't available.
     */
    long getLastCommittedVersion()
    {
        try
        {
            TopDocs hits = searcher().search(
                    selectEverything,
                    1,
                    new Sort(new SortedNumericSortField(VERSION_FIELD_NAME, SortField.Type.LONG, true)),
                    false,
                    false);

            return ofNullable(hits.scoreDocs)
                    .filter(docs -> docs.length > 0)
                    .map(docs -> ((FieldDoc) docs[0]).fields)
                    .filter(fields -> fields.length > 0)
                    .map(fields -> (Long)fields[0])
                    .orElse(SolrContentStore.NO_VERSION_AVAILABLE);
        }
        catch(Exception exception)
        {
            LOGGER.error("Unable to retrieve the last committed content store changeset version. " +
                            "As consequence of that a dummy value of " + SolrContentStore.NO_VERSION_AVAILABLE +
                            " will be returned. See further details in the stacktrack below.",
                    exception);
            return SolrContentStore.NO_VERSION_AVAILABLE;
        }
    }

    /**
     * Returns the content store changes (adds / deletes) since the given version (exclusive).
     *
     * @param version the start offset version (exclusive).
     * @return the content store changes (adds / deletes) since the given version (exclusive).
     */
    public ChangeSet since(long version)
    {
        try
        {
            Query query = LongPoint.newRangeQuery(RVERSION_FIELD_NAME, Math.addExact(version, 1), Long.MAX_VALUE);
            TopDocs hits = searcher().search(
                    query,
                    100,
                    new Sort(new SortedNumericSortField(VERSION_FIELD_NAME, SortField.Type.LONG)),
                    false,
                    false);

            final BiFunction<ChangeSet, ? super Pair<List<String>,List<String>>, ChangeSet> accumulator =
                    (partial, nth) -> {
                        final List<String> nthDeletes = nth.getFirst();
                        final List<String> nthAdds = nth.getSecond();

                        nthDeletes.forEach(partial::delete);
                        nthAdds.forEach(partial::addOrReplace);

                        return partial;
                    };

            final BinaryOperator<ChangeSet> combiner = (c1, c2) -> {
                c1.deletes.forEach(c2::delete);
                c1.adds.forEach(c2::addOrReplace);
                return c1;
            };

            return stream(hits.scoreDocs)
                        .map(this::toDoc)
                        .map(doc ->
                            new Pair<>(
                                    asList(doc.getValues(DELETES_FIELD_NAME)),
                                    asList(doc.getValues(ADDS_FIELD_NAME))))
                        .reduce(new ChangeSet.Builder().build(), accumulator, combiner);
        }
        catch(Exception exception)
        {
            LOGGER.error("Unable to retrieve the changeset since version {}. " +
                    "As consequence of that an empty result will be returned. " +
                    "See further details in the stacktrack below.",
                    version,
                    exception);
            return EMPTY_CHANGESET;
        }
    }

    private Document toDoc(ScoreDoc hit)
    {
        try
        {
            return searcher().doc(hit.doc);
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void debugPendingChanges()
    {
        if (LOGGER.isDebugEnabled())
        {
            LOGGER.debug("ContentStore pending deletes: " + deletes);
            LOGGER.debug("ContentStore pending adds/updates: " + adds);
        }
    }

    /**
     * Silently close (i.e. without any exception re-throwing) the incoming resource.
     *
     * @param resource the closeable resource.
     */
    private static void silentyClose(Closeable resource)
    {
        try
        {
            resource.close();
        }
        catch (Exception exception)
        {
           LOGGER.error("Unable to properly close the resource instance {}. See further details in the stacktrace below.", resource, exception);
        }
    }

    private IndexSearcher searcher() throws IOException
    {
        return searcher.acquire();
    }
}