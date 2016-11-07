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
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.Iterator;
import java.util.LinkedList;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PackedTokenAttributeImpl;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

/**
 * @author andyh TODO To change the template for this generated type comment go to Window - Preferences - Java - Code
 *         Style - Code Templates
 */
public class PathTokenFilter extends Tokenizer
{
    public final static String INTEGER_FORMAT = "0000000000";

    public final static char PATH_SEPARATOR = ';';

    public final static char NAMESPACE_START_DELIMITER = '{';

    public final static char NAMESPACE_END_DELIMITER = '}';

    public final static String SEPARATOR_TOKEN_TEXT = ";";

    public final static String NO_NS_TOKEN_TEXT = "<No Namespace>";

    public final static String TOKEN_TYPE_PATH_SEP = "PATH_SEPARATOR";

    public final static String TOKEN_TYPE_PATH_LENGTH = "PATH_LENGTH";

    public final static String TOKEN_TYPE_PATH_ELEMENT_NAME = "PATH_ELEMENT_NAME";

    public final static String TOKEN_TYPE_PATH_ELEMENT_NAMESPACE = "PATH_ELEMENT_NAMESPACE";

    public final static String TOKEN_TYPE_PATH_ELEMENT_NAMESPACE_PREFIX = "PATH_ELEMENT_NAMESPACE_PREFIX";

    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private final OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private final TypeAttribute typeAtt = addAttribute(TypeAttribute.class);
    
    private final PositionIncrementAttribute posIncAtt = addAttribute(PositionIncrementAttribute.class);
    
    char pathSeparator;

    String separatorTokenText;

    String noNsTokenText;

    char nsStartDelimiter;

    int nsStartDelimiterLength;

    char nsEndDelimiter;

    int nsEndDelimiterLength;

    char nsPrefixDelimiter = ':';

    LinkedList<PackedTokenAttributeImpl> tokens = new LinkedList<PackedTokenAttributeImpl>();

    Iterator<PackedTokenAttributeImpl> it = null;

    private boolean includeNamespace;

    private boolean endOfStream = false;
    
    public PathTokenFilter(char pathSeparator, String separatorTokenText, String noNsTokenText,
            char nsStartDelimiter, char nsEndDelimiter, boolean includeNameSpace)
    {
        this.pathSeparator = pathSeparator;
        this.separatorTokenText = separatorTokenText;
        this.noNsTokenText = noNsTokenText;
        this.nsStartDelimiter = nsStartDelimiter;
        this.nsEndDelimiter = nsEndDelimiter;
        this.includeNamespace = includeNameSpace;

        this.nsStartDelimiterLength = 1;
        this.nsEndDelimiterLength = 1;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.apache.lucene.analysis.TokenStream#next()
     */

    public PackedTokenAttributeImpl next() throws IOException
    {
    	PackedTokenAttributeImpl nextToken;
        if (it == null)
        {
            buildTokenListAndIterator();
        }
        if (it.hasNext())
        {
            nextToken = it.next();
        }
        else
        {
            nextToken = null;
        }
        return nextToken;
    }

    // TODO: temporary replacement for Token.termText()
    private String termText(PackedTokenAttributeImpl token)
    {
        return new String(token.buffer(), 0, token.length());
    }
    
    private void buildTokenListAndIterator() throws IOException
    {
        NumberFormat nf = new DecimalFormat(INTEGER_FORMAT);

        // Could optimise to read each path ata time - not just all paths
        int insertCountAt = 0;
        int lengthCounter = 0;
        PackedTokenAttributeImpl t;
        PackedTokenAttributeImpl pathSplitToken = null;
        PackedTokenAttributeImpl nameToken = null;
        PackedTokenAttributeImpl countToken = null;
        PackedTokenAttributeImpl namespaceToken = null;
        while ((t = nextToken()) != null)
        {
            String text = termText(t);

            if (text.length() == 0)
            {
                continue; // Skip if we find // or /; or ;; etc
            }

            if (text.charAt(text.length() - 1) == pathSeparator)
            {
                text = text.substring(0, text.length() - 1);
                pathSplitToken = new PackedTokenAttributeImpl();
                pathSplitToken.setEmpty().append(separatorTokenText);
                pathSplitToken.setOffset(t.startOffset(), t.endOffset());
                pathSplitToken.setType( TOKEN_TYPE_PATH_SEP);
                pathSplitToken.setPositionIncrement(1);

            }

            int split = -1;
            boolean isPrefix = false;

            if ((text.length() > 0) && (text.charAt(0) == nsStartDelimiter))
            {
                split = text.indexOf(nsEndDelimiter);
            }

            if (split == -1)
            {
                split = text.indexOf(nsPrefixDelimiter);
                isPrefix = true;
            }

            if (split == -1)
            {
                namespaceToken = new PackedTokenAttributeImpl();
                namespaceToken.setEmpty().append(noNsTokenText);
                namespaceToken.setOffset(t.startOffset(), t.startOffset());
                namespaceToken.setType(TOKEN_TYPE_PATH_ELEMENT_NAMESPACE);
                nameToken = new PackedTokenAttributeImpl();
                nameToken.setEmpty().append(text);
                nameToken.setOffset(t.startOffset(), t.endOffset());
                
                nameToken.setType(TOKEN_TYPE_PATH_ELEMENT_NAME);
            }
            else
            {
                if (isPrefix)
                {
                    namespaceToken = new PackedTokenAttributeImpl();
                    namespaceToken.setEmpty().append(text.substring(0, split));
                    namespaceToken.setOffset(t.startOffset(), t.startOffset() + split);
                    namespaceToken.setType(TOKEN_TYPE_PATH_ELEMENT_NAMESPACE_PREFIX);
                    nameToken = new PackedTokenAttributeImpl();
                    nameToken.setEmpty().append(text.substring(split + 1));
                    nameToken.setOffset(t.startOffset() + split + 1, t.endOffset());
                    nameToken.setType(TOKEN_TYPE_PATH_ELEMENT_NAME);
                }
                else
                {
                    namespaceToken = new PackedTokenAttributeImpl();
                    namespaceToken.setEmpty().append(text.substring(nsStartDelimiterLength, (split + nsEndDelimiterLength - 1)));
                    namespaceToken.setOffset(t.startOffset(), t.startOffset() + split);
                    namespaceToken.setType(TOKEN_TYPE_PATH_ELEMENT_NAMESPACE);
                    nameToken = new PackedTokenAttributeImpl();
                    nameToken.setEmpty().append(text.substring(split + nsEndDelimiterLength));
                    nameToken.setOffset(t.startOffset() + split + nsEndDelimiterLength, t.endOffset());
                    nameToken.setType(TOKEN_TYPE_PATH_ELEMENT_NAME);
                }
            }

            namespaceToken.setPositionIncrement(1);
            nameToken.setPositionIncrement(1);

            if (includeNamespace)
            {
                if (termText(namespaceToken).equals(""))
                {
                    namespaceToken = new PackedTokenAttributeImpl();
                    namespaceToken.setEmpty().append(noNsTokenText);
                    namespaceToken.setOffset(t.startOffset(), t.startOffset());
                    namespaceToken.setType(TOKEN_TYPE_PATH_ELEMENT_NAMESPACE);
                    namespaceToken.setPositionIncrement(1);
                }

                tokens.add(namespaceToken);

            }
            tokens.add(nameToken);

            lengthCounter++;

            if (pathSplitToken != null)
            {

                String countString = nf.format(lengthCounter);
                countToken = new PackedTokenAttributeImpl();
                countToken.setEmpty().append(countString);
                countToken.setOffset(t.startOffset(), t.endOffset());
                countToken.setType(TOKEN_TYPE_PATH_SEP);
                countToken.setPositionIncrement(1);

                tokens.add(insertCountAt, countToken);
                tokens.add(pathSplitToken);

                lengthCounter = 0;
                insertCountAt = tokens.size();

                pathSplitToken = null;
            }
        }

        String countString = nf.format(lengthCounter);
        countToken = new PackedTokenAttributeImpl();
        countToken.setEmpty().append(countString);
        countToken.setOffset(0, 0); 
        countToken.setType(TOKEN_TYPE_PATH_SEP);
        countToken.setPositionIncrement(1);

        tokens.add(insertCountAt, countToken);

        if ((tokens.size() == 0) || !(termText(tokens.get(tokens.size() - 1)).equals(TOKEN_TYPE_PATH_SEP)))
        {
            pathSplitToken = new PackedTokenAttributeImpl();
            pathSplitToken.setEmpty().append(separatorTokenText);
            pathSplitToken.setOffset(0, 0);
            pathSplitToken.setType(TOKEN_TYPE_PATH_SEP);
            pathSplitToken.setPositionIncrement(1);
            tokens.add(pathSplitToken);
        }

        it = tokens.iterator();
    }

    int readerPosition = 0;

    private PackedTokenAttributeImpl nextToken() throws IOException
    {
        if (endOfStream)
        {
            return null;
        }
        StringBuilder buffer = new StringBuilder(64);
        boolean inNameSpace = false;
        int start = readerPosition;
        int current;
        char c;
        while ((current = input.read()) != -1)
        {
            c = (char) current;
            readerPosition++;
            if (c == nsStartDelimiter)
            {
                inNameSpace = true;
            }
            else if (c == nsEndDelimiter)
            {
                inNameSpace = false;
            }
            else if (!inNameSpace && (c == '/'))
            {
            	PackedTokenAttributeImpl qNameToken =  new PackedTokenAttributeImpl();
            	qNameToken.setEmpty().append(buffer.toString());
            	qNameToken.setOffset(start, readerPosition - 1);
            	qNameToken.setType("QNAME");
                return qNameToken;
            }
            else if (!inNameSpace && (c == ';'))
            {
                buffer.append(c);
                PackedTokenAttributeImpl lastQNameToken = new PackedTokenAttributeImpl();
                lastQNameToken.setEmpty().append(buffer.toString());
                lastQNameToken.setOffset(start, readerPosition);
                lastQNameToken.setType("LASTQNAME");
                return lastQNameToken;
            }

            buffer.append(c);
        }
        int end = readerPosition - 1;
        // Stop the final token being returned with an end before the start.
        if (start > end)
        {
            end = start;
        }
        endOfStream = true;
        if (!inNameSpace)
        {
        	PackedTokenAttributeImpl qNameToken = new PackedTokenAttributeImpl();
        	qNameToken.setEmpty().append(buffer.toString());
        	qNameToken.setOffset(start, end);
        	qNameToken.setType("QNAME");
            return qNameToken;
        }
        else
        {
            throw new IllegalStateException("QName terminated incorrectly: " + buffer.toString());
        }

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
        offsetAtt.setOffset(correctOffset(next.startOffset()), correctOffset(next.endOffset()));
        typeAtt.setType(next.type());
        posIncAtt.setPositionIncrement(next.getPositionIncrement());
        return true;
    }

    @Override
    public void reset() throws IOException
    {
        super.reset();
        tokens.clear();
        it = null;
        readerPosition = 0;
        endOfStream = false;
    }

    @Override
    public void end() throws IOException
    {
        super.end();
        int finalOffset = correctOffset(readerPosition);
        offsetAtt.setOffset(finalOffset, finalOffset);
    }
}