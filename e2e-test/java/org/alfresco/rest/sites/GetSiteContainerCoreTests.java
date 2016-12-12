package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.constants.ContainerName;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
public class GetSiteContainerCoreTests extends RestTest{

    private UserModel adminUserModel, testUserModel;
    private SiteModel publicSiteModel, moderatedSiteModel, privateSiteModel;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        testUserModel = dataUser.createRandomTestUser();
        publicSiteModel = dataSite.usingAdmin().createPublicRandomSite();
        moderatedSiteModel = dataSite.usingAdmin().createModeratedRandomSite();
        privateSiteModel = dataSite.usingAdmin().createPrivateRandomSite();
        
        dataLink.usingAdmin().usingSite(publicSiteModel).createRandomLink();
        dataDiscussion.usingAdmin().usingSite(publicSiteModel).createRandomDiscussion();

        dataLink.usingAdmin().usingSite(moderatedSiteModel).createRandomLink();
        dataDiscussion.usingAdmin().usingSite(moderatedSiteModel).createRandomDiscussion();

        dataLink.usingAdmin().usingSite(privateSiteModel).createRandomLink();
        dataDiscussion.usingAdmin().usingSite(privateSiteModel).createRandomDiscussion();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get container request returns status code 400 when site doesn't exist")            
    public void getContainerWithNonExistentSite() throws Exception
    {
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite("NonExistentSiteId").getSiteContainer(ContainerName.discussions.toString());
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, "NonExistentSiteId", ContainerName.discussions.toString()));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get container request returns status code 400 when container item doesn't exist")            
    public void getContainerWithNonExistentItem() throws Exception
    {
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite(publicSiteModel).getSiteContainer("NonExistentFolder");
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.RELATIONSHIP_NOT_FOUND, publicSiteModel.getId(), "NonExistentFolder"));
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get container request returns status 200 for public site")
    public void getContainerForPublicSite() throws Exception
    {
        restClient.authenticateUser(testUserModel)
                .withCoreAPI().usingSite(publicSiteModel).getSiteContainer(ContainerName.discussions.toString())
                .assertThat().field("folderId").is(ContainerName.discussions.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        restClient.authenticateUser(testUserModel)
	        .withCoreAPI().usingSite(publicSiteModel).getSiteContainer(ContainerName.links.toString())
	        .assertThat().field("folderId").is(ContainerName.links.toString()); 
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get container request returns status 200 for private site")
    public void getContainerForPrivateSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel)
                .withCoreAPI().usingSite(privateSiteModel).getSiteContainer(ContainerName.discussions.toString())
                .assertThat().field("folderId").is(ContainerName.discussions.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        
        restClient.withCoreAPI().usingSite(privateSiteModel).getSiteContainer(ContainerName.links.toString())
        	.assertThat().field("folderId").is(ContainerName.links.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.SANITY })
    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify get container request returns status 200 for moderated site")
    public void getContainerForModeratedSite() throws Exception
    {
        restClient.authenticateUser(adminUserModel)
	        .withCoreAPI().usingSite(moderatedSiteModel).getSiteContainer(ContainerName.discussions.toString())
	        .assertThat().field("folderId").is(ContainerName.discussions.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
       
        restClient.withCoreAPI().usingSite(moderatedSiteModel).getSiteContainer(ContainerName.links.toString())
        	.assertThat().field("folderId").is(ContainerName.links.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

}