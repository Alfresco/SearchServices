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

import org.apache.commons.io.FileUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.Query;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Solr ContentStore {@link ChangeSet} test case.
 *
 * @author Andrea Gazzarini
 * @since 1.5
 */
@RunWith(MockitoJUnitRunner.class)
@Ignore
// FIXME: Remove once SEARCH-1687 will be completed
public class SolrContentStoreChangeSetTest
{
    private ChangeSet changeSet;
    private final String contentStoreRootFolder = "/tmp";
    private final File rootFolder = new File(contentStoreRootFolder, ChangeSet.CHANGESETS_ROOT_FOLDER_NAME);

    @Before
    public void setUp()
    {
        changeSet = new ChangeSet.Builder().withContentStoreRoot(contentStoreRootFolder).build();
    }

    @After 
    public void tearDown() throws IOException
    {
        changeSet.close();
        FileUtils.cleanDirectory(rootFolder);
    }

    @Test(expected = IllegalArgumentException.class)
    public void nullContentStoreRootFolder_shouldThrowAnException()
    {
        new ChangeSet.Builder().withContentStoreRoot(null).build();
    }

    @Test(expected = IllegalArgumentException.class)
    public void nonWriteableContentStoreRootFolder_shouldThrowAnException()
    {
        new ChangeSet.Builder().withContentStoreRoot("/root").build();
    }

    @Test
    public void newAddOrReplaceRecord_shouldRemovePreviousDeletion()
    {
        String path ="some/random/dbid.gz";

        assertTrue(changeSet.deletes.isEmpty());
        assertTrue(changeSet.adds.isEmpty());

        changeSet.delete(path);

        assertTrue(String.valueOf(changeSet.deletes), changeSet.deletes.contains(path));
        assertTrue(String.valueOf(changeSet.adds), changeSet.adds.isEmpty());

        changeSet.addOrReplace(path);

        assertTrue(String.valueOf(changeSet.deletes), changeSet.deletes.isEmpty());
        assertTrue(String.valueOf(changeSet.adds),changeSet.adds.contains(path));
    }

    @Test
    public void deletedRecord_shouldRemovePreviousAdd()
    {
        String path ="some/random/dbid.gz";

        assertTrue(changeSet.deletes.isEmpty());
        assertTrue(changeSet.adds.isEmpty());

        changeSet.addOrReplace(path);

        assertTrue(String.valueOf(changeSet.deletes), changeSet.deletes.isEmpty());
        assertTrue(String.valueOf(changeSet.adds), changeSet.adds.contains(path));

        changeSet.delete(path);

        assertTrue(String.valueOf(changeSet.deletes), changeSet.deletes.contains(path));
        assertTrue(String.valueOf(changeSet.adds),changeSet.adds.isEmpty());
    }

    @Test
    public void transientChangeset_doesNothingOnFlush() throws IOException
    {
        ChangeSet changeset = new ChangeSet.Builder().build();
        changeset.addOrReplace("A");
        changeset.delete("B");

        assertEquals(1, changeset.deletes.size());
        assertEquals(1, changeset.adds.size());

        changeset.flush();

        assertEquals(1, changeset.deletes.size());
        assertEquals(1, changeset.adds.size());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void emptyChangeset_isImmutableDoesntAllowAdds()
    {
        ChangeSet changeset = new ChangeSet.Builder().empty().build();
        changeset.addOrReplace("A");
    }

    @Test(expected = UnsupportedOperationException.class)
    public void emptyChangeset_isImmutableDoesntAllowDeletes()
    {
        ChangeSet changeset = new ChangeSet.Builder().empty().build();
        changeset.delete("A");
    }

    @Test
    public void lastCommittedVersionNotPresentAtVeryBeginning()
    {
        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, changeSet.getLastCommittedVersion());
    }

    @Test
    public void lastCommittedVersionNotAvailable_shouldReturnNO_AVAILABLE_VERSION() throws IOException
    {
        changeSet.selectEverything = mock(Query.class);
        when(changeSet.selectEverything.rewrite(any(IndexReader.class))).thenThrow(new RuntimeException());
        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, changeSet.getLastCommittedVersion());
    }

    @Test
    public void flushDoesNothingIfThereAreNoChanges() throws IOException
    {
        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, changeSet.getLastCommittedVersion());

        changeSet.flush();

        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, changeSet.getLastCommittedVersion());
    }

    @Test
    public void invalidOrUnknownVersion() throws IOException
    {
        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, changeSet.getLastCommittedVersion());

        assertTrue(changeSet.isUnknownVersion(SolrContentStore.NO_VERSION_AVAILABLE));
        assertTrue(changeSet.isUnknownVersion(SolrContentStore.NO_VERSION_AVAILABLE - 1L));
        assertTrue(changeSet.isUnknownVersion(System.currentTimeMillis()));

        changeSet.addOrReplace("A1");
        changeSet.addOrReplace("A2");
        changeSet.delete("A3");
        changeSet.delete("A1");

        changeSet.flush();

        long lastCommittedVersionAfterFirstFlush = changeSet.getLastCommittedVersion();
        assertNotEquals(SolrContentStore.NO_VERSION_AVAILABLE, lastCommittedVersionAfterFirstFlush);

        assertTrue(changeSet.isUnknownVersion(System.currentTimeMillis()));
    }

    @Test
    public void validVersion() throws IOException
    {
        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, changeSet.getLastCommittedVersion());

        assertTrue(changeSet.isUnknownVersion(System.currentTimeMillis()));

        changeSet.addOrReplace("A1");
        changeSet.addOrReplace("A2");
        changeSet.delete("A3");
        changeSet.delete("A1");

        changeSet.flush();

        long lastCommittedVersionAfterFirstFlush = changeSet.getLastCommittedVersion();

        changeSet.addOrReplace("B1");
        changeSet.addOrReplace("B2");
        changeSet.delete("B3");
        changeSet.delete("B1");

        changeSet.flush();

        long lastCommittedVersionAfterSecondFlush = changeSet.getLastCommittedVersion();
        assertNotEquals(lastCommittedVersionAfterSecondFlush, lastCommittedVersionAfterFirstFlush);

        assertFalse(changeSet.isUnknownVersion(lastCommittedVersionAfterFirstFlush));
        assertFalse(changeSet.isUnknownVersion(lastCommittedVersionAfterSecondFlush));
    }

    @Test
    public void inCaseOfFailure_inputVersionIsConsideredUnknown() throws IOException
    {
        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, changeSet.getLastCommittedVersion());

        assertTrue(changeSet.isUnknownVersion(System.currentTimeMillis()));

        changeSet.addOrReplace("A1");
        changeSet.addOrReplace("A2");
        changeSet.delete("A3");
        changeSet.delete("A1");

        changeSet.flush();

        long lastCommittedVersion = changeSet.getLastCommittedVersion();

        // Force a NPE exception...
        changeSet.searcher = null;

        // ...so a valid version is considered unknown even if it is valid
        assertTrue(changeSet.isUnknownVersion(lastCommittedVersion));
    }

    @Test
    public void persistentChangesetsAreMergedBeforeReturningToRequestor() throws IOException
    {
        assertEquals(SolrContentStore.NO_VERSION_AVAILABLE, changeSet.getLastCommittedVersion());

        changeSet.addOrReplace("A1");
        changeSet.addOrReplace("A2");
        changeSet.delete("A3");
        changeSet.delete("A1");

        changeSet.flush();

        long lastCommittedVersionAfterFirstFlush = changeSet.getLastCommittedVersion();
        assertNotEquals(SolrContentStore.NO_VERSION_AVAILABLE, lastCommittedVersionAfterFirstFlush);

        changeSet.addOrReplace("A1");
        changeSet.addOrReplace("A3");
        changeSet.delete("A4");

        changeSet.flush();

        long lastCommittedVersionAfterSecondFlush = changeSet.getLastCommittedVersion();
        assertNotEquals(lastCommittedVersionAfterFirstFlush, lastCommittedVersionAfterSecondFlush);

        ChangeSet changesSinceTheVeryBeginning = changeSet.since(SolrContentStore.NO_VERSION_AVAILABLE);

        // ADDS = [A1, A2, A3]
        // DELS = [A4]
        assertEquals(3, changesSinceTheVeryBeginning.adds.size());
        assertEquals(1, changesSinceTheVeryBeginning.deletes.size());
        assertTrue(changesSinceTheVeryBeginning.adds.contains("A1"));
        assertTrue(changesSinceTheVeryBeginning.adds.contains("A2"));
        assertTrue(changesSinceTheVeryBeginning.adds.contains("A3"));
        assertTrue(changesSinceTheVeryBeginning.deletes.contains("A4"));

        ChangeSet changesAfterSecondFlush = changeSet.since(lastCommittedVersionAfterFirstFlush);

        // ADDS = [A1, A3]
        // DELS = [A4]
        assertEquals(2, changesAfterSecondFlush.adds.size());
        assertEquals(1, changesAfterSecondFlush.deletes.size());
        assertTrue(changesAfterSecondFlush.adds.contains("A1"));
        assertTrue(changesAfterSecondFlush.adds.contains("A3"));
        assertTrue(changesAfterSecondFlush.deletes.contains("A4"));
    }
}