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

    @Test(groups={ TestGroup.ACS_60n}, priority=3)
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
	 * This is a test to check the different spellcheck types searchInsteadFor and
	 * didYouMean
	 * 
	 * @throws Exception
	 */
	@Test(groups = { TestGroup.ACS_60n }, priority = 4)
	public void testSpellCheckType() throws Exception {
		// Create a file with word in cm:name only
		FileModel file = new FileModel("learning", "", "", FileType.TEXT_PLAIN, "");
		dataContent.usingUser(testUser).usingSite(testSite).createContent(file);

		waitForIndexing(file.getName(), true);

		// Correct spelling with cm:name field
		SearchResponse response = SearchSpellcheckQuery(testUser, "cm:name:learning", "learning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNull();

		// Correct spelling with no field
		response = SearchSpellcheckQuery(testUser, "learning", "learning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNull();

		// Correct spelling with a different field. Used cm:content field
		response = SearchSpellcheckQuery(testUser, "cm:content:learning", "learning");

		response.assertThat().entriesListIsEmpty();

		// Incorrect spelling with cm:name field
		response = SearchSpellcheckQuery(testUser, "cm:name:lerning", "lerning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("learning");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with no field
		response = SearchSpellcheckQuery(testUser, "lerning", "lerning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("learning");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with a different field. Used cm:content field
		response = SearchSpellcheckQuery(testUser, "cm:content:lerning", "lerning");

		response.assertThat().entriesListIsEmpty();

		// Create a file with word in cm:name only
		FileModel file2 = new FileModel("leaning", "", "", FileType.TEXT_PLAIN, "");
		dataContent.usingUser(testUser).usingSite(testSite).createContent(file2);

		waitForIndexing(file2.getName(), true);

		// Create a file with word in cm:name and cm:content only
		FileModel file3 = new FileModel("leaning-2", "", "", FileType.TEXT_PLAIN, "leaning-2");
		dataContent.usingUser(testUser).usingSite(testSite).createContent(file3);

		waitForContentIndexing(file3.getContent(), true);

		// Incorrect spelling with cm:name field
		response = SearchSpellcheckQuery(testUser, "cm:name:lerning", "lerning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("leaning");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with no field
		response = SearchSpellcheckQuery(testUser, "lerning", "lerning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("leaning");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with cm:content field
		response = SearchSpellcheckQuery(testUser, "cm:content:lerning", "lerning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("leaning");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Correct spelling with cm:name field
		response = SearchSpellcheckQuery(testUser, "cm:name:learning", "learning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("leaning");
		response.getContext().getSpellCheck().assertThat().field("type").is("didYouMean");

		// Correct spelling with no field
		response = SearchSpellcheckQuery(testUser, "learning", "learning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("leaning");
		response.getContext().getSpellCheck().assertThat().field("type").is("didYouMean");

		// Correct spelling with cm:content field
		response = SearchSpellcheckQuery(testUser, "cm:content:learning", "learning");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("leaning");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");
	}

	/**
	 * This is a test for the spellcheck parameters minEdit and maxPrefix
	 * 
	 * @throws Exception
	 */
	@Test(groups = { TestGroup.ACS_60n }, priority = 5)
	public void testSpellCheckParameters() throws Exception {
		// Create a file with word in cm:name and cm:content
		FileModel file = new FileModel("eklipse", "", "", FileType.TEXT_PLAIN, "eklipse");
		dataContent.usingUser(testUser).usingSite(testSite).createContent(file);

		waitForContentIndexing(file.getContent(), true);

		// Create a file with word in cm:name, cm:title and cm:content
		FileModel file2 = new FileModel("eklipses", "eklipses", "", FileType.TEXT_PLAIN, "eklipses");
		dataContent.usingUser(testUser).usingSite(testSite).createContent(file2);

		waitForContentIndexing(file2.getContent(), true);

		// Search with field not filed in either files
		SearchResponse response = SearchSpellcheckQuery(testUser, "cm:description:eclipse", "eclipse");

		response.assertThat().entriesListIsEmpty();
		response.getContext().assertThat().field("spellCheck").isNull();

		// Incorrect spelling with the field on a file as well
		response = SearchSpellcheckQuery(testUser, "cm:name:eclipse", "eclipse");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("eklipse");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with no field for file1
		response = SearchSpellcheckQuery(testUser, "eclipse", "eclipse");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("eklipse");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with no field for file2
		response = SearchSpellcheckQuery(testUser, "eclipses", "eclipses");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("eklipses");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Search for the field only filed on file2 and not file1
		response = SearchSpellcheckQuery(testUser, "cm:title:eclipses", "eclipses");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("eklipses");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Query using 3 edits (more than spellcheck works for [maxEdits<=2])
		response = SearchSpellcheckQuery(testUser, "elapssed", "elapssed");

		response.assertThat().entriesListIsEmpty();
		response.getContext().assertThat().field("spellCheck").isNull();

		// Query with edit on first letter (does not work with spellcheck [minPrefix=1])
		response = SearchSpellcheckQuery(testUser, "iklipse ", "iklipse ");

		response.assertThat().entriesListIsEmpty();
		response.getContext().assertThat().field("spellCheck").isNull();
	}

	/**
	 * This is a test to check the fields defined for spellcheck in
	 * shared.properties work cm:name, cm:title, cm:description and cm:content are
	 * defined in shared.properties
	 * 
	 * @throws Exception
	 */
	@Test(groups = { TestGroup.ACS_60n }, priority = 6)
	public void testSpellCheckFields() throws Exception {
		// Create a file with same word in all fields
		FileModel file = new FileModel("book", "book", "book", FileType.TEXT_PLAIN, "book");
		dataContent.usingUser(testUser).usingSite(testSite).createContent(file);

		waitForContentIndexing(file.getContent(), true);

		// Incorrect spelling with no field
		SearchResponse response = SearchSpellcheckQuery(testUser, "b00k", "b00k");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("book");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with the cm:name field
		response = SearchSpellcheckQuery(testUser, "cm:name:b00k", "b00k");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("book");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with the cm:title field
		response = SearchSpellcheckQuery(testUser, "cm:title:b00k", "b00k");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("book");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with the cm:description field
		response = SearchSpellcheckQuery(testUser, "cm:description:b00k", "b00k");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("book");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with the cm:description field
		response = SearchSpellcheckQuery(testUser, "cm:content:b00k", "b00k");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("book");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");

		// Incorrect spelling with the cm:author field (not a field in shared.properties
		// for spellcheck)
		response = SearchSpellcheckQuery(testUser, "cm:author:b00k", "b00k");

		response.assertThat().entriesListIsEmpty();
		response.getContext().assertThat().field("spellCheck").isNull();
	}
	
	/**
	 * This is a test to check the ACL tracker works with spellcheck enabled
	 * 
	 * @throws Exception
	 */
	@Test(groups = { TestGroup.ACS_60n }, priority = 7)
	public void testSpellCheckACL() throws Exception {
		
		setupACLSpellcheckTest();
		
		SearchResponse response = SearchSpellcheckQuery(testUser, "prive", "prive");
        
		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("prime");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");
		
		response = SearchSpellcheckQuery(testUser, "prize", "prize");
        
		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("prime");
		response.getContext().getSpellCheck().assertThat().field("type").is("didYouMean");
		
		response = SearchSpellcheckQuery(testUser, "priez", "priez");
        
		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("prize");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");
		
		response = SearchSpellcheckQuery(testUser, "prime", "prime");

		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNull();
		
		response = SearchSpellcheckQuery(testUser2, "price", "price");
        
		response.assertThat().entriesListIsNotEmpty();
		response.getContext().assertThat().field("spellCheck").isNotEmpty();
		response.getContext().getSpellCheck().assertThat().field("suggestions").contains("prime");
		response.getContext().getSpellCheck().assertThat().field("type").is("searchInsteadFor");
	}	
}