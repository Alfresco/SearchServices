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
import java.io.StringReader;

import junit.framework.TestCase;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

public class PathTokenFilterTest extends TestCase
{

    public PathTokenFilterTest()
    {
        super();
    }

    public PathTokenFilterTest(String arg0)
    {
        super(arg0);
    }

    
    public void testFullPath() throws IOException
    {
        tokenise("{uri1}one", new String[]{"uri1", "one"});
        tokenise("/{uri1}one", new String[]{"uri1", "one"});
        tokenise("{uri1}one/{uri2}two/", new String[]{"uri1", "one", "uri2", "two"});
        tokenise("/{uri1}one/{uri2}two/", new String[]{"uri1", "one", "uri2", "two"});
        tokenise("{uri1}one/{uri2}two/{uri3}three", new String[]{"uri1", "one", "uri2", "two", "uri3", "three"});
        tokenise("/{uri1}one/{uri2}two/{uri3}three", new String[]{"uri1", "one", "uri2", "two", "uri3", "three"});
        try
        {
           tokenise("{uri1}one;{uri2}two/", new String[]{"uri1", "one", "uri2", "two"});
        }
        catch(IllegalStateException ise)
        {
            
        }
       
    }
    
    
    public void testPrefixPath() throws IOException
    {
        tokenise("uri1:one", new String[]{"uri1", "one"});
        tokenise("/uri1:one", new String[]{"uri1", "one"});
        tokenise("uri1:one/uri2:two/", new String[]{"uri1", "one", "uri2", "two"});
        tokenise("/uri1:one/uri2:two/", new String[]{"uri1", "one", "uri2", "two"});
        tokenise("uri1:one/uri2:two/uri3:three", new String[]{"uri1", "one", "uri2", "two", "uri3", "three"});
        tokenise("/uri1:one/uri2:two/uri3:three", new String[]{"uri1", "one", "uri2", "two", "uri3", "three"});
        try
        {
           tokenise("{uri1}one;{uri2}two/", new String[]{"uri1", "one", "uri2", "two"});
        }
        catch(IllegalStateException ise)
        {
            
        }
       
    }
    
    
    public void testMixedPath() throws IOException
    {
     
        tokenise("{uri1}one/uri2:two/", new String[]{"uri1", "one", "uri2", "two"});
        tokenise("/{uri1}one/uri2:two/", new String[]{"uri1", "one", "uri2", "two"});
        tokenise("uri1:one/{uri2}two/uri3:three", new String[]{"uri1", "one", "uri2", "two", "uri3", "three"});
        tokenise("/uri1:one/{uri2}two/uri3:three", new String[]{"uri1", "one", "uri2", "two", "uri3", "three"});
        try
        {
           tokenise("{uri1}one;{uri2}two/", new String[]{"uri1", "one", "uri2", "two"});
        }
        catch(IllegalStateException ise)
        {
            
        }
       
    }
    
    public void testTokenizerReuse() throws IOException
    {
        // We should be able to use the same Tokenizer twice.
        final String path = "uri1:one";
        StringReader reader = new StringReader(path);
        PathTokenFilter ts = new PathTokenFilter(PathTokenFilter.PATH_SEPARATOR,
                PathTokenFilter.SEPARATOR_TOKEN_TEXT, PathTokenFilter.NO_NS_TOKEN_TEXT,
                PathTokenFilter.NAMESPACE_START_DELIMITER, PathTokenFilter.NAMESPACE_END_DELIMITER, true);
        ts.setReader(reader);

        OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
        
        // First use
        tokenise(ts, new String[]{"uri1", "one"});
        assertEquals(path.length(), offsetAtt.startOffset());
        assertEquals(path.length(), offsetAtt.endOffset());
        
        // Second use
        final String path2 = "/{uri1}one/uri2:two/";
        StringReader reader2 = new StringReader(path2);
        ts.setReader(reader2);
        tokenise(ts, new String[]{"uri1", "one", "uri2", "two"});
        assertEquals(path2.length(), offsetAtt.startOffset());
        assertEquals(path2.length(), offsetAtt.endOffset());
    }
    
    
    public void testAttributesAfterStreamEnd() throws IOException
    {
        final String path = "uri1:one";
        StringReader reader = new StringReader(path);
        PathTokenFilter ts = new PathTokenFilter(PathTokenFilter.PATH_SEPARATOR,
                PathTokenFilter.SEPARATOR_TOKEN_TEXT, PathTokenFilter.NO_NS_TOKEN_TEXT,
                PathTokenFilter.NAMESPACE_START_DELIMITER, PathTokenFilter.NAMESPACE_END_DELIMITER, true);
        ts.setReader(reader);

        CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
        TypeAttribute typeAtt = ts.addAttribute(TypeAttribute.class);
        OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
        PositionIncrementAttribute posIncAtt = ts.addAttribute(PositionIncrementAttribute.class);
        
        // PathTokenFilter.end() will be called after all tokens consumed.
        tokenise(ts, new String[]{"uri1", "one"});
        
        // Check attributes cleaned up
        assertEquals("", termAtt.toString());
        assertEquals("word", typeAtt.type()); // the default
        assertEquals(0, posIncAtt.getPositionIncrement());
        // Final offset...
        assertEquals(path.length(), offsetAtt.startOffset());
        assertEquals(path.length(), offsetAtt.endOffset());
    }
    
    private void tokenise(String path, String[] tokens) throws IOException
    {
        StringReader reader = new StringReader(path);
        PathTokenFilter ts = new PathTokenFilter( PathTokenFilter.PATH_SEPARATOR,
                PathTokenFilter.SEPARATOR_TOKEN_TEXT, PathTokenFilter.NO_NS_TOKEN_TEXT,
                PathTokenFilter.NAMESPACE_START_DELIMITER, PathTokenFilter.NAMESPACE_END_DELIMITER, true);
        ts.setReader(reader);
        
        tokenise(ts, tokens);
    }

    private void tokenise(TokenStream ts, String[] tokens) throws IOException
    {
       int i = 0;
       
       CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);
       TypeAttribute typeAtt = ts.addAttribute(TypeAttribute.class);
       
       try
       {
           ts.reset();
           while (ts.incrementToken())
           {
               System.out.println("token: " + ts.reflectAsString(true));
               
               String termText = termAtt.toString();
               
               if(typeAtt.type().equals(PathTokenFilter.TOKEN_TYPE_PATH_ELEMENT_NAMESPACE))
               {
                   assert(i % 2 == 0);
                   assertEquals(termText, tokens[i++]);
               }
               else if(typeAtt.type().equals(PathTokenFilter.TOKEN_TYPE_PATH_ELEMENT_NAMESPACE_PREFIX))
               {
                   assert(i % 2 == 0);
                   assertEquals(termText, tokens[i++]);
               }
               else if(typeAtt.type().equals(PathTokenFilter.TOKEN_TYPE_PATH_ELEMENT_NAME))
               {
                   assert(i % 2 == 1);
                   assertEquals(termText, tokens[i++]);
               }
           }
           ts.end();
       }
       finally
       {
           ts.close();
       }
       
       if(i != tokens.length)
       {
           fail("Invalid number of tokens, found "+i+" and expected "+tokens.length);
       }
    }
}
