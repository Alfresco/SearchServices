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

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.AbstractSearchTest;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;
import org.hamcrest.Matchers;

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

        restClient.onResponse().assertThat().body("result-set", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs", Matchers.notNullValue());
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

        restClient.onResponse().assertThat().body("result-set", Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", Matchers.notNullValue());

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(locales);
        sqlRequest.setFormat("json"); // Format json

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", Matchers.notNullValue());

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(locales);
        sqlRequest.setFormat("abcd"); // Format any other than solr

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", Matchers.notNullValue());
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

        restClient.onResponse().assertThat().body("result-set", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.SITE", Matchers.equalToIgnoringCase("SITE"));
        restClient.onResponse().assertThat().body("result-set.docs[0].isMetadata", Matchers.is(true));
        restClient.onResponse().assertThat().body("result-set.docs[0].fields[0]", Matchers.equalToIgnoringCase("SITE"));
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

        restClient.onResponse().assertThat().body("result-set", Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("aliases"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase("{\"SITE\":\"SITE\",\"cm_owner\":\"CM_OWNER\"}"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("isMetadata"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.is("true"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][2].label", Matchers.equalToIgnoringCase("fields"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][2].value", Matchers.equalToIgnoringCase("[\"SITE\",\"cm_owner\"]"));
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 05)
    public void testIncludeMetadataFalse() throws Exception
    {
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setFormat("solr"); // Format solr
        sqlRequest.setIncludeMetadata(false);

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases", Matchers.nullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].isMetadata", Matchers.nullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].fields", Matchers.nullValue());

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setIncludeMetadata(false);
        sqlRequest.setFormat("json"); // Format json

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.not("aliases"));

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setIncludeMetadata(false); // Format not set

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.not("aliases"));

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setFormat(solrFormat); // IncludeMetadata = false when not specified

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases", Matchers.nullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].isMetadata", Matchers.nullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].fields", Matchers.nullValue());
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 06)
    public void testLocales() throws Exception
    {
        String[] noLocales = {}; // Not specified

        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(noLocales);

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        String[] singleLocale = { "en-Uk" };

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(singleLocale);

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        String[] multipleLocales = { "en-US", "ja" };

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(multipleLocales);

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 07)
    public void testTimezone() throws Exception
    {
        String timezone = ""; // Not specified

        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setTimezone(timezone);

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        timezone = "UTC"; // UTC

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setTimezone(timezone);

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);

        timezone = "false"; // Invalid timezone is ignored

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setTimezone(timezone);

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 8)
    public void testAggregateSQL() throws Exception
    {
        String agSql = "select count(*) FROM alfresco where TYPE='cm:content'";

        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(agSql);

        RestResponse response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.entries", Matchers.notNullValue());
        response.assertThat().body("list.entries.entry[0][0].value", Matchers.not("0"));
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 9)
    public void testLimit() throws Exception
    {
        Integer defaultLimit = 1000;

        Integer limit = null; // Limit defaults to the default setting when null

        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLimit(limit);

        RestResponse response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.maxItems", Matchers.equalTo(defaultLimit));

        limit = 0; // Limit defaults to the default setting when 0
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLimit(limit);

        response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.maxItems", Matchers.equalTo(defaultLimit));

        limit = 100; // Limit is applied correctly, with maxItems = limit
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLimit(limit);

        response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.maxItems", Matchers.equalTo(limit));
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 10)
    public void testAuthenticationError() throws Exception
    {
        UserModel dummyUser = dataUser.createRandomTestUser("UserSearchDummy");
        dummyUser.setPassword("incorrect-password");

        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(locales);

        restClient.authenticateUser(dummyUser).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 11)
    public void testPermissions() throws Exception
    {
        UserModel userNoPerm = dataUser.createRandomTestUser("UserSearchNoPerm");
        UserModel userPerm = dataUser.createRandomTestUser("UserSearchPerm");

        dataUser.addUserToSite(userPerm, siteModel, UserRole.SiteContributor);

        String siteSQL = "select SITE, CM_OWNER from alfresco where SITE='" + siteModel.getId() + "'";
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(siteSQL);

        // 0 results expected for query as User does not have access to the private site
        restClient.authenticateUser(userNoPerm).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("list.pagination.count", Matchers.is(0));
        restClient.onResponse().assertThat().body("list.pagination.totalItems", Matchers.is(0));
        restClient.onResponse().assertThat().body("list.pagination.hasMoreItems", Matchers.is(false));
        restClient.onResponse().assertThat().body("list.entries", Matchers.empty());

        // Results expected for query as User does has access to the private site
        restClient.authenticateUser(userPerm).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("list.pagination.count", Matchers.greaterThan(0));
        restClient.onResponse().assertThat().body("list.pagination.totalItems", Matchers.greaterThan(0));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("SITE"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase("[\"" + siteModel.getId() + "\"]"));
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 12)
    public void testErrors() throws Exception
    {
        String incorrectSQL = ""; // Missing SQL

        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(incorrectSQL);

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.containsString("Required stmt parameter is missing"));

        incorrectSQL = "select SITE from unknownTable"; // Wrong table name

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(incorrectSQL);

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.notNullValue());
        // restClient.onResponse().assertThat().body("error.briefSummary", Matchers.containsString("Table 'unknownTable' not found"));

        incorrectSQL = "select Column1 from alfresco"; // Wrong ColumnName

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(incorrectSQL);

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.containsString("Column 'Column1' not found"));

        incorrectSQL = "select SITE alfresco"; // BAD SQL Grammar: from missing

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(incorrectSQL);

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.containsString("Unable to execute the query"));

        incorrectSQL = "select SITE, CM_OWNER from alfresco group by SITE"; // BAD SQL Grammar: CM_OWNER is not being grouped

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(incorrectSQL);

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.containsString("Expression 'CM_OWNER' is not being grouped"));

        incorrectSQL = "delete SITE from alfresco"; // BAD SQL Grammar

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(incorrectSQL);

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("error.briefSummary", Matchers.containsString("Was expecting one of"));
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API }, priority = 13)
    public void testErrorForSQLAPIWithASS() throws Exception
    {
        String acsVersion = serverHealth.getAlfrescoVersion();
        HttpStatus expectedStatus = HttpStatus.OK;

        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setLocales(locales);

        // Set expected result based on ACS Version and ASS Version
        if (!acsVersion.startsWith("6"))
        {
            expectedStatus = HttpStatus.NOT_FOUND;
        }
        else
        {
            expectedStatus = HttpStatus.BAD_REQUEST;
        }

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(expectedStatus);
    }
}