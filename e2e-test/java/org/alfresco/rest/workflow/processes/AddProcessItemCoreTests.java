package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddProcessItemCoreTests extends RestTest
{
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsProcess, assignee, adminUser, adminTenantUser, tenantUserAssignee, adminTenantUser2, tenantUser;
    private RestProcessModel processModel;
    private RestItemModel processItem;


    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsProcess).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Add process item using by the user who started the process.")
    public void addProcessItemByUserThatStartedTheProcess() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        // document = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItem.assertThat().field("createdAt").isNotEmpty().and().field("size").is("19").and().field("createdBy").is(adminUser.getUsername()).and()
                .field("modifiedAt").isNotEmpty().and().field("name").is(document.getName()).and().field("modifiedBy").is(userWhoStartsProcess.getUsername())
                .and().field("id").isNotEmpty().and().field("mimeType").is(document.getFileType().mimeType);

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Add process item by a random user.")
    public void addProcessItemByAnyUser() throws Exception
    {
        processModel = restClient.authenticateUser(assignee).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItem.assertThat().field("createdAt").isNotEmpty().and().field("size").is("19").and().field("createdBy").is(adminUser.getUsername()).and()
                .field("modifiedAt").isNotEmpty().and().field("name").is(document.getName()).and().field("modifiedBy").is(assignee.getUsername()).and()
                .field("id").isNotEmpty().and().field("mimeType").is(document.getFileType().mimeType);

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, description = "Add process item using by the admin in same network.")
    public void addProcessItemByAdminSameNetwork() throws Exception
    {
        restClient.authenticateUser(adminUser);

        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser);

        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(tenantUserAssignee);

        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItem.assertThat().field("createdAt").isNotEmpty().and().field("size").is("19").and().field("createdBy").is(adminTenantUser.getUsername()).and()
                .field("modifiedAt").isNotEmpty().and().field("name").is(document.getName()).and().field("modifiedBy").is(adminTenantUser.getUsername()).and()
                .field("id").isNotEmpty().and().field("mimeType").is(document.getFileType().mimeType);

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES, TestGroup.NETWORKS }, executionType = ExecutionType.SANITY, description = "Add process item using by admin in other network.")
    public void addProcessItemByAdminInOtherNetwork() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");

        adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser2);

        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.authenticateUser(adminTenantUser2);
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process item is falling in case of invalid process id is provided")
    public void failedAddingProcessItemIfInvalidProcessIdIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processModel.setId("invalidProcessId");
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidProcessId"));
    }

    @Bug(id = "ACE-5683")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process item is falling in case of invalid body item is provided")
    public void failedAddingProcessItemIfInvalidItemBodyIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{\"id\":\"invalidId\"}", "processes/{processId}/items", processModel.getId());
        restClient.processModel(RestItemModel.class, request);

        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidId"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process item is falling in case of empty body item value is provided")
    public void failedAddingProcessItemIfEmptyItemBodyIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{\"id\":\"\"}", "processes/{processId}/items", processModel.getId());
        restClient.processModel(RestItemModel.class, request);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("itemId is required to add an attached item");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, description = "Adding process item is falling in case of incomplete body (empty) is provided")
    public void failedAddingProcessItemIfIncompleteBodyIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();

        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{}", "processes/{processId}/items", processModel.getId());
        restClient.processModel(RestItemModel.class, request);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("itemId is required to add an attached item");
    }

}
