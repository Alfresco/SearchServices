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
package org.alfresco.rest.search.sql;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.requests.search.SearchSQLJDBC;
import org.alfresco.rest.search.AbstractSearchTest;
import org.alfresco.rest.search.SearchSqlJDBCRequest;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.Test;
import org.testng.Assert;

/**
 * Search SQL end point test via JDBC.
 * @author MSuzuki
 * @author Meenal Bhave
 *
 */
public class SearchSQLViaJDBCTest extends AbstractSearchTest
{
    List<String> sites = new ArrayList<String>();
    SearchSQLJDBC searchSql;
    SearchSqlJDBCRequest sqlRequest = new SearchSqlJDBCRequest();

    @AfterMethod
    public void cleanUp() throws SQLException
    {
        restClient.withSearchSqlViaJDBC().clearSearchQuery(sqlRequest);
        sqlRequest = new SearchSqlJDBCRequest();
        sites = new ArrayList<String>();
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority=01)
    public void testQueryPublicSite() throws SQLException
    {

        SiteModel publicSite = new SiteModel(RandomData.getRandomName("SiteSearch"));
        publicSite.setVisibility(Visibility.PUBLIC);

        publicSite = dataSite.usingUser(adminUserModel).createSite(publicSite);

        String sql = "select SITE,CM_OWNER from alfresco where SITE ='" + publicSite.getTitle() + "' group by SITE,CM_OWNER";
        sqlRequest.setSql(sql);
        sqlRequest.setAuthUser(userModel);

        ResultSet rs = restClient.withSearchSqlViaJDBC().executeQueryViaJDBC(sqlRequest);

        while (rs.next())
        {
            // User can see the Public Site created by other user
            Assert.assertNotNull(rs.getString("SITE"));
            Assert.assertTrue(publicSite.getTitle().equalsIgnoreCase(rs.getString("SITE")));
            
            Assert.assertNotNull(rs.getString("CM_OWNER"));
            Assert.assertTrue(rs.getString("CM_OWNER").contains(adminUserModel.getUsername()));
        }
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority=02)
    public void testQueryMyCreatedPrivateSite() throws SQLException
    {
        String sql = "select distinct SITE from alfresco where SITE ='" + siteModel.getTitle() + "'";
        sqlRequest.setSql(sql);
        sqlRequest.setAuthUser(userModel);

        ResultSet rs = restClient.withSearchSqlViaJDBC().executeQueryViaJDBC(sqlRequest);

        while (rs.next())
        {
            // User can see Own Private Site
            Assert.assertNotNull(rs.getString("SITE"));
            Assert.assertTrue(siteModel.getTitle().equalsIgnoreCase(rs.getString("SITE")));
        }
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority=03)
    public void testQueryPrivateSiteWithSuperUser() throws SQLException
    {
        String sql = "select distinct SITE from alfresco where SITE ='" + siteModel.getTitle() + "'";
        sqlRequest.setSql(sql);
        sqlRequest.setAuthUser(adminUserModel);

        ResultSet rs = restClient.withSearchSqlViaJDBC().executeQueryViaJDBC(sqlRequest);

        while (rs.next())
        {
            // Super user can see Private Site created by other user
            Assert.assertNotNull(rs.getString("SITE"));
            Assert.assertTrue(siteModel.getTitle().equalsIgnoreCase(rs.getString("SITE")));
        }
    }

    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority=04)
    public void testQueryPrivateSiteWithSimpleUser() throws SQLException
    {
        UserModel managerUser = dataUser.createRandomTestUser("UserSearchMgr");
        
        String sql = "select SITE from alfresco where SITE = '" + siteModel.getTitle() + "'";
        sqlRequest.setSql(sql);
        sqlRequest.setAuthUser(managerUser);

        // Non admin user can NOT see Private Site created by other user
        ResultSet rs = restClient.withSearchSqlViaJDBC().executeQueryViaJDBC(sqlRequest);
        Assert.assertFalse(rs.next());
    }
    
    
    
    @Test(groups = { TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10 }, priority=05)
    public void testQueryErrorReturned() throws SQLException
    {
        String expectedError = "Column 'SITE1' not found";
        
        String sql = "select SITE1 from alfresco";
        sqlRequest.setSql(sql);
        sqlRequest.setAuthUser(userModel);

        // Appropriate error is retrieved when SQL is incorrect
        ResultSet rs = restClient.withSearchSqlViaJDBC().executeQueryViaJDBC(sqlRequest);
        Assert.assertNull(rs);

        String error = sqlRequest.getErrorDetails();
        Assert.assertNotNull(error);
        Assert.assertTrue(error.contains(expectedError), "Error shown: " + error + " Error expected: " + expectedError);
    }
}