/*
 * Copyright (C) 2017 Alfresco Software Limited.
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
package org.alfresco.rest.search;

import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Search end point Public API test with finger print.
 * @author Michael Suzuki
 *
 */
public class FingerPrintTest extends AbstractSearchTest
{
    private FileModel file1,file2,file3,file4;
    
    @BeforeClass(alwaysRun=true)
    public void indexSimilarFile() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        
        siteModel = new SiteModel(RandomData.getRandomName("SiteSearch"));
        siteModel.setVisibility(Visibility.PRIVATE);
        
        siteModel = dataSite.usingUser(userModel).createSite(siteModel);
        
        /*
         * Create the following file structure for preconditions : 
         *   |- folder
         *        |-- pangram-banana.txt
         *        |-- pangram-taco.txt
         *        |-- pangram-cat.txt
         *        |-- dog.txt
         */
        
        FolderModel folder = new FolderModel(SEARCH_DATA_SAMPLE_FOLDER);
        dataContent.usingUser(userModel).usingSite(siteModel).createFolder(folder);
        
        file1 = new FileModel("pangram-banana.txt", FileType.TEXT_PLAIN, "The quick brown fox jumps over the lazy banana");
        file2 = new FileModel("pangram-taco.txt", FileType.TEXT_PLAIN, "The quick brown fox jumps over the lazy dog that ate the taco");
        file3 = new FileModel("pangram-cat.txt", FileType.TEXT_PLAIN, "The quick brown fox jumps over the lazy cat");
        file4 = new FileModel("dog.txt", FileType.TEXT_PLAIN, "The quick brown fox ate the lazy dog");

        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(folder).createContent(file1);
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(folder).createContent(file2);
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(folder).createContent(file3);
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(folder).createContent(file4);
        
        waitForIndexing(file4.getName(), true);
    }
    
    /**
     * Search similar document based on document finger print.
     * The data prep should have loaded 2 files which one is similar
     * to the files loaded as part of this test.
     * Note that for fingerprint to work it need a 5 word sequence.
     * 
     * @throws Exception
     */
    @Test(groups= {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1})
    public void search() throws Exception
    {
        String uuid = file1.getNodeRefWithoutVersion();
        Assert.assertNotNull(uuid);
        String fingerprint = String.format("FINGERPRINT:%s", uuid);
        SearchResponse response = query(fingerprint);
        int count = response.getEntries().size();
        Assert.assertTrue(count > 1);
        for(SearchNodeModel m :response.getEntries())
        {
            String match = m.getModel().getName();
            switch (match)
            {
            case "pangram.txt":
                break;
            case "pangram-banana.txt":
                break;
            case "pangram-taco.txt":
                break;
            case "pangram-cat.txt":
                break;
            default:
                throw new AssertionError("Not a match to an expected file: " +  m.getModel().getName());
            }
            m.getModel().assertThat().field("name").isNot("dog.txt");
            m.getModel().assertThat().field("name").isNot("cars.txt");
        }
    }
    
    @Test(groups= {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1})
    public void searchSimilar() throws Exception
    {
        String uuid = file2.getNodeRefWithoutVersion();
        Assert.assertNotNull(uuid);
        
        // In the response entity there is a score of each doc, change below threshold to bring more like or less.
        String fingerprint = String.format("FINGERPRINT:%s_68", uuid); 
        SearchResponse response = query(fingerprint);
        
        int count = response.getEntries().size();
        Assert.assertTrue(count > 1);
        
        for(SearchNodeModel m :response.getEntries())
        {
            switch (m.getModel().getName())
            {
            case "pangram.txt":
                break;
            case "pangram-taco.txt":
                break;
            default:
               throw new AssertionError("Not a match to an expected file: " +  m.getModel().getName());
            }
            m.getModel().assertThat().field("name").isNot("pangram-banana.txt");
            m.getModel().assertThat().field("name").isNot("pangram-cat.txt");
            m.getModel().assertThat().field("name").isNot("dog.txt");
            m.getModel().assertThat().field("name").isNot("cars.txt");
        }
    }
    
    @Test(groups= {TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_1})
    public void searchSimilar67Percent() throws Exception
    {
        String uuid = file2.getNodeRefWithoutVersion();
        Assert.assertNotNull(uuid);
        String fingerprint = String.format("FINGERPRINT:%s_68", uuid);
        SearchResponse response = query(fingerprint);
        int count = response.getEntries().size();
        Assert.assertTrue(count > 1);
        for(SearchNodeModel m :response.getEntries())
        {
            switch (m.getModel().getName())
            {
            case "pangram.txt":
                break;
            case "pangram-taco.txt":
                break;
            case "pangram-cat.txt":
                break;
            default:
               throw new AssertionError("Not a match to an expected file: " +  m.getModel().getName());
            }
            m.getModel().assertThat().field("name").isNot("pangram-banana.txt");
            m.getModel().assertThat().field("name").isNot("dog.txt");
            m.getModel().assertThat().field("name").isNot("cars.txt");
        }
    }
}
