/*
 * Copyright 2018 Alfresco Software, Ltd. All rights reserved.
 * License rights for this program may be obtained from Alfresco Software, Ltd.
 * pursuant to a written agreement and any use of this program without such an
 * agreement is prohibited.
 */
package org.alfresco.service.search.e2e.searchservices.tracker;

import org.alfresco.service.search.AbstractSearchServiceE2E;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.junit.Assert;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.core.Is.is;

public class CascadingTrackerIntegrationTest extends AbstractSearchServiceE2E
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;

    private SiteModel testSite;

    private UserModel testUser;
    
    private FolderModel testFolder;

    @BeforeClass(alwaysRun = true)
    public void setupEnvironment() throws Exception
    {
        serverHealth.assertServerIsOnline();
        
        testSite = dataSite.createPublicRandomSite();
        testUser = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);
    }
    
    @Test(groups = { TestGroup.ASS_13 })
    public void testCascadingTracking_parentFolderRenaming_shouldReIndexChildren() throws Exception
    {
        testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        FileModel customFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, customFile.getName());
        
        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
            .createFile(customFile, properties, VersioningState.MAJOR).assertThat().existsInRepo();
        
        waitForIndexing(customFile.getName(), true);
        
        String parentQuery = "NPATH:\"4/Company Home/Sites/" + testSite.getTitle() + "/documentLibrary/" + testFolder.getName() + "\"";
        int initialDescendantCount = query(parentQuery).getPagination().getCount();
        
        String parentNewName = "parentRenamed";
        ContentModel parentNewNameModel = new ContentModel(parentNewName);

        this.dataContent.usingUser(testUser).usingResource(testFolder).renameContent(parentNewNameModel);

        testFolder.setName(parentNewName);
        waitForIndexing(testFolder.getName(), true);

        String parentQueryAfterRename = "NPATH:\"4/Company Home/Sites/" + testSite.getTitle() + "/documentLibrary/" + parentNewName + "\"";
        int descendantCountOfDismissedName = query(parentQuery).getPagination().getCount();
        int descendantCountOfNewName = query(parentQueryAfterRename).getPagination().getCount();

        Assert.assertThat("New renamed path has not the same descendants as before renaming: " + parentQueryAfterRename,descendantCountOfNewName,is(initialDescendantCount));
        Assert.assertThat("Old path still has descendants: " + parentQuery,descendantCountOfDismissedName,is(0));
    }

    @Test(groups = { TestGroup.ASS_13 })
    public void testCascadingTracking_granParentFolderRenaming_shouldReIndexChildren() throws Exception
    {
        testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        // Create child folder
        FolderModel childFolder = dataContent.usingUser(testUser).usingResource(testFolder).createFolder();
        
        // Create grandchild file
        FileModel customFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "custom content");
        Map<String, Object> properties = new HashMap<>();
        properties.put(PropertyIds.NAME, customFile.getName());
        
        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(childFolder)
            .createFile(customFile, properties, VersioningState.MAJOR).assertThat().existsInRepo();
        waitForIndexing(customFile.getName(), true);
        
        String parentQuery = "NPATH:\"4/Company Home/Sites/" + testSite.getTitle() + "/documentLibrary/" + testFolder.getName() + "\"";
        int initialDescendantCount = query(parentQuery).getPagination().getCount();

        // Edit grand parent folder name
        String granParentNewName = "granParentRenamed";
        ContentModel granParentNewNameModel = new ContentModel(granParentNewName);
        this.dataContent.usingUser(testUser).usingResource(testFolder).renameContent(granParentNewNameModel);

        waitForIndexing(granParentNewName, true);
        
        String parentQueryAfterRename = "NPATH:\"4/Company Home/Sites/" + testSite.getTitle() + "/documentLibrary/" + granParentNewName + "\"";
        int descendantCountOfDismissedName = query(parentQuery).getPagination().getCount();
        int descendantCountOfNewName = query(parentQueryAfterRename).getPagination().getCount();

        Assert.assertThat("New renamed path has not the same descendants as before renaming: " + parentQueryAfterRename,descendantCountOfNewName,is(initialDescendantCount));
        Assert.assertThat("Old path still has descendants: " + parentQuery,descendantCountOfDismissedName,is(0));
    }
}

