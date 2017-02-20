package org.alfresco.rest.workflow.tasks.post;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestTaskModel;
import org.alfresco.rest.model.RestVariableModel;
import org.alfresco.rest.model.RestVariableModelsCollection;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;


public class AddTaskVariablesFullTests extends RestTest
{
    private UserModel userWhoStartsTask, adminUser;
    private SiteModel siteModel;
    private FileModel fileModel;
    private UserModel assigneeUser;
    private TaskModel taskModel;
    private RestVariableModel restVariablemodel, variableModel, variableModel1;
    private RestVariableModelsCollection restVariableCollection;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsTask = dataUser.createRandomTestUser();
        assigneeUser = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        taskModel = dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);
    }

    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Create task variable with name containing symbols")
    public void createTaskVariableWithSymbolsInName() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel.setName("!@#$%^&*({}<>.,;'=_|");
        restVariablemodel = restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restVariablemodel.assertThat()
             .field("scope").is(variableModel.getScope())
             .and().field("name").is(variableModel.getName())
             .and().field("value").is(variableModel.getValue())
             .and().field("type").is(variableModel.getType());
    }
    
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Create multiple task variable with name containing symbols")
    public void createMultipleTaskVariableWithSymbolsInName() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        variableModel1 = RestVariableModel.getRandomTaskVariableModel("global", "d:text");
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel.setName("!@#$%^&*({}<>.,;'=_|");
                
        restVariableCollection = restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariables(variableModel,variableModel1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restVariableCollection.getEntries().get(0).onModel().assertThat()
                              .field("scope").is(variableModel.getScope())
                              .and().field("name").is(variableModel.getName())
                              .and().field("value").is(variableModel.getValue())
                              .and().field("type").is(variableModel.getType());

        restVariableCollection.getEntries().get(1).onModel().assertThat()
                              .field("scope").is(variableModel1.getScope())
                              .and().field("name").is(variableModel1.getName())
                              .and().field("value").is(variableModel1.getValue())
                              .and().field("type").is(variableModel1.getType());                      
    }
    
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Create task variable with empty name")
    public void createTaskVariableWithEmptyName() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel.setName("");
        restVariablemodel = restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsErrorKey(RestErrorModel.VARIABLE_NAME_REQUIRED)
                              .containsSummary(RestErrorModel.VARIABLE_NAME_REQUIRED)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = {TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Create task variable with empty name")
    public void failledCreatingMultipleTaskVariableWithEmptyName() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel1 = RestVariableModel.getRandomTaskVariableModel("global", "d:text");
        variableModel.setName("");
        
        restVariableCollection = restClient.withWorkflowAPI().usingTask(taskModel).addTaskVariables(variableModel, variableModel1);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsErrorKey(RestErrorModel.VARIABLE_NAME_REQUIRED)
                              .containsSummary(RestErrorModel.VARIABLE_NAME_REQUIRED)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the same network is able to add task variables")
    public void addTaskVariablesByTenantAdmin() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser1);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");

        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser1).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser1)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser1);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        restVariablemodel = restClient.withWorkflowAPI()
                    .usingTask(task.onModel())
                        .addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restVariablemodel.assertThat()
             .field("scope").is(variableModel.getScope())
             .and().field("name").is(variableModel.getName())
             .and().field("value").is(variableModel.getValue())
             .and().field("type").is(variableModel.getType());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from the same network is able to add multiple task variables")
    public void addMultipleTaskVariablesByTenantAdmin() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser1);
        UserModel tenantUser1 = dataUser.usingUser(adminTenantUser1).createUserWithTenant("uTenant1");

        RestProcessModel networkProcess1 = restClient.authenticateUser(tenantUser1).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser1, false, CMISUtil.Priority.High);
        RestTaskModel task = restClient.authenticateUser(adminTenantUser1)
                            .withWorkflowAPI().usingProcess(networkProcess1).getProcessTasks().getOneRandomEntry();
        restClient.authenticateUser(adminTenantUser1);
        
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel1 = RestVariableModel.getRandomTaskVariableModel("global", "d:text");
                
        restVariableCollection = restClient.withWorkflowAPI().usingTask(task.onModel())
                                           .addTaskVariables(variableModel, variableModel1);        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restVariableCollection.getEntries().get(0).onModel().assertThat()
                              .field("scope").is(variableModel.getScope())
                              .and().field("name").is(variableModel.getName())
                              .and().field("value").is(variableModel.getValue())
                              .and().field("type").is(variableModel.getType());

        restVariableCollection.getEntries().get(1).onModel().assertThat()
                              .field("scope").is(variableModel1.getScope())
                              .and().field("name").is(variableModel1.getName())
                              .and().field("value").is(variableModel1.getValue())
                              .and().field("type").is(variableModel1.getType());      
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from another network is not able to add task variables")
    public void addTaskVariablesByTenantFromAnotherNetwork() throws Exception
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
        restVariablemodel = restClient.authenticateUser(adminTenantUser2).withWorkflowAPI()
                    .usingTask(task.onModel())
                        .addTaskVariable(variableModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                              .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify that admin from another network is not able to add multiple task variables")
    public void addMultipleTaskVariablesByTenantFromAnotherNetwork() throws Exception
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
        
        variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        variableModel1 = RestVariableModel.getRandomTaskVariableModel("global", "d:text");
        restVariableCollection = restClient.authenticateUser(adminTenantUser2).withWorkflowAPI()
                                           .usingTask(task.onModel())
                                           .addTaskVariables(variableModel,variableModel1);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
            .assertLastError().containsErrorKey(RestErrorModel.PERMISSION_DENIED_ERRORKEY)
                              .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED)
                              .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                              .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}
