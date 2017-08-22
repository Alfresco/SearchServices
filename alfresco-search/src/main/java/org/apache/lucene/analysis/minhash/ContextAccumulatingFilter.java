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
import java.util.ArrayDeque;
import java.util.LinkedList;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

public class ContextAccumulatingFilter extends TokenFilter {

	private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
	private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);

	private ArrayDeque<String> tokens = new ArrayDeque<String>(9);

	private LinkedList<String> tokenStack = new LinkedList<String>();

	private boolean exhausted = false;

	private int windowSize = 9;

	protected ContextAccumulatingFilter(TokenStream input) {
		super(input);

	}

	@Override
	final public boolean incrementToken() throws IOException {
		// Pull the underlying stream of tokens
		// Hash each token found
		// Generate the required number of variants of this hash
		// Keep the minimum hash value found so far of each variant

		if (exhausted) {
			return false;
		}

		if (tokens.size() > 0) {
			String token = tokens.removeFirst();
			termAttribute.setEmpty().append(token);
			typeAttribute.setType(MinHashFilter.MIN_HASH_TYPE);
			return true;
		}

		boolean filled = false;
		boolean incremented = false;
		while ((!filled) && (incremented = input.incrementToken())) {
			String current = new String(termAttribute.buffer(), 0, termAttribute.length());
			if (tokenStack.size() >= windowSize) {
				tokenStack.removeFirst();
			}
			tokenStack.addLast(current);
			if (tokenStack.size() >= windowSize) {
				filled = true;
			}
		}

		if (!incremented) {
			exhausted = true;
		}

		if (filled) {
			StringBuilder wordBuilder = new StringBuilder("");
			ArrayDeque<String> before = new ArrayDeque<String>();
			ArrayDeque<String> after = new ArrayDeque<String>();
			for (int i = 0, l = tokenStack.size(); i < l; i++) {
				if ((i == Math.round(Math.floor((l - 1) / 2.0))) || (i == Math.round(Math.ceil((l - 1) / 2.0)))) {
					if (wordBuilder.length() > 0) {
						wordBuilder.append(" ");
					}
					wordBuilder.append(tokenStack.get(i));
				} else if (i < Math.round(Math.floor((l - 1) / 2.0))) {
					before.addFirst(tokenStack.get(i));
				} else if (i > Math.round(Math.ceil((l - 1) / 2.0))) {
					after.addLast(tokenStack.get(i));
				}
			}

			tokens.clear();
			int i = 1;
			while (before.peek() != null) {
				// tokens.add(wordBuilder.toString() + ":-" + i++ + ":" +
				// before.removeFirst());
				String beforeString = before.removeFirst();
				tokens.add(wordBuilder.toString() + ":" + beforeString);
				// tokens.add(wordBuilder.toString() + ":" + i + "-" +
				// beforeString);
				// tokens.add( i + "-" + beforeString + ":" +
				// wordBuilder.toString());
				// i++;
			}
			i = 1;
			while (after.peek() != null) {
				// tokens.add(wordBuilder.toString() + ":+" + i++ + ":" +
				// after.removeFirst());
				String afterString = after.removeFirst();
				tokens.add(wordBuilder.toString() + ":" + afterString);
				// tokens.add(wordBuilder.toString() + ":" + i + "+" +
				// afterString);
				// tokens.add(i + "+" + afterString + ":" +
				// wordBuilder.toString());
				// i++;
			}
			tokens.add("__tf__:" + wordBuilder.toString());

			if (tokens.size() > 0) {
				String token = tokens.removeFirst();
				termAttribute.setEmpty().append(token);
				typeAttribute.setType(MinHashFilter.MIN_HASH_TYPE);
			}
		}

		return filled;
	}

	@Override
	public void end() throws IOException {
		if (!exhausted) {
			input.end();
		}

	}

	@Override
	public void reset() throws IOException {
		super.reset();
		exhausted = false;
		tokens.clear();
	}
}
