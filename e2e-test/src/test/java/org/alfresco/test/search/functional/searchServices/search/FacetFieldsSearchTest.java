/*
 * Copyright (C) 2019 Alfresco Software Limited.
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

import java.util.ArrayList;
import java.util.List;

import javax.json.Json;
import javax.json.JsonObject;

import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.search.FacetFieldBucket;
import org.alfresco.rest.search.RestRequestFacetFieldModel;
import org.alfresco.rest.search.RestRequestFacetFieldsModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.RestResultBucketsModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.search.TestGroup;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Faceted search test with FacetFields
 * @author Meenal Bhave
 *
 */
public class FacetFieldsSearchTest extends AbstractE2EFunctionalTest
{   
    private UserModel userWithNoAccess, userCanAccessTextFile;
    private SiteModel testSite;
    private String fname;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();
        
        fname = unique_searchString + "facet";

        testSite = new SiteModel(RandomData.getRandomName("SiteSearch"));
        testSite.setVisibility(Visibility.PRIVATE);
        
        testSite = dataSite.usingUser(testUser).createSite(testSite);
        
        // Create another user who would not have access to the Private Site created by testUser
        userWithNoAccess = dataUser.createRandomTestUser("UserSearch2");
        userCanAccessTextFile = dataUser.createRandomTestUser("UserSearch3");
        
        // Create a folder and a file as test User
        FolderModel testFolder = new FolderModel(fname);
        dataContent.usingUser(testUser).usingSite(testSite).createFolder(testFolder);

        FileModel textFile = new FileModel(fname + "-1.txt", fname, fname, FileType.TEXT_PLAIN, fname + " file for search ");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(textFile);

        FileModel htmlFile = new FileModel(fname + "-2.html", FileType.HTML, fname + " file 2 for search ");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(htmlFile);
        
        // Set Node Permissions to allow access for user for text File
        JsonObject userPermission = Json.createObjectBuilder()
                .add("permissions", Json.createObjectBuilder().add("isInheritanceEnabled", false)
                        .add("locallySet",Json.createObjectBuilder().add("authorityId", userCanAccessTextFile.getUsername())
                                .add("name", "SiteConsumer").add("accessStatus", "ALLOWED")))
                .build();
        String putBody = userPermission.toString();

        restClient.authenticateUser(testUser).withCoreAPI().usingNode(textFile).updateNode(putBody);
        
        // Wait for the file to be indexed
        waitForIndexing(htmlFile.getName(), true);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_121 })
    public void testSearchFacetFieldsBucketExcludedWhenMinCount2() throws Exception
    {
        // Create Query with FacetFields: Site and Content MimeType
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("name:" + fname);
        query.setQuery(queryReq);

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> facets = new ArrayList<>();

        facets.add(new RestRequestFacetFieldModel("SITE", "SEARCH.FACET_FIELDS.SITE", 0)); // MinCount = 0
        facets.add(new RestRequestFacetFieldModel("cm:content.mimetype", "Mimetype", 2)); // MinCount = 2

        facetFields.setFacets(facets);
        query.setFacetFields(facetFields);

        // Search query using user who created site
        SearchResponse response = query(query);
        
        // Expect MimeType bucket with size 1 is excluded as minCount = 2 won't be reached
        Assert.assertEquals(response.getContext().getFacetsFields().size(), 1);

        // Expect SITE bucket is included and MinCount defaults to 1
        RestResultBucketsModel facetFieldList = response.getContext().getFacetsFields().get(0);
        Assert.assertEquals(facetFieldList.getLabel(), "SEARCH.FACET_FIELDS.SITE");
        Assert.assertEquals(facetFieldList.getBuckets().size(), 1);

        FacetFieldBucket bucket1 = facetFieldList.getBuckets().get(0);
        bucket1.assertThat().field("label").is(testSite.getId());
        bucket1.assertThat().field("filterQuery").contains(testSite.getId());
        bucket1.assertThat().field("count").is(3); // One folder and 2 files created above

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_121 })
    public void testSearchWithFacetFieldsMinCountChecks() throws Exception
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("name:" + fname);
        query.setQuery(queryReq);

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> facets = new ArrayList<>();

        // MinCount not set
        facets.add(new RestRequestFacetFieldModel("SITE", "SEARCH.FACET_FIELD1.SITE", null)); // MinCount Not set
        facets.add(new RestRequestFacetFieldModel("cm:content.mimetype", "SEARCH.FACET_FIELD2.Mimetype", 1)); // MinCount = 1

        facetFields.setFacets(facets);
        query.setFacetFields(facetFields);

        // Search query using user who created site
        SearchResponse response = query(query);

        // Expect 2 Facet buckets to be retrieved: MinCount when not set defaults to 1
        List<RestResultBucketsModel> facetFieldBucketsList = response.getContext().getFacetsFields();
        Assert.assertEquals(facetFieldBucketsList.size(), 2, "FacetField");

        RestResultBucketsModel facetFieldList = facetFieldBucketsList.get(0);
        Assert.assertEquals(facetFieldList.getLabel(), "SEARCH.FACET_FIELD1.SITE");

        FacetFieldBucket bucket1 = facetFieldList.getBuckets().get(0);
        bucket1.assertThat().field("label").is(testSite.getId());
        bucket1.assertThat().field("filterQuery").contains(testSite.getId());
        bucket1.assertThat().field("count").is(3);

        // MimeType bucket will be shown with 2 buckets
        facetFieldList = facetFieldBucketsList.get(1);
        Assert.assertEquals(facetFieldList.getLabel(), "SEARCH.FACET_FIELD2.Mimetype");
        Assert.assertEquals(facetFieldList.getBuckets().size(), 2);

        bucket1 = facetFieldList.getBuckets().get(0);
        bucket1.assertThat().field("label").is("text/html");
        bucket1.assertThat().field("filterQuery").contains("text/html");
        bucket1.assertThat().field("count").is(1);
        bucket1.assertThat().field("display").is("HTML");

        bucket1 = facetFieldList.getBuckets().get(1);
        bucket1.assertThat().field("label").is("text/plain");
        bucket1.assertThat().field("filterQuery").contains("text/plain");
        bucket1.assertThat().field("count").is(1);
        bucket1.assertThat().field("display").is("Plain Text");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_121 })
    public void testSearchWithFacetFieldsNoFacetsWhenNoAccess() throws Exception
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("name:" + fname);
        query.setQuery(queryReq);

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> facets = new ArrayList<>();

        facets.add(new RestRequestFacetFieldModel("SITE", "SEARCH.FACET_FIELD1.SITE", null)); // MinCount Not set
        facets.add(new RestRequestFacetFieldModel("cm:content.mimetype", "SEARCH.FACET_FIELD2.Mimetype", 1)); // MinCount = 1

        facetFields.setFacets(facets);
        query.setFacetFields(facetFields);
        
        // Search query using other user
        SearchResponse response = restClient.authenticateUser(userWithNoAccess).withSearchAPI().search(query);
        
        // User has No access to matching content hence no buckets expected
        Assert.assertNull(response.getContext().getFacetsFields());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SEARCH, TestGroup.ASS_121 })
    public void testSearchWithFacetFieldsOnlyFacetsWhereAccess() throws Exception
    {
        SearchRequest query = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("name:" + fname);
        query.setQuery(queryReq);

        RestRequestFacetFieldsModel facetFields = new RestRequestFacetFieldsModel();
        List<RestRequestFacetFieldModel> facets = new ArrayList<>();

        facets.add(new RestRequestFacetFieldModel("SITE", "SEARCH.FACET_FIELD1.SITE", null)); // MinCount Not set
        facets.add(new RestRequestFacetFieldModel("cm:content.mimetype", "SEARCH.FACET_FIELD2.Mimetype", 1)); // MinCount = 1

        facetFields.setFacets(facets);
        query.setFacetFields(facetFields);

        // Search query using user that can access 1 text file
        SearchResponse response = restClient.authenticateUser(userCanAccessTextFile).withSearchAPI().search(query);

        List<RestResultBucketsModel> facetFieldBucketsList = response.getContext().getFacetsFields();
        Assert.assertEquals(facetFieldBucketsList.size(), 2);

        // Check FacetField 1
        RestResultBucketsModel facetFieldList = facetFieldBucketsList.get(0);

        // User has granular permissions to content within this private site, so expect the Site bucket
        Assert.assertEquals(facetFieldList.getLabel(), "SEARCH.FACET_FIELD1.SITE");

        FacetFieldBucket bucket1 = facetFieldList.getBuckets().get(0);
        bucket1.assertThat().field("label").is(testSite.getId());
        bucket1.assertThat().field("filterQuery").contains(testSite.getId());
        bucket1.assertThat().field("count").is(1);

        // Expect MimeType bucket shows only 1 bucket where user has access to content
        facetFieldList = facetFieldBucketsList.get(1);
        Assert.assertEquals(facetFieldList.getLabel(), "SEARCH.FACET_FIELD2.Mimetype");
        Assert.assertEquals(facetFieldList.getBuckets().size(), 1);

        // User has access to text file alone, so expect bucket for text/plain and not for html content.
        bucket1 = facetFieldList.getBuckets().get(0);
        bucket1.assertThat().field("label").is("text/plain");
        bucket1.assertThat().field("label").isNot("text/html");
        bucket1.assertThat().field("filterQuery").contains("text/plain");
        bucket1.assertThat().field("count").is(1);
        bucket1.assertThat().field("display").is("Plain Text");
    }
}
