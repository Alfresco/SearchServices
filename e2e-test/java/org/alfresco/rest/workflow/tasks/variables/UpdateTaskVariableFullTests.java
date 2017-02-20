package org.alfresco.rest.workflow.tasks.variables;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.rest.model.RestVariableModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class UpdateTaskVariableFullTests extends RestTest
{
    private SiteModel siteModel;
    private FileModel fileModel;
    private TaskModel taskModel;
    private UserModel userModel;
    private RestVariableModel taskVariable, updatedTaskVariable;  

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);      
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(userModel);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with invalid name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithInvalidVariableName() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"scope\": \"local\",\"names\": \"varName\",\"value\": \"test\","
                + "\"type\": \"d:text\"}",
                              "tasks/{taskId}/variables/{variableName}", taskModel.getId(), taskVariable.getName());
        restClient.processModel(RestVariableModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NO_CONTENT,"Unrecognized field " + "\"names\"")); 
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with invalid name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithInvalidVariableValue() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"scope\": \"local\",\"name\": \"varName\",\"values\": \"test\","
                + "\"type\": \"d:text\"}",
                              "tasks/{taskId}/variables/{variableName}", taskModel.getId(), taskVariable.getName());
        restClient.processModel(RestVariableModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NO_CONTENT,"Unrecognized field " + "\"values\"")); 
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with invalid name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithEmptyVariableName() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"scope\": \"local\",\"\": \"varName\",\"value\": \"test\","
                + "\"type\": \"d:text\"}",
                              "tasks/{taskId}/variables/{variableName}", taskModel.getId(), taskVariable.getName());
        restClient.processModel(RestVariableModel.class, request);     
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NO_CONTENT,"Unrecognized field " + "\"\"")); 
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with empty name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithEmptyName() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        taskVariable.setName("");        
        restClient.withWorkflowAPI().usingTask(taskModel).updateTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED)
                  .assertLastError().containsErrorKey(RestErrorModel.PUT_EMPTY_ARGUMENT)
                  .containsSummary(RestErrorModel.PUT_EMPTY_ARGUMENT)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with empty name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithInvalidName() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"scope\": \"local\",\"name\": ',\"value\": \"test\","
                + "\"type\": \"d:text\"}",
                              "tasks/{taskId}/variables/{variableName}", taskModel.getId(), taskVariable.getName());
        restClient.processModel(RestVariableModel.class, request); 
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NO_CONTENT,"Unexpected character " + "('''")); 
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with empty name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithInvalidValue() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"scope\": \"local\",\"name\": \"varName\",\"value\"::,"
                + "\"type\": \"d:text\"}",
                              "tasks/{taskId}/variables/{variableName}", taskModel.getId(), taskVariable.getName());
        restClient.processModel(RestVariableModel.class, request); 
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NO_CONTENT,"Unexpected character " + "(':'"));      
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with empty name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithMissingType() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"scope\": \"local\",\"name\": \"varName\",\"value\": \"test\"}",
                              "tasks/{taskId}/variables/{variableName}", taskModel.getId(), taskVariable.getName());
        restClient.processModel(RestVariableModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        taskVariable.assertThat().field("scope").is(taskVariable.getScope())
                    .and().field("name").is(taskVariable.getName())
                    .and().field("type").is("d:text")
                    .and().field("value").is(taskVariable.getValue());   
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with empty name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithMissingTypeAndValue() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"scope\": \"local\",\"name\": \"varName\"}",
                              "tasks/{taskId}/variables/{variableName}", taskModel.getId(), taskVariable.getName());
        updatedTaskVariable = restClient.processModel(RestVariableModel.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedTaskVariable.assertThat().field("scope").is(updatedTaskVariable.getScope())
                    .and().field("name").is(updatedTaskVariable.getName())
                    .and().field("type").is("d:any");                   
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with invalid name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithEmptyBody() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{}",
                              "tasks/{taskId}/variables/{variableName}", taskModel.getId(), taskVariable.getName());
        restClient.processModel(RestVariableModel.class, request);        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError().containsErrorKey(RestErrorModel.VARIABLE_NAME_REQUIRED)
                  .containsSummary(RestErrorModel.VARIABLE_NAME_REQUIRED)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with invalid name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTaskVariableWithInvalidBody() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel)
                .withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        RestRequest request = RestRequest.requestWithBody(HttpMethod.PUT, "{\"scope\": \"local\",\"name\": \"varName\",\"value\": \"test\","
                + "\"type\": \"d:text\", \"errorKey\": \"invalidBody\"}",
                              "tasks/{taskId}/variables/{variableName}", taskModel.getId(), taskVariable.getName());
        restClient.processModel(RestVariableModel.class, request);        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.NO_CONTENT,"Unrecognized field " + "\"errorKey\"")); 
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Update task variable with empty name - PUT call")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void updateTwiceInARowSameTaskVariable() throws Exception
    {
        taskVariable = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restClient.authenticateUser(userModel).withWorkflowAPI().usingTask(taskModel).addTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        taskVariable.setName("newName");    
        taskVariable.setScope("global");
        updatedTaskVariable = restClient.withWorkflowAPI().usingTask(taskModel).updateTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedTaskVariable.assertThat().field("scope").is("global");
        updatedTaskVariable.assertThat().field("name").is("newName");
        
        updatedTaskVariable = restClient.withWorkflowAPI().usingTask(taskModel).updateTaskVariable(taskVariable);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        updatedTaskVariable.assertThat().field("scope").is("global");
        updatedTaskVariable.assertThat().field("name").is("newName");        
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from another network is not able to update task variables")
    public void updateTaskVariablesByTenantFromAnotherNetwork() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        UserModel adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser1);
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser2);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");

        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser1).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser1)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser1);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().usingTask(task.onModel())
                                 .addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(adminTenantUser2).withWorkflowAPI().usingTask(task.onModel())
                 .updateTaskVariable(taskVariable);
                        
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                              .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}
