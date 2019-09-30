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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertTrue;

/**
 * Solr ContentStore {@link ChangeSet} test case.
 *
 * @author Andrea Gazzarini
 */
@RunWith(MockitoJUnitRunner.class) 
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
}
