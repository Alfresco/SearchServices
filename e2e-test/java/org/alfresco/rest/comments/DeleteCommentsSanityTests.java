package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestCommentsApi;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "rest-api", "comments", "sanity" })
public class DeleteCommentsSanityTests extends RestTest
{

    @Autowired
    RestCommentsApi commentsAPI;
    
    @Autowired
    DataUser dataUser;
        
    private UserModel adminUserModel;
    
    private FileModel document;
    private SiteModel siteModel;
    private String commentId;
        
    @BeforeClass
    public void initTest() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        restClient.authenticateUser(adminUserModel);
        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        commentsAPI.useRestClient(restClient);
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(DocumentType.TEXT_PLAIN);
        commentId = commentsAPI.addComment(document.getNodeRef(), "This is a new comment").getId();
    }
    
    @TestRail(section={"rest-api", "comments"}, executionType= ExecutionType.SANITY,
            description= "Verify Admin user gets comments with Rest API and status code is 200")
    public void adminIsAbleToRetrieveComments() throws JsonToModelConversionException, Exception
    {
        commentsAPI.deleteComment(document.getNodeRef(), commentId);
        commentsAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.NO_CONTENT.toString());
    }

}
