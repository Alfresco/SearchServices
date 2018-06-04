package org.alfresco.rest.discovery;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.List;

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

    @Test(groups = { TestGroup.REST_API, TestGroup.DISCOVERY, TestGroup.ALL_AMPS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.DISCOVERY }, executionType = ExecutionType.SANITY,
            description = "Sanity tests for GET /discovery endpoint")
    public void getRepositoryInstalledModules() throws Exception
    {
        // Get repository info using Discovery API
        restClient.authenticateUser(userModel).withDiscoveryAPI().getRepositoryInfo();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        // Check that all modules are present
        List<String> modules = restClient.onResponse().getResponse().jsonPath().getList("entry.repository.modules.id", String.class);
        assertTrue(modules.contains("alfresco-aos-module"));
        assertTrue(modules.contains("org.alfresco.integrations.google.docs"));
        assertTrue(modules.contains("org_alfresco_integrations_S3Connector"));
        assertTrue(modules.contains("org_alfresco_module_xamconnector"));
        assertTrue(modules.contains("org.alfresco.module.KofaxAddon"));
        assertTrue(modules.contains("alfresco-content-connector-for-salesforce-repo"));
        assertTrue(modules.contains("alfresco-share-services"));
        assertTrue(modules.contains("alfresco-saml-repo"));
        assertTrue(modules.contains("org_alfresco_device_sync_repo"));
        assertTrue(modules.contains("org_alfresco_mm_repo"));

        // Check that all installed modules are in INSTALLED state
        List<String> modulesStates = restClient.onResponse().getResponse().jsonPath().getList("entry.repository.modules.installState", String.class);
        assertEquals(Collections.frequency(modulesStates, "INSTALLED"), 10);
    }
}
