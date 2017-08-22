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
package org.apache.lucene.analysis.minhash;

import java.util.HashMap;
import java.util.Random;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Removes stop words from a token stream.
 * <p>
 * This class moved to Lucene Core, but a reference in the
 * {@code analysis/common} module is preserved for documentation purposes and
 * consistency with filter factory.
 * 
 * @see org.apache.lucene.analysis.StopFilter
 * @see StopFilterFactory
 */
public final class StopFilter extends org.apache.lucene.analysis.StopFilter {

	public static HashMap<String, Integer> topWordFreq = new HashMap<String, Integer>();

	static {
		topWordFreq.put("the", 61847);
		topWordFreq.put("of", 9391);
		topWordFreq.put("and", 26817);
		topWordFreq.put("a", 21626);
		topWordFreq.put("in", 18214);
		topWordFreq.put("to", 16284);
		topWordFreq.put("it", 10875);
		topWordFreq.put("is", 9982);
		topWordFreq.put("to", 9343);
		topWordFreq.put("was", 9236);
		topWordFreq.put("I", 8875);
		topWordFreq.put("for", 8412);
		topWordFreq.put("that", 7308);
		topWordFreq.put("you", 6954);
		topWordFreq.put("he", 6810);
		topWordFreq.put("be", 6644);
		topWordFreq.put("with", 6575);
		topWordFreq.put("on", 6475);
		topWordFreq.put("by", 5096);
		topWordFreq.put("at", 4790);
		topWordFreq.put("have*", 4735);
		topWordFreq.put("are", 4707);
		topWordFreq.put("not", 4626);
		topWordFreq.put("this", 4623);
		// topWordFreq.put("'s Gen 4599);
		topWordFreq.put("but", 4577);
		topWordFreq.put("had", 4452);
		topWordFreq.put("they", 4332);
		topWordFreq.put("his", 4285);
		topWordFreq.put("from", 4134);
		topWordFreq.put("she", 3801);
		topWordFreq.put("that", 3792);
		topWordFreq.put("which", 3719);
		topWordFreq.put("or", 3707);
		topWordFreq.put("we", 3578);
		// 's", 3490);
		topWordFreq.put("an", 3430);
		// ~n't", 3328);
		topWordFreq.put("were", 3227);
		topWordFreq.put("as", 3006);
		topWordFreq.put("do", 2802);
		topWordFreq.put("been", 2686);
		topWordFreq.put("their", 2608);
		topWordFreq.put("has", 2593);
		topWordFreq.put("would", 2551);
		topWordFreq.put("there", 2532);
		topWordFreq.put("what", 2493);
		topWordFreq.put("will", 2470);
		topWordFreq.put("all", 2436);
		topWordFreq.put("if", 2369);
		topWordFreq.put("can", 2354);
		topWordFreq.put("her", 2183);
		topWordFreq.put("said", 2087);
		topWordFreq.put("who", 2055);
		topWordFreq.put("one", 1962);
		topWordFreq.put("so", 1893);
		topWordFreq.put("up", 1795);
		topWordFreq.put("as", 1774);
		topWordFreq.put("them", 1733);
		topWordFreq.put("some", 1712);
		topWordFreq.put("when", 1712);
		topWordFreq.put("could", 1683);
		topWordFreq.put("him", 1649);
		topWordFreq.put("into", 1634);
		topWordFreq.put("its", 1632);
		topWordFreq.put("then", 1595);
		topWordFreq.put("two", 1561);
		topWordFreq.put("out", 1542);
		topWordFreq.put("time", 1542);
		topWordFreq.put("my", 1525);
		topWordFreq.put("about", 1524);
		topWordFreq.put("did", 1434);
		topWordFreq.put("your", 1383);
		topWordFreq.put("now", 1382);
		topWordFreq.put("me", 1364);
		topWordFreq.put("no", 1343);
		topWordFreq.put("other", 1336);
		topWordFreq.put("only", 1298);
		topWordFreq.put("just", 1277);
		topWordFreq.put("more", 1275);
		topWordFreq.put("these", 1254);
		topWordFreq.put("also", 1248);
		topWordFreq.put("people", 1241);
		topWordFreq.put("know", 1233);
		topWordFreq.put("any", 1220);
		topWordFreq.put("first", 1193);
		topWordFreq.put("see", 1186);
		topWordFreq.put("very", 1165);
		topWordFreq.put("new", 1145);
		topWordFreq.put("may", 1135);
		topWordFreq.put("well", 1119);
		topWordFreq.put("should", 1112);
		// topWordFreq.put("her", 1085);
		topWordFreq.put("like", 1064);
		topWordFreq.put("than", 1033);
		topWordFreq.put("how", 1016);
		topWordFreq.put("get", 995);
		topWordFreq.put("way", 958);
		// topWordFreq.put("one", 953);
		topWordFreq.put("our", 950);
		topWordFreq.put("made", 943);
		topWordFreq.put("got", 932);
		topWordFreq.put("after", 927);
		topWordFreq.put("think", 916);
		topWordFreq.put("between", 903);
		topWordFreq.put("many", 902);
		topWordFreq.put("years", 902);
		topWordFreq.put("er", 896);
		// 've", 891 );
		topWordFreq.put("those", 888);
		topWordFreq.put("go", 881);
		topWordFreq.put("being", 862);
		topWordFreq.put("because*", 852);
		topWordFreq.put("down", 845);
		// 're", 835);
		// yeah Int 834);
		topWordFreq.put("three", 797);
		topWordFreq.put("good", 795);
		topWordFreq.put("back", 793);
		topWordFreq.put("make", 791);
		topWordFreq.put("such", 763);
		topWordFreq.put("on", 756);
		topWordFreq.put("there", 746);
		topWordFreq.put("through", 743);
		topWordFreq.put("year", 737);
		topWordFreq.put("over", 735);
		// 'll", 726);
		topWordFreq.put("must", 723);
		topWordFreq.put("still", 718);
		topWordFreq.put("even", 716);
		topWordFreq.put("take", 715);
		topWordFreq.put("too", 701);
	}

	private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);

	private Random random = new Random();

	/**
	 * Constructs a filter which removes words from the input TokenStream that
	 * are named in the Set.
	 * 
	 * @param in
	 *            Input stream
	 * @param stopWords
	 *            A {@link org.apache.lucene.analysis.util.CharArraySet}
	 *            representing the stopwords.
	 * @see #makeStopSet(java.lang.String...)
	 */
	public StopFilter(TokenStream in, org.apache.lucene.analysis.util.CharArraySet stopWords) {
		super(in, stopWords);
	}

	/**
	 * Constructs a filter which removes words from the input TokenStream that
	 * are named in the Set.
	 * 
	 * @param in
	 *            Input stream
	 * @param stopWords
	 *            A {@link org.apache.lucene.analysis.CharArraySet} representing
	 *            the stopwords.
	 * @see #makeStopSet(java.lang.String...)
	 */
	public StopFilter(TokenStream in, org.apache.lucene.analysis.CharArraySet stopWords) {
		super(in, stopWords);
	}

	@Override
	protected boolean accept() {
		return super.accept() || allowSome();
	}

	private boolean allowSome() {
		String word = termAtt.toString();
		Integer freq = topWordFreq.get(word);
		if (freq == null) {
			return true;
		}
		if(random.nextDouble() < 200d/freq)
		{
			return true;
		}
		return false;
	}

}
