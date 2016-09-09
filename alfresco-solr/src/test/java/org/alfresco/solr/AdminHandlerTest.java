package org.alfresco.solr;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.SolrException;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.CoreAdminParams;
import org.apache.solr.handler.admin.CoreAdminHandler;
import org.apache.solr.response.SolrQueryResponse;
import org.junit.BeforeClass;
import org.junit.Test;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AdminHandlerTest extends AbstractAlfrescoSolrTests {

    static CoreAdminHandler admin;

    @BeforeClass
    public static void beforeClass() throws Exception {
        initAlfrescoCore("schema.xml");
        admin = h.getCoreContainer().getMultiCoreHandler();
    }

    @Test(expected = SolrException.class)
    public void testUnhandled() throws Exception
    {
        SolrQueryResponse resp = new SolrQueryResponse();
        admin.handleRequestBody(req(CoreAdminParams.ACTION, "totalnonsense",
                CoreAdminParams.NAME, h.getCore().getName()),
                resp);
    }

    @Test
    public void testhandledCores() throws Exception
    {
        requestAction("newCore");
        requestAction("updateCore");
        requestAction("removeCore");
    }

    @Test
    public void testhandledReports() throws Exception
    {
        requestAction("CHECK");
        requestAction("REPORT");
        requestAction("SUMMARY");
    }

    private void requestAction(String actionName) throws Exception {
        admin.handleRequestBody(req(CoreAdminParams.ACTION, actionName,
                CoreAdminParams.NAME, h.getCore().getName()),
                new SolrQueryResponse());
    }
}
