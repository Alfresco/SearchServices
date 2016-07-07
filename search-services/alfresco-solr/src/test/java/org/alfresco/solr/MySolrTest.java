//
//package org.alfresco.solr;
//
//import java.io.File;
//import java.io.IOException;
//
//import org.apache.solr.SolrJettyTestBase;
//import org.apache.solr.client.solrj.SolrClient;
//import org.apache.solr.client.solrj.SolrQuery;
//import org.apache.solr.client.solrj.SolrServerException;
//import org.apache.solr.client.solrj.embedded.JettyConfig;
//import org.apache.solr.client.solrj.embedded.JettySolrRunner;
//import org.apache.solr.client.solrj.embedded.SSLConfig;
//import org.apache.solr.client.solrj.request.UpdateRequest;
//import org.apache.solr.client.solrj.response.QueryResponse;
//import org.apache.solr.client.solrj.response.UpdateResponse;
//import org.apache.solr.common.SolrInputDocument;
//import org.apache.solr.common.params.ModifiableSolrParams;
//import org.apache.solr.common.params.SolrParams;
//import org.junit.After;
//import org.junit.Assert;
//import org.junit.Before;
//import org.junit.Test;
//import org.junit.runner.RunWith;
//
//import com.carrotsearch.randomizedtesting.RandomizedRunner;
//@RunWith(RandomizedRunner.class)
//public class MySolrTest extends SolrJettyTestBase
//{
//    JettySolrRunner solrRunner;
//    SolrClient client;
//
//    @After
//    public  void afterClass() throws Exception 
//    {
//        del("*:*");
//        solrRunner.stop();
//    }
//    protected void del(String q) throws Exception 
//    {
//        client.deleteByQuery(q);
//    }
//
//    protected void index_specific(int serverNumber, Object... fields) throws Exception 
//    {
//        SolrInputDocument doc = new SolrInputDocument();
//        for (int i = 0; i < fields.length; i += 2) {
//          doc.addField((String) (fields[i]), fields[i + 1]);
//        }
//
//        client.add(doc);
//    }
//    
//    protected void commit() throws SolrServerException, IOException
//    {
//        UpdateResponse res = client.commit();
//        System.out.println(res);
//    }
//    
//    protected UpdateResponse add(SolrClient client, SolrParams params, SolrInputDocument... sdocs) throws IOException, SolrServerException 
//    {
//        UpdateRequest ureq = new UpdateRequest();
//        ureq.setParams(new ModifiableSolrParams(params));
//        for (SolrInputDocument sdoc : sdocs)
//        {
//            ureq.add(sdoc);
//        }
//        return ureq.process(client);
//    }
//    @Before
//    public void setupData() throws Exception
//    {
//        System.setProperty("solr.solr.home", "target/test-classes/solr");
//        System.setProperty("solr.directoryFactory","solr.RAMDirectoryFactory");
//        System.setProperty("solr.tests.maxBufferedDocs", "1000");
//        System.setProperty("solr.tests.maxIndexingThreads", "10");
//        System.setProperty("solr.tests.ramBufferSizeMB", "1024");
//        System.setProperty("solr.data.dir","data");
//        File solrHome = new File("target/test-classes/solr");
//        SSLConfig sslConfig = new SSLConfig(false, false, null, null, null, null);
//        JettyConfig config = JettyConfig.builder().setContext("/solr").stopAtShutdown(true).setPort(8983).withSSLConfig(sslConfig).build();
//        solrRunner = createJetty(solrHome.toString(), config);
//        client = getSolrClient();
//        System.out.println(solrRunner.getLocalPort());
//        del("*:*");
//
//        SolrInputDocument document = new SolrInputDocument();
//        document.addField("id", "1");
//        document.addField("name", "Gouda cheese wheel");
//        client.add(document);
//        SolrInputDocument document2 = new SolrInputDocument();
//        document2.addField("id", "2");
//        document2.addField("name", "Blue cheese");
//        client.add(document2);
//        commit();
//        
//    }
//    @Test
//    public void testAll() throws Exception
//    {
//        SolrQuery q = new SolrQuery();
//        q.setQuery("*:*");
//        QueryResponse res = client.query(q);
//        Assert.assertEquals(2, res.getResults().getNumFound());
//    }
//    @Test
//    public void testBlueCheese() throws Exception
//    {
//        SolrQuery q = new SolrQuery();
//        q.setQuery("\"Blue cheese\"");
//        q.setFields("name");
//        QueryResponse res = client.query(q);
//        Assert.assertEquals(1, res.getResults().getNumFound());
//    }
//}
