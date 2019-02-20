/*
 * Copyright 2019 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.service.search.e2e.searchservices.tracker;

import java.util.HashMap;
import java.util.Map;

import org.alfresco.service.search.AbstractSearchServiceE2E;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.TestGroup;
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
public class CascadingTrackerIntegrationTest extends AbstractSearchServiceE2E
{
    @Autowired
    protected DataContent dataContent;
    
    private FolderModel parentFolder, grandParentFolder, childFolder;
    private FileModel childFile, grandChildFile;
    
    @Test(priority = 1, groups = { TestGroup.ASS_13 })
    public void testChildrenWhenParentRenamed() throws Exception
    {
        // Create PArent folder
        parentFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        // Create a file in the parent folder
        childFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");

        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, childFile.getName());
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(parentFolder)
            .createFile(childFile, properties, VersioningState.MAJOR).assertThat().existsInRepo();
        
        // Query to find nodes where Path with original folder name matches
        String parentQuery = "NPATH:\"4/Company Home/Sites/" + testSite.getTitle() + "/documentLibrary/" + parentFolder.getName() + "\"";

        // Rename parent folder
        String parentNewName = "parentRenamed";
        parentFolder.setName(parentNewName);

        ContentModel parentNewNameModel = new ContentModel(parentNewName);
        dataContent.usingUser(testUser).usingResource(parentFolder).renameContent(parentNewNameModel);

        waitForMetadataIndexing(parentNewName, true);

        // Find nodes where Path with new folder name matches
        String parentQueryAfterRename = "NPATH:\"4/Company Home/Sites/" + testSite.getTitle() + "/documentLibrary/" + parentNewName + "\"";
        waitForContent(parentQueryAfterRename, childFile.getName(), true);

        // Query using new parent name
        int descendantCountOfNewName = query(parentQueryAfterRename).getPagination().getCount();
        Assert.assertEquals(descendantCountOfNewName, 2, "New renamed path has not the same descendants as before renaming: " + parentQueryAfterRename);

        // Query using old parent name
        int descendantCountOfOriginalName = query(parentQuery).getPagination().getCount();
        Assert.assertEquals(descendantCountOfOriginalName, 0, "Old path still has descendants: " + parentQuery);
    }

    @Test(priority = 2, groups = { TestGroup.ASS_13 })
    public void testChildrenWhenGrandParentRenamed() throws Exception
    {
        // Create grand-parent folder
        grandParentFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        // Create child folder
        childFolder = dataContent.usingUser(testUser).usingResource(grandParentFolder).createFolder();
        
        // Create grand-child file
        grandChildFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, grandChildFile.getName());
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(childFolder)
            .createFile(grandChildFile, properties, VersioningState.MAJOR).assertThat().existsInRepo();

        // Wait for file to be indexed
        waitForMetadataIndexing(grandChildFile.getName(), true);
        
        // Query to find nodes where Path with original folder name matches
        String parentQuery = "NPATH:\"4/Company Home/Sites/" + testSite.getTitle() + "/documentLibrary/" + grandParentFolder.getName() + "\"";

        // Rename grand parent folder
        String grandParentNewName = "grandParentRenamed";
        grandParentFolder.setName(grandParentNewName);

        ContentModel granParentNewNameModel = new ContentModel(grandParentNewName);
        this.dataContent.usingUser(testUser).usingResource(grandParentFolder).renameContent(granParentNewNameModel);
        
        // Find nodes where Path with new folder name matches
        String parentQueryAfterRename = "NPATH:\"4/Company Home/Sites/" + testSite.getTitle() + "/documentLibrary/" + grandParentNewName + "\"";
        waitForContent(parentQueryAfterRename, grandChildFile.getName(), true);

        // Query using new parent name
        int descendantCountOfNewName = query(parentQueryAfterRename).getPagination().getCount();
        Assert.assertEquals(descendantCountOfNewName, 3, "New renamed path has not the same descendants as before renaming: " + parentQueryAfterRename);

        // Query using old parent name
        int descendantCountOfOriginalName = query(parentQuery).getPagination().getCount();
        Assert.assertEquals(descendantCountOfOriginalName, 0, "Old path still has descendants: " + parentQuery);
    }
}

