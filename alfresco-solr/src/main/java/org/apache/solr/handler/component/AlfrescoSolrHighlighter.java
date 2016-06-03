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
package org.apache.solr.handler.component;

import static org.alfresco.repo.search.adaptor.lucene.QueryConstants.FIELD_SOLR4_ID;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldUse;
import org.alfresco.solr.AlfrescoSolrDataModel.TenantAclIdDbId;
import org.alfresco.solr.content.SolrContentStore;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.FilterLeafReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Encoder;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.QueryTermScorer;
import org.apache.lucene.search.highlight.Scorer;
import org.apache.lucene.search.vectorhighlight.FastVectorHighlighter;
import org.apache.lucene.search.vectorhighlight.FieldQuery;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.highlight.DefaultSolrHighlighter;
import org.apache.solr.highlight.SolrHighlighter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.update.DocumentBuilder;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andy
 */
public class AlfrescoSolrHighlighter extends DefaultSolrHighlighter implements
		PluginInfoInitialized {

	private static final Logger log = LoggerFactory.getLogger(MethodHandles
			.lookup().lookupClass());

	public AlfrescoSolrHighlighter(SolrCore solrCore) {
		super(solrCore);
	}


	/**
	 * Return a {@link org.apache.lucene.search.highlight.Highlighter}
	 * appropriate for this field.
	 * 
	 * @param query
	 *            The current Query
	 * @param fieldName
	 *            The name of the field
	 * @param request
	 *            The current SolrQueryRequest
	 */
	@Override
	protected Highlighter getHighlighter(Query query, String requestFieldname,
			SolrQueryRequest request) {
		String schemaFieldName = AlfrescoSolrDataModel.getInstance()
				.mapProperty(requestFieldname, FieldUse.HIGHLIGHT, request); 
		SolrParams params = request.getParams();
		Highlighter highlighter = new Highlighter(getFormatter(
				requestFieldname, params),
				getEncoder(requestFieldname, params), getQueryScorer(query,
						schemaFieldName, request));
		highlighter.setTextFragmenter(getFragmenter(requestFieldname, params));
		return highlighter;
	}

	/**
	 * Return a {@link org.apache.lucene.search.highlight.QueryScorer} suitable
	 * for this Query and field.
	 * 
	 * @param query
	 *            The current query
	 * @param tokenStream
	 *            document text CachingTokenStream
	 * @param fieldName
	 *            The name of the field
	 * @param request
	 *            The SolrQueryRequest
	 */
	@Override
	protected QueryScorer getSpanQueryScorer(Query query,
			String requestFieldname,
			TokenStream tokenStream, SolrQueryRequest request) {
		String schemaFieldName = AlfrescoSolrDataModel.getInstance()
				.mapProperty(requestFieldname, FieldUse.HIGHLIGHT, request); 
		QueryScorer scorer = new QueryScorer(query,
				request.getParams().getFieldBool(requestFieldname,
						HighlightParams.FIELD_MATCH, false) ? schemaFieldName : null);
		scorer.setExpandMultiTermQuery(request.getParams().getBool(
				HighlightParams.HIGHLIGHT_MULTI_TERM, true));

		boolean defaultPayloads = true;// overwritten below
		try {
			// It'd be nice to know if payloads are on the tokenStream but the
			// presence of the attribute isn't a good
			// indicator.
			final Terms terms = request.getSearcher().getLeafReader().fields()
					.terms(schemaFieldName);
			if (terms != null) {
				defaultPayloads = terms.hasPayloads();
			}
		} catch (IOException e) {
			log.error("Couldn't check for existence of payloads", e);
		}
		scorer.setUsePayloads(request.getParams().getFieldBool(requestFieldname,
				HighlightParams.PAYLOADS, defaultPayloads));
		return scorer;
	}

	/**
	 * Return a {@link org.apache.lucene.search.highlight.Scorer} suitable for
	 * this Query and field.
	 * 
	 * @param query
	 *            The current query
	 * @param fieldName
	 *            The name of the field
	 * @param request
	 *            The SolrQueryRequest
	 */
	protected Scorer getQueryScorer(Query query, String requestFieldname, SolrQueryRequest request) {
		String schemaFieldName = AlfrescoSolrDataModel.getInstance()
				.mapProperty(requestFieldname, FieldUse.HIGHLIGHT, request);
		boolean reqFieldMatch = request.getParams().getFieldBool(
				requestFieldname, HighlightParams.FIELD_MATCH, false);
		if (reqFieldMatch) {
			return new QueryTermScorer(query, request.getSearcher()
					.getIndexReader(), schemaFieldName);
		} else {
			return new QueryTermScorer(query);
		}
	}

	

	/**
	 * Generates a list of Highlighted query fragments for each item in a list
	 * of documents, or returns null if highlighting is disabled.
	 *
	 * @param docs
	 *            query results
	 * @param query
	 *            the query
	 * @param req
	 *            the current request
	 * @param defaultFields
	 *            default list of fields to summarize
	 *
	 * @return NamedList containing a NamedList for each document, which in
	 *         turns contains sets (field, summary) pairs.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public NamedList<Object> doHighlighting(DocList docs, Query query,
			SolrQueryRequest req, String[] defaultFields) throws IOException {
		SolrParams params = req.getParams();
		if (!isHighlightingEnabled(params)) // also returns early if no unique
											// key field
			return null;

		SolrIndexSearcher searcher = req.getSearcher();
		IndexSchema schema = searcher.getSchema();

		// fetch unique key if one exists.
		SchemaField keyField = schema.getUniqueKeyField();
		if (keyField == null) {
			return null;// exit early; we need a unique key field to populate
						// the response
		}

		String[] fieldNames = getHighlightFields(query, req, defaultFields);

		Set<String> preFetchFieldNames = getDocPrefetchFieldNames(fieldNames,
				req);
		if (preFetchFieldNames != null) {
			preFetchFieldNames.add(keyField.getName());
		}

		FastVectorHighlighter fvh = null; // lazy
		FieldQuery fvhFieldQuery = null; // lazy

		IndexReader reader = new TermVectorReusingLeafReader(req.getSearcher()
				.getLeafReader()); // SOLR-5855

		// Highlight each document
		NamedList fragments = new SimpleOrderedMap();
		DocIterator iterator = docs.iterator();
		for (int i = 0; i < docs.size(); i++) {
			int docId = iterator.nextDoc();
			Document doc = getDocument(searcher.doc(docId, preFetchFieldNames), req);

			@SuppressWarnings("rawtypes")
			NamedList docHighlights = new SimpleOrderedMap();
			// Highlight per-field
			for (String fieldName : fieldNames) {
				String schemaFieldName = AlfrescoSolrDataModel.getInstance().mapProperty(fieldName, FieldUse.HIGHLIGHT, req);
				SchemaField schemaField = schema.getFieldOrNull(schemaFieldName);

				Object fieldHighlights; // object type allows flexibility for
										// subclassers
				if (schemaField == null) {
					fieldHighlights = null;
				} else if (schemaField.getType() instanceof org.apache.solr.schema.TrieField) {
					// TODO: highlighting numeric fields is broken (Lucene) - so
					// we disable them until fixed (see LUCENE-3080)!
					fieldHighlights = null;
				} else if (useFastVectorHighlighter(params, schemaField)) {
					if (fvhFieldQuery == null) {
						fvh = new FastVectorHighlighter(
						// FVH cannot process hl.usePhraseHighlighter parameter
						// per-field basis
								params.getBool(
										HighlightParams.USE_PHRASE_HIGHLIGHTER,
										true),
								// FVH cannot process hl.requireFieldMatch
								// parameter per-field basis
								params.getBool(HighlightParams.FIELD_MATCH,
										false));
						fvh.setPhraseLimit(params.getInt(
								HighlightParams.PHRASE_LIMIT,
								SolrHighlighter.DEFAULT_PHRASE_LIMIT));
						fvhFieldQuery = fvh.getFieldQuery(query, reader);
					}
					fieldHighlights = doHighlightingByFastVectorHighlighter(
							doc, docId, schemaField, fvh, fvhFieldQuery,
							reader, req);
				} else { // standard/default highlighter
					fieldHighlights = doHighlightingByHighlighter(doc, docId,
							schemaField, query, reader, req);
				}

				if (fieldHighlights == null) {
					// no summaries made; copy text from alternate field
					fieldHighlights = alternateField(doc, fieldName, req);
				}

				if (fieldHighlights != null) {
					docHighlights.add(fieldName, fieldHighlights);
				}
			} // for each field
			fragments.add(schema.printableUniqueKey(doc), docHighlights);
		} // for each doc
		return fragments;
	}

	

	private String fixLocalisedText(String text) {
		if ((text == null) || (text.length() == 0)) {
			return text;
		}

		if (text.charAt(0) == '\u0000') {
			int index = text.indexOf('\u0000', 1);
			if (index == -1) {
				return text;
			} else {
				if (index + 1 < text.length()) {
					return text.substring(index + 1);
				} else {
					return text;
				}
			}
		} else {
			return text;
		}

	}

	/**
	 * Returns the alternate highlight object for this field -- a String[] by
	 * default. Null if none.
	 */
	@SuppressWarnings("unchecked")
	protected Object alternateField(Document doc, String fieldName,
			SolrQueryRequest req) {
		SolrParams params = req.getParams();
		String alternateField = params.getFieldParam(fieldName,
				HighlightParams.ALTERNATE_FIELD);
		
		if (alternateField == null || alternateField.length() == 0) {
			return null;
		}
		alternateField = AlfrescoSolrDataModel
					.getInstance().mapProperty(fieldName,
							FieldUse.HIGHLIGHT, req);
		IndexableField[] docFields = doc.getFields(alternateField);
		if (docFields.length == 0) {
			// The alternate field did not exist, treat the original field as
			// fallback instead
			docFields = doc.getFields(fieldName);
		}
		List<String> listFields = new ArrayList<>();
		for (IndexableField field : docFields) {
			if (field.binaryValue() == null)
				listFields.add(field.stringValue());
		}

		if (listFields.isEmpty()) {
			return null;
		}
		String[] altTexts = listFields.toArray(new String[listFields.size()]);

		Encoder encoder = getEncoder(fieldName, params);
		int alternateFieldLen = params.getFieldInt(fieldName,
				HighlightParams.ALTERNATE_FIELD_LENGTH, 0);
		List<String> altList = new ArrayList<>();
		int len = 0;
		for (String altText : altTexts) {
			if (alternateFieldLen <= 0) {
				altList.add(encoder.encodeText(altText));
			} else {
				// note: seemingly redundant new String(...) releases memory to
				// the larger text. But is copying better?
				altList.add(len + altText.length() > alternateFieldLen ? encoder
						.encodeText(new String(altText.substring(0,
								alternateFieldLen - len))) : encoder
						.encodeText(altText));
				len += altText.length();
				if (len >= alternateFieldLen)
					break;
			}
		}
		return altList;
	}

	


	private Document getDocument(Document doc, SolrQueryRequest req)
			throws IOException {
		try {
			String id = getFieldValueString(doc, FIELD_SOLR4_ID);
			TenantAclIdDbId tenantAndDbId = AlfrescoSolrDataModel
					.decodeNodeDocumentId(id);
			SolrInputDocument sid = SolrContentStore.retrieveDocFromSolrContentStore(
					tenantAndDbId.tenant, tenantAndDbId.dbId);
			if (sid == null) {
				sid = new SolrInputDocument();
				sid.addField(FIELD_SOLR4_ID, id);
				sid.addField("_version_", 0);
				return DocumentBuilder.toDocument(sid, req.getSchema());
			} else {
				return DocumentBuilder.toDocument(sid, req.getSchema());
			}
		} catch (StringIndexOutOfBoundsException e) {
			throw new IOException(e);
		}
	}

	private String getFieldValueString(Document doc, String fieldName) {
		IndexableField field = (IndexableField) doc.getField(fieldName);
		String value = null;
		if (field != null) {
			value = field.stringValue();
		}
		return value;
	}
	
	
	/** Wraps a DirectoryReader that caches the {@link LeafReader#getTermVectors(int)} so that
	 * if the next call has the same ID, then it is reused.
	 */
	class TermVectorReusingLeafReader extends FilterLeafReader {

	  private int lastDocId = -1;
	  private Fields tvFields;

	  public TermVectorReusingLeafReader(LeafReader in) {
	    super(in);
	  }

	  @Override
	  public Fields getTermVectors(int docID) throws IOException {
	    if (docID != lastDocId) {
	      lastDocId = docID;
	      tvFields = in.getTermVectors(docID);
	    }
	    return tvFields;
	  }
	}
}

