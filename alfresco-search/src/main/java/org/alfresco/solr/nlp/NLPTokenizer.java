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
import java.io.IOException;
import java.util.ArrayDeque;

import org.apache.commons.io.IOUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import edu.emory.mathcs.nlp.component.template.node.NLPNode;
import edu.emory.mathcs.nlp.decode.NLPDecoder;

/**
 * @author Andy
 *
 */
public class NLPTokenizer extends Tokenizer {
	private static NLPDecoder decoder = new NLPDecoder(
			new ByteArrayInputStream(getXMLConfigNER().getBytes()));

	public static final String NLP_TYPE = "NLP";

	private boolean initialized = false;

	private ArrayDeque<Entity> entities = null;

	private final CharTermAttribute termAttribute = addAttribute(CharTermAttribute.class);
	private final OffsetAttribute offsetAttribute = addAttribute(OffsetAttribute.class);
	private final TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
	private final PositionIncrementAttribute posIncAttribute = addAttribute(PositionIncrementAttribute.class);
	private final PositionLengthAttribute posLenAttribute = addAttribute(PositionLengthAttribute.class);

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.apache.lucene.analysis.TokenStream#incrementToken()
	 */
	@Override
	public boolean incrementToken() throws IOException {
		if (!initialized) {
			entities = new ArrayDeque<>();

			String asString = IOUtils.toString(input);
			if((asString == null) || (asString.length() == 0))
			{
				return false;
			}

			NLPNode[] output = decoder.decode(asString);
			StringBuilder builder = null;
			Entity entity = null;
			for (NLPNode node : output) {
				String tag = node.getNamedEntityTag();
				if (!tag.equals("0")) {
					if (tag.startsWith("U-")) {
						builder = new StringBuilder();
						entity = new Entity();
						builder.append(tag.substring(2)).append(":");
						builder.append(node.getWordForm());
						entity.start = node.getStartOffset();
						entity.end = node.getEndOffset();
						entity.entity = builder.toString();
						entities.add(entity);
						builder = null;
						entity = null;
						node.getWordEmbedding();
					} else if (tag.startsWith("B-")) {
						builder = new StringBuilder();
						entity = new Entity();
						builder.append(tag.substring(2)).append(":");
						builder.append(node.getWordForm());
						entity.start = node.getStartOffset();
					} else if (tag.startsWith("I-")) {
						builder.append(" ").append(node.getWordForm());
					} else if (tag.startsWith("L-")) {
						builder.append(" ").append(node.getWordForm());
						entity.end = node.getEndOffset();
						entity.entity = builder.toString();
						entities.add(entity);
						builder = null;
						entity = null;
					}
				}
			}
			initialized = true;
		}

		Entity entity;
		if (entities != null) {
			entity = entities.poll();
			if (entity != null) {
				termAttribute.setEmpty().append(entity.entity);
				posIncAttribute.setPositionIncrement(1);
				offsetAttribute.setOffset(entity.start, entity.end);
				typeAttribute.setType(NLP_TYPE);
				posLenAttribute.setPositionLength(1);
				return true;
			} else {
				return false;
			}
		} else {
			return false;
		}

	}

	@Override
	public void reset() throws IOException {
		super.reset();
		initialized = false;
		entities = null;
	}

	private static String getXMLConfigNER() {
		return "<configuration>\n"
				+ "    <lexica>\n"
				+ "        <ambiguity_classes field=\"word_form_simplified_lowercase\">edu/emory/mathcs/nlp/lexica/en-ambiguity-classes-simplified-lowercase.xz</ambiguity_classes>\n"
				+ "        <word_clusters field=\"word_form_simplified_lowercase\">edu/emory/mathcs/nlp/lexica/en-brown-clusters-simplified-lowercase.xz</word_clusters>\n"
				+ "        <named_entity_gazetteers field=\"word_form_simplified\">edu/emory/mathcs/nlp/lexica/en-named-entity-gazetteers-simplified.xz</named_entity_gazetteers>\n"
				+ "        <word_embeddings field=\"word_form_undigitalized\">edu/emory/mathcs/nlp/lexica/en-word-embeddings-undigitalized.xz</word_embeddings>\n"
				+ "    </lexica>\n" + "    <models>\n"
				+ "        <ner>edu/emory/mathcs/nlp/models/en-ner.xz</ner>\n"
				+ "    </models>\n" + "</configuration>\n";
	}
	
	
	private static String getXMLConfigFull() {
		return "<configuration>\n"
				+ "    <lexica>\n"
				+ "        <ambiguity_classes field=\"word_form_simplified_lowercase\">edu/emory/mathcs/nlp/lexica/en-ambiguity-classes-simplified-lowercase.xz</ambiguity_classes>\n"
				+ "        <word_clusters field=\"word_form_simplified_lowercase\">edu/emory/mathcs/nlp/lexica/en-brown-clusters-simplified-lowercase.xz</word_clusters>\n"
				+ "        <named_entity_gazetteers field=\"word_form_simplified\">edu/emory/mathcs/nlp/lexica/en-named-entity-gazetteers-simplified.xz</named_entity_gazetteers>\n"
				+ "        <word_embeddings field=\"word_form_undigitalized\">edu/emory/mathcs/nlp/lexica/en-word-embeddings-undigitalized.xz</word_embeddings>\n"
				+ "    </lexica>\n" + "    <models>\n"
				+ "        <pos>edu/emory/mathcs/nlp/models/en-pos.xz</pos>\n"
				+ "        <ner>edu/emory/mathcs/nlp/models/en-ner.xz</ner>\n"
				+ "        <dep>edu/emory/mathcs/nlp/models/en-dep.xz</dep>\n"
				+ "    </models>\n" + "</configuration>\n";
	}

	public static class Entity {
		public String entity;
		public int start;
		public int end;

	}
}
