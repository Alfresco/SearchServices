package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.rest.model.RestProcessDefinitionModelsCollection;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 2/1/2017.
 */
public class GetProcessDefinitionFullTests extends RestTest
{
    private UserModel adminUser;
    private RestProcessDefinitionModel randomProcessDefinition, returnedProcessDefinition;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW,  TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify if get process definition returns all process definitions when empty processDefinitionId is used")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION })
    public void getProcessDefinitionUsingEmptyProcessDefinitionId() throws Exception
    {
        restClient.authenticateUser(adminUser).withWorkflowAPI();
        RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "/process-definitions/{processDefinitionId}", "");
        RestProcessDefinitionModelsCollection processDefinitions = restClient.processModels(RestProcessDefinitionModelsCollection.class, request);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        processDefinitions.assertThat().entriesListIsNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user gets a process definition with properties parameter applied using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.REGRESSION })
    public void getProcessDefinitionWithValidProperties() throws Exception
    {
        randomProcessDefinition = restClient.authenticateUser(adminUser).withWorkflowAPI().getAllProcessDefinitions().getOneRandomEntry().onModel();
        returnedProcessDefinition = restClient.withParams("properties=id,name,graphicNotationDefined,version")
                .withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedProcessDefinition.assertThat()
                .fieldsCount().is(4).and()
                .field("deploymentId").isNull().and()
                .field("description").isNull().and()
                .field("id").is(randomProcessDefinition.getId()).and()
                .field("startFormResourceKey").isNull().and()
                .field("category").isNull().and()
                .field("title").isNull().and()
                .field("version").is(randomProcessDefinition.getVersion()).and()
                .field("graphicNotationDefined").is(randomProcessDefinition.getGraphicNotationDefined()).and()
                .field("key").isNull().and()
                .field("name").is(randomProcessDefinition.getName());
    }

    @TestRail(section = { TestGroup.REST_API,  TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.REGRESSION,
            description = "Verify Network user is not able to get a process definition from another network")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESS_DEFINITION, TestGroup.NETWORKS, TestGroup.REGRESSION })
    public void getProcessDefinitionFromAnotherNetwork() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        UserModel adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser1);
        restClient.usingTenant().createTenant(adminTenantUser2);

        randomProcessDefinition = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI()
                .getAllProcessDefinitions().getOneRandomEntry().onModel();
        restClient.authenticateUser(adminTenantUser2).withWorkflowAPI()
                .usingProcessDefinitions(randomProcessDefinition).getProcessDefinition();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, randomProcessDefinition.getId()))
                .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}
