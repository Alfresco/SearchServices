package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.requests.RestProcessesApi;
import org.alfresco.utility.data.DataUser;
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
public class GetProcessesVariablesSanityTests extends RestWorkflowTest
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestProcessesApi processesApi;

    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee;
    private RestProcessModel processModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
        processesApi.useRestClient(restClient);
    }

    @TestRail(section = {"rest-api", "processes" }, executionType = ExecutionType.SANITY, 
            description = "Verify that user that started the process gets all process variables")
    public void getProcessVariablesUsingTheUserWhoStartedProcess() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(userWhoStartsTask);
        processModel = processesApi.getProcesses().getOneEntry();
        processesApi.getProcessesVariables(processModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { "rest-api", "processes" }, executionType = ExecutionType.SANITY, 
            description = "Verify get all process variables call using a user that is involved in the process")
    public void getProcessVariablesUsingUserInvolvedInProcess() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(assignee);
        processModel = processesApi.getProcesses().getOneEntry();
        processesApi.getProcessesVariables(processModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
