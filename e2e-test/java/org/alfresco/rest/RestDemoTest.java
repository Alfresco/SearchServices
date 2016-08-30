package org.alfresco.rest;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.SiteMember;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.apache.chemistry.opencmis.client.api.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Role;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class RestDemoTest extends RestTest
{
    @Autowired
    RestSitesApi sitesApi;

    @Autowired
    RestCommentsApi commentsAPI;

    private UserModel userModel;
    private SiteModel siteModel;

    @BeforeClass
    public void setUp() throws DataPreparationException
    {
        userModel = dataUser.getAdminUser();
        siteModel = dataSite.createPublicRandomSite();
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
    public void sitesTest() throws JsonToModelConversionException, Exception
    {
        sitesApi.getAllSites().assertThatResponseHasSite(siteModel)
        			.getSite(siteModel)
                		.assertSiteHasVisibility(Visibility.PUBLIC)
                		.assertSiteHasTitle(siteModel.getTitle())
                		.assertSiteHasDescription(siteModel.getDescription());
    }

    /**
     * Data preparation – create site and document on server <br/>
     * POST one comment to file using admin user <br/>
     * Perform GET comments, check the new one is listed <br/>
     * Update existing comment using PUT call, check that comment content is updated <br/>
     * 
     * @throws JsonToModelConversionException
     */
    @Test
    public void commentsTest() throws JsonToModelConversionException
    {
        Document document = dataContent.usingPath("Shared")
        							   .usingUser(userModel)
        							   .createDocument(DocumentType.TEXT_PLAIN);
        // add new comment
        RestCommentModel commentEntry = commentsAPI.addComment(document.getId(), "This is a new comment");
        commentsAPI.getNodeComments(document.getId())
        				.assertThatResponseIsNotEmpty()
        				.assertThatCommentWithIdExists(commentEntry.getId())
        				.assertThatCommentWithContentExists("This is a new comment");

        // update comment
        commentEntry = commentsAPI.updateComment(document.getId(), commentEntry.getId(), "This is the updated comment");
        commentsAPI.getNodeComments(document.getId()).assertThatResponseIsNotEmpty()
                .assertThatCommentWithIdExists(commentEntry.getId())
                .assertThatCommentWithContentExists("This is the updated comment");
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
    public void siteMembersTest() throws DataPreparationException, JsonToModelConversionException
    {
        UserModel newUser = dataUser.createRandomTestUser();
        SiteMember siteMember = new SiteMember(Role.SiteConsumer.toString(), newUser.getUsername());

        // add user as Consumer to site
        sitesApi.addPerson(siteModel.getId(), siteMember);
        sitesApi.getSiteMembers(siteModel.getId()).assertThatSiteHasMember(siteMember.getId())
                .getSiteMember(siteMember.getId()).assertSiteMemberHasRole(Role.SiteConsumer);

        // update site member to Manager
        siteMember.setRole(Role.SiteManager.toString());
        ;
        sitesApi.updateSiteMember(siteModel.getId(), newUser.getUsername(), siteMember);
        sitesApi.getSiteMembers(siteModel.getId()).assertThatSiteHasMember(siteMember.getId())
                .getSiteMember(siteMember.getId()).assertSiteMemberHasRole(Role.SiteManager);

        // delete site member
        sitesApi.deleteSiteMember(siteModel.getId(), newUser.getUsername());
        sitesApi.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT.toString());

    }
}