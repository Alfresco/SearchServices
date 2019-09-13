package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.search.TestGroup;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.testng.Assert.assertTrue;

public class ExplicitRouting  extends AbstractE2EFunctionalTest {


    /**
     * Checks indexing still works after sharding model used for explicit routing has been disabled
     * @throws Exception
     */
    @Test(priority = 1, groups = {TestGroup.NOT_BAMBOO, TestGroup.EXPLICIT_SHARDING })
    public void testIndexingStillWorkingAfterShardModelIsDeactivated() throws Exception
    {

        // Deploy sharding model
        assertTrue(deployCustomModel("model/sharding-content-model.xml"),
                "failing while deploying sharding model");

        // Create a first child in parent folder. It will be indexed in the parent shard (shard 0)
        FileModel file = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        Map<String, Object> propertiesFirstChild = Map.of(PropertyIds.NAME, file.getName(),
                PropertyIds.OBJECT_TYPE_ID, "cmis:document",
                "cmis:secondaryObjectTypeIds", List.of("P:shard:sharding"),
                "shard:shardId", "0");

        // Create file using shard:shardId
        cmisApi.authenticateUser(testUser).usingSite(testSite)
                .createFile(file,
                        Map.of(PropertyIds.NAME, file.getName(),
                                PropertyIds.OBJECT_TYPE_ID, "cmis:document"),
                        VersioningState.MAJOR)
                .assertThat().existsInRepo();

        // Wait for file to be indexed
        assertTrue(waitForMetadataIndexing(file.getName(), true),
                "A file using sharding model has not been indexed");


        // Deleting file
        dataContent.usingSite(testSite).usingUser(testUser).usingResource(file).deleteContent();
        restClient.withCoreAPI().usingTrashcan().deleteNodeFromTrashcan(file);

        // Deleting sharding model
        assertTrue(deactivateCustomModel("sharding-content-model.xml"),
                "failing while deactivating sharding model");
        assertTrue(deleteCustomModel("sharding-content-model.xml"),
                "failing while removing sharding model");

        assertTrue(waitForIndexing("TYPE:'" + "shard:shardId" + "'", false),
                "Indexes are not updated after deactivating a model");

        // Create a file in the parent folder
        file = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");

        cmisApi.authenticateUser(testUser).usingSite(testSite)
                .createFile(file,
                        Map.of(PropertyIds.NAME, file.getName(),
                                PropertyIds.OBJECT_TYPE_ID, "cmis:document"),
                        VersioningState.MAJOR)
                .assertThat().existsInRepo();


        assertTrue(waitForMetadataIndexing(file.getName(), true),
                "Indexing is not working after the sharding model has been removed");

    }
}
