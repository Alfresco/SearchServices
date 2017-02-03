package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessVariableModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.ProcessModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DeleteProcessVariableFullTests extends RestTest
{
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, anotherUser, adminUser;
    private RestProcessModel restProcessModel;
    private ProcessModel processModel;
    private RestProcessVariableModel variableModel, updatedVariable;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        anotherUser = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        processModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createSingleReviewerTaskAndAssignTo(assignee);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Delete empty process variable")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void deleteEmptyProcessVariable() throws Exception
    {
        variableModel = new RestProcessVariableModel("", "", "bpm:workflowPackage");
        restProcessModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().getProcesses()
                                     .getProcessModelByProcessDefId(processModel.getId());
        
        restClient.withWorkflowAPI().usingProcess(restProcessModel)
                  .deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED)
                  .assertLastError()
                  .containsSummary(RestErrorModel.DELETE_EMPTY_ARGUMENT)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.DELETE_EMPTY_ARGUMENT)
                  .stackTraceIs(RestErrorModel.STACKTRACE);     
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
              description = "Delete process variable using any user.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void deleteProcessVariableWithAnyUser() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(anotherUser).withWorkflowAPI()
                                 .addProcess("activitiAdhoc", anotherUser, false, Priority.Normal);

        variableModel = restClient.withWorkflowAPI().usingProcess(processModel).updateProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        variableModel.assertThat().field("name").is(variableModel.getName()).and().field("type")
                     .is(variableModel.getType()).and().field("value")
                     .is(variableModel.getValue());

        restClient.authenticateUser(anotherUser).withWorkflowAPI().usingProcess(processModel)
                  .deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                  .assertThat()
                  .entriesListDoesNotContain("name", variableModel.getName());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
              description = "Add a new process varaiables, update the variable and then delete.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void createUpdateDeleteProcessVariable() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        restProcessModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().getProcesses()
                                     .getProcessModelByProcessDefId(processModel.getId());
        restClient.withWorkflowAPI().usingProcess(restProcessModel)
                  .addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        variableModel.setValue("newValue");
        updatedVariable = restClient.withWorkflowAPI().usingProcess(processModel)
                                    .updateProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedVariable.assertThat().field("value").is("newValue");

        restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().usingProcess(processModel)
                  .deleteProcessVariable(updatedVariable);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                  .assertThat()
                  .entriesListDoesNotContain("name", updatedVariable.getName());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
              description = "Delete process then delete process variables, status OK should be returned")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void deleteProcessVariablesForADeletedProcess() throws Exception
    {
        UserModel userWhoStartsTask = dataUser.createRandomTestUser();
        UserModel assignee = dataUser.createRandomTestUser();

        RestProcessModel processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI()
                                                  .addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        restProcessModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI()
                                     .getProcesses().getProcessModelByProcessDefId(processModel.getId());
        restClient.withWorkflowAPI().usingProcess(restProcessModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI().usingProcess(processModel)
                  .deleteProcess();
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withWorkflowAPI().usingProcess(processModel).deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, processModel.getId()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Delete process variable using by the user who started the process.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void deleteProcessVariableByUserThatStartedTheProcess() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI()
                                 .addProcess("activitiAdhoc", assignee, false, Priority.Normal);
     
        restProcessModel = restClient.withWorkflowAPI().getProcesses()
                                     .getProcessModelByProcessDefId(processModel.getId());
        restClient.withWorkflowAPI().usingProcess(restProcessModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.withWorkflowAPI().usingProcess(processModel)
                  .deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                  .assertThat()
                  .entriesListDoesNotContain("name", variableModel.getName());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Delete process variable using by the user involved in the process.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void deleteProcessVariableByUserInvolvedInTheProcess() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI()
                                 .addProcess("activitiAdhoc", assignee, false, Priority.Normal);
     
        restProcessModel = restClient.withWorkflowAPI().getProcesses()
                                     .getProcessModelByProcessDefId(processModel.getId());
        restClient.withWorkflowAPI().usingProcess(restProcessModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(assignee).withWorkflowAPI().usingProcess(processModel)
                  .deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                  .assertThat()
                  .entriesListDoesNotContain("name", variableModel.getName());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Delete process variable with invalid type")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void deleteProcessVariableInvalidType() throws Exception
    {       
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        restProcessModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI()
                .getProcesses().getProcessModelByProcessDefId(processModel.getId());
        restClient.withWorkflowAPI().usingProcess(restProcessModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
                     
        variableModel.setType("invalid-type");        
        restClient.withWorkflowAPI().usingProcess(restProcessModel).deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                  .assertThat()
                  .entriesListDoesNotContain("name", variableModel.getName());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Delete process variable by non assigned user")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void deleteProcessVarialbleByNonAssignedUser() throws Exception
    {
        UserModel nonAssigned = dataUser.createRandomTestUser();
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        restProcessModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI()
                .getProcesses().getProcessModelByProcessDefId(processModel.getId());
        restClient.withWorkflowAPI().usingProcess(restProcessModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(nonAssigned).withWorkflowAPI().usingProcess(restProcessModel)
                 .deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)                 
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.ACCESS_INFORMATION_NOT_ALLOWED, processModel.getId()))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(String.format(RestErrorModel.ACCESS_INFORMATION_NOT_ALLOWED, processModel.getId()))
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Delete process variable by inexistent user")
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })    
    public void deleteProcessVarialbleByInexistentUser() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI()
                                 .addProcess("activitiAdhoc", userWhoStartsTask, false, Priority.Normal);
        restProcessModel = restClient.authenticateUser(userWhoStartsTask).withWorkflowAPI()
                .getProcesses().getProcessModelByProcessDefId(processModel.getId());
        restClient.withWorkflowAPI().usingProcess(restProcessModel).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(UserModel.getRandomUserModel()).withWorkflowAPI()
                  .usingProcess(restProcessModel)
                  .deleteProcessVariable(variableModel);
                
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                  .assertLastError()
                  .containsSummary(RestErrorModel.AUTHENTICATION_FAILED)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }           

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, 
              description = "Verify that admin from the same network is able to delete network process variables")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL, TestGroup.NETWORKS })
    public void deleteProcessVariablesWithAdminFromSameNetwork() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser1);        
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");
        
        RestProcessModel processModel = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI()
                .addProcess("activitiAdhoc", tenantUser1, false, Priority.Normal);        
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser1).withWorkflowAPI()
                                                     .getProcesses().getProcessModelByProcessDefId(processModel.getId());       
        restClient.withWorkflowAPI().usingProcess(networkProcess1).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
                     
        restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().usingProcess(networkProcess1)
                  .deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restClient.withWorkflowAPI().usingProcess(networkProcess1).getProcessVariables()
                  .assertThat()
                  .entriesListDoesNotContain("name", variableModel.getName());
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, 
             description = "Verify that admin from different network is not able to delete network process variables")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL, TestGroup.NETWORKS })
    public void deleteProcessVariablesWithAdminFromDifferentNetwork() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser1);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");

        RestProcessModel processModel = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI()
                                                  .addProcess("activitiAdhoc", tenantUser1, false, Priority.Normal);        
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser1).withWorkflowAPI()
                                                     .getProcesses().getProcessModelByProcessDefId(processModel.getId());       
        restClient.withWorkflowAPI().usingProcess(networkProcess1).addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.authenticateUser(adminUser).withWorkflowAPI().usingProcess(networkProcess1)
                  .deleteProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
        restClient.assertLastError().containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
        
        restClient.authenticateUser(tenantUser1).withWorkflowAPI().usingProcess(networkProcess1).getProcessVariables().assertThat()
                  .entriesListContains("name", variableModel.getName());
    }

}
