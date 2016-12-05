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
import java.io.StringReader;
import java.util.HashMap;

import org.apache.lucene.analysis.BaseTokenStreamTestCase;
import org.apache.lucene.analysis.MockTokenizer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.util.automaton.CharacterRunAutomaton;
import org.apache.lucene.util.automaton.RegExp;
import org.junit.Ignore;
import org.junit.Test;

public class ContextAccumulatingFilterTest extends BaseTokenStreamTestCase {

	@Ignore
	@Test
	public void testTokenStreamSingleInput() throws IOException {
		String[] hashes = new String[] {"0:woof", "-1:woof", "-2:woof", "+1:woof", "+2:woof"};
		TokenStream ts = createTokenStream(5, "woof woof woof woof woof");
		assertTokenStreamContents(ts, hashes, new int[] { 0 }, new int[] { 24 },
				new String[] { MinHashFilter.MIN_HASH_TYPE }, new int[] { 1 }, new int[] { 1 }, 24, 0, null, true);

		
	}

	public static TokenStream createTokenStream(int shingleSize, String shingles) {
		Tokenizer tokenizer = createMockShingleTokenizer(shingleSize, shingles);
		HashMap<String, String> lshffargs = new HashMap<String, String>();
		ContextAccumulatingFilterFactory lshff = new ContextAccumulatingFilterFactory(lshffargs);
		return lshff.create(tokenizer);
	}

	public static Tokenizer createMockShingleTokenizer(int shingleSize, String shingles) {
		MockTokenizer tokenizer = new MockTokenizer(
				new CharacterRunAutomaton(new RegExp("[^ \t\r\n]+([ \t\r\n]+[^ \t\r\n]+){4}").toAutomaton()), true);
		tokenizer.setEnableChecks(true);
		if (shingles != null) {
			tokenizer.setReader(new StringReader(shingles));
		}
		return tokenizer;
	}

}
