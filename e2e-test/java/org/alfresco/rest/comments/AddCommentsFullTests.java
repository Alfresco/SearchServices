package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.JsonBodyGenerator;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.RestCommentModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang.RandomStringUtils;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Andrei Rusu
 */
public class AddCommentsFullTests extends RestTest
{
    private UserModel adminUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private DataUser.ListUserWithRoles usersWithRoles;
    private String comment = "This is a new comment";
    private String comment2 = "This is the second comment";
    private RestCommentModelsCollection comments;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite();        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.FULL, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Using Manager user verify that you can provide a large string for one comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addLongCommentsWithManagerAndCheckThatCommentIsReturned() throws Exception
    {
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        String longString = RandomStringUtils.randomAlphanumeric(10000);
        String longString1 = RandomStringUtils.randomAlphanumeric(90000);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
        .withCoreAPI().usingResource(document).addComments(longString, longString1);                  
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", longString);
        comments.assertThat().entriesListContains("content", longString1);
        comments.assertThat().paginationField("totalItems").is("2");
        comments.assertThat().paginationField("count").is("2");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.FULL, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Using Manager user verify that you can provide a short string for one comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addShortCommentsWithManagerAndCheckThatCommentIsReturned() throws Exception
    {
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        String shortString = RandomStringUtils.randomAlphanumeric(2);
        String shortString1 = RandomStringUtils.randomAlphanumeric(1);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
        .withCoreAPI().usingResource(document).addComments(shortString, shortString1);                  
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", shortString);
        comments.assertThat().entriesListContains("content", shortString1);
        comments.assertThat().paginationField("totalItems").is("2");
        comments.assertThat().paginationField("count").is("2");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.FULL, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Using Collaborator user verify that you can provide a string with special characters for one comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addCommentsWithSpecialCharsWithCollaboratorCheckCommentIsReturned() throws Exception
    {
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        String specialCharsString = "!@#$%^&*()'\".,<>-_+=|\\";
        String shortString = RandomStringUtils.randomAlphanumeric(2);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
        .withCoreAPI().usingResource(document).addComments(specialCharsString, shortString);                  
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", specialCharsString);  
        comments.assertThat().entriesListContains("content", shortString);
        comments.assertThat().paginationField("totalItems").is("2");
        comments.assertThat().paginationField("count").is("2");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.FULL, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Using Manager user verify that you can not provide an empty string for one comment")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addEmptyStringCommentsWithManagerCheckCommentIsReturned() throws Exception
    {
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        String emptyString = "";
        String spaceString = " ";
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
        .withCoreAPI().usingResource(document).addComments(emptyString, spaceString);                  
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(RestErrorModel.NON_NULL_COMMENT);
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.FULL, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Using Collaborator user verify that you can provide several comments in one request")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void addSeveralCommentsWithCollaboratorCheckCommentsAreReturned() throws Exception
    {
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        String charString = RandomStringUtils.randomAlphanumeric(10);
        String charString1 = RandomStringUtils.randomAlphanumeric(10);
        String charString2 = RandomStringUtils.randomAlphanumeric(10);
        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
        .withCoreAPI().usingResource(document).addComments(comment, comment2, charString, charString1, charString2);                 
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        comments = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", comment);
        comments.assertThat().paginationField("totalItems").is("5");
        comments.assertThat().paginationField("count").is("5");
    }
    
    @TestRail(section={TestGroup.REST_API, TestGroup.FULL, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Provide invalid request body parameter and check default error model schema")
    @Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.FULL })
    public void invalidRequestBodyParameterCheckErrorModelSchema() throws Exception
    {
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        
        restClient.authenticateUser(adminUserModel).withCoreAPI();
        String postBody = JsonBodyGenerator.keyValueJson("content2", comment);
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, postBody, "nodes/{nodeId}/comments", document.getNodeRef());
        restClient.processModel(RestCommentModel.class, request);
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.assertLastError().getErrorKey().contains("Unrecognized field \"content2\"");
        restClient.assertLastError().containsSummary("Unrecognized field \"content2\"");
        restClient.assertLastError().getDescriptionURL().contains("https://api-explorer.alfresco.com");
        restClient.assertLastError().getStackTrace().contains("For security reasons the stack trace is no longer displayed, but the property is kept for previous versions");
    }
}
