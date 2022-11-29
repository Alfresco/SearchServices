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

import org.alfresco.rest.exception.EmptyRestModelCollectionException;
import org.alfresco.rest.search.FacetFieldBucket;
import org.alfresco.rest.search.RestRequestFacetFieldModel;
import org.alfresco.rest.search.RestRequestFacetFieldsModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.RestResultBucketsModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

public class SearchCasesTest extends AbstractSearchServicesE2ETest
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        searchServicesDataPreparation();
        Assert.assertTrue(waitForContentIndexing(file4.getContent(), true));
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

    @Test(priority=5)
    public void testSearchUpdateContent() throws EmptyRestModelCollectionException
    {
        //TODO: remove - temp check for debugging
        SearchResponse responsePBQ = queryAsUser(testUser, "cm:content:purplenotthereyet");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        String beforeAnything = "purplenotthereyet entries: " + responsePBQ.getEntries().size();
        if (responsePBQ.getEntries().size() != 0)
        {
            String msg = "Name: " + responsePBQ.getEntryByIndex(0).getName();
            String content = " Content: " + responsePBQ.getEntryByIndex(0).getContent().toString();
            Assert.assertEquals( beforeAnything, msg + content);
        }
        Assert.assertEquals( beforeAnything, "purplenotthereyet entries: 0");

        SearchResponse response4 = queryAsUser(testUser, "cm:content:brown");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        response4.assertThat().entriesListIsNotEmpty();

        //TODO: remove - temp check for debugging
        SearchResponse responsePAQ = queryAsUser(testUser, "cm:content:purplenotthereyet");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        String afterQuery = "purplenotthereyet entries after query: " + responsePAQ.getEntries().size();
        Assert.assertEquals(afterQuery, "purplenotthereyet entries after query: 0");

        String newContent = "The quick purple fox jumps over the lazy dog";
        dataContent.usingUser(adminUserModel).usingSite(testSite).usingResource(file)
                .updateContent(newContent);
        Assert.assertTrue(waitForContentIndexing("purple", true));

        //TODO: remove - artificially long wait for debugging
        Utility.waitToLoopTime(60);

        SearchResponse response5 = queryAsUser(testUser, "cm:content:brown");
        restClient.assertStatusCodeIs(HttpStatus.OK);

        //TODO: remove - temp check for debugging
        SearchResponse responseRA = queryAsUser(testUser, "cm:content:purple");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        String afterUpdate = "brown entries: " + response5.getEntries().size() + "  purple entries: " + responseRA.getEntries().size();
        Assert.assertEquals(afterUpdate, "brown entries: 0  purple entries: 1");

        int initialEntriesSize = response4.getEntries().size();
        response5.assertThat().entriesListCountIs( initialEntriesSize - 1);
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

    /**
     * {
     *  "query": {
     *              "query": "*"
     *           },
     *  "facetFields": {
     *      "facets": [{"field": "cm:mimetype"},{"field": "modifier"}]
     *  }
     * }
     */
    @Test(priority=10)
    public void searchWithFacedFields()
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:content:unique");
        query.setQuery(queryReq);

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> facets = new ArrayList<>();
        facets.add(new RestRequestFacetFieldModel("cm:mimetype"));
        facets.add(new RestRequestFacetFieldModel("modifier"));
        facetFields.setFacets(facets);
        query.setFacetFields(facetFields);

        SearchResponse response = query(query);

        Assert.assertNotNull(response.getContext().getFacetsFields());
        Assert.assertFalse(response.getContext().getFacetsFields().isEmpty());
        Assert.assertNull(response.getContext().getFacetQueries());
        Assert.assertNull(response.getContext().getFacets());

        RestResultBucketsModel model = response.getContext().getFacetsFields().get(0);
        Assert.assertEquals(model.getLabel(), "modifier");

        model.assertThat().field("label").is("modifier");
        FacetFieldBucket bucket1 = model.getBuckets().get(0);
        bucket1.assertThat().field("label").is(testUser.getUsername());
        bucket1.assertThat().field("display").is("FN-" + testUser.getUsername() + " LN-" + testUser.getUsername());
        bucket1.assertThat().field("filterQuery").is("modifier:\"" + testUser.getUsername() + "\"");
        bucket1.assertThat().field("count").isGreaterThan(0);
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
