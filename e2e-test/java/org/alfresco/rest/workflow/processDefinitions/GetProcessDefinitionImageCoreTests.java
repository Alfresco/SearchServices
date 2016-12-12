package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 12/6/2016.
 */
public class GetProcessDefinitionImageCoreTests extends RestTest
{
    private UserModel adminUser, adminTenantUser;
    private RestProcessDefinitionModel randomProcessDefinition;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify if get process definition image returns status code 404 when invalid processDefinitionId is used")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.CORE })
    public void getProcessDefinitionImageUsingInvalidProcessDefinitionId() throws Exception
    {
        restClient.authenticateUser(adminUser);
        randomProcessDefinition = restClient.withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry();
        randomProcessDefinition.onModel().setId("invalidID");
        restClient.withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionImage();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidID"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify network admin is able to get a process definition image using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.CORE, TestGroup.NETWORKS })
    @Bug(id = "MNT-17243")
    public void networkAdminGetProcessDefinitionImage() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);
        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry();
        restClient.withWorkflowAPI().usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionImage()
                .assertResponseContainsImage();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify network user is able to get a process definition image using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.CORE, TestGroup.NETWORKS })
    @Bug(id = "MNT-17243")
    public void networkUserGetProcessDefinitionImage() throws Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser)
                .usingTenant().createTenant(adminTenantUser);
        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        randomProcessDefinition = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry();
        restClient.authenticateUser(tenantUser).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinitionImage()
                .assertResponseContainsImage();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
