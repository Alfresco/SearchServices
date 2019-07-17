/*
 * Copyright (C) 2019 Alfresco Software Limited.
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
package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.report.Bug;
import org.testng.annotations.Test;

import static org.testng.Assert.assertTrue;

/**
 *
 * @author Elia Porciani
 */
public class SpecialCharacterSearchTest extends AbstractE2EFunctionalTest
{

    /**
     *
     * Index a file with \u007F (delete char) in name.
     * The goal is to check that the file is actually indexed in solr.
     * @throws Exception
     */
    @Test(groups = { TestGroup.ACS_52n, TestGroup.ACS_60n, TestGroup.ACS_61n })
    @Bug(id = "MNT-20507")
    public void testIndexDELChar() throws Exception
    {
        FileModel file = new FileModel("Delete char\u007Ffile", FileType.TEXT_PLAIN, "content of \u007F file");
        dataContent.usingUser(testUser).usingSite(testSite).createContent(file);
        assertTrue(waitForIndexing("name:'" + file.getName()+"'", true));
    }

}

