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

import org.alfresco.rest.model.RestRequestSpellcheckModel;
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
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API})
    public void searchMissSpelled() throws Exception
    {        
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:title:alfrezco");
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
    }
    @Test
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
    public void searchMissSpelledVersion2() throws Exception
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
    @Test
    public void searchWithSpellcheckerAndCorrectSpelling() throws Exception
    {
        SearchRequest searchReq = new SearchRequest();
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:title:alfresco");
        queryReq.setUserQuery("alfresco");
        searchReq.setQuery(queryReq);
        searchReq.setSpellcheck(new RestRequestSpellcheckModel());
        SearchResponse res = query(searchReq);
        Assert.assertNull(res.getContext());
        res.assertThat().entriesListIsNotEmpty();
    }
}
