package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessVariableModel;
import org.alfresco.rest.requests.RestProcessesApi;
import org.alfresco.rest.requests.RestTenantApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
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
@Test(groups = { "rest-api", "processes", "sanity" })
public class AddProcessVariableSanityTests extends RestWorkflowTest
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestProcessesApi processesApi;
    
    @Autowired
    RestTenantApi tenantApi;

    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, adminTenantUser, tenantUser, tenantUserAssignee;
    private RestProcessModel processModel;
    private UserModel adminUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
        processesApi.useRestClient(restClient);
    }

    @TestRail(section = {"rest-api", "processes" }, executionType = ExecutionType.SANITY, 
            description = "Create non-existing variable")
    public void addProcessVariable() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(userWhoStartsTask);
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = processesApi.getProcesses().getOneEntry();
        processesApi.addProcessVariable(processModel, variableModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section = {"rest-api", "processes" }, executionType = ExecutionType.SANITY, 
            description = "Update existing variable")
    public void updateExistingProcessVariable() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(userWhoStartsTask);
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = processesApi.getProcesses().getOneEntry();
        processesApi.addProcessVariable(processModel, variableModel);
        variableModel.setValue(RandomData.getRandomName("newValue"));
        processesApi.addProcessVariable(processModel, variableModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section = {"rest-api", "processes" }, executionType = ExecutionType.SANITY, 
            description = "Add process variable using admin user from same network")
    public void addProcessVariableByAdmin() throws JsonToModelConversionException, Exception
    {
        UserModel adminuser = dataUser.getAdminUser();
        restClient.authenticateUser(adminuser);
        
        adminTenantUser = UserModel.getAdminTenantUser();
        tenantApi.useRestClient(restClient);
        tenantApi.createTenant(adminTenantUser);
        
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");
        
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();   
        dataWorkflow.usingUser(tenantUser).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(tenantUserAssignee);
        
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = processesApi.getProcesses().getOneEntry();
        processesApi.addProcessVariable(processModel, variableModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @TestRail(section = {"rest-api", "processes" }, executionType = ExecutionType.SANITY, 
            description = "Adding process variable is falling in case invalid variableBody is provided")
    public void failedAddingProcessVariableIfInvalidBodyIsProvided() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("incorrect type");
        processModel = processesApi.getProcesses().getOneEntry();
        processesApi.addProcessVariable(processModel, variableModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }
}
