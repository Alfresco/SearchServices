package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetProcessDefinitionsCoreTests extends RestTest
{
    private UserModel adminUserModel;
    private UserModel userModel;
    private UserModel adminTenantUser;
    private UserModel tenantUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        adminTenantUser = UserModel.getAdminTenantUser();
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION }, executionType = ExecutionType.REGRESSION,
            description = "Verify get process definitions using any network user for network enabled deployments with REST API status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION, TestGroup.NETWORKS})
    public void networkUserGetsProcessDefinitions() throws Exception
    {
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        restClient.authenticateUser(tenantUser)
                .withWorkflowAPI()
                .getAllProcessDefinitions()
                .assertThat().entriesListIsNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION }, executionType = ExecutionType.REGRESSION,
            description = "Verify call to get process definitions with invalid orderBy parameter with REST API and status code is BAD_REQUEST (400)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION })
    public void userGetProcessDefinitionsWithInvalidOrderBy() throws Exception
    {
        restClient.authenticateUser(userModel)
                .withParams("orderBy=test")
                .withWorkflowAPI()
                .getAllProcessDefinitions()
                .assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_ORDERBY, "test", "deploymentId, name, id, category, version, key"));
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION }, executionType = ExecutionType.REGRESSION,
            description = "Verify call to get process definitions with invalid where parameter with REST API and status code is BAD_REQUEST (400)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION })
    public void userGetProcessDefinitionsWithInvalidWhere() throws Exception
    {
        restClient.authenticateUser(userModel)
                .withParams("where=test")
                .withWorkflowAPI()
                .getAllProcessDefinitions()
                .assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_WHERE_QUERY, "test"))
                .containsErrorKey(RestErrorModel.INVALID_QUERY_ERRORKEY)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}
