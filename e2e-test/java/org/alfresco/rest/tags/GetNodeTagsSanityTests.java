package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestTagsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
public class GetNodeTagsSanityTests extends RestTest
{
    @Autowired
    private DataUser dataUser;
        
    @Autowired
    private RestTagsApi tagsAPI;
    
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private FileModel document;

    private String tagValue;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, 
                UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);
        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        tagsAPI.useRestClient(restClient);
        
        tagValue = RandomData.getRandomName("tag");
        tagsAPI.addTag(document, tagValue);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
                executionType = ExecutionType.SANITY, description = "Verify site Manager is able to get node tags")
    public void siteManagerIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        tagsAPI.addTag(document, tagValue);
        
        tagsAPI.getNodeTags(document)
              .assertThat()
              .entriesListContains("tag", tagValue.toLowerCase());            
        
        tagsAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify site Collaborator is able to get node tags")
    public void siteCollaboratorIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
   

        tagsAPI.getNodeTags(document).assertThat().entriesListContains("tag", tagValue.toLowerCase());  

        tagsAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify site Contributor is able to get node tags")
    public void siteContributorIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        tagsAPI.getNodeTags(document).assertThat().entriesListContains("tag", tagValue.toLowerCase());  

        tagsAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify site Consumer is able to get node tags")
    public void siteConsumerIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        tagsAPI.getNodeTags(document).assertThat().entriesListContains("tag", tagValue.toLowerCase());  

        tagsAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify admin is able to get node tags")
    public void adminIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        tagsAPI.getNodeTags(document).assertThat().entriesListContains("tag", tagValue.toLowerCase());  

        tagsAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to get node tags")
    @Bug(id="MNT-16904")
    public void unauthenticatedUserIsNotAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password"));
        tagsAPI.getNodeTags(document).assertThat().entriesListContains("tag", tagValue.toLowerCase());  

        tagsAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.UNAUTHORIZED);
    }
}