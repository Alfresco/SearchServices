package org.alfresco.rest.v1;

import java.io.File;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.ContentService;
import org.alfresco.rest.RestCommentsApi;
import org.alfresco.rest.RestSitesApi;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.Content;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.rest.model.SiteMember;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
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
    DataUser dataUser;

    @Autowired
    DataSite dataSite;

    @Autowired
    ContentService content;

    @Autowired
    RestCommentsApi commentsAPI;

    private UserModel userModel;
    private SiteModel siteModel;

    @BeforeClass
    public void setUp()
    {
        userModel = dataUser.getAdminUser();
        siteModel = dataSite.createPublicRandomSite();
        restClient.authenticateUser(userModel);
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
        sitesApi.useRestClient(restClient);

        sitesApi.getAllSites().assertThatResponseHasSite(siteModel.getId()).getSite(siteModel.getId())
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
        commentsAPI.useRestClient(restClient);

        File file = new File("textDocument-" + System.currentTimeMillis());
        Document document = content.createDocumentInRepository(userModel.getUsername(), 
                userModel.getPassword(), "Shared", DocumentType.TEXT_PLAIN, file, "This is a text file");

        // add new comment
        Content content = new Content("This is a new comment");
        RestCommentModel commentEntry = commentsAPI.addComment(document.getId(), content);
        commentsAPI.getNodeComments(document.getId()).assertThatResponseIsNotEmpty()
                .assertThatCommentWithIdExists(commentEntry.getId())
                .assertThatCommentWithContentExists(content);

        // update comment
        content = new Content("This is the updated comment");
        commentEntry = commentsAPI.updateComment(document.getId(), commentEntry.getId(), content);
        commentsAPI.getNodeComments(document.getId()).assertThatResponseIsNotEmpty()
                .assertThatCommentWithIdExists(commentEntry.getId())
                .assertThatCommentWithContentExists(content);
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
        sitesApi.useRestClient(restClient);

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