package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.ContainerName;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class GetSiteContainerFullTests extends RestTest
{
    private UserModel adminUserModel, testUserModel;
    private SiteModel publicSiteModel, moderatedSiteModel, privateSiteModelByAdmin, privateSiteModelByUser;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        testUserModel = dataUser.createRandomTestUser();
        publicSiteModel = dataSite.usingAdmin().createPublicRandomSite();
        moderatedSiteModel = dataSite.usingUser(testUserModel).createModeratedRandomSite();
        privateSiteModelByAdmin = dataSite.usingAdmin().createPrivateRandomSite();       
        
        privateSiteModelByUser = dataSite.usingUser(testUserModel).createPrivateRandomSite();
        
        dataLink.usingAdmin().usingSite(publicSiteModel).createRandomLink();
        dataDiscussion.usingAdmin().usingSite(publicSiteModel).createRandomDiscussion();

        dataLink.usingUser(testUserModel).usingSite(moderatedSiteModel).createRandomLink();
        
        dataLink.usingAdmin().usingSite(privateSiteModelByAdmin).createRandomLink();
        
        dataDiscussion.usingUser(testUserModel).usingSite(privateSiteModelByUser).createRandomDiscussion();
        dataLink.usingUser(testUserModel).usingSite(privateSiteModelByUser).createRandomLink();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get container request with properties parameter returns status code 200 and parameter is applied")            
    public void getContainerUsingPropertiesParameter() throws Exception
    {
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite(publicSiteModel).usingParams("properties=id").getSiteContainer(ContainerName.discussions.toString())
                .assertThat().fieldsCount().is(1)
                .and().field("id").isNotEmpty()
                .and().field("folderId").isNull();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get container request for a container that does not belong to site returns status code 404")            
    public void getContainerThatDoesNotBelongToSite() throws Exception
    {
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite(moderatedSiteModel).getSiteContainer(ContainerName.discussions.toString());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, moderatedSiteModel.getId(), ContainerName.discussions.toString()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get container request for empty siteId returns status code 404")            
    public void getContainerForEmptySiteId() throws Exception
    {        
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite("").getSiteContainer(ContainerName.discussions.toString());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, "", ContainerName.discussions.toString()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get container request for empty container returns status code 200 and the list of containers")            
    public void getContainerForEmptyContainer() throws Exception
    {        
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite(moderatedSiteModel).getSiteContainer("");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get container with name containing special chars returns status code 404")            
    public void getContainerWithNameContainingSpecialChars() throws Exception
    {
        String containerSpecialName = RandomStringUtils.randomAlphabetic(2) + "~!%40%23%24%25%5E%26*()_%2B%5B%5D%7B%7D%7C%5C%3B%27%3A%22%2C.%2F%3C%3E";
        
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite(publicSiteModel).getSiteContainer(containerSpecialName);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, publicSiteModel.getId(), containerSpecialName))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if admin can get container request for a private site created by another user and status code is 200")            
    public void adminCanGetContainerForPrivateSiteCreatedByUser() throws Exception
    {        
        restClient.authenticateUser(adminUserModel)
                .withCoreAPI().usingSite(privateSiteModelByUser).getSiteContainer(ContainerName.discussions.toString())
                .assertThat().field("folderId").is(ContainerName.discussions.toString())
                .and().field("id").isNotEmpty();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if user cannot get container request for a private site created by admin and status code is 403")            
    public void userCannotGetContainerForPrivateSiteCreatedByAdmin() throws Exception
    {        
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite(privateSiteModelByAdmin).getSiteContainer(ContainerName.links.toString());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, privateSiteModelByAdmin.getId(), ContainerName.links.toString()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if user can get container request for a public site created by admin and status code is 200")            
    public void userCanGetContainerForPublicSiteCreatedByAdmin() throws Exception
    {        
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite(publicSiteModel).getSiteContainer(ContainerName.links.toString())
                .assertThat().field("folderId").is(ContainerName.links.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get container request for a deleted container returns status code is 404")            
    public void getContainerThatWasDeleted() throws Exception
    { 
        restClient.authenticateUser(testUserModel)
            .withCoreAPI().usingSite(privateSiteModelByUser).getSiteContainer(ContainerName.links.toString())
            .assertThat().field("folderId").is(ContainerName.links.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        //use dataprep for delete 'links' container
        FolderModel folder= new FolderModel("links");
        folder.setCmisLocation(String.format("/Sites/%s/%s", privateSiteModelByUser.getId(), ContainerName.links.toString()));
        dataContent.deleteTree(folder);
        
        restClient.authenticateUser(testUserModel)
            .withCoreAPI().usingSite(privateSiteModelByUser).getSiteContainer(ContainerName.links.toString());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsErrorKey(RestErrorModel.RELATIONSHIP_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, privateSiteModelByUser.getId(), ContainerName.links.toString()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}
