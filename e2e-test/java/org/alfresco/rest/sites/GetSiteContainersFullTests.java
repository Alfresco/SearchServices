package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteContainerModelsCollection;
import org.alfresco.utility.constants.ContainerName;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class GetSiteContainersFullTests  extends RestTest
{
    private SiteModel publicSiteWithContainers;
    private ListUserWithRoles publicSiteWithContainersUsers;
    RestSiteContainerModelsCollection restSiteContainers;
    int totalItems;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        publicSiteWithContainers = dataSite.usingAdmin().createPublicRandomSite();

        publicSiteWithContainersUsers = dataUser
                .addUsersWithRolesToSite(publicSiteWithContainers, UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer,
                        UserRole.SiteContributor);

        dataLink.usingAdmin().usingSite(publicSiteWithContainers).createRandomLink();
        dataDiscussion.usingAdmin().usingSite(publicSiteWithContainers).createRandomDiscussion();
        dataContent.usingAdmin().usingSite(publicSiteWithContainers).createFolder();
        
        restSiteContainers = restClient.authenticateUser(publicSiteWithContainersUsers.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingSite(publicSiteWithContainers).getSiteContainers();
        totalItems = restSiteContainers.getPagination().getTotalItems();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request for a container that is not empty returns status code 200")
    public void getSiteContainersThatIsNotEmpty() throws Exception
    {
        restClient.authenticateUser(publicSiteWithContainersUsers.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingSite(publicSiteWithContainers).getSiteContainers()
            .assertThat().entriesListCountIs(3)
            .and().entriesListContains("folderId" ,ContainerName.documentLibrary.toString());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site containers request returns status code 200 when first container is skipped")
    public void getSiteContainersAndSkipFirst() throws Exception
    {       
        restClient.authenticateUser(publicSiteWithContainersUsers.getOneUserWithRole(UserRole.SiteCollaborator))
            .withParams("skipCount=1").withCoreAPI().usingSite(publicSiteWithContainers).getSiteContainers()
            .assertThat().entriesListCountIs(2)
            .and().entriesListDoesNotContain("folderId" , restSiteContainers.getEntries().get(0).onModel().getFolderId())
            .and().entriesListContains("folderId" , restSiteContainers.getEntries().get(1).onModel().getFolderId())
            .and().entriesListContains("folderId" , restSiteContainers.getEntries().get(2).onModel().getFolderId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site containers request and get last container returns status code 200")
    public void getLastSiteContainer() throws Exception
    {       
        restClient.authenticateUser(publicSiteWithContainersUsers.getOneUserWithRole(UserRole.SiteCollaborator))
            .withParams("skipCount=" + String.valueOf(totalItems-1)).withCoreAPI().usingSite(publicSiteWithContainers).getSiteContainers()
            .assertThat().entriesListCountIs(1)
            .and().entriesListDoesNotContain("folderId" , restSiteContainers.getEntries().get(0).onModel().getFolderId())
            .and().entriesListDoesNotContain("folderId" , restSiteContainers.getEntries().get(1).onModel().getFolderId())
            .and().entriesListContains("folderId" , restSiteContainers.getEntries().get(2).onModel().getFolderId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site containers request returns status code 200 when high skipCount parameter is used")
    public void getSiteContainersWithHighSkipCount() throws Exception
    {
        restClient.authenticateUser(publicSiteWithContainersUsers.getOneUserWithRole(UserRole.SiteCollaborator))
            .withParams("skipCount=999999").withCoreAPI().usingSite(publicSiteWithContainers).getSiteContainers()
            .assertThat().entriesListIsEmpty()
            .and().entriesListCountIs(0)
            .and().paginationField("skipCount").is("999999");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request for a empty siteId returns status code 404")
    public void getSiteContainersForEmptySiteId() throws Exception
    {
        restClient.authenticateUser(publicSiteWithContainersUsers.getOneUserWithRole(UserRole.SiteManager))
            .withCoreAPI().usingSite("").getSiteContainers();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, ""))
            .stackTraceIs(RestErrorModel.STACKTRACE)
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.FULL })
    @TestRail(section={TestGroup.REST_API, TestGroup.SITES}, executionType= ExecutionType.REGRESSION,
            description= "Verify if get site container request returns status code 200 with valid maxItems parameter")
    public void getContainersWithValidMaxItems() throws Exception
    {
        restClient.authenticateUser(publicSiteWithContainersUsers.getOneUserWithRole(UserRole.SiteManager))
            .withParams("maxItems=2").withCoreAPI().usingSite(publicSiteWithContainers).getSiteContainers()
            .assertThat().entriesListCountIs(2)
            .getPagination().assertThat().field("count").is("2")
            .and().field("hasMoreItems").is("true")
            .and().field("totalItems").is("3")
            .and().field("skipCount").is("0")
            .and().field("maxItems").is("2");
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
