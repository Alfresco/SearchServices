package org.alfresco.rest.v1;

import java.io.File;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.dataprep.ContentService;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.Content;
import org.alfresco.tester.data.DataUser;
import org.alfresco.tester.exception.DataPreparationException;
import org.alfresco.tester.model.UserModel;
import org.apache.chemistry.opencmis.client.api.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SampleCommentsTest extends RestTest
{
    @Autowired
    DataUser dataUser;

    @Autowired
    ContentService content;

    @Autowired
    RestCommentsApi commentsAPI;

    private UserModel userModel;
    private File file;
    private Document document;
    private String documentName = "textDocument-" + System.currentTimeMillis();

    @BeforeClass
    public void initTest() throws DataPreparationException
    {
        userModel = dataUser.getAdminUser();
        restClient.authenticateUser(userModel);
        commentsAPI.useRestClient(restClient);

        file = new File(documentName);
        document = content.createDocumentInRepository(userModel.getUsername(), userModel.getPassword(), "Shared", DocumentType.TEXT_PLAIN, file,
                "shared document content");
    }

    @Test
    public void addComments() throws JsonToModelConversionException
    {
        Content content = new Content("This is a new comment");
        commentsAPI.addCommentToNode(document.getId(), content);
        Assert.assertEquals(commentsAPI.usingRestWrapper().getStatusCode(), HttpStatus.CREATED.toString(), "Add comments response status code is not correct");

    }

    @Test
    public void getCommentsCheckStatusCode() throws JsonToModelConversionException
    {
        commentsAPI.getNodeComments(document.getId());
        Assert.assertEquals(commentsAPI.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get comments response status code is not correct");
    }

}