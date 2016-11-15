package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.requests.Processes;
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
public class RemoveProcessItemSanityTests extends RestWorkflowTest
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private Processes processesApi;

    private FileModel document, document2, document3;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee;
    private RestProcessModel processModel;
    private UserModel adminUser;
    private RestItemModel processItem;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
        processesApi.useRestClient(restClient);
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Delete existing process item")
    public void deleteProcessItem() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        processModel = processesApi.getProcesses().getOneRandomEntry();
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.MSWORD);
        processItem = processesApi.addProcessItem(processModel, document2);    
        processesApi.deleteProcessItem(processModel, processItem);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT);
        processesApi.getProcessesItems(processModel).assertThat().entriesListDoesNotContain("id", processItem.getId());
    }
    
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Try to delete existing process item using invalid processId")
    public void deleteProcessItemUsingInvalidProcessId() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUser);
        processModel = processesApi.getProcesses().getOneRandomEntry();
        document3 = dataContent.usingSite(siteModel).createContent(DocumentType.MSPOWERPOINT);
        processItem = processesApi.addProcessItem(processModel, document3);  
        processModel.onModel().setId("incorrectProcessId");
        processesApi.deleteProcessItem(processModel, processItem);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }
}
