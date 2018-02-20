package org.alfresco.rest.actions;

import static org.alfresco.utility.report.log.Step.STEP;
import static org.testng.Assert.assertFalse;

import java.util.Arrays;
import java.util.List;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestActionDefinitionModel;
import org.alfresco.rest.model.RestActionDefinitionModelsCollection;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.model.RestTagModelsCollection;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.json.JSONObject;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class ActionsTests extends RestTest
{
    private UserModel adminUser;
    private FileModel document;
    private SiteModel publicSite;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        publicSite = dataSite.createPublicRandomSite();
        document = dataContent.usingSite(publicSite).usingUser(adminUser).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.ACTIONS }, executionType = ExecutionType.SANITY, description = "Verify actions")
    @Test(groups = { TestGroup.REST_API, TestGroup.ACTIONS, TestGroup.SANITY })
    public void testActionDefinitions() throws Exception
    {
        restClient.authenticateUser(dataContent.getAdminUser());

        RestActionDefinitionModelsCollection restActionDefinitions = restClient.withCoreAPI().usingActions().listActionDefinitions();

        restClient.assertStatusCodeIs(HttpStatus.OK);
        assertFalse(restActionDefinitions.isEmpty());
        restActionDefinitions.assertThat().entriesListContains("name", "copy").and().entriesListContains("name", "move").and().entriesListContains("name", "check-out").and()
                .entriesListContains("name", "check-in");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.ACTIONS }, executionType = ExecutionType.REGRESSION, description = "Verify actions error conditions")
    @Test(groups = { TestGroup.REST_API, TestGroup.ACTIONS, TestGroup.REGRESSION })
    public void testActionDefinitionsNegative() throws Exception
    {
        // Badly formed request -> 400
        {
            restClient.authenticateUser(dataContent.getAdminUser()).
            // invalid skipCount
                    withParams("skipCount=-1").withCoreAPI().usingActions().listActionDefinitions();

            restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);

        }

        // Unauthorized -> 401
        {

            UserModel userUnauthorized = new UserModel("invalid-user", "invalid-pasword");
            restClient.authenticateUser(userUnauthorized).withCoreAPI().usingActions().listActionDefinitions();

            restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
        }
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.ACTIONS }, executionType = ExecutionType.SANITY, description = "Sanity test for POST /action-executions")
    @Test(groups = { TestGroup.REST_API, TestGroup.ACTIONS, TestGroup.SANITY })
    public void executeAction() throws Exception
    {
        JSONObject response = restClient.authenticateUser(adminUser).withCoreAPI().usingActions().executeAction("add-features", document, "aspect-name", "cm:versionable");
        restClient.assertStatusCodeIs(HttpStatus.ACCEPTED);

        assertFalse(response.getString("id").isEmpty());

        /*
         * Get all node properties and check that action was executed and cm:versionable
         * aspect was added
         */
        Utility.sleep(500, 20000, () -> {
            RestNodeModel fileModel = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(document).getNode();

            restClient.assertStatusCodeIs(HttpStatus.OK);
            fileModel.assertThat().field("aspectNames").contains("cm:versionable");
        });
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.ACTIONS }, executionType = ExecutionType.SANITY, description = "Sanity test for ACTIONS endpoint GET action-definitions/{actionDefinitionId}")
    @Test(groups = { TestGroup.REST_API, TestGroup.ACTIONS, TestGroup.SANITY })
    public void testGetActionDefinitionById() throws Exception
    {
        restClient.authenticateUser(dataContent.getAdminUser());

        RestActionDefinitionModel restActionDefinition = restClient.withCoreAPI().usingActions().getActionDefinitionById("add-features");

        restClient.assertStatusCodeIs(HttpStatus.OK);
        assertFalse(restActionDefinition.getId().isEmpty());
        restActionDefinition.getId().equals("add-features");
        restActionDefinition.getDescription().equals("This will add an aspect to the matched item.");
        restActionDefinition.getTitle().equals("Add aspect");
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.ACTIONS }, executionType = ExecutionType.REGRESSION, description = "Verify actions on Content Metadata Extracter")
    @Test(groups = { TestGroup.REST_API, TestGroup.ACTIONS, TestGroup.REGRESSION, TestGroup.REQUIRE_SOLR })
    public void testContentMetadataExtracter() throws Exception
    {
        STEP("1. Authenticate as a system user.");
        restClient.authenticateUser(dataContent.getAdminUser());

        STEP("2. Create  a folder and files with tags.");
        FolderModel folder = FolderModel.getRandomFolderModel();
        folder = dataContent.usingUser(adminUser).usingSite(publicSite).createFolder(folder);
        // Create files with tags
        restClient.configureRequestSpec().addMultiPart("filedata", Utility.getResourceTestDataFile("quickIPTC.jpg"));
        RestNodeModel fileNode1 = restClient.withCoreAPI().usingNode(folder).createNode();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.configureRequestSpec().addMultiPart("filedata", Utility.getResourceTestDataFile("quickIPTC2.jpg"));
        RestNodeModel fileNode2 = restClient.withCoreAPI().usingNode(folder).createNode();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.configureRequestSpec().addMultiPart("filedata", Utility.getResourceTestDataFile("quickIPTC3.jpg"));
        RestNodeModel fileNode3 = restClient.withCoreAPI().usingNode(folder).createNode();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        STEP("3. Extract and check tags.");
        extractAndCheckTags(fileNode1, Arrays.asList("fox", "dog", "lazy", "jumping"));
        extractAndCheckTags(fileNode2, Arrays.asList("k1", "k2", "k3"));
        extractAndCheckTags(fileNode3, Arrays.asList("keyword1", "keyword2", "keyword3", "keyword4"));
    }

    /**
     * Extract using Action Executer and check the tags from created nodes
     *
     * @param fileNode
     * @param expectedTags
     * @throws Exception
     */
    private void extractAndCheckTags(RestNodeModel fileNode, List<String> expectedTags) throws Exception
    {
        JSONObject response = restClient.authenticateUser(adminUser).withCoreAPI().usingActions().executeAction("extract-metadata", fileNode.getId(), null, null);
        restClient.assertStatusCodeIs(HttpStatus.ACCEPTED);
        // Allow indexing to complete.
        Utility.sleep(1000, 30000, () -> {
            RestTagModelsCollection returnedCollection = restClient.withCoreAPI().usingNode().getNodeTags(fileNode.getId());
            restClient.assertStatusCodeIs(HttpStatus.OK);

            for (String expectedTag : expectedTags)
            {
                returnedCollection.assertThat().entriesListContains("tag", expectedTag);
            }
        });
    }
}
