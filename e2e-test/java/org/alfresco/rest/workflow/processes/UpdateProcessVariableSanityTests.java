package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.model.RestProcessVariableModel;
import org.alfresco.rest.requests.RestProcessesApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
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
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class UpdateProcessVariableSanityTests extends RestWorkflowTest     
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestProcessesApi processesApi;
    
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee;
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
    
    @TestRail(section = {"rest-api", TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Create non-existing variable using put call")
    public void addProcessVariable() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = processesApi.addProcess("activitiAdhoc", adminUser, false, CMISUtil.Priority.Normal);
        processesApi.updateProcessVariable(processModel, variableModel);
        processesApi.getProcessesVariables(processModel).assertThat().entriesListContains("name", variableModel.getName());
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {"rest-api", TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Update existing variable using put call")
    public void updateProcessVariable() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = processesApi.getProcesses().getOneRandomEntry();
        processesApi.addProcessVariable(processModel, variableModel);
        variableModel.setValue("newValue");
        processesApi.updateProcessVariable(processModel, variableModel).assertThat().field("value").is("newValue");
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = {"rest-api", TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Try to add process variable using an invalid processId")
    public void addProcessVariableUsingInvalidProcessId() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        RestProcessVariableModel variableModel = RestProcessVariableModel.getRandomProcessVariableModel("d:text");
        processModel = processesApi.getProcesses().getOneRandomEntry();
        processModel.onModel().setId("abc");
        processesApi.updateProcessVariable(processModel, variableModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }   
}
