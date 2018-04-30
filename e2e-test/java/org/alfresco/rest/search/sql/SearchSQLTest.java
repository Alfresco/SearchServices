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

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

import org.alfresco.dataprep.SiteService.Visibility;
import org.alfresco.rest.search.AbstractSearchTest;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.testng.annotations.BeforeClass;
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
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        
        connectionString = properties.getFullServerUrl() + "/alfresco/api/-default-/public/search/versions/1";
        
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser("UserSearch");
        managerUser = dataUser.createRandomTestUser("UserSearchMgr");
        
        //Create site 
        adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
                
        siteModel = new SiteModel(RandomData.getRandomName("SiteSearch"));
        siteModel.setVisibility(Visibility.PRIVATE);
        
        siteModel = dataSite.usingUser(userModel).createSite(siteModel);
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.INSIGHT_10})
    public void queryTest() throws SQLException
    {
        String sql = "select SITE, CM_OWNER from alfresco group by SITE,CM_OWNER";
        Properties props = new Properties();
        props.put("user", adminUser.getUsername());
        props.put("password", adminUser.getPassword());
        Connection con = null;
        Statement stmt = null;
        ResultSet rs = null;
        try 
        {
            con = DriverManager.getConnection(connectionString, props);
            stmt = con.createStatement();
            rs = stmt.executeQuery(sql);
            int i=0;
            while (rs.next())
            {
                ++i;
                assertNotNull(rs.getString("SITE"));
            }
        } 
        finally 
        {
            rs.close();
            stmt.close();
            con.close();
        }
    }
}