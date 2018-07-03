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

import java.sql.ResultSet;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.AbstractSearchTest;
import org.alfresco.rest.search.SearchSqlJDBCRequest;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.TestGroup;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;
import org.hamcrest.Matchers;

/**
 * Tests for /sql end point Search API.
 * 
 * @author Meenal Bhave
 */
public class SearchSQLPhraseTest extends AbstractSearchTest
{
    FileModel fileBanana, fileYellowBanana, fileBigYellowBanana, fileBigBananaBoat, fileYellowBananaBigBoat, fileBigYellowBoat;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        super.dataPreparation();

        // Create files with different phrases
        fileBanana = new FileModel(unique_searchString + "-1.txt", "banana", "phrase searching", FileType.TEXT_PLAIN, "banana");
        dataContent.usingUser(userModel).usingSite(siteModel).createContent(fileBanana);

        fileYellowBanana = new FileModel(unique_searchString + "-2.txt", "yellow banana", "phrase searching", FileType.TEXT_PLAIN, "yellow banana");
        dataContent.usingUser(userModel).usingSite(siteModel).createContent(fileYellowBanana);

        fileBigYellowBanana = new FileModel(unique_searchString + "-3.txt", "big yellow banana", "phrase searching", FileType.TEXT_PLAIN, "big yellow banana");
        dataContent.usingUser(userModel).usingSite(siteModel).createContent(fileBigYellowBanana);

        fileBigBananaBoat = new FileModel(unique_searchString + "-4.txt", "big boat", "phrase searching", FileType.TEXT_PLAIN, "big boat");
        dataContent.usingUser(userModel).usingSite(siteModel).createContent(fileBigBananaBoat);

        fileYellowBananaBigBoat = new FileModel(unique_searchString + "-5.txt", "yellow banana big boat", "phrase searching", FileType.TEXT_PLAIN, "yellow banana big boat");
        dataContent.usingUser(userModel).usingSite(siteModel).createContent(fileYellowBananaBigBoat);

        fileBigYellowBoat = new FileModel(unique_searchString + "-6.txt", "big yellow boat", "phrase searching", FileType.TEXT_PLAIN, "big yellow boat");
        dataContent.usingUser(userModel).usingSite(siteModel).createContent(fileBigYellowBoat);

        waitForIndexing(fileBigYellowBoat.getName(), true);
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 1)
    public void testPhraseQueries() throws Exception
    {
        // yellow banana: 5 results expected
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name, cm_content from alfresco where cm_content = '(yellow banana)'");
        sqlRequest.setLimit(10);

        RestResponse response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(5));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("cm_content"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase(fileYellowBanana.getContent()));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("cm_name"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase(fileYellowBanana.getName()));

        // yellow banana big boat: 6 results expected
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name, cm_content from alfresco where cm_content = '(yellow banana big boat)'");
        sqlRequest.setLimit(10);

        response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(6));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("cm_content"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase(fileYellowBananaBigBoat.getContent()));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("cm_name"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase(fileYellowBananaBigBoat.getName()));

        // yellow banana big boat: 4 results expected
        sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name, cm_content from alfresco where cm_content = '(big boat)'");
        sqlRequest.setLimit(10);

        response = searchSql(sqlRequest);

        restClient.assertStatusCodeIs(HttpStatus.OK);
        response.assertThat().body("list.pagination.count", Matchers.equalTo(4));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("cm_content"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase(fileBigBananaBoat.getContent()));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("cm_name"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase(fileBigBananaBoat.getName()));
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority = 2)
    public void testPhraseQueriesViaJDBC() throws Exception
    {
        // yellow banana: 5 results expected
        SearchSqlJDBCRequest sqlRequest = new SearchSqlJDBCRequest();
        String sql = "select cm_name, cm_content from alfresco where cm_content = '(yellow banana)'";
        sqlRequest.setSql(sql);
        sqlRequest.setAuthUser(userModel);

        ResultSet rs = restClient.withSearchSqlViaJDBC().executeQueryViaJDBC(sqlRequest);
        Assert.assertNotNull(rs);
        Assert.assertNull(sqlRequest.getErrorDetails());

        // Set expected Results
        Integer expectedCount = 5;
        String firstResultFileName = fileYellowBanana.getName();
        String firstResultContent = fileYellowBanana.getContent();
        
        Integer i = 0;
        while (rs.next())
        {
            // Field values are retrieved
            Assert.assertNotNull(rs.getString("cm_name"));
            Assert.assertNotNull(rs.getString("cm_content"));
            
            if(i == 0)
            {
                Assert.assertEquals(rs.getString("cm_name"), firstResultFileName);
                Assert.assertEquals(rs.getString("cm_content"), firstResultContent);
            }
            
            i++;
        }
        
        Assert.assertEquals(i, expectedCount);
        
        // yellow banana big boat: 6 results expected        
        sql = "select cm_name, cm_content from alfresco where cm_content = '(yellow banana big boat)'";
        sqlRequest.setSql(sql);
        sqlRequest.setAuthUser(userModel);

        rs = restClient.withSearchSqlViaJDBC().executeQueryViaJDBC(sqlRequest);
        Assert.assertNotNull(rs);
        Assert.assertNull(sqlRequest.getErrorDetails());

        expectedCount = 6;
        firstResultFileName = fileYellowBananaBigBoat.getName();
        firstResultContent = fileYellowBananaBigBoat.getContent();
        
        i = 0;
        while (rs.next())
        {
            // Field values are retrieved
            Assert.assertNotNull(rs.getString("cm_name"));
            Assert.assertNotNull(rs.getString("cm_content"));
            
            if(i == 0)
            {
                Assert.assertEquals(rs.getString("cm_name"), firstResultFileName);
                Assert.assertEquals(rs.getString("cm_content"), firstResultContent);
            }
            
            i++;
        }
        
        Assert.assertEquals(i, expectedCount);
        
        // big boat: 4 results expected        
        sql = "select cm_name, cm_content from alfresco where cm_content = '(big boat)'";
        sqlRequest.setSql(sql);
        sqlRequest.setAuthUser(userModel);

        rs = restClient.withSearchSqlViaJDBC().executeQueryViaJDBC(sqlRequest);
        Assert.assertNotNull(rs);
        Assert.assertNull(sqlRequest.getErrorDetails());

        expectedCount = 4;
        firstResultFileName = fileBigBananaBoat.getName();
        firstResultContent = fileBigBananaBoat.getContent();
        i = 0;
        while (rs.next())
        {
            // Field values are retrieved
            Assert.assertNotNull(rs.getString("cm_name"));
            Assert.assertNotNull(rs.getString("cm_content"));
            
            if(i == 0)
            {
                Assert.assertEquals(rs.getString("cm_name"), firstResultFileName);
                Assert.assertEquals(rs.getString("cm_content"), firstResultContent);
            }
            
            i++;
        }
        
        Assert.assertEquals(i, expectedCount);
    }
}