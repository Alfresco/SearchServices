package org.alfresco.service.search.e2e.insightEngine.sql;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.service.search.AbstractSearchServiceE2E;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.*;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

public class CustoModelGroupByFacetableTest extends AbstractSearchServiceE2E
{

    protected SiteModel testSite;
    private UserModel testUser;
    private FolderModel testFolder;

    private String filename1;
    private String filename2;
    private String filename3;
    private String filename4;

    private String filecontent1;
    private String filecontent2;
    private String filecontent3;
    private String filecontent4;

    private String author1;
    private String author2;

    private Boolean exists;

    private int quantity1;
    private int quantity2;
    private int quantity3;

    @BeforeClass(alwaysRun = true)
    public void setupEnvironment() throws Exception
    {

        serverHealth.assertServerIsOnline();
        deployCustomModel("model/facetable-aspect-model.xml");
        testSite = dataSite.createPublicRandomSite();

        // Create test user and add the user as a SiteContributor
        testUser = dataUser.createRandomTestUser();
        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);
        testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

        filename1 = "file1";
        filename2 = "file2";
        filename3 = "file3";
        filename4 = "file4";

        filecontent1 = "file content 1";
        filecontent2 = "content file 2";
        filecontent3 = "content file 3";
        filecontent4 = "content file 4";

        author1 = "Giuseppe Verdi";
        author2 = "Mario Rossi";

        exists = true;

        quantity1 = 9;
        quantity2 = 10;
        quantity3 = 12;

        FileModel customFile = new FileModel(filename1, FileType.TEXT_PLAIN, filecontent1);

        Map<String, Object> properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, customFile.getName());

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile, properties, VersioningState.MAJOR).assertThat()
                .existsInRepo();

        cmisApi.authenticateUser(testUser).usingResource(customFile).addSecondaryTypes("P:csm:author").assertThat().secondaryTypeIsAvailable("P:csm:author");

        cmisApi.authenticateUser(testUser).usingResource(customFile).addSecondaryTypes("P:csm:nontexttypes").assertThat()
                .secondaryTypeIsAvailable("P:csm:nontexttypes");

        cmisApi.authenticateUser(testUser).usingResource(customFile).updateProperty("csm:author", author2);
        cmisApi.authenticateUser(testUser).usingResource(customFile).updateProperty("csm:quantity", quantity3);
        cmisApi.authenticateUser(testUser).usingResource(customFile).updateProperty("csm:exists", exists);

        FileModel customFile2 = new FileModel(filename2, FileType.TEXT_PLAIN, filecontent2);
        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, customFile2.getName());

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile2, properties, VersioningState.MAJOR).assertThat()
                .existsInRepo();

        cmisApi.authenticateUser(testUser).usingResource(customFile2).addSecondaryTypes("P:csm:author").assertThat().secondaryTypeIsAvailable("P:csm:author");
        cmisApi.authenticateUser(testUser).usingResource(customFile2).addSecondaryTypes("P:csm:nontexttypes").assertThat()
                .secondaryTypeIsAvailable("P:csm:nontexttypes");
        cmisApi.authenticateUser(testUser).usingResource(customFile2).updateProperty("csm:author", author1);
        cmisApi.authenticateUser(testUser).usingResource(customFile2).updateProperty("csm:quantity", quantity2);
        cmisApi.authenticateUser(testUser).usingResource(customFile2).updateProperty("csm:exists", exists);

        FileModel customFile3 = new FileModel(filename3, FileType.TEXT_PLAIN, filecontent3);
        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, customFile3.getName());

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile3, properties, VersioningState.MAJOR).assertThat()
                .existsInRepo();

        cmisApi.authenticateUser(testUser).usingResource(customFile3).addSecondaryTypes("P:csm:author").assertThat().secondaryTypeIsAvailable("P:csm:author");
        cmisApi.authenticateUser(testUser).usingResource(customFile3).addSecondaryTypes("P:csm:nontexttypes").assertThat()
                .secondaryTypeIsAvailable("P:csm:nontexttypes");
        cmisApi.authenticateUser(testUser).usingResource(customFile3).updateProperty("csm:author", author1);
        cmisApi.authenticateUser(testUser).usingResource(customFile3).updateProperty("csm:quantity", quantity2);
        cmisApi.authenticateUser(testUser).usingResource(customFile3).updateProperty("csm:exists", exists);

        FileModel customFile4 = new FileModel(filename4, FileType.TEXT_PLAIN, filecontent4);
        properties = new HashMap<String, Object>();
        properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
        properties.put(PropertyIds.NAME, customFile4.getName());

        cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile4, properties, VersioningState.MAJOR).assertThat()
                .existsInRepo();

        cmisApi.authenticateUser(testUser).usingResource(customFile4).addSecondaryTypes("P:csm:nontexttypes").assertThat()
                .secondaryTypeIsAvailable("P:csm:nontexttypes");
        cmisApi.authenticateUser(testUser).usingResource(customFile4).updateProperty("csm:quantity", quantity1);
        cmisApi.authenticateUser(testUser).usingResource(customFile4).updateProperty("csm:exists", !exists);

        // wait for indexing
        waitForIndexing("cm:name:'" + customFile4.getName() + "'", true);
        waitForIndexing("cm:name:'" + testFolder.getName() + "'", true);

    }

    @Test(priority = 1, groups = { TestGroup.INSIGHT_11 })
    public void testGroupByTextFacetableModel() throws Exception
    {
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name, csm_author from alfresco group by cm_name, csm_author");

        RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("cm_name"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("csm_author"));

        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase(filename1));
        restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalToIgnoringCase(filename2));
        restClient.onResponse().assertThat().body("list.entries.entry[2][0].value", Matchers.equalToIgnoringCase(filename3));

        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase(author2));
        restClient.onResponse().assertThat().body("list.entries.entry[1][1].value", Matchers.equalToIgnoringCase(author1));
        restClient.onResponse().assertThat().body("list.entries.entry[2][1].value", Matchers.equalToIgnoringCase(author1));

        // Test changing the order of fields.
        sqlRequest.setSql("select csm_author, cm_name from alfresco group by csm_author, cm_name");

        response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("list.pagination.count", Matchers.equalTo(3));

        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("cm_name"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("csm_author"));

        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase(filename2));
        restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalToIgnoringCase(filename3));
        restClient.onResponse().assertThat().body("list.entries.entry[2][0].value", Matchers.equalToIgnoringCase(filename1));

        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase(author1));
        restClient.onResponse().assertThat().body("list.entries.entry[1][1].value", Matchers.equalToIgnoringCase(author1));
        restClient.onResponse().assertThat().body("list.entries.entry[2][1].value", Matchers.equalToIgnoringCase(author2));

    }

    @Test(priority = 2, groups = { TestGroup.INSIGHT_11 })
    public void testGroupByTextInteger() throws Exception
    {
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select csm_quantity, csm_author from alfresco group by csm_quantity, csm_author");

        RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("list.pagination.count", Matchers.equalTo(2));

        restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("csm_author"));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("csm_quantity"));

        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase(Integer.toString(quantity2)));
        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase(author1));

        restClient.onResponse().assertThat().body("list.entries.entry[1][1].value", Matchers.equalToIgnoringCase(Integer.toString(quantity3)));
        restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalToIgnoringCase(author2));

    }

    @Test(priority = 3, groups = { TestGroup.INSIGHT_11 })
    public void testGroupByBoolAggregation() throws Exception
    {
        SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select csm_exists, max(csm_quantity) from alfresco group by csm_exists");

        RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("list.pagination.count", Matchers.equalTo(2));

        restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase(Boolean.toString(!exists)));
        restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalToIgnoringCase(Boolean.toString(exists)));
        restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase(Integer.toString(quantity1)));
        restClient.onResponse().assertThat().body("list.entries.entry[1][1].value", Matchers.equalToIgnoringCase(Integer.toString(quantity3)));

    }

}
