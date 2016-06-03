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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.text.BreakIterator;

import edu.emory.mathcs.nlp.decode.AbstractNLPDecoder;
import edu.emory.mathcs.nlp.decode.DecodeConfig;
import edu.emory.mathcs.nlp.decode.NLPDecoder;

/**
 * @author Andy
 *
 */
public class NLPFilter
{
    /**
     * 
     */
    private static final String S_12 = "Everyone please note my blog on Donews http://blog.donews.com/pangshengdong. What I say is not necessarily right, but I am confident that if you read it carefully it should give you a start.";
    /**
     * 
     */
    private static final String S_11 = "The JW considers itself THE kingdom of God on earth. ('Kindom Hall') So it is only to be expected that they do not see a reason to run to and report everything to the government.";
    /**
     * 
     */
    private static final String S_10 = "\"It's too much, there's only us two, how are we going to eat this?\" I asked young Zhao as I looked at him in surprise.";
    /**
     * 
     */
    private static final String S_9 = "Wang first asked: \"Are you sure you want the original inscription ground off?\" Without thinking twice about it, Huang said yes.";
    /**
     * 
     */
    private static final String S_8 = "The agency said it confirmed American Continental's preferred stock rating at C. American Continental's thrift unit, Los Angeles-based Lincoln Savings & Loan Association, is in receivership and the parent company has filed for protection from creditor lawsuits under Chapter 11 of the federal Bankruptcy Code.";
    /**
     * 
     */
    private static final String S_7 = "Bharat Ratna Avul Pakir Jainulabdeen Abdul Kalam is also called as Dr. A.P.J Abdul Kalam.";
    /**
     * 
     */
    private static final String S_6 = "After seeing the list of what would not be open and/or on duty... which I'm also quite sure is not complete... I 'll go out on a limb.... and predict... that this will not happen.";
    /**
     * 
     */
    private static final String S_5 = "No, to my mind, the Journal did not \"defend sleaze, fraud, waste, embezzlement, influence-peddling and abuse of the public trust...\" it defended appropriate constitutional safeguards and practical common sense.";
    /**
     * 
     */
    private static final String S_4 = "The luxury auto maker last year sold 1,214 cars in the U.S. Howard Mosher, president and chief executive officer, said he anticipates growth for the luxury auto maker in Britain and Europe, and in Far Eastern markets.";
    private static final String S_1 = "At some schools, even professionals boasting Ph.D. degrees are coming back to school for Master's degrees.";
    private static final String S_2 = "If Harvard doesn't come through, I 'll take the test to get into Yale. many parents set goals for their children, or maybe they don't set a goal.";
    private static final String S_3 = "He adds, in a far less amused tone, that the government has been talking about making Mt. Kuanyin a national park for a long time, and has banned construction or use of the mountain.";
    
  
    
    public static void main(String[] args)
    {
       
        NLPDecoder decoder = new NLPDecoder(new ByteArrayInputStream(getXMLConfig().getBytes()));
        String output = decoder.decode("Andy Hind is a Techincal Architext at Alfresco Software in Maidenhead, England. He has worked there for 11 years sinces Aug 2005. Alfresco is Great! London is the capital city of England where Mr Fish lives. My dog has no nose. how does he smell? Awful! N.B.C broadcast stuff in 1984", AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_1, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_2, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_3, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_4, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_5, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_6, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_7, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_8, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_9, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_10, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_11, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        output = decoder.decode(S_12, AbstractNLPDecoder.FORMAT_RAW);
        System.out.println(output);
        System.out.println();
        
        
        BreakIterator bi = BreakIterator.getSentenceInstance();
        bi.setText(S_1);
        printEachForward(bi, S_1);
        System.out.println("============================");
        bi.setText(S_2);
        printEachForward(bi, S_2);
        System.out.println("============================");
        bi.setText(S_3);
        printEachForward(bi, S_3);
        System.out.println("============================");
        bi.setText(S_4);
        printEachForward(bi, S_4);
        System.out.println("============================");
        bi.setText(S_5);
        printEachForward(bi, S_5);
        System.out.println("============================");
        bi.setText(S_6);
        printEachForward(bi, S_6);
        System.out.println("============================");
        bi.setText(S_7);
        printEachForward(bi, S_7);
        System.out.println("============================");
        bi.setText(S_8);
        printEachForward(bi, S_8);
        System.out.println("============================");
        bi.setText(S_9);
        printEachForward(bi, S_9);
        System.out.println("============================");
        bi.setText(S_10);
        printEachForward(bi, S_10);
        System.out.println("============================");
        bi.setText(S_11);
        printEachForward(bi, S_11);
        System.out.println("============================");
        bi.setText(S_12);
        printEachForward(bi, S_12);
        System.out.println("============================");
        
    }
    
    
    public static void printEachForward(BreakIterator boundary, String source) {
        int start = boundary.first();
        for (int end = boundary.next();
             end != BreakIterator.DONE;
             start = end, end = boundary.next()) {
             System.out.println(source.substring(start,end));
        }
    }

    
    private static String getXMLConfig()
    {
        return "<configuration>\n"
                + "    <lexica>\n"
                + "        <ambiguity_classes field=\"word_form_simplified_lowercase\">edu/emory/mathcs/nlp/lexica/en-ambiguity-classes-simplified-lowercase.xz</ambiguity_classes>\n"
                + "        <word_clusters field=\"word_form_simplified_lowercase\">edu/emory/mathcs/nlp/lexica/en-brown-clusters-simplified-lowercase.xz</word_clusters>\n"
                + "        <named_entity_gazetteers field=\"word_form_simplified\">edu/emory/mathcs/nlp/lexica/en-named-entity-gazetteers-simplified.xz</named_entity_gazetteers>\n"
                + "        <word_embeddings field=\"word_form_undigitalized\">edu/emory/mathcs/nlp/lexica/en-word-embeddings-undigitalized.xz</word_embeddings>\n"
                + "    </lexica>\n"
                + "    <models>\n"
                + "        <pos>edu/emory/mathcs/nlp/models/en-pos.xz</pos>\n"
                + "        <ner>edu/emory/mathcs/nlp/models/en-ner.xz</ner>\n"
                + "        <dep>edu/emory/mathcs/nlp/models/en-dep.xz</dep>\n"
                + "    </models>\n"
                + "</configuration>\n";
    }
}
