/*
 * Copyright (C) 2017 Alfresco Software Limited.
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
package org.alfresco.rest.search;

import java.util.ArrayList;
import java.util.List;

import org.alfresco.utility.model.TestGroup;
import org.testng.annotations.Test;

/**
 * Search high lighting test.
 * @author Michael Suzuki
 *
 */
public class SearchHighLightTest extends AbstractSearchTest
{
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API})
    public void searchWithHighLight() throws Exception
    {        
        RestRequestQueryModel queryReq =  new RestRequestQueryModel();
        queryReq.setQuery("description:workflow");
        queryReq.setUserQuery("workflow");
        
        RestRequestHighlightModel highlight = new RestRequestHighlightModel();
        highlight.setPrefix("¿");
        highlight.setPostfix("?");
        highlight.setMergeContiguous(true);
        List<RestRequestFieldsModel> fields = new ArrayList<RestRequestFieldsModel>();
        fields.add(new RestRequestFieldsModel("cm:title"));
        highlight.setFields(fields);
        SearchResponse nodes =  query(queryReq, highlight);
        nodes.assertThat().entriesListIsNotEmpty();
        ResponseHighLightModel hl = nodes.getEntryByIndex(0).getSearch().getHighlight().get(0);
        hl.assertThat().field("snippets").contains("Customized ¿Workflow? Process Definitions");
    }
    
    @Test(groups={TestGroup.SEARCH,TestGroup.REST_API})
    public void searchNonIndexedData() throws Exception
    {        
        RestRequestQueryModel queryReq =  new RestRequestQueryModel();
        queryReq.setQuery("cm:title");
        queryReq.setUserQuery("zoro");
        
        RestRequestHighlightModel highlight = new RestRequestHighlightModel();
        highlight.setPrefix("¿");
        highlight.setPostfix("?");
        highlight.setMergeContiguous(true);
        List<RestRequestFieldsModel> fields = new ArrayList<RestRequestFieldsModel>();
        fields.add(new RestRequestFieldsModel("cm:title"));
        highlight.setFields(fields);
        SearchResponse nodes =  query(queryReq, highlight);
        nodes.assertThat().entriesListDoesNotContain("highlight");
    }
}
