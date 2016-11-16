package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 10/10/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.SANITY })
public class AddCommentsSanityTests extends RestTest
{     
    private UserModel adminUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private DataUser.ListUserWithRoles usersWithRoles;
    private String comment1, comment2;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @BeforeMethod(alwaysRun = true)
    public void generateRandomComments()
    {
        comment1 = RandomData.getRandomName("comment1");
        comment2 = RandomData.getRandomName("comment2");
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify admin user adds multiple comments with Rest API and status code is 201")
    public void adminIsAbleToAddComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel)
                  .onCoreAPI().usingResource(document).addComments(comment1, comment2)
                  .assertThat().entriesListIsNotEmpty()
                  .and().entriesListContains("content", comment1)
                  .and().entriesListContains("content", comment2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Manager user adds multiple comments with Rest API and status code is 201")
    public void managerIsAbleToAddComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                  .onCoreAPI().usingResource(document).addComments(comment1, comment2)
                  .assertThat().entriesListIsNotEmpty()
                  .and().entriesListContains("content", comment1)
                  .and().entriesListContains("content", comment2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Contributor user adds multiple comments with Rest API and status code is 201")
    public void contributorIsAbleToAddComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
                .onCoreAPI().usingResource(document).addComments(comment1, comment2)
                .assertThat().entriesListIsNotEmpty()
                .and().entriesListContains("content", comment1)
                .and().entriesListContains("content", comment2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Collaborator user adds multiple comments with Rest API and status code is 201")
    public void collaboratorIsAbleToAddComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                   .onCoreAPI().usingResource(document).addComments(comment1, comment2)
                   .assertThat().paginationExist().and().entriesListIsNotEmpty()
                   .and().entriesListContains("content", comment1)
                   .and().entriesListContains("content", comment2);
                      
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify Consumer user adds multiple comments with Rest API and status code is 201")
    public void consumerIsAbleToAddComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .onCoreAPI().usingResource(document).addComments(comment1, comment2);
        
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.COMMENTS }, executionType = ExecutionType.SANITY, description = "Verify unauthenticated user gets status code 401 on post multiple comments call")
    @Bug(id="MNT-16904")
    public void unauthenticatedUserIsNotAbleToAddComments() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password"))
                  .onCoreAPI().usingResource(document).addComments(comment1, comment2);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}
