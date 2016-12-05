package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
public class UpdateSiteMembershipRequestCoreTests extends RestTest
{
    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private SiteModel siteModel;
    private UserModel managerUser;
    private String updatedMessage;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        managerUser = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(managerUser).createModeratedRandomSite();
        updatedMessage = "Please review my request";
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                executionType = ExecutionType.REGRESSION, description = "Verify unauthorized user is not able to update user site membership request")
    public void unauthorizedUserIsNotAbleToUpdateSiteMembershipRequest() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember)
            .withCoreAPI()
            .usingAuthUser()
            .addSiteMembershipRequest(siteModel);
        newMember.setPassword("fakePass");
        restClient.withCoreAPI()
            .usingUser(newMember).updateSiteMembershipRequest(siteModel, updatedMessage);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
            .assertLastError().containsSummary(ErrorModel.AUTHENTICATION_FAILED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                executionType = ExecutionType.REGRESSION, description = "Verify user is able to update its own site membership request using -me-")
    public void usingMeUpdateSiteMembershipRequest() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember)
            .withCoreAPI()
            .usingAuthUser()
            .addSiteMembershipRequest(siteModel);
        restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(siteModel, updatedMessage)
            .assertMembershipRequestMessageIs(updatedMessage)
            .and().field("id").is(siteModel.getId())
            .and().field("modifiedAt").isNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                executionType = ExecutionType.REGRESSION, description = "Verify inexistent user is not able to update its own site membership request")
    public void inexistentUserCannotUpdateSiteMembershipRequest() throws Exception
    {
        UserModel inexistentUser = UserModel.getRandomUserModel();
        restClient.authenticateUser(managerUser)
            .withCoreAPI().usingUser(inexistentUser)
            .updateSiteMembershipRequest(siteModel, updatedMessage);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(ErrorModel.ENTITY_NOT_FOUND, inexistentUser.getUsername()));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                executionType = ExecutionType.REGRESSION, description = "Verify user is not able to update its own site membership request for inexistent site")
    public void userCannotUpdateSiteMembershipRequestForInexistentSite() throws Exception
    {
        SiteModel randomSite = SiteModel.getRandomSiteModel();
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember).withCoreAPI()
            .usingMe()
            .updateSiteMembershipRequest(randomSite, updatedMessage);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(ErrorModel.RELATIONSHIP_NOT_FOUND, newMember.getUsername(), randomSite.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                executionType = ExecutionType.REGRESSION, description = "Verify user is able not to update its own site membership request for public site")
    public void userCannotUpdateSiteMembershipRequestForPublicSite() throws Exception
    {
        SiteModel publicSite = dataSite.usingUser(managerUser).createPublicRandomSite();
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember)
            .withCoreAPI()
            .usingAuthUser()
            .addSiteMembershipRequest(publicSite);
        restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(publicSite, updatedMessage);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(ErrorModel.RELATIONSHIP_NOT_FOUND, newMember.getUsername(), publicSite.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                executionType = ExecutionType.REGRESSION, description = "Verify user is not able to update its own site membership request for private site")
    public void userCannotUpdateSiteMembershipRequestForPrivateSite() throws Exception
    {
        SiteModel privateSite = dataSite.usingUser(managerUser).createPrivateRandomSite();
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember)
            .withCoreAPI()
            .usingAuthUser()
            .addSiteMembershipRequest(privateSite);
        restClient.withCoreAPI().usingMe()
            .updateSiteMembershipRequest(privateSite, updatedMessage);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(ErrorModel.RELATIONSHIP_NOT_FOUND, newMember.getUsername(), privateSite.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                executionType = ExecutionType.REGRESSION, description = "Verify user is able to update its own site membership request with initial message")
    public void userCanUpdateSiteMembershipRequestWithInitialMessage() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember)
            .withCoreAPI()
            .usingAuthUser()
            .addSiteMembershipRequest(siteModel, updatedMessage);
        restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(siteModel, updatedMessage)
            .assertMembershipRequestMessageIs(updatedMessage)
            .and().field("id").is(siteModel.getId())
            .and().field("modifiedAt").isNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                executionType = ExecutionType.REGRESSION, description = "Verify user is able to update its own site membership request with different message")
    public void userCanUpdateSiteMembershipRequestWithDifferentMessage() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember)
            .withCoreAPI()
            .usingAuthUser()
            .addSiteMembershipRequest(siteModel, "");
        restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(siteModel, updatedMessage)
            .assertMembershipRequestMessageIs(updatedMessage)
            .and().field("id").is(siteModel.getId())
            .and().field("modifiedAt").isNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                executionType = ExecutionType.REGRESSION, description = "Verify modifiedAt field for update siteMembership request call")
    public void verifyModifiedAtForUpdateSiteMembershipRequest() throws Exception
    {
        UserModel newMember = dataUser.createRandomTestUser();
        restClient.authenticateUser(newMember)
            .withCoreAPI()
            .usingAuthUser()
            .addSiteMembershipRequest(siteModel);
        String firstModifiedAt = restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(siteModel, "first message").getModifiedAt();
        String secondModifiedAt = restClient.withCoreAPI().usingMe().updateSiteMembershipRequest(siteModel, "second message").getModifiedAt();
        Assert.assertNotEquals(firstModifiedAt, secondModifiedAt);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}