package org.alfresco.service.search.e2e.insightEngine.sql;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.service.search.AbstractSearchServiceE2E;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.*;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.springframework.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CustoModelGroupByFacetableTest extends AbstractSearchServiceE2E
{

        protected SiteModel testSite;
        private UserModel testUser;
        private FolderModel testFolder;

        @BeforeClass(alwaysRun = true) public void initDataModel() throws Exception
        {

                serverHealth.assertServerIsOnline();
                deployCustomModel("model/facetable-aspect-model.xml");
                testSite = dataSite.createPublicRandomSite();

                // Create test user and add the user as a SiteContributor
                testUser = dataUser.createRandomTestUser();
                dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);
                testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();

                String fileName = "file1";
                String fileContent = "file content 1";
                String author = "Mario Rossi";
                Boolean exists = true;
                int quantity = 10;

                FileModel customFile = new FileModel(fileName, FileType.TEXT_PLAIN, fileContent);

                Map<String, Object> properties = new HashMap<String, Object>();
                properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
                properties.put(PropertyIds.NAME, customFile.getName());

                cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile, properties, VersioningState.MAJOR)
                        .assertThat().existsInRepo();

                cmisApi.authenticateUser(testUser).usingResource(customFile).addSecondaryTypes("P:csm:author").assertThat()
                        .secondaryTypeIsAvailable("P:csm:author");

                cmisApi.authenticateUser(testUser).usingResource(customFile).addSecondaryTypes("P:csm:nontexttypes").assertThat()
                        .secondaryTypeIsAvailable("P:csm:nontexttypes");

                cmisApi.authenticateUser(testUser).usingResource(customFile).updateProperty("csm:author", author);
                cmisApi.authenticateUser(testUser).usingResource(customFile).updateProperty("csm:quantity", quantity);
                cmisApi.authenticateUser(testUser).usingResource(customFile).updateProperty("csm:exists", exists);

                fileName = "file2";
                fileContent = "content file 2";
                author = "Giuseppe Verdi";
                quantity = 12;

                FileModel customFile2 = new FileModel(fileName, FileType.TEXT_PLAIN, fileContent);
                properties = new HashMap<String, Object>();
                properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
                properties.put(PropertyIds.NAME, customFile2.getName());

                cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile2, properties, VersioningState.MAJOR)
                        .assertThat().existsInRepo();

                cmisApi.authenticateUser(testUser).usingResource(customFile2).addSecondaryTypes("P:csm:author").assertThat()
                        .secondaryTypeIsAvailable("P:csm:author");
                cmisApi.authenticateUser(testUser).usingResource(customFile2).addSecondaryTypes("P:csm:nontexttypes").assertThat()
                        .secondaryTypeIsAvailable("P:csm:nontexttypes");
                cmisApi.authenticateUser(testUser).usingResource(customFile2).updateProperty("csm:author", author);
                cmisApi.authenticateUser(testUser).usingResource(customFile2).updateProperty("csm:quantity", quantity);
                cmisApi.authenticateUser(testUser).usingResource(customFile2).updateProperty("csm:exists", exists);

                fileName = "file3";
                fileContent = "content file 3";
                author = "Giuseppe Verdi";
                quantity = 12;

                FileModel customFile3 = new FileModel(fileName, FileType.TEXT_PLAIN, fileContent);
                properties = new HashMap<String, Object>();
                properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
                properties.put(PropertyIds.NAME, customFile3.getName());

                cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile3, properties, VersioningState.MAJOR)
                        .assertThat().existsInRepo();

                cmisApi.authenticateUser(testUser).usingResource(customFile3).addSecondaryTypes("P:csm:author").assertThat()
                        .secondaryTypeIsAvailable("P:csm:author");
                cmisApi.authenticateUser(testUser).usingResource(customFile3).addSecondaryTypes("P:csm:nontexttypes").assertThat()
                        .secondaryTypeIsAvailable("P:csm:nontexttypes");
                cmisApi.authenticateUser(testUser).usingResource(customFile3).updateProperty("csm:author", author);
                cmisApi.authenticateUser(testUser).usingResource(customFile3).updateProperty("csm:quantity", quantity);
                cmisApi.authenticateUser(testUser).usingResource(customFile3).updateProperty("csm:exists", exists);

                fileName = "file4";
                fileContent = "content file 4";
                exists = false;
                quantity = 9;

                FileModel customFile4 = new FileModel(fileName, FileType.TEXT_PLAIN, fileContent);
                properties = new HashMap<String, Object>();
                properties.put(PropertyIds.OBJECT_TYPE_ID, "cmis:document");
                properties.put(PropertyIds.NAME, customFile4.getName());

                cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder).createFile(customFile4, properties, VersioningState.MAJOR)
                        .assertThat().existsInRepo();

                cmisApi.authenticateUser(testUser).usingResource(customFile4).addSecondaryTypes("P:csm:nontexttypes").assertThat()
                        .secondaryTypeIsAvailable("P:csm:nontexttypes");
                cmisApi.authenticateUser(testUser).usingResource(customFile4).updateProperty("csm:quantity", quantity);
                cmisApi.authenticateUser(testUser).usingResource(customFile4).updateProperty("csm:exists", exists);

                // wait for indexing
                waitForIndexing("cm:name:'" + customFile4.getName() + "'", true);
                waitForIndexing("cm:name:'" + testFolder.getName() + "'", true);

        }

        @Test(priority = 1, groups = { TestGroup.INSIGHT_11 }) public void testGroupByTextFacetableModel() throws Exception
        {
                SearchSqlRequest sqlRequest = new SearchSqlRequest();
                sqlRequest.setSql("select cm_name, csm_author from alfresco group by cm_name, csm_author");

                RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);
                restClient.assertStatusCodeIs(HttpStatus.OK);

                restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("cm_name"));
                restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("csm_author"));

                restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase("file1"));
                restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalToIgnoringCase("file2"));
                restClient.onResponse().assertThat().body("list.entries.entry[2][0].value", Matchers.equalToIgnoringCase("file3"));

                restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase("Mario Rossi"));
                restClient.onResponse().assertThat().body("list.entries.entry[1][1].value", Matchers.equalToIgnoringCase("Giuseppe Verdi"));
                restClient.onResponse().assertThat().body("list.entries.entry[2][1].value", Matchers.equalToIgnoringCase("Giuseppe Verdi"));

                // Test changing the order of fields.
                sqlRequest.setSql("select csm_author, cm_name from alfresco group by csm_author, cm_name");

                response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);
                restClient.assertStatusCodeIs(HttpStatus.OK);

                restClient.onResponse().assertThat().body("list.pagination.count", Matchers.equalTo(3));

                restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("cm_name"));
                restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("csm_author"));

                restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase("file2"));
                restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalToIgnoringCase("file3"));
                restClient.onResponse().assertThat().body("list.entries.entry[2][0].value", Matchers.equalToIgnoringCase("file1"));

                restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase("Giuseppe Verdi"));
                restClient.onResponse().assertThat().body("list.entries.entry[1][1].value", Matchers.equalToIgnoringCase("Giuseppe Verdi"));
                restClient.onResponse().assertThat().body("list.entries.entry[2][1].value", Matchers.equalToIgnoringCase("Mario Rossi"));

        }

        @Test(priority = 1, groups = { TestGroup.INSIGHT_11 }) public void testGroupByTextInteger() throws Exception
        {
                SearchSqlRequest sqlRequest = new SearchSqlRequest();
                sqlRequest.setSql("select csm_quantity, csm_author from alfresco group by csm_quantity, csm_author");

                RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);
                restClient.assertStatusCodeIs(HttpStatus.OK);

                restClient.onResponse().assertThat().body("list.pagination.count", Matchers.equalTo(2));

                restClient.onResponse().assertThat().body("list.entries.entry[0][0].label", Matchers.equalToIgnoringCase("csm_author"));
                restClient.onResponse().assertThat().body("list.entries.entry[0][1].label", Matchers.equalToIgnoringCase("csm_quantity"));

                restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase("Mario Rossi"));
                restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase("10"));

                restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalToIgnoringCase("Giuseppe Verdi"));
                restClient.onResponse().assertThat().body("list.entries.entry[1][1].value", Matchers.equalToIgnoringCase("12"));

        }

        @Test(priority = 1, groups = { TestGroup.INSIGHT_11 }) public void testGroupByBoolAggregation() throws Exception
        {
                SearchSqlRequest sqlRequest = new SearchSqlRequest();
                sqlRequest.setSql("select csm_exists, max(csm_quantity) from alfresco group by csm_exists");

                RestResponse response = restClient.authenticateUser(testUser).withSearchSqlAPI().searchSql(sqlRequest);
                restClient.assertStatusCodeIs(HttpStatus.OK);

                restClient.onResponse().assertThat().body("list.pagination.count", Matchers.equalTo(2));

                restClient.onResponse().assertThat().body("list.entries.entry[0][0].value", Matchers.equalToIgnoringCase("false"));
                restClient.onResponse().assertThat().body("list.entries.entry[1][0].value", Matchers.equalToIgnoringCase("true"));
                restClient.onResponse().assertThat().body("list.entries.entry[0][1].value", Matchers.equalToIgnoringCase("9"));
                restClient.onResponse().assertThat().body("list.entries.entry[1][1].value", Matchers.equalToIgnoringCase("12"));

        }

}
