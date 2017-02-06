package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessVariableModel;
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

public class AddProcessVariablesFullTests extends RestTest
{
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsProcess, assignee, adminUser, adminTenantUser, adminTenantUser2, tenantUser, tenantUserAssignee;
    private RestProcessModel processModel;
    private RestProcessVariableModel variableModel, processVariable;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsProcess = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsProcess).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsProcess).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add process variables using by the user involved in the process.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void addProcessVariablesByUserInvolvedTheProcess() throws Exception
    {        
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(assignee).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();

        processVariable = restClient.authenticateUser(assignee).withWorkflowAPI().usingProcess(processModel)           
                                    .addProcessVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processVariable.assertThat().field("name").is(processVariable.getName())
                       .and().field("type").is(processVariable.getType())
                       .and().field("value").is(processVariable.getValue());

        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                .assertThat().entriesListContains("name", processVariable.getName());     
    }    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Add process variables using by inexistent user.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void addProcessVariablesByInexistentUser() throws Exception
    {        
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.authenticateUser(userWhoStartsProcess).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();

        processVariable = restClient.authenticateUser(UserModel.getRandomUserModel()).withWorkflowAPI().usingProcess(processModel)           
                                    .addProcessVariable(variableModel);
        
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                  .assertLastError()
                  .containsSummary(RestErrorModel.AUTHENTICATION_FAILED)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }   
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding process variables is falling in case invalid type is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void failedAddingProcessVariablesIfInvalidTypeIsProvided() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:textarea");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError().containsSummary(
                      String.format(RestErrorModel.UNSUPPORTED_TYPE, "d:textarea"))
                  .containsErrorKey(String.format(RestErrorModel.UNSUPPORTED_TYPE, "d:textarea"))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Bug(id = "REPO-1938")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding process variables is falling in case invalid type prefix is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void failedAddingProcessVariablesIfInvalidTypePrefixIsProvided() throws Exception
    {
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("ddt:text");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.INVALID_NAMEPACE_PREFIX, "ddt"))
                  .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding process variables is falling in case invalid value is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void failedAddingProcessVariablesIfInvalidValueIsProvided() throws Exception
    {
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:int");
        variableModel.setValue("invalidValue");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                  .containsSummary(String.format(RestErrorModel.FOR_INPUT_STRING, "invalidValue"))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION,
            description = "Adding process variables is falling in case invalid variableBody (adding extra parameter in body:scope) is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void failedAddingProcessVariablesIfInvalidBodyIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
              
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{\"name\": \"variableName\",\"scope\": \"local\",\"value\": \"testing\",\"type\": \"d:text\"}",
                "processes/{processId}/variables", processModel.getId());
        restClient.processModel(RestProcessVariableModel.class, request);
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NO_CONTENT, "Unrecognized field \"scope\""))
                  .containsErrorKey(String.format(RestErrorModel.NO_CONTENT, "Unrecognized field \"scope\""))
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Bug(id="REPO-1985")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding process variables is falling in case empty name is provided")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void failedAddingProcessVariablesIfEmptyNameIsProvided() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        variableModel.setName("");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        restClient.withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);

        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(RestErrorModel.VARIABLE_NAME_REQUIRED)
                  .containsErrorKey(RestErrorModel.VARIABLE_NAME_REQUIRED)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Bug(id="REPO-1985")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding process variables is falling in case invalid name is provided: ony white spaces")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void failedAddingProcessVariablesUsingOnlyWhiteSpaceInName() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        variableModel.setName(" ");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processVariable = restClient.withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);
       
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processVariable.assertThat().field("name").isNull()
                       .and().field("type").is(processVariable.getType())
                       .and().field("value").is(processVariable.getValue());

        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                .assertThat().entriesListContains("value", processVariable.getValue());               
    }
    
    @Bug(id="REPO-1987")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES }, executionType = ExecutionType.REGRESSION, 
            description = "Adding process variables is falling in case invalid name is provided: symbols")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL })
    public void addingProcessVariablesUsingSymbolsInName() throws Exception
    {
        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().addProcess("activitiAdhoc", assignee, false, Priority.Normal);
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        variableModel.setName("123_%^&: õÈ,Ì,Ò");

        processModel = restClient.authenticateUser(adminUser).withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processVariable = restClient.withWorkflowAPI().usingProcess(processModel).addProcessVariable(variableModel);
       
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processVariable.assertThat().field("name").is(processVariable.getName())
                       .and().field("type").is(processVariable.getType())
                       .and().field("value").is(processVariable.getValue());

        restClient.withWorkflowAPI().usingProcess(processModel).getProcessVariables()
                .assertThat().entriesListContains("name", processVariable.getName()); 
    }    
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES,  TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION, 
            description = "Add process variables using by Admin in other network.")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.FULL, TestGroup.NETWORKS })
    public void addProcessVariablesByAdminInOtherNetwork() throws Exception
    { 
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
       
        adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.usingTenant().createTenant(adminTenantUser2);
        
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(document)
                    .createNewTaskAndAssignTo(tenantUserAssignee);
        
        variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = restClient.withWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        processVariable = restClient.authenticateUser(adminTenantUser2).withWorkflowAPI().usingProcess(processModel)           
                .addProcessVariable(variableModel);
        
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .containsErrorKey(RestErrorModel.PROCESS_RUNNING_IN_ANOTHER_TENANT)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }

}
