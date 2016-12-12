package org.alfresco.rest.workflow.processes;

import java.util.List;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessModelsCollection;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.ProcessModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class GetProcessesCoreTests extends RestTest
{
    private FileModel document;
    private SiteModel siteModel;
    private UserModel adminUser, userWhoStartsTask, assignee, adminTenantUser;
    private TaskModel task1, task2;
    private ProcessModel process3;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        task1 = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);  
        task2 = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(adminUser);
        process3 = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createSingleReviewerTaskAndAssignTo(assignee);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin gets all processes from same network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE, TestGroup.NETWORKS })
    public void getProcessFromSameNetworkUsingAdmin() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        UserModel tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");
        RestProcessModel process = restClient.authenticateUser(tenantUser).withWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee, false, Priority.Normal);

        RestProcessModelsCollection tenantProcesses = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        tenantProcesses.assertThat().entriesListContains("id", process.getId());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify user gets all processes started by him ordered descending by id")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessesOrderedByIdDESC() throws Exception
    {
        RestProcessModelsCollection processes = restClient.authenticateUser(userWhoStartsTask).withParams("orderBy=id DESC")
                .withWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processes.assertThat().entriesListIsNotEmpty();
        List<RestProcessModel> processesList = processes.getEntries();
        processesList.get(0).onModel().assertThat().field("id").is(process3.getId());
        processesList.get(1).onModel().assertThat().field("id").is(task2.getNodeRef());
        processesList.get(2).onModel().assertThat().field("id").is(task1.getNodeRef());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Verify user gets processes that matches a where clause")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.CORE })
    public void getProcessesWithWhereClauseAsParameter() throws JsonToModelConversionException, Exception
    {       
        RestProcessModelsCollection processes = restClient.authenticateUser(userWhoStartsTask).where("processDefinitionKey='activitiReview'")
                .withWorkflowAPI().getProcesses();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processes.assertThat().entriesListIsNotEmpty().and().entriesListContains("processDefinitionId", "activitiReview:1:8")
                .and().entriesListDoesNotContain("processDefinitionId", "activitiAdhoc:1:4");
    }
}
