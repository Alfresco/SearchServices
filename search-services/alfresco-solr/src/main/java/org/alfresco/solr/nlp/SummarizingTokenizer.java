/*
 * Copyright (C) 2005-2013 Alfresco Software Limited.
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
package org.alfresco.solr.nlp;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.NavigableSet;
import java.util.TreeSet;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.StopAnalyzer;
import org.apache.lucene.analysis.minhash.MinHashFilter;
import org.apache.lucene.analysis.minhash.MinHashFilter.LongPair;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.CharArraySet;

/**
 * @author Andy
 */
public class SummarizingTokenizer extends Tokenizer
{
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

    private OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    private boolean initialized = false;

    PorterStemmer stemmer = new PorterStemmer();

    CharArraySet stopWords = new CharArraySet(StopAnalyzer.ENGLISH_STOP_WORDS_SET, true);

    /*
     * (non-Javadoc)
     * @see org.apache.lucene.analysis.TokenStream#incrementToken()
     */
    @Override
    public boolean incrementToken() throws IOException
    {
        if (!initialized)
        {
            String asString = IOUtils.toString(input);
            ArrayList<Sentence> sentences = new ArrayList<Sentence>();
            TreeSet<HashEntry> orderedByHash = new TreeSet<HashEntry>();
            rankSentences(asString, sentences, orderedByHash, 3);

            for (int i = 0; i < 100; i++)
            {
                Sentence current = sentences.get(i);
                System.out.println("" + current.rank + " @ " + current.sentenceOrdinal + "    ->     " + current.sentence);
            }

        }
        return false;
    }

    /**
     * @param asString
     * @param sentences
     * @param orderedByHash
     * @throws UnsupportedEncodingException
     */
    private void rankSentences(String asString, ArrayList<Sentence> sentences, TreeSet<HashEntry> orderedByHash, int shingleSize) throws UnsupportedEncodingException
    {
        BreakIterator si = BreakIterator.getSentenceInstance();
        BreakIterator wi = BreakIterator.getWordInstance();
        si.setText(asString);
        int s_start = si.first();

        for (int s_end = si.next(); s_end != BreakIterator.DONE; s_start = s_end, s_end = si.next())
        {
            String previousStopWord = null;
            String text = asString.substring(s_start, s_end);
            Sentence sentence = new Sentence(text, sentences.size());

            ArrayList<String> words = new ArrayList<String>();
            wi.setText(text);
            int w_start = wi.first();
            for (int w_end = wi.next(); w_end != BreakIterator.DONE; w_start = w_end, w_end = wi.next())
            {
                String word = text.substring(w_start, w_end);
                if (isNotAllWhiteSpace(word))
                {
                    if ((shingleSize > 1) || !stopWords.contains(word))
                    {
                        word = stemmer.stem(word.toLowerCase());
                        words.add(word);
                    }

                }
            }

            ArrayList<String> shingles = getShingles(words, shingleSize);

            for (String shingle : shingles)
            {
                byte[] bytes = shingle.getBytes("UTF-16LE");
                LongPair hash = new LongPair();
                MinHashFilter.murmurhash3_x64_128(bytes, 0, bytes.length, 0, hash);

                HashEntry entry = new HashEntry(hash, orderedByHash.size());
                NavigableSet<HashEntry> existing = orderedByHash.subSet(entry, true, entry, true);
                if (existing.size() == 0)
                {
                    existing.add(entry);
                }
                else
                {
                    entry = existing.first();
                }
                entry.addSentence(sentence);
                sentence.addEntry(entry);
            }
            sentences.add(sentence);

        }

        // Are there any potential overlaps?
        BitSet sentenceOrdinalsWithAnyOverlap = new BitSet();
        for (HashEntry entry : orderedByHash)
        {
            if (entry.sentences.size() > 1)
            {
                for (Sentence sentence : entry.sentences)
                {
                    sentenceOrdinalsWithAnyOverlap.set(sentence.sentenceOrdinal);
                }
            }
        }

        // build graph

        System.out.println("Sentances - " + sentences.size());
        System.out.println("Overlap - " + sentenceOrdinalsWithAnyOverlap.cardinality());

        for (int i = sentenceOrdinalsWithAnyOverlap.nextSetBit(0); i >= 0; i = sentenceOrdinalsWithAnyOverlap.nextSetBit(i + 1))
        {
            Sentence current = sentences.get(i);
            int[] hashOverlapsBySentenceOrdinal = new int[sentences.size()];
            for (HashEntry entry : current.hashes)
            {
                for (Sentence sentence : entry.sentences)
                {
                    hashOverlapsBySentenceOrdinal[sentence.sentenceOrdinal]++;
                }
            }

            // build connectoins
            for (int j = 0; j < hashOverlapsBySentenceOrdinal.length; j++)
            {
                if (hashOverlapsBySentenceOrdinal[j] > 0)
                {
                    if (current.sentenceOrdinal != j)
                    {
                        //double weight = ((double) hashOverlapsBySentenceOrdinal[j]) / (current.hashes.size() + sentences.get(j).hashes.size() - hashOverlapsBySentenceOrdinal[j]);
                        double weight = ((double) 2.0d * hashOverlapsBySentenceOrdinal[j]) / (current.hashes.size() + sentences.get(j).hashes.size());
                        if (weight > 1)
                        {
                            System.out.println("Weight " + weight);
                        }
                        current.links.add(new SentenceAndWeight(sentences.get(j), weight));
                    }

                }
            }

            if (i == Integer.MAX_VALUE)
            {
                break; // or (i+1) would overflow
            }
        }

        // Page rank

        double maxDelta = 0;
        for (int i = 0; i < sentences.size(); i++)
        {
            if (sentenceOrdinalsWithAnyOverlap.get(i))
            {
                sentences.get(i).rank = 1 / sentences.size();
            }
            else
            {
                sentences.get(i).rank = 0;
            }
        }
        do
        {
            maxDelta = 0;
            for (int i = sentenceOrdinalsWithAnyOverlap.nextSetBit(0); i >= 0; i = sentenceOrdinalsWithAnyOverlap.nextSetBit(i + 1))
            {
                Sentence current = sentences.get(i);
                double incomingSum = 0;
                for (SentenceAndWeight in : current.links)
                {
                    double outgoingSum = 0;
                    for (SentenceAndWeight out : in.sentence.links)
                    {
                        outgoingSum += out.weight;
                    }
                    incomingSum += in.weight * in.sentence.rank / outgoingSum;
                }
                double newRank = (0.15 / sentences.size()) + (0.85 * incomingSum);
                double delta = Math.abs((current.rank - newRank) / current.rank);
                current.rank = newRank;
                if (delta > maxDelta)
                {
                    maxDelta = delta;
                }

                if (i == Integer.MAX_VALUE)
                {
                    break; // or (i+1) would overflow
                }
            }
            System.out.println("Delta = " + maxDelta);
        }
        while (maxDelta > 0.001);

        Collections.sort(sentences, new Comparator<Sentence>()
        {

            @Override
            public int compare(Sentence left, Sentence right)
            {
                return -Double.compare(left.rank, right.rank);
            }
        });

    }

    /**
     * @param word
     * @return
     */
    private boolean isNotAllWhiteSpace(String word)
    {
        for (char c : word.toCharArray())
        {
            if (!Character.isWhitespace(c))
            {
                return true;
            }
        }
        return false;
    }

    /**
     * @param words
     * @return
     */
    private ArrayList<String> getShingles(ArrayList<String> words, int count)
    {
        ArrayList<String> shingles = new ArrayList<String>();
        for (int i = 0; i <= words.size() - count; i++)
        {
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < count; j++)
            {
                if (builder.length() > 0)
                {
                    builder.append(" ");
                }
                builder.append(words.get(i + j));
            }
            shingles.add(builder.toString());
        }
        return shingles;
    }

    private static class SentenceAndWeight
    {
        Sentence sentence;

        double weight;

        SentenceAndWeight(Sentence sentence, double weight)
        {
            this.sentence = sentence;
            this.weight = weight;
        }
    }

    private static class Sentence
    {
        String sentence;

        int sentenceOrdinal;

        HashSet<HashEntry> hashes = new HashSet<HashEntry>();

        double rank = 1;

        ArrayList<SentenceAndWeight> links = new ArrayList<SentenceAndWeight>();

        Sentence(String sentence, int sentenceOrdinal)
        {
            this.sentence = sentence;
            this.sentenceOrdinal = sentenceOrdinal;
        }

        /**
         * @param entry
         */
        public void addEntry(HashEntry entry)
        {
            hashes.add(entry);
        }

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + sentenceOrdinal;
            return result;
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            Sentence other = (Sentence) obj;
            if (sentenceOrdinal != other.sentenceOrdinal)
                return false;
            return true;
        }

    }

    private static class HashEntry implements Comparable<HashEntry>
    {
        LongPair hash;

        int hashOrdinal;

        HashSet<Sentence> sentences = new HashSet<Sentence>();

        @Override
        public int hashCode()
        {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((hash == null) ? 0 : hash.hashCode());
            return result;
        }

        /**
         * @param sentanceOrdinal
         */
        public void addSentence(Sentence sentence)
        {
            sentences.add(sentence);
        }

        @Override
        public boolean equals(Object obj)
        {
            if (this == obj)
                return true;
            if (obj == null)
                return false;
            if (getClass() != obj.getClass())
                return false;
            HashEntry other = (HashEntry) obj;
            if (hash == null)
            {
                if (other.hash != null)
                    return false;
            }
            else if (!hash.equals(other.hash))
                return false;
            return true;
        }

        public HashEntry(LongPair hash, int hashOrdinal)
        {
            this.hash = hash;
            this.hashOrdinal = hashOrdinal;
        }

        @Override
        public int compareTo(HashEntry other)
        {
            return this.hash.compareTo(other.hash);
        }
    }

    public static void main(String[] args) throws IOException
    {
        File file = new File(args[0]);
        if (!file.exists())
        {
            throw new IOException("File missing ..." + args[0]);
        }
        SummarizingTokenizer t = new SummarizingTokenizer();
        Reader reader = new BufferedReader(new FileReader(file));
        t.setReader(reader);

        String asString = IOUtils.toString(reader);
        System.out.println(asString);
        ArrayList<Sentence> sentences = new ArrayList<Sentence>();
        TreeSet<HashEntry> orderedByHash = new TreeSet<HashEntry>();
        t.rankSentences(asString, sentences, orderedByHash, 1);

        for (int i = 0; i < sentences.size(); i++)
        {
            Sentence current = sentences.get(i);
            System.out.println("" + current.rank + " @ " + current.sentenceOrdinal + "    ->     " + current.sentence);
        }
    }

}
