package org.alfresco.rest.discovery;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.model.RestDiscoveryModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class DiscoveryTests extends RestTest
{
    private UserModel adminModel, userModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {  
        adminModel = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.DISCOVERY, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.DISCOVERY }, executionType = ExecutionType.SANITY,
            description = "Sanity tests for GET /discovery endpoint")
    public void getRepositoryInformation() throws Exception
    {
        // Get repository info from admin console
        RestResponse adminConsoleRepoInfo = restClient.authenticateUser(adminModel).withAdminConsole().getAdminConsoleRepoInfo();
        String id = adminConsoleRepoInfo.getResponse().getBody().path("data.id");
        String edition = adminConsoleRepoInfo.getResponse().getBody().path("data.edition");
        String schema = adminConsoleRepoInfo.getResponse().getBody().path("data.schema");
        String version = adminConsoleRepoInfo.getResponse().getBody().path("data.version");

        // Get repository info using Discovery API
        RestDiscoveryModel response = restClient.authenticateUser(userModel).withDiscoveryAPI().getRepositoryInfo();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Compare information
        response.getRepository().getVersion().assertThat().field("major").is(version.charAt(0));
        response.getRepository().getVersion().assertThat().field("minor").is(version.charAt(2));
        response.getRepository().getVersion().assertThat().field("patch").is(version.charAt(4));
        response.getRepository().getVersion().assertThat().field("schema").is(schema);
        response.getRepository().getId().equals(id);
        response.getRepository().getEdition().equals(edition);
        response.getRepository().getStatus().assertThat().field("isReadOnly").is(false);
    }
}
