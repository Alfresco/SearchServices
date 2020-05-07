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

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.icu.segmentation.ICUTokenizerFactory;
import org.apache.lucene.analysis.standard.StandardTokenizer;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.synonym.SynonymGraphFilterFactory;
import org.apache.lucene.analysis.tokenattributes.*;
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
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

/**
 * Test case which illustrates some multi-term synonyms scenarios.
 *
 * @author Andrea Gazzarini
 */
@RunWith(MockitoJUnitRunner.class)
public class MultiTermSynonymsTest
{
    private Analyzer analyzer;

    private SolrResourceLoader resourceLoader;
    private final String ALPHANUM_TYPE = StandardTokenizer.TOKEN_TYPES[StandardTokenizer.ALPHANUM];
    private final String SYNONYM_TYPE = SynonymGraphFilter.TYPE_SYNONYM;

    @Rule
    public TemporaryFolder testFolder = new TemporaryFolder();

    /**
     * The Unit test makes use of the same filter chain used within the solrconfig.xml,
     * see the query analyzer of "text_en" field type:
     *
     * <ul>
     *     <li>{@link org.apache.lucene.analysis.synonym.SynonymGraphFilter}</li>
     *     <li>{@link org.alfresco.repo.search.impl.lucene.analysis.SynonymAwareStopFilterFactory.SynonymAwareStopFilter}</li>
     * </ul>
     *
     */
    @Before
    public void setUp() throws Exception
    {
        File stopwordsFile = testFolder.newFile("stopwords.txt");
        File synonyms = testFolder.newFile("synonyms.txt");

        FileUtils.writeLines(stopwordsFile, asList("of", "my"));
        FileUtils.writeLines(synonyms, asList(
                "OOW,Out of Warranty",
                "Apache Solr,Apache Solr")); // this is how we define a multi-term concept without any synonym

        System.setProperty("solr.solr.home", stopwordsFile.getParentFile().getAbsolutePath());
        resourceLoader = new SolrResourceLoader();

        analyzer = new Analyzer()
        {
            @Override
            protected TokenStreamComponents createComponents(String fieldName)
            {
                try
                {
                    ICUTokenizerFactory tokenizerFactory = new ICUTokenizerFactory(Collections.emptyMap());
                    tokenizerFactory.inform(resourceLoader);
                    Tokenizer tokenizer = tokenizerFactory.create();

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
     * No synonyms and no stopwords (plain and subsequent tokens with no removal).
     * No Graph is being generated from the outcoming stream.
     */
    @Test
    public void noSynonmsNoStopwords() throws Exception
    {
        final String text = "no synonyms and no stopwords";
        final List<PackedTokenAttributeImpl> expected = asList(
                token("no", 0, 2, 1, 1, ALPHANUM_TYPE),
                token("synonyms", 3, 11, 1, 1, ALPHANUM_TYPE),
                token("and", 12, 15, 1, 1, ALPHANUM_TYPE),
                token("no", 16, 18, 1, 1, ALPHANUM_TYPE),
                token("stopwords", 19, 28, 1, 1, ALPHANUM_TYPE));

        assertAnalysisCorrectness(text, expected);
    }

    /**
     * A synonym is detected and the correct graph is generated.
     * Note the original text (out *of* warranty) contains a stopwords but being that part of a synonym, it won't be
     * removed so a the graph will later generate a correct phrase query (i.e. "out of warranty")
     */
    @Test
    public void multiTermConceptWithSynonyms() throws Exception
    {
        final String text = "Car is Out of warranty";
        final List<PackedTokenAttributeImpl> expected = asList(
                token("Car", 0, 3, 1, 1, ALPHANUM_TYPE),
                token("is", 4, 6, 1, 1, ALPHANUM_TYPE),
                token("oow", 7, 22, 1, 3, SYNONYM_TYPE),
                token("Out", 7, 10, 0, 1, ALPHANUM_TYPE),
                token("of", 11, 13, 1, 1, ALPHANUM_TYPE),
                token("warranty", 14, 22, 1, 1, ALPHANUM_TYPE));

        assertAnalysisCorrectness(text, expected);
    }

    /**
     * This is the same test as above but instead of starting from the expanded form, the original text contains the
     * acronym (oow). Expected result is the same, the only difference is in the different tokens types.
     */
    @Test
    public void acronymsIsExpandedAndContainsStopwords() throws Exception
    {
        final String text = "Car is OOW";
        final List<PackedTokenAttributeImpl> expected = asList(
                token("Car", 0, 3, 1, 1, ALPHANUM_TYPE),
                token("is", 4, 6, 1, 1, ALPHANUM_TYPE),
                token("out", 7, 10, 1, 1, SYNONYM_TYPE),
                token("OOW", 7, 10, 0, 3, ALPHANUM_TYPE),
                token("of", 7, 10, 1, 1, SYNONYM_TYPE),
                token("warranty", 7, 10, 1, 1, SYNONYM_TYPE));

        assertAnalysisCorrectness(text, expected);
    }

    /**
     * If a multi-term concept needs to be captured, then its definition must be double in the synonyms file.
     *
     */
    @Test
    public void multiTermConceptWithoutSynonyms() throws Exception
    {
        final String text = "apache solr, the enterprise search platform";
        final List<PackedTokenAttributeImpl> expected = asList(
                token("apache", 0, 11, 1, 1, SYNONYM_TYPE),
                token("apache", 0, 6, 0, 2, ALPHANUM_TYPE),
                token("solr", 0, 11, 1, 2, SYNONYM_TYPE),
                token("solr", 7, 11, 1, 1, ALPHANUM_TYPE),
                token("the", 13, 16, 1, 1, ALPHANUM_TYPE),
                token("enterprise", 17, 27, 1, 1, ALPHANUM_TYPE),
                token("search", 28, 34, 1, 1, ALPHANUM_TYPE),
                token("platform", 35, 43, 1, 1, ALPHANUM_TYPE));

        assertAnalysisCorrectness(text, expected);
    }

    /**
     * Check the text analysis produces the expected tokens graph.
     *
     * @param text the input text.
     * @param expectedTokens  the list of tokens in the order expected from the stream.
     */
    private void assertAnalysisCorrectness(String text, List<PackedTokenAttributeImpl> expectedTokens) throws IOException
    {
        final List<PackedTokenAttributeImpl> actualTokens = new ArrayList<>();

        try (Reader reader = new StringReader(text);
             TokenStream tokenStream = analyzer.tokenStream("dummy_field", reader))
        {
            CharTermAttribute termAtt = tokenStream.addAttribute(CharTermAttribute.class);
            TypeAttribute typeAtt = tokenStream.addAttribute(TypeAttribute.class);
            PositionIncrementAttribute positionIncrementAtt = tokenStream.addAttribute(PositionIncrementAttribute.class);
            PositionLengthAttribute positionLengthAtt = tokenStream.addAttribute(PositionLengthAttribute.class);
            OffsetAttribute offsetAtt = tokenStream.addAttribute(OffsetAttribute.class);

            tokenStream.reset();
            while (tokenStream.incrementToken())
            {
                actualTokens.add(
                        token(new String(termAtt.buffer(), 0, termAtt.length()),
                                offsetAtt.startOffset(),
                                offsetAtt.endOffset(),
                                positionIncrementAtt.getPositionIncrement(),
                                positionLengthAtt.getPositionLength(),
                                typeAtt.type()));
            }
            tokenStream.end();
        }

        assertEquals(mismatchTokensListMessage(expectedTokens, actualTokens), expectedTokens, actualTokens);
    }

    private String mismatchTokensListMessage(List<PackedTokenAttributeImpl> expected, List<PackedTokenAttributeImpl> actual)
    {
        Function<PackedTokenAttributeImpl, String> toString =
                token -> "term=" + token.toString() +
                            ",startOffset=" + token.startOffset() +
                            ",endOffset=" + token.endOffset() +
                            ",posInc=" + token.getPositionIncrement() +
                            ",posLength=" + token.getPositionLength() +
                            ",type=" + token.type();

        String expectedMessage = expected.stream().map(toString).collect(Collectors.joining("\n"));
        String actualMessage = actual.stream().map(toString).collect(Collectors.joining("\n"));

        return "*** EXPECTED ***\n" + expectedMessage + "\n*** ACTUAL ***\n" + actualMessage;
    }

    /**
     * Creates a test {@link PackedTokenAttributeImpl} used for test verifications.
     *
     * @param text the token content.
     * @param positionLength the token position length.
     * @param type the token type.
     * @param startOffset the token start offset.
     * @param endOffset the token end offset.
     * @param positionIncrement the token position increment.
     * @return a test {@link PackedTokenAttributeImpl} used for test verifications.
     */
    private PackedTokenAttributeImpl token(String text, int startOffset, int endOffset, int positionIncrement, int positionLength, String type)
    {
        PackedTokenAttributeImpl token = new PackedTokenAttributeImpl();
        token.setPositionLength(positionLength);
        token.setType(type);
        token.setOffset(startOffset, endOffset);
        token.setPositionIncrement(positionIncrement);
        token.copyBuffer(text.toCharArray(), 0, text.length());
        return token;
    }
}