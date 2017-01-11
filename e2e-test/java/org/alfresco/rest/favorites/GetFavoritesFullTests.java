package org.alfresco.rest.favorites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestPersonFavoritesModel;
import org.alfresco.rest.model.RestPersonFavoritesModelsCollection;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetFavoritesFullTests extends RestTest
{
    private UserModel adminUserModel;
    private UserModel secondUserModel;
    private SiteModel firstSiteModel, secondSiteModel, thirdSiteModel;
    private RestPersonFavoritesModelsCollection userFavorites;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        secondUserModel = dataUser.createRandomTestUser();
        restClient.authenticateUser(adminUserModel);
        firstSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        secondSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        thirdSiteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();

        firstSiteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(firstSiteModel).getSite().getGuid());
        secondSiteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(secondSiteModel).getSite().getGuid());
        thirdSiteModel.setGuid(restClient.authenticateUser(adminUserModel).withCoreAPI().usingSite(thirdSiteModel).getSite().getGuid());

        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingAuthUser().addSiteToFavorites(secondSiteModel);
        
        restClient.authenticateUser(secondUserModel);
        restClient.withCoreAPI().usingUser(secondUserModel).addSiteToFavorites(firstSiteModel);
        restClient.withCoreAPI().usingUser(secondUserModel).addSiteToFavorites(secondSiteModel);
        restClient.withCoreAPI().usingUser(secondUserModel).addSiteToFavorites(thirdSiteModel);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify get favorites specifying -me- string in place of <personid> for request")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsAbleToGetFavoritesWhenUsingMeAsUsername() throws Exception
    {
        userFavorites = restClient.authenticateUser(secondUserModel).withCoreAPI().usingMe().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        userFavorites.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid()).and().entriesListContains("targetGuid", secondSiteModel.getGuid());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify get favorites using empty for where parameter for request")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsAbleToGetFavoritesWhenUsingEmptyWhereParameter() throws Exception
    {
        userFavorites = restClient.authenticateUser(adminUserModel).withCoreAPI().usingAuthUser().where().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_ARGUMENT, "WHERE query"));
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify that for invalid maxItems parameter status code returned is 400.")            
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void checkInvalidMaxItemsStatusCode() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withParams("maxItems=AB").withCoreAPI().usingUser(adminUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Invalid paging parameter");
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION,
            description = "Verify that for invalid skipCount parameter status code returned is 400.")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void checkInvalidSkipCountStatusCode() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withParams("skipCount=AB").withCoreAPI().usingUser(adminUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary("Invalid paging parameter");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify get favorites when using invalid network id")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void getFavoritesWhenNetworkIdIsInvalid() throws Exception
    {
        UserModel networkUserModel = dataUser.createRandomTestUser();
        networkUserModel.setDomain("invalidNetwork");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingUser(networkUserModel).where().targetSiteExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, networkUserModel.getUsername()));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify get favorites using special chars in where parameter for request")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsNotAbleToGetFavoritesWhenUsingSpecialCharsInWhereParameter() throws Exception
    {
        userFavorites = restClient.withCoreAPI().usingAuthUser().where().invalidWhereParameter("~!%40%23%24%25%5E%26*()_%2B%5B%5D%7B%7D%7C%5C%3B%27%3A%22%2C.%2F%3C%3E").getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                  .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_ARGUMENT, "WHERE query"))
                  .containsErrorKey(RestErrorModel.INVALID_QUERY_ERRORKEY)
                  .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
                  .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify get favorites using AND instead of OR in where parameter for request")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsNotAbleToGetFavoritesWhenUsingANDInWhereParameter() throws Exception
    {
        userFavorites = restClient.withCoreAPI().usingAuthUser().where().targetFolderExist().invalidWhereParameter("AND").targetFileExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
        .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_ARGUMENT, "WHERE query"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify get favorites using wrong name instead of EXISTS in where parameter for request")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsNotAbleToGetFavoritesWhenUsingWrongWhereParameter() throws Exception
    {
        userFavorites = restClient.withCoreAPI().usingAuthUser().where().invalidWhereParameter("EXIST((target/site))").targetFileExist().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_ARGUMENT, "WHERE query"));
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify get favorites except the first one for request")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsAbleToGetFavoritesExceptTheFirstOne() throws Exception
    {        
        userFavorites = restClient.authenticateUser(secondUserModel).withParams("skipCount=1").withCoreAPI().usingUser(secondUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);  
        
        userFavorites.assertThat().entriesListContains("targetGuid", firstSiteModel.getGuid())
            .and().entriesListContains("targetGuid", secondSiteModel.getGuid())
            .and().entriesListDoesNotContain("targetGuid", thirdSiteModel.getGuid());
    }
   
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify get first two favorites sites")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsAbleToGetFirstTwoFavorites() throws Exception
    {        
        userFavorites = restClient.authenticateUser(secondUserModel).withParams("maxItems=2").withCoreAPI().usingUser(secondUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);  
        
        userFavorites.assertThat().entriesListContains("targetGuid", thirdSiteModel.getGuid())
            .and().entriesListContains("targetGuid", secondSiteModel.getGuid())
            .and().entriesListDoesNotContain("targetGuid", firstSiteModel.getGuid())
            .getPagination().assertThat().field("maxItems").is("2")
            .and().field("hasMoreItems").is("true")
            .and().field("count").is("2");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify get favorites sites when using empty values for skipCount and maxItems")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsAbleToGetFavoritesWhenSkipCountAndMaxItemsAreEmpty() throws Exception
    {        
        restClient.authenticateUser(secondUserModel).withParams("skipCount= ").withCoreAPI().usingUser(secondUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_SKIPCOUNT, " "));  
        
        restClient.authenticateUser(secondUserModel).withParams("maxItems= ").withCoreAPI().usingUser(secondUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST).assertLastError().containsSummary(String.format(RestErrorModel.INVALID_MAXITEMS, " ")); 
    }
    
    @Bug(id = "MNT-16904")
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify the get favorites request when network id is invalid")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL, TestGroup.NETWORKS})
    public void getFavoriteSitesWhenNetworkIdIsInvalid() throws JsonToModelConversionException, Exception
    {   
        UserModel adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
        restClient.usingTenant().createTenant(adminTenantUser);
        UserModel tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().addSiteToFavorites(firstSiteModel);
        
        tenantUser.setDomain("invalidNetwork");
        restClient.authenticateUser(tenantUser).withCoreAPI().usingAuthUser().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED)
            .assertLastError().containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify the get favorites request for a high value for skipCount parameter")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsAbleToGetFavoritesWithHighSkipCount() throws Exception
    {        
        userFavorites = restClient.authenticateUser(secondUserModel).withParams("skipCount=999999999").withCoreAPI().usingUser(secondUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);  
        
        userFavorites.assertThat().entriesListIsEmpty().assertThat().paginationField("skipCount").is("999999999");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, 
            description = "Verify the get favorites request with properties parameter applied")
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    public void userIsAbleToGetFavoritesWithPropertiesParamApplied() throws Exception
    {        
        userFavorites = restClient.authenticateUser(secondUserModel).withParams("properties=targetGuid").withCoreAPI().usingUser(secondUserModel).getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);  
        
        RestPersonFavoritesModel restPersonFavoritesModel = userFavorites.getEntries().get(0).onModel();
        restPersonFavoritesModel.assertThat().field("targetGuid").is(thirdSiteModel.getGuid()).and().field("createdAt").isNull();
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.FAVORITES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.FAVORITES }, executionType = ExecutionType.REGRESSION, description = "Verify entry details for get favorites response with Rest API")
    public void checkResponseSchemaForGetFavorites() throws Exception
    {
        userFavorites = restClient.authenticateUser(secondUserModel).withCoreAPI().usingAuthUser().getFavorites();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        RestPersonFavoritesModel restPersonFavoritesModel = userFavorites.getEntries().get(0).onModel();
        restPersonFavoritesModel.assertThat().field("targetGuid").is(thirdSiteModel.getGuid());
        
        RestSiteModel restSiteModel = restPersonFavoritesModel.getTarget().getSite();
        restSiteModel.assertThat().field("visibility").is(thirdSiteModel.getVisibility()).and()
                                  .field("guid").is(thirdSiteModel.getGuid()).and()
                                  .field("description").is(thirdSiteModel.getDescription()).and()
                                  .field("id").is(thirdSiteModel.getId()).and()
                                  .field("title").is(thirdSiteModel.getTitle());
    }
}