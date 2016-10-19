package org.alfresco.rest.demo;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.requests.RestCommentsApi;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Role;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "demo" })
public class RestDemoTests extends RestTest
{
    @Autowired
    RestSitesApi sitesApi;

    @Autowired
    RestCommentsApi commentsAPI;

    private UserModel userModel;
    private SiteModel siteModel;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws DataPreparationException
    {       
        userModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        restClient.authenticateUser(userModel);
        
        sitesApi.useRestClient(restClient);
        commentsAPI.useRestClient(restClient);        
    }

    /**
     * Data preparation – create site with custom details <br/>
     * Perform GET sites call using admin user<br/>
     * Check that created site is included in response <br/>
     * Perform GET site call, validate that site title, description and visibility are correct <br/>
     * 
     * @throws JsonToModelConversionException
     * @throws Exception
     */
    @Test
    public void adminRetrievesCorrectSiteDetails() throws JsonToModelConversionException, Exception
    {

        sitesApi.getAllSites()
            .assertEntriesListContains("id", siteModel.getId())            
            .getSite(siteModel)
              .and().assertField("id").isNotNull()
              .and().assertField("description").is(siteModel.getDescription())
              .and().assertField("title").is(siteModel.getTitle())            
          	  .and().assertField("visibility").is(Visibility.PUBLIC);
    }

    /**
     * Data preparation – create site and document on server <br/>
     * POST one comment to file using admin user <br/>
     * Perform GET comments, check the new one is listed <br/>
     * Update existing comment using PUT call, check that comment content is updated <br/>
     * @throws Exception 
     */
    @Test  
    public void adminCanPostAndUpdateComments() throws Exception
    {       
        FileModel fileModel = dataContent.usingUser(userModel)
                                  .usingResource(FolderModel.getSharedFolderModel())        			       
                                  .createContent(DocumentType.TEXT_PLAIN);
        // add new comment
        RestCommentModel commentEntry = commentsAPI.addComment(fileModel, "This is a new comment");
        commentsAPI.getNodeComments(fileModel)
            .assertEntriesListIsNotEmpty()
            .assertEntriesListContains("content", "This is a new comment");
            //.assertEntriesListIsEmpty()
            //.assertCommentWithIdExists(commentEntry);

//        // update comment
//        commentEntry = commentsAPI.updateComment(fileModel, commentEntry, "This is the updated comment with Collaborator user");
//
//        commentsAPI.getNodeComments(fileModel)
//            .assertEntriesListIsNotEmpty();
            //.assertCommentWithIdExists(commentEntry)
            //.assertCommentWithContentExists("This is the updated comment");
    }

    /**
     * Data preparation – create site and a new user <br/>
     * As admin, add user as Consumer to site as a new site member using POST call <br/>
     * Update site member role to Manager using PUT call <br/>
     * Delete site member using DELETE call <br/>
     * 
     * @throws DataPreparationException
     * @throws JsonToModelConversionException
     */
    @Test
    public void adminCanAddAndUpdateSiteMemberDetails() throws Exception
    {
        UserModel testUser = dataUser.createRandomTestUser("testUser");
        testUser.setUserRole(UserRole.SiteConsumer);

        // add user as Consumer to site
        sitesApi.addPerson(siteModel, testUser);
        sitesApi.getSiteMembers(siteModel).assertEntriesListContains("id", testUser.getUsername())            
            .getSiteMember(testUser.getUsername())
            .assertSiteMemberHasRole(Role.SiteConsumer);

        // update site member to Manager
        testUser.setUserRole(UserRole.SiteCollaborator);
        sitesApi.updateSiteMember(siteModel, testUser);
        sitesApi.getSiteMembers(siteModel)
            .assertEntriesListContains("id", testUser.getUsername())
            .getSiteMember(testUser.getUsername())
            .assertSiteMemberHasRole(Role.SiteCollaborator);

        sitesApi.deleteSiteMember(siteModel, testUser);
        sitesApi.usingRestWrapper()
            .assertStatusCodeIs(HttpStatus.NO_CONTENT);
    }
}