package org.alfresco.rest.tags.put;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class UpdateTagFullTests extends RestTest
{
    private UserModel adminUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private RestTagModel oldTag;
    private DataUser.ListUserWithRoles usersWithRoles;
    private RestTagModel returnedModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @BeforeMethod(alwaysRun=true)
    public void addTagToDocument() throws Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        oldTag = restClient.withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("old"));
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify Manager user can provide large string for new tag value.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    @Bug(id="REPO-1828")
    public void managerIsAbleToUpdateTagsProvideLargeStringTag() throws JsonToModelConversionException, Exception
    {
        String largeStringTag = RandomStringUtils.randomAlphanumeric(10000);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        returnedModel = restClient.withCoreAPI().usingTag(oldTag).update(largeStringTag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("tag").is(largeStringTag);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify Manager user can provide short string for new tag value.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    @Bug(id="REPO-1828")
    public void managerIsAbleToUpdateTagsProvideShortStringTag() throws JsonToModelConversionException, Exception
    {
        String shortStringTag = RandomStringUtils.randomAlphanumeric(2);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        returnedModel = restClient.withCoreAPI().usingTag(oldTag).update(shortStringTag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("tag").is(shortStringTag);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify Manager user can provide string with special chars for new tag value.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    @Bug(id="REPO-1828")
    public void managerIsAbleToUpdateTagsProvideSpecialCharsStringTag() throws JsonToModelConversionException, Exception
    {
        String specialCharsString = "!@#$%^&*()'\".,<>-_+=|\\";
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        returnedModel = restClient.withCoreAPI().usingTag(oldTag).update(specialCharsString);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("tag").is(specialCharsString);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify Admin user can provide existing tag for new tag value.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    @Bug(id="REPO-1828")
    public void adminIsAbleToUpdateTagsProvideExistingTag() throws JsonToModelConversionException, Exception
    {
        String existingTag = "oldTag";
        RestTagModel oldExistingTag = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                .withCoreAPI().usingResource(document).addTag(existingTag);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.authenticateUser(adminUserModel);
        returnedModel = restClient.withCoreAPI().usingTag(oldExistingTag).update(existingTag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("tag").is(existingTag);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify Admin user can delete a tag, add tag and update it.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    @Bug(id="REPO-1828")
    public void withAdminDeleteTagAddTagUpdateTag() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(document).deleteTag(oldTag);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        String newTag = "addTag";
        RestTagModel newTagModel = restClient.withCoreAPI().usingResource(document).addTag(newTag);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        returnedModel = restClient.withCoreAPI().usingTag(newTagModel).update(newTag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("tag").is(newTag);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify Admin user can update a tag, delete tag and add it.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    @Bug(id="REPO-1828")
    public void withAdminUpdateTagDeleteTagAddTag() throws JsonToModelConversionException, Exception
    {     
        String newTag = "addTag";
        
        returnedModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingTag(oldTag).update(newTag);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedModel.assertThat().field("tag").is(newTag);
        
        restClient.withCoreAPI().usingResource(document).deleteTag(returnedModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingResource(document).addTag(newTag);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }
}
