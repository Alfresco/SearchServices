package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GetNodeTagsSanityTests extends RestTest
{
    private UserModel adminUserModel;
    private SiteModel siteModel;
    private ListUserWithRoles usersWithRoles;
    private FileModel document;
    private RestTagModelsCollection returnedCollection;
    private String tagValue;
    private String tagValue2;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, 
                UserRole.SiteManager, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);        
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        
        tagValue = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(document).addTag(tagValue);
        
        tagValue2 = RandomData.getRandomName("tag");
        restClient.withCoreAPI().usingResource(document).addTag(tagValue2);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
                executionType = ExecutionType.SANITY, description = "Verify site Manager is able to get node tags")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void siteManagerIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {        
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        
        returnedCollection = restClient.withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
            .entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", tagValue2.toLowerCase());
   
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify site Collaborator is able to get node tags")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void siteCollaboratorIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteCollaborator));
        
        returnedCollection = restClient.withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
            .entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", tagValue2.toLowerCase()); 
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify site Contributor is able to get node tags")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void siteContributorIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteContributor));
        
        returnedCollection = restClient.withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
            .entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", tagValue2.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify site Consumer is able to get node tags")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void siteConsumerIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteConsumer));
        
        returnedCollection = restClient.withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
            .entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", tagValue2.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify admin is able to get node tags")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    public void adminIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(adminUserModel);
        returnedCollection = restClient.withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        returnedCollection.assertThat()
            .entriesListContains("tag", tagValue.toLowerCase())
            .and().entriesListContains("tag", tagValue2.toLowerCase());
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.TAGS }, 
            executionType = ExecutionType.SANITY, description = "Verify unauthenticated user is not able to get node tags")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.SANITY })
    @Bug(id="MNT-16904")
    public void unauthenticatedUserIsNotAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(new UserModel("random user", "random password"));
        restClient.withCoreAPI().usingResource(document).getNodeTags();
        restClient.assertStatusCodeIs(HttpStatus.UNAUTHORIZED).assertLastError()
                .containsSummary(RestErrorModel.AUTHENTICATION_FAILED);
    }
}