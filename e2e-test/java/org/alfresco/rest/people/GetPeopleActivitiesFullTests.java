package org.alfresco.rest.people;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestActivityModelsCollection;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestSiteModelsCollection;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * 
 * @author Cristina Axinte
 *
 */
public class GetPeopleActivitiesFullTests extends RestTest
{
    UserModel userModel, managerUser;
    SiteModel siteModel1, siteModel2;
    FileModel fileInSite1, fileInSite2;
    FolderModel folderInSite2;
    private RestActivityModelsCollection restActivityModelsCollection;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        siteModel1 = dataSite.usingUser(userModel).createPublicRandomSite();
        fileInSite1 = dataContent.usingUser(userModel).usingSite(siteModel1).createContent(DocumentType.TEXT_PLAIN);
        
        siteModel2 = dataSite.usingUser(userModel).createPublicRandomSite();
        folderInSite2 = dataContent.usingUser(userModel).usingSite(siteModel2).createFolder(); 
        fileInSite2 = dataContent.usingAdmin().usingSite(siteModel2).createContent(DocumentType.TEXT_PLAIN);   
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.ACTIVITIES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.ACTIVITIES }, executionType = ExecutionType.REGRESSION, description = "Verify user gets its activities using me with Rest API and response is successful")
    public void userShouldGetItsPeopleActivitiesUsingMe() throws Exception
    {
        restActivityModelsCollection = restClient.authenticateUser(userModel).withCoreAPI().usingMe().getPersonActivities();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restActivityModelsCollection.assertThat().paginationField("count").is("3");
        restActivityModelsCollection.assertThat().entriesListContains("title", fileInSite1.getName())
                .and().entriesListContains("title", folderInSite2.getName())
                .and().entriesListContains("title", fileInSite2.getName());
    }
    
    @Test(groups = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.ACTIVITIES, TestGroup.FULL })
    @TestRail(section = { TestGroup.REST_API, TestGroup.PEOPLE, TestGroup.ACTIVITIES }, executionType = ExecutionType.REGRESSION, description = "Verify user cannot get activities for empty user with Rest API and response is 404")
    public void userCannotGetPeopleActivitiesForEmptyPersonId() throws Exception
    {
        UserModel emptyUserName = new UserModel("", "password");
        
        restActivityModelsCollection = restClient.authenticateUser(userModel).withCoreAPI().usingUser(emptyUserName).getPersonActivities();
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsErrorKey(RestErrorModel.ENTITY_NOT_FOUND_ERRORKEY)
                                .containsSummary(RestErrorModel.LOCAL_NAME_CONSISTANCE)
                                .stackTraceIs(RestErrorModel.STACKTRACE)
                                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }
}
