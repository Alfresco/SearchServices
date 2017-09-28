/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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

package org.alfresco.solr.query;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;


@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class AlfrescoReRankQParserPluginTest extends AbstractAlfrescoSolrTests
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema-rerank.xml");
    }
    @Before
    public void setUp() throws Exception
    {
        
        // if you override setUp or tearDown, you better call
        // the super classes version
        clearIndex();
        assertU(commit());
    }

    @Test
    public void testReRankQueries() throws Exception {

        assertU(delQ("*:*"));
        assertU(commit());

        String[] doc = {"id","1", "term_s", "YYYY", "group_s", "group1", "test_ti", "5", "test_tl", "10", "test_tf", "2000"};
        assertU(adoc(doc));
        assertU(commit());
        String[] doc1 = {"id","2", "term_s","YYYY", "group_s", "group1", "test_ti", "50", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc1));

        String[] doc2 = {"id","3", "term_s", "YYYY", "test_ti", "5000", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc2));
        assertU(commit());
        String[] doc3 = {"id","4", "term_s", "YYYY", "test_ti", "500", "test_tl", "1000", "test_tf", "2000"};
        assertU(adoc(doc3));

        String[] doc4 = {"id","5", "term_s", "YYYY", "group_s", "group2", "test_ti", "4", "test_tl", "10", "test_tf", "2000"};
        assertU(adoc(doc4));
        assertU(commit());
        String[] doc5 = {"id","6", "term_s","YYYY", "group_s", "group2", "test_ti", "10", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc5));
        assertU(commit());


        Random random = new Random();
        boolean scale = random.nextBoolean();

        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=200 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "term_s:YYYY");
        params.add("rqq", "{!edismax bf=$bff}*:*");
        params.add("bff", "field(test_ti)");
        params.add("start", "0");
        params.add("rows", "6");
        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='3.0']",
                "//result/doc[2]/float[@name='id'][.='4.0']",
                "//result/doc[3]/float[@name='id'][.='2.0']",
                "//result/doc[4]/float[@name='id'][.='6.0']",
                "//result/doc[5]/float[@name='id'][.='1.0']",
                "//result/doc[6]/float[@name='id'][.='5.0']"
        );

        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "{!edismax bq=$bqq2}*:*");
        params.add("bqq2", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='2.0']",
                "//result/doc[2]/float[@name='id'][.='6.0']",
                "//result/doc[3]/float[@name='id'][.='5.0']",
                "//result/doc[4]/float[@name='id'][.='4.0']",
                "//result/doc[5]/float[@name='id'][.='3.0']",
                "//result/doc[6]/float[@name='id'][.='1.0']"
        );

        //Test with sort by score.
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "{!edismax bq=$bqq2}*:*");
        params.add("bqq2", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");
        params.add("sort", "score desc");
        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='2.0']",
                "//result/doc[2]/float[@name='id'][.='6.0']",
                "//result/doc[3]/float[@name='id'][.='5.0']",
                "//result/doc[4]/float[@name='id'][.='4.0']",
                "//result/doc[5]/float[@name='id'][.='3.0']",
                "//result/doc[6]/float[@name='id'][.='1.0']"
        );


        //Test with compound sort.
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "{!edismax bq=$bqq2}*:*");
        params.add("bqq2", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");
        params.add("sort", "score desc,test_ti asc");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='2.0']",
                "//result/doc[2]/float[@name='id'][.='6.0']",
                "//result/doc[3]/float[@name='id'][.='5.0']",
                "//result/doc[4]/float[@name='id'][.='4.0']",
                "//result/doc[5]/float[@name='id'][.='3.0']",
                "//result/doc[6]/float[@name='id'][.='1.0']"
        );


        //Test with elevation
        /*

        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 reRankWeight=50 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "{!edismax bq=$bqq2}*:*");
        params.add("bqq2", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");
        params.add("qt", "/elevate");
        params.add("elevateIds", "1");
        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='1.0']",
                "//result/doc[2]/float[@name='id'][.='2.0']",
                "//result/doc[3]/float[@name='id'][.='6.0']",
                "//result/doc[4]/float[@name='id'][.='5.0']",
                "//result/doc[5]/float[@name='id'][.='4.0']",
                "//result/doc[6]/float[@name='id'][.='3.0']"

        );
        */


        //Test TermQuery rqq
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 reRankWeight=2 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='2.0']",
                "//result/doc[2]/float[@name='id'][.='6.0']",
                "//result/doc[3]/float[@name='id'][.='5.0']",
                "//result/doc[4]/float[@name='id'][.='4.0']",
                "//result/doc[5]/float[@name='id'][.='3.0']",
                "//result/doc[6]/float[@name='id'][.='1.0']"
        );


        //Test Elevation
        /*
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 reRankWeight=2 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");
        params.add("qt","/elevate");
        params.add("elevateIds", "1,4");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='1.0']", //Elevated
                "//result/doc[2]/float[@name='id'][.='4.0']", //Elevated
                "//result/doc[3]/float[@name='id'][.='2.0']", //Boosted during rerank.
                "//result/doc[4]/float[@name='id'][.='6.0']",
                "//result/doc[5]/float[@name='id'][.='5.0']",
                "//result/doc[6]/float[@name='id'][.='3.0']"
        );
        */

        //Test Elevation swapped
        /*
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 reRankWeight=2 scale="+scale+"}");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");
        params.add("qt","/elevate");
        params.add("elevateIds", "4,1");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='4.0']", //Elevated
                "//result/doc[2]/float[@name='id'][.='1.0']", //Elevated
                "//result/doc[3]/float[@name='id'][.='2.0']", //Boosted during rerank.
                "//result/doc[4]/float[@name='id'][.='6.0']",
                "//result/doc[5]/float[@name='id'][.='5.0']",
                "//result/doc[6]/float[@name='id'][.='3.0']"
        );
        */

        /*
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=4 reRankWeight=2 scale="+scale+"}");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");
        params.add("qt","/elevate");
        params.add("elevateIds", "4,1");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='4.0']", //Elevated
                "//result/doc[2]/float[@name='id'][.='1.0']", //Elevated
                "//result/doc[3]/float[@name='id'][.='6.0']",
                "//result/doc[4]/float[@name='id'][.='5.0']",
                "//result/doc[5]/float[@name='id'][.='3.0']",
                "//result/doc[6]/float[@name='id'][.='2.0']"  //Not in reRankeDocs
        );

        */


        //Test Elevation with start beyond the rerank docs
        /*
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=3 reRankWeight=2 scale="+scale+"}");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "4");
        params.add("rows", "10");
        params.add("qt","/elevate");
        params.add("elevateIds", "4,1");

        assertQ(req(params), "*[count(//doc)=2]",
                "//result/doc[1]/float[@name='id'][.='3.0']",
                "//result/doc[2]/float[@name='id'][.='2.0']"  //Was not in reRankDocs
        );
        */
        //Test Elevation with zero results
        /*
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=3 reRankWeight=2 scale="+scale+"}");
        params.add("q", "{!edismax bq=$bqq1}nada");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "4");
        params.add("rows", "10");
        params.add("qt","/elevate");
        params.add("elevateIds", "4,1");

        assertQ(req(params), "*[count(//doc)=0]");
        */


        //Pass in reRankDocs lower then the length being collected.
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=1 reRankWeight=2 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='6.0']",
                "//result/doc[2]/float[@name='id'][.='5.0']",
                "//result/doc[3]/float[@name='id'][.='4.0']",
                "//result/doc[4]/float[@name='id'][.='3.0']",
                "//result/doc[5]/float[@name='id'][.='2.0']",
                "//result/doc[6]/float[@name='id'][.='1.0']"
        );

        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=0 reRankWeight=2 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='6.0']",
                "//result/doc[2]/float[@name='id'][.='5.0']",
                "//result/doc[3]/float[@name='id'][.='4.0']",
                "//result/doc[4]/float[@name='id'][.='3.0']",
                "//result/doc[5]/float[@name='id'][.='2.0']",
                "//result/doc[6]/float[@name='id'][.='1.0']"
        );

        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=2 reRankWeight=2 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:4^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "10");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='5.0']",
                "//result/doc[2]/float[@name='id'][.='6.0']",
                "//result/doc[3]/float[@name='id'][.='4.0']",
                "//result/doc[4]/float[@name='id'][.='3.0']",
                "//result/doc[5]/float[@name='id'][.='2.0']",
                "//result/doc[6]/float[@name='id'][.='1.0']"
        );

        //Test reRankWeight of 0, reranking will have no effect.
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 reRankWeight=0 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "5");

        assertQ(req(params), "*[count(//doc)=5]",
                "//result/doc[1]/float[@name='id'][.='6.0']",
                "//result/doc[2]/float[@name='id'][.='5.0']",
                "//result/doc[3]/float[@name='id'][.='4.0']",
                "//result/doc[4]/float[@name='id'][.='3.0']",
                "//result/doc[5]/float[@name='id'][.='2.0']"
        );



        //Test range query
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "test_ti:[0 TO 2000]");
        params.add("rqq", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "6");

        assertQ(req(params), "*[count(//doc)=5]",
                "//result/doc[1]/float[@name='id'][.='6.0']",
                "//result/doc[2]/float[@name='id'][.='5.0']",
                "//result/doc[3]/float[@name='id'][.='4.0']",
                "//result/doc[4]/float[@name='id'][.='2.0']",
                "//result/doc[5]/float[@name='id'][.='1.0']"
        );
        
        //Test range query embedded in larger query
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "*:* OR test_ti:[0 TO 2000]");
        params.add("rqq", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "6");

        assertQ(req(params), "*[count(//doc)=6]",
                "//result/doc[1]/float[@name='id'][.='6.0']",
                "//result/doc[2]/float[@name='id'][.='5.0']",
                "//result/doc[3]/float[@name='id'][.='4.0']",
                "//result/doc[4]/float[@name='id'][.='3.0']",
                "//result/doc[5]/float[@name='id'][.='2.0']",
                "//result/doc[6]/float[@name='id'][.='1.0']"
        );


        //Test with start beyond reRankDocs
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=3 reRankWeight=2 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60");
        params.add("rqq", "id:1^1000");
        params.add("fl", "id,score");
        params.add("start", "4");
        params.add("rows", "5");

        assertQ(req(params), "*[count(//doc)=2]",
                "//result/doc[1]/float[@name='id'][.='2.0']",
                "//result/doc[2]/float[@name='id'][.='1.0']"
        );

        //Test ReRankDocs > docs returned

        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=6 reRankWeight=2 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50");
        params.add("rqq", "id:1^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "1");

        assertQ(req(params), "*[count(//doc)=1]",
                "//result/doc[1]/float[@name='id'][.='1.0']"
        );



        //Test with zero results
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=3 reRankWeight=2 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "term_s:NNNN");
        params.add("rqq", "id:1^1000");
        params.add("fl", "id,score");
        params.add("start", "4");
        params.add("rows", "5");

        assertQ(req(params), "*[count(//doc)=0]");

    }

    @Test
    public void testOverRank() throws Exception {

        assertU(delQ("*:*"));
        assertU(commit());

        //Test the scenario that where we rank more documents then we return.

        boolean scale = new Random().nextBoolean();

        String[] doc = {"id","1", "term_s", "YYYY", "group_s", "group1", "test_ti", "5", "test_tl", "10", "test_tf", "2000"};
        assertU(adoc(doc));
        
        String[] doc1 = {"id","2", "term_s","YYYY", "group_s", "group1", "test_ti", "50", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc1));

        String[] doc2 = {"id","3", "term_s", "YYYY", "test_ti", "5000", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc2));
        String[] doc3 = {"id","4", "term_s", "YYYY", "test_ti", "500", "test_tl", "1000", "test_tf", "2000"};
        assertU(adoc(doc3));


        String[] doc4 = {"id","5", "term_s", "YYYY", "group_s", "group2", "test_ti", "4", "test_tl", "10", "test_tf", "2000"};
        assertU(adoc(doc4));

        String[] doc5 = {"id","6", "term_s","YYYY", "group_s", "group2", "test_ti", "10", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc5));

        String[] doc6 = {"id","7", "term_s", "YYYY", "group_s", "group1", "test_ti", "5", "test_tl", "10", "test_tf", "2000"};
        assertU(adoc(doc6));


        String[] doc7 = {"id","8", "term_s","YYYY", "group_s", "group1", "test_ti", "50", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc7));

        String[] doc8 = {"id","9", "term_s", "YYYY", "test_ti", "5000", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc8));
        String[] doc9 = {"id","10", "term_s", "YYYY", "test_ti", "500", "test_tl", "1000", "test_tf", "2000"};
        assertU(adoc(doc9));

        String[] doc10 = {"id","11", "term_s", "YYYY", "group_s", "group2", "test_ti", "4", "test_tl", "10", "test_tf", "2000"};
        assertU(adoc(doc10));
        assertU(commit());


        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=11 reRankWeight=2 scale="+scale+"}");
        params.add("df", "TEXT");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60 id:7^70 id:8^80 id:9^90 id:10^100 id:11^110");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "2");

        assertQ(req(params), "*[count(//doc)=2]",
                "//result/doc[1]/float[@name='id'][.='8.0']",
                "//result/doc[2]/float[@name='id'][.='2.0']"
        );

        //Test Elevation
        /*
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=11 reRankWeight=2 scale="+scale+"}");
        params.add("q", "{!edismax bq=$bqq1}*:*");
        params.add("bqq1", "id:1^10 id:2^20 id:3^30 id:4^40 id:5^50 id:6^60 id:7^70 id:8^80 id:9^90 id:10^100 id:11^110");
        params.add("rqq", "test_ti:50^1000");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "3");
        params.add("qt","/elevate");
        params.add("elevateIds", "1,4");

        assertQ(req(params), "*[count(//doc)=3]",
                "//result/doc[1]/float[@name='id'][.='1.0']", //Elevated
                "//result/doc[2]/float[@name='id'][.='4.0']", //Elevated
                "//result/doc[3]/float[@name='id'][.='8.0']"); //Boosted during rerank.
                */

    }

    @Test
    public void testScale() throws Exception {

        assertU(delQ("*:*"));
        assertU(commit());

        String[] doc = {"id", "1", "term_s", "YYYY", "group_s", "group1", "test_ti", "5", "test_tl", "10", "test_tf", "2000"};
        assertU(adoc(doc));
        assertU(commit());
        String[] doc1 = {"id", "2", "term_s", "YYYY", "group_s", "group1", "test_ti", "50", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc1));

        String[] doc2 = {"id", "3", "term_s", "YYYY", "test_ti", "5000", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc2));
        assertU(commit());
        String[] doc3 = {"id", "4", "term_s", "YYYY", "test_ti", "500", "test_tl", "1000", "test_tf", "2000"};
        assertU(adoc(doc3));

        String[] doc4 = {"id", "5", "term_s", "YYYY", "group_s", "group2", "test_ti", "4", "test_tl", "10", "test_tf", "2000"};
        assertU(adoc(doc4));
        assertU(commit());
        String[] doc5 = {"id", "6", "term_s", "YYYY", "group_s", "group2", "test_ti", "10", "test_tl", "100", "test_tf", "200"};
        assertU(adoc(doc5));
        assertU(commit());


        //Calculate the scales manually
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=200 scale=false}");
        params.add("df", "TEXT");
        params.add("q", "term_s:YYYY");
        params.add("rqq", "{!edismax bf=$bff}id:(1 2 4 5 6)");
        params.add("bff", "field(test_ti)");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "6");

        SolrQueryRequest req = req(params);
        SolrQueryResponse res = null;

        try {
            res = h.queryAndResponse(null, req);
        } finally {
            req.close();
        }

        @SuppressWarnings("rawtypes")
        NamedList vals = res.getValues();
        ResultContext resultContext = (ResultContext)vals.get("response");

        DocList docs = resultContext.getDocList();
        DocIterator it = docs.iterator();
        float max = -Float.MAX_VALUE;
        List<Float> scores = new ArrayList<Float>();

        while(it.hasNext()) {
            it.next();
            float score = it.score();
            max = Math.max(score, max);
            scores.add(score);
        }


        float[] scaledScores = new float[scores.size()];

        for(int i=0; i<scaledScores.length; i++) {
            float score = scores.get(i);
            if(i<5) {
                //The first 5 docs are hit on the reRanker so add 1 to score
                scaledScores[i] = (score / max) + 1;
            } else {
                //The last score is not a hit on the reRanker
                scaledScores[i] = (score / max);
            }
        }

        //Get the scaled scores from the reRanker
        params = new ModifiableSolrParams();
        params.add("rq", "{!alfrescoReRank reRankQuery=$rqq reRankDocs=200 scale=true}");
        params.add("df", "TEXT");
        params.add("q", "term_s:YYYY");
        params.add("rqq", "{!edismax bf=$bff}id:(1 2 4 5 6)");
        params.add("bff", "field(test_ti)");
        params.add("fl", "id,score");
        params.add("start", "0");
        params.add("rows", "6");

        req = req(params);
        try {
            res = h.queryAndResponse(null, req);
        }finally {
            req.close();
        }
        vals = res.getValues();
        resultContext = (ResultContext)vals.get("response");
        docs = resultContext.getDocList();
        it = docs.iterator();

        int index = 0;
        while(it.hasNext()) {
            it.next();
            float score = it.score();
            float scaledScore = scaledScores[index++];
            assertTrue(score == scaledScore);
        }

        req.close();
    }
    
    /**
     * Fix for SEARCH-296, handling too many rows.
     * @throws Exception if error
     */
    @Test
    public void testInsaneAmoutOfRows() throws Exception 
    {
        String[] doc = {"id","1", "term_s", "YYYY", "group_s", "group1", "test_ti", "5", "test_tl", "10", "test_tf", "2000"};
        assertU(adoc(doc));
        assertU(commit());

        //Request with lots of rows 200000001
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("qt", "/afts");
        params.add("df", "TEXT");
        params.add("q", "term_s:YYYY");
        params.add("rows", "200000001");
        params.add("rq","{!alfrescoReRank reRankQuery=$rqq reRankDocs=3 reRankWeight=2 }");
        params.add("rqq", "{!edismax bf=$bff}*:*");
        params.add("bff", "field(test_ti)");
        assertQ(req(params), "*[count(//doc)=1]");
    }
 
}
