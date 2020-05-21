/*-
 * #%L
 * Alfresco Solr Search
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
package org.alfresco.solr;

import static org.junit.Assert.fail;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.handler.admin.CoreAdminHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.BeforeClass;
import org.junit.Test;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AdminHandlerIT extends AbstractAlfrescoSolrIT
{

    static CoreAdminHandler admin;

    @BeforeClass
    public static void beforeClass() throws Exception {
        initAlfrescoCore("schema.xml");
        admin = getMultiCoreHandler();
    }

    @Test(expected = SolrException.class)
    public void testUnhandled() throws Exception
    {
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "totalnonsense",
                CoreAdminParams.NAME, getCore().getName()),
                resp);
    }

    @Test
    public void testHandledCores()
    {
        try
        {
            requestAction("newCore");
            requestAction("updateCore");
            requestAction("removeCore");
        }
        catch (Exception e)
        {
            fail("Expected to receive no exception, but got " + e);
        }
    }

    @Test
    public void testHandledReports()
    {
        try
        {
            requestAction("CHECK");
            requestAction("REPORT");
            requestAction("SUMMARY");
        }
        catch (Exception e)
        {
            fail("Expected to receive no exception, but got " + e);
        }
    }

    private void requestAction(String actionName) throws Exception {
        admin.handleRequestBody(req(CoreAdminParams.ACTION, actionName,
                CoreAdminParams.NAME, getCore().getName()),
                new SolrQueryResponse());
    }
}
