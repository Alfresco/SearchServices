/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.apache.solr.handler.component;

import static java.lang.String.join;
import static java.util.Arrays.stream;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.AlfrescoSolrDataModel.FieldUse;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Terms;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.WeightedSpanTerm;
import org.apache.lucene.search.highlight.WeightedSpanTermExtractor;
import org.apache.solr.common.params.HighlightParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.SolrCore;
import org.apache.solr.highlight.DefaultSolrHighlighter;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.StreamSupport;

/**
 * The Alfresco customisation of the original (or somewhere called default) Solr highlighter.
 * In this latest revision, the core Highlighting logic has been delegated to the Solr highlighter.
 * The main difference introduced in this customisation is related with the fields mappings (i.e. mappings between
 * the Alfresco and Solr fields names).
 *
 * The purpose of the delegation to the built-in Solr logic is to allow (later) an easier refactoring/removal.
 * Specifically, when the mapping logic will be extracted in an external (calling) layer this component will be removed
 * in favour of the built-in Solr (Default) Highlighter.
 *
 * The {@link #doHighlighting(DocList, Query, SolrQueryRequest, String[])} core method, which has been overriden,
 * provides the required mapping between the requested highlighting fields in the hl.fl parameters (which follow the
 * Alfresco semantic) and the Solr fields we have in the schema.
 *
 * At the end of the highlighting process the same mappings are used for doing the reverse process (replace the Solr
 * fields with the original requested fields).
 *
 * Additionally, since the Solr ID has no meaning outside Solr, the Highlighter adds the DBID to each highlighting
 * snippets:
 *
 * <pre>
 *  &lt;lst name="_DEFAULT_!8000016f66a1a298!8000016f66a1a29e">
 *      &lt;str name="DBID">1577974866590&lt;/str>
 *      &lt;arr name="name">
 *          &lt;str>some very &lt;em&gt;long&lt;/em&gt; name&lt;/str>
 *      &lt;/arr>
 *      &lt;arr name="title">
 *          &lt;str>This the &lt;em&gt;long&lt;/em&gt; french version of of the&lt;/str>
 *          &lt;str>This the &lt;em&gt;long&lt;/em&gt; english version of the&lt;/str>
 *      &lt;/arr>
 *  &lt;/lst>
 * </pre>
 *
 * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-2033">SEARCH-2033</a>
 */
public class AlfrescoSolrHighlighter extends DefaultSolrHighlighter implements PluginInfoInitialized
{
	private static final Logger LOGGER = LoggerFactory.getLogger(AlfrescoSolrHighlighter.class);

	private static class DocumentIdentifiers
	{
		final String solrId;
		final String dbid;

		private DocumentIdentifiers(String solrId, String dbid)
		{
			this.solrId = solrId;
			this.dbid = dbid;
		}

		String dbid()
		{
			return dbid;
		}

		@Override
		public boolean equals(Object obj)
		{
			return obj instanceof DocumentIdentifiers
					&& ((DocumentIdentifiers)obj).solrId.equals(solrId);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(solrId);
		}
	}

	private final Predicate<NamedList<Object>> notNullAndNotEmpty = response -> response != null && response.size() > 0;

	public AlfrescoSolrHighlighter(SolrCore core)
	{
		super(core);
	}

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

	/**
	 * We need to override this method because here Solr manages the {@link HighlightParams#FIELD_MATCH} parameter.
	 * In the last iteration, we improved the Solr schema by using one single stored field (used in this context for the
	 * highlighting) which is copied across multiple search fields (e.g. text@s__t,text@s_lt,mltext@m__lt).
	 *
	 * The input requestFieldName parameter is the stored field mentioned above; in case {@link HighlightParams#FIELD_MATCH}
	 * is set to true only query terms aligning with the field being highlighted will in turn be highlighted.
	 *
	 * The reverse field mapping you'll find in this method is because the stored field is not indexed, and therefore
	 * there won't be any "alignment" as described above, never.
	 *
	 * So starting from the stored field we need to query {@link AlfrescoSolrDataModel} for retrieving the corresponding
	 * cross-locale field (text|mltext@m|s__t@) and inject it into the {@link QueryScorer} that is in charge to score only
	 * those terms that contributed in generating the 'hit' on the document.
	 *
	 * TODO: this is one thing that can be improved together with the multilanguage improvement/implementation tasks
	 *
	 * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-1665">SEARCH-1665</a>
	 * @see <a href="https://issues.alfresco.com/jira/browse/SEARCH-2056">SEARCH-2056</a>
	 */
	@Override
	protected QueryScorer getSpanQueryScorer(Query query, String requestFieldname, TokenStream tokenStream, SolrQueryRequest request)
	{
		String localFieldName = requestFieldname.substring(requestFieldname.lastIndexOf("}") + 1);
		String schemaFieldName = AlfrescoSolrDataModel.getInstance().mapProperty(localFieldName, FieldUse.FTS, request);
		if (!schemaFieldName.contains("_t@{"))
		{
			schemaFieldName = AlfrescoSolrDataModel.getInstance().mapProperty(localFieldName, FieldUse.FTS, request, 1);
		}

		// In case after the second query we didn't get the cross-locale version, then it's better to ignore the
		// fieldMatch parameter; in this way we are sure the snippets will be properly returned (together with other
		// unwanted fields)
		if (!schemaFieldName.contains("_t@{"))
		{
			schemaFieldName = null;
		}

		// The query scorer purpose is to give a score to the text fragments by the number of unique query terms found.
		//
		// Fields that use the Alfresco MLAnalyser have a locale marker at the beginning of each token. For example,
		// the text "My name is Gazza" becomes (locale = en):
		//
		// {en}my {en}name {en}is {en}gazza
		//
		// while this is not an issue for searching, those tokens never find a match during the highlighting because
		// my != {en}my
		//
		// The purpose of this custom query scorer is to replace the collected weighted terms with their corresponding
		// un-localised version.
 		QueryScorer scorer = new QueryScorer(query,request.getParams().getFieldBool(requestFieldname, HighlightParams.FIELD_MATCH, false) ? schemaFieldName : null)
		{
			@Override
			protected WeightedSpanTermExtractor newTermExtractor(String defaultField)
			{
				return new WeightedSpanTermExtractor(defaultField)
				{
					@Override
					protected void extractWeightedTerms(Map<String, WeightedSpanTerm> terms, Query query, float boost) throws IOException
					{
						super.extractWeightedTerms(terms, query, boost);
						List<WeightedSpanTerm> termsWithoutLocale =
								terms.values()
										.stream()
										.peek(term -> term.setTerm(withoutLocalePrefixMarker(term.getTerm())))
										.collect(toList());

						terms.clear();
						termsWithoutLocale.forEach(term -> terms.put(term.getTerm(), term));
					}
				};
			}

			/**
			 * Removes the locale marker from the given text.
			 *
			 * @param text the input text.
			 * @return the text without the beginning locale marker, or the same text is the marker cannot be found.
			 */
			private String withoutLocalePrefixMarker(String text)
			{
				if (text == null) return null;

				int startIndexOfMarker = text.indexOf("{");
				if (startIndexOfMarker == 0)
				{
					int endIndexOfMarker = text.indexOf("}");
					if (endIndexOfMarker != -1 && text.length() > (endIndexOfMarker + 1))
					{
						return text.substring(endIndexOfMarker + 1);
					}
				}

				return text;
			}
		};
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
			For each field name collected in the previous step, we need to query the AlfrescoSolrDataModel in order to
			retrieve the corresponding field that will be used for the current highlighting request.

			e.g.
		 	{
		 		name 	=>	text@s_stored_lt@{http://www.alfresco.org/model/content/1.0}name,
		 		title	=>	mltext@m__stored_lt@{http://www.alfresco.org/model/content/1.0}title,
		 		content	=>	content@s_stored_lt@{http://www.alfresco.org/model/content/1.0}content
		 	}

		 	Since at the end we need to restore (in the response) the original request(ed) fields names (e.g. content, name) used on client side
		 	we need to maintain a map which associates each schema field (e.g. text@s_stored_lt@{http://www.alfresco.org/model/content/1.0}name)
		 	with the corresponding request(ed) field (e.g. name).
		*/
		Map<String, String> mappings = withDebug(createInitialFieldMappings(request, highlightFields));

		// The identifiers map collects two documents identifiers for each document (Solr "id" and "DBID").
		// Keys of the identifiers map are Solr "id", while values are simple value objects encapsulating all those two identifiers (for a specific document).
		Iterable<Integer> iterable = docs::iterator;
		Map<String, DocumentIdentifiers> identifiers =
				StreamSupport.stream(iterable.spliterator(), false)
					.map(docid -> identifiersEntry(request.getSearcher(), docid, idFields, idFieldName))
					.filter(Objects::nonNull)
					.collect(toMap(AbstractMap.SimpleEntry::getKey, AbstractMap.SimpleEntry::getValue));

		// First round: call the Solr highlighting procedure using the current fields mappings.
		request.setParams(rewrite(originalRequestParameters, mappings, join(",", mappings.keySet())));
		NamedList<Object> highlightingResponse = super.doHighlighting(docs, query, request, defaultFields);

        // Final step: under each document section, highlight snippets are associated with Solr field names,
		// so we need to replace them with fields actually requested
		// In addition, beside the snippets we want to have the document DBID as well.
		NamedList<Object> response = new SimpleOrderedMap<>();
		highlightingResponse.forEach( entry -> {
					String id = entry.getKey();
					NamedList<Object> documentHighlighting = (NamedList<Object>) entry.getValue();
					NamedList<Object> renamedDocumentHighlighting = new SimpleOrderedMap<>();
					if (notNullAndNotEmpty.test(documentHighlighting))
					{
						ofNullable(identifiers.get(id))
								.map(DocumentIdentifiers::dbid)
								.ifPresent(dbid -> renamedDocumentHighlighting.add("DBID", dbid));
					}

					documentHighlighting.forEach(fieldEntry -> {
						detectAndRemoveLocalePrefix(fieldEntry);

						String solrFieldName = fieldEntry.getKey();
						String requestFieldName = mappings.get(solrFieldName);
						renamedDocumentHighlighting.add(requestFieldName, fieldEntry.getValue());
					});

					response.add(id, renamedDocumentHighlighting);
				});

		return response;
	}

	/**
	 * Remember the stored field, used for highlighting, contains a locale marker prefix which has to be removed
	 * before returning back the response.
	 *
	 * @param highlightFieldEntry the response highlight entry for a specific field.
	 */
	private void detectAndRemoveLocalePrefix(Map.Entry<String, Object> highlightFieldEntry)
	{
		if (highlightFieldEntry.getValue() instanceof String[])
		{
			String [] snippets = (String[])highlightFieldEntry.getValue();
			if (snippets.length > 0)
			{
				snippets[0] = snippets[0].charAt(3) == '\u0000' ? snippets[0].substring(4) : snippets[0].substring(5);
			}
		}
	}

	private AbstractMap.SimpleEntry<String, DocumentIdentifiers> identifiersEntry(SolrIndexSearcher searcher, int docid, Set<String> idFields, String idFieldName)
	{
		try
		{
			Document doc = searcher.doc(docid, idFields);
			String solrId = doc.get(idFieldName);
			return new AbstractMap.SimpleEntry<>(solrId, new DocumentIdentifiers(solrId, doc.get("DBID")));
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