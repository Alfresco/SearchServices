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

import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.model.RestRequestSpellcheckModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchRequest;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.search.TestGroup;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Search end point Public API test with spell checking enabled.
 *
 * @author Michael Suzuki
 * @author Meenal Bhave
 */
public class SearchSpellCheckTest extends AbstractSearchServicesE2ETest
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        searchServicesDataPreparation();
    }

    /**
     * Perform the below query 
     * {
     * "spellcheck" : { },
     * "query" : {
     *   "userQuery" : "alfrezco",
     *   "query" : "cm:title:alfrezco"
     * }
     * to yeild the following result.
     * {
     *  "list": {
     *    "pagination": {
     *      "count": 22,
     *      "hasMoreItems": false,
     *      "totalItems": 22,
     *      "skipCount": 0,
     *      "maxItems": 100
     *    },
     *    "context": {
     *      "spellCheck": {
     *        "type": "searchInsteadFor",
     *        "suggestions": [
     *          "alfresco"
     *        ]
     *      }
     *    },
     *    "entries": [...]
     * }
     */
    @Test(groups={ TestGroup.ACS_60n}, priority=1)
    public void testSearchMissSpelled() throws Exception
    {        
        waitForContentIndexing(file4.getContent(), true);
        
        // Name
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:name:alfrezco");
        queryReq.setUserQuery("alfrezco");
        searchReq.setQuery(queryReq);
        searchReq.setSpellcheck(new RestRequestSpellcheckModel());
        assertResponse(query(searchReq));
        
        // Title
        queryReq.setQuery("cm:title:alfrezco");
        queryReq.setUserQuery("alfrezco");
        searchReq.setQuery(queryReq);
        searchReq.setSpellcheck(new RestRequestSpellcheckModel());
        assertResponse(query(searchReq));
        
        // Description
        queryReq.setQuery("cm:description:alfrezco");
        queryReq.setUserQuery("alfrezco");
        searchReq.setQuery(queryReq);
        searchReq.setSpellcheck(new RestRequestSpellcheckModel());
        assertResponse(query(searchReq));
        
        // Content
        queryReq.setQuery("cm:content:alfrezco");
        queryReq.setUserQuery("alfrezco");
        searchReq.setQuery(queryReq);
        searchReq.setSpellcheck(new RestRequestSpellcheckModel());
        assertResponse(query(searchReq));
    }
    
    private void assertResponse(SearchResponse nodes)
    {
        nodes.assertThat().entriesListIsNotEmpty();
        nodes.getContext().assertThat().field("spellCheck").isNotEmpty();
        nodes.getContext().getSpellCheck().assertThat().field("suggestions").contains("alfresco");
        nodes.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");
    }

    /**
     * Perform alternative way by setting the value in spellcheck object.
     * 
     * {
     *   "query": {
     *     "query": "cm:title:alfrezco",
     *     "language": "afts"
     *   },
     *   "spellcheck": {"query": "alfrezco"}
     * }
     */
    @Test(groups={TestGroup.ACS_60n}, priority=2)
    public void testSearchMissSpelledVersion2()
    {        
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:title:alfrezco");
        searchReq.setQuery(queryReq);
        
        RestRequestSpellcheckModel spellCheck = new RestRequestSpellcheckModel();
        spellCheck.setQuery("alfrezco");
        searchReq.setSpellcheck(spellCheck);
        assertResponse(query(searchReq));
    }   

    @Test(groups = { TestGroup.ACS_60n }, priority = 3)
    public void testSearchWithSpellcheckerAndCorrectSpelling()
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:title:alfresco");
        queryReq.setUserQuery("alfresco");
        searchReq.setQuery(queryReq);
        searchReq.setSpellcheck(new RestRequestSpellcheckModel());
        SearchResponse res = query(searchReq);
        Assert.assertNull(res.getContext().getSpellCheck());
        res.assertThat().entriesListIsNotEmpty();
    }

    /**
     * Test to check the different spellcheck types searchInsteadFor and didYouMean
     * Search suggestion is based on maxEdits, count of entries, Alphabetical
     * 
     * @throws Exception
     */
    @Test(groups = { TestGroup.ACS_60n }, priority = 4)
    public void testSpellCheckType() throws Exception
    {
        // Create a file with word in cm:name only
        FileModel file = new FileModel("learning", "", "", FileType.TEXT_PLAIN, "");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(file);

        // Wait for the file to be indexed
        Assert.assertTrue(waitForMetadataIndexing(file.getName(), true));

        // Correct spelling with cm:name field
        SearchResponse response = SearchSpellcheckQuery(testUser, "cm:name:learning", "learning");

        response.assertThat().entriesListIsNotEmpty();
        response.getContext().assertThat().field("spellCheck").isNull();

        // Matching Result, No Spellcheck object returned
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, null, null);

        // Correct spelling with no specific field
        response = SearchSpellcheckQuery(testUser, "learning", "learning");

        // Matching Result, No Spellcheck object returned
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, null, null);

        // Correct spelling with a different field. Used cm:content field
        response = SearchSpellcheckQuery(testUser, "cm:content:learning", "learning");

        // 0 Results, No Spellcheck object returned
        response.assertThat().entriesListIsEmpty();
        testSearchSpellcheckResponse(response, null, null);

        // Incorrect spelling with cm:name field
        response = SearchSpellcheckQuery(testUser, "cm:name:lerning", "lerning");

        // 1 Match with right spelling and Spellcheck = SearchInsteadFor: lerning
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        // TODO: Investigate: Share shows searchInsteadFor = lerning, API shows learning
        // testSearchSpellcheckResponse(response, "searchInsteadFor", "lerning");

        // Incorrect spelling with no field
        response = SearchSpellcheckQuery(testUser, "lerning", "lerning");

        // 1 Match with right spelling and Spellcheck = SearchInsteadFor: lerning
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        // TODO: Investigate: Share shows searchInsteadFor = lerning, API shows learning
        // testSearchSpellcheckResponse(response, "searchInsteadFor", "lerning");

        // Incorrect spelling with a different field. Used cm:content field
        response = SearchSpellcheckQuery(testUser, "cm:content:lerning", "lerning");

        // 0 Results, No Spellcheck object returned
        response.assertThat().entriesListIsEmpty();
        response.getContext().assertThat().field("spellCheck").isNull();

        // Create a file with word with 1 max Edit in cm:name only
        FileModel file2 = new FileModel("leaning", "", "", FileType.TEXT_PLAIN, "");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(file2);

        // Create a file with word in cm:name and cm:content only
        FileModel file3 = new FileModel("leaning 2", "", "", FileType.TEXT_PLAIN, "leaning 2");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(file3);

        Assert.assertTrue(waitForContentIndexing(file3.getContent(), true));

        // Incorrect spelling with cm:name field
        response = SearchSpellcheckQuery(testUser, "cm:name:lerning", "lerning");

        // Matching Result, Spellcheck = searchInsteadFor: leaning
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file2.getName());
        Assert.assertTrue(isContentInSearchResponse(response, file3.getName()), "Expected file not returned in the search results: " + file3.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "leaning");

        // Incorrect spelling with no field
        response = SearchSpellcheckQuery(testUser, "lerning", "lerning");

        // Matching Result, Spellcheck = searchInsteadFor: leaning
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file2.getName());
        Assert.assertTrue(isContentInSearchResponse(response, file3.getName()), "Expected file not returned in the search results: " + file3.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "leaning");

        // Incorrect spelling with cm:content field
        response = SearchSpellcheckQuery(testUser, "cm:content:lerning", "lerning");

        // Matching Result, Spellcheck = searchInsteadFor: leaning
        Assert.assertTrue(isContentInSearchResponse(response, file3.getName()), "Expected file not returned in the search results: " + file3.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "leaning");

        // Correct spelling with cm:name field
        response = SearchSpellcheckQuery(testUser, "cm:name:learning", "learning");

        // Matching Result, Spellcheck = didYouMean: leaning
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "didYouMean", "leaning");

        // Correct spelling with no field
        response = SearchSpellcheckQuery(testUser, "learning", "learning");

        // Matching Result, Spellcheck = didYouMean: leaning
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "didYouMean", "leaning");

        // Correct spelling with cm:content field
        response = SearchSpellcheckQuery(testUser, "cm:content:learning", "learning");

        // Matching Result, Spellcheck = didYouMean: leaning
        Assert.assertTrue(isContentInSearchResponse(response, file3.getName()), "Expected file not returned in the search results: " + file3.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "leaning");
    }

    /**
     * This is a test for the spellcheck parameters minEdit and maxPrefix
     * 
     * @throws Exception
     */
    @Test(groups = { TestGroup.ACS_60n }, priority = 5)
    public void testSpellCheckParameters() throws Exception
    {
        // Create a file with word in cm:name and cm:content
        FileModel file = new FileModel("eklipse", "", "", FileType.TEXT_PLAIN, "eklipse");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(file);

        // Create a file with word in cm:name, cm:title and cm:content
        FileModel file2 = new FileModel("eklipses", "eklipses", "", FileType.TEXT_PLAIN, "eklipses");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(file2);

        Assert.assertTrue(waitForContentIndexing(file2.getName(), true));

        // Search with field not filed in either files
        SearchResponse response = SearchSpellcheckQuery(testUser, "cm:description:'eclipse'", "eclipse");

        // 0 Results, Spellcheck not returned
        testSearchSpellcheckResponse(response, null, null);

        // Incorrect spelling with the field on a file as well
        response = SearchSpellcheckQuery(testUser, "cm:name:'eclipse'", "eclipse");

        // Matching Result, Spellcheck = searchInsteadFor: eklipse
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "eklipse");

        // Incorrect spelling with no field for file1
        response = SearchSpellcheckQuery(testUser, "eclipse", "eclipse");

        // Matching Result, Spellcheck = searchInsteadFor: eklipse
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "eklipse");
        
        // Add Solr Query, to check the suggestions on each shard
        restClient.authenticateUser(testUser).withParams("spellcheck.q=eclipses&spellcheck=on").withSolrAPI().getSelectQuery();

        // Incorrect spelling with no field for file2
        response = SearchSpellcheckQuery(testUser, "eclipsess", "eclipsess");

        // Matching Result, Spellcheck = searchInsteadFor: eklipses
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "eklipses");

        // Search for the field only filed on file2 and not file1
        response = SearchSpellcheckQuery(testUser, "cm:title:'eclipses'", "eclipses");

        // Matching Result, Spellcheck = searchInsteadFor: eklipses
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "eklipses");

        // Query using 3 edits (more than spellcheck works for [maxEdits<=2])
        response = SearchSpellcheckQuery(testUser, "elapssed", "elapssed");

        // 0 Results, No Spellcheck object returned 
        testSearchSpellcheckResponse(response, null, null);

        // Query with edit on first letter (does not work with spellcheck [minPrefix=1])
        response = SearchSpellcheckQuery(testUser, "iklipse ", "iklipse ");

        // 0 Results, No Spellcheck object returned
        testSearchSpellcheckResponse(response, null, null);
    }

    /**
     * This is a test to check the fields defined for spellcheck in
     * shared.properties work cm:name, cm:title, cm:description and cm:content are
     * defined in shared.properties
     * 
     * @throws Exception
     */
    @Test(groups = { TestGroup.ACS_60n }, priority = 6)
    public void testSpellCheckFields() throws Exception
    {
        // Create a file with same word in all fields
        FileModel file = new FileModel("book", "book", "book", FileType.TEXT_PLAIN, "book");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(file);

        Assert.assertTrue(waitForContentIndexing(file.getContent(), true));

        // Incorrect spelling with no field
        SearchResponse response = SearchSpellcheckQuery(testUser, "bo0k", "bo0k");

        // Matching Result, Spellcheck = searchInsteadFor: book
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "book");

        // Incorrect spelling with the cm:name field
        response = SearchSpellcheckQuery(testUser, "cm:name:'bo0k'", "bo0k");

        // Matching Result, Spellcheck = searchInsteadFor: book
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "book");

        // Incorrect spelling with the cm:title field
        response = SearchSpellcheckQuery(testUser, "cm:title:'bo0k'", "bo0k");

        // Matching Result, Spellcheck = searchInsteadFor: book
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "book");

        // Incorrect spelling with the cm:description field
        response = SearchSpellcheckQuery(testUser, "cm:description:'bo0k'", "bo0k");

        // Matching Result, Spellcheck = searchInsteadFor: book
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "book");

        // Incorrect spelling with the cm:description field
        response = SearchSpellcheckQuery(testUser, "cm:content:'bo0k'", "bo0k");

        // Matching Result, Spellcheck = searchInsteadFor: book
        Assert.assertTrue(isContentInSearchResponse(response, file.getName()), "Expected file not returned in the search results: " + file.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "book");

        // Incorrect spelling with the cm:author field (not suggestable field for Spellcheck
        response = SearchSpellcheckQuery(testUser, "cm:author:'bo0k'", "bo0k");

        testSearchSpellcheckResponse(response, null, null);
    }

    /**
     * This is a test to check the ACL tracker works with spellcheck enabled
     * 
     * @throws Exception
     */
    @Test(groups = { TestGroup.ACS_60n }, priority = 7)
    public void testSpellCheckACL() throws Exception
    {
        // Create User 2
        testUser2 = dataUser.createRandomTestUser("User2");

        // Create Private Site 2
        testSite2 = new SiteModel(RandomData.getRandomName("Site2"));
        testSite2.setVisibility(Visibility.PRIVATE);

        testSite2 = dataSite.usingUser(testUser).createSite(testSite2);

        // Make User 2 Site Collaborator
        getDataUser().addUserToSite(testUser2, testSite2, UserRole.SiteCollaborator);

        // Add file <spacebar> to testSite
        FileModel file1 = new FileModel("spacebar", "", "", FileType.TEXT_PLAIN, "spacebar");

        dataContent.usingUser(testUser).usingSite(testSite).createContent(file1);

        // Add file <spacecar> to testSite, testSite2
        FileModel file2 = new FileModel("spacecar", "", "", FileType.TEXT_PLAIN, "spacecar");

        dataContent.usingUser(testUser).usingSite(testSite).createContent(file2);
        dataContent.usingUser(testUser).usingSite(testSite2).createContent(file2);

        Assert.assertTrue(waitForContentIndexing(file2.getContent(), true));

        // Checks for User 2
        // Incorrect spelling with no field
        SearchResponse response = SearchSpellcheckQuery(testUser, "spaceber", "spaceber");

        // Matching Result, Spellcheck = searchInsteadFor: spacebar: alphabetical
        Assert.assertTrue(isContentInSearchResponse(response, file1.getName()), "Expected file not returned in the search results: " + file2.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "spacebar");
        
        // Correct spelling with no field
        response = SearchSpellcheckQuery(testUser, "spacebar", "spacebar");

        // Matching Result, Spellcheck = searchInsteadFor: spacebar
        Assert.assertTrue(isContentInSearchResponse(response, file1.getName()), "Expected file not returned in the search results: " + file1.getName());
        testSearchSpellcheckResponse(response, "didYouMean", "spacecar");

        // Incorrect spelling with no field
        response = SearchSpellcheckQuery(testUser, "spacebra", "spacebra");

        // Matching Result, Spellcheck = searchInsteadFor: spacebar
        Assert.assertTrue(isContentInSearchResponse(response, file1.getName()), "Expected file not returned in the search results: " + file1.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "spacebar");

        // Correct spelling with no field
        response = SearchSpellcheckQuery(testUser, "spacecar", "spacecar");

        // Matching Result, Spellcheck Not returned
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file2.getName());
        testSearchSpellcheckResponse(response, null, null);

        // Add Solr Query, to check the suggestions on the shard
        restClient.authenticateUser(testUser).withParams("spellcheck.q=spaceber&spellcheck=on").withSolrAPI().getSelectQuery();
 
        // Checks for User 2
        // Incorrect spelling for files created no field
        response = SearchSpellcheckQuery(testUser2, "spacefur", "spacefur");

        // Matching Result, Spellcheck = searchInsteadFor: spacecar
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file2.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "spacecar");

        // correct spelling no field
        response = SearchSpellcheckQuery(testUser2, "spacebur", "spacebur");

        // Matching Result, Spellcheck = searchInsteadFor: spacecar
        Assert.assertFalse(isContentInSearchResponse(response, file1.getName()), "Expected file not returned in the search results: " + file1.getName());
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file2.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "spacecar");

        // Incorrect spelling no field
        response = SearchSpellcheckQuery(testUser2, "spacecra", "spacecra");

        // Matching Result, Spellcheck = searchInsteadFor: spacebar
        Assert.assertFalse(isContentInSearchResponse(response, file1.getName()), "Expected file not returned in the search results: " + file1.getName());        
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file2.getName());
        testSearchSpellcheckResponse(response, "searchInsteadFor", "spacecar");

        // Correct spelling no field
        response = SearchSpellcheckQuery(testUser2, "spacecar", "spacecar");

        // Matching Result, Spellcheck not returned
        Assert.assertFalse(isContentInSearchResponse(response, file1.getName()), "Expected file not returned in the search results: " + file1.getName());
        Assert.assertTrue(isContentInSearchResponse(response, file2.getName()), "Expected file not returned in the search results: " + file2.getName());
        testSearchSpellcheckResponse(response, null, null);
    }
}