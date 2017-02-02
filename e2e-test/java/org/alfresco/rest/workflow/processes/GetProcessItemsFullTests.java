package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestItemModelsCollection;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 2/2/2017.
 */
public class GetProcessItemsFullTests extends RestTest
{
    private FileModel document1, document2, document3;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, adminTenantUser, tenantUser, tenantUserAssignee;
    private RestProcessModel processModel;
    private RestItemModelsCollection items;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document1 = dataContent.usingUser(userWhoStartsTask).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document2 = dataContent.usingUser(userWhoStartsTask).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document3 = dataContent.usingUser(userWhoStartsTask).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
    }

//    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
//            description = "Get process items using admin from different network")
//    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL, TestGroup.NETWORKS })
//    public void getProcessItemsUsingAdminUserFromDifferentNetwork() throws Exception
//    {
//        adminTenantUser = UserModel.getAdminTenantUser();
//        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser);
//        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
//        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");
//        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee, false, CMISUtil.Priority.Normal);
//
//        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
//        document1 = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
//        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document1);
//        restClient.assertStatusCodeIs(HttpStatus.CREATED);
//
//        UserModel adminTenantUser2 = UserModel.getAdminTenantUser();
//        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser2);
//
//        restClient.authenticateUser(adminTenantUser2).withWorkflowAPI().usingProcess(processModel).getProcessItems();
//        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
//                .assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
//    }
//
//    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
//            description = "Get process items using admin from different network")
//    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL, TestGroup.NETWORKS })
//    public void getProcessItemsReturnsOnlyItemsInsideNetwork() throws Exception
//    {
//        adminTenantUser = UserModel.getAdminTenantUser();
//        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser);
//        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
//        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");
//        processModel = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee, false, CMISUtil.Priority.Normal);
//
//        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
//        document1 = dataContent.usingUser(adminTenantUser).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
//        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document1);
//        restClient.assertStatusCodeIs(HttpStatus.CREATED);
//
//        UserModel adminTenantUser2 = UserModel.getAdminTenantUser();
//        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser2);
//        RestProcessModel processModel2 = restClient.authenticateUser(adminTenantUser2).withWorkflowAPI().addProcess("activitiAdhoc", adminTenantUser2, false, CMISUtil.Priority.Normal);
//
//        SiteModel siteModel2 = dataSite.usingUser(adminTenantUser2).createPublicRandomSite();
//        document2 = dataContent.usingUser(adminTenantUser2).usingSite(siteModel2).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
//        restClient.authenticateUser(adminTenantUser2).withWorkflowAPI().usingProcess(processModel2).addProcessItem(document2);
//        restClient.assertStatusCodeIs(HttpStatus.CREATED);
//
//        items = restClient.withWorkflowAPI().usingProcess(processModel2).getProcessItems();
//        restClient.assertStatusCodeIs(HttpStatus.OK);
//        items.assertThat().entriesListDoesNotContain("name", document1.getName());
//    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process items for process without items")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL})
    public void getProcessItemsForProcessWithoutItems() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        items = restClient.withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        items.assertThat().entriesListIsEmpty();
    }

    @Bug(id = "MNT-17438")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process items for process with valid skipCount parameter")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL})
    public void getProcessItemsWithValidSkipCount() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document3);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        items = restClient.authenticateUser(assignee).withParams("skipCount=2").withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        items.assertThat().entriesListContains("name", document3.getName())
                .assertThat().entriesListDoesNotContain("name", document1.getName())
                .assertThat().entriesListDoesNotContain("name", document2.getName())
                .assertThat().entriesListCountIs(1)
                .assertThat().paginationField("skipCount").is("2");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process items for process with negative skipCount")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL})
    public void getProcessItemsWithNegativeSkipCount() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        restClient.authenticateUser(assignee).withParams("skipCount=-2").withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(RestErrorModel.NEGATIVE_VALUES_SKIPCOUNT)
                .containsErrorKey(RestErrorModel.NEGATIVE_VALUES_SKIPCOUNT)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .statusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process items for process with non numeric skipCount")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL})
    public void getProcessItemsWithNonNumericSkipCount() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        restClient.authenticateUser(assignee).withParams("skipCount=A").withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "A"));
    }

    @Bug(id = "MNT-17438")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process items for process with valid maxItems parameter")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL})
    public void getProcessItemsWithValidMaxItems() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document3);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        items = restClient.authenticateUser(assignee).withParams("maxItems=2").withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        items.assertThat().entriesListDoesNotContain("name", document3.getName())
                .assertThat().entriesListContains("name", document1.getName())
                .assertThat().entriesListContains("name", document2.getName())
                .assertThat().entriesListCountIs(2)
                .assertThat().paginationField("maxItems").is("2");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process items for process with negative maxItems")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL})
    public void getProcessItemsWithNegativeMaxItems() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        restClient.authenticateUser(assignee).withParams("maxItems=-2").withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
                .containsErrorKey(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .statusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Get process items for process with non numeric maxItems")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL})
    public void getProcessItemsWithNonNumericMaxItems() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        restClient.authenticateUser(assignee).withParams("maxItems=A").withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "A"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify get all process items with properties parameter.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void getProcessItemsWithPropertiesParameter() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        items = restClient.authenticateUser(assignee).withParams("properties=createdBy,name,mimeType,size").withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        items.assertThat().entriesListIsNotEmpty();
        items.getProcessItemByName(document1.getName())
                .assertThat().field("createdBy").is(userWhoStartsTask.getUsername())
                .assertThat().field("name").is(document1.getName())
                .assertThat().field("mimeType").is(document1.getFileType().mimeType)
                .assertThat().field("size").isNotNull()
                .assertThat().field("modifiedAt").isNull()
                .assertThat().field("modifiedBy").isNull()
                .assertThat().field("id").isNull()
                .assertThat().field("createdAt ").isNull()
                .assertThat().fieldsCount().is(4);
    }


    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify get all process items after process is deleted.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void getProcessItemsAfterDeletingProcess() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, CMISUtil.Priority.Normal);
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.withWorkflowAPI().usingProcess(processModel).deleteProcess();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        items = restClient.authenticateUser(assignee).withWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        items.assertThat().entriesListIsNotEmpty();
        items.getProcessItemByName(document1.getName())
                .assertThat().field("createdBy").is(userWhoStartsTask.getUsername())
                .assertThat().field("name").is(document1.getName())
                .assertThat().field("mimeType").is(document1.getFileType().mimeType)
                .assertThat().field("size").isNotNull()
                .assertThat().field("modifiedAt").isNotNull()
                .assertThat().field("modifiedBy").is(userWhoStartsTask.getUsername())
                .assertThat().field("id").is(document1.getNodeRefWithoutVersion())
                .assertThat().field("createdAt ").isNotNull();
    }
}
