package org.alfresco.rest.people;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestFavoriteSiteModel;
import org.alfresco.rest.model.RestPersonFavoritesModel;
import org.alfresco.rest.model.RestPersonFavoritesModelsCollection;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.LinkModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class AddFavoriteSiteFullTests extends RestTest
{
    UserModel userModel, adminUserModel, adminTenantUser, tenantUser;
    SiteModel siteModel, privateSiteModel, moderatedSiteModel;
    UserModel searchedUser;
    FileModel document;
    FolderModel folder;
    private RestCommentModel comment;
    private RestTagModel returnedModel;
    private RestFavoriteSiteModel restFavoriteSiteModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        privateSiteModel = dataSite.usingUser(userModel).createPrivateRandomSite();   
        moderatedSiteModel = dataSite.usingUser(userModel).createModeratedRandomSite(); 
        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        folder = dataContent.usingSite(siteModel).usingUser(adminUserModel).createFolder();
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
     description = "Verify admin user doesn't have permission to add a favorite site of another user with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void adminIsNotAbleToAddFavoriteSiteOfAnotherUser() throws JsonToModelConversionException, Exception
    {
        UserModel collaboratorUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(collaboratorUser, siteModel, UserRole.SiteCollaborator);
        restClient.authenticateUser(collaboratorUser)
                  .withCoreAPI().usingUser(adminUserModel).addFavoriteSite(siteModel);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }    
    
    @Bug(id="MNT-17338")
    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify admin user doesn't have permission to add a favorite site of another user with Rest API and status code is 403")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userIsNotAbleToAddFavoriteSiteOfAdmin() throws JsonToModelConversionException, Exception
    {       
        UserModel consumerUser = dataUser.usingAdmin().createRandomTestUser();
        dataUser.usingUser(userModel).addUserToSite(consumerUser, siteModel, UserRole.SiteConsumer);
        restClient.authenticateUser(adminUserModel)
                  .withCoreAPI().usingUser(consumerUser).addFavoriteSite(siteModel);

        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                  .assertLastError()
                  .containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }    
         
    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify add favorite site using empty body - status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL})
    public void addFavoriteSiteWithEmptyBody() throws JsonToModelConversionException, Exception
    {           
        restClient.authenticateUser(adminUserModel).withCoreAPI();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{\"ids\": \"local\"}" ,
                              "people/{personId}/favorite-sites", adminUserModel.getUsername());
        restClient.processModel(RestPersonFavoritesModel.class, request);  
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()                  
                  .containsSummary(String.format(RestErrorModel.NO_CONTENT, "Unrecognized field " + "\"ids\""));  
           }  
           
    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify add favorite site using empty body - status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void addFavoriteSiteWithEmptyRequiredFieldsBody() throws JsonToModelConversionException, Exception
    {      
        restClient.authenticateUser(adminUserModel).withCoreAPI();
        RestRequest request = RestRequest.requestWithBody(HttpMethod.POST, "{\"\": \"\"}", 
                              "people/{personId}/favorite-sites", adminUserModel.getUsername());
        restClient.processModel(RestPersonFavoritesModel.class, request);  
        
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError()                  
                  .containsSummary(String.format(RestErrorModel.NO_CONTENT, "Unrecognized field " + "\"\""));    
         } 
          
    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify non member user is not able to add a private favorite site - status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userAddFavoriteSiteUserNotMemberOfPrivateSite() throws JsonToModelConversionException, Exception
    {            
        UserModel user = dataUser.usingAdmin().createRandomTestUser();                            
        restClient.authenticateUser(user).withCoreAPI()
                  .usingAuthUser().addFavoriteSite(privateSiteModel);
               
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                  .assertLastError()                  
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, privateSiteModel.getId()))   
                  .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);  
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify non member user is able to add a moderated favorite site")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userAddFavoriteSiteUserNotMemberOfModeratedSite() throws JsonToModelConversionException, Exception
    {            
        UserModel user = dataUser.usingAdmin().createRandomTestUser();       
        restClient.authenticateUser(user).withCoreAPI().usingAuthUser()
                  .addFavoriteSite(moderatedSiteModel); 
                
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
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION,
            description = "Verify non member user is able to add a public favorite site")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void userAddFavoriteSiteUserNotMemberOfPublicSite() throws JsonToModelConversionException, Exception
    {            
        UserModel user = dataUser.usingAdmin().createRandomTestUser();       
        restFavoriteSiteModel =  restClient.authenticateUser(user).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel); 
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restFavoriteSiteModel.assertThat().field("id").is(siteModel.getId());
        
        RestPersonFavoritesModelsCollection favorites = restClient.withCoreAPI().usingAuthUser().getFavorites();
        favorites.assertThat().entriesListContains("targetGuid", siteModel.getGuidWithoutVersion())
                              .and().paginationField("totalItems").is("1");        
        favorites.getOneRandomEntry().onModel().getTarget().getSite()
                 .assertThat()
                 .field("description").is(siteModel.getDescription())
                 .and().field("id").is(siteModel.getId())
                 .and().field("visibility").is(siteModel.getVisibility().toString())
                 .and().field("title").is(siteModel.getTitle());                        
    }          
      
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if id does not exist, status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void addFavoriteSiteUsingInvalidId() throws Exception
    {
        SiteModel site = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        String id = siteModel.getId();
        site.setId("invalidID");       

        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFavoriteSite(site);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "invalidID"));
        
        siteModel.setId(id); 
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user provides site in id but id is of a file status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void addSiteToFavoritesUsingFileId() throws Exception
    {
        String id = siteModel.getId();
        siteModel.setId(document.getNodeRefWithoutVersion());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, document.getNodeRefWithoutVersion()));
        
        siteModel.setId(id);    
     }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user provides site in id but id is of a folder status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void addSiteToFavoritesUsingFolderId() throws Exception
    {
        String id = siteModel.getId();
        siteModel.setId(folder.getNodeRefWithoutVersion());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, folder.getNodeRefWithoutVersion()));
        
        siteModel.setId(id);    
     }          

    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user provides site in id but id is of a comment status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void addSiteToFavoriteUsingCommentId() throws Exception
    {
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        String id = siteModel.getId();
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        comment = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(file).addComment("This is a comment");
        file.setNodeRef(comment.getId());
        siteModel.setId(comment.getId());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
        siteModel.setId(id);    
    }
    
    @Bug(id="MNT-16917")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if user provides site in id but id is of a tag status code is 404")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.FULL })
    public void addSiteFavoriteUsingTagId() throws Exception
    {
        FileModel file = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        returnedModel = restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).addTag("random_tag");
        file.setNodeRef(returnedModel.getId());
        siteModel.setId(returnedModel.getId());
        
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
                  .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, returnedModel.getId()));
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Check that if id does not describe a site, file, or folder status code is 400")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.CORE })
    public void addFavoriteUsingInvalidGuid() throws Exception
     {
      LinkModel link = dataLink.usingAdmin().usingSite(siteModel).createRandomLink();
      SiteModel site = dataSite.usingUser(adminUserModel).createPublicRandomSite();
      site.setId(link.getNodeRef());
      
      restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().addFavoriteSite(site);
      restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()   
                .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, site.getId().split("/")[3]));
  }       
    
    @Bug(id="MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user is not able to delete favorites using an invalid network ID - status code (401)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE,TestGroup.NETWORKS, TestGroup.FULL })
    public void tenantIsNotAbleToDeleteFavoriteSiteWithInvalidNetworkID() throws JsonToModelConversionException, Exception
    {
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");        
        siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();        
        siteModel.setGuid(restClient.authenticateUser(tenantUser).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        restClient.authenticateUser(tenantUser).withCoreAPI();  
        tenantUser.setDomain("invalidNetwork");       
        
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
                  .assertLastError()
                  .containsSummary(RestErrorModel.AUTHENTICATION_FAILED)
                  .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)    
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
       }  
      
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user deletes favorites site and status code is (201)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE,TestGroup.NETWORKS, TestGroup.FULL })
    public void tenantDeleteFavoriteSiteValidNetwork() throws JsonToModelConversionException, Exception
    {              
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);       
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");      
        siteModel = dataSite.usingUser(tenantUser).createPublicRandomSite();
        siteModel.setGuid(restClient.authenticateUser(tenantUser).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        restClient.authenticateUser(tenantUser).withCoreAPI();
        tenantUser.setDomain(tenantUser.getDomain());  
        
        restClient.withCoreAPI()
                  .usingAuthUser().addSiteToFavorites(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
              
        restClient.withCoreAPI().usingAuthUser().getFavorites()
                  .assertThat()
                  .entriesListContains("targetGuid", siteModel.getGuidWithoutVersion());
    }  
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.NETWORKS }, executionType = ExecutionType.REGRESSION,
            description = "Verify tenant user deletes favorites site created by admin tenant - same network and status code is (201)")
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE,TestGroup.NETWORKS, TestGroup.FULL })
    public void tenantUserIsAbleToDeleteFavoriteSiteAddedByAdminSameNetwork() throws JsonToModelConversionException, Exception
    {      
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel).usingTenant().createTenant(adminTenantUser);       
        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");      
        siteModel = dataSite.usingUser(adminTenantUser).createPublicRandomSite();
        siteModel.setGuid(restClient.authenticateUser(adminTenantUser).withCoreAPI().usingSite(siteModel).getSite().getGuid());
        restClient.authenticateUser(tenantUser).withCoreAPI();
        tenantUser.setDomain(adminTenantUser.getDomain());  
        
        restClient.withCoreAPI().usingAuthUser().addFavoriteSite(siteModel);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        restClient.withCoreAPI().usingAuthUser().getFavorites()
                  .assertThat()
                  .entriesListContains("targetGuid", siteModel.getGuidWithoutVersion());
    }   

}
