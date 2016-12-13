package org.alfresco.rest.tags;

import org.alfresco.dataprep.CMISUtil;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestTagModel;
import org.alfresco.utility.Utility;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Created by Bogdan Bocancea
 */
public class UpdateTagCoreTests extends RestTest
{
    private UserModel managerUser;
    private FileModel document;
    private SiteModel siteModel;
    @SuppressWarnings("unused")
    private DataUser.ListUserWithRoles usersWithRoles;
    @SuppressWarnings("unused")
    private RestTagModel oldTag;
    
    @BeforeClass(alwaysRun=true)
    public void dataPreparation() throws Exception
    {
        managerUser = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(managerUser).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).usingUser(managerUser).createContent(CMISUtil.DocumentType.TEXT_PLAIN);
        usersWithRoles = dataUser.addUsersWithRolesToSite(siteModel, UserRole.SiteCollaborator, UserRole.SiteConsumer, UserRole.SiteContributor);

        oldTag = restClient.authenticateUser(managerUser).withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("old"));
    }

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify site manager is not able to update tag with invalid id")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void managerIsNotAbleToUpdateTagWithInvalidId() throws JsonToModelConversionException, Exception
    {
        String invalidTagId = "invalid-id";
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag"));
        tag.setId(invalidTagId);
        restClient.withCoreAPI().usingTag(tag).update(RandomData.getRandomName("tag"));
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND)
            .assertLastError().containsSummary(String.format(RestErrorModel.ENTITY_NOT_FOUND, invalidTagId));
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify site manager is not able to update tag with empty id")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void managerIsNotAbleToUpdateTagWithEmptyId() throws JsonToModelConversionException, Exception
    {
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag"));
        tag.setId("");
        restClient.withCoreAPI().usingTag(tag).update(RandomData.getRandomName("tag"));
        restClient.assertStatusCodeIs(HttpStatus.METHOD_NOT_ALLOWED)
                .assertLastError().containsSummary(RestErrorModel.PUT_EMPTY_ARGUMENT);
    }
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, description = "Verify site manager is not able to update tag with invalid body")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void managerIsNotAbleToUpdateTagWithEmptyBody() throws JsonToModelConversionException, Exception
    {
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag"));
        restClient.withCoreAPI().usingTag(tag).update("");
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(RestErrorModel.EMPTY_TAG);
    }
    
    @Bug(id="ACE-5629")
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify site manager is not able to update tag with invalid body containing '|' symbol")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void managerIsNotAbleToUpdateTagWithInvalidBodyScenario1() throws JsonToModelConversionException, Exception
    {
        String invalidTagBody = "|.\"/<>*";
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag"));
        Utility.waitToLoopTime(20);
        restClient.withCoreAPI().usingTag(tag).update(invalidTagBody);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_TAG, invalidTagBody));
    }
    
    @Bug(id="ACE-5629")
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.TAGS }, executionType = ExecutionType.REGRESSION, 
            description = "Verify site manager is not able to update tag with invalid body without '|' symbol")
    @Test(groups = { TestGroup.REST_API, TestGroup.TAGS, TestGroup.CORE })
    public void managerIsNotAbleToUpdateTagWithInvalidBodyScenario2() throws JsonToModelConversionException, Exception
    {
        String invalidTagBody = ".\"/<>*";
        RestTagModel tag = restClient.withCoreAPI().usingResource(document).addTag(RandomData.getRandomName("tag"));
        Utility.waitToLoopTime(20);
        restClient.withCoreAPI().usingTag(tag).update(invalidTagBody);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST)
            .assertLastError().containsSummary(String.format(RestErrorModel.INVALID_TAG, invalidTagBody));
    }
}
