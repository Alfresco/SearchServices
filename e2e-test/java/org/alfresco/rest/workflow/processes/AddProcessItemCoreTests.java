package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.model.RestItemModelsCollection;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessModelsCollection;
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
    private FileModel document, document2;
    private SiteModel siteModel;
    private UserModel userWhoStartsProcess, assignee, adminUser, anotherUser, adminTenantUser, tenantUserAssignee, adminTenantUser2, tenantUser;
    private RestProcessModel processModel;
    private RestItemModel processItem;
    private RestItemModelsCollection processItems;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsProcess).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add process item using by the user who started the process.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void addProcessItemByUserThatStartedTheProcess() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItem.assertThat().field("createdAt").isNotEmpty().and().field("size").is("19").and().field("createdBy").is(adminUser.getUsername()).and()
                .field("modifiedAt").isNotEmpty().and().field("name").is(document.getName()).and().field("modifiedBy").is(userWhoStartsProcess.getUsername())
                .and().field("id").isNotEmpty().and().field("mimeType").is(document.getFileType().mimeType);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add multiple process items using by the user who started the process.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void addMultipleProcessItemsByUserThatStartedTheProcess() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processItems = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItems(document, document2);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItems.getEntries().get(0).onModel().assertThat()
                    .field("createdAt").isNotEmpty().and()
                    .field("size").is("19").and()    
                    .field("createdBy").is(adminUser.getUsername()).and()
                    .field("modifiedAt").isNotEmpty().and()
                    .field("name").is(document.getName()).and()
                    .field("modifiedBy").is(userWhoStartsProcess.getUsername()).and()
                    .field("id").isNotEmpty().and()
                    .field("mimeType").is(document.getFileType().mimeType);
        processItems.getEntries().get(1).onModel().assertThat()
                    .field("createdAt").isNotEmpty().and()
                    .field("size").is("19").and()    
                    .field("createdBy").is(adminUser.getUsername()).and()
                    .field("modifiedAt").isNotEmpty().and()
                    .field("name").is(document2.getName()).and()
                    .field("modifiedBy").is(userWhoStartsProcess.getUsername()).and()
                    .field("id").isNotEmpty().and()
                    .field("mimeType").is(document2.getFileType().mimeType);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add process item by a random user.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void addProcessItemByAnyUser() throws Exception
    {
        anotherUser = dataUser.createRandomTestUser();

        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        processModel = restClient.authenticateUser(adminUser).withParams("maxItems=1").withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processItem = restClient.authenticateUser(anotherUser).withWorkflowAPI().usingProcess(processModel).addProcessItem(document2);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError()
                .containsSummary(String.format(RestErrorModel.ACCESS_INFORMATION_NOT_ALLOWED, processModel.getId()));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add process item using by the admin in same network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void addProcessItemByAdminSameNetwork() throws Exception
    {
        restClient.authenticateUser(adminUser);
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser);

        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(tenantUserAssignee);

        processModel = restClient.withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItem.assertThat().field("createdAt").isNotEmpty().and().field("size").is("19").and().field("createdBy").is(adminUser.getUsername()).and()
                .field("modifiedAt").isNotEmpty().and().field("name").is(document.getName()).and().field("modifiedBy").is(adminUser.getUsername()).and()
                .field("id").isNotEmpty().and().field("mimeType").is(document.getFileType().mimeType);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add multiple process items using by the admin in same network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void addMultipleProcessItemsByAdminSameNetwork() throws Exception
    {
        restClient.authenticateUser(adminUser);
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser);

        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");

        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(tenantUserAssignee);

        processModel = restClient.withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processItems = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItems(document, document2);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItems.getEntries().get(0).onModel().assertThat()
                    .field("createdAt").isNotEmpty().and()
                    .field("size").is("19").and()    
                    .field("createdBy").is(adminUser.getUsername()).and()
                    .field("modifiedAt").isNotEmpty().and()
                    .field("name").is(document.getName()).and()
                    .field("modifiedBy").is(userWhoStartsProcess.getUsername()).and()
                    .field("id").isNotEmpty().and()
                    .field("mimeType").is(document.getFileType().mimeType);
        processItems.getEntries().get(1).onModel().assertThat()
                    .field("createdAt").isNotEmpty().and()
                    .field("size").is("19").and()    
                    .field("createdBy").is(adminUser.getUsername()).and()
                    .field("modifiedAt").isNotEmpty().and()
                    .field("name").is(document2.getName()).and()
                    .field("modifiedBy").is(userWhoStartsProcess.getUsername()).and()
                    .field("id").isNotEmpty().and()
                    .field("mimeType").is(document2.getFileType().mimeType);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Add process item using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void addProcessItemByAdminInOtherNetwork() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");

        adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser2);

        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee, false, Priority.Normal);
        restClient.authenticateUser(adminTenantUser2);
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Add multiple process items using by admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void addMultipleProcessItemsByAdminInOtherNetwork() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");

        adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser2);

        processModel = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee, false, Priority.Normal);
        restClient.authenticateUser(adminTenantUser2);
        
        processItems = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItems(document, document2);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding process item is falling in case of invalid process id is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void failedAddingProcessItemIfInvalidProcessIdIsProvided() throws Exception
    {
        RestProcessModelsCollection processes = restClient.authenticateUser(adminUser).withParams("maxItems=1").withWorkflowAPI().getProcesses();
        processModel = processes.assertThat().entriesListIsNotEmpty().when().getOneRandomEntry().onModel();
        FileModel testDocument = dataContent.usingAdmin().usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        
        processModel.setId("invalidProcessId");
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(testDocument);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidProcessId"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding multiple process items is falling in case of invalid process id is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void failedAddingMultipleProcessItemsIfInvalidProcessIdIsProvided() throws Exception
    {
        RestProcessModelsCollection processes = restClient.authenticateUser(adminUser).withParams("maxItems=1").withWorkflowAPI().getProcesses();
        processModel = processes.assertThat().entriesListIsNotEmpty().when().getOneRandomEntry().onModel();
        FileModel testDocument = dataContent.usingAdmin().usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        
        processModel.setId("invalidProcessId");
        processItems = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItems(testDocument, document);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidProcessId"));
    }

    @Bug(id = "ACE-5683")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding process item is falling in case of invalid body item is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void failedAddingProcessItemIfInvalidItemBodyIsProvided() throws Exception
    {
        document.setNodeRef("invalidNodeRef");
        processItem = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItem(document);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidId"));
    }   
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Adding multiple process items is falling in case of empty body item value is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void failedAddingMultipleProcessItemIfEmptyItemBodyIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        document.setNodeRef("");
        processItems = restClient.withWorkflowAPI().usingProcess(processModel).addProcessItems(document, document2);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
        .containsSummary(String.format(RestErrorModel.REQUIRED_TO_ADD, "itemId"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding process item is falling in case of incomplete body (empty) is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void failedAddingProcessItemIfIncompleteBodyIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{}", "processes/{processId}/items", processModel.getId());
        restClient.processModel(RestItemModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.REQUIRED_TO_ADD, "itemId"));
    }
}
