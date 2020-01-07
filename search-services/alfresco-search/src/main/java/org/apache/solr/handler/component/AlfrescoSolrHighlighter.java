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

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldUse;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.highlight.DefaultSolrHighlighter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocSlice;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.StreamSupport;

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static org.alfresco.solr.utils.Utils.isNullOrEmpty;
import static org.alfresco.solr.utils.Utils.startsWithLanguageMarker;

public class AlfrescoSolrHighlighter extends DefaultSolrHighlighter implements PluginInfoInitialized
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoSolrHighlighter.class);

	private static class IdTriple
	{
		final int docid;
		final String solrId;
		final String dbid;

		private IdTriple(int docid, String solrId, String dbid)
		{
			this.docid = docid;
			this.solrId = solrId;
			this.dbid = dbid;
		}

		int docid()
		{
			return docid;
		}

		String dbid()
		{
			return dbid;
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof IdTriple
					&& ((IdTriple)obj).solrId.equals(solrId);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(solrId);
		}

		@Override
		public String toString()
		{
			return "IdTriple {" + "docid=" + docid + ", solrId='" + solrId + '\'' + ", dbdid='" + dbid + '\'' + '}';
		}
	}

	public AlfrescoSolrHighlighter(SolrCore core)
	{
		super(core);
	}

	// TODO: E' possibile fare a meno di questo?
	@Override
	protected Highlighter getHighlighter(Query query, String requestFieldname, SolrQueryRequest request)
	{
		String schemaFieldName =
				AlfrescoSolrDataModel.getInstance()
					.mapProperty(requestFieldname, FieldUse.HIGHLIGHT, request);

		Highlighter highlighter =
				new Highlighter(
						getFormatter(requestFieldname, request.getParams()),
						getEncoder(requestFieldname, request.getParams()),
						getQueryScorer(query,schemaFieldName, request));

		highlighter.setTextFragmenter(getFragmenter(requestFieldname, request.getParams()));
		return highlighter;
	}

	@Override
	protected QueryScorer getSpanQueryScorer(Query query, String requestFieldname, TokenStream tokenStream, SolrQueryRequest request)
	{
		String schemaFieldName = AlfrescoSolrDataModel.getInstance().mapProperty(requestFieldname, FieldUse.HIGHLIGHT, request);
		QueryScorer scorer = new QueryScorer(query,request.getParams().getFieldBool(requestFieldname, HighlightParams.FIELD_MATCH, false) ? schemaFieldName : null);
		scorer.setExpandMultiTermQuery(request.getParams().getBool(HighlightParams.HIGHLIGHT_MULTI_TERM, true));

		boolean defaultPayloads = true;// overwritten below
		try
		{
			// It'd be nice to know if payloads are on the tokenStream but the
			// presence of the attribute isn't a good
			// indicator.
			final Terms terms = request.getSearcher().getSlowAtomicReader().fields().terms(schemaFieldName);
			if (terms != null)
			{
				defaultPayloads = terms.hasPayloads();
			}
		}
		catch (IOException e)
		{
			LOGGER.error("Couldn't check for existence of payloads", e);
		}
		scorer.setUsePayloads(request.getParams().getFieldBool(requestFieldname, HighlightParams.PAYLOADS, defaultPayloads));
		return scorer;
	}

	@SuppressWarnings("unchecked")
	@Override
	public NamedList<Object> doHighlighting(DocList docs, Query query, SolrQueryRequest request, String[] defaultFields) throws IOException
	{
		final String idFieldName = request.getSchema().getUniqueKeyField().getName();
		final Set<String> idFields = Set.of(idFieldName, "DBID");
		final SolrParams originalRequestParameters = request.getParams();

		// raw fields in the hl.fl parameter (e.g. hl.fl=content, name, title)
		List<String> highlightFields = stream(super.getHighlightFields(query, request, defaultFields)).collect(toList());

		/*
			The Alfresco Data Model is queried in order to retrieve the top-level choice mapping for the fields collected above.
			Top-level choice because for each incoming field name (e.g. content) the Alfresco Data Model could provide more
			than one alternative. The first one which is tried is the cross language field.

			e.g.
		 	{
		 		name 	=>	text@s___t@{http://www.alfresco.org/model/content/1.0}name,
		 		title	=>	mltext@m___t@{http://www.alfresco.org/model/content/1.0}title,
		 		content	=>	content@s___t@{http://www.alfresco.org/model/content/1.0}content
		 	}

		 	Since at the end we need to restore (in the response) the original request(ed) fields names (e.g. content, name) used by requestor
		 	we collect a map which associates each schema field (e.g. text@s___t@{http://www.alfresco.org/model/content/1.0}name)
		 	with the corresponding request(ed) field (e.g. name).
		*/
		Map<String, String> mappings = withDebug(createInitialFieldMappings(request, highlightFields));

		// The identifiers map collects three documents identifiers for each document (Lucene docid, Solr "id" and "DBID").
		// Keys of the identifiers map are Solr "id", while values are simple value objects encapsulating all those three identifiers (for a specific document).
		Iterable<Integer> iterable = docs::iterator;
		Map<String, IdTriple> identifiers =
				StreamSupport.stream(iterable.spliterator(), false)
					.map(docid -> identifiersEntry(request.getSearcher(), docid, idFields, idFieldName, "DBID"))
					.filter(Objects::nonNull)
					.collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

		// First round: call the Solr highlighting procedure using the current fields mappings.
		request.setParams(rewrite(originalRequestParameters, mappings, join(",", mappings.keySet())));
		NamedList<Object> highlightingResponse = super.doHighlighting(docs, query, request, defaultFields);

		// Remember, in the first try we used the cross-language field coming from Alfresco Data Model.
		// Since it is possible that the stored content is not on that field (e.g. it could be on the localised version)
		// the highlight response for that document/field will be empty.
		// For that reason, and for those documents/fields we will repeat the highlight call using the second choice
		// (i.e. the localised version of the field).

		// Key = 2nd round fields got from Alfresco Data Model (i.e. localised fields)
		// Value = list of identifiers of documents that didn't provide the highlighting info in the first round (for the key field)
		Map<String, List<IdTriple>> missingHighlightedDocumentsByFields = new HashMap<>();

		// Additional mappings coming from this 2nd round
		Map<String, String> additionalMappings = new HashMap<>();

		identifiers.keySet()
				.forEach(id -> {
					final NamedList<Object> docHighlighting = (NamedList<Object>) highlightingResponse.get(id);
					mappings.entrySet().stream()
						// we want to process only those entries that didn't produce any result in the first round.
						.filter(fieldEntry -> docHighlighting.indexOf(fieldEntry.getKey(), 0) == -1)
						.map(fieldEntry -> {
							String solrFieldName = AlfrescoSolrDataModel.getInstance().mapProperty(fieldEntry.getValue(), FieldUse.HIGHLIGHT, request, 1);
							additionalMappings.put(solrFieldName, fieldEntry.getValue());
							return solrFieldName;})
						.peek(fieldName -> missingHighlightedDocumentsByFields.putIfAbsent(fieldName, new ArrayList<>()))
						.map(missingHighlightedDocumentsByFields::get)
						.forEach(docList -> docList.add(identifiers.get(id)));});

		mappings.putAll(additionalMappings);
		withDebug(mappings);

		// We are going to re-call the highlight for those documents/fields which didnt' produce any result in the
		// previous step. In order to do that we need
		// - a (Solr) field name: the second-level choice coming from Alfresco Data Model
		// - an artificial DocList which is subset of the input DocList
		Map<String, DocList> parametersForSubsequentHighlightRequest =
				missingHighlightedDocumentsByFields.entrySet().stream()
					.map(entry -> {
						int [] docids = entry.getValue().stream().mapToInt(IdTriple::docid).toArray();
						return new AbstractMap.SimpleEntry<>(
								entry.getKey(),
								new DocSlice(0, docids.length, docids, null, docids.length, 1));})
					.collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

		// For each field and corresponding document list, a new highlight request is executed
		List<NamedList<Object>> partialHighlightingResponses =
				parametersForSubsequentHighlightRequest.entrySet().stream()
					.map(entry -> {
						String fieldName = entry.getKey();
						DocList doclist = entry.getValue();
						try
						{
							request.setParams(rewrite(originalRequestParameters, additionalMappings, fieldName));
							return super.doHighlighting(doclist, query, request, defaultFields);
						}
						catch (Exception exception)
						{
							// This is a "2nd round" request so in that case we log the error but we still return something to
							// the requestor (i.e. the result of the first highlight call)
							LOGGER.error("Error during the execution of a \"2nd round\" highlighting request. " +
									"See the stacktrace below for further details.", exception);
							return null;
						}})
					.filter(Objects::nonNull)
					.collect(toList());

		// Combine (actually reduce) the highlight response coming from the first try, with each
		// partial highlight response coming from subsequent calls
		NamedList<Object> responseBeforeRenaming = partialHighlightingResponses.stream()
			.reduce(highlightingResponse, (accumulator, partial) -> {
				partial.forEach(entry -> {
					String id = entry.getKey();
					NamedList<Object> specificFieldHighlighting = (NamedList<Object>) entry.getValue();
					NamedList<Object> preExistingDocHighlighting = (NamedList<Object>) accumulator.get(id);
					if (preExistingDocHighlighting == null) // this document were never collected
					{
						accumulator.add(id, entry.getValue());
					}
					else
					{
						preExistingDocHighlighting.addAll(specificFieldHighlighting);
					}
				});
				return accumulator;
			});


		// Final step: under each document section, highlight snippets are associated with Solr field names,
		// so we need to replace them with fields actually requested
		// In addition, beside the snippets we want to have the document DBID as well.
		NamedList<Object> response = new SimpleOrderedMap<>();
		responseBeforeRenaming.forEach( entry -> {
					String id = entry.getKey();
					NamedList<Object> documentHighlighting = (NamedList<Object>) entry.getValue();
					NamedList<Object> renamedDocumentHighlighting = new SimpleOrderedMap<>();
					ofNullable(identifiers.get(id)).map(IdTriple::dbid).ifPresent(dbid -> renamedDocumentHighlighting.add("DBID", dbid));

					documentHighlighting.forEach(fieldEntry -> {
						String solrFieldName = fieldEntry.getKey();
						String requestFieldName = mappings.get(solrFieldName);
						renamedDocumentHighlighting.add(requestFieldName, fieldEntry.getValue());
					});

					response.add(id, renamedDocumentHighlighting);
				});

		return response;
	}

	/**
	 * Alfresco Highlighter overrides this method in order to (eventually) remove the language markers from the text.
	 */
	@Override
	protected List<String> getFieldValues(Document doc, String fieldName, int maxValues, int maxCharsToAnalyze, SolrQueryRequest req) {
		return super.getFieldValues(doc, fieldName, maxValues, maxCharsToAnalyze, req).stream()
			.map(this::withoutLanguageMarkers)
			.collect(toList());
	}

	private AbstractMap.SimpleEntry<String, IdTriple> identifiersEntry(SolrIndexSearcher searcher, int docid, Set<String> idFields, String idFieldName, String dbIdFieldName)
	{
		try
		{
			Document doc = searcher.doc(docid, idFields);
			String solrId = doc.get(idFieldName);
			return new AbstractMap.SimpleEntry<>(solrId, new IdTriple(docid, solrId, doc.get(dbIdFieldName)));
		}
		catch (Exception exception)
		{
			return null;
		}
	}

	private void rewriteLocalFieldParameters(ModifiableSolrParams newParams, SolrParams previousParams, String fieldName, String schemaFieldName)
	{
		rewriteHighlightFieldOptions(newParams, previousParams, HighlightParams.SIMPLE_PRE, fieldName, schemaFieldName);
		rewriteHighlightFieldOptions(newParams, previousParams, HighlightParams.SIMPLE_POST, fieldName, schemaFieldName);
		rewriteHighlightFieldOptions(newParams, previousParams, HighlightParams.FRAGSIZE, fieldName, schemaFieldName);
		rewriteHighlightFieldOptions(newParams, previousParams, HighlightParams.MERGE_CONTIGUOUS_FRAGMENTS, fieldName, schemaFieldName);
		rewriteHighlightFieldOptions(newParams, previousParams, HighlightParams.SNIPPETS, fieldName, schemaFieldName);
	}

	private void rewriteHighlightFieldOptions(ModifiableSolrParams fixed, SolrParams params, String paramName, String oldField, String newField)
	{
		for(Iterator<String> it = params.getParameterNamesIterator(); it.hasNext();)
		{
			String name = it.next();
			if(name.startsWith("f."))
			{
				if(name.endsWith("." + paramName))
				{

					String source = name.substring(2, name.length() - paramName.length() - 1);
					if(source.equals(oldField))
					{
						fixed.set("f." + newField + "." + paramName, params.getParams(name));
					}
					else
					{
						fixed.set(name, params.getParams(name));
					}
				}
				else
				{
					fixed.set(name, params.getParams(name));
				}
			}       
		}
	}

	private String withoutLanguageMarkers(String text)
	{
		if (isNullOrEmpty(text)) return text;
		if (startsWithLanguageMarker(text))
		{
			int index = text.indexOf('\u0000', 1);
			if (index == -1)
			{
				return text;
			} else
			{
				if (index + 1 < text.length())
				{
					return text.substring(index + 1);
				}
				else
				{
					return text;
				}
			}
		}
		return text;
	}

	private SolrParams rewrite(SolrParams params, Map<String, String> mappings, String fields)
	{
		ModifiableSolrParams rewrittenParams = new ModifiableSolrParams(params).set(HighlightParams.FIELDS, fields);
		mappings.forEach((key, value) -> rewriteLocalFieldParameters(rewrittenParams, params, value, key));
		return rewrittenParams;
	}

	/**
	 * Debugs the content of the given mappings.
	 *
	 * @param mappings the fields mapping.
	 * @return the same input mappings instance.
	 */
	private Map<String, String> withDebug(Map<String, String> mappings)
	{
		if (LOGGER.isDebugEnabled())
		{
			mappings.forEach( (solrField, requestField) -> LOGGER.debug("Request field {} has been mapped to {}", requestField, solrField));
		}
		return mappings;
	}

	/**
	 * Starting from the input requested highlight fields (i.e. fields listed in {@link HighlightParams#FIELDS} parameter)
	 * we create a map which associates each member with the corresponding field in the Solr schema.
	 * For example:
	 *
	 * <pre>
	 * 	name 	=>	text@s___t@{http://www.alfresco.org/model/content/1.0}name,
	 * 	title	=>	mltext@m___t@{http://www.alfresco.org/model/content/1.0}title,
	 * 	content	=>	content@s___t@{http://www.alfresco.org/model/content/1.0}content
	 * </pre>
	 *
	 * IMPORTANT: although returned as {@link Map} interface, the returned data structure IS MUTABLE. This is needed
	 * because during the highlighting workflow we need to change its content by adding fields.
	 *
	 * @param request the current incoming client request.
	 * @param requestedHighlightFields a list of raw fields listed in {@link HighlightParams#FIELDS} parameter
	 * @return a map associating request(ed) fields with the corresponding schema fields.
	 */
	private Map<String, String> createInitialFieldMappings(SolrQueryRequest request, List<String> requestedHighlightFields)
	{
		return requestedHighlightFields.stream()
				.map(requestFieldName ->
						new AbstractMap.SimpleEntry<>(
								AlfrescoSolrDataModel.getInstance().mapProperty(requestFieldName, FieldUse.HIGHLIGHT, request),
								requestFieldName))
				.collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue, (prev, next) -> next, HashMap::new));
	}
}