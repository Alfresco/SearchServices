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

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;

import org.alfresco.repo.search.MLAnalysisMode;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * Create duplicate tokens for multilingual varients The forms are Tokens: Token - all languages {fr}Token - if a
 * language is specified {fr_CA}Token - if a language and country is specified {fr_CA_Varient}Token - for all three
 * {fr__Varient}Token - for a language varient with no country
 * 
 * @author andyh
 */
public class MLTokenDuplicator extends TokenStream
{
    private static Log    s_logger = LogFactory.getLog(MLTokenDuplicator.class);
    
    TokenStream source;

    Locale locale;

    Iterator<PackedTokenAttributeImpl> it;

    HashSet<String> prefixes;
    
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);

    private boolean done = false;

    public MLTokenDuplicator(TokenStream source, Locale locale, Reader reader, MLAnalysisMode mlAnalysisMode)
    {
        this.source = source;
        this.locale = locale;
        
        Collection<Locale> locales = MLAnalysisMode.getLocales(mlAnalysisMode, locale, false);
        prefixes = new HashSet<String>(locales.size());
        for(Locale toAdd : locales)
        {
            String localeString = toAdd.toString();
            if(localeString.length() == 0)
            {
                prefixes.add("");
            }
            else
            {
                StringBuilder builder = new StringBuilder(16);
                builder.append("{").append(localeString).append("}");
                prefixes.add(builder.toString());
            }
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("Locale "+ locale +" using "+mlAnalysisMode+" is "+prefixes);
        }

    }

    /**
     *
     * @param locale
     * @param mlAnalysisMode
     */
    public MLTokenDuplicator(Locale locale, MLAnalysisMode mlAnalysisMode)
    {
        this.locale = locale;
        
        Collection<Locale> locales = MLAnalysisMode.getLocales(mlAnalysisMode, locale, false);
        prefixes = new HashSet<String>(locales.size());
        for(Locale toAdd : locales)
        {
            String localeString = toAdd.toString();
            if(localeString.length() == 0)
            {
                prefixes.add("");
            }
            else
            {
                StringBuilder builder = new StringBuilder(16);
                builder.append("{").append(localeString).append("}");
                prefixes.add(builder.toString());
            }
        }
        if(s_logger.isDebugEnabled())
        {
            s_logger.debug("Locale "+ locale +" using "+mlAnalysisMode+" is "+prefixes);
        }
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.analysis.Tokenizer#close()
     */
    @Override
    public void close() throws IOException
    {
        source.close();
        super.close();
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.analysis.Tokenizer#reset()
     */
    @Override
    public void reset() throws IOException
    {
        source.reset();
        super.reset();
    }

    /* (non-Javadoc)
     * @see org.apache.lucene.analysis.TokenStream#end()
     */
    @Override
    public void end() throws IOException
    {
        source.end();
        super.end();
    }

    private PackedTokenAttributeImpl next() throws IOException
    {
    	PackedTokenAttributeImpl t = null;
        if (it == null)
        {
            it = buildIterator();
        }
        if (it == null)
        {
            return null;
        }
        if (it.hasNext())
        {
            t = it.next();
            
            return t;
        }
        else
        {
            it = null;
            t = this.next();
            return t;
        }
    }

    private Iterator<PackedTokenAttributeImpl> buildIterator() throws IOException
    {
        // TODO: use incrementToken() somehow?
        if(!done && source.incrementToken())
        {
            CharTermAttribute cta = source.getAttribute(CharTermAttribute.class);
            OffsetAttribute offsetAtt = source.getAttribute(OffsetAttribute.class);
            TypeAttribute typeAtt = null;
            if(source.hasAttribute(TypeAttribute.class))
            {
                typeAtt = source.getAttribute(TypeAttribute.class);
            }
            PositionIncrementAttribute posIncAtt = null;
            if(source.hasAttribute(PositionIncrementAttribute.class))
            {
                posIncAtt = source.getAttribute(PositionIncrementAttribute.class);
            }
            PackedTokenAttributeImpl token = new PackedTokenAttributeImpl();
            token.setEmpty().append(new String(cta.buffer()), 0, cta.length());
            token.setOffset(offsetAtt.startOffset(), offsetAtt.endOffset());
            if(typeAtt != null)
            {
                token.setType(typeAtt.type());
            }
            if(posIncAtt != null)
            {
                token.setPositionIncrement(posIncAtt.getPositionIncrement());
            }
            return buildIterator(token);
        }
        else
        {
            done = true;
            return buildIterator(null);
        }
        

    }


    public Iterator<PackedTokenAttributeImpl> buildIterator(PackedTokenAttributeImpl token)
    {
        if (token == null)
        {
            return null;
        }

        ArrayList<PackedTokenAttributeImpl> tokens = new ArrayList<PackedTokenAttributeImpl>(prefixes.size());
        for (String prefix : prefixes)
        {   
            
        	PackedTokenAttributeImpl newToken = new PackedTokenAttributeImpl();
        	newToken.setEmpty().append(prefix + termText(token));
        	newToken.setOffset(token.startOffset(), token.endOffset());
            newToken.setType(token.type());
            if (tokens.size() == 0)
            {
                newToken.setPositionIncrement(token.getPositionIncrement());
            }
            else
            {
                newToken.setPositionIncrement(0);
            }
            tokens.add(newToken);
        }
        return tokens.iterator();

    }

    @Override
    public final boolean incrementToken() throws IOException
    {
        clearAttributes();
        
        PackedTokenAttributeImpl next = next();
        if (next == null)
        {
            return false;
        }
        
        termAtt.copyBuffer(next.buffer(), 0, next.length());
        offsetAtt.setOffset(next.startOffset(), next.endOffset());
        typeAtt.setType(next.type());
        posIncAtt.setPositionIncrement(next.getPositionIncrement());
        return true;
        
    }

    // TODO: temporary replacement for Token.termText()
    private String termText(PackedTokenAttributeImpl token)
    {
        return new String(token.buffer(), 0, token.length());
    }
    
}
