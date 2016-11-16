package org.alfresco.rest.demo;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestCommentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "demo" })
public class SampleCommentsTests extends RestTest
{    
    private UserModel userModel;
    private FolderModel folderModel;
    private SiteModel siteModel;
    private FileModel document;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.getAdminUser();
        siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        folderModel = dataContent.usingUser(userModel).usingSite(siteModel).createFolder();
        restClient.authenticateUser(userModel);        
        document = dataContent.usingUser(userModel).usingResource(folderModel).createContent(DocumentType.TEXT_PLAIN);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description= "Verify admin user adds comments with Rest API and status code is 200")
    public void admiShouldAddComment() throws JsonToModelConversionException, Exception
    {
        restClient.onCoreAPI().usingResource(document).addComment("This is a new comment");
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description= "Verify admin user gets comments with Rest API and status code is 200")
    public void admiShouldRetrieveComments() throws Exception
    {
        restClient.onCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }

    @TestRail(section={"demo", "sample-section"}, executionType= ExecutionType.SANITY,
            description= "Verify admin user updates comments with Rest API")
    public void adminShouldUpdateComment() throws JsonToModelConversionException, Exception
    {
        RestCommentModel commentModel = restClient.onCoreAPI().usingResource(document).addComment("This is a new comment");

        restClient.onCoreAPI().usingResource(document).updateComment(commentModel, "This is the updated comment with Collaborator user")
                    .assertThat().field("content").is("This is the updated comment with Collaborator user");
    }

}