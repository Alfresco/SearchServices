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
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.AlfrescoSolrDataModel.TenantAclIdDbId;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.content.SolrContentStore;
import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexableField;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrException.ErrorCode;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.handler.clustering.ClusteringEngine;
import org.apache.solr.handler.clustering.ClusteringParams;
import org.apache.solr.handler.clustering.DocumentClusteringEngine;
import org.apache.solr.handler.clustering.SearchClusteringEngine;
import org.apache.solr.handler.clustering.carrot2.CarrotClusteringEngine;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.DocListAndSet;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;

/**
 * @author Andy
 *
 */
public class AlfrescoSolrClusteringComponent extends SearchComponent implements
		SolrCoreAware {
	private static final Logger log = LoggerFactory.getLogger(MethodHandles
			.lookup().lookupClass());

	

	/**
	 * Base name for all component parameters. This name is also used to
	 * register this component with SearchHandler.
	 */
	public static final String COMPONENT_NAME = "clustering";

	/**
	 * Declaration-order list of search clustering engines.
	 */
	private final LinkedHashMap<String, SearchClusteringEngine> searchClusteringEngines = Maps
			.newLinkedHashMap();

	/**
	 * Declaration order list of document clustering engines.
	 */
	private final LinkedHashMap<String, DocumentClusteringEngine> documentClusteringEngines = Maps
			.newLinkedHashMap();

	/**
	 * An unmodifiable view of {@link #searchClusteringEngines}.
	 */
	private final Map<String, SearchClusteringEngine> searchClusteringEnginesView = Collections
			.unmodifiableMap(searchClusteringEngines);

	/**
	 * Initialization parameters temporarily saved here, the component is
	 * initialized in {@link #inform(SolrCore)} because we need to know the
	 * core's {@link SolrResourceLoader}.
	 * 
	 * @see #init(NamedList)
	 */
	private NamedList<Object> initParams;

	@Override
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public void init(NamedList args) {
		this.initParams = args;
		super.init(args);
	}

	@SuppressWarnings("unchecked")
	@Override
	public void inform(SolrCore core) {
		if (initParams != null) {
			log.info("Initializing Clustering Engines");

			// Our target list of engines, split into search-results and
			// document clustering.
			SolrResourceLoader loader = core.getResourceLoader();

			for (Map.Entry<String, Object> entry : initParams) {
				if ("engine".equals(entry.getKey())) {
					NamedList<Object> engineInitParams = (NamedList<Object>) entry
							.getValue();
					Boolean optional = engineInitParams
							.getBooleanArg("optional");
					optional = (optional == null ? Boolean.FALSE : optional);

					String engineClassName = StringUtils.defaultIfBlank(
							(String) engineInitParams.get("classname"),
							CarrotClusteringEngine.class.getName());

					// Instantiate the clustering engine and split to
					// appropriate map.
					final ClusteringEngine engine = loader.newInstance(
							engineClassName, ClusteringEngine.class);
					final String name = StringUtils.defaultIfBlank(
							engine.init(engineInitParams, core), "");

					if (!engine.isAvailable()) {
						if (optional) {
							log.info("Optional clustering engine not available: "
									+ name);
						} else {
							throw new SolrException(ErrorCode.SERVER_ERROR,
									"A required clustering engine failed to initialize, check the logs: "
											+ name);
						}
					}

					final ClusteringEngine previousEntry;
					if (engine instanceof SearchClusteringEngine) {
						previousEntry = searchClusteringEngines.put(name,
								(SearchClusteringEngine) engine);
					} else if (engine instanceof DocumentClusteringEngine) {
						previousEntry = documentClusteringEngines.put(name,
								(DocumentClusteringEngine) engine);
					} else {
						log.warn("Unknown type of a clustering engine for class: "
								+ engineClassName);
						continue;
					}
					if (previousEntry != null) {
						log.warn("Duplicate clustering engine component named '"
								+ name + "'.");
					}
				}
			}

			// Set up the default engine key for both types of engines.
			setupDefaultEngine("search results clustering",
					searchClusteringEngines);
			setupDefaultEngine("document clustering", documentClusteringEngines);

			log.info("Finished Initializing Clustering Engines");
		}
	}

	@Override
	public void prepare(ResponseBuilder rb) throws IOException {
		SolrParams params = rb.req.getParams();
		if (!params.getBool(COMPONENT_NAME, false)) {
			return;
		}
	}

	@Override
	public void process(ResponseBuilder rb) throws IOException {
		SolrParams params = rb.req.getParams();
		if (!params.getBool(COMPONENT_NAME, false)) {
			return;
		}

		final String name = getClusteringEngineName(rb);
		boolean useResults = params.getBool(
				ClusteringParams.USE_SEARCH_RESULTS, false);
		if (useResults == true) {
			SearchClusteringEngine engine = searchClusteringEngines.get(name);
			if (engine != null) {
				checkAvailable(name, engine);
				DocListAndSet results = rb.getResults();
				Map<SolrDocument, Integer> docIds = Maps
						.newHashMapWithExpectedSize(results.docList.size());
				SolrDocumentList solrDocList = docListToSolrDocumentList(
						results.docList, rb.req, docIds);
				Object clusters = engine.cluster(rb.getQuery(), solrDocList,
						docIds, rb.req);
				rb.rsp.add("clusters", clusters);
			} else {
				log.warn("No engine named: " + name);
			}
		}

		boolean useCollection = params.getBool(ClusteringParams.USE_COLLECTION,
				false);
		if (useCollection == true) {
			DocumentClusteringEngine engine = documentClusteringEngines
					.get(name);
			if (engine != null) {
				checkAvailable(name, engine);
				boolean useDocSet = params.getBool(
						ClusteringParams.USE_DOC_SET, false);
				NamedList<?> nl = null;

				// TODO: This likely needs to be made into a background task
				// that runs in an executor
				if (useDocSet == true) {
					nl = engine.cluster(rb.getResults().docSet, params);
				} else {
					nl = engine.cluster(params);
				}
				rb.rsp.add("clusters", nl);
			} else {
				log.warn("No engine named: " + name);
			}
		}
	}

	private void checkAvailable(String name, ClusteringEngine engine) {
		if (!engine.isAvailable()) {
			throw new SolrException(ErrorCode.SERVER_ERROR,
					"Clustering engine declared, but not available, check the logs: "
							+ name);
		}
	}

	private String getClusteringEngineName(ResponseBuilder rb) {
		return rb.req.getParams().get(ClusteringParams.ENGINE_NAME,
				ClusteringEngine.DEFAULT_ENGINE_NAME);
	}
	
	public SolrDocumentList docListToSolrDocumentList(DocList docs,
			SolrQueryRequest req, Map<SolrDocument, Integer> ids)
			throws IOException {
	    
		SolrDocumentList list = new SolrDocumentList();
		list.setNumFound(docs.matches());
		list.setMaxScore(docs.maxScore());
		list.setStart(docs.offset());

		DocIterator dit = docs.iterator();

		while (dit.hasNext()) {
			int docid = dit.nextDoc();

			Document luceneDoc = req.getSearcher().doc(docid);
			SolrInputDocument input = getSolrInputDocument(luceneDoc, req);

			SolrDocument doc = new SolrDocument();

			for (String fieldName : input.getFieldNames()) {

				doc.addField(fieldName, input.getFieldValue(fieldName));
			}

			doc.addField("score", dit.score());

			list.add(doc);

			if (ids != null) {
				ids.put(doc, new Integer(docid));
			}
		}
		return list;
	}

	

	private SolrInputDocument getSolrInputDocument(Document doc,
			SolrQueryRequest req) throws IOException {
		try {
			String id = getFieldValueString(doc, FIELD_SOLR4_ID);
			TenantAclIdDbId tenantAndDbId = AlfrescoSolrDataModel
					.decodeNodeDocumentId(id);
			
			CoreContainer coreContainer = req.getSearcher().getCore().getCoreContainer();
			AlfrescoCoreAdminHandler coreAdminHandler = (AlfrescoCoreAdminHandler) coreContainer.getMultiCoreHandler();
			SolrInformationServer srv = (SolrInformationServer) coreAdminHandler.getInformationServers().get(req.getSearcher().getCore().getName());
            SolrContentStore solrContentStore = srv.getSolrContentStore();
			SolrInputDocument sid = solrContentStore.retrieveDocFromSolrContentStore(
					tenantAndDbId.tenant, tenantAndDbId.dbId);
			return sid;
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

	@Override
	public void finishStage(ResponseBuilder rb) {
		SolrParams params = rb.req.getParams();
		if (!params.getBool(COMPONENT_NAME, false)
				|| !params.getBool(ClusteringParams.USE_SEARCH_RESULTS, false)) {
			return;
		}

		if (rb.stage == ResponseBuilder.STAGE_GET_FIELDS) {
			String name = getClusteringEngineName(rb);
			SearchClusteringEngine engine = searchClusteringEngines.get(name);
			if (engine != null) {
				checkAvailable(name, engine);
				SolrDocumentList solrDocList = (SolrDocumentList) rb.rsp
						.getValues().get("response");
				// TODO: Currently, docIds is set to null in distributed
				// environment.
				// This causes CarrotParams.PRODUCE_SUMMARY doesn't work.
				// To work CarrotParams.PRODUCE_SUMMARY under distributed mode,
				// we can choose either one of:
				// (a) In each shard, ClusteringComponent produces summary and
				// finishStage()
				// merges these summaries.
				// (b) Adding doHighlighting(SolrDocumentList, ...) method to
				// SolrHighlighter and
				// making SolrHighlighter uses "external text" rather than
				// stored values to produce snippets.
				Map<SolrDocument, Integer> docIds = null;
				Object clusters = engine.cluster(rb.getQuery(), solrDocList,
						docIds, rb.req);
				rb.rsp.add("clusters", clusters);
			} else {
				log.warn("No engine named: " + name);
			}
		}
	}

	/**
	 * @return Expose for tests.
	 */
	Map<String, SearchClusteringEngine> getSearchClusteringEngines() {
		return searchClusteringEnginesView;
	}

	@Override
	public String getDescription() {
		return "A Clustering component";
	}

	/**
	 * Setup the default clustering engine.
	 * 
	 * @see "https://issues.apache.org/jira/browse/SOLR-5219"
	 */
	private static <T extends ClusteringEngine> void setupDefaultEngine(
			String type, LinkedHashMap<String, T> map) {
		// If there's already a default algorithm, leave it as is.
		String engineName = ClusteringEngine.DEFAULT_ENGINE_NAME;
		T defaultEngine = map.get(engineName);

		if (defaultEngine == null || !defaultEngine.isAvailable()) {
			// If there's no default algorithm, and there are any algorithms
			// available,
			// the first definition becomes the default algorithm.
			for (Map.Entry<String, T> e : map.entrySet()) {
				if (e.getValue().isAvailable()) {
					engineName = e.getKey();
					defaultEngine = e.getValue();
					map.put(ClusteringEngine.DEFAULT_ENGINE_NAME, defaultEngine);
					break;
				}
			}
		}

		if (defaultEngine != null) {
			log.info("Default engine for " + type + ": " + engineName + " ["
					+ defaultEngine.getClass().getSimpleName() + "]");
		} else {
			log.warn("No default engine for " + type + ".");
		}
	}
}
