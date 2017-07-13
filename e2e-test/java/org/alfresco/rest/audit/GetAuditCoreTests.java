package org.alfresco.rest.audit;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.alfresco.rest.model.RestAuditAppModel;
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

        restClient.authenticateUser(adminUser).withCoreAPI().usingAudit()
        .getAuditApp(restAuditAppModel).assertThat().field("isEnabled").is(true);
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingAudit()
        .getAuditApp(restAuditAppModel).assertThat().field("name").is("alfresco-access");

        restClient.authenticateUser(adminUser).withCoreAPI().usingAudit()
        .getAuditApp(restAuditAppModel).assertThat().field("id").is("alfresco-access");

        RestAuditAppModel secondRestAuditAppModel = restAuditCollection.getEntries().get(1).onModel();
        secondRestAuditAppModel.assertThat().field("isEnabled").is(true);
        secondRestAuditAppModel.assertThat().field("name").is("Alfresco Tagging Service");
        secondRestAuditAppModel.assertThat().field("id").is("tagging");
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingAudit()
        .getAuditApp(secondRestAuditAppModel).assertThat().field("isEnabled").is(true);
        
        restClient.authenticateUser(adminUser).withCoreAPI().usingAudit()
        .getAuditApp(secondRestAuditAppModel).assertThat().field("name").is("Alfresco Tagging Service");

        restClient.authenticateUser(adminUser).withCoreAPI().usingAudit()
        .getAuditApp(secondRestAuditAppModel).assertThat().field("id").is("tagging");  
               
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
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using invalid skipCount and/or invalid maxItems and status code is 400")
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
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using valid skipCount and maxItems and status code is 200")
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
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using orderBy and status code is 200")
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
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using the where parameter to  and status code is 200")
    public void getAuditEntriesWithAdminUserUsingWhere() throws Exception
    {
        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("maxItems=10").withCoreAPI().usingAudit().listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);

        String id1 = restAuditEntryCollection.getEntries().get(restAuditEntryCollection.getPagination().getCount()-1).onModel().getId();
        String id2 = restAuditEntryCollection.getEntries().get(restAuditEntryCollection.getPagination().getCount()/2).onModel().getId();

        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("where=(id BETWEEN ("+id2+","+id1+"))")
                .withCoreAPI().usingAudit().listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        restAuditEntryCollection.assertThat().entriesListCountIs(Integer.parseInt(id1)-Integer.parseInt(id2));
        assertEquals(id2, restAuditEntryCollection.getEntries().get(0).onModel().getId());
        assertEquals(Integer.toString(Integer.parseInt(id1)-1), restAuditEntryCollection.getEntries().get(Integer.parseInt(id1)-Integer.parseInt(id2)-1).onModel().getId());
        
    }
}
