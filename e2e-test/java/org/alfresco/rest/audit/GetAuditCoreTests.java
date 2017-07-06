package org.alfresco.rest.audit;

import static org.testng.Assert.assertEquals;


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
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify that the admin user can get audit application info")
    public void getAuditApplicationInfoWithAdminUser() throws Exception
    {
    	syncRestAuditAppModel = getSyncRestAuditAppModel(dataUser.getAdminUser());
        syncRestAuditAppModel.assertThat().field("isEnabled").is(true);
        syncRestAuditAppModel.assertThat().field("name").is("Alfresco Sync Service");
        syncRestAuditAppModel.assertThat().field("id").is("sync");

    	taggingRestAuditAppModel = getTaggingRestAuditAppModel(dataUser.getAdminUser());
    	taggingRestAuditAppModel.assertThat().field("isEnabled").is(true);
    	taggingRestAuditAppModel.assertThat().field("name").is("Alfresco Tagging Service");
    	taggingRestAuditAppModel.assertThat().field("id").is("tagging");
               
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
