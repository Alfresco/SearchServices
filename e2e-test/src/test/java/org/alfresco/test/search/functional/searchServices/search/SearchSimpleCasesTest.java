/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software.
 * If the software was purchased under a paid Alfresco license, the terms of
 * the paid license agreement will prevail. Otherwise, the software is
 * provided under the following open source license terms:
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */
package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SearchSimpleCasesTest extends AbstractSearchServicesE2ETest
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        searchServicesDataPreparation();
        waitForContentIndexing(file4.getContent(), true);
    }

    @Test(priority=1)
    public void testSearchNameField()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:pangram");
        queryReq.setUserQuery("pangram");
        searchReq.setQuery(queryReq);
        SearchResponse response = queryAsUser(testUser, "cm:name:pangram");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=2)
    public void testSearchTitleField()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:title:cars");
        queryReq.setUserQuery("cars");
        searchReq.setQuery(queryReq);
        SearchResponse response2 = queryAsUser(testUser, "cm:title:cars");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response2.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=3)
    public void testSearchDescriptionField()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:description:alfresco");
        queryReq.setUserQuery("alfresco");
        searchReq.setQuery(queryReq);
        SearchResponse response3 = queryAsUser(testUser, "cm:description:alfresco");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response3.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=4)
    public void testSearchContentField()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:content:unique");
        queryReq.setUserQuery("unique");
        searchReq.setQuery(queryReq);
        SearchResponse response4 = queryAsUser(testUser, "cm:content:unique");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response4.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=6)
    public void testSearchTextFile()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:pangram.txt");
        queryReq.setUserQuery("pangram.txt");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "cm:name:pangram.txt");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=7)
    public void testSearchPDFFile()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:cars.PDF");
        queryReq.setUserQuery("cars.PDF");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "cm:name:cars.PDF");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=8)
    public void testSearchDocxFile()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:alfresco.docx");
        queryReq.setUserQuery("alfresco.docx");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "cm:name:alfresco.docx");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=9)
    public void testSearchODTFile()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:unique.ODT");
        queryReq.setUserQuery("unique.ODT");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "cm:name:unique.ODT");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=12)
    public void testSearchPhraseQueries()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:alfresco");
        queryReq.setUserQuery("alfresco");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "The quick brown fox jumps over the lazy dog");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=13)
    public void testSearchExactTermQueries()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:alfresco");
        queryReq.setUserQuery("alfresco");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "=alfresco");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=14)
    public void testSearchConjunctionQueries()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:unique");
        queryReq.setUserQuery("unique");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "unique AND search");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=15)
    public void testSearchDisjunctionQueries()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:cars");
        queryReq.setUserQuery("cars");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "file OR discovery");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=16)
    public void testSearchNegationQueries()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:pangram");
        queryReq.setUserQuery("pangram");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "pangram NOT pan");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=17)
    public void testSearchWildcardQueries()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:alfresco");
        queryReq.setUserQuery("alfresco");
        searchReq.setQuery(queryReq);
        SearchResponse response6 = queryAsUser(testUser, "al?res*");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response6.assertThat().entriesListIsNotEmpty();
    }

    @Test(priority=18)
    public void searchSpecialCharacters()
    {
        String specialCharfileName = "è¥äæ§ç§-åæ.pdf";
        FileModel file = new FileModel(specialCharfileName, "è¥äæ§ç§-åæ¬¯¸" + "è¥äæ§ç§-åæ¬¯¸", "è¥äæ§ç§-åæ¬¯¸", FileType.TEXT_PLAIN,
                "Text file with Special Characters: " + specialCharfileName);
        dataContent.usingUser(testUser).usingSite(testSite).createContent(file);

        waitForIndexing(file.getName(), true);

        SearchRequest searchReq = createQuery("name:'" + specialCharfileName + "'");
        SearchResponse nodes = query(searchReq);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsNotEmpty();

        restClient.onResponse().assertThat().body("list.entries.entry[0].name", Matchers.equalToIgnoringCase(specialCharfileName));
    }
}
