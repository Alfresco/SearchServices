package org.alfresco.rest.workflow.processDefinitions;

import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.model.RestProcessDefinitionModel;
import org.alfresco.rest.requests.RestProcessDefinitionsApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/13/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION, TestGroup.COMMENTS })
public class GetProcessDefinitionSanityTests extends RestWorkflowTest
{
    @Autowired
    private DataUser dataUser;
    @Autowired
    private RestProcessDefinitionsApi processDefinitionsApi;

    private UserModel testUser;
    private RestProcessDefinitionModel randomProcessDefinition;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        testUser = dataUser.createRandomTestUser();
        processDefinitionsApi.useRestClient(restClient);
        restClient.authenticateUser(dataUser.getAdminUser());
        randomProcessDefinition = processDefinitionsApi.getProcessDefinitions().getOneRandomEntry();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Admin user gets a specific process definition for non-network deployments using REST API and status code is OK (200)")
    public void adminGetsProcessDefinition() throws Exception
    {
        processDefinitionsApi.getProcessDefinition(randomProcessDefinition).assertProcessDefinitionNameIs(randomProcessDefinition.onModel().getName());
        processDefinitionsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PROCESS_DEFINITION },
            executionType = ExecutionType.SANITY,
            description = "Verify Any user gets a specific process definition for non-network deployments using REST API and status code is OK (200)")
    public void anyUserGetsProcessDefinition() throws Exception
    {
        restClient.authenticateUser(testUser);
        processDefinitionsApi.getProcessDefinition(randomProcessDefinition).assertProcessDefinitionNameIs(randomProcessDefinition.onModel().getName());
        processDefinitionsApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);
    }
}
