/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.test.search.functional.searchServices.search.tracker;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.search.TestGroup;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Test class tests cascading updates for a child node when parent node is updated
 * 
 * @author Alessandro Benedetti
 * @author Meenal Bhave
 */
public class CascadingTrackerIntegrationTest extends AbstractE2EFunctionalTest
{
    @Autowired
    protected DataContent dataContent;

    @Test(priority = 1)
    public void testChildPathWhenParentRenamed() throws Exception
    {
        // Create Parent folder
        FolderModel parentFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        // Create a file in the parent folder
        FileModel childFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, childFile.getName());
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(parentFolder)
            .createFile(childFile, properties, VersioningState.MAJOR).assertThat().existsInRepo();
        
        // Query to find nodes where Path with original folder name matches
        String parentQuery = "PATH:\"/app:company_home/st:sites/cm:" + testSite.getTitle() +
                "/cm:documentLibrary/cm:" + parentFolder.getName() + "/*\"";

        // Rename parent folder
        String parentNewName = "parentRenamed";
        parentFolder.setName(parentNewName);

        ContentModel parentNewNameModel = new ContentModel(parentNewName);
        dataContent.usingUser(testUser).usingResource(parentFolder).renameContent(parentNewNameModel);

        waitForMetadataIndexing(parentNewName, true);

        // Find nodes where Path with new folder name matches
        String parentQueryAfterRename =  "PATH:\"/app:company_home/st:sites/cm:" + testSite.getTitle() +
                "/cm:documentLibrary/cm:" + parentNewName + "/*\"";
        boolean indexingInProgress = !isContentInSearchResults(parentQueryAfterRename, childFile.getName(), true);

        // Query using new parent name: Expect parent folder and child file
        int descendantCountOfNewName = query(parentQueryAfterRename).getPagination().getCount();
        Assert.assertEquals(descendantCountOfNewName, 1, String.format("Indexing in progress: %s New renamed path has not the same descendants as before renaming: %s", indexingInProgress, parentQueryAfterRename));

        // Query using old parent name: Expect no descendant after rename
        int descendantCountOfOriginalName = query(parentQuery).getPagination().getCount();
        Assert.assertEquals(descendantCountOfOriginalName, 0, "Old path still has descendants: " + parentQuery);
    }

    @Test(priority = 2)
    public void testGrandChildPathWhenGrandParentRenamed() throws Exception
    {
        // Create grand parent folder
        FolderModel grandParentFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        // Create child folder
        FolderModel childFolder = dataContent.usingUser(testUser).usingResource(grandParentFolder).createFolder();
        
        // Create grand child file
        FileModel grandChildFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, grandChildFile.getName());
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(childFolder)
            .createFile(grandChildFile, properties, VersioningState.MAJOR).assertThat().existsInRepo();

        // Wait for file to be indexed
        waitForMetadataIndexing(grandChildFile.getName(), true);
        
        // Query to find nodes where Path with original folder name matches

        String parentQuery = "PATH:\"/app:company_home/st:sites/cm:" + testSite.getTitle() +
                "/cm:documentLibrary/cm:" + grandParentFolder.getName() + "/*\"";


        // Rename grand parent folder
        String grandParentNewName = "grandParentRenamed";
        grandParentFolder.setName(grandParentNewName);

        ContentModel grandParentFolderRenamed = new ContentModel(grandParentNewName);
        dataContent.usingUser(testUser).usingResource(grandParentFolder).renameContent(grandParentFolderRenamed);
        
        // Find nodes where Path with new folder name matches
        String childrenQueryAfterRename = "PATH:\"/app:company_home/st:sites/cm:" + testSite.getTitle() +
                "/cm:documentLibrary/cm:" + grandParentNewName + "/*\"";

        String grandChildrenQueryAfterRename = "PATH:\"app:company_home/st:sites/cm:" + testSite.getTitle() +
                "/cm:documentLibrary/cm:" + grandParentNewName + "/cm:" + childFolder.getName() + "/*\"";

        boolean indexingInProgress = !isContentInSearchResults(grandChildrenQueryAfterRename, grandChildFile.getName(), true);

        // Query using new parent name: Expect grandchild file
        int grandChildrenCountOfNewName = query(grandChildrenQueryAfterRename).getPagination().getCount();
        Assert.assertEquals(grandChildrenCountOfNewName, 1,
                String.format("Indexing in progress: %s New renamed path has not the same descendants as before renaming: %s",
                        indexingInProgress,
                        grandChildrenQueryAfterRename));

        // Query using new parent name: Expect child folder
        int childrenCountOfNewName = query(childrenQueryAfterRename).getPagination().getCount();
        Assert.assertEquals(childrenCountOfNewName, 1,
                String.format("Indexing in progress: %s New renamed path has not the same descendants as before renaming: %s",
                        indexingInProgress,
                        childrenQueryAfterRename));

        // Query using old parent name: Expect no descendant after rename
        int descendantCountOfOriginalName = query(parentQuery).getPagination().getCount();
        Assert.assertEquals(descendantCountOfOriginalName, 0, "Old path still has descendants: " + parentQuery);
    }

    /**
     * Index three nodes (parent folder and two files) in two different shards.
     * Check that, after parent renaming, both the children are searchable in the new path
     * (computed accordingly with the new parent folder name)
     */
    @Test(priority = 1, groups = {TestGroup.NOT_BAMBOO, TestGroup.EXPLICIT_SHARDING })
    public void testChildrenPathOnParentRenamedWithChildrenInDifferentShards() throws Exception{


        // Create Parent folder. It will be indexed in shard 0
        FolderModel parentFolder = FolderModel.getRandomFolderModel();
        Map<String, Object> parentProperties = new HashMap<>();
        List<String> secondaryTypes = new ArrayList();
        secondaryTypes.add("P:shard:sharding");
        secondaryTypes.add("P:cm:titled");
        parentProperties.put(PropertyIds.NAME, parentFolder.getName());
        parentProperties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:folder");
        parentProperties.put("cmis:secondaryObjectTypeIds", secondaryTypes);
        parentProperties.put("shard:shardId", "0");

        // Create a first child in parent folder. It will be indexed in the parent shard (shard 0)
        FileModel firstChildFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");

        Map<String, Object> propertiesFirstChild = new HashMap<>();
        propertiesFirstChild.put(PropertyIds.NAME, firstChildFile.getName());
        propertiesFirstChild.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        propertiesFirstChild.put("cmis:secondaryObjectTypeIds", secondaryTypes);
        propertiesFirstChild.put("shard:shardId", "0");

        // Create a second child in parent folder. It will be indexed in shard 1.
        FileModel secondChildFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");

        Map<String, Object> propertiesSecondChild = new HashMap<>();
        propertiesSecondChild.put(PropertyIds.NAME, secondChildFile.getName());
        propertiesSecondChild.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        propertiesSecondChild.put("cmis:secondaryObjectTypeIds", secondaryTypes);
        propertiesSecondChild.put("shard:shardId", "1");

        cmisApi.authenticateUser(testUser).usingSite(testSite).createFolder(parentFolder, parentProperties).then()
                .usingResource(parentFolder)
                .createFile(firstChildFile, propertiesFirstChild, VersioningState.MAJOR)
                .createFile(secondChildFile, propertiesSecondChild, VersioningState.MAJOR);

        // Check everything is indexed
        Assert.assertTrue(waitForIndexing(firstChildFile.getName(), true), "file: " + firstChildFile.getName() + " has not been indexed.");
        Assert.assertTrue(waitForIndexing(secondChildFile.getName(), true), "file: " + secondChildFile.getName() + " has not been indexed.");
        Assert.assertTrue(waitForIndexing(parentFolder.getName(), true), "file: " + parentFolder.getName() + " has not been indexed.");

        // Query to find nodes where Path with original folder name matches
        String parentQuery = "PATH:\"/app:company_home/st:sites/cm:" + testSite.getTitle() +
                "/cm:documentLibrary/cm:" + parentFolder.getName() + "/*\"";

        // Rename parent folder
        String parentNewName = "parentRenamedSharding";
        parentFolder.setName(parentNewName);
        ContentModel parentNewNameModel = new ContentModel(parentNewName);
        dataContent.usingUser(testUser).usingResource(parentFolder).renameContent(parentNewNameModel);

        String parentQueryAfterRename =  "PATH:\"/app:company_home/st:sites/cm:" + testSite.getTitle() +
                "/cm:documentLibrary/cm:" + parentNewName + "/*\"";

        Assert.assertTrue(waitForMetadataIndexing(parentNewName, true), "failing while renaming " + parentFolder.getName() + " to " + parentNewName);

        boolean indexingInProgress = !isContentInSearchResults(parentQueryAfterRename, firstChildFile.getName(), true);

        // Query using new parent name: Expect parent folder and child file
        int descendantCountOfNewName = query(parentQueryAfterRename).getPagination().getCount();
        Assert.assertEquals(descendantCountOfNewName, 2, String.format("Indexing in progress: %s New renamed path has not the same descendants as before renaming: %s", indexingInProgress, parentQuery));

        // Query using old parent name: Expect no descendant after rename
        int descendantCountOfOriginalName = query(parentQuery).getPagination().getCount();
        Assert.assertEquals(descendantCountOfOriginalName, 0, "Old path still has descendants: " + parentQuery);
    }
}

