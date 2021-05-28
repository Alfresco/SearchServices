/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.alfresco.test.search.functional.searchServices.search;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.alfresco.rest.search.SearchResponse;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class SearchExactTermTest extends AbstractE2EFunctionalTest
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;
    
    private static final DateFormat QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();
        
        deployCustomModel("model/ses-model.xml");
        
        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);

        FolderModel testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        FileModel agent1 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "Agent");
        agent1.setName("ses1-" + agent1.getName());

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:ses:businessplan");
        properties.put(PropertyIds.NAME, agent1.getName());
        properties.put("ses:agent", "1");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(agent1, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        // Wait for the file to be indexed
        waitForIndexing(agent1.getName(), true);
    }

    @Test(priority = 1)
    public void testExactTermQueryConjunction()
    {
        
        Date today = new Date();
        
        LocalDateTime yesterday = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        yesterday = yesterday.plusDays(-1);
        String fromDate = QUERY_DATE_FORMAT.format(Date.from(yesterday.atZone(ZoneId.systemDefault()).toInstant()));
        
        LocalDateTime tomorrow = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        tomorrow = tomorrow.plusDays(1);
        String toDate = QUERY_DATE_FORMAT.format(Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant()));
        
        String query = "=ses:agent:1";
        SearchResponse response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

        query = "cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 100, query);

        query = "ses:agent:1 AND cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);
        
        query = "=ses:agent:1 AND cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

    }

}
