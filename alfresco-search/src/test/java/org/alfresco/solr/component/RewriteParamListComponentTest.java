/*
 * Copyright (C) 2005-2017 Alfresco Software Limited.
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
package org.alfresco.solr.component;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.request.LocalSolrQueryRequest;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * Tests the RewriteParamListComponent
 *
 * If works on a comma seperated list of writers
 *
 * @author Gethin James
 */
public class RewriteParamListComponentTest {

    @Test
    public void testBasicRewrite() {
        RewriteParamListComponent rewriter = new RewriteParamListComponent();
        rewriter.init(new NamedList());
        rewriter.inform(null); //Use the defaults;

        assertEquals("",rewriter.rewrite(""));

        //Default include
        assertEquals("id,[cached]",rewriter.rewrite("id"));
        assertEquals("id,[cached]",rewriter.rewrite("id,[cached]"));
        assertEquals("id,[cached]",rewriter.rewrite("[cached],id"));
        assertEquals("*,[cached]", rewriter.rewrite("*"));

        //Default exclude
        assertEquals("id,[cached]",rewriter.rewrite("id,FIELDS"));
        assertEquals("id,[cached]",rewriter.rewrite("FIELDS,id"));
    }

    @Test
    public void testIncludes() {
        RewriteParamListComponent rewriter = new RewriteParamListComponent();
        NamedList nList = new NamedList();
        nList.add("includes", "bob");
        rewriter.init(nList);
        rewriter.inform(null);

        assertEquals("id,bob",rewriter.rewrite("id"));
        assertEquals("id,bob",rewriter.rewrite("id,bob"));
        assertEquals("id,bob",rewriter.rewrite("bob,id"));

        NamedList nextList = new NamedList();
        nextList.add("includes", "bob,bab");
        rewriter.init(nextList);
        rewriter.inform(null);

        assertEquals("id,bob,bab",rewriter.rewrite("id"));
        assertEquals("id,bob,bab",rewriter.rewrite("id,bob"));
        assertEquals("id,bob,bab",rewriter.rewrite("bob,id"));

    }

    @Test
    public void testExcludes() {
        RewriteParamListComponent rewriter = new RewriteParamListComponent();
        NamedList nList = new NamedList();
        nList.add("excludes", "bob");
        rewriter.init(nList);
        rewriter.inform(null);

        assertEquals("id,[cached]",rewriter.rewrite("id"));
        assertEquals("id,[cached]",rewriter.rewrite("id,bob"));
        assertEquals("id,[cached]",rewriter.rewrite("bob,id"));

        NamedList nextList = new NamedList();
        nextList.add("excludes", "bab");
        rewriter.init(nextList);
        rewriter.inform(null);

        assertEquals("id,[cached]",rewriter.rewrite("id"));
        assertEquals("id,[cached]",rewriter.rewrite("id,bab"));
        assertEquals("id,[cached]",rewriter.rewrite("bab,id"));
        assertEquals("id,bob,[cached]",rewriter.rewrite("bab,id,bob"));
        assertEquals("bob,id,[cached]",rewriter.rewrite("bob,bab,id"));

    }

    @Test
    public void testConfig() throws IOException {
        RewriteParamListComponent rewriter = new RewriteParamListComponent();
        rewriter.init(new NamedList());
        rewriter.inform(null); //Use the defaults;

        SolrQueryRequest request = req("fl", "id", "wt", "csv");
        rewriter.prepare(new ResponseBuilder(request, new SolrQueryResponse(), null));
        //Default include
        assertEquals("id,[cached]",request.getParams().get("fl"));

        rewriter = new RewriteParamListComponent();
        NamedList nList = new NamedList();
        nList.add("requestParam", "xy");
        rewriter.init(nList);
        rewriter.inform(null);

        request = req("fl", "id", "wt", "csv", "xy", "bob");
        rewriter.prepare(new ResponseBuilder(request, new SolrQueryResponse(), null));
        assertEquals("id",request.getParams().get("fl"));
        assertEquals("bob,[cached]",request.getParams().get("xy"));


        rewriter = new RewriteParamListComponent();
        nList = new NamedList();
        nList.add("writers", "xml,yaml");
        rewriter.init(nList);
        rewriter.inform(null);

        request = req("fl", "id", "wt", "csv");
        rewriter.prepare(new ResponseBuilder(request, new SolrQueryResponse(), null));
        //CSV writer is no long effected
        assertEquals("id",request.getParams().get("fl"));

        request = req("fl", "id", "wt", "YAML");
        rewriter.prepare(new ResponseBuilder(request, new SolrQueryResponse(), null));
        assertEquals("id,[cached]",request.getParams().get("fl"));
    }

    public static SolrQueryRequest req(String... params)
    {
        ModifiableSolrParams mp = new ModifiableSolrParams();
        for (int i=0; i<params.length; i+=2)
        {
            mp.add(params[i], params[i+1]);
        }
        return new LocalSolrQueryRequest(null, mp);
    }
}