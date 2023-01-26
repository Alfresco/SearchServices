/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2023 Alfresco Software Limited
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

package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.rest.exception.EmptyJsonResponseException;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.http.HttpStatus;
import org.testng.Assert;

import static java.util.List.of;

/**
 * Abstract Search test class that contains useful methods
 * such as:
 * <ul>
 * <li>Preparing the data to index.
 * <li>Preparing search requests.
 *
 * @author Michael Suzuki
 * @author Meenal Bhave
 */
public abstract class AbstractSearchServicesE2ETest extends AbstractE2EFunctionalTest
{
    private static final String SEARCH_DATA_SAMPLE_FOLDER = "FolderSearch";
    private static final int MAX_ATTEMPTS_TO_RETRY_QUERY = 10;
    private static final int MAX_WAIT_IN_SECONDS_BEFORE_RETRY_QUERY = 5;
    private static final int MAX_ATTEMPTS_TO_READ_RESPONSE = 10;
    private static final int MAX_WAIT_IN_SECONDS_BEFORE_REREAD_RESPONSE = 2;

    /** The maximum time to wait for content indexing to complete (in ms). */
    private static final int MAX_TIME = 120 * 1000;
    /** The frequency to check the report (in ms). */
    private static final int RETRY_INTERVAL = 30000;

    protected FileModel file, file2, file3, file4;
    protected FolderModel folder;

    public void searchServicesDataPreparation() throws InterruptedException
    {
        /*
         * Create the following file structure for preconditions :
         * |- folder
         * |-- pangram.txt
         * |-- cars.txt
         * |-- alfresco.txt
         * |-- <uniqueFileName>
         */

        folder = new FolderModel(SEARCH_DATA_SAMPLE_FOLDER);
        dataContent.usingUser(testUser).usingSite(testSite).createFolder(folder);

        // Create files
        String title = "Title: " + unique_searchString;
        String description = "Description: File is created for search tests by Author: " + unique_searchString + " . ";

        file = new FileModel("pangram.txt", "pangram" + title, description, FileType.TEXT_PLAIN,
                description + " The quick brown fox jumps over the lazy dog");

        file2 = new FileModel("cars.PDF", "cars", description, FileType.TEXT_PLAIN,
                "The landrover discovery is not a sports car");

        file3 = new FileModel("alfresco.docx", "alfresco", "alfresco", FileType.TEXT_PLAIN,
                "Alfresco text file for search ");

        file4 = new FileModel(unique_searchString + ".ODT", "uniquee" + title, description, FileType.TEXT_PLAIN,
                "Unique text file for search ");


        of(file, file2, file3, file4).forEach(
                f -> dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(f)
                                             );
        waitForMetadataIndexing(file4.getName(), true);
    }

    protected FileModel createFileWithProvidedText(String filename, String providedText) throws InterruptedException
    {
        String title = "Title: File containing " + providedText;
        String description = "Description: Contains provided string: " + providedText;
        FileModel uniqueFile = new FileModel(filename, title, description, FileType.TEXT_PLAIN,
                "The content " + providedText + " is a provided string");
        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(uniqueFile);
        Assert.assertTrue(waitForContentIndexing(providedText, true));

        return uniqueFile;
    }

    protected SearchResponse queryUntilResponseEntriesListNotEmpty(UserModel user, String queryString)
    {
       SearchResponse response = queryUntilStatusIsOk(user, queryString);
       if (restClient.getStatusCode().matches(HttpStatus.OK.toString()))
       {
           for (int readAttempts = 0; readAttempts < MAX_ATTEMPTS_TO_READ_RESPONSE; readAttempts++)
           {
               if (!response.isEmpty())
               {
                   return response;
               }
               Utility.waitToLoopTime(MAX_WAIT_IN_SECONDS_BEFORE_REREAD_RESPONSE, "Re-reading empty response. Retry Attempt: " + (readAttempts + 1));
           }
       }

       return response;
    }

    private SearchResponse queryUntilStatusIsOk(UserModel user, String queryString)
    {
        // Repeat query until status is OK or Query Retry limit is hit
        for (int queryAttempts = 0; queryAttempts < MAX_ATTEMPTS_TO_RETRY_QUERY - 1; queryAttempts++)
        {
            try
            {
                SearchResponse response = queryAsUser(user, queryString);
                if (restClient.getStatusCode().matches(HttpStatus.OK.toString()))
                {
                    return response;
                }

                // Wait for pipeline to calm down
                Utility.waitToLoopTime(MAX_WAIT_IN_SECONDS_BEFORE_RETRY_QUERY, "Re-trying query for valid status code. Retry Attempt: " + (queryAttempts + 1));
            }
            catch (EmptyJsonResponseException ignore)
            {
            }
        }
        // Final attempt
        return queryAsUser(user, queryString);
    }
}
