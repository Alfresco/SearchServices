package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestTagsApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.data.DataUser.ListUserWithRoles;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "tags", "sanity" })
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
    }
    
    @BeforeMethod(alwaysRun = true)
    public void generateRandomTag()
    {
        tagValue = RandomData.getRandomName("tag");
    }
    
    @TestRail(section = { "rest-api", "tags" }, 
                executionType = ExecutionType.SANITY, description = "Verify site Manager is able to get node tags")
    public void siteManagerIsAbleToRetrieveNodeTags() throws JsonToModelConversionException, Exception
    {
        restClient.authenticateUser(usersWithRoles.getOneUserWithRole(UserRole.SiteManager));
        tagsAPI.addTag(document, tagValue);
        
        tagsAPI.getNodeTags(document)
            .assertTagExists(tagValue);
        
        tagsAPI.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.OK);
    }
    
}