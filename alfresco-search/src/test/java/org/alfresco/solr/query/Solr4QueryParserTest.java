/*
 * Copyright (C) 2005-2015 Alfresco Software Limited.
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.LinkedList;

import org.alfresco.repo.search.impl.parsers.FTSQueryException;
import org.alfresco.repo.search.impl.parsers.FTSQueryParser;
import org.alfresco.service.cmr.search.SearchParameters;
import org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.spans.SpanNearQuery;
import org.apache.lucene.search.spans.SpanQuery;
import org.apache.lucene.search.spans.SpanTermQuery;
import org.apache.lucene.util.Version;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

public class Solr4QueryParserTest
{

    private Solr4QueryParser parser;
    private final static String TEST_FIELD = "creator";
    private final static String TEST_QUERY = "user@1user1";
    private SearchParameters searchParameters = new SearchParameters();
    
    @Before
    public void setUp() throws Exception
    {
        SolrQueryRequest req = Mockito.mock(SolrQueryRequest.class);
        parser = new Solr4QueryParser(req, Version.LUCENE_5_5_0, "TEXT", null, FTSQueryParser.RerankPhase.SINGLE_PASS);
        parser.setSearchParameters(searchParameters);
    }

    private PackedTokenAttributeImpl getTokenAttribute(String text, int startOffset, int endOffset)
    {
    	PackedTokenAttributeImpl token = new PackedTokenAttributeImpl();
    	token.setEmpty().append(text);
    	token.setOffset(startOffset, endOffset);
    	return token;
    }
    
    @Test
    public void testGetSearchParameters()
    {
    	assertEquals(searchParameters, parser.getSearchParameters());
    }
    
    @Test
    public void testSQLTranslationFailure() throws ParseException
    {
    	try
    	{
    	    parser.getLikeQuery("example", "_%woof_\\" , null);
    	    Assert.fail();
    	}
    	catch(FTSQueryException e)
    	{
    		
    	}
    }
    
    @Test
    public void testFlatQueryShouldBeGeneratedFromSequentiallyShiftedTokens() throws Exception
    {
        // prepare test data
        LinkedList<PackedTokenAttributeImpl> tokenSequenceWithRepeatedGroup = new LinkedList<PackedTokenAttributeImpl>();
        tokenSequenceWithRepeatedGroup.add(getTokenAttribute(TEST_QUERY.substring(0, 4), 0, 4));
        tokenSequenceWithRepeatedGroup.add(getTokenAttribute(TEST_QUERY.substring(5, 6), 5, 6));
        tokenSequenceWithRepeatedGroup.add(getTokenAttribute(TEST_QUERY.substring(6, 10), 6, 10));
        tokenSequenceWithRepeatedGroup.add(getTokenAttribute(TEST_QUERY.substring(10, 11), 10, 11));
        
        assertTrue("All tokens in test data must be sequentially shifted",
                parser.isAllTokensSequentiallyShifted(tokenSequenceWithRepeatedGroup));
        assertTrue(parser.getEnablePositionIncrements());
        
        LinkedList<LinkedList<PackedTokenAttributeImpl>> fixedTokenSequences = new LinkedList<LinkedList<PackedTokenAttributeImpl>>();
        fixedTokenSequences.add(tokenSequenceWithRepeatedGroup);
        
        // call method to test
        SpanQuery q = parser.generateSpanOrQuery(TEST_FIELD, fixedTokenSequences);
        
        // check results
        assertNotNull(q);
        assertTrue(q instanceof SpanNearQuery);
        SpanNearQuery spanNearQuery = (SpanNearQuery) q;
        assertEquals("Slop between term must be 0", 0, spanNearQuery.getSlop());
        assertTrue("Terms must be in order", spanNearQuery.isInOrder());
        
        SpanQuery[] termClauses = spanNearQuery.getClauses();
        assertEquals("Flat query must be generated (Query: " + q + ")", tokenSequenceWithRepeatedGroup.size(), termClauses.length);
        for (int i = 0; i < termClauses.length; i++)
        {
            assertTrue(termClauses[i] instanceof SpanTermQuery);
            assertEquals("All tokens must become spanQuery terms",
                    tokenSequenceWithRepeatedGroup.get(i).toString(), ((SpanTermQuery) termClauses[i]).getTerm().text());
        }
    }
    
}
