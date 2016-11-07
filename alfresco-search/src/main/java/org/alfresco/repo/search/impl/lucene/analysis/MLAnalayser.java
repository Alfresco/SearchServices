/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

import org.alfresco.repo.search.MLAnalysisMode;
import org.alfresco.solr.AlfrescoAnalyzerWrapper.Mode;
import org.alfresco.util.Pair;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;
import org.apache.solr.schema.FieldType;
import org.apache.solr.schema.IndexSchema;
import org.springframework.extensions.surf.util.I18NUtil;

public class MLAnalayser extends Analyzer
{
    private static Log s_logger = LogFactory.getLog(MLAnalayser.class);

    private MLAnalysisMode mlAnalaysisMode;
   
    private IndexSchema schema;

	private Mode mode;
    
    public MLAnalayser(MLAnalysisMode mlAnalaysisMode, IndexSchema schema, Mode mode)
    {
        super(Analyzer.PER_FIELD_REUSE_STRATEGY);
        this.mlAnalaysisMode = mlAnalaysisMode;
        this.schema = schema;
        this.mode = mode;
    }


    public MLAnalayser(MLAnalysisMode mlAnalaysisMode)
    {
        super(Analyzer.PER_FIELD_REUSE_STRATEGY);
        this.mlAnalaysisMode = mlAnalaysisMode;
    }

  
    
    @Override
    protected TokenStreamComponents createComponents(String fieldName) 
    {
        MLTokenizer mltokenizer = new MLTokenizer(fieldName, schema, mlAnalaysisMode, mode);
//        try
//        {
//            mltokenizer.setLocaleAndPositionReaderAfterLocaleEncoding(reader);
//        }
//        catch (IOException e)
//        {
//            try{mltokenizer.close();} catch(IOException ioe) {};
//            throw new AnalysisException("Failed to init MLTokenizer", e);
//        }
        return new LocaleAwareTokenStreamComponents(mltokenizer);
    }
    
    private static class LocaleAwareTokenStreamComponents extends Analyzer.TokenStreamComponents 
    {
        MLTokenizer mltokenizer;
        
        /**
         * @param source
         */
        public LocaleAwareTokenStreamComponents(final MLTokenizer source)
        {
            super(source);
            this.mltokenizer = source;
        }

        /* (non-Javadoc)
         * @see org.apache.lucene.analysis.Analyzer.TokenStreamComponents#setReader(java.io.Reader)
         */
        @Override
        protected void setReader(Reader reader)
        {
            super.setReader(mltokenizer.setLocaleAndPositionReaderAfterLocaleEncoding(reader));
        }
    }
    
    private static class MLTokenizer extends Tokenizer
    {
        TokenStream ts;
        
        String fieldName;
        
        private IndexSchema schema;
        
        MLAnalysisMode mlAnalaysisMode;

        @SuppressWarnings("unused")
		private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

        @SuppressWarnings("unused")
        private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

        @SuppressWarnings("unused")
        private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
        
        @SuppressWarnings("unused")
        private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

		private Mode mode;
     
        MLTokenizer(String fieldName, IndexSchema schema, MLAnalysisMode mlAnalaysisMode, Mode mode) 
        {
            this.fieldName = fieldName;
            this.schema = schema;
            this.mlAnalaysisMode = mlAnalaysisMode;
            this.mode = mode;
        }
        
        

        /**
         * @param reader
         * @throws IOException 
         */
        public Reader setLocaleAndPositionReaderAfterLocaleEncoding(Reader reader) 
        {
            Pair<Locale, Reader> pair = getLocaleAndPositioReaderAfterLocaleEncoding(fieldName, reader);

            if(s_logger.isDebugEnabled())
            {
                s_logger.debug("Created ML analyser token stream for "+fieldName+ " with locale "+pair.getFirst());
            }
            TokenStream source = getAnalyser(fieldName, pair.getFirst()).tokenStream(fieldName, pair.getSecond());
            ts =  new MLTokenDuplicator(source, pair.getFirst(), pair.getSecond(), mlAnalaysisMode);
            return pair.getSecond();
        }



        /* (non-Javadoc)
         * @see org.apache.lucene.analysis.Tokenizer#close()
         */
        @Override
        public void close() throws IOException
        {
            ts.close();
            super.close();
        }



        /* (non-Javadoc)
         * @see org.apache.lucene.analysis.Tokenizer#reset()
         */
        @Override
        public void reset() throws IOException
        {
            ts.reset();
            super.reset();
        }


        /* (non-Javadoc)
         * @see org.apache.lucene.analysis.Tokenizer#reset()
         */
        @Override
        public void end() throws IOException
        {
            ts.end();
            super.end();
        }


        

        /* (non-Javadoc)
         * @see org.apache.lucene.analysis.TokenStream#incrementToken()
         */
        @Override
        public boolean incrementToken() throws IOException
        {
            clearAttributes();
            if(ts.incrementToken())
            {
                ts.copyTo(this);
                return true;
                
            }
            else
            {
                ts.end();
                ts.close();
                return false;
            }
            
           
          
        }
        
        
        public Pair<Locale,Reader> getLocaleAndPositioReaderAfterLocaleEncoding(String fieldName, Reader reader)
        {
            // We use read ahead to get the language info - if this does not exist we need to restart
            // an use the default - there for we need mark and restore.
            BufferedReader breader;
            if (reader instanceof BufferedReader)
            {
                breader = (BufferedReader)reader;
            }
            else
            {
                breader = new BufferedReader(reader);
            }

            try
            {
                if (!breader.markSupported())
                {
                    throw new AnalysisException(
                            "Multilingual tokenisation requires a reader that supports marks and reset");
                }
                breader.mark(100);
                StringBuilder builder = new StringBuilder();
                if (breader.read() == '\u0000')
                {
                    String language = "";
                    String country = "";
                    String varient = "";
                    char c;
                    int count = 0;
                    while ((c = (char) breader.read()) != '\u0000')
                    {
                        if (count++ > 99)
                        {
                            breader.reset();
                            return new Pair<Locale, Reader>(I18NUtil.getLocale(), breader);
                        }
                        if (c == '_')
                        {
                            if (language.length() == 0)
                            {
                                language = builder.toString();
                            }
                            else if (country.length() == 0)
                            {
                                country = builder.toString();
                            }
                            else if (varient.length() == 0)
                            {
                                varient = builder.toString();
                            }
                            else
                            {
                                breader.reset();
                                return new Pair<Locale, Reader>(I18NUtil.getLocale(), breader);
                            }
                            builder = new StringBuilder();
                        }
                        else
                        {
                            builder.append(c);
                        }
                    }
                    if (builder.length() > 0)
                    {
                        if (language.length() == 0)
                        {
                            language = builder.toString();
                        }
                        else if (country.length() == 0)
                        {
                            country = builder.toString();
                        }
                        else if (varient.length() == 0)
                        {
                            varient = builder.toString();
                        }
                        else
                        {
                            breader.reset();
                            return new Pair<Locale, Reader>(I18NUtil.getLocale(), breader);
                        }
                    }
                    Locale locale = new Locale(language, country, varient);
                    // leave the reader where it is ....
                    return new Pair<Locale, Reader>(locale, breader);
                    
                }
                else
                {
                    breader.reset();
                    return new Pair<Locale, Reader>(I18NUtil.getLocale(), breader);
                }
            }
            catch (IOException io)
            {
                try
                {
                    breader.reset();
                }
                catch (IOException e)
                {
                    throw new AnalysisException("Failed to reset buffered reader - token stream will be invalid", e);
                }
                return null;
            }
        }   
        

        private Analyzer getAnalyser(String fieldName, Locale locale)
        {
             if(schema != null)
             {
                 if(fieldName.contains("l_@{"))
                 {
                     FieldType fieldType = schema.getFieldTypeByName("identifier");
                     return selectAnalyzer(fieldType);
                 }
                 else if(fieldName.contains("lt@{"))
                 {
                     StringBuilder builder = new StringBuilder();
                     builder.append("text_");
                     builder.append(locale.getLanguage());
                     FieldType fieldType = schema.getFieldTypeByName(builder.toString());
                     if(fieldType == null)
                     {
                         fieldType = schema.getFieldTypeByName("text_en");
                     }
                     return selectAnalyzer(fieldType);
                 }
                 else
                 {
                     return null;
                 }
                 
             }
             else
             {
                 return null;
             }
        }



		private Analyzer selectAnalyzer(FieldType fieldType) {
			 if(mode == Mode.INDEX)
			 {
				 return fieldType.getIndexAnalyzer();
			 }
			 else if(mode == Mode.QUERY)
			 {
				 return fieldType.getQueryAnalyzer();
			 }
			 else
			 {
				 return null;
			 }
		}
     

    }
}
