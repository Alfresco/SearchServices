/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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
package org.apache.lucene.analysis.minhash;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.minhash.MinHashFilter.LongPair;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

public class ContextAccumulatingFilter extends TokenFilter
{   

    private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
    
    private static  Map<String, Set<LongPair> > contexts = new ConcurrentHashMap<String, Set<LongPair> >(128);
 
    protected ContextAccumulatingFilter(TokenStream input)
    {
        super(input);
    }
  

    @Override
    final public boolean incrementToken() throws IOException
    {
        // Pull the underlying stream of tokens
        // Hash each token found
        // Generate the required number of variants of this hash
        // Keep the minimum hash value found so far of each variant

        boolean incremented = input.incrementToken();
        
        if(incremented)
        {
            String current = new String(termAttribute.buffer(), 0, termAttribute.length());
            String[] parts = current.split(" ");
            StringBuilder contextBuilder = new StringBuilder();
            StringBuilder wordBuilder = new StringBuilder();
            for(int i = 0, l = parts.length; i < l; i++)
            {
                if( (i == Math.round(Math.floor((l-1)/2.0))) || (i == Math.round(Math.ceil((l-1)/2.0))))
                {
                    if(wordBuilder.length() > 0)
                    {
                        wordBuilder.append(" ");
                    }
                    wordBuilder.append(parts[i]);
                }
                else
                {
                    if(contextBuilder.length() > 0)
                    {
                        contextBuilder.append(" ");
                    }
                    contextBuilder.append(parts[i]);
                }
            }
            
            String word = wordBuilder.toString();
            String context = contextBuilder.toString();
            
            Set<LongPair> wordContexts = contexts.get(word);
            if(wordContexts == null)
            {
                wordContexts = ConcurrentHashMap.<LongPair>newKeySet(128);
                contexts.put(word, wordContexts);
            }
            byte[] bytes = context.getBytes("UTF-16LE");
            LongPair contextHash = new LongPair();
            MinHashFilter.murmurhash3_x64_128(bytes, 0, bytes.length, 0, contextHash);
            wordContexts.add(contextHash);
        }
        
        return incremented;
    }


    @Override
    public void end() throws IOException
    {
        super.end();
        long count = 0;
        for(Set<LongPair> thing : contexts.values())
        {
            count += thing.size();
        }
        
        System.out.println("Words = "+contexts.size() + "     contexts = "+count);
    } 
    
    
}
