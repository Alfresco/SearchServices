package org.alfresco.rest.targets;

import static org.alfresco.utility.report.log.Step.STEP;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestAssocTargetModel;
import org.alfresco.rest.model.RestNodeParentChildModelCollection;
import org.alfresco.rest.model.builder.NodesBuilder;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetTargetSanityTests extends RestTest
{

    private UserModel adminUserModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
    }

    /**
     * Sanity check for the following api endpoints 
     * POST /nodes/{nodeId}/targets
     * GET /nodes/{nodeId}/targets 
     * DELETE /nodes/{nodeId}/targets/{targetId}
     */

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.NODES }, executionType = ExecutionType.SANITY, description = "Check /targets (create, list, delete) api calls")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.SANITY })
    public void checkTargetsNodeApi() throws Exception
    {
        STEP("1.Create a folder hierarchy folder1/folder2, with folder2 containing 3 files: f1, f2, and f3");
        NodesBuilder nodesBuilder = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingNode(ContentModel.my()).defineNodes();
        nodesBuilder.folder("F1").folder("F2").folder("F3").file("f1").file("f2").file("f3");

        STEP("2. Create target associations model objects");
        RestAssocTargetModel assocDocTarget1 = new RestAssocTargetModel(nodesBuilder.getNode("f2").toContentModel().getNodeRef(), "cm:references");
        RestAssocTargetModel assocDocTarget2 = new RestAssocTargetModel(nodesBuilder.getNode("f3").toContentModel().getNodeRef(), "cm:references");

        STEP("3. Create target  associations using POST /nodes/{nodeId}/targets");
        restClient.authenticateUser(adminUserModel);
        restClient.withCoreAPI().usingResource(nodesBuilder.getNode("f1").toContentModel()).createTargetForNode(assocDocTarget1);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingResource(nodesBuilder.getNode("f1").toContentModel()).createTargetForNode(assocDocTarget2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        STEP("4. Check using GET /nodes/{nodeId}/targets targets associations were created");
        RestNodeParentChildModelCollection tagets = restClient.withParams("where=(assocType='cm:references')").withCoreAPI()
                .usingResource(nodesBuilder.getNode("f1").toContentModel()).getNodeTargets();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        tagets.assertThat().entriesListCountIs(2);

        STEP("5. Check using DELETE /nodes/{nodeId}/targets/{targetId} that a target can be deleted");
        restClient.authenticateUser(adminUserModel);
        restClient.withCoreAPI().usingResource(nodesBuilder.getNode("f1").toContentModel()).deleteTarget(assocDocTarget1);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        STEP("6. Check using GET /nodes/{nodeId}/targets that target association was deleted");
        RestNodeParentChildModelCollection targetsRes = restClient.withParams("where=(assocType='cm:references')").withCoreAPI()
                .usingResource(nodesBuilder.getNode("f1").toContentModel()).getNodeTargets();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        targetsRes.assertThat().entriesListCountIs(1);
    }

}
