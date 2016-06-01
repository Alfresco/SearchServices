package org.apache.lucene.analysis.minhash;
/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizerFactory;
import org.apache.lucene.analysis.minhash.MinHashFilter.FixedSizeTreeSet;
import org.apache.lucene.analysis.minhash.MinHashFilter.LongPair;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.util.CharFilterFactory;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause.Occur;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.ConstantScoreQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.RegExp;
import org.junit.Test;

public class MinHashFilterTest extends BaseTokenStreamTestCase
{
    @Test
    public void testIntHash() {
      LongPair hash = new LongPair();
      MinHashFilter.murmurhash3_x64_128(MinHashFilter.getBytes(0), 0, 4, 0, hash);
      assertEquals(-3485513579396041028L, hash.val1);
      assertEquals(6383328099726337777L, hash.val2);
    }
    
    @Test
    public void testStringHash() throws UnsupportedEncodingException {
      LongPair hash = new LongPair();
      byte[] bytes = "woof woof woof woof woof".getBytes("UTF-16LE");
      MinHashFilter.murmurhash3_x64_128(bytes, 0, bytes.length, 0, hash);
      assertEquals(7638079586852243959L, hash.val1);
      assertEquals(4378804943379391304L, hash.val2);
    }
    
    @Test
    public void testSimpleOrder() throws UnsupportedEncodingException {
      LongPair hash1 = new LongPair();
      hash1.val1 = 1;
      hash1.val2 = 2;
      LongPair hash2 = new LongPair();
      hash2.val1 = 2;
      hash2.val2 = 1;
      assert (hash1.compareTo(hash2) > 0);
    }

    
    @Test
    public void testHashOrder() {
      assertTrue(!MinHashFilter.isLessThanUnsigned(0l, 0l));
      assertTrue(MinHashFilter.isLessThanUnsigned(0l, -1l));
      assertTrue(MinHashFilter.isLessThanUnsigned(1l, -1l));
      assertTrue(MinHashFilter.isLessThanUnsigned(-2l, -1l));
      assertTrue(MinHashFilter.isLessThanUnsigned(1l, 2l));
      assertTrue(MinHashFilter.isLessThanUnsigned(Long.MAX_VALUE, Long.MIN_VALUE));

      FixedSizeTreeSet<LongPair> minSet = new FixedSizeTreeSet<LongPair>(500);
      HashSet<LongPair> unadded = new HashSet<LongPair>();
      for (int i = 0; i < 100; i++) {
        LongPair hash = new LongPair();
        MinHashFilter.murmurhash3_x64_128(MinHashFilter.getBytes(i), 0, 4, 0, hash);
        LongPair peek = null;
        if (minSet.size() > 0) {
          peek = minSet.last();
        }

        if (!minSet.add(hash)) {
          unadded.add(hash);
        } else {
          if (peek != null) {
            if ((minSet.size() == 500) && !peek.equals(minSet.last())) {
              unadded.add(peek);
            }
          }
        }
      }
      assertEquals(100, minSet.size());
      assertEquals(0, unadded.size());

      HashSet<LongPair> collisionDetection = new HashSet<LongPair>();
      unadded = new HashSet<LongPair>();
      minSet = new FixedSizeTreeSet<LongPair>(500);
      for (int i = 0; i < 1000000; i++) {
        LongPair hash = new LongPair();
        MinHashFilter.murmurhash3_x64_128(MinHashFilter.getBytes(i), 0, 4, 0, hash);
        collisionDetection.add(hash);
        LongPair peek = null;
        if (minSet.size() > 0) {
          peek = minSet.last();
        }

        if (!minSet.add(hash)) {
          unadded.add(hash);
        } else {
          if (peek != null) {
            if ((minSet.size() == 500) && !peek.equals(minSet.last())) {
              unadded.add(peek);
            }
          }
        }
      }
      assertEquals(1000000, collisionDetection.size());
      assertEquals(500, minSet.size());
      assertEquals(999500, unadded.size());

      LongPair last = null;
      LongPair current = null;
      while ((current = minSet.pollLast()) != null) {
        if (last != null) {
          assertTrue(isLessThan(current, last));
        } else {

        }
        last = current;
      }
    }

    
    
    @Test
    public void testHashNotRepeated() {
      FixedSizeTreeSet<LongPair> minSet = new FixedSizeTreeSet<LongPair>(500);
      HashSet<LongPair> unadded = new HashSet<LongPair>();
      for (int i = 0; i < 10000; i++) {
        LongPair hash = new LongPair();
        MinHashFilter.murmurhash3_x64_128(MinHashFilter.getBytes(i), 0, 4, 0, hash);
        LongPair peek = null;
        if (minSet.size() > 0) {
          peek = minSet.last();
        }
        if (!minSet.add(hash)) {
          unadded.add(hash);
        } else {
          if (peek != null) {
            if ((minSet.size() == 500) && !peek.equals(minSet.last())) {
              unadded.add(peek);
            }
          }
        }
      }
      assertEquals(500, minSet.size());

      LongPair last = null;
      LongPair current = null;
      while ((current = minSet.pollLast()) != null) {
        if (last != null) {
          assertTrue(isLessThan(current, last));
        } else {

        }
        last = current;
      }
    }

    @Test
    public void testMockShingleTokenizer() throws IOException {
      Tokenizer mockShingleTokenizer = createMockShingleTokenizer(5,
          "woof woof woof woof woof" + " " + "woof woof woof woof puff");
      assertTokenStreamContents(mockShingleTokenizer,
          new String[] {"woof woof woof woof woof", "woof woof woof woof puff"});
    }
    
    @Test
    public void testTokenStreamSingleInput() throws IOException {
      String[] hashes = new String[] {"℁팽徭聙↝ꇁ홱杯"};
      TokenStream ts = createTokenStream(5, "woof woof woof woof woof", 1, 1, 100, false);
      assertTokenStreamContents(ts, hashes, new int[] {0},
          new int[] {24}, new String[] {MinHashFilter.MIN_HASH_TYPE}, new int[] {1}, new int[] {1}, 24, 0, null,
          true);

      ts = createTokenStream(5, "woof woof woof woof woof", 2, 1, 1, false);
      assertTokenStreamContents(ts, new String[] {new String(new char[] {0, 0, 8449, 54077, 64133, 32857, 8605, 41409}),
          new String(new char[] {0, 1, 16887, 58164, 39536, 14926, 6529, 17276})}, new int[] {0, 0},
          new int[] {24, 24}, new String[] {MinHashFilter.MIN_HASH_TYPE, MinHashFilter.MIN_HASH_TYPE}, new int[] {1, 0}, new int[] {1, 1}, 24, 0, null,
          true);
    }
    
    @Test
    public void testTokenStream1() throws IOException {
      String[] hashes = new String[] {"℁팽徭聙↝ꇁ홱杯",
          new String(new char[] {36347, 63457, 43013, 56843, 52284, 34231, 57934, 42302})};

      TokenStream ts = createTokenStream(5, "woof woof woof woof woof" + " " + "woof woof woof woof puff", 1, 1, 100,false);
      assertTokenStreamContents(ts, hashes, new int[] {0, 0},
          new int[] {49, 49}, new String[] {MinHashFilter.MIN_HASH_TYPE, MinHashFilter.MIN_HASH_TYPE}, new int[] {1, 0},
          new int[] {1, 1}, 49, 0, null, true);
    }
    
    private ArrayList<String> getTokens(TokenStream ts) throws IOException {
        ArrayList<String> tokens = new ArrayList<String>();
        ts.reset();
        while (ts.incrementToken()) {
          CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);
          String token = new String(termAttribute.buffer(), 0, termAttribute.length());
          tokens.add(token);
        }
        ts.end();
        ts.close();

        return tokens;
      }
    
    private ArrayList<String> getTokens(Analyzer analyzer, String field, String value) throws IOException
    {
        ArrayList<String> tokens = new ArrayList<String>();
        
        TokenStream ts = analyzer.tokenStream(field, value);
        ts.reset();
        while(ts.incrementToken())
        {
            CharTermAttribute termAttribute = ts.getAttribute(CharTermAttribute.class);
            String token = new String(termAttribute.buffer(), 0, termAttribute.length());
            tokens.add(token);
        }
        ts.end();
        ts.close();     
        
        return tokens;
    }
    
    @Test
    public void testTokenStream2() throws IOException {
      TokenStream ts = createTokenStream(5, "woof woof woof woof woof" + " " + "woof woof woof woof puff", 100, 1, 1, false);
      ArrayList<String> tokens = getTokens(ts);
      ts.close();

      assertEquals(100, tokens.size());
    }

    @Test
    public void testTokenStream3() throws IOException {
      TokenStream ts = createTokenStream(5, "woof woof woof woof woof" + " " + "woof woof woof woof puff", 10, 1, 10, false);
      ArrayList<String> tokens = getTokens(ts);
      ts.close();

      assertEquals(20, tokens.size());
    }
    
    @Test
    public void testTokenStream4() throws IOException {
      TokenStream ts = createTokenStream(5, "woof woof woof woof woof" + " " + "woof woof woof woof puff", 10, 10, 1, false);
      ArrayList<String> tokens = getTokens(ts);
      ts.close();

      assertEquals(20, tokens.size());
      
      ts = createTokenStream(5, "woof woof woof woof woof" + " " + "woof woof woof woof puff", 10, 10, 1, true);
      tokens = getTokens(ts);
      ts.close();

      assertEquals(100, tokens.size());
      
    }
    
    @Test
    public void testLSHQuery() throws IOException 
    {
            Analyzer analyzer = createMinHashAnalyzer(5, 1, 100);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
        
            RAMDirectory directory = new RAMDirectory();
            IndexWriter writer = new IndexWriter(directory, config);
            Document doc = new Document();
            doc.add(new TextField("text", "woof woof woof woof woof", Store.NO));
            writer.addDocument(doc);
        
            doc = new Document();
            doc.add(new TextField("text", "woof woof woof woof woof puff", Store.NO));
            writer.addDocument(doc);
            
            doc = new Document();
            doc.add(new TextField("text", "woof woof woof woof puff", Store.NO));
            writer.addDocument(doc);
        
            writer.commit();
            writer.close();
        
            IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
          
            BooleanQuery.Builder builder = new BooleanQuery.Builder();
            builder.add(new ConstantScoreQuery(new TermQuery(new Term("text", "℁팽徭聙↝ꇁ홱杯"))), Occur.SHOULD);
            builder.add(new ConstantScoreQuery(new TermQuery(new Term("text", new String(new char[] {36347, 63457, 43013, 56843, 52284, 34231, 57934, 42302})))), Occur.SHOULD);
            builder.setDisableCoord(true);
            TopDocs topDocs = searcher.search(builder.build(), 10);
        
            assertEquals(3, topDocs.totalHits);
            
            float score = topDocs.scoreDocs[0].score;
            assertEquals(topDocs.scoreDocs[1].score, score/2, 0f);
            assertEquals(topDocs.scoreDocs[2].score, score/2, 0f);
         
    }
    
    
    
    @Test
    public void testLSHQuery2() throws IOException 
    {
            String[] parts = new String[]{"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
            int min = 5;
        
            Analyzer analyzer = createMinHashAnalyzer(min, 1, 100);
            IndexWriterConfig config = new IndexWriterConfig(analyzer);
        
            RAMDirectory directory = new RAMDirectory();
            IndexWriter writer = new IndexWriter(directory, config);
          
            for(int i = 0; i < parts.length; i++)
            {
                StringBuilder builder = new StringBuilder();
                for(int j = 0; j < parts.length -i; j++)
                {
                    if(builder.length() > 0)
                    {
                        builder.append(" ");
                    }
                    builder.append(parts[i+j]);
                    if(j >= min -1)
                    {
                        Document doc = new Document();
                        doc.add(new TextField("text", builder.toString(), Store.NO));
                        writer.addDocument(doc);
                    }
                }    
            }
               
            writer.commit();
            writer.close();
        
             
            IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(directory));
          
            TopDocs topDocs = searcher.search(buildQuery("text", "one two three four five", min, 1, 100), 100);        
            assertEquals(6, topDocs.totalHits);
            assertAllScores(topDocs, 1.0f);
            topDocs = searcher.search(buildQuery("text", "two three four five six", min, 1, 100), 100);        
            assertEquals(10, topDocs.totalHits);
            assertAllScores(topDocs, 1.0f);
            topDocs = searcher.search(buildQuery("text", "three four five six seven", min, 1, 100), 100);        
            assertEquals(12, topDocs.totalHits);
            assertAllScores(topDocs, 1.0f);
            topDocs = searcher.search(buildQuery("text", "four five six seven eight", min, 1, 100), 100);        
            assertEquals(12, topDocs.totalHits);
            assertAllScores(topDocs, 1.0f);
            topDocs = searcher.search(buildQuery("text", "five six seven eight nine", min, 1, 100), 100);        
            assertEquals(10, topDocs.totalHits);
            assertAllScores(topDocs, 1.0f);
            topDocs = searcher.search(buildQuery("text", "six seven eight nine ten", min, 1, 100), 100);        
            assertEquals(6, topDocs.totalHits);
            assertAllScores(topDocs, 1.0f);
            
            topDocs = searcher.search(buildQuery("text", "one two three four five six", min, 1, 100), 100);        
            assertEquals(11, topDocs.totalHits);
            
            topDocs = searcher.search(buildQuery("text", "one two three four five six seven eight nine ten", min, 1, 100), 100);        
            assertEquals(21, topDocs.totalHits);
            for(int i = 0; i < topDocs.totalHits; i++)
            {
                System.out.println(i+" = "+topDocs.scoreDocs[i]);
            }
            
            float topScore = 6.0f;
            assertEquals(topDocs.scoreDocs[0].score, topScore, 0.001f);
            assertEquals(topDocs.scoreDocs[1].score, topScore * 5/6, 0.001f);
            assertEquals(topDocs.scoreDocs[2].score, topScore * 5/6, 0.001f);
            assertEquals(topDocs.scoreDocs[3].score, topScore * 4/6, 0.001f);
            assertEquals(topDocs.scoreDocs[4].score, topScore * 4/6, 0.001f);
            assertEquals(topDocs.scoreDocs[5].score, topScore * 4/6, 0.001f);
            assertEquals(topDocs.scoreDocs[6].score, topScore * 3/6, 0.001f);
            assertEquals(topDocs.scoreDocs[7].score, topScore * 3/6, 0.001f);
            assertEquals(topDocs.scoreDocs[8].score, topScore * 3/6, 0.001f);
            assertEquals(topDocs.scoreDocs[9].score, topScore * 3/6, 0.001f);
            assertEquals(topDocs.scoreDocs[10].score, topScore * 2/6, 0.001f);
            assertEquals(topDocs.scoreDocs[11].score, topScore * 2/6, 0.001f);
            assertEquals(topDocs.scoreDocs[12].score, topScore * 2/6, 0.001f);
            assertEquals(topDocs.scoreDocs[13].score, topScore * 2/6, 0.001f);
            assertEquals(topDocs.scoreDocs[14].score, topScore * 2/6, 0.001f);
            assertEquals(topDocs.scoreDocs[15].score, topScore * 1/6, 0.001f);
            assertEquals(topDocs.scoreDocs[16].score, topScore * 1/6, 0.001f);
            assertEquals(topDocs.scoreDocs[17].score, topScore * 1/6, 0.001f);
            assertEquals(topDocs.scoreDocs[18].score, topScore * 1/6, 0.001f);
            assertEquals(topDocs.scoreDocs[19].score, topScore * 1/6, 0.001f);
            assertEquals(topDocs.scoreDocs[20].score, topScore * 1/6, 0.001f);
         
    }
    
    
    private void assertAllScores(TopDocs topDocs, float score)
    {
        for(int i = 0; i < topDocs.totalHits; i++)
        {
            assertEquals(topDocs.scoreDocs[i].score, score, 0f);
        }
    }
    
    private Query buildQuery(String field, String query, int min, int hashCount, int hashSetSize) throws IOException
    {
        TokenizerChain chain = createMinHashAnalyzer(min, hashCount, hashSetSize);
        ArrayList<String> tokens = getTokens(chain, field, query);
        chain.close();
        
        BooleanQuery.Builder builder = new BooleanQuery.Builder();
        for(String token : tokens)
        {
            builder.add(new ConstantScoreQuery(new TermQuery(new Term("text", token))), Occur.SHOULD);
        }
        builder.setDisableCoord(true);
        return builder.build();
    }
    
    public static TokenStream createTokenStream(int shingleSize, String shingles, int hashCount, int bucketCount, int hashSetSize, boolean withRotation) {
        Tokenizer tokenizer = createMockShingleTokenizer(shingleSize, shingles);
        HashMap<String,String> lshffargs = new HashMap<String,String>();
        lshffargs.put("hashCount", "" + hashCount);
        lshffargs.put("bucketCount", "" + bucketCount);
        lshffargs.put("hashSetSize", "" + hashSetSize);
        lshffargs.put("withRotation", "" + withRotation);
        MinHashFilterFactory lshff = new MinHashFilterFactory(lshffargs);
        return lshff.create(tokenizer);
      }
    
    public static TokenizerChain createMinHashAnalyzer(int min, int hashCount, int hashSetSize) 
    {
        WhitespaceTokenizerFactory icutf = new WhitespaceTokenizerFactory(Collections.<String, String>emptyMap());
        HashMap<String, String> sffargs = new HashMap<String, String>();
        sffargs.put("minShingleSize", ""+min);
        sffargs.put("maxShingleSize", ""+min);
        sffargs.put("outputUnigrams", "false");
        sffargs.put("outputUnigramsIfNoShingles", "false");
        sffargs.put("tokenSeparator", " ");
        ShingleFilterFactory sff = new ShingleFilterFactory(sffargs);
        HashMap<String, String> lshffargs = new HashMap<String, String>();
        lshffargs.put("hashCount", ""+hashCount);
        lshffargs.put("hashSetSize", ""+hashSetSize);
        MinHashFilterFactory lshff = new MinHashFilterFactory(lshffargs);

        TokenizerChain chain = new TokenizerChain(new CharFilterFactory[]{}, icutf, new TokenFilterFactory[]{sff, lshff});
        return chain;
    }
    
    public static Tokenizer createMockShingleTokenizer(int shingleSize, String shingles) {
        MockTokenizer tokenizer = new MockTokenizer(
            new CharacterRunAutomaton(new RegExp("[^ \t\r\n]+([ \t\r\n]+[^ \t\r\n]+){4}").toAutomaton()),
            true);
        tokenizer.setEnableChecks(true);
        if (shingles != null) {
          tokenizer.setReader(new StringReader(shingles));
        }
        return tokenizer;
      }
    
    private boolean isLessThan(LongPair hash1, LongPair hash2) {
        if (MinHashFilter.isLessThanUnsigned(hash1.val2, hash2.val2)) {
          return true;
        } else if (hash1.val2 == hash2.val2) {
          return (MinHashFilter.isLessThanUnsigned(hash1.val1, hash2.val1));
        } else {
          return false;
        }
      }
    
    
    /**
     * An analyzer that uses a tokenizer and a list of token filters to
     * create a TokenStream - lifted from SOLR to make this analyzer test lucene only. 
     */
    public static class TokenizerChain extends Analyzer {
      
      final private CharFilterFactory[] charFilters;
      final private TokenizerFactory tokenizer;
      final private TokenFilterFactory[] filters;

     
      /** 
       * Creates a new TokenizerChain.
       *
       * @param charFilters Factories for the CharFilters to use, if any - if null, will be treated as if empty.
       * @param tokenizer Factory for the Tokenizer to use, must not be null.
       * @param filters Factories for the TokenFilters to use if any- if null, will be treated as if empty.
       */
      public TokenizerChain(CharFilterFactory[] charFilters, TokenizerFactory tokenizer, TokenFilterFactory[] filters) {
        this.charFilters = charFilters;
        this.tokenizer = tokenizer;
        this.filters = filters;
      }

      @Override
      public Reader initReader(String fieldName, Reader reader) {
        if (charFilters != null && charFilters.length > 0) {
          Reader cs = reader;
          for (CharFilterFactory charFilter : charFilters) {
            cs = charFilter.create(cs);
          }
          reader = cs;
        }
        return reader;
      }

      @Override
      protected TokenStreamComponents createComponents(String fieldName) {
        Tokenizer tk = tokenizer.create();
        TokenStream ts = tk;
        for (TokenFilterFactory filter : filters) {
          ts = filter.create(ts);
        }
        return new TokenStreamComponents(tk, ts);
      }

      @Override
      public String toString() {
        StringBuilder sb = new StringBuilder("TokenizerChain(");
        for (CharFilterFactory filter: charFilters) {
          sb.append(filter);
          sb.append(", ");
        }
        sb.append(tokenizer);
        for (TokenFilterFactory filter: filters) {
          sb.append(", ");
          sb.append(filter);
        }
        sb.append(')');
        return sb.toString();
      }
    }
}
