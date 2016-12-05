package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
public class GetSiteMembershipInformationCoreTests extends RestTest
{
    private SiteModel publicSiteModel;
    private SiteModel moderatedSiteModel;
    private SiteModel privateSiteModel;
    private UserModel userModel;
    UserModel leaveSiteUserModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        userModel = dataUser.createRandomTestUser();
        leaveSiteUserModel = dataUser.createRandomTestUser();
        publicSiteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        moderatedSiteModel = dataSite.usingUser(userModel).createModeratedRandomSite();
        privateSiteModel = dataSite.usingUser(userModel).createPrivateRandomSite();
        dataUser.addUserToSite(leaveSiteUserModel, publicSiteModel, UserRole.SiteCollaborator);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify site manager is able to retrieve site membership information with -me- as personId")
    public void siteManagerCanRetrieveSiteMembershipInformationWithMeAsPersonId() throws Exception
    {
        restClient.authenticateUser(userModel)
                .withCoreAPI()
                .usingMe()
                .getSitesMembershipInformation()
                .assertThat().entriesListIsNotEmpty()
                .and().paginationExist()
                .and().paginationField("count").isNot("0");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify site manager is not able to retrieve site membership information for a personId that does not exist")
    public void siteManagerCantRetrieveSiteMembershipInformationForAPersonIdThatDoesNotExist() throws Exception
    {
        restClient.authenticateUser(userModel)
                .withCoreAPI()
                .usingUser(new UserModel("invalidPersonId", "password"))
                .getSitesMembershipInformation()
                .assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify if get site membership information request returns status code 400 when invalid maxItems parameter is used")
    public void getSiteMembershipInformationRequestReturns400ForInvalidMaxItemsParameter() throws Exception
    {
        restClient.authenticateUser(userModel)
                .withParams("maxItems=0")
                .withCoreAPI()
                .usingAuthUser()
                .getSitesMembershipInformation()
                .assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify if get site membership information request returns status code 400 when invalid skipCount parameter is used")
    public void getSiteMembershipInformationRequestReturns400ForInvalidSkipCountParameter() throws Exception
    {
        restClient.authenticateUser(userModel)
                .withParams("skipCount=-1")
                .withCoreAPI()
                .usingAuthUser()
                .getSitesMembershipInformation()
                .assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify site manager is able to retrieve public sites")
    public void siteManagerCheckThatPublicSitesAreRetrieved() throws Exception
    {
        restClient.authenticateUser(userModel)
                .withCoreAPI()
                .usingAuthUser()
                .getSitesMembershipInformation()
                .assertThat().entriesListContains("id", publicSiteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify site manager is able to retrieve moderated sites")
    public void siteManagerCheckThatModeratedSitesAreRetrieved() throws Exception
    {
        restClient.authenticateUser(userModel)
                .withCoreAPI()
                .usingAuthUser()
                .getSitesMembershipInformation()
                .assertThat().entriesListContains("id", moderatedSiteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify site manager is able to retrieve private sites")
    public void siteManagerCheckThatPrivateSitesAreRetrieved() throws Exception
    {
        restClient.authenticateUser(userModel)
                .withCoreAPI()
                .usingAuthUser()
                .getSitesMembershipInformation()
                .assertThat().entriesListContains("id", privateSiteModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify site member is able to leave site")
    public void siteMemberIsAbleToLeaveSite() throws Exception
    {
        restClient.authenticateUser(leaveSiteUserModel).withCoreAPI().usingAuthUser().deleteSiteMember(publicSiteModel);
        restClient.authenticateUser(leaveSiteUserModel)
                .withCoreAPI()
                .usingAuthUser()
                .getSitesMembershipInformation()
                .assertThat().entriesListIsEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
