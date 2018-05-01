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
package org.alfresco.rest.search;

import org.alfresco.rest.model.RestRequestSpellcheckModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.TestGroup;
import org.junit.Assert;
import org.testng.annotations.Test;

/**
 * Search end point Public API test with spell checking enabled.
 * @author Michael Suzuki
 *5
 */
public class SearchSpellCheckTest extends AbstractSearchTest
{
   
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
     * 
     * @throws Exception
     */
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ACS_60n}, priority=1)
    public void testSearchMissSpelled() throws Exception
    {        
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
    
    private void assertResponse(SearchResponse nodes) throws Exception
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
     * @throws Exception
     */
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ACS_60n}, priority=2)
    public void testSearchMissSpelledVersion2() throws Exception
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

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ACS_60n}, priority=3)
    public void testSearchWithSpellcheckerAndCorrectSpelling() throws Exception
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
    
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ACS_60n}, priority=4)
    public void testSpellCheckType() throws Exception
    {
        // Create a file with mis-spelt name, expect spellcheck type = didYouMean
        FileModel file = new FileModel(unique_searchString + "-1.txt", "uniquee" + "uniquee", "uniquee", FileType.TEXT_PLAIN, "Unique text file for search ");
        dataContent.usingUser(userModel).usingSite(siteModel).createContent(file);

        waitForIndexing(file.getName(), true);
        
        // Search
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:title:uniquee");
        queryReq.setUserQuery("uniquee");
        searchReq.setQuery(queryReq);
        searchReq.setSpellcheck(new RestRequestSpellcheckModel());
        SearchResponse nodes = query(searchReq);
        
        nodes.assertThat().entriesListIsNotEmpty();
        nodes.getContext().assertThat().field("spellCheck").isNotEmpty();
        nodes.getContext().getSpellCheck().assertThat().field("suggestions").contains("unique");
        nodes.getContext().getSpellCheck().assertThat().field("type").is("didYouMean");
    }
}
