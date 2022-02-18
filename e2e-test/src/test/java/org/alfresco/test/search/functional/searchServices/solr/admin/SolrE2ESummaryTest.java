/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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
package org.alfresco.test.search.functional.searchServices.solr.admin;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotEquals;
import static org.testng.Assert.assertTrue;

import java.util.Random;
import java.util.UUID;
import java.util.stream.IntStream;

import org.alfresco.rest.core.RestResponse;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.testng.annotations.Test;

/** E2E tests for the SUMMARY admin report. */
public class SolrE2ESummaryTest extends AbstractE2EFunctionalTest
{
    /** The maximum time to wait for a report to update (in ms). */
    private static final int MAX_TIME = 60 * 1000;
    /** The frequency to check the report (in ms). */
    private static final int RETRY_INTERVAL = 100;

    /** Check the FTS section of the admin summary contains the expected fields. */
    @Test
    public void testFTSReport() throws Exception
    {
        RestResponse response = restClient.withParams("core=alfresco").withSolrAdminAPI().getAction("SUMMARY");

        int toUpdate = response.getResponse().body().jsonPath().get("Summary.alfresco.FTS.'Node count whose content needs to be updated'");
        assertTrue(toUpdate >= 0, "Expecting non-negative pieces of content to need updating.");

        int inSync = response.getResponse().body().jsonPath().get("Summary.alfresco.FTS.'Node count whose content is in sync'");
        assertTrue(inSync >= 0, "Expecting non-negative pieces of content to need updating.");
    }

    /** Check that we can spot a document updating by using the SUMMARY report. */
    @Test
    public void testFTSReport_contentUpdate() throws Exception
    {
        RestResponse response2 = restClient.withParams("core=alfresco").withSolrAdminAPI().getAction("SUMMARY");
        int previousInSync = response2.getResponse().body().jsonPath().get("Summary.alfresco.FTS.'Node count whose content is in sync'");

        FileModel file = new FileModel("file.txt", "file.txt", "", FileType.TEXT_PLAIN, "file.txt");
        FileModel content = dataContent.usingUser(adminUserModel).usingSite(testSite).createContent(file);

        // Wait for the number of "in-sync" documents to increase (i.e. when the document is indexed).
        Utility.sleep(RETRY_INTERVAL, MAX_TIME, () -> {
            RestResponse response = restClient.withParams("core=alfresco").withSolrAdminAPI().getAction("SUMMARY");
            int inSync = response.getResponse().body().jsonPath().get("Summary.alfresco.FTS.'Node count whose content is in sync'");
            assertTrue(inSync > previousInSync, "Expected a document to be indexed.");
        });

        // Wait for the number of outdated documents to become zero.
        Utility.sleep(RETRY_INTERVAL, MAX_TIME, () ->
        {
            RestResponse response = restClient.withParams("core=alfresco").withSolrAdminAPI().getAction("SUMMARY");
            int toUpdate = response.getResponse().body().jsonPath().get("Summary.alfresco.FTS.'Node count whose content needs to be updated'");
            assertEquals(toUpdate, 0, "Expected number of outdated documents to drop to zero.");
        });

        // Update the document's content with a large amount of text.
        StringBuilder largeText = new StringBuilder("Big update");
        IntStream.range(0, 100000).forEach((i) -> largeText.append(" ").append(UUID.randomUUID().toString()));
        dataContent.usingUser(adminUserModel).usingResource(content).updateContent(largeText.toString());

        // Expect to spot the number of outdated documents increase beyond zero.
        Utility.sleep(RETRY_INTERVAL, MAX_TIME, () ->
        {
            RestResponse response = restClient.withParams("core=alfresco").withSolrAdminAPI().getAction("SUMMARY");
            int toUpdate = response.getResponse().body().jsonPath().get("Summary.alfresco.FTS.'Node count whose content needs to be updated'");
            assertNotEquals(toUpdate, 0, "Expected number of outdated documents to be greater than zero.");
        });
    }
}
