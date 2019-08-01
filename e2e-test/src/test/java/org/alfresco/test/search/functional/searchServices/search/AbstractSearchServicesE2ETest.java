/*
 * Copyright (C) 2018 Alfresco Software Limited.
 * This file is part of Alfresco
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Lesser General Public License for more details.
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;

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

    protected FileModel file, file2, file3, file4;

    public void searchServicesDataPreparation()
    {
        /*
         * Create the following file structure for preconditions :
         * |- folder
         * |-- pangram.txt
         * |-- cars.txt
         * |-- alfresco.txt
         * |-- <uniqueFileName>
         */

        FolderModel folder = new FolderModel(SEARCH_DATA_SAMPLE_FOLDER);
        dataContent.usingUser(testUser).usingSite(testSite).createFolder(folder);

        // Create files
        String title = "Title: " + unique_searchString;
        String description = "Description: File is created for search tests by Author: " + unique_searchString + " . ";

        file = new FileModel("pangram.txt", "pangram" + title, description, FileType.TEXT_PLAIN,
                description + " The quick brown fox jumps over the lazy dog");

        file2 = new FileModel("cars.txt", "cars" + title, description, FileType.TEXT_PLAIN,
                "The landrover discovery is not a sports car ");

        file3 = new FileModel("alfresco.txt", "alfresco", "alfresco", FileType.TEXT_PLAIN,
                "Alfresco text file for search ");

        file4 = new FileModel(unique_searchString + ".txt", "uniquee" + title, description, FileType.TEXT_PLAIN,
                "Unique text file for search ");

        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(file);
        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(file2);
        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(file3);
        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(file4);

        waitForMetadataIndexing(file4.getName(), true);
    }
}
