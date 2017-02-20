package org.alfresco.rest.workflow.tasks;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.*;
import org.alfresco.utility.model.*;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 2/3/2017.
 */
public class GetTaskFormModelFullTests extends RestTest
{
    UserModel userModel, adminUser;
    SiteModel siteModel;
    FileModel fileModel;
    TaskModel taskModel;
    RestFormModelsCollection returnedCollection;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        fileModel = dataContent.usingSite(siteModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        taskModel = dataWorkflow.usingUser(userModel).usingSite(siteModel).usingResource(fileModel).createNewTaskAndAssignTo(userModel);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.REGRESSION, description = "Verify admin user gets all task form models with properties parameter applied and response is successful (200)")
    public void adminGetsTaskFormModelsWithPropertiesParameter() throws Exception
    {
        returnedCollection = restClient.authenticateUser(adminUser).withParams("properties=qualifiedName,required").withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListIsNotEmpty();
        returnedCollection.getOneRandomEntry().onModel()
                .assertThat()
                    .field("qualifiedName").isNotEmpty().and()
                    .field("required").isNotEmpty().and()
                    .field("dataType").isNull().and()
                    .field("name").isNull().and()
                    .field("title").isNull().and()
                    .field("defaultValue").isNull().and()
                    .field("allowedValues").isNull().and()
                    .fieldsCount().is(2);
        }

    @Bug(id = "MNT-17438")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin gets task form model with valid skipCount parameter applied using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void getTaskFormModelWithValidSkipCount() throws Exception
    {
        returnedCollection = restClient.authenticateUser(adminUser)
                .withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        RestFormModel firstTaskFormModel = returnedCollection.getEntries().get(0).onModel();
        RestFormModel secondTaskFormModel = returnedCollection.getEntries().get(1).onModel();

        RestFormModelsCollection formModelsWithSkipCount = restClient.withParams("skipCount=2").withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        formModelsWithSkipCount
                .assertThat().entriesListDoesNotContain("name", firstTaskFormModel.getName())
                .assertThat().entriesListDoesNotContain("name", secondTaskFormModel.getName())
                .assertThat().entriesListCountIs(returnedCollection.getEntries().size()-2);
        formModelsWithSkipCount.assertThat().paginationField("skipCount").is("2");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin doesn't get task form model with negative skipCount parameter applied using REST API")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void getTaskFormModelWithNegativeSkipCount() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("skipCount=-1").withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(RestErrorModel.NEGATIVE_VALUES_SKIPCOUNT)
                .containsErrorKey(RestErrorModel.NEGATIVE_VALUES_SKIPCOUNT)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .statusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin doesn't get task form model with non numeric skipCount parameter applied using REST API")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void getTaskFormModelWithNonNumericSkipCount() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("skipCount=A").withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, "A"));
    }

    @Bug(id = "MNT-17438")
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin gets task form model with valid maxItems parameter applied using REST API and status code is OK (200)")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void getTaskFormModelWithValidMaxItems() throws Exception
    {
        returnedCollection = restClient.authenticateUser(adminUser)
                .withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        RestFormModel firstTaskFormModel = returnedCollection.getEntries().get(0).onModel();
        RestFormModel secondTaskFormModel = returnedCollection.getEntries().get(1).onModel();

        RestFormModelsCollection formModelsWithMaxItems = restClient.withParams("maxItems=2").withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        formModelsWithMaxItems
                .assertThat().entriesListContains("name", firstTaskFormModel.getName())
                .assertThat().entriesListContains("name", secondTaskFormModel.getName())
                .assertThat().entriesListCountIs(2);
        formModelsWithMaxItems.assertThat().paginationField("maxItems").is("2");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin doesn't get task form model with negative maxItems parameter applied using REST API")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void getTaskFormModelWithNegativeMaxItems() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("maxItems=-1").withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
                .containsErrorKey(RestErrorModel.ONLY_POSITIVE_VALUES_MAXITEMS)
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                .stackTraceIs(RestErrorModel.STACKTRACE)
                .statusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin doesn't get task form model with non numeric maxItems parameter applied using REST API")
    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL })
    public void getTaskFormModelWithNonNumericMaxItems() throws Exception
    {
        restClient.authenticateUser(adminUser).withParams("maxItems=A").withWorkflowAPI().usingTask(taskModel).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError()
                .containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, "A"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS, TestGroup.FULL, TestGroup.NETWORKS })
    @TestRail(section = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.TASKS },
            executionType = ExecutionType.REGRESSION, description = "Verify network admin user gets all task form models inside his network with Rest API and response is successful (200)")
    public void networkAdminGetsTaskFormModels() throws Exception
    {
        UserModel adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUser).usingTenant().createTenant(adminTenantUser);
        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant1");

        RestProcessModel networkProcess = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .addProcess("activitiReview", tenantUser, false, CMISUtil.Priority.High);
        RestTaskModel networkTask = restClient.authenticateUser(adminTenantUser).withWorkflowAPI()
                .usingProcess(networkProcess).getProcessTasks().getOneRandomEntry().onModel();

        restClient.authenticateUser(adminUser).withWorkflowAPI().usingTask(networkTask).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);

        returnedCollection = restClient.authenticateUser(adminTenantUser).withWorkflowAPI().usingTask(networkTask).getTaskFormModel();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat().entriesListIsNotEmpty();
    }
}
