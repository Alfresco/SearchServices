package org.alfresco.rest.tags.nodes;

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
import org.testng.annotations.Test;

public class DeleteTagFullTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private DataUser.ListUserWithRoles usersWithRoles;
    private RestTagModel tagReturnedModel, returnedTag;
    private FileModel document;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.REGRESSION, description = "Verify Manager user can't delete deleted tag.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    @Bug(id = "ACE-5455")
    public void managerCannotDeleteDeletedTag() throws JsonToModelConversionException, Exception
    {
        tagReturnedModel = restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag"));

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(document).deleteTag(tagReturnedModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingResource(document).deleteTag(tagReturnedModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.REGRESSION, description = "Verify Collaborator user can delete long tag.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void userCollaboratorCanDeleteLongTag() throws JsonToModelConversionException, Exception
    {
        String longTag = RandomStringUtils.randomAlphanumeric(5000);
          
        tagReturnedModel = restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(document).addTag(longTag);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withCoreAPI().usingResource(document).deleteTag(tagReturnedModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.REGRESSION, description = "Verify Manager user can delete short tag.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void managerCanDeleteShortTag() throws JsonToModelConversionException, Exception
    {
        String shortTag = RandomStringUtils.randomAlphanumeric(10);
        
        tagReturnedModel = restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(document).addTag(shortTag);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(document).deleteTag(tagReturnedModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.REGRESSION, description = "Verify Admin can delete tag then add it again.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void adminRemovesTagAndAddsItAgain() throws JsonToModelConversionException, Exception
    {
        String tag = RandomStringUtils.randomAlphanumeric(10);
        
        tagReturnedModel = restClient.authenticateUser(adminUserModel)
            .withCoreAPI().usingResource(document).addTag(tag);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(document).deleteTag(tagReturnedModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        tagReturnedModel = restClient.authenticateUser(adminUserModel)
                .withCoreAPI().usingResource(document).addTag(tag);
        returnedTag = restClient.withCoreAPI().getTag(tagReturnedModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedTag.assertThat().field("tag").is(tag.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS },
            executionType = ExecutionType.REGRESSION, description = "Verify Manager user can delete tag added by another user.")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.FULL })
    public void managerCanDeleteTagAddedByAnotherUser() throws JsonToModelConversionException, Exception
    {
        String tag = RandomStringUtils.randomAlphanumeric(10);
        
        tagReturnedModel = restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
            .withCoreAPI().usingResource(document).addTag(tag);

        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingResource(document).deleteTag(tagReturnedModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }   
}
