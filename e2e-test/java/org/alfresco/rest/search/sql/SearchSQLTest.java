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
package org.alfresco.rest.search.sql;

import static org.junit.Assert.assertNotNull;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.search.AbstractSearchTest;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Search SQL end point Public API test.
 * @author Michael Suzuki
 *
 */
public class SearchSQLTest extends AbstractSearchTest
{
    UserModel userModel, adminUser, managerUser; 
    SiteModel siteModel;
    FileModel document;
    String connectionString;
    private Connection con;
    private Statement stmt;
    private ResultSet rs;
    List<String> sites;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        connectionString = String.format("jdbc:alfresco://%s:%s?collection=alfresco", 
                                         properties.getServer(),
                                         properties.getPort());
        //Prepare users, one with full access and one with limited access.
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser("UserSearch");
        managerUser = dataUser.createRandomTestUser("UserSearch");
        //Create a private site 
        siteModel = new SiteModel(RandomData.getRandomName("SiteSearch"));
        siteModel.setVisibility(Visibility.PRIVATE);
        siteModel = dataSite.usingUser(managerUser).createSite(siteModel);
        //Allow tracker to index.
    }
    
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10})
    public void queryPublicSiteWithSimpleUser() throws SQLException
    {
        String sql = "select SITE,CM_OWNER from alfresco group by SITE,CM_OWNER";
        rs = executeSql(userModel.getUsername(), userModel.getPassword(), sql);
        while (rs.next())
        {
            assertNotNull(rs.getString("SITE"));
            //Check for the private site which was created
            assertNotNull(rs.getString("CM_OWNER"));
        }
    }
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10})
    public void queryMyCreatedPrivateSite() throws SQLException
    {
        String sql = "select SITE from alfresco where SITE ='" + siteModel.getTitle() + "'";
        rs = executeSql(managerUser.getUsername(), managerUser.getPassword(), sql);
        while (rs.next())
        {
            assertNotNull(rs.getString("SITE"));
            assertTrue(siteModel.getTitle().equalsIgnoreCase(rs.getString("SITE")));
        }
    }
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10})
    public void queryPrivateSiteWithSuperUser() throws SQLException
    {
        String sql = "select SITE from alfresco where SITE ='" + siteModel.getTitle() + "'";
        rs = executeSql(adminUser.getUsername(), adminUser.getPassword(), sql);
        while (rs.next())
        {
            assertNotNull(rs.getString("SITE"));
            assertTrue(siteModel.getTitle().equalsIgnoreCase(rs.getString("SITE")));
        }
    }
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10})
    public void queryPrivateSiteWithSimpleUser() throws SQLException
    {
        String sql = "select SITE from alfresco where SITE = '" + siteModel.getTitle() +"'";
        rs = executeSql(userModel.getUsername(), userModel.getPassword(), sql);
        assertFalse(rs.next());
    }

    
    @AfterMethod
    public void close()
    {
        sites = null;
        try
        {
            if(rs != null)
            {
                rs.close();
            }
            if(stmt != null)
            {
                stmt.close();
            }
            if(con != null)
            {
                con.close();
            }
        }
        catch (SQLException e)
        {
            // TODO: handle exception
        }
    }
    @BeforeMethod
    public void prep()
    {
        sites = new ArrayList<String>();
    }
    private Connection getConnection(String username, String password) throws SQLException
    {
        Properties props = new Properties();
        props.put("user", username);
        props.put("password", password);
        return DriverManager.getConnection(connectionString, props);
    }
    private ResultSet executeSql(String username, String password, String sql) throws SQLException
    {
        con = getConnection(username, password);
        stmt = con.createStatement();
        return stmt.executeQuery(sql);
    }
}