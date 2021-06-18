/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */
package org.alfresco.test.search.functional;

import static com.google.common.collect.ImmutableMap.of;
import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.apache.chemistry.opencmis.commons.PropertyIds;
import org.apache.chemistry.opencmis.commons.enums.VersioningState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

/**
 * Base corpus for Exact Term tests.
 * SearchExactTerm tests, with and without cross locale configuration, are using this corpus
 * so results can be compared.
 */
public abstract class AbstractSearchExactTermTest extends AbstractE2EFunctionalTest
{
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;
    
    private static final DateFormat QUERY_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    protected String fromDate;
    protected String toDate;
    
    private void prepareExactSearchData(FolderModel testFolder) throws Exception {

        List<Map<String, String>> exactSearchData = asList(
                    // Document #1                    
                    of("name", "Running",
                        "description", "Running is a sport is a nice activity",
                        "content", "when you are running you are doing an amazing sport",
                        "title", "Running jumping"),
                    // Document #2                    
                    of("name", "Run",
                        "description", "you are supposed to run jump",
                        "content", "after many runs you are tired and if you jump it happens the same",
                        "title", "Run : a philosophy"),
                    // Document #3                    
                    of("name", "Poetry",
                        "description", "a document about poetry and jumpers",
                        "content", "poetry is unrelated to sport",
                        "title", "Running jumping twice jumpers"),
                    // Document #4                    
                    of("name", "Jump",
                        "description", "a document about jumps",
                        "content", "runnings jumpings",
                        "title", "Running"),
                    // Document #5                    
                    of("name", "Running jumping",
                        "description", "runners jumpers runs everywhere",
                        "content", "run is Good as jump",
                        "title", "Running the art of jumping"));
        
        // tok:true, tok:false and tok:both have a copy of the value in cm:title field

        List<FileModel> createdFileModels = new ArrayList<>();
        range(0, exactSearchData.size())
                .forEach(id -> {

                    Map<String, String> record = exactSearchData.get(id);

                    Map<String, Object> properties = new HashMap<>();
                    properties.put(PropertyIds.OBJECT_TYPE_ID, "D:tok:document");
                    properties.put(PropertyIds.NAME, record.get("name"));
                    properties.put("cm:title", record.get("title"));
                    properties.put("cm:description", record.get("description"));
                    properties.put("tok:true", record.get("title"));
                    properties.put("tok:false", record.get("title"));
                    properties.put("tok:both", record.get("title"));
                    properties.put(PropertyIds.SECONDARY_OBJECT_TYPE_IDS, List.of("P:cm:titled"));
                    
                    FileModel fileModel = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, record.get("content"));
                    fileModel.setName(record.get("name"));

                    cmisApi.authenticateUser(testUser).usingSite(testSite).usingResource(testFolder)
                                .createFile(fileModel, properties, VersioningState.MAJOR)
                                .assertThat().existsInRepo();
                    
                    createdFileModels.add(fileModel);
                    
                });
        
        waitForContentIndexing(createdFileModels.get(createdFileModels.size() - 1).getName(), true);
        
    }
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        serverHealth.assertServerIsOnline();
        
        deployCustomModel("model/tokenised-model.xml");
        
        dataUser.addUserToSite(testUser, testSite, UserRole.SiteContributor);

        FolderModel testFolder = dataContent.usingSite(testSite).usingUser(testUser).createFolder();
        prepareExactSearchData(testFolder);
        
        // Calculate time query range, required for conjunction queries
        Date today = new Date();
        
        LocalDateTime yesterday = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        yesterday = yesterday.plusDays(-1);
        fromDate = QUERY_DATE_FORMAT.format(Date.from(yesterday.atZone(ZoneId.systemDefault()).toInstant()));
        
        LocalDateTime tomorrow = today.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
        tomorrow = tomorrow.plusDays(1);
        toDate = QUERY_DATE_FORMAT.format(Date.from(tomorrow.atZone(ZoneId.systemDefault()).toInstant()));
        
    }
    
    protected void assertResponseCardinality(String query, int num)
    {
        SearchResponse response = queryAsUser(testUser, query);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), num, query);
    }
    
    protected void assertException(String query)
    {
        queryAsUser(testUser, query);
        Assert.assertTrue(
            restClient.getStatusCode().equals(String.valueOf(HttpStatus.NOT_IMPLEMENTED)) ||
            restClient.getStatusCode().equals(String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR)), 
            "Status code is not as expected.");
    }

}
