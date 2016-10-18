package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.rest.requests.RestProcessesApi;
import org.alfresco.rest.requests.RestTenantApi;
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

@Test(groups = { TestGroup.REST_API, TestGroup.PROCESSES, TestGroup.SANITY })
public class GetProcessItemsSanityTests extends RestWorkflowTest
{
    @Autowired
    private DataUser dataUser;

    @Autowired
    private RestProcessesApi processesApi;

    @Autowired
    RestTenantApi tenantApi;
    
    private FileModel document;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, adminTenantUser, tenantUser;
    private RestProcessModel processModel;

    private UserModel tenantUserAssignee;

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

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Verify that user that started the process gets all process items")
    public void getProcessItemsUsingTheUserWhoStartedProcess() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(userWhoStartsTask);
        processModel = processesApi.getProcesses().getOneRandomEntry();
        processesApi.getProcessesItems(processModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Verify that user that is involved in the process gets all process items")
    public void getProcessItemsUsingUserInvolvedInProcess() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(assignee);
        processModel = processesApi.getProcesses().getOneRandomEntry();
        processesApi.getProcessesItems(processModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.NETWORKS })
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY, 
            description = "Get process items using admin from same network")
    public void getProcessItemsUsingAdminUserFromSameNetwork() throws JsonToModelConversionException, Exception
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
        
        processModel = processesApi.getProcesses().getOneRandomEntry();
        processesApi.getProcessesItems(processModel);
        processesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
