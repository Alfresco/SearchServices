/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */

package org.alfresco.service.search;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.rest.search.SearchResponse;
import org.alfresco.service.search.AbstractSearchServiceE2E;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;
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

public class SearchWithCustomModelTest extends AbstractSearchServiceE2E
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    private FolderModel testFolder;

    private FileModel expenseLondon, expenseParis, expenseNoLocation;

    @BeforeClass(alwaysRun = true)
    public void setupEnvironment() throws Exception
    {
        serverHealth.assertServerIsOnline();

        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);

        testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        Long uniqueRef = System.currentTimeMillis();

        expenseLondon = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "Expense");
        expenseLondon.setName("fin1-" + expenseLondon.getName());

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expenseLondon.getName());
        properties.put("finance:No", uniqueRef);
        properties.put("finance:amount", 300);

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(expenseLondon, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        // Location value is set to London
        cmisApi.authenticateUser(testUser).usingResource(expenseLondon).addSecondaryTypes("P:finance:ParkEx").assertThat()
                .secondaryTypeIsAvailable("P:finance:ParkEx");
        cmisApi.authenticateUser(testUser).usingResource(expenseLondon).updateProperty("finance:Location", "London");

        expenseParis = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "Expense");
        expenseParis.setName("fin2-" + expenseParis.getName());

        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expenseParis.getName());
        properties.put("finance:No", uniqueRef + 1);
        properties.put("finance:amount", 100);
        properties.put("finance:Title", "Airport Taxi Outgoing");
        properties.put("finance:Emp", "David A");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(expenseParis, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        // Location value is set to Paris
        cmisApi.authenticateUser(testUser).usingResource(expenseParis).addSecondaryTypes("P:finance:ParkEx").assertThat()
                .secondaryTypeIsAvailable("P:finance:ParkEx");
        cmisApi.authenticateUser(testUser).usingResource(expenseParis).updateProperty("finance:Location", "Paris");

        expenseNoLocation = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "receipt");
        expenseNoLocation.setName("fin3-" + expenseNoLocation.getName());

        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "D:finance:Expense");
        properties.put(PropertyIds.NAME, expenseNoLocation.getName());
        properties.put("finance:No", uniqueRef + 2);
        properties.put("finance:amount", 0);
        properties.put("finance:Title", "Hotel Stay");
        properties.put("finance:Emp", "Daniel S");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(expenseNoLocation, properties, VersioningState.MAJOR)
                .assertThat().existsInRepo();

        // Location value is set to null
        cmisApi.authenticateUser(testUser).usingResource(expenseNoLocation).addSecondaryTypes("P:finance:ParkEx").assertThat()
                .secondaryTypeIsAvailable("P:finance:ParkEx");

        // Wait for the file to be indexed
        waitForIndexing(expenseNoLocation.getName(), true);
    }

    // Search-1359: Search AFTS Query with Range
    @Test(priority = 1, groups = { TestGroup.ASS_14 })
    public void testRangeQueryTextField() throws Exception
    {
        // Search Range Query
        SearchResponse response = queryAsUser(testUser, "finance_Location:[* TO London]");
        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Content where Location = London is returned, If property is not set, its ignored.
        Assert.assertEquals(response.getPagination().getCount(), 1);

        response = queryAsUser(testUser, "finance_Location:[London TO *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        // Content where Location = London, Paris is returned, If property is not set, its ignored.
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance_Location:[London To Paris]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);

        response = queryAsUser(testUser, "finance_Location:[* To *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);
    }

    @Test(priority = 2, groups = { TestGroup.ASS_14 })
    public void testRangeQueryTextFieldWhiteSpace() throws Exception
    {
        SearchResponse response = queryAsUser(testUser, "finance:Emp:[* TO Daniel]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        // TODO: Uncomment when fixed
        // Assert.assertEquals(response.getPagination().getCount(), 1); //2

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

    @Test(priority = 3, groups = { TestGroup.ASS_14 })
    public void testRangeQueryTextFieldNonFacetable() throws Exception
    {
        // Search Range Query
        SearchResponse response = queryAsUser(testUser, "finance:Title:[* TO H]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);

        response = queryAsUser(testUser, "finance:Title:[Hotel TO *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        // TODO: Uncomment when fixed
        // Assert.assertEquals(response.getPagination().getCount(), 1); //2

        response = queryAsUser(testUser, "finance:Title:[B To H]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        // TODO: Uncomment when fixed
        // Assert.assertEquals(response.getPagination().getCount(), 1); // H, H* return 0
        
        response = queryAsUser(testUser, "finance:Title:[B To I]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);

        response = queryAsUser(testUser, "finance:Title:[* To *]");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 2);
    }

    @Test(priority = 4, groups = { TestGroup.ASS_14 })
    public void testRangeQueryDoubleField() throws Exception
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
