package org.alfresco.rest.ratings.get;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestRatingModelsCollection;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class GetRatingsFullTests extends RestTest
{
    private SiteModel siteModel;
    private UserModel adminUserModel, userModel;
    private FileModel document;
    private RestRatingModelsCollection ratingsModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws DataPreparationException
    {
        adminUserModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
        siteModel = dataSite.usingAdmin().createPublicRandomSite();
    }

    @BeforeMethod(alwaysRun = true)
    public void setUp() throws DataPreparationException, Exception
    {
        document = dataContent.usingSite(siteModel).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        restClient.authenticateUser(userModel).withCoreAPI().usingNode(document).rateStarsToDocument(5);
        restClient.authenticateUser(userModel).withCoreAPI().usingNode(document).likeDocument();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Check default error schema in case of failure")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void checkDefaultErrorSchema() throws Exception
    {
        FileModel document = dataContent.usingSite(siteModel).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document.setNodeRef("abc");
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, "abc"))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Check maxItems and skipCount parameters")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void checkMaxItemsAndSkipCountParameters() throws Exception
    {
        ratingsModel = restClient.authenticateUser(adminUserModel).withParams("maxItems=1", "skipCount=1").withCoreAPI().usingResource(document).getRatings();
        ratingsModel.assertThat().entriesListCountIs(1);
        ratingsModel.getPagination().assertThat().field("maxItems").is("1")
            .and().field("skipCount").is("1");
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Check totalItems and hasMoreitems parameters")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void checkTotalItemsAndHasMoreItemsParameters() throws Exception
    {
        ratingsModel = restClient.authenticateUser(adminUserModel).withParams("maxItems=1").withCoreAPI().usingResource(document).getRatings();
        ratingsModel.assertThat().entriesListCountIs(1);
        ratingsModel.getPagination().assertThat().field("hasMoreItems").is("true")
            .and().field("totalItems").is("2");
    }
    
    @Bug(id = "REPO-1831")
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Get ratings for a document to which authenticated user does not have access")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void userIsNotAbleToGetRatingsOfDocumentToWhichItHasNoAccess() throws Exception
    {
        SiteModel privateSite = dataSite.usingAdmin().createPrivateRandomSite();
        FileModel file = dataContent.usingSite(privateSite).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        ratingsModel = restClient.authenticateUser(userModel).withCoreAPI().usingResource(file).getRatings();
        ratingsModel.assertThat().entriesListIsEmpty();
    }

    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Check high value for skipCount parameter")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getRatingsUsingHighValueForSkipCount() throws Exception
    {
        ratingsModel = restClient.authenticateUser(adminUserModel).withParams("skipCount=100").withCoreAPI().usingResource(document).getRatings();
        ratingsModel.getPagination().assertThat().field("skipCount").is("100");
        ratingsModel.assertThat().entriesListIsEmpty();
    }
    
    @TestRail(section = { TestGroup.REST_API, TestGroup.RATINGS }, executionType = ExecutionType.REGRESSION, 
            description = "Get ratings using site id instead of node id")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.FULL })
    public void getRatingsUsingSiteId() throws Exception
    {
        FileModel document = dataContent.usingSite(siteModel).usingAdmin().createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        document.setNodeRef(siteModel.getId());
        restClient.authenticateUser(adminUserModel).withCoreAPI().usingResource(document).getRatings();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND).assertLastError()
            .containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
            .containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, siteModel.getId()))
            .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER)
            .stackTraceIs(RestErrorModel.STACKTRACE);
    }
}