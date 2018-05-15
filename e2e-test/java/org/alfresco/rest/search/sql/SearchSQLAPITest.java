/*
 * Copyright (C) 2018 Alfresco Software Limited.
 * This file is part of Alfresco
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
 */
package org.alfresco.rest.search.sql;

import org.alfresco.rest.search.AbstractSearchTest;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.utility.model.TestGroup;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

/**
 * Tests for /sql end point Search API.
 * 
 * @author Meenal Bhave
 */
public class SearchSQLAPITest extends AbstractSearchTest
{
    String sql = "select SITE, CM_OWNER from alfresco group by SITE,CM_OWNER";
    String[] locales = { "en-US" };
    String solrFormat = "solr";
    
    /**
     * API post:
     * {
     * "stmt": "select SITE from alfresco",
     * "locales" : ["en_US"],
     * "format" : "solr",
     * "timezone":"",
     * "includeMetadata":false
     * }
     * Example Response: In Solr Format
     * {
     * "result-set": 
     * {
     *  "docs": 
     *   [
     *    {
     *     "SITE": 
     *     [
     *      "swsdp"
     *     ]
     *    },
     *   {
     *    "SITE": 
     *    [
     *     "swsdp"
     *    ]
     *   }
     *  ]
     * }
     */
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 01)
    public void testWithSolr() throws Exception
    {
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setFormat(solrFormat);
        sqlRequest.setLocales(locales);
        
        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", org.hamcrest.Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs", org.hamcrest.Matchers.notNullValue());
    }

    /**
     * API post example:
     * {
     * "stmt": "select SITE from alfresco",
     * "locales" : ["en_US"],
     * "format" : "json",
     * "timezone":"",
     * "includeMetadata":false
     * }
     * Example Response: In json Format
     * {
     * "list": 
     * {
     *  "pagination": 
     *  {
            "count": 103,
            "hasMoreItems": false,
            "totalItems": 103,
            "skipCount": 0,
            "maxItems": 1000
        },
     *  "entries": 
     *   [
     *   "entry": [
                    {
                        "label": "SITE",
                        "value": "[\"swsdp\"]"
                    }
                ],
          "entry": [
                    {
                        "label": "SITE",
                        "value": "[\"swsdp\"]"
                    }
                ]      
     *   ]
     * }
     */
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 02)
    public void testWithJson() throws Exception
    {
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(locales);
        // Format not set

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", org.hamcrest.Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", org.hamcrest.Matchers.notNullValue());
        
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(locales);
        sqlRequest.setFormat("json"); // Format json

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", org.hamcrest.Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", org.hamcrest.Matchers.notNullValue());
        
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(locales);
        sqlRequest.setFormat("abcd"); // Format any other than solr

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", org.hamcrest.Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", org.hamcrest.Matchers.notNullValue());
    }
    
    /**
     * API post:
     * {
     * "stmt": "select SITE from alfresco",
     * "locales" : ["en_US"],
     * "format" : "solr",
     * "timezone":"",
     * "includeMetadata":true
     * }
     * Example Response: In Solr Format: Includes metadata: aliases, fields, isMetadata=true
     * {
     *     "result-set": {
     *         "docs": [
     *             {
     *                 "aliases": {
     *                     "SITE": "SITE"
     *                 },
     *                 "isMetadata": true,
     *                 "fields": [
     *                     "SITE"
     *                 ]
     *             },
     *             {
     *                 "SITE": [
     *                    "swsdp"
     *                 ]
     *             },
     *             {
     *                 "SITE": [
     *                     "swsdp"
     *                 ]
     *             },
     *             {
     *                 "RESPONSE_TIME": 79,
     *                 "EOF": true
     *             }
     *         ]
     *     }
     * }
     */
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 03)
    public void testWithSolrIncludeMetadata() throws Exception
    {
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setFormat(solrFormat);
        sqlRequest.setLocales(locales);
        sqlRequest.setIncludeMetadata(true);
        
        searchSql(sqlRequest);
        
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", org.hamcrest.Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs", org.hamcrest.Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.SITE", org.hamcrest.Matchers.equalToIgnoringCase("SITE"));
        restClient.onResponse().assertThat().body("result-set.docs[0].isMetadata", org.hamcrest.Matchers.is(true));
        restClient.onResponse().assertThat().body("result-set.docs[0].fields[0]", org.hamcrest.Matchers.equalToIgnoringCase("SITE"));
    }

    /**
     * API post example:
     * {
     * "stmt": "select SITE from alfresco limit 2",
     * "locales" : ["en_US"],
     * "format" : "json",
     * "timezone":"",
     * "includeMetadata":false
     * }
     * Example Response: In json Format
     * {
     * "list": 
     * {
     *  "pagination": 
     *  {
            "count": 3,
            "hasMoreItems": false,
            "totalItems": 3,
            "skipCount": 0,
            "maxItems": 1000
        },
     *  "entries": 
     *   [
     *   "entry": [
                    {
                        "label": "aliases",
                        "value": "{\"SITE\":\"SITE\"}"
                    },
                    {
                        "label": "isMetadata",
                        "value": "true"
                    },
                    {
                        "label": "fields",
                        "value": "[\"SITE\"]"
                    }
                ]
            },
     *   "entry": [
                    {
                        "label": "SITE",
                        "value": "[\"swsdp\"]"
                    }
                ],
          "entry": [
                    {
                        "label": "SITE",
                        "value": "[\"swsdp\"]"
                    }
                ]      
     *   ]
     * }
     */
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 04)
    public void testWithJsonIncludeMetadata() throws Exception
    {        
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setFormat("");
        sqlRequest.setLocales(locales);
        sqlRequest.setIncludeMetadata(true);
        
        searchSql(sqlRequest);
        
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", org.hamcrest.Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", org.hamcrest.Matchers.notNullValue());
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", org.hamcrest.Matchers.equalToIgnoringCase("aliases"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", org.hamcrest.Matchers.equalToIgnoringCase("{\"SITE\":\"SITE\",\"cm_owner\":\"CM_OWNER\"}"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", org.hamcrest.Matchers.equalToIgnoringCase("isMetadata"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", org.hamcrest.Matchers.is("true"));        
        restClient.onResponse().assertThat().body("list.entries.entry[0][2].label", org.hamcrest.Matchers.equalToIgnoringCase("fields"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][2].value", org.hamcrest.Matchers.equalToIgnoringCase("[\"SITE\",\"cm_owner\"]"));
    }
}