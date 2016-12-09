package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.utility.model.*;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetProcessTasksCoreTests extends RestTest
{
    private FileModel document, document1, document2, document3;
    private SiteModel siteModel;
    private UserModel adminUser, userModel, userModel1, userModel2, assignee;
    private ProcessModel process;
    private GroupModel group;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        userModel1 = dataUser.createRandomTestUser();
        userModel2 = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document1 = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document2 = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        process = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(document)
                .createSingleReviewerTaskAndAssignTo(assignee);
        group = dataGroup.createRandomGroup();
        dataGroup.addListOfUsersToGroup(group, userModel1);
    }

    @Bug(id = "TBD")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "getProcessTaks with user that is candidate with REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessTasksWithUserThatIsCandidate() throws Exception
    {
        ProcessModel processModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(document1)
                .createGroupReviewTaskAndAssignTo(group);

        restClient.authenticateUser(assignee).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsNotEmpty()
                .and().entriesListContains("assignee", assignee.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Bug(id = "TBD")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify getProcessTasks using admin from same network with REST API and status code is FORBIDDEN (403)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void getProcessTasksWithAdminFromSameNetwork() throws Exception
    {
        UserModel adminTenant, tenantAssignee;
        adminTenant = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant);
        SiteModel siteModel = dataSite.usingUser(adminTenant).createPublicRandomSite();
        tenantAssignee = dataUser.usingUser(adminTenant).createUserWithTenant("uTenant");
        document3 = dataContent.usingUser(adminTenant).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);

        ProcessModel processModel = dataWorkflow.usingUser(adminTenant).usingSite(siteModel).usingResource(document3)
                .createSingleReviewerTaskAndAssignTo(tenantAssignee);

        restClient.withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Bug(id = "TBD")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify getProcessTasks using admin from different network with REST API and status code is FORBIDDEN (403)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void getProcessTasksWithAdminFromDifferentNetwork() throws Exception
    {
        UserModel adminTenant, secondAdminTenant, tenantAssignee;
        adminTenant = UserModel.getAdminTenantUser();
        secondAdminTenant = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenant);

        SiteModel siteModel = dataSite.usingUser(adminTenant).createPublicRandomSite();
        document3 = dataContent.usingUser(adminTenant).usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        tenantAssignee = dataUser.usingUser(adminTenant).createUserWithTenant("uTenant");
        ProcessModel processModel = dataWorkflow.usingUser(adminTenant).usingSite(siteModel).usingResource(document3)
                .createSingleReviewerTaskAndAssignTo(tenantAssignee);

        restClient.authenticateUser(adminUser).usingTenant().createTenant(secondAdminTenant);
        restClient.authenticateUser(secondAdminTenant).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "getProcessTasks for invalid processId with REST API and status code is NOT_FOUND (404)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessTasksUsingInvalidProcessId() throws Exception
    {
        ProcessModel processModel = process;
        processModel.setId("invalidProcessId");
        restClient.authenticateUser(userModel).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "getProcessTasks for empty processId with REST API and status code is NOT_FOUND (404)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessTasksUsingEmptyProcessId() throws Exception
    {
        ProcessModel processModel = process;
        processModel.setId("");
        restClient.authenticateUser(userModel).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "User completes task then getProcessTasks with REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void completeTaskThenGetProcessTasks() throws Exception
    {
        ProcessModel processModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(document2)
                .createMoreReviewersWorkflowAndAssignTo(userModel2);
        RestTaskModel restTaskModel = restClient.authenticateUser(userModel2).withWorkflowAPI()
                .usingProcess(processModel).getProcessTasks().assertThat().entriesListIsNotEmpty().getOneRandomEntry().onModel();
        restTaskModel = restClient.withParams("select=state").withWorkflowAPI().usingTask(restTaskModel).updateTask("completed");
        restTaskModel.assertThat().field("id").is(restTaskModel.getId()).and().field("state").is("completed");
        restClient.withWorkflowAPI().getTasks().assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
