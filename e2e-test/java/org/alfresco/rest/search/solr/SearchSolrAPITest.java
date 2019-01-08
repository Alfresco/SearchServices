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
package org.alfresco.rest.search.solr;

import java.net.URLEncoder;

import javax.json.JsonArrayBuilder;

import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.model.RestTextResponse;
import org.alfresco.rest.search.AbstractSearchTest;
import org.alfresco.utility.model.TestGroup;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Tests for solr/alfresco Solr API.
 * 
 * @author Meenal Bhave
 */
public class SearchSolrAPITest extends AbstractSearchTest
{
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_112 }, priority = 01)
    public void testGetSolrConfig() throws Exception
    {
        RestTextResponse response = restClient.authenticateUser(adminUserModel).withSolrAPI().getConfig();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().content(Matchers.containsString("config"));
        Assert.assertNotNull(response.getJsonValueByPath("config.requestHandler"));
        Assert.assertNotNull(response.getJsonObjectByPath("config.requestHandler"));

        // TODO: Following asserts fail with error: 
        /*
         * java.lang.IllegalStateException: Expected response body to be verified as JSON, HTML or XML but content-type 'text/plain' is not supported out of the box.
         * Try registering a custom parser using: RestAssured.registerParser("text/plain", <parser type>);
         */

        // response.assertThat().body("config.requestHandler", Matchers.notNullValue());
        // restClient.onResponse().assertThat().body("config.requestHandler",Matchers.notNullValue());
    }
    
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_112 }, priority = 02)
    public void testEditSolrConfig() throws Exception
    {
        String expectedError = "solrconfig editing is not enabled due to disable.configEdit";

        JsonArrayBuilder argsArray = JsonBodyGenerator.defineJSONArray();
        argsArray.add("ANYARGS");

        String postBody = JsonBodyGenerator.defineJSON()
            .add("add-listener", JsonBodyGenerator.defineJSON()
            .add("event", "postCommit")
            .add("name", "newlistener")
            .add("class", "solr.RunExecutableListener")
            .add("exe", "ANYCOMMAND")
            .add("dir", "/usr/bin/")
            .add("args", argsArray)).build().toString();

        // RestTextResponse response =
        restClient.authenticateUser(adminUserModel).withSolrAPI().postConfig(postBody);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);

        restClient.onResponse().assertThat().content(Matchers.containsString(expectedError));

        // TODO: Following asserts fail with error: 
        /*
         * java.lang.IllegalStateException: Expected response body to be verified as JSON, HTML or XML but content-type 'text/plain' is not supported out of the box.
         * Try registering a custom parser using: RestAssured.registerParser("text/plain", <parser type>);
         */

        // response.assertThat().body("error.msg", Matchers.contains(expectedError));
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_112 }, priority = 03)
    public void testGetSolrConfigOverlay() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withSolrAPI().getConfigOverlay();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().content(Matchers.containsString("overlay"));
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_112 }, priority = 04)
    public void testGetSolrConfigParams() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withSolrAPI().getConfigParams();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().content(Matchers.containsString("response"));
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_112 }, priority = 05)
    public void testGetSolrSelect() throws Exception
    {
        String queryParams = "{!xmlparser v='<!DOCTYPE a SYSTEM \"http://localhost:4444/executed\"><a></a>'}";

        String encodedQueryParams = URLEncoder.encode(queryParams, "UTF-8");
        
        restClient.authenticateUser(dataContent.getAdminUser()).withParams(encodedQueryParams).withSolrAPI().getSelectQuery();
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        String errorMsg = "No QueryObjectBuilder defined for node a in {q={!xmlparser";
        Assert.assertTrue(restClient.onResponse().getResponse().body().xmlPath().getString("response").contains(errorMsg));
    }
}