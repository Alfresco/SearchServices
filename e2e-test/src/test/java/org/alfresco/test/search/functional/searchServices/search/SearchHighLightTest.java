/*
 * Copyright (C) 2017 Alfresco Software Limited.
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

import java.util.ArrayList;
import java.util.List;

import org.alfresco.rest.search.ResponseHighLightModel;
import org.alfresco.rest.search.RestRequestFieldsModel;
import org.alfresco.rest.search.RestRequestHighlightModel;
import org.alfresco.rest.search.RestRequestQueryModel;
import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.report.Bug;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

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
}
