package org.alfresco.rest.people;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestPersonFavoritesModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddFavoriteSiteCoreTests extends RestTest
{
    UserModel userModel, adminUserModel;
    SiteModel siteModel, privateSiteModel, moderatedSiteModel;
    UserModel searchedUser;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        privateSiteModel = dataSite.usingUser(userModel).createPrivateRandomSite();   
        moderatedSiteModel = dataSite.usingUser(userModel).createModeratedRandomSite();   
    }

    @Bug(id="ACE-2413")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })  
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
    description = "Verify empty user is not able to add a site from favorites and response is (400)")
    public void emptyUserIsNotAbleToRemoveFavoriteSite() throws Exception
    {
        UserModel emptyUser = new UserModel("", "password");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(emptyUser).addFavoriteSite(siteModel);
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "emptyUser"));
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })  
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
    description = "Verify inexistent user is not able to add a site from favorites and response is (404)")
    public void inexistentUserIsNotAbleToRemoveFavoriteSite() throws Exception
    {
        UserModel inexistentUser = new UserModel("inexistentUser", "password");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(inexistentUser).addFavoriteSite(siteModel);
        
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "inexistentUser"))   
                  .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
    description = "Verify manager user is not able to add a favorite site when the site is already favorite - status code (409)")
    public void managerUserAddFavoriteSiteAlreadyAFavoriteSite() throws Exception
    {
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();       
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);

        restClient.authenticateUser(managerUser).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    
        restClient.withCoreAPI().usingUser(managerUser).addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CONFLICT);
        restClient.assertLastError()
                  .containsSummary(String.format("%s is already a favourite site", siteModel.getId()))                
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE)
                  .containsErrorKey(String.format("Site %s is already a favourite site", siteModel.getId()));
    }
    
    @Bug(id="MNT-17337")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
    description = "Verify user is not able to add a favorite site when the 'id' is empty - status code (400)")
    public void userAddFavoriteSiteAFavoriteSite() throws Exception
    {         
        siteModel.setId("");
        restClient.authenticateUser(userModel).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
       
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        restClient.assertLastError()
                  .containsSummary("siteId is null")                
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE)
                  .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY);
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify user doesn't have permission to delete favorites of admin user with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void userIsNotAbleToAddFavoriteSiteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        UserModel contributorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(contributorUser, siteModel, UserRole.SiteContributor);
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
             
        restClient.authenticateUser(consumerUser).withCoreAPI()
                  .usingUser(contributorUser).addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);   
    }         
        
    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify manager user is able to add as favorite a private site.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void managerAddsPrivateSiteAsFavorite() throws JsonToModelConversionException, Exception
    {            
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, privateSiteModel, UserRole.SiteManager);

        restClient.authenticateUser(managerUser).withCoreAPI().usingUser(managerUser).addFavoriteSite(privateSiteModel); 
        restClient.assertStatusCodeIs(HttpStatus.CREATED);     
        
        RestPersonFavoritesModelsCollection favorites = restClient.withCoreAPI().usingAuthUser().getFavorites();
        favorites.assertThat().entriesListContains("targetGuid", privateSiteModel.getGuidWithoutVersion())
                              .and().paginationField("totalItems").is("1");        
        favorites.getOneRandomEntry().onModel().getTarget().getSite()
                 .assertThat()
                 .field("description").is(privateSiteModel.getDescription())
                 .and().field("id").is(privateSiteModel.getId())
                 .and().field("visibility").is(privateSiteModel.getVisibility().toString())
                 .and().field("title").is(privateSiteModel.getTitle());               
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify manager user is able to add as favorite a moderated site.")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    public void managerAddsModeratedSiteAsFavorite() throws JsonToModelConversionException, Exception
    {            
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, moderatedSiteModel, UserRole.SiteManager);

        restClient.authenticateUser(managerUser).withCoreAPI().usingUser(managerUser).addFavoriteSite(moderatedSiteModel); 
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
          
        RestPersonFavoritesModelsCollection favorites = restClient.withCoreAPI().usingAuthUser().getFavorites();
        favorites.assertThat().entriesListContains("targetGuid", moderatedSiteModel.getGuidWithoutVersion())
                              .and().paginationField("totalItems").is("1");        
        favorites.getOneRandomEntry().onModel().getTarget().getSite()
                 .assertThat()
                 .field("description").is(moderatedSiteModel.getDescription())
                 .and().field("id").is(moderatedSiteModel.getId())
                 .and().field("visibility").is(moderatedSiteModel.getVisibility().toString())
                 .and().field("title").is(moderatedSiteModel.getTitle());               
    }    
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
    description = "Verify user removes a site from favorites using '-me-' in place of personId with Rest API and response is successful (201)")
    public void addFavoriteSiteWithSuccessUsingMeAsPersonId() throws Exception
    {
        restClient.authenticateUser(adminUserModel); 
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingMe().addFavoriteSite(moderatedSiteModel);         
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
    description = "Verify manager user removes a site from its favorites and adds it again and response is successful (204)")
    public void managerUserRemovesFavoriteSiteAndAddItAgain() throws Exception
    {
        UserModel managerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(managerUser, siteModel, UserRole.SiteManager);
        dataSite.usingUser(managerUser).usingSite(siteModel).addSiteToFavorites();

        restClient.authenticateUser(managerUser).withCoreAPI().usingAuthUser().removeFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        
        restClient.withCoreAPI().usingUser(managerUser).addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }    

}