package org.alfresco.solr.nlp;

import java.util.Map;

import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

public class NLPTokenizerFactory extends TokenizerFactory {
	  
	  /** Creates a new KeywordTokenizerFactory */
	  public NLPTokenizerFactory(Map<String,String> args) {
	    super(args);
	    if (!args.isEmpty()) {
	      throw new IllegalArgumentException("Unknown parameters: " + args);
	    }
	  }
	  
	  @Override
	  public NLPTokenizer create(AttributeFactory factory) {
	    return new NLPTokenizer();
	  }
	}
