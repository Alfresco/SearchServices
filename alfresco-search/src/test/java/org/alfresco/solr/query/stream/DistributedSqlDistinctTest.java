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
package org.alfresco.solr.query.stream;

import java.util.List;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.client.solrj.io.Tuple;
import org.junit.Rule;
import org.junit.Test;

/**
 * @author Michael Suzuki
 */
@SolrTestCaseJ4.SuppressSSL
@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
public class DistributedSqlDistinctTest extends AbstractStreamTest
{
    private String alfrescoJson = "{ \"authorities\": [ \"jim\", \"joel\" ], \"tenants\": [ \"\" ] }";
    
    @Rule
    public JettyServerRule jetty = new JettyServerRule(2, this);

    @Test
    public void testSearch() throws Exception
    {
        List<Tuple> tuples = sqlQuery("select distinct ACLID from alfresco where `cm:content` = 'world' limit 10", alfrescoJson);
        assertTrue(tuples.size() == 2);
        assertFalse(tuples.get(0).get("ACLID").toString().equals(tuples.get(1).get("ACLID").toString()));

        tuples = sqlQuery("select distinct ACLID from alfresco limit 10", alfrescoJson);
        assertTrue(tuples.size() == 2);
        
        tuples = sqlQuery("select distinct ACLID,DBID from alfresco where `cm:content` = 'world' limit 10 ", alfrescoJson);
        assertTrue(tuples.size() == 4);
        
        tuples = sqlQuery("select distinct `cm:name` from alfresco where `cm:content` = 'world' limit 10 ", alfrescoJson);
        assertTrue(tuples.size() == 3);
        
        tuples = sqlQuery("select distinct `cm:title` from alfresco limit 10 ", alfrescoJson);
        assertTrue(tuples.size() == 2);
        
        tuples = sqlQuery("select distinct `cm:creator` from alfresco where `cm:content` = 'world' limit 10 ", alfrescoJson);
        assertTrue(tuples.size() == 3);
        
        tuples = sqlQuery("select distinct owner from alfresco where `cm:content` = 'world' limit 10 ", alfrescoJson);
        assertTrue(tuples.size() == 1);
    }

}

