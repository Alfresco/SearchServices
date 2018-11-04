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

import java.util.UUID;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.search.AbstractSearchTest;
import org.alfresco.rest.search.SearchSqlRequest;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.model.*;
import org.alfresco.utility.report.Bug;
import org.hamcrest.Matchers;
import org.springframework.http.HttpStatus;
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

    private String songName;
    private String genre;
    private String coProducer;

    private String artistName;
    private String voiceType;

    private String bassistName;
    private String drummerName;
    private String saxophonistName;

    private FileModel file5;

    /**
     * Setup fixture for this test case.
     * Overrides the superlayer method because the data preparation requires a bit different preconditions.
     * The method uses a transient test site created in the {@link RestTest#checkServerHealth()} and on top of that:
     *
     * <ul>
     *     <li>It adds a user which is added to the site as contributor</li>
     *     <li>
     *         It deploys a custom model which declares a set of prefixes composed by a combination of digits, hyphens
     *         and underscores. Those prefixes are associated with four entities (song, artist, bassist, drummer and
     *         sax) with their corresponding attributes.
     *     </li>
     *     <li>
     *         It creates a folder with 5 files associated with the types declared in the model.
     *         That allows those files to have a value for the properties/attributes included in the model definition.
     *     </li>
     * </ul>
     *
     * @see RestTest#checkServerHealth()
     * @throws Exception hopefully never, otherwise the test fails.
     */
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        songName = "The Dry Cleaner from Des Moines ";
        genre = "Jazz, vocal jazz ";
        coProducer = "Roberta Joan Mitchell ";

        artistName = "Joni Mitchell " + UUID.randomUUID();
        voiceType = "Blue Mezzo (1965-1984) / Cloudy Contralto (1985-present) ";

        bassistName = "Jaco Pastorius";
        drummerName = "Peter Erskine";
        saxophonistName = "Wayne Shorter";

        userModel = dataUser.createRandomTestUser();

        dataContent.usingAdmin().deployContentModel("models/SEARCH-1063.xml");

        dataUser.addUserToSite(userModel, testSite, UserRole.SiteContributor);

        FolderModel testFolder = dataContent.usingSite(testSite).usingUser(userModel).createFolder();

        file = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        file2 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        file3 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        file4 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);
        file5 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN);

        dataContent.usingUser(userModel)
            .usingResource(testFolder)
            .createCustomContent(
                    file,
                    "D:1:song",
                    new CustomObjectTypeProperties()
                        .addProperty("1:name", songName)
                        .addProperty("1:genre", genre)
                        .addProperty("1:co-producer", coProducer));

        dataContent.usingUser(userModel)
                .usingResource(testFolder)
                .createCustomContent(
                        file2,
                        "D:123:artist",
                        new CustomObjectTypeProperties()
                                .addProperty("123:name", artistName)
                                .addProperty("123:voice_type", voiceType));

        dataContent.usingUser(userModel)
                .usingResource(testFolder)
                .createCustomContent(
                        file3,
                        "D:1_2_3:bassist",
                        new CustomObjectTypeProperties()
                                .addProperty("1_2_3:name", bassistName));

        dataContent.usingUser(userModel)
                .usingResource(testFolder)
                .createCustomContent(
                        file4,
                        "D:1-2-3:drummer",
                        new CustomObjectTypeProperties()
                                .addProperty("1-2-3:name", drummerName));

        ContentModel content =
                dataContent.usingUser(userModel)
                    .usingResource(testFolder)
                    .createCustomContent(
                        file5,
                        "D:1-2_3:saxophonist",
                        new CustomObjectTypeProperties()
                                .addProperty("1-2_3:name", saxophonistName));

        waitForIndexing(content.getName(), true);
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    @Bug(id = "SEARCH-1063")
    public void prefixIsComposedByOneNumber() throws Exception
    {
        executeQuery("select cm_name, `1_name`, `1_genre`, `1_co-producer` from alfresco where TYPE='1:song' and SITE='" + testSite.getId() + "'");

        restClient.onResponse().assertThat().body("result-set.docs[0].cm_name", Matchers.equalTo(file.getName()));
        restClient.onResponse().assertThat().body("result-set.docs[0].1_name", Matchers.equalTo(songName));
        restClient.onResponse().assertThat().body("result-set.docs[0].1_genre", Matchers.equalTo(genre));
        restClient.onResponse().assertThat().body("result-set.docs[0].1_co-producer", Matchers.equalTo(coProducer));
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    @Bug(id = "SEARCH-1063")
    public void prefixIsComposedByMultipleNumbers() throws Exception
    {
        executeQuery("select cm_name, `123_name`, `123_voice_type` from alfresco where TYPE='123:artist' and SITE='" + testSite.getId() + "'");

        restClient.onResponse().assertThat().body("result-set.docs[0].cm_name", Matchers.equalTo(file2.getName()));
        restClient.onResponse().assertThat().body("result-set.docs[0].123_name", Matchers.equalTo(artistName));
        restClient.onResponse().assertThat().body("result-set.docs[0].123_voice_type", Matchers.equalTo(voiceType));
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    @Bug(id = "SEARCH-1063")
    public void prefixIncludesUnderscore() throws Exception
    {
        executeQuery("select cm_name, `1_2_3_name` from alfresco where TYPE='1_2_3:bassist' and SITE='" + testSite.getId() + "'");

        restClient.onResponse().assertThat().body("result-set.docs[0].cm_name", Matchers.equalTo(file3.getName()));
        restClient.onResponse().assertThat().body("result-set.docs[0].1_2_3_name", Matchers.equalTo(bassistName));
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    @Bug(id = "SEARCH-1063")
    public void prefixIncludesHyphen() throws Exception
    {
        executeQuery("select cm_name, `1-2-3_name` from alfresco where TYPE='1-2-3:drummer' and SITE='" + testSite.getId() + "'");

        restClient.onResponse().assertThat().body("result-set.docs[0].cm_name", Matchers.equalTo(file4.getName()));
        restClient.onResponse().assertThat().body("result-set.docs[0].1-2-3_name", Matchers.equalTo(drummerName));
    }

    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API, TestGroup.ASS_1})
    @Bug(id = "SEARCH-1063")
    public void prefixIncludesHyphenAndUnderscore() throws Exception
    {
        executeQuery("select cm_name, `1-2_3_name` from alfresco where TYPE='1-2_3:saxophonist' and SITE='" + testSite.getId() + "'");

        restClient.onResponse().assertThat().body("result-set.docs[0].cm_name", Matchers.equalTo(file5.getName()));
        restClient.onResponse().assertThat().body("result-set.docs[0].1-2_3_name", Matchers.equalTo(saxophonistName));
    }

    /**
     * Internal method for executing a SQL Query.
     *
     * TODO: maybe would be better to move this method (and similar methods) on a SQL supertype layer (e.g. AbstractSqlSearchTest)
     *
     * @param sql the SQL statement.
     */
    private void executeQuery(String sql) throws Exception
    {
        SearchSqlRequest sqlRequest = new SearchSqlRequest(sql, "solr");

        restClient.authenticateUser(userModel).withSearchSqlAPI().searchSql(sqlRequest);
        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}