package org.alfresco.rest;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.UserModel;
import org.apache.chemistry.opencmis.client.api.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SampleCommentsTest extends RestTest
{
    @Autowired
    DataUser dataUser;

    @Autowired
    RestCommentsApi commentsAPI;

    private UserModel userModel;
    private Document document;

    @BeforeClass
    public void initTest() throws DataPreparationException
    {
        userModel = dataUser.getAdminUser();
        restClient.authenticateUser(userModel);
        commentsAPI.useRestClient(restClient);

        document = dataContent.usingPath("Shared")
                              .usingUser(userModel)
                              .createDocument(DocumentType.TEXT_PLAIN);
    }

    @Test
    public void addComments() throws JsonToModelConversionException, Exception
    {
        commentsAPI.addComment(document.getId(), "This is a new comment");
        commentsAPI.usingRestWrapper()
                   .assertStatusCodeIs(HttpStatus.CREATED.toString());
    }

    @Test
    public void getCommentsCheckStatusCode() throws JsonToModelConversionException
    {
        commentsAPI.getNodeComments(document.getId());
        commentsAPI.usingRestWrapper()
                   .assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @Test
    public void updateComment() throws JsonToModelConversionException, Exception
    {
        // add initial comment
        String commentId = commentsAPI.addComment(document.getId(), "This is a new comment").getId();

        // update comment
        RestCommentModel commentEntry = commentsAPI.updateComment(document.getId(), commentId, "This is the updated comment");
        commentEntry.assertCommentContentIs("This is the updated comment");
    }

}