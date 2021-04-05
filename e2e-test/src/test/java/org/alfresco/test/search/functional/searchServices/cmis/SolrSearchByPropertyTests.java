/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.alfresco.test.search.functional.searchServices.cmis;

import java.util.List;
import java.util.Set;

import org.alfresco.utility.Utility;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.QueryModel;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SolrSearchByPropertyTests extends AbstractCmisE2ETest
{
    private FolderModel guestf, tesf, restf, testtttf, testf, testf1, testf2, testf3, testf4;
    private FileModel guestc, restc, tesc, testtttc, testc, testc1, testc2, testc3;

    @BeforeClass (alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        dataContent.usingAdmin().deployContentModel("model/tas-model.xml");

        // Folders
        guestf = new FolderModel("guestf");
        tesf = new FolderModel("tesf");

        // SubFolders
        restf = new FolderModel("restf");
        testtttf = new FolderModel("testtttf");
        testf = new FolderModel("testf");
        testf1 = new FolderModel("testf1");
        testf2 = new FolderModel("testf2");
        testf3 = new FolderModel("testf3");
        testf4 = new FolderModel("testf4");

        // Files
        guestc = new FileModel("guestc.txt");
        restc = new FileModel("restc.txt");
        tesc = new FileModel("tesc.txt");
        testtttc = new FileModel("testtttc.txt");
        testc = new FileModel("testc.txt");
        testc1 = new FileModel("testc1.txt");
        testc2 = new FileModel("testc2.txt");
        testc3 = new FileModel("testc3.txt");

        // Sites
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(guestf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "guestf text")
                                                .addProperty("tas:IntPropertyF", 222));
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(tesf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "tesf text")
                                                .addProperty("tas:IntPropertyF", 224));

        // Sites >> Folders
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(restf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "restf text")
                                                .addProperty("tas:IntPropertyF", 223));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testtttf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testtttf text")
                                                .addProperty("tas:IntPropertyF", 225));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf text")
                                                .addProperty("tas:IntPropertyF", 226));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf1, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf1 text")
                                                .addProperty("tas:IntPropertyF", 2221));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf2, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf2 text")
                                                .addProperty("tas:IntPropertyF", 2222));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf3, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf3 text")
                                                .addProperty("tas:IntPropertyF", 2223));
        dataContent.usingUser(testUser).usingResource(guestf).createCustomContent(testf4, "F:tas:folder",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyF", "testf4 text")
                                                .addProperty("tas:IntPropertyF", 2224));

        // Sites >> Files
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(guestc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "guestc text")
                                                .addProperty("tas:IntPropertyC", 222));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(restc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "restc text")
                                                .addProperty("tas:IntPropertyC", 223));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(tesc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "tesc text")
                                                .addProperty("tas:IntPropertyC", 224));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testtttc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testtttc text")
                                                .addProperty("tas:IntPropertyC", 225));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testc, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testc text")
                                                .addProperty("tas:IntPropertyC", 226));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testc1, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testc1 text")
                                                .addProperty("tas:IntPropertyC", 2221));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testc2, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testc2 text")
                                                .addProperty("tas:IntPropertyC", 2222));
        dataContent.usingUser(testUser).usingResource(tesf).createCustomContent(testc3, "D:tas:document",
                new CustomObjectTypeProperties().addProperty("tas:TextPropertyC", "testc3 text")
                                                .addProperty("tas:IntPropertyC", 2223));

        // wait for solr index
        cmisApi.authenticateUser(testUser);
        waitForIndexing("SELECT * FROM tas:document where cmis:name = 'testc3.txt'", 1);
    }

    @Test
    public void testFileNameEquality()
    {
        String query = "SELECT * FROM tas:document where cmis:name = 'testc.txt'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testc.txt"));
    }

    @Test
    public void testFileNameInequality()
    {
        String query = "SELECT * FROM tas:document where cmis:name <> 'testc.txt'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestc.txt", "restc.txt", "tesc.txt", "testtttc.txt", "testc1.txt", "testc2.txt", "testc3.txt"));
    }

    @Test
    public void testFileNameIn()
    {
        String query = "SELECT * FROM tas:document where cmis:name IN('testc.txt', 'guestc.txt', 'restc.txt')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testc.txt", "guestc.txt", "restc.txt"));
    }

    @Test
    public void testFileNameNotIn()
    {
        // Nb. "gustc" is missing an "e".
        String query = "SELECT * FROM tas:document where cmis:name NOT IN('testc.txt', 'gustc.txt', 'restc.txt')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestc.txt", "tesc.txt", "testtttc.txt", "testc1.txt", "testc2.txt", "testc3.txt"));
    }

    @Test
    public void testFileNameLike()
    {
        String query = "SELECT * FROM tas:document where cmis:name LIKE '%testc%'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testc.txt", "testc1.txt", "testc2.txt", "testc3.txt"));
    }

    @Test
    public void testFileNameLikeExact()
    {
        String query = "SELECT * FROM tas:document where cmis:name LIKE 'testc.txt'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testc.txt"));
    }

    @Test
    public void testFileNamePrefixSuffix()
    {
        String query = "SELECT * FROM tas:document where cmis:name LIKE 't%tc.txt'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testc.txt", "testtttc.txt"));
    }

    @Test
    public void testFileNameUnderscore()
    {
        String query = "SELECT * FROM tas:document where cmis:name LIKE 't__tc.txt'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testc.txt"));
    }

    @Test
    public void testFolderNameEquality()
    {
        String query = "SELECT * FROM tas:folder where cmis:name = 'testf'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testf"));
    }

    @Test
    public void testFolderNameInequality()
    {
        String query = "SELECT * FROM tas:folder where cmis:name <> 'testf'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestf", "tesf", "restf", "testtttf", "testf1", "testf2", "testf3", "testf4"));
    }

    @Test
    public void testFolderNameIn()
    {
        String query = "SELECT * FROM tas:folder where cmis:name IN('testf', 'guestf', 'restf')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testf", "guestf", "restf"));
    }

    @Test
    public void testFolderNameNotIn()
    {
        // Nb. "gustc" is missing an "e".
        String query = "SELECT * FROM tas:folder where cmis:name NOT IN('testf', 'gustf', 'restf')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestf", "tesf", "testtttf", "testf1", "testf2", "testf3", "testf4"));
    }

    @Test
    public void testFolderNameLike()
    {
        String query = "SELECT * FROM tas:folder where cmis:name LIKE '%testf%'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testf", "testf1", "testf2", "testf3", "testf4"));
    }

    @Test
    public void testFolderNameLikeExact()
    {
        String query = "SELECT * FROM tas:folder where cmis:name LIKE 'testf'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testf"));
    }

    @Test
    public void testFolderNamePrefixSuffix()
    {
        String query = "SELECT * FROM tas:folder where cmis:name LIKE 't%tf'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testtttf", "testf"));
    }

    @Test
    public void testFolderNameUnderscore()
    {
        String query = "SELECT * FROM tas:folder where cmis:name LIKE 't__tf'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testf"));
    }

    @Test
    public void testFileNameOrderAsc()
    {
        String query = "SELECT * FROM tas:document where cmis:name LIKE '%testc%' ORDER BY cmis:name ASC";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningOrderedValues("cmis:name",
                List.of("testc1.txt", "testc2.txt", "testc3.txt", "testc.txt"));
    }

    @Test
    public void testFileNameOrderDesc()
    {
        String query = "SELECT * FROM tas:document where cmis:name LIKE '%testc%' ORDER BY cmis:name DESC";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningOrderedValues("cmis:name",
                List.of("testc.txt", "testc3.txt", "testc2.txt", "testc1.txt"));
    }

    @Test
    public void testFileOrderNameDescDateAsc()
    {
        String query = "SELECT * FROM tas:document where cmis:name LIKE '%testc%' ORDER BY cmis:name DESC, cmis:creationDate ASC";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningOrderedValues("cmis:name",
                List.of("testc.txt", "testc3.txt", "testc2.txt", "testc1.txt"));
    }

    @Test
    public void testFolderNameOrderAsc()
    {
        String query = "SELECT * FROM tas:folder where cmis:name LIKE '%testf%' ORDER BY cmis:name ASC";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningOrderedValues("cmis:name",
                List.of("testf", "testf1", "testf2", "testf3", "testf4"));
    }

    @Test
    public void testFolderNameOrderDesc()
    {
        String query = "SELECT * FROM tas:folder where cmis:name LIKE '%testf%' ORDER BY cmis:name DESC";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningOrderedValues("cmis:name",
                List.of("testf4", "testf3", "testf2", "testf1", "testf"));
    }

    @Test
    public void testFolderOrderNameDescDateAsc()
    {
        String query = "SELECT * FROM tas:folder where cmis:name LIKE '%testf%' ORDER BY cmis:name DESC, cmis:creationDate ASC";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningOrderedValues("cmis:name",
                List.of("testf4", "testf3", "testf2", "testf1", "testf"));
    }

    @Test
    public void testFileCustomPropertyEquality()
    {
        String query = "SELECT * FROM tas:document where tas:TextPropertyC = 'restc text'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("restc.txt"));
    }

    @Test
    public void testFileCustomPropertyInequality()
    {
        String query = "SELECT * FROM tas:document where tas:TextPropertyC <> 'testc1 text'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestc.txt", "restc.txt", "tesc.txt", "testtttc.txt", "testc.txt", "testc2.txt", "testc3.txt"));
    }

    @Test
    public void testFileCustomPropertyIn()
    {
        String query = "SELECT * FROM tas:document where tas:TextPropertyC IN('restc text', 'testc2 text', 'testc3 text')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("restc.txt", "testc2.txt", "testc3.txt"));
    }


    @Test
    public void testFileCustomPropertyNotIn()
    {
        String query = "SELECT * FROM tas:document where tas:TextPropertyC NOT IN('restc text', 'testc2 text', 'testc3 text')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestc.txt", "tesc.txt", "testtttc.txt", "testc.txt", "testc1.txt"));
    }

    @Test
    public void testFileCustomPropertyLike()
    {
        String query = "SELECT * FROM tas:document where tas:TextPropertyC LIKE '%restc%'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("restc.txt"));
    }

    @Test
    public void testFileCustomPropertyUnderscore()
    {
        String query = "SELECT * FROM tas:document where tas:TextPropertyC LIKE 't__tc text'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testc.txt"));
    }

    @Test
    public void testFolderCustomPropertyEquality()
    {
        String query = "SELECT * FROM tas:folder where tas:TextPropertyF = 'restf text'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("restf"));
    }

    @Test
    public void testFolderCustomPropertyInequality()
    {
        String query = "SELECT * FROM tas:folder where tas:TextPropertyF <> 'testf1 text'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestf", "tesf", "restf", "testtttf", "testf", "testf2", "testf3", "testf4"));
    }

    @Test
    public void testFolderCustomPropertyIn()
    {
        String query = "SELECT * FROM tas:folder where tas:TextPropertyF IN('restf text', 'testf2 text', 'testf3 text')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("restf", "testf2", "testf3"));
    }

    @Test
    public void testFolderCustomPropertyNotIn()
    {
        String query = "SELECT * FROM tas:folder where tas:TextPropertyF NOT IN('restf text', 'testf2 text', 'testf3 text')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestf", "tesf", "testtttf", "testf", "testf1", "testf4"));
    }

    @Test
    public void testFolderCustomPropertyLike()
    {
        String query = "SELECT * FROM tas:folder where tas:TextPropertyF LIKE '%restf%'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("restf"));
    }

    @Test
    public void testFolderCustomPropertyUnderscore()
    {
        String query = "SELECT * FROM tas:folder where tas:TextPropertyF LIKE 't__tf text'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testf"));
    }

    @Test
    public void testFileIntEquality()
    {
        String query = "SELECT * FROM tas:document where tas:IntPropertyC = '222'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestc.txt"));
    }

    @Test
    public void testFileIntInequality()
    {
        String query = "SELECT * FROM tas:document where tas:IntPropertyC <> '223'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestc.txt", "tesc.txt", "testtttc.txt", "testc.txt", "testc1.txt", "testc2.txt", "testc3.txt"));
    }

    @Test
    public void testFileIntLessThen()
    {
        String query = "SELECT * FROM tas:document where tas:IntPropertyC < '223'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestc.txt"));
    }

    @Test
    public void testFileIntLessThanOrEqual()
    {
        String query = "SELECT * FROM tas:document where tas:IntPropertyC <= '224'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestc.txt", "restc.txt", "tesc.txt"));
    }

    @Test
    public void testFileIntGreaterThanOrEqual()
    {
        String query = "SELECT * FROM tas:document where tas:IntPropertyC >= '224'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("tesc.txt", "testtttc.txt", "testc.txt", "testc1.txt", "testc2.txt", "testc3.txt"));
    }

    @Test
    public void testFileIntGreaterThan()
    {
        String query = "SELECT * FROM tas:document where tas:IntPropertyC > '224'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testtttc.txt", "testc.txt", "testc1.txt", "testc2.txt", "testc3.txt"));
    }

    @Test
    public void testFileIntIn()
    {
        String query = "SELECT * FROM tas:document where tas:IntPropertyC IN('222', '223', '224', '225')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestc.txt", "restc.txt", "tesc.txt", "testtttc.txt"));
    }

    @Test
    public void testFileIntNotIn()
    {
        String query = "SELECT * FROM tas:document where tas:IntPropertyC NOT IN('222', '223', '224')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testtttc.txt", "testc.txt", "testc1.txt", "testc2.txt", "testc3.txt"));
    }

    @Test
    public void testFolderIntEquality()
    {
        String query = "SELECT * FROM tas:folder where tas:IntPropertyF = '222'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestf"));
    }

    @Test
    public void testFolderIntInequality()
    {
        String query = "SELECT * FROM tas:folder where tas:IntPropertyF <> '223'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestf", "tesf", "testtttf", "testf", "testf1", "testf2", "testf3", "testf4"));
    }

    @Test
    public void testFolderIntLessThan()
    {
        String query = "SELECT * FROM tas:folder where tas:IntPropertyF < '223'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestf"));
    }

    @Test
    public void testFolderIntLessThanOrEqual()
    {
        String query = "SELECT * FROM tas:folder where tas:IntPropertyF <= '224'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestf", "tesf", "restf"));
    }

    @Test
    public void testFolderIntGreaterThanOrEqual()
    {
        String query = "SELECT * FROM tas:folder where tas:IntPropertyF >= '224'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("tesf", "testtttf", "testf", "testf1", "testf2", "testf3", "testf4"));
    }

    @Test
    public void testFolderIntGreaterThan()
    {
        String query = "SELECT * FROM tas:folder where tas:IntPropertyF > '224'";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testtttf", "testf", "testf1", "testf2", "testf3", "testf4"));
    }

    @Test
    public void testFolderIntIn()
    {
        String query = "SELECT * FROM tas:folder where tas:IntPropertyF IN('222', '223', '224', '225')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("guestf", "tesf", "restf", "testtttf"));
    }

    @Test
    public void testFolderIntNotIn()
    {
        String query = "SELECT * FROM tas:folder where tas:IntPropertyF NOT IN('222', '223', '224')";
        cmisApi.authenticateUser(testUser).withQuery(query).assertValues().isReturningValues("cmis:name",
                Set.of("testtttf", "testf", "testf1", "testf2", "testf3", "testf4"));
    }
}
