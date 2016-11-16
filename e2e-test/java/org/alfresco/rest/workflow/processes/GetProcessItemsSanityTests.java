package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.CMISUtil.Priority;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestItemModelsCollection;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */

@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY })
public class GetProcessItemsSanityTests extends RestWorkflowTest
{
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, adminTenantUser, tenantUser, tenantUserAssignee;
    private RestProcessModel processModel;
    private RestItemModelsCollection items;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userWhoStartsTask).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY,
            description = "Verify that user that started the process gets all process items")
    public void getProcessItemsUsingTheUserWhoStartedProcess() throws Exception
    {
        processModel = restClient.authenticateUser(userWhoStartsTask).onWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        items = restClient.onWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        items.assertThat().entriesListIsNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY,
            description = "Verify that user that is involved in the process gets all process items")
    public void getProcessItemsUsingUserInvolvedInProcess() throws Exception
    {
        processModel = restClient.authenticateUser(assignee).onWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        items = restClient.onWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        items.assertThat().entriesListIsNotEmpty();
    }

    @Test(groups = { TestGroup.NETWORKS })
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY,
            description = "Get process items using admin from same network")
    public void getProcessItemsUsingAdminUserFromSameNetwork() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(dataUser.getAdminUser()).usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        tenantUserAssignee = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenantAssignee");
        restClient.authenticateUser(tenantUser).onWorkflowAPI().addProcess("activitiAdhoc", tenantUserAssignee, false, Priority.Normal);

        processModel = restClient.authenticateUser(adminTenantUser).onWorkflowAPI().getProcesses().getOneRandomEntry().onModel();
        items = restClient.onWorkflowAPI().usingProcess(processModel).getProcessItems();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        items.assertThat().entriesListIsNotEmpty();
    }
}
