/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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

package org.alfresco.solr.schema.highlight;

import org.alfresco.solr.AlfrescoAnalyzerWrapper;
import org.alfresco.util.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.util.Locale;
import java.util.Optional;

import static java.util.Arrays.stream;
import static org.alfresco.solr.schema.highlight.LanguagePrefixedTokenStream.FALLBACK_TEXT_FIELD_TYPE_NAME;
import static org.alfresco.solr.schema.highlight.LanguagePrefixedTokenStream.LOCALISED_FIELD_TYPE_NAME_PREFIX;
import static org.alfresco.solr.schema.highlight.LanguagePrefixedTokenStream.LOCALISED_HIGHLIGHTING_FIELD_TYPE_NAME_PREFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class LanguagePrefixedTokenStreamTest
{
    private LanguagePrefixedTokenStream classUnderTest;

    @Mock
    private IndexSchema schema;

    @Before
    public void setUp()
    {
        classUnderTest = new LanguagePrefixedTokenStream(schema, "aFieldName", AlfrescoAnalyzerWrapper.Mode.INDEX);
    }

    @Test
    public void localisedFieldTypeName()
    {
        stream(Locale.getAvailableLocales())
                .map(Locale::getLanguage)
                .forEach(language ->
                        assertEquals(
                                LOCALISED_FIELD_TYPE_NAME_PREFIX  + language,
                                classUnderTest.localisedFieldTypeName(language)));

    }

    @Test
    public void highlightingFieldTypeName()
    {
        stream(Locale.getAvailableLocales())
                .map(Locale::getLanguage)
                .forEach(language ->
                        assertEquals(
                                LOCALISED_HIGHLIGHTING_FIELD_TYPE_NAME_PREFIX  + language,
                                classUnderTest.highlightingFieldTypeName(language)));

    }

    @Test
    public void localeMarkerLenghthWithValidLocale_shouldReturnTheMarkerLength()
    {
        stream(Locale.getAvailableLocales())
                .map(Locale::getLanguage)
                .forEach(language ->
                        assertEquals(
                                language.length() + 2,
                                classUnderTest.localeMarkerLength(Optional.of(language))));

    }

    @Test
    public void localeMarkerLenghthWithInvalidLocale_shouldReturnZero()
    {
        assertEquals(0, classUnderTest.localeMarkerLength(Optional.empty()));
    }

    @Test
    public void analyzerInCaseTheHighlightingFieldTypeExists()
    {
        String language = "fr";
        String fieldTypeName = classUnderTest.highlightingFieldTypeName(language);

        FieldType highlightedFrenchFieldType = mock(FieldType.class);
        Analyzer queryTimeAnalyzer = mock(Analyzer.class);
        Analyzer indexTimeAnalyzer = mock(Analyzer.class);

        when(highlightedFrenchFieldType.getIndexAnalyzer()).thenReturn(indexTimeAnalyzer);
        when(highlightedFrenchFieldType.getQueryAnalyzer()).thenReturn(queryTimeAnalyzer);
        when(schema.getFieldTypeByName(fieldTypeName)).thenReturn(highlightedFrenchFieldType);

        assertSame(indexTimeAnalyzer, classUnderTest.analyzer(language));

        classUnderTest = new LanguagePrefixedTokenStream(schema, "aFieldName", AlfrescoAnalyzerWrapper.Mode.QUERY);

        assertSame(queryTimeAnalyzer, classUnderTest.analyzer(language));
    }

    @Test
    public void analyzerInCaseTheHighlightingFieldTypeDoesntExists_shouldFallbackToLocalisedFieldType()
    {
        String language = "fr";
        String highlightingFieldTypeName = classUnderTest.highlightingFieldTypeName(language);
        String localisedFieldTypeName = classUnderTest.localisedFieldTypeName(language);

        FieldType localisedFieldType = mock(FieldType.class);
        Analyzer queryTimeAnalyzer = mock(Analyzer.class);
        Analyzer indexTimeAnalyzer = mock(Analyzer.class);

        when(localisedFieldType.getIndexAnalyzer()).thenReturn(indexTimeAnalyzer);
        when(localisedFieldType.getQueryAnalyzer()).thenReturn(queryTimeAnalyzer);
        when(schema.getFieldTypeByName(highlightingFieldTypeName)).thenReturn(null);
        when(schema.getFieldTypeByName(localisedFieldTypeName)).thenReturn(localisedFieldType);

        assertSame(indexTimeAnalyzer, classUnderTest.analyzer(language));

        classUnderTest = new LanguagePrefixedTokenStream(schema, "aFieldName", AlfrescoAnalyzerWrapper.Mode.QUERY);

        assertSame(queryTimeAnalyzer, classUnderTest.analyzer(language));
    }

    @Test
    public void analyzerInCaseHighlightAndLocalisedFieldsDontExist_shouldFallbackToTextGeneral()
    {
        String language = "fr";
        String highlightingFieldTypeName = classUnderTest.highlightingFieldTypeName(language);
        String localisedFieldTypeName = classUnderTest.localisedFieldTypeName(language);

        FieldType textGeneralFieldType = mock(FieldType.class);
        Analyzer queryTimeAnalyzer = mock(Analyzer.class);
        Analyzer indexTimeAnalyzer = mock(Analyzer.class);

        when(textGeneralFieldType.getIndexAnalyzer()).thenReturn(indexTimeAnalyzer);
        when(textGeneralFieldType.getQueryAnalyzer()).thenReturn(queryTimeAnalyzer);
        when(schema.getFieldTypeByName(highlightingFieldTypeName)).thenReturn(null);
        when(schema.getFieldTypeByName(localisedFieldTypeName)).thenReturn(null);
        when(schema.getFieldTypeByName(FALLBACK_TEXT_FIELD_TYPE_NAME)).thenReturn(textGeneralFieldType);

        assertSame(indexTimeAnalyzer, classUnderTest.analyzer(language));

        classUnderTest = new LanguagePrefixedTokenStream(schema, "aFieldName", AlfrescoAnalyzerWrapper.Mode.QUERY);

        assertSame(queryTimeAnalyzer, classUnderTest.analyzer(language));
    }

    @Test
    public void languageAndReaderWithNoData() throws IOException
    {
        StringReader emptyReader = new StringReader("");

        Pair<Optional<String>, Reader> data = classUnderTest.languageAndReaderFrom(emptyReader);

        assertTrue(data.getFirst().isEmpty());
    }
}
