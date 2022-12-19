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

import org.alfresco.rest.search.FacetFieldBucket;
import org.alfresco.rest.search.RestRequestFacetFieldModel;
import org.alfresco.rest.search.RestRequestFacetFieldsModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.RestResultBucketsModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.model.FileModel;
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

    @Test(priority=5)
    public void testSearchUpdateContent() throws InterruptedException
    {
        String originalText = String.valueOf(System.currentTimeMillis());
        String newText = String.valueOf(System.currentTimeMillis() + 300000);

        // Create test file to be accessed only by this test method to avoid inconsistent results when querying updates
        FileModel updateableFile = createFileWithProvidedText(originalText + ".txt", originalText);

        // Verify that 1 occurrence of the original text is found
        SearchResponse response1 = queryAsUser(testUser, "cm:content:" + originalText);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response1.getEntries().size(), 1, "Expected 1 original text before update");

        // Verify that 0 occurrences of the replacement text are found
        SearchResponse response2 = queryAsUser(testUser, "cm:content:" + newText);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response2.getEntries().size(), 0, "Expected 0 new text before update");

        // Update the content
        String newContent = "Description: Contains provided string: " + newText;
        dataContent.usingUser(adminUserModel).usingSite(testSite).usingResource(updateableFile)
                .updateContent(newContent);
        Assert.assertTrue(waitForContentIndexing(newText, true));

        // Verify that 0 occurrences of the original text are found
        SearchResponse response3 = queryAsUser(testUser, "cm:content:" + originalText);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response3.getEntries().size(), 0, "Expected 0 original text after update");

        // Verify that 1 occurrence of the replacement text is found
        SearchResponse response4 = queryAsUser(testUser, "cm:content:" + newText);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response4.getEntries().size(), 1, "Expected 1 new text before update");

        //TODO remove this
        throw new IllegalArgumentException("Temporary exception to prove test is run");
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
    public void searchWithFacedFields() throws InterruptedException
    {
        String uniqueText = String.valueOf(System.currentTimeMillis());

        // Create test file to be accessed only by this test method to avoid inconsistent results
        createFileWithProvidedText(uniqueText + ".ODT", uniqueText);

        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:content:" + uniqueText);
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
        bucket1.assertThat().field("count").is(1);
    }
}
