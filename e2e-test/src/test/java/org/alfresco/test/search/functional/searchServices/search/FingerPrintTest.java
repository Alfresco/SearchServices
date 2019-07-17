/*
 * Copyright (C) 2018 Alfresco Software Limited.
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
package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.rest.search.SearchNodeModel;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.search.TestGroup;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.report.Bug;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 * Search end point Public API test with finger print.
 * 
 * @author Michael Suzuki
 */
public class FingerPrintTest extends AbstractE2EFunctionalTest
{
    private FileModel file1;
    private FileModel file2;
    private FolderModel folder;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        /*
         * Create the following file structure in the same Site  : In addition to the preconditions created in dataPreparation
         * |- folder
         *    |-- pangram-banana.txt
         *    |-- pangram-taco.txt
         *    |-- pangram-cat.txt
         *    |-- dog.txt
         */
        folder = new FolderModel("The quick brown fox jumps over");
        dataContent.usingUser(testUser).usingSite(testSite).createFolder(folder);

        file1 = new FileModel("pangram-banana.txt", FileType.TEXT_PLAIN, "The quick brown fox jumps over the lazy banana");
        file2 = new FileModel("pangram-taco.txt", FileType.TEXT_PLAIN, "The quick brown fox jumps over the lazy dog that ate the taco");

        FileModel file3 = new FileModel("pangram-cat.txt", FileType.TEXT_PLAIN, "The quick brown fox jumps over the lazy cat");
        FileModel file4 = new FileModel("dog.txt", FileType.TEXT_PLAIN, "The quick brown fox ate the lazy dog");

        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(file1);
        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(file2);
        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(file3);
        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(file4);

        waitForContentIndexing(file4.getContent(), true);

        // Additional wait implemented to remove inconsistent failures. Ref: Search-1438 for details
        waitForIndexing("FINGERPRINT:" + file2.getNodeRefWithoutVersion(), true);
        waitForIndexing("FINGERPRINT:" + file3.getNodeRefWithoutVersion(), true);
    }

    /**
     * Search similar document based on document finger print.
     * The data prep should have loaded 2 files which one is similar
     * to the files loaded as part of this test.
     * Note that for fingerprint to work it need a 5 word sequence.
     */
    @Test
    public void search()
    {
        String uuid = file1.getNodeRefWithoutVersion();
        Assert.assertNotNull(uuid);
        String fingerprint = String.format("FINGERPRINT:%s", uuid);
        SearchResponse response = query(fingerprint);
        int count = response.getEntries().size();
        assertTrue(count > 1);
        for (SearchNodeModel m : response.getEntries())
        {
            String match = m.getModel().getName();
            switch (match)
            {
                case "pangram.txt":
                case "pangram-banana.txt":
                case "pangram-taco.txt":
                case "pangram-cat.txt":
                    break;
                default:
                    throw new AssertionError("Not a match to an expected file: " + m.getModel().getName());
            }
            m.getModel().assertThat().field("name").isNot("dog.txt");
            m.getModel().assertThat().field("name").isNot("cars.txt");
        }
    }

    @Test
    public void searchSimilar()
    {
        String uuid = file2.getNodeRefWithoutVersion();
        Assert.assertNotNull(uuid);

        // In the response entity there is a score of each doc, change below threshold to bring more like or less.
        String fingerprint = String.format("FINGERPRINT:%s_68", uuid);
        SearchResponse response = query(fingerprint);

        int count = response.getEntries().size();
        assertTrue(count > 1);

        for (SearchNodeModel m : response.getEntries())
        {
            switch (m.getModel().getName())
            {
                case "pangram.txt":
                case "pangram-taco.txt":
                    break;
                default:
                    throw new AssertionError("Not a match to an expected file: " + m.getModel().getName());
            }
            m.getModel().assertThat().field("name").isNot("pangram-banana.txt");
            m.getModel().assertThat().field("name").isNot("pangram-cat.txt");
            m.getModel().assertThat().field("name").isNot("dog.txt");
            m.getModel().assertThat().field("name").isNot("cars.txt");
        }
    }

    @Test
    public void searchSimilar67Percent()
    {
        String uuid = file2.getNodeRefWithoutVersion();
        Assert.assertNotNull(uuid);
        String fingerprint = String.format("FINGERPRINT:%s_68", uuid);
        SearchResponse response = query(fingerprint);
        int count = response.getEntries().size();
        assertTrue(count > 1);
        for (SearchNodeModel m : response.getEntries())
        {
            switch (m.getModel().getName())
            {
                case "pangram.txt":
                case "pangram-taco.txt":
                case "pangram-cat.txt":
                    break;
                default:
                    throw new AssertionError("Not a match to an expected file: " + m.getModel().getName());
            }
            m.getModel().assertThat().field("name").isNot("pangram-banana.txt");
            m.getModel().assertThat().field("name").isNot("dog.txt");
            m.getModel().assertThat().field("name").isNot("cars.txt");
        }
    }
}
