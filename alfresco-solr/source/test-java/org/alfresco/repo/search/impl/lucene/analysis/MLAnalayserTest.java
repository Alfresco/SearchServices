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
package org.alfresco.repo.search.impl.lucene.analysis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.service.namespace.QName;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Retrofitted tests for the {@link MLAnalayser} class. These
 * should not be considered comprehensive, but are here to aid
 * refactoring during move to SOLR4.
 * 
 * @author Matt Ward
 */
@RunWith(MockitoJUnitRunner.class)
public class MLAnalayserTest
{
    private static final String PROPERTY_NAME = "@{http://www.alfresco.org/model/content/1.0}propertyName";
    private MLAnalayser analyser;
    private @Mock DictionaryService dictionaryService;
    private MLAnalysisMode mlAnalaysisMode = MLAnalysisMode.EXACT_COUNRTY;
    

    @Before
    public void setUp() throws Exception
    {
        analyser = new MLAnalayser(mlAnalaysisMode);
        PropertyDefinition propDef = Mockito.mock(PropertyDefinition.class);
        when(propDef.resolveAnalyserClassName(any(Locale.class))).thenReturn(StandardAnalyzer.class.getName()); //AlfrescoStandardAnalyser.class.getName());
        when(dictionaryService.getProperty(any(QName.class))).thenReturn(propDef);
    }

    @Ignore
    @Test
    public void testTokenStreamForLanguageAndCountry() throws IOException
    {        
        final String inputStr = "\u0000fr_FR\u0000Ceci n'est pas Française";
        final Reader reader = new StringReader(inputStr);
        
        List<String> expectedTokens = new ArrayList<>();
        expectedTokens.add("{fr_FR}ceci");
        expectedTokens.add("{fr_FR}n'est");
        expectedTokens.add("{fr_FR}pas");
        expectedTokens.add("{fr_FR}francaise"); // normalised 'c'.
        
        TokenStream ts = analyser.tokenStream(PROPERTY_NAME, reader);
        verifyTokenStream(ts, expectedTokens);
    }
    
    @Ignore
    @Test
    public void testTokenStreamForLanguage() throws IOException
    {        
        final String inputStr = "\u0000fr\u0000Ceci n'est pas Française";
        final Reader reader = new StringReader(inputStr);
        
        List<String> expectedTokens = new ArrayList<>();
        expectedTokens.add("{fr}ceci");
        expectedTokens.add("{fr_CH}ceci");
        expectedTokens.add("{fr_LU}ceci");
        expectedTokens.add("{fr_FR}ceci");
        expectedTokens.add("{fr_BE}ceci");
        expectedTokens.add("{fr_CA}ceci");
        
        expectedTokens.add("{fr}n'est");
        expectedTokens.add("{fr_CH}n'est");
        expectedTokens.add("{fr_LU}n'est");
        expectedTokens.add("{fr_FR}n'est");
        expectedTokens.add("{fr_BE}n'est");
        expectedTokens.add("{fr_CA}n'est");
        
        expectedTokens.add("{fr}pas");
        expectedTokens.add("{fr_CH}pas");
        expectedTokens.add("{fr_LU}pas");
        expectedTokens.add("{fr_FR}pas");
        expectedTokens.add("{fr_BE}pas");
        expectedTokens.add("{fr_CA}pas");
        
        expectedTokens.add("{fr}francaise");
        expectedTokens.add("{fr_CH}francaise");
        expectedTokens.add("{fr_LU}francaise");
        expectedTokens.add("{fr_FR}francaise");
        expectedTokens.add("{fr_BE}francaise");
        expectedTokens.add("{fr_CA}francaise");
        
        TokenStream ts = analyser.tokenStream(PROPERTY_NAME, reader);
        verifyTokenStream(ts, expectedTokens);
    }

    /**
     * Check that the TokenStream yields the exact tokens specified.
     * Note that order is not checked, since the map of locales will not provide a
     * predictable ordering when enumerated.
     * 
     * The expected list of tokens may contain the same token more than once and
     * the number of instances will have to match the number found in the stream.
     * 
     * @param ts              TokenStream to inspect.
     * @param expectedTokens  List of tokens in the order expected from the stream.
     * @throws IOException
     */
    private void verifyTokenStream(TokenStream ts, List<String> expectedTokens) throws IOException
    {
        final int expectedCount = expectedTokens.size();
        int count = 0;
        
        CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
        
        try
        {
            ts.reset();
            while (ts.incrementToken())
            {
                count++;
                System.out.println("Token: " + termAtt.toString());
                if (expectedTokens.contains(termAtt.toString()))
                {
                    // remove an instance of the term text so that it is not matched again
                    expectedTokens.remove(termAtt.toString());
                }
                else
                {
                    fail("Unexpected token: " + termAtt.toString());
                }
            }
            ts.end();
        }
        finally
        {
            ts.close();
        }
        
        assertEquals("Incorrect number of tokens generated.", expectedCount, count);
    }
}
