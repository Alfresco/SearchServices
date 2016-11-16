package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.ErrorModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.SANITY })
public class DeleteSiteMemberSanityTests extends RestTest
{    
    private SiteModel siteModel;
    private UserModel adminUser;
    private ListUserWithRoles usersWithRoles;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUser = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel,
                                                          UserRole.SiteManager, 
                                                          UserRole.SiteCollaborator, 
                                                          UserRole.SiteConsumer, 
                                                          UserRole.SiteContributor);    
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
                  executionType = ExecutionType.SANITY, 
                  description = "Verify site manager is able to delete another member of the site")
    public void siteManagerCanDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel newUser = dataUser.createRandomTestUser("testUser");
        newUser.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager))
                  .withCoreAPI()
                  .usingSite(siteModel).addPerson(newUser);
        
        restClient.withCoreAPI().usingUser(newUser).deleteSiteMember(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify admin user is able to delete another member of the site")
    public void adminIsAbleToDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel newUser = dataUser.createRandomTestUser("testUser");
        newUser.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(adminUser)  
                  .withCoreAPI()
                  .usingSite(siteModel).addPerson(newUser);
        
        restClient.withCoreAPI().usingUser(newUser).deleteSiteMember(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify site collaborator does not have permission to delete another member of the site")    
    @Bug(id="ACE-5444")
    public void siteCollaboratorIsNotAbleToDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel newUser = dataUser.createRandomTestUser("testUser");
        newUser.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(adminUser)  
                  .withCoreAPI()
                  .usingSite(siteModel).addPerson(newUser);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));

        restClient.withCoreAPI().usingUser(newUser).deleteSiteMember(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                                    .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify site contributor does not have permission to delete another member of the site")
    @Bug(id="ACE-5444")
    public void siteContributorIsNotAbleToDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel newUser = dataUser.createRandomTestUser("testUser");
        newUser.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(adminUser)  
                  .withCoreAPI()
                  .usingSite(siteModel).addPerson(newUser);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));

        restClient.withCoreAPI().usingUser(newUser).deleteSiteMember(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                                    .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify site consumer does not have permission to delete another member of the site")
    @Bug(id="ACE-5444")
    public void siteConsumerIsNotAbleToDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        UserModel newUser = dataUser.createRandomTestUser("testUser");
        newUser.setUserRole(UserRole.SiteCollaborator);
        restClient.authenticateUser(adminUser)  
                  .withCoreAPI()
                  .usingSite(siteModel).addPerson(newUser);
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));

        restClient.withCoreAPI().usingUser(newUser).deleteSiteMember(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                                    .assertLastError().containsSummary(ErrorModel.PERMISSION_WAS_DENIED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, 
              executionType = ExecutionType.SANITY, 
              description = "Verify unauthenticated user is not able to delete another member of the site")
    public void unauthenticatedUserIsNotAbleToDeleteSiteMember() throws JsonToModelConversionException, DataPreparationException, Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password"));

        restClient.withCoreAPI().usingUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer)).deleteSiteMember(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}