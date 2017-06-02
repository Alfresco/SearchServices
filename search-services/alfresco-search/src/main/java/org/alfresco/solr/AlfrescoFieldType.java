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
package org.alfresco.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.analysis.core.KeywordTokenizerFactory;
import org.apache.lucene.analysis.tokenattributes.TermToBytesRefAttribute;
import org.apache.lucene.analysis.util.AbstractAnalysisFactory;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.MultiTermAwareComponent;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.SortField;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IOUtils;
import org.apache.lucene.util.QueryBuilder;
import org.apache.solr.analysis.TokenizerChain;
import org.apache.solr.common.SolrException;
import org.apache.solr.response.TextResponseWriter;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.QParser;
import org.apache.solr.uninverting.UninvertingReader.Type;

/**
 * Basic behaviour filtched from TextField
 * 
 * @author Andy
 */
public class AlfrescoFieldType extends FieldType
{
    IndexSchema schema;

    /*
     * (non-Javadoc)
     * @see org.apache.solr.schema.FieldType#init(org.apache.solr.schema.IndexSchema, java.util.Map)
     */
    @Override
    protected void init(IndexSchema schema, Map<String, String> args)
    {
        this.schema = schema;
        properties |= TOKENIZED;
        properties &= ~OMIT_TF_POSITIONS;
        super.init(schema, args);
        
        // TODO: Wire up localised analysis driven from the schema
        // for now we do something basic
        setIndexAnalyzer(new AlfrescoAnalyzerWrapper(schema, AlfrescoAnalyzerWrapper.Mode.INDEX));
        setQueryAnalyzer(new AlfrescoAnalyzerWrapper(schema, AlfrescoAnalyzerWrapper.Mode.QUERY));
    }

    /*
     * (non-Javadoc)
     * @see org.apache.solr.schema.FieldType#getSortField(org.apache.solr.schema.SchemaField, boolean)
     */
    @Override
    public SortField getSortField(SchemaField field, boolean reverse)
    {
        return getStringSort(field, reverse);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.solr.schema.FieldType#write(org.apache.solr.response.TextResponseWriter, java.lang.String,
     * org.apache.lucene.index.IndexableField)
     */
    @Override
    public void write(TextResponseWriter writer, String name, IndexableField f) throws IOException
    {
        writer.writeStr(name, f.stringValue(), true);
    }

    @Override
    public Query getFieldQuery(QParser parser, SchemaField field, String externalVal)
    {
        return parseFieldQuery(parser, getQueryAnalyzer(), field.getName(), externalVal);
    }

    @Override
    public Object toObject(SchemaField sf, BytesRef term)
    {
        return term.utf8ToString();
    }

    @Override
    public Query getRangeQuery(QParser parser, SchemaField field, String part1, String part2, boolean minInclusive, boolean maxInclusive)
    {
        Analyzer multiAnalyzer = constructMultiTermAnalyzer(getQueryAnalyzer());
        BytesRef lower = analyzeMultiTerm(field.getName(), part1, multiAnalyzer);
        BytesRef upper = analyzeMultiTerm(field.getName(), part2, multiAnalyzer);
        return new TermRangeQuery(field.getName(), lower, upper, minInclusive, maxInclusive);
    }

    private Analyzer constructMultiTermAnalyzer(Analyzer queryAnalyzer)
    {
        if (queryAnalyzer == null)
            return null;

        if (!(queryAnalyzer instanceof TokenizerChain))
        {
            return new KeywordAnalyzer();
        }

        TokenizerChain tc = (TokenizerChain) queryAnalyzer;
        MultiTermChainBuilder builder = new MultiTermChainBuilder();

        CharFilterFactory[] charFactories = tc.getCharFilterFactories();
        if (charFactories != null)
        {
            for (CharFilterFactory fact : charFactories)
            {
                builder.add(fact);
            }
        }

        builder.add(tc.getTokenizerFactory());

        for (TokenFilterFactory fact : tc.getTokenFilterFactories())
        {
            builder.add(fact);
        }

        return builder.build();
    }

    private static class MultiTermChainBuilder
    {
        static final KeywordTokenizerFactory keyFactory = new KeywordTokenizerFactory(new HashMap<String, String>());

        ArrayList<CharFilterFactory> charFilters = null;

        ArrayList<TokenFilterFactory> filters = new ArrayList<TokenFilterFactory>(2);

        TokenizerFactory tokenizer = keyFactory;

        public void add(Object current)
        {
            if (!(current instanceof MultiTermAwareComponent))
                return;
            AbstractAnalysisFactory newComponent = ((MultiTermAwareComponent) current).getMultiTermComponent();
            if (newComponent instanceof TokenFilterFactory)
            {
                if (filters == null)
                {
                    filters = new ArrayList<TokenFilterFactory>(2);
                }
                filters.add((TokenFilterFactory) newComponent);
            }
            else if (newComponent instanceof TokenizerFactory)
            {
                tokenizer = (TokenizerFactory) newComponent;
            }
            else if (newComponent instanceof CharFilterFactory)
            {
                if (charFilters == null)
                {
                    charFilters = new ArrayList<CharFilterFactory>(1);
                }
                charFilters.add((CharFilterFactory) newComponent);

            }
            else
            {
                throw new SolrException(SolrException.ErrorCode.SERVER_ERROR, "Unknown analysis component from MultiTermAwareComponent: " + newComponent);
            }
        }

        public TokenizerChain build()
        {
            CharFilterFactory[] charFilterArr = charFilters == null ? null : charFilters.toArray(new CharFilterFactory[charFilters.size()]);
            TokenFilterFactory[] filterArr = filters == null ? new TokenFilterFactory[0] : filters.toArray(new TokenFilterFactory[filters.size()]);
            return new TokenizerChain(charFilterArr, tokenizer, filterArr);
        }

    }

    public static BytesRef analyzeMultiTerm(String field, String part, Analyzer analyzerIn)
    {
        if (part == null || analyzerIn == null)
            return null;

        TokenStream source = null;
        try
        {
            source = analyzerIn.tokenStream(field, part);
            source.reset();

            TermToBytesRefAttribute termAtt = source.getAttribute(TermToBytesRefAttribute.class);
            BytesRef bytes = termAtt.getBytesRef();

            if (!source.incrementToken())
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "analyzer returned no terms for multiTerm term: " + part);
            if (source.incrementToken())
                throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "analyzer returned too many terms for multiTerm term: " + part);

            source.end();
            return BytesRef.deepCopyOf(bytes);
        }
        catch (IOException e)
        {
            throw new SolrException(SolrException.ErrorCode.BAD_REQUEST, "error analyzing range part: " + part, e);
        }
        finally
        {
            IOUtils.closeWhileHandlingException(source);
        }
    }

    static Query parseFieldQuery(QParser parser, Analyzer analyzer, String field, String queryText)
    {
        // note, this method always worked this way (but nothing calls it?) because it has no idea of quotes...
        return new QueryBuilder(analyzer).createPhraseQuery(field, queryText);
    }

    @Override
    public Object marshalSortValue(Object value) 
    {
    	return marshalStringSortValue(value);
    }

    @Override
    public Object unmarshalSortValue(Object value) 
    {
    	return unmarshalStringSortValue(value);
    }
    
    protected boolean supportsAnalyzers() 
    {
        return true;
    }
    
    @Override
    public Type getUninversionType(SchemaField sf) {
      return Type.SORTED_SET_BINARY;
    }
}
