package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestVariableModel;
import org.alfresco.rest.requests.RestTasksApi;
import org.alfresco.rest.requests.RestTenantApi;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TaskModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.SANITY })
public class UpdateTaskVariableSanityTests extends RestWorkflowTest
{
    @Autowired
    RestTasksApi tasksApi;
    
    @Autowired
    RestTenantApi tenantApi;
    
    private UserModel userModel;
    private SiteModel siteModel; 
    private FileModel fileModel;
    private UserModel assigneeUser;
    private TaskModel taskModel, tenantTask;

    
    private UserModel adminTenantUser, tenantUser, tenantUserAssignee;

    private RestVariableModel taskVariable;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        assigneeUser = dataUser.createRandomTestUser();
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(assigneeUser);

        tasksApi.useRestClient(restClient);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Create non-existing task variable")
    public void createTaskVariable() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = tasksApi.updateTaskVariable(taskModel, variableModel);
        taskVariable.and().assertField("scope").is(variableModel.getScope())
                    .and().assertField("name").is(variableModel.getName())
                    .and().assertField("type").is(variableModel.getType())
                    .and().assertField("value").is(variableModel.getValue());
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Update existing task variable")
    public void updateTaskVariable() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = tasksApi.updateTaskVariable(taskModel, variableModel);
        taskVariable.and().assertField("scope").is(variableModel.getScope())
                    .and().assertField("name").is(variableModel.getName())
                    .and().assertField("type").is(variableModel.getType())
                    .and().assertField("value").is(variableModel.getValue());
        variableModel.setValue("updatedValue");
        taskVariable = tasksApi.updateTaskVariable(taskModel, variableModel).and().assertField("value").is("updatedValue");
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = {TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.SANITY, 
            description = "Update existing task variable by admin in the same network")
    public void updateTaskVariableByAdminInSameNetwork() throws Exception
    {
        UserModel adminUser = dataUser.getAdminUser();
        restClient.authenticateUser(adminUser);
        
        adminTenantUser = UserModel.getAdminTenantUser();
        tenantApi.useRestClient(restClient);
        tenantApi.createTenant(adminTenantUser);
        
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");
        
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();   
        tenantTask = dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(tenantUserAssignee);
        
        RestVariableModel variableModel = RestVariableModel.getRandomTaskVariableModel("local", "d:text");
        taskVariable = tasksApi.updateTaskVariable(tenantTask, variableModel);
        
        taskVariable.and().assertField("scope").is(taskVariable.getScope())
            .and().assertField("name").is(variableModel.getName())
            .and().assertField("type").is(variableModel.getType())
            .and().assertField("value").is(variableModel.getValue());
        
        variableModel.setValue("updatedValue");
        taskVariable = tasksApi.updateTaskVariable(taskModel, variableModel).and().assertField("value").is("updatedValue");
        tasksApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}