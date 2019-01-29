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

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardTokenizerFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.solr.core.SolrResourceLoader;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Test case for {@link SynonymAwareStopFilterFactory.SynonymAwareStopFilter}
 *
 * @author Andrea Gazzarini
 */
@RunWith(MockitoJUnitRunner.class)
public class SynonymAwareStopFilterTest
{

    private Analyzer analyzer;

    private SolrResourceLoader resourceLoader;

    private final List<String> stopwords = asList("of", "my");

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    /**
     * Setup fixture for this test case.
     * Specifically:
     *
     * <ul>
     *     <li>A test analyzer composed by a StandardTokenizer, SynonymsGraphFilter and the class under test (SynonymAwareStopFilter)</li>
     *     <li>a synonyms file containing two rules: ["OOW,out of warranty","transfer phone number,port number"]</li>
     *     <li>a stopwords file with two stopwords: ["of","my"]</li>
     * </ul>
     */
    @Before
    public void setUp() throws Exception
    {
        File stopwordsFile = testFolder.newFile("stopwords.txt");
        File synonyms = testFolder.newFile("synonyms.txt");

        FileUtils.writeLines(stopwordsFile, stopwords);
        FileUtils.writeLines(synonyms, asList("OOW,Out of Warranty","transfer Phone Number,Port Number"));

        System.setProperty("solr.solr.home", stopwordsFile.getParentFile().getAbsolutePath());
        resourceLoader = new SolrResourceLoader();

        analyzer = new Analyzer()
        {
            @Override
            protected TokenStreamComponents createComponents(String fieldName)
            {
                try
                {
                    Tokenizer tokenizer = new StandardTokenizerFactory(Collections.emptyMap()).create();

                    Map<String, String> synonymsConfig = new HashMap<>();
                    synonymsConfig.put("synonyms", "synonyms.txt");
                    synonymsConfig.put("ignoreCase", "true");

                    SynonymGraphFilterFactory synonymsFactory = new SynonymGraphFilterFactory(synonymsConfig);
                    synonymsFactory.inform(resourceLoader);
                    TokenStream synonymsFilter = synonymsFactory.create(tokenizer);

                    Map<String, String> stopFilterConfig = new HashMap<>();
                    stopFilterConfig.put("words", "stopwords.txt");
                    stopFilterConfig.put("ignoreCase", "true");

                    SynonymAwareStopFilterFactory stopWordsFactory = new SynonymAwareStopFilterFactory(stopFilterConfig);
                    stopWordsFactory.inform(resourceLoader);

                    TokenStream sinkStream = stopWordsFactory.create(synonymsFilter);

                    return new TokenStreamComponents(tokenizer, sinkStream);
                }
                catch (Exception exception)
                {
                    throw new IllegalArgumentException(exception);
                }
            }
        };
    }

    /**
     * In case we have no synonyms and no stopwords then the input should be left untouched.
     */
    @Test
    public void noSynonmsNoStopwords() throws Exception
    {
        String text = "no synonyms and no stopwords here";
        List<String> expected = asList(text.split("\\W"));

        assertAnalysisCorrectness(text, expected);
    }

    /**
     * If a stream doesn't contain any SYNONYM token, the custom stop filter behaves like its superclass (StopFilter)
     */
    @Test
    public void noSynonymsOnlyStopwords() throws Exception
    {
        String text = "No synonyms detection. However we have two stopwords of and my which will be removed";
        List<String> expectedTokens = asList("No", "synonyms", "detection","However", "we", "have", "two", "stopwords", "and", "which", "will", "be", "removed");

        assertAnalysisCorrectness(text, expectedTokens);
    }

    /**
     * A synonym is detected but it doesn't contain any stopwords
     */
    @Test
    public void synonymsDetectionWithoutStopwordTokens() throws Exception {
        String text = "How do I transfer phone number?";
        List<String> expectedTokens =
                asList(
                        "How",
                        "do",
                        "I",
                        "port", "transfer", "number", "phone", "number"); // detected synonym

        assertAnalysisCorrectness(text, expectedTokens);
    }

    /**
     * A synonym is detected; there are also stopwords which are not part of the synonym so the filter will be remove it.
     */
    @Test
    public void synonymsDetectionWithStopwordTokensOutside() throws Exception
    {
        String text = "How do I transfer phone number, in my new device, of course?";
        List<String> expectedTokens =
                asList(
                        "How",
                        "do",
                        "I",
                        "port", "transfer", "number", "phone", "number", // detected synonym
                        "in",
                        // "my" is removed as it is a stopword outside the synonym detection
                        "new",
                        "device",
                        // "of" is removed as it is a stopword outside the synonym detection
                        "course");

        assertAnalysisCorrectness(text, expectedTokens);
    }

    /**
     * A synonym is detected (oow => out of warranty) but it contains a stopword token ("of")
     * The filter won't remove it.
     */
    @Test
    public void synonymsDetectionWithStopwordTokensInside() throws Exception
    {
        String text = "My car is out of warranty and it's broken. What should I do?";
        List<String> expectedTokens =
                asList(
                        // "My" is removed because outside the detected synonym
                        "car",
                        "is",
                        "oow", "out", "of", "warranty", // detected synonym: of, although a stopword, is not removed
                        "and",
                        "it's",
                        "broken",
                        "What",
                        "should",
                        "I", "do");

        assertAnalysisCorrectness(text, expectedTokens);
    }

    /**
     * Check the text analysis produces the expected token sequence.
     *
     * @param text the input text.
     * @param expectedTokens  the list of tokens in the order expected from the stream.
     */
    private void assertAnalysisCorrectness(String text, List<String> expectedTokens) throws IOException
    {
        List<String> actualTokens = new ArrayList<>();

        try (Reader reader = new StringReader(text);
             TokenStream tokenStream = analyzer.tokenStream("dummy_field", reader))
        {
            CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
            tokenStream.reset();
            while (tokenStream.incrementToken())
            {
                actualTokens.add(new String(termAtt.buffer(), 0, termAtt.length()));
            }
            tokenStream.end();
        }

        assertEquals("Expected tokens don't match", expectedTokens, actualTokens);
    }
}