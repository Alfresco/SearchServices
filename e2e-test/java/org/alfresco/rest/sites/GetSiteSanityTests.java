package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.StatusModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */

@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
public class GetSiteSanityTests extends RestTest
{
    @Autowired
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    private UserModel adminUserModel;
    private ListUserWithRoles usersWithRoles;
    private UserModel userModel;
    private SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);        
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Manager role gets site information and gets status code OK (200)")
    public void getSiteWithManagerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                  .withCoreAPI().usingSite(siteModel)
                  .getSite()
                  .and().field("id").is(siteModel.getId())
                  .and().field("title").is(siteModel.getTitle());
        
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Collaborator role gets site information and gets status code OK (200)")
    public void getSiteWithCollaboratorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator))
                  .withCoreAPI().usingSite(siteModel)
                  .getSite()
                  .and().field("id").is(siteModel.getId())
                  .and().field("title").is(siteModel.getTitle());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Contributor role gets site information and gets status code OK (200)")
    public void getSiteWithContributorRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor))
                  .withCoreAPI().usingSite(siteModel)
                  .getSite()
                  .and().field("id").is(siteModel.getId())
                  .and().field("title").is(siteModel.getTitle());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with Consumer role gets site information and gets status code OK (200)")
    public void getSiteWithConsumerRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer))
                  .withCoreAPI().usingSite(siteModel)
                  .getSite()
                  .and().field("id").is(siteModel.getId())
                  .and().field("title").is(siteModel.getTitle());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Verify user with admin role gets site information and gets status code OK (200)")
    public void getSiteWithAdminRole() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingSite(siteModel)
                  .getSite()
                  .and().field("id").is(siteModel.getId())
                  .and().field("title").is(siteModel.getTitle());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Bug(id="MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, 
            description = "Failed authentication get site call returns status code 401")
    public void unauthenticatedUserIsNotAuthorizedToRetrieveSite() throws JsonToModelConversionException, Exception
    {
        userModel = dataUser.createRandomTestUser();
        userModel.setPassword("user wrong password");
        dataUser.addUserToSite(userModel, siteModel, UserRole.SiteManager);
        restClient.authenticateUser(userModel).withParams("maxItems=10000")
                  .withCoreAPI()
                  .getSites();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastException().hasName(StatusModel.UNAUTHORIZED);
    }
}
