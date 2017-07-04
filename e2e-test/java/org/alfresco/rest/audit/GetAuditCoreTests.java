package org.alfresco.rest.audit;

import static org.testng.Assert.assertEquals;

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
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications and status code is 200")
    public void getAuditApplicationsWithAdminUser() throws Exception
    {
        restAuditCollection = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restAuditCollection.assertThat().entriesListIsNotEmpty();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets audit application info")
    public void getAuditApplicationInfoWithAdminUser() throws Exception
    {
        restAuditCollection = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit().getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restAuditCollection.assertThat().entriesListIsNotEmpty();
        
        RestAuditAppModel firstRestAuditAppModel = restAuditCollection.getEntries().get(0).onModel();
        firstRestAuditAppModel.assertThat().field("isEnabled").is(true);
        firstRestAuditAppModel.assertThat().field("name").is("Alfresco Sync Service");
        firstRestAuditAppModel.assertThat().field("id").is("sync");

        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit()
        .getAuditApp(firstRestAuditAppModel).assertThat().field("isEnabled").is(true);
        
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit()
        .getAuditApp(firstRestAuditAppModel).assertThat().field("name").is("Alfresco Sync Service");

        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit()
        .getAuditApp(firstRestAuditAppModel).assertThat().field("id").is("sync");

        RestAuditAppModel secondRestAuditAppModel = restAuditCollection.getEntries().get(1).onModel();
        secondRestAuditAppModel.assertThat().field("isEnabled").is(true);
        secondRestAuditAppModel.assertThat().field("name").is("Alfresco Tagging Service");
        secondRestAuditAppModel.assertThat().field("id").is("tagging");
        
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit()
        .getAuditApp(secondRestAuditAppModel).assertThat().field("isEnabled").is(true);
        
        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit()
        .getAuditApp(secondRestAuditAppModel).assertThat().field("name").is("Alfresco Tagging Service");

        restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit()
        .getAuditApp(secondRestAuditAppModel).assertThat().field("id").is("tagging");  
               
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify a normal user gets a list of audit applications and status code is 403")
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
        restAuditCollection = restClient.authenticateUser(dataUser.getAdminUser()).withParams("skipCount=1").withCoreAPI()
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
        restClient.authenticateUser(dataUser.getAdminUser()).withParams("skipCount=-1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using maxItems and status code is 200")
    public void getAuditApplicationsWithAdminUserUsingValidMaxItems() throws Exception
    {
        restAuditCollection = restClient.authenticateUser(dataUser.getAdminUser()).withParams("maxItems=1").withCoreAPI()
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
        restClient.authenticateUser(dataUser.getAdminUser()).withParams("maxItems=-1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(dataUser.getAdminUser()).withParams("maxItems=0").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user gets a list of audit applications using skipCount and maxItems and status code is 200")
    public void getAuditApplicationsWithAdminUserUsingValidSkipCountAndMaxItems() throws Exception
    {
        restAuditCollection = restClient.authenticateUser(dataUser.getAdminUser()).withParams("skipCount=1&maxItems=1")
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
        restClient.authenticateUser(dataUser.getAdminUser()).withParams("skipCount=-1&maxItems=1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(dataUser.getAdminUser()).withParams("skipCount=-1&maxItems=-1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(dataUser.getAdminUser()).withParams("skipCount=-1&maxItems=0").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        restClient.authenticateUser(dataUser.getAdminUser()).withParams("skipCount=1&maxItems=-1").withCoreAPI().usingAudit()
                .getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

}
