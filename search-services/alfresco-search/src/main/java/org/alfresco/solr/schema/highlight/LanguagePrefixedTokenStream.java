/*
 * #%L
 * Alfresco Search Services
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

package org.alfresco.solr.schema.highlight;

import static java.util.Optional.of;
import static java.util.Optional.ofNullable;

import org.alfresco.solr.AlfrescoAnalyzerWrapper;
import org.alfresco.util.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Optional;

/**
 * A {@link TokenStream} decorator which determines dynamically the field type and the analyzer used for
 * executing the analysis of an input text.
 * Although this class extends {@link Tokenizer}, actually it is not a tokenizer: this because in order to individuate
 * the analyzer dynamically, a component must access to a {@link IndexSchema} instance, and usually this is not possible
 * in the components involved in the analysis chain (e.g. tokenizer, token filters, char filters).
 *
 * The field type and the analyzer that will control the text analysis are computed in the following way:
 *
 * <ul>
 *     <li>
 *         pre-process the input reader given to this chain in order to detect the locale language code at the very
 *         beginning.
 *         The locale language prefix includes
 *         <ul>
 *              <li>a beginning sentinel token #0;</li>
 *              <li>a language code (two or three chars)</li>
 *              <li>a closing sentinel token #0;</li>
 *         </ul>
 *     </li>
 *     <li>
 *         if any language code has been found, it is used for determine a field type name composed by the
 *         prefix "highlighted_text_" and the detected language code (e.g. highlighted_text_ + en = highlighted_text_en).
 *     </li>
 *     <li>
 *         If the field type above doesn't exist in the schema, the the same procedure is repeated using the prefix
 *         "text_" (e.g. text_ + en = text_en)
 *     </li>
 *     <li>
 *         If the field type above doesn't exist in the schema, then the "text___" general text field type is used.
 *     </li>
 *     <li>
 *         The input text is analyzed using the (query or index) analyzer associated to the field type determined above.
 *     </li>
 * </ul>
 *
 * @author Andrea Gazzarini
 */
public final class LanguagePrefixedTokenStream extends Tokenizer
{
    static final String FALLBACK_TEXT_FIELD_TYPE_NAME = "text___";
    static final String LOCALISED_FIELD_TYPE_NAME_PREFIX = "text_";
    static final String LOCALISED_HIGHLIGHTING_FIELD_TYPE_NAME_PREFIX = "highlighted_text_";

    private final static char LANGUAGE_SENTINEL_TOKEN = '\u0000';
    private final static char [] EMPTY_CHARSTREAM = {};

    protected String fieldName;
    protected IndexSchema indexSchema;
    protected AlfrescoAnalyzerWrapper.Mode mode;
    protected Analyzer analyzer;

    private TokenStream stream;
    private int localeMarkerLength;

    private CharTermAttribute decoratorTerm;
    private PositionIncrementAttribute decoratorPositionIncrement;
    private OffsetAttribute decoratorOffset;
    private TypeAttribute decoratorType;

    private CharTermAttribute decoratedTerm;
    private PositionIncrementAttribute decoratedPositionInc;
    private OffsetAttribute decoratedOffset;
    private TypeAttribute decoratedType;

    LanguagePrefixedTokenStream(IndexSchema indexSchema, String fieldName, AlfrescoAnalyzerWrapper.Mode mode)
    {
        this.indexSchema = indexSchema;
        this.fieldName = fieldName;
        this.mode = mode;

        decoratorTerm = addAttribute(CharTermAttribute.class);
        decoratorPositionIncrement = addAttribute(PositionIncrementAttribute.class);
        decoratorOffset = addAttribute(OffsetAttribute.class);
        decoratorType = addAttribute(TypeAttribute.class);
    }

    @Override
    public void reset() throws IOException
    {
        super.reset();

        clearAttributes();

        final Pair<Optional<String>, Reader> info = languageAndReaderFrom(input);

        this.localeMarkerLength = localeMarkerLength(info.getFirst());
        String language = info.getFirst().orElse("__");

        this.analyzer = analyzer(language);
        this.stream = analyzer.tokenStream(fieldName, info.getSecond());
        this.stream.reset();

        createOrRefreshAttributesOfDecoratedStream();
    }

    @Override
    public boolean incrementToken() throws IOException
    {
        boolean result = stream.incrementToken();

        if (result)
        {
            copyAttributesFromDecorateeToDecorator();
        }
        return result;
    }

    @Override
    public void end() throws IOException
    {
        super.end();
        stream.end();
    }

    @Override
    public void close() throws IOException
    {
        super.close();
        stream.close();
    }

    /**
     * Starting from the input reader, this method detects the (eventual) locale language prefix and then
     * creates a new {@link Reader} instance which, behind the scenes, reuses the unread part of the input reader.
     * This avoids to read the full stream in a new string or char array and the create a completely new reader
     * on top of that.
     *
     * We can have the following scenarios:
     *
     * <ul>
     *     <li>the underlying stream doesn't have any locale language prefix: in this case the returned reader will
     *     consume the whole stream from the beginning</li>
     *     <li>the underlying stream starts with a locale language marker (\u0000 + locale language + \u0000): in this
     *     case the language is isolated (that's the reason why this method returns a pair) and the new reader will
     *     consume the stream from the first char after the language marker.</li>
     * </ul>
     *
     * @param reader the input reader
     * @return a pair consisting of a locale language (a String) and a new content reader.
     */
    Pair<Optional<String>, Reader> languageAndReaderFrom(Reader reader) throws IOException
    {
        final char [] prefix = new char[5];
        final int read = reader.read(prefix);
        if (read < prefix.length)
        {
            return new Pair<>(Optional.empty(),
                    isValidLocaleMarker(prefix)
                            ? new CharArrayReader(EMPTY_CHARSTREAM)
                            : new CharArrayReader(prefix, 0, Math.max(read, 0)));
        }
        else
        {
            if (isValidLocaleMarker(prefix))
            {
                if (isTwoCharsLanguageCode(prefix))
                {
                    Reader alreadyConsumedCharsReader = new CharArrayReader(prefix, prefix.length - 1, 1);
                    return new Pair<>(
                            of(new String(prefix, 1, 2).toLowerCase()),
                            new CompositeReader(alreadyConsumedCharsReader, reader));
                }
                else
                {
                    return new Pair<>(
                            of(new String(prefix, 1, 3).toLowerCase()),
                            reader);
                }
            }
            else
            {
                Reader alreadyConsumedCharsReader = new CharArrayReader(prefix);
                return new Pair<>(Optional.empty(), new CompositeReader(alreadyConsumedCharsReader, reader));
            }
        }
    }

    /**
     * Returns true if the input marker contains a language code composed by 2 chars.
     * A language code can be composed by two (e.g. en) or three characters (e.g. kyr).
     * Since the buffer used for reading the marker from the input reader has 9 slots, we need to understand the size of
     * the language code in the array. This because in case it is a 2 chars code, there will be a remaining character
     * in the buffer which represents content to be indexed.
     *
     * The buffer is composed by
     *
     * <ul>
     *     <li>
     *         3 chars (#0;)
     *     </li>
     *     <li>
     *         locale language code
     *     </li>
     *     <li>
     *         3 chars (#0;)
     *     </li>
     * </ul>
     *
     * The buffer where we read the information above has a size equal to 9. That means,
     *
     * <ul>
     *     <li>
     *         if we read a 3-chars language code the whole array is used (#0;kyr#0;)
     *     </li>
     *     <li>
     *         if we read a 2-chars language code the last position in the array is not part of the marker and
     *         we need to make it available for the analyzer consumption.
     *     </li>
     * </ul>
     *
     * @param marker the char array where we previously read the locale marker.
     * @return true if the locale language code in the marker is a 2-chars code, false otherwise (3-chars code).
     */
    private boolean isTwoCharsLanguageCode(final char[] marker)
    {
        return marker.length == 5 && marker[3] == LANGUAGE_SENTINEL_TOKEN;
    }

    /**
     * Checks if the given prefix chars corresponds to a valid locale marker.
     * The locale key delimiter used in SearchServices (\u0000) becomes "#0;" in the indexable value,
     * so in order to understand if a given prefix is a valid locale marker or not we expected a 8/9 char array where
     *
     * pos [0] and [length - 3] = '#'
     * pos [1] and [length - 2] = '0'
     * pos [2] and [length - 1] = ';'
     * pos [3] and [4] and optionally pos [5] = two/three chars that should* correspond to a locale language.
     *
     * Example: "#0;en#0;", "#0;kyr#0;"
     *
     * *Note this method doesn't check if the two/three chars at pos 3 and 4 (opt 5) actually correspond to a valid locale language code.
     */
    private boolean isValidLocaleMarker(char[] prefix)
    {
        int length = prefix.length;
        return prefix[0] == LANGUAGE_SENTINEL_TOKEN
                && (prefix[length - 1] == LANGUAGE_SENTINEL_TOKEN ||
                    (prefix[length - 2] == LANGUAGE_SENTINEL_TOKEN));
    }

    /**
     * Returns the {@link Analyzer} associated with the given language.
     * The proper {@link Analyzer} is retrieved from the first field type not null in the following list:
     *
     * <ul>
     *     <li>highlighted_text_ + locale (e.g. highlighted_text_en)</li>
     *     <li>text_ + locale (e.g. text_en)</li>
     *     <li>text___ (text general field)</li>
     * </ul>
     *
     * @param language the language code.
     * @return the {@link Analyzer} associated with the given language.
     */
    Analyzer analyzer(String language) {
        FieldType localisedFieldType =
                ofNullable(indexSchema.getFieldTypeByName(highlightingFieldTypeName(language)))
                        .orElseGet(() -> indexSchema.getFieldTypeByName(localisedFieldTypeName(language)));

        FieldType targetFieldType =
                ofNullable(localisedFieldType)
                        .orElseGet(() ->  indexSchema.getFieldTypeByName(FALLBACK_TEXT_FIELD_TYPE_NAME));
        switch (mode)
        {
            case QUERY:
                return targetFieldType.getQueryAnalyzer();
            case INDEX:
            default:
                return targetFieldType.getIndexAnalyzer();
        }
    }

    /**
     * After a consumption cycle of the current managed token stream (see {@link #incrementToken()} we need to copy
     * the attributes of that stream on the top level attributes of this decorator so the caller can retrieve them
     * properly.
     */
    void copyAttributesFromDecorateeToDecorator()
    {
        this.decoratorTerm.copyBuffer(decoratedTerm.buffer(), 0, decoratedTerm.length());
        this.decoratorOffset.setOffset(
                decoratedOffset.startOffset() + localeMarkerLength,
                decoratedOffset.endOffset() + localeMarkerLength);
        this.decoratorType.setType(decoratedType.type());
        this.decoratorPositionIncrement.setPositionIncrement(decoratedPositionInc.getPositionIncrement());
    }

    /**
     * The current token stream has been consumed. This decorator will switch on the next stream for consumption.
     * In order to do that, we need to create a valid set of attributes from the new token stream.
     */
    void createOrRefreshAttributesOfDecoratedStream()
    {
        decoratedTerm = stream.addAttribute(CharTermAttribute.class);
        decoratedPositionInc = stream.addAttribute(PositionIncrementAttribute.class);
        decoratedOffset = stream.addAttribute(OffsetAttribute.class);
        decoratedType = stream.addAttribute(TypeAttribute.class);
    }

    /**
     * Returns the name of the localised field type specifically used for the highlighting.
     * By convention, the field type name is obtained using a "highlighted_text_" prefix followed by the
     * locale language code (e.g. text_en, text_fr).
     *
     * @return the name of the localised field type specifically used for the highlighting.
     */
    String highlightingFieldTypeName(String language)
    {
        return LOCALISED_HIGHLIGHTING_FIELD_TYPE_NAME_PREFIX + language;
    }

    /**
     * Returns the name of the localised field type we are interested in the current processing.
     * By convention, the field type name is obtained using a "text_" prefix followed by the locale language code
     * (e.g. text_en, text_fr).
     *
     * @return the localised field type name.
     */
    String localisedFieldTypeName(String language)
    {
        return LOCALISED_FIELD_TYPE_NAME_PREFIX + language;
    }

    int localeMarkerLength(Optional<String> language)
    {
        return language.map(String::length).map(length -> length + 2).orElse(0);
    }
}