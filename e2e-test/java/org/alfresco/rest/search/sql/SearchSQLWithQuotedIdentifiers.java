/*
 * Copyright (C) 2018 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.rest.search.sql;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.search.AbstractSearchTest;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.model.*;
import org.alfresco.utility.report.Bug;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Tests for /sql end point Search API when a custom model contains identifiers that need to be quoted in queries.
 * While it seems there is not an ANSI standard for naming database objects, on the other side the most popular
 * databases don't like columns starting with a number.
 * In those cases, the column identifier need to be quoted. How to quote a given object identifier (a column name, in
 * this case) depends on the specific database lexicon: Oracle uses double quotes ("), MySQL uses back quotes(`).
 *
 * At time of writing, the lexicon used by the /sql endpoint is MySql so queries that use column names starting with
 * a number need to be quoted with back quotes.
 *
 */
public class SearchSQLWithQuotedIdentifiers extends AbstractSearchTest
{
    private final String songName = "Got a match?";
    private final String genre = "Fusion";

    /**
     * Setup fixture for this test case.
     * Overrides the superlayer method because the data preparation requires a bit different preconditions.
     *
     * @throws Exception hopefully never, otherwise the test fails.
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();

        dataContent.usingAdmin().deployContentModel("models/SEARCH-1063.xml");

        userModel = dataUser.createRandomTestUser();

        dataUser.addUserToSite(userModel, testSite, UserRole.SiteContributor);

        final FolderModel testFolder = dataContent.usingSite(testSite).usingUser(userModel).createFolder();

        file = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "Some text content.");

        final CustomObjectTypeProperties attributes = new CustomObjectTypeProperties();
//        attributes.addProperty(PropertyIds.NAME, file.getName());
        attributes.addProperty("1:name", songName);
        attributes.addProperty("1:genre", genre);

        final ContentModel content =
                dataContent.usingUser(userModel)
                        .usingResource(testFolder)
                        .createCustomContent(file, "D:1:song", attributes);

        waitForIndexing(content.getName(), true);
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    @Bug(id = "SEARCH-1063")
    public void testSearchUsingCustomAttributeStartingWithNumber() throws Exception
    {
        final SearchSqlRequest sqlRequest = new SearchSqlRequest();
        sqlRequest.setSql("select cm_name, `1_name`, `1_genre` from alfresco where TYPE='1:song'");
        sqlRequest.setFormat("solr");
        sqlRequest.setLocales(new String[] { "en-US" });
        sqlRequest.setIncludeMetadata(false);

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.onResponse().assertThat().body("result-set.docs[0].cm_name", Matchers.equalTo(file.getName()));
        restClient.onResponse().assertThat().body("result-set.docs[0].1_name", Matchers.equalTo(songName));
        restClient.onResponse().assertThat().body("result-set.docs[0].1_genre", Matchers.equalTo(genre));
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() throws Exception
    {
        dataContent.usingSite(testSite).usingUser(dataUser.getAdminUser()).deleteSite(testSite);
    }
}