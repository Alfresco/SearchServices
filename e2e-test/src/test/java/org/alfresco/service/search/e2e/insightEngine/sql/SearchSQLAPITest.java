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
package org.alfresco.service.search.e2e.insightEngine.sql;

import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isIn;
import static org.hamcrest.Matchers.not;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.service.search.e2e.searchservices.AbstractSearchTest;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for /sql end point Search API.
 * 
 * @author Meenal Bhave
 */
public class SearchSQLAPITest extends AbstractSearchTest
{
    /**
     * Path selector to get the values of all cm_name entries for returned search results.
     * <p>
     * For example it will retrieve the "alfresco.txt" from:
     * <pre>
     * {"list": {
     *    "entries": [
     *       {"entry": [{
     *          "label": "cm_name",
     *          "value": "alfresco.txt"
     *       }]},
     *       ...
     * </pre>
     */
    private static final String CM_NAME_VALUES = "list.entries.collect {it.entry.findAll {it.label == 'cm_name'}}.flatten().value";
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
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.SITE", equalToIgnoringCase("SITE"));
        restClient.onResponse().assertThat().body("result-set.docs[0].isMetadata", is(true));
        restClient.onResponse().assertThat().body("result-set.docs[0].fields[0]", equalToIgnoringCase("SITE"));
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
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", equalToIgnoringCase("aliases"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", equalToIgnoringCase("{\"SITE\":\"SITE\",\"cm_owner\":\"CM_OWNER\"}"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", equalToIgnoringCase("isMetadata"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", is("true"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][2].label", equalToIgnoringCase("fields"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][2].value", equalToIgnoringCase("[\"SITE\",\"cm_owner\"]"));
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
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", not("aliases"));

        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(sql);
        sqlRequest.setIncludeMetadata(false); // Format not set

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set", Matchers.nullValue());
        restClient.onResponse().assertThat().body("list.entries", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", not("aliases"));

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
        response.assertThat().body("list.entries.entry[0][0].value", not("0"));
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

        restClient.onResponse().assertThat().body("list.pagination.count", is(0));
        restClient.onResponse().assertThat().body("list.pagination.totalItems", is(0));
        restClient.onResponse().assertThat().body("list.pagination.hasMoreItems", is(false));
        restClient.onResponse().assertThat().body("list.entries", empty());

        // Results expected for query as User does has access to the private site
        restClient.authenticateUser(userPerm).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("list.pagination.count", greaterThan(0));
        restClient.onResponse().assertThat().body("list.pagination.totalItems", greaterThan(0));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", equalToIgnoringCase("SITE"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", equalToIgnoringCase("[\"" + siteModel.getId() + "\"]"));
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

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.NOT_INSIGHT_ENGINE }, priority = 13)
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
    
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 14)
    public void testSelectStar() throws Exception
    {
        // Select * with Limit, json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select * from alfresco");
        sqlRequest.setLimit(1);

        RestResponse response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.maxItems", Matchers.equalTo(1));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", equalToIgnoringCase("PATH"));

        // Select * with Limit, solr format: Also covered in JDBC
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select * from alfresco");
        sqlRequest.setFormat("solr");
        sqlRequest.setIncludeMetadata(true);
        sqlRequest.setLimit(1);

        response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_name", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_created", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_creator", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_modified", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_modifier", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_owner", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.OWNER", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.TYPE", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.LID", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.DBID", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_title", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_description", Matchers.notNullValue());

        // This type of assertion is required because of a '.' in the field name
        Assert.assertTrue(response.getResponse().body().jsonPath().get("result-set.docs[0].aliases").toString().contains("cm_content.size=cm_content.size"));
        Assert.assertTrue(response.getResponse().body().jsonPath().get("result-set.docs[0].aliases").toString().contains("cm_content.mimetype=cm_content.mimetype"));
        Assert.assertTrue(response.getResponse().body().jsonPath().get("result-set.docs[0].aliases").toString().contains("cm_content.encoding=cm_content.encoding"));
        Assert.assertTrue(response.getResponse().body().jsonPath().get("result-set.docs[0].aliases").toString().contains("cm_content.locale=cm_content.locale"));

        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_lockOwner", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.SITE", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.PARENT", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.PATH", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.PRIMARYPARENT", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.ASPECT", Matchers.notNullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.QNAME", Matchers.notNullValue());
        
        // Test that cm_content and any other random field does not appear in the response
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.cm_content", Matchers.nullValue());
        restClient.onResponse().assertThat().body("result-set.docs[0].aliases.RandomNonExistentField", Matchers.nullValue());
    }
    
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 15)
    public void testDistinct() throws Exception
    {
        // Select distinct site: json format
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select distinct Site from alfresco");
        sqlRequest.setLimit(10);

        RestResponse response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.maxItems", Matchers.equalTo(10));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", equalToIgnoringCase("site"));

        // Select distinct cm_name: solr format
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select distinct cm_name from alfresco limit 5");
        sqlRequest.setFormat("solr");

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("result-set.docs[0].cm_name", Matchers.notNullValue());
    }

    /** Check that a filter query can affect which results are included. */
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_11 }, priority = 16)
    public void testFilterQuery() throws Exception
    {
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        String siteSQL = "select SITE, CM_OWNER from alfresco where SITE='" + siteModel.getId() + "'";
        sqlRequest.setSql(siteSQL);
        // Add a filter query to only include results from inside the site (i.e. all results).
        String[] filterQuery = { "SITE:'" + siteModel.getId() + "'" };
        sqlRequest.setFilterQuery(filterQuery);

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("list.pagination.count", greaterThan(0));
        restClient.onResponse().assertThat().body("list.pagination.totalItems", greaterThan(0));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", equalToIgnoringCase("SITE"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", equalToIgnoringCase("[\"" + siteModel.getId() + "\"]"));

        // Now try instead removing everything from the site (i.e. all results).
        String[] inverseFilterQuery = { "-SITE:'" + siteModel.getId() + "'" };
        sqlRequest.setFilterQuery(inverseFilterQuery);

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("list.pagination.count", is(0));
        restClient.onResponse().assertThat().body("list.pagination.totalItems", is(0));
        restClient.onResponse().assertThat().body("list.pagination.hasMoreItems", is(false));
        restClient.onResponse().assertThat().body("list.entries", empty());
    }

    /** Check that the combination of multiple filter queries produce the intersection of results. */
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_11 }, priority = 17)
    public void testCombiningFilterQueries() throws Exception
    {
        String siteSQL = "select cm_name from alfresco where SITE='" + siteModel.getId() + "'";
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(siteSQL);
        // Add a filter query to only include results from inside the site (i.e. all results).
        String[] filterQueries = { "-cm_name:'cars'", "-cm_name:'pangram'" };
        sqlRequest.setFilterQuery(filterQueries);

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        restClient.onResponse().assertThat().body("list.pagination.count", greaterThan(0));
        restClient.onResponse().assertThat().body("list.pagination.totalItems", greaterThan(0));
        // Check that pangram.txt and cars.txt were both filtered out.
        restClient.onResponse().assertThat()
                    .body(CM_NAME_VALUES, everyItem(not(isIn(asList("pangram.txt", "cars.txt")))));
    }

    /** Check that an empty list of filter queries doesn't remove anything from the results. */
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_11 }, priority = 18)
    public void testEmptyFilterQuery() throws Exception
    {
        String siteSQL = "select cm_name from alfresco where SITE='" + siteModel.getId() + "'";
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql(siteSQL);
        // Add an empty list of filter queries.
        sqlRequest.setFilterQuery(new String[]{});

        searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        // Check that the cm_names of all nodes in the site are returned.
        restClient.onResponse().assertThat()
                    .body(CM_NAME_VALUES, containsInAnyOrder("documentLibrary", SEARCH_DATA_SAMPLE_FOLDER, "pangram.txt", "cars.txt", "alfresco.txt", unique_searchString + ".txt"));
    }
}
