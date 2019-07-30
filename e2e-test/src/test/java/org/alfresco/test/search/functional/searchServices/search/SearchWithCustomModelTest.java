/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.test.search.functional.searchServices.search;

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
 * Purpose of this TestClass is to test that the search range query tests work as expected with CustomModels
 * Tests added for Search-1359
 * 
 * @author meenal bhave
 */

public class SearchWithCustomModelTest extends AbstractE2EFunctionalTest
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();

        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);

        FolderModel testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        Long uniqueRef = System.currentTimeMillis();

        FileModel expenseLondon = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "Expense");
        expenseLondon.setName("fin1-" + expenseLondon.getName());

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expenseLondon.getName());
        properties.put("finance:No", uniqueRef);
        properties.put("finance:amount", 300);

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(expenseLondon, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        // Location value is set to London
        cmisApi.authenticateUser(testUser).usingResource(expenseLondon).addSecondaryTypes("P:finance:ParkEx").assertThat()
                .secondaryTypeIsAvailable("P:finance:ParkEx");
        cmisApi.authenticateUser(testUser).usingResource(expenseLondon).updateProperty("finance:ParkingLocation", "London");

        FileModel expenseParis = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "Expense");
        expenseParis.setName("fin2-" + expenseParis.getName());

        properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expenseParis.getName());
        properties.put("finance:No", uniqueRef + 1);
        properties.put("finance:amount", 100);
        properties.put("finance:Title", "Airport Taxi Outgoing");
        properties.put("finance:Emp", "David A");
        properties.put("finance:Desc", "David's Taxi");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(expenseParis, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        // Location value is set to Paris
        cmisApi.authenticateUser(testUser).usingResource(expenseParis).addSecondaryTypes("P:finance:ParkEx").assertThat()
                .secondaryTypeIsAvailable("P:finance:ParkEx");
        cmisApi.authenticateUser(testUser).usingResource(expenseParis).updateProperty("finance:ParkingLocation", "Paris");

        FileModel expenseNoLocation = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "receipt");
        expenseNoLocation.setName("fin3-" + expenseNoLocation.getName());

        properties = new HashMap<>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expenseNoLocation.getName());
        properties.put("finance:No", uniqueRef + 2);
        properties.put("finance:amount", 0);
        properties.put("finance:Title", "Hotel Stay");
        properties.put("finance:Emp", "Daniel S");
        properties.put("finance:Desc", "Daniel's Taxi");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(expenseNoLocation, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        // Location value is set to null
        cmisApi.authenticateUser(testUser).usingResource(expenseNoLocation).addSecondaryTypes("P:finance:ParkEx").assertThat()
                .secondaryTypeIsAvailable("P:finance:ParkEx");

        // Wait for the file to be indexed
        waitForIndexing(expenseNoLocation.getName(), true);
    }

    // Search-1359: Search AFTS Query with Range
    @Test(priority = 1)
    public void testRangeQueryTextField()
    {
        // Search Range Query
        SearchResponse response = queryAsUser(testUser, "finance_ParkingLocation:[* TO London]");
        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Content where Location = London is returned, If property is not set, its ignored.
        Assert.assertEquals(response.getPagination().getCount(), 1);

        response = queryAsUser(testUser, "finance_ParkingLocation:[London TO *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        // Content where Location = London, Paris is returned, If property is not set, its ignored.
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance_ParkingLocation:[London To Paris]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance_ParkingLocation:[* To *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);
    }

    @Test(priority = 2)
    public void testRangeQueryTextFieldWhiteSpace()
    {
        SearchResponse response = queryAsUser(testUser, "finance:Emp:[* TO Daniel]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        // Tockenised field, so Returns Daniel, David A (A before Daniel)
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance:Emp:[Dan TO *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance:Emp:[Dan To David]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance:Emp:[* To *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);
    }

    @Test(priority = 3)
    public void testRangeQueryTextFieldNonFacetable()
    {
        // Search Range Query
        SearchResponse response = queryAsUser(testUser, "finance:Title:[* TO H]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);

        response = queryAsUser(testUser, "finance:Title:[Hotel TO *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        // Tockenised field, so Includes: Hotel, (Airport) Taxi Outgoing
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance:Title:[B To Hotel]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        // Tockenised field, so Includes: (Airport) Taxi Outgoing
        Assert.assertEquals(response.getPagination().getCount(), 1);
        
        response = queryAsUser(testUser, "finance:Title:[B To I]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);

        response = queryAsUser(testUser, "finance:Title:[* To *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);
    }

    @Test(priority = 4)
    public void testRangeQueryTextFieldNotTockenised()
    {
        SearchResponse response = queryAsUser(testUser, "finance:Desc:[* TO David]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        // Includes: Daniel's Taxi
        Assert.assertEquals(response.getPagination().getCount(), 1);

        response = queryAsUser(testUser, "finance:Desc:[Dan TO *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance:Desc:[Dan To David]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);

        response = queryAsUser(testUser, "finance:Desc:[* To *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);
    }

    @Test(priority = 5)
    public void testRangeQueryDoubleField()
    {
        // Search Range Query
        SearchResponse response = queryAsUser(testUser, "finance:amount:[* TO 100]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance_amount:[100 TO *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance_amount:[100 To 300]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance_amount:[* To *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 3);
    }
}
