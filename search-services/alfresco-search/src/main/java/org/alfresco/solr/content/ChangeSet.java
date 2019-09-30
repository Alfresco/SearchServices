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

import org.alfresco.solr.handler.ReplicationHandler;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.LongPoint;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexCommit;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import static java.util.Optional.ofNullable;

/**
 * Encapsulates changes occurred in the hosting content store since the last commit.
 *
 * @author Andrea Gazzarini
 */
class ChangeSet implements AutoCloseable
{
    /**
     * Builds class for creating {@link ChangeSet} instances.
     */
    static class Builder
    {
        private String root;

        Builder withContentStoreRoot(String root)
        {
            if (root == null) throw new IllegalArgumentException("Unable to build the Changeset structures with a null content store root folder.");
            if (!new File(root).canWrite()) throw new IllegalArgumentException("Unable to build the Changeset structures with a non-writeable content store root folder.");

            this.root = root;
            return this;
        }

        ChangeSet build()
        {
            // Necessary Evil: needed for making sure the Lucene stuff will be closed
            // in case of initialisation exception.
            IndexWriter writer = null;
            IndexSearcher searcher = null;
            try
            {
                File file = new File(root, CHANGESETS_ROOT_FOLDER_NAME);
                Directory indexDirectory = FSDirectory.open(file.toPath());

                writer = new IndexWriter(indexDirectory, new IndexWriterConfig());
                writer.commit();

                searcher = new IndexSearcher(DirectoryReader.open(indexDirectory));
                return new ChangeSet(searcher, writer);
            }
            catch (Exception exception)
            {
                ofNullable(writer).ifPresent(ChangeSet::silentyClose);
                ofNullable(searcher).map(IndexSearcher::getIndexReader).ifPresent(ChangeSet::silentyClose);
                throw new IllegalArgumentException("Unable to create the ContentStore ChangeSet data structure.", exception);
            }
        }
    }

    public final static String VERSION_FIELD_NAME = "version";
    public final static String ADDS_FIELD_NAME = "adds";
    public final static String DELETES_FIELD_NAME = "deletes";
    public final static String CHANGESETS_ROOT_FOLDER_NAME = "changeSets";

    final Set<String> deletes = new HashSet<>();
    final Set<String> adds = new HashSet<>();

    private final IndexSearcher searcher;
    private final IndexWriter writer;

    /**
     * Builds a new {@link ChangeSet} with the given Lucene facades.
     *
     * @param searcher the index searcher.
     * @param writer the index writer.
     */
    private ChangeSet(final IndexSearcher searcher, final IndexWriter writer)
    {
        this.searcher = searcher;
        this.writer = writer;
    }

    /**
     * Records a delete change.
     *
     * @param path the relative path of the file which has been deleted.
     */
    void delete(String path)
    {
        adds.remove(path);
        deletes.add(path);
    }

    /**
     * Records an add or update change.
     *
     * @param path the relative path of the file which has been updated or added.
     */
    void addOrReplace(String path)
    {
        deletes.remove(path);
        adds.add(path);
    }

    /**
     * Flushes all pending collected content store changes.
     *
     * @param commit the {@link IndexCommit} instance belonging to the main index.
     * @throws IOException in case of I/O failure.
     */
    public void flush(IndexCommit commit) throws IOException
    {
        ReplicationHandler.CommitVersionInfo commitInfo = ReplicationHandler.CommitVersionInfo.build(commit);
        Document document = new Document();
        document.add(new LongPoint(VERSION_FIELD_NAME, commitInfo.version));
        deletes.forEach(delete -> document.add(new StoredField(DELETES_FIELD_NAME, delete)));
        adds.forEach(addOrReplace -> document.add(new StoredField(ADDS_FIELD_NAME, addOrReplace)));

        writer.addDocument(document);
        writer.commit();
    }

    @Override
    public void close()
    {
        ofNullable(writer).ifPresent(ChangeSet::silentyClose);
        ofNullable(searcher).map(IndexSearcher::getIndexReader).ifPresent(ChangeSet::silentyClose);
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
            // Ignore
        }
    }
}