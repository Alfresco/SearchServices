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

package org.alfresco.test.search.functional.searchServices.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.alfresco.rest.search.*;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.report.Bug;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import static org.testng.AssertJUnit.assertEquals;

/**
 * Search high lighting test.
 * 
 * @author Michael Suzuki
 */
public class SearchHighLightTest extends AbstractSearchServicesE2ETest
{
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        searchServicesDataPreparation();
        FileModel fileHl = new FileModel("very long name", "some title", "description", FileType.TEXT_PLAIN,
                "Content of long name ");
        dataContent.usingUser(testUser).usingSite(testSite).usingResource(folder).createContent(fileHl);
        waitForMetadataIndexing(fileHl.getName(), true);
    }

    @Test
    @Bug(id = "TAS-3220")
    public void searchWithHighLight() throws Exception
    {
        waitForContentIndexing(file2.getContent(), true);

        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:content:cars");
        queryReq.setUserQuery("cars");

        RestRequestHighlightModel highlight = new RestRequestHighlightModel();
        highlight.setPrefix("¿");
        highlight.setPostfix("?");
        highlight.setMergeContiguous(true);
        List<RestRequestFieldsModel> fields = new ArrayList<>();
        fields.add(new RestRequestFieldsModel("cm:content"));
        highlight.setFields(fields);
        SearchResponse nodes = query(queryReq, highlight);
        nodes.assertThat().entriesListIsNotEmpty();
        ResponseHighLightModel hl = nodes.getEntryByIndex(0).getSearch().getHighlight().get(0);
        hl.assertThat().field("snippets").contains("The landrover discovery is not a sports ¿car?");
    }

    @Test
    public void searchNonIndexedData() throws Exception
    {
        RestRequestQueryModel queryReq = new RestRequestQueryModel();
        queryReq.setQuery("cm:title");
        queryReq.setUserQuery("zoro");

        RestRequestHighlightModel highlight = new RestRequestHighlightModel();
        highlight.setPrefix("¿");
        highlight.setPostfix("?");
        highlight.setMergeContiguous(true);
        List<RestRequestFieldsModel> fields = new ArrayList<>();
        fields.add(new RestRequestFieldsModel("cm:title"));
        highlight.setFields(fields);
        SearchResponse nodes = query(queryReq, highlight);
        nodes.assertThat().entriesListDoesNotContain("highlight");
    }

    @Test
    public void searchDataConjunctionGenericQuery() throws Exception
    {
        RestRequestQueryModel queryReq = new RestRequestQueryModel();

        queryReq.setQuery("very AND name");

        RestRequestHighlightModel highlight = new RestRequestHighlightModel();
        highlight.setPrefix("¿");
        highlight.setPostfix("?");
        highlight.setMergeContiguous(false);
        List<RestRequestFieldsModel> fields = new ArrayList<>();
        fields.add(new RestRequestFieldsModel("cm:name"));
        highlight.setFields(fields);
        SearchResponse nodes = query(queryReq, highlight);
        nodes.assertThat().entriesListDoesNotContain("highlight");

        String expectedHighlight = "¿very? long ¿name?";

        assertEquals(1, nodes.getEntries().size());

        nodes.getEntries().stream()
                .map(SearchNodeModel::getModel)
                .map(SearchNodeModel::getSearch)
                .map(SearchScoreModel::getHighlight).forEach(( hl -> {
                    assertEquals(1, hl.size());
                    assertEquals(expectedHighlight, hl.get(0).getSnippets().get(0));
                })
        );

    }
}
