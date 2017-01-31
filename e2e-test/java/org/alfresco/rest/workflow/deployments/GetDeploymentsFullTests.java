package org.alfresco.rest.workflow.deployments;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestDeploymentModel;
import org.alfresco.rest.model.RestDeploymentModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 1/30/2017.
 */
public class GetDeploymentsFullTests extends RestTest
{
    private UserModel adminUserModel;
    private RestDeploymentModelsCollection deployments;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user gets non-network deployments with skipCount parameter applied using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.FULL})
    public void getNonNetworkDeploymentsWithValidSkipCount() throws JsonToModelConversionException, Exception
    {
        deployments = restClient.withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        RestDeploymentModel firstDeployment = deployments.getEntries().get(0);
        RestDeploymentModel secondDeployment = deployments.getEntries().get(1);
        RestDeploymentModelsCollection deploymentsWithSkipCount = restClient.withParams("skipCount=2").withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deploymentsWithSkipCount
                .assertThat().entriesListDoesNotContain("name", firstDeployment.getName())
                .assertThat().entriesListDoesNotContain("name", secondDeployment.getName())
                .assertThat().entriesListCountIs(deployments.getEntries().size()-2);
        deploymentsWithSkipCount.assertThat().paginationField("skipCount").is("2");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user doesn't get non-network deployments when negative skipCount parameter is applied using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.FULL})
    public void getNonNetworkDeploymentsWithNegativeSkipCount() throws JsonToModelConversionException, Exception
    {
        restClient.withParams("skipCount=-1").withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(RestErrorModel.NEGATIVE_VALUES_SKIPCOUNT)
                .containsErrorKey(RestErrorModel.NEGATIVE_VALUES_SKIPCOUNT)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .statusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user doesn't get non-network deployments when non numeric skipCount parameter is applied using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.FULL})
    public void getNonNetworkDeploymentsWithNotNumericSkipCount() throws JsonToModelConversionException, Exception
    {
        restClient.withParams("skipCount=A").withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "A"));
    }


    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user gets non-network deployments with maxItems parameter applied using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.FULL})
    public void getNonNetworkDeploymentsWithValidMaxItems() throws JsonToModelConversionException, Exception
    {
        deployments = restClient.withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        RestDeploymentModel firstDeployment = deployments.getEntries().get(0).onModel();
        RestDeploymentModel secondDeployment = deployments.getEntries().get(1).onModel();
        RestDeploymentModelsCollection deploymentsWithMaxItems = restClient.withParams("maxItems=2").withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deploymentsWithMaxItems
                .assertThat().entriesListContains("name", firstDeployment.getName())
                .assertThat().entriesListContains("name", secondDeployment.getName())
                .assertThat().entriesListCountIs(2);
        deploymentsWithMaxItems.assertThat().paginationField("maxItems").is("2");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user doesn't get non-network deployments when negative maxItems parameter is applied using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.FULL})
    public void getNonNetworkDeploymentsWithNegativeMaxItems() throws JsonToModelConversionException, Exception
    {
        restClient.withParams("maxItems=-1").withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
                .containsErrorKey(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .statusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user doesn't get non-network deployments when non numeric maxItems parameter is applied using REST API and status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.FULL})
    public void getNonNetworkDeploymentsWithNotNumericMaxItems() throws JsonToModelConversionException, Exception
    {
        restClient.withParams("maxItems=A").withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "A"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user gets non-network deployments with properties parameter applied using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.FULL})
    public void getNonNetworkDeploymentsWithValidProperties() throws JsonToModelConversionException, Exception
    {
        deployments = restClient.withParams("properties=name").withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deployments.assertThat().entriesListIsNotEmpty();
        deployments.getOneRandomEntry().onModel().assertThat()
                .fieldsCount().is(1).and()
                .field("id").isNull().and()
                .field("deployedAt").isNull().and()
                .field("name").isNotEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS }, executionType = ExecutionType.REGRESSION,
            description = "Verify Admin user gets non-network deployments with non existing properties parameter applied using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.FULL})
    public void getNonNetworkDeploymentsWithNonExistingProperties() throws JsonToModelConversionException, Exception
    {
        deployments = restClient.withParams("properties=TAS").withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deployments.assertThat().entriesListIsNotEmpty();
        deployments.getOneRandomEntry().onModel().assertThat()
                .fieldsCount().is(0).and()
                .field("id").isNull().and()
                .field("deployedAt").isNull().and()
                .field("name").isNull();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS },
            executionType = ExecutionType.REGRESSION,
            description = "Verify that network admin user is not able to get a deployment from other network using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.DEPLOYMENTS, TestGroup.FULL, TestGroup.NETWORKS})
    public void adminDoesNotGetDeploymentsFromOtherNetwork() throws Exception
    {
        UserModel adminTenantUser1 = UserModel.getAdminTenantUser();
        UserModel adminTenantUser2 = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser1);
        restClient.usingTenant().createTenant(adminTenantUser2);

        RestDeploymentModel deployment = restClient.authenticateUser(adminTenantUser1).withWorkflowAPI().getDeployments().getOneRandomEntry().onModel();
        deployments = restClient.authenticateUser(adminTenantUser2).withWorkflowAPI().getDeployments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        deployments.assertThat().entriesListDoesNotContain("id", deployment.getId());
    }

}
