package org.alfresco.rest.comments;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestCommentModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.utility.model.*;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Claudia Agache on 11/18/2016.
 */
@Test(groups = { TestGroup.REST_API, TestGroup.COMMENTS, TestGroup.CORE })
public class GetCommentsCoreTests extends RestTest
{
    private UserModel adminUserModel, userModel, networkUserModel;
    private FileModel document;
    private SiteModel siteModel;
    private String comment = "This is a new comment";
    private String comment2 = "This is a 2nd comment";
    private String comment3 = "This is a 3rd comment";
    private RestCommentModelsCollection comments;

    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        networkUserModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(adminUserModel).createPrivateRandomSite();
        document = dataContent.usingSite(siteModel).usingUser(adminUserModel).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingResource(document).addComment(comment);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingResource(document).addComment(comment2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        restClient.withCoreAPI().usingResource(document).addComment(comment3);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify invalid request returns status code 400 for invalid maxItems or skipCount")
    public void checkStatusCodeForInvalidMaxItems() throws Exception
    {
        restClient.authenticateUser(adminUserModel).withParams("maxItems=0")
                .withCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary("Only positive values supported for maxItems");
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify User can't get comments for node with ID that does not exist and status code is 404")
    public void userCanNotGetCommentsOnNonexistentFile() throws Exception
    {
        FileModel nonexistentFile = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        nonexistentFile.setNodeRef("ABC");
        restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingResource(nonexistentFile).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
                .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, nonexistentFile.getNodeRef()));
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify User can't get comments for node that exists but is not a document or a folder and status code is 400")
    @Bug(id = "MNT-16904")
    public void userCanNotGetCommentsOnLink() throws Exception
    {
        LinkModel link = dataLink.usingAdmin().usingSite(siteModel).createRandomLink();
        FileModel fileWithNodeRefFromLink = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        fileWithNodeRefFromLink.setNodeRef(link.getNodeRef());
        restClient.authenticateUser(adminUserModel).withCoreAPI()
                .usingResource(fileWithNodeRefFromLink).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
                .assertLastError().containsSummary(RestErrorModel.UNABLE_TO_LOCATE);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify request returns status 403 if the user does not have permission read comments on the node")
    public void uninvitedUserCanNotGetCommentsFromPrivateSite() throws Exception
    {
        restClient.authenticateUser(userModel).withCoreAPI()
                .usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN)
                .assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify user gets comments without the first 2 and status code is 200")
    public void skipFirst2Comments() throws Exception
    {
        comments = restClient.authenticateUser(adminUserModel).withParams("skipCount=2")
                .withCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        comments.assertThat().entriesListContains("content", comment)
                .and().paginationField("count").is("1");
        comments.assertThat().paginationField("skipCount").is("2");
        comments.assertThat().paginationField("totalItems").is("3");
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify get comments from node with invalid network id returns status code 401")
    public void getCommentsWithInvalidNetwork() throws Exception
    {
        networkUserModel.setDomain("invalidNetwork");
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

    @TestRail(section={TestGroup.REST_API, TestGroup.CORE, TestGroup.COMMENTS}, executionType= ExecutionType.REGRESSION,
            description= "Verify get comments from node with empty network id returns status code 401")
    public void getCommentsWithEmptyNetwork() throws Exception
    {
        networkUserModel.setDomain("");
        restClient.authenticateUser(userModel).withCoreAPI().usingResource(document).getNodeComments();
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN).assertLastError().containsSummary(RestErrorModel.PERMISSION_WAS_DENIED);
    }

}
