package org.alfresco.rest.audit;

import static org.alfresco.utility.report.log.Step.STEP;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

public class GetAuditCoreTests extends AuditTest
{

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets audit application info")
    public void getAuditApplicationInfoWithAdminUser() throws Exception
    {
        restAuditAppModel.assertThat().field("isEnabled").is(true);
        restAuditAppModel.assertThat().field("name").is("alfresco-access");
        restAuditAppModel.assertThat().field("id").is("alfresco-access");

        syncRestAuditAppModel = getSyncRestAuditAppModel(adminUser);
        syncRestAuditAppModel.assertThat().field("isEnabled").is(true);
        syncRestAuditAppModel.assertThat().field("name").is("Alfresco Sync Service");
        syncRestAuditAppModel.assertThat().field("id").is("sync");

        taggingRestAuditAppModel = getTaggingRestAuditAppModel(adminUser);
        taggingRestAuditAppModel.assertThat().field("isEnabled").is(true);
        taggingRestAuditAppModel.assertThat().field("name").is("Alfresco Tagging Service");
        taggingRestAuditAppModel.assertThat().field("id").is("tagging");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if a normal user gets a list of audit applications and status code is 403")
    public void getAuditApplicationsWithNormalUser() throws Exception
    {
        restAuditCollection = restClient.authenticateUser(userModel).withCoreAPI().usingAudit().getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
        restAuditCollection.assertThat().entriesListIsEmpty();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using skipCount and status code is 200")
    public void getAuditApplicationsWithAdminUserUsingValidSkipCount() throws Exception
    {
        restAuditCollection = restClient.authenticateUser(adminUser).withParams("skipCount=1").withCoreAPI()
                .usingAudit().getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restAuditCollection.getPagination().assertThat().field("totalItems").isNotNull().and().field("totalItems")
                .isGreaterThan(0).and().field("skipCount").is("1");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using invalid skipCount and status code is 400")
    public void getAuditApplicationsWithAdminUserUsingInvalidSkipCount() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("skipCount=-1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using maxItems and status code is 200")
    public void getAuditApplicationsWithAdminUserUsingValidMaxItems() throws Exception
    {
        restAuditCollection = restClient.authenticateUser(adminUser).withParams("maxItems=1").withCoreAPI()
                .usingAudit().getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restAuditCollection.getPagination().assertThat().field("totalItems").isNotNull().and().field("totalItems")
                .isGreaterThan(0).and().field("maxItems").is("1");
        assertEquals(restAuditCollection.getEntries().size(), 1);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using invalid maxItems and status code is 400")
    public void getAuditApplicationsWithAdminUserUsingInvalidMaxItems() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("maxItems=-1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(adminUser).withParams("maxItems=0").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using skipCount and maxItems and status code is 200")
    public void getAuditApplicationsWithAdminUserUsingValidSkipCountAndMaxItems() throws Exception
    {
        restAuditCollection = restClient.authenticateUser(adminUser).withParams("skipCount=1&maxItems=1")
                .withCoreAPI().usingAudit().getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restAuditCollection.getPagination().assertThat().field("totalItems").isNotNull().and().field("totalItems")
                .isGreaterThan(0).and().field("maxItems").is("1").and().field("skipCount").is("1");
        assertEquals(restAuditCollection.getEntries().size(), 1);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using invalid skipCount and/or invalid maxItems and status code is 400")
    public void getAuditApplicationsWithAdminUserUsingInvalidSkipCountAndMaxItems() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("skipCount=-1&maxItems=1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(adminUser).withParams("skipCount=-1&maxItems=-1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(adminUser).withParams("skipCount=-1&maxItems=0").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(adminUser).withParams("skipCount=1&maxItems=-1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if a normal user gets a list of audit entries for audit application auditApplicationId and status code is 403")
    public void getAuditEntriesWithNormalUser() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI().usingAudit()
                .listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit entries using invalid skipCount and/or invalid maxItems and status code is 400")
    public void getAuditEntriesWithAdminUserUsingInvalidSkipCountAndMaxItems() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("skipCount=-1&maxItems=1").withCoreAPI().usingAudit()
                .listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(adminUser).withParams("skipCount=-1&maxItems=-1").withCoreAPI().usingAudit()
                .listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(adminUser).withParams("skipCount=-1&maxItems=0").withCoreAPI().usingAudit()
                .listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(adminUser).withParams("skipCount=1&maxItems=-1").withCoreAPI().usingAudit()
                .listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit entries using valid skipCount and maxItems and status code is 200")
    public void getAuditEntriesWithAdminUserUsingValidSkipCountAndMaxItems() throws Exception
    {
        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("skipCount=1&maxItems=1")
                .withCoreAPI().usingAudit().listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restAuditEntryCollection.getPagination().assertThat().field("totalItems").isNotNull().and().field("totalItems")
                .isGreaterThan(0).and().field("maxItems").is("1").and().field("skipCount").is("1");
        assertEquals(restAuditEntryCollection.getEntries().size(), 1);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit entries using orderBy and status code is 200")
    public void getAuditEntriesWithAdminUserUsingOrderBy() throws Exception
    {
        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("orderBy=createdAt ASC&maxItems=10")
                .withCoreAPI().usingAudit().listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);

        String ascId = restAuditEntryCollection.getEntries().get(1).onModel().getId();
        
        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("orderBy=createdAt DESC&maxItems=10")
                .withCoreAPI().usingAudit().listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        assertTrue(Integer.parseInt(restAuditEntryCollection.getEntries().get(1).onModel().getId()) > Integer.parseInt(ascId));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit entries using the where parameter to  and status code is 200")
    public void getAuditEntriesWithAdminUserUsingWhere() throws Exception
    {
        int expectedNumberOfItems;
        String id1,id2;
        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("maxItems=2").withCoreAPI().usingAudit().listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);

        if (restAuditEntryCollection.getPagination().getCount() == 2)
        {
            id1 = restAuditEntryCollection.getEntries().get(0).onModel().getId();
            id2 = restAuditEntryCollection.getEntries().get(1).onModel().getId();
            expectedNumberOfItems = 2;
        }
        else
        {
            id1 = id2 = restAuditEntryCollection.getEntries().get(0).onModel().getId();
            expectedNumberOfItems = 1;
        }

        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("where=(id BETWEEN ("+id1+","+id2+"))")
                .withCoreAPI().usingAudit().listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restAuditEntryCollection.assertThat().entriesListCountIs(expectedNumberOfItems);
        assertEquals(id1, restAuditEntryCollection.getEntries().get(0).onModel().getId());
        if (!id1.equals(id2))
        {
            assertEquals(id2, restAuditEntryCollection.getEntries().get(1).onModel().getId());
        }

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if retrieving an audit entry for an audit application is only for admin user, with and without the include=values paramater and status code is 200")
    public void getAuditEntry() throws Exception
    {
        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("maxItems=10").withCoreAPI().usingAudit()
                .listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        String entryId = restAuditEntryCollection.getEntries().get(restAuditEntryCollection.getPagination().getCount() - 1)
                .onModel().getId();
        String entryApplicationId = restAuditEntryCollection.getEntries()
                .get(restAuditEntryCollection.getPagination().getCount() - 1).onModel().getAuditApplicationId();

        restAuditEntryModel = restClient.authenticateUser(userModel).withCoreAPI().usingAudit()
                .getAuditEntryForAnAuditApplication(restAuditAppModel.getId(), entryId);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);

        restAuditEntryModel = restClient.authenticateUser(adminUser).withCoreAPI().usingAudit()
                .getAuditEntryForAnAuditApplication(restAuditAppModel.getId(), entryId);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        assertEquals(entryId, restAuditEntryModel.getId());
        assertEquals(entryApplicationId, restAuditEntryModel.getAuditApplicationId());
        assertNull(restAuditEntryModel.getValues());

        restAuditEntryModel = restClient.authenticateUser(adminUser).withParams("include=values").withCoreAPI().usingAudit()
                .getAuditEntryForAnAuditApplication(restAuditAppModel.getId(), entryId);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        assertEquals(entryId, restAuditEntryModel.getId());
        assertEquals(entryApplicationId, restAuditEntryModel.getAuditApplicationId());
        assertNotNull(restAuditEntryModel.getValues());
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit entries for node id nodeId using 'where' and 'maxItems' param and status code is 200")
    public void getAuditEntriesForNodeUsingMaxItemsAndWhereParam() throws Exception
    {
        String createdAt1, createdAt2;
        int expectedNumberOfItems;

        // Get the node id and construct a fileModel to be used when adding comments
        String nodeId = node.getId();
        FileModel fileModel = new FileModel("testFile", FileType.TEXT_PLAIN);
        fileModel.setNodeRef(nodeId);

        // Add comments for a node (to create audit entries)
        restClient.authenticateUser(adminUser).withCoreAPI().usingNode(fileModel).addComment("This is the first comment");
        restClient.authenticateUser(adminUser).withCoreAPI().usingNode(fileModel).addComment("This is the second comment");

        // Get maxium two audit entries for the node using 'maxItems' param on /nodes/{nodeId}/audit-entries
        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("maxItems=2").withCoreAPI().usingAudit().listAuditEntriesForNode(nodeId);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        if (restAuditEntryCollection.getPagination().getCount() == 2)
        {
            createdAt1 = restAuditEntryCollection.getEntries().get(0).onModel().getCreatedAt();
            createdAt2 = restAuditEntryCollection.getEntries().get(1).onModel().getCreatedAt();
            expectedNumberOfItems = 2;
        }
        else
        {
            createdAt1 = createdAt2 = restAuditEntryCollection.getEntries().get(0).onModel().getCreatedAt();
            expectedNumberOfItems = 1;
        }

        // Get audit entries between ids for the node using 'where' clause on /nodes/{nodeId}/audit-entries
        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("where=(createdAt BETWEEN ('" + createdAt1 + "','" + createdAt2 + "'))")
                .withCoreAPI().usingAudit().listAuditEntriesForNode(nodeId);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restAuditEntryCollection.assertThat().entriesListCountIs(expectedNumberOfItems);
        assertEquals(createdAt1, restAuditEntryCollection.getEntries().get(0).onModel().getCreatedAt());
        if (!createdAt1.equals(createdAt2))
        {
            assertEquals(createdAt2, restAuditEntryCollection.getEntries().get(1).onModel().getCreatedAt());
        }

    }
}
