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

package org.alfresco.test.search.functional.searchServices.search.crosslocale;

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


/**
 * Tests including all different tokenization (false, true, both) modes with Exact Term queries.
 * Search Services must be configured with Cross Locale enabled in order to run this tests.
 */
public class SearchExactTermTest extends AbstractE2EFunctionalTest
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;
    
    private static final DateFormat QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private String fromDate;
    private String toDate;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();
        
        deployCustomModel("model/tokenised-model.xml");
        
        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);

        FolderModel testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        FileModel tok1 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "Tokenised");
        tok1.setName("tok1-" + tok1.getName());

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:tok:document");
        properties.put(PropertyIds.NAME, tok1.getName());
        properties.put("tok:true", "running");
        properties.put("tok:false", "running");
        properties.put("tok:both", "running");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(tok1, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        waitForIndexing(tok1.getName(), true);
        
        
        // Calculate time query range
        Date today = new Date();
        
        LocalDateTime yesterday = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        yesterday = yesterday.plusDays(-1);
        fromDate = QUERY_DATE_FORMAT.format(Date.from(yesterday.atZone(ZoneId.systemDefault()).toInstant()));
        
        LocalDateTime tomorrow = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        tomorrow = tomorrow.plusDays(1);
        toDate = QUERY_DATE_FORMAT.format(Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant()));
        
    }

    @Test(priority = 1)
    public void testExactTermTokenised()
    {
        
        String query = "=tok:true:running";
        SearchResponse response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

        query = "=tok:both:running";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);
        
        query = "cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 100, query);

        query = "tok:true:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);
        
        query = "=tok:true:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

        query = "tok:both:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);
        
        query = "=tok:both:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

    }

    @Test(priority = 2)
    public void testExactTermNonTokenised()
    {
        
        String query = "=tok:false:running";
        SearchResponse response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

        query = "cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 100, query);

        query = "tok:false:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);
        
        query = "=tok:false:running AND cm:created:['" + fromDate + "' TO '" + toDate + "']";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

    }
    
    /**
     * Since this test is not strictly related with exact term searching,
     * it includes a set of queries to verify different tokenisation options:
     * 
     *   - false: only exact term (running) will provide results
     *   - true | both: exact term (running) and tokens (run) will provide results
     */
    @Test(priority = 3)
    public void testTokenisation()
    {
        
        String query = "tok:false:running";
        SearchResponse response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

        query = "tok:false:run";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 0, query);
        
        query = "tok:true:running";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

        query = "tok:true:run";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);
        
        query = "tok:both:running";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);

        query = "tok:both:run";
        response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1, query);
        
    }
    
}
