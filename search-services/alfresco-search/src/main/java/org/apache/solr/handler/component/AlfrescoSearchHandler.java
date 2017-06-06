/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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

import static org.apache.solr.common.params.CommonParams.PATH;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringWriter;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.solr.query.AbstractQParser;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.ExitableDirectoryReader;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.util.Version;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.util.ContentStream;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.PluginInfo;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.ResultContext;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.SchemaField;
import org.apache.solr.search.DocIterator;
import org.apache.solr.search.DocList;
import org.apache.solr.search.SolrQueryTimeoutImpl;
import org.apache.solr.search.facet.FacetModule;
import org.apache.solr.util.RTimerTree;
import org.apache.solr.util.SolrPluginUtils;
import org.apache.solr.util.plugin.PluginInfoInitialized;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andy
 */
public class AlfrescoSearchHandler extends RequestHandlerBase implements
		SolrCoreAware, PluginInfoInitialized {

	static final String INIT_COMPONENTS = "components";
	static final String INIT_FIRST_COMPONENTS = "first-components";
	static final String INIT_LAST_COMPONENTS = "last-components";

	private static final Logger log = LoggerFactory.getLogger(MethodHandles
			.lookup().lookupClass());

	protected volatile List<SearchComponent> components;
	private ShardHandlerFactory shardHandlerFactory;
	private PluginInfo shfInfo;
	private SolrCore core;

	protected List<String> getDefaultComponents() {
		ArrayList<String> names = new ArrayList<>(8);
		names.add(QueryComponent.COMPONENT_NAME);
		names.add(FacetComponent.COMPONENT_NAME);
		names.add(FacetModule.COMPONENT_NAME);
		names.add(MoreLikeThisComponent.COMPONENT_NAME);
		names.add(HighlightComponent.COMPONENT_NAME);
		names.add(StatsComponent.COMPONENT_NAME);
		names.add(DebugComponent.COMPONENT_NAME);
		names.add(ExpandComponent.COMPONENT_NAME);
		return names;
	}

	@Override
	public void init(PluginInfo info) {
		init(info.initArgs);
		for (PluginInfo child : info.children) {
			if ("shardHandlerFactory".equals(child.type)) {
				this.shfInfo = child;
				break;
			}
		}
	}

	/**
	 * Initialize the components based on name. Note, if using
	 * <code>INIT_FIRST_COMPONENTS</code> or <code>INIT_LAST_COMPONENTS</code>,
	 * then the {@link DebugComponent} will always occur last. If this is not
	 * desired, then one must explicitly declare all components using the
	 * <code>INIT_COMPONENTS</code> syntax.
	 */
	@Override
	@SuppressWarnings("unchecked")
	public void inform(SolrCore core) {
		this.core = core;
		Set<String> missing = new HashSet<>();
		List<String> c = (List<String>) initArgs.get(INIT_COMPONENTS);
		missing.addAll(core.getSearchComponents().checkContains(c));
		List<String> first = (List<String>) initArgs.get(INIT_FIRST_COMPONENTS);
		missing.addAll(core.getSearchComponents().checkContains(first));
		List<String> last = (List<String>) initArgs.get(INIT_LAST_COMPONENTS);
		missing.addAll(core.getSearchComponents().checkContains(last));
		if (!missing.isEmpty())
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
					"Missing SearchComponents named : " + missing);
		if (c != null && (first != null || last != null))
			throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
					"First/Last components only valid if you do not declare 'components'");

		if (shfInfo == null) {
			shardHandlerFactory = core.getCoreContainer().getShardHandlerFactory();
		} else {
			shardHandlerFactory = core.createInitInstance(shfInfo,
					ShardHandlerFactory.class, null, null);
			core.addCloseHook(new CloseHook() {
				@Override
				public void preClose(SolrCore core) {
					shardHandlerFactory.close();
				}

				@Override
				public void postClose(SolrCore core) {
				}
			});
		}

	}

	private void initComponents() {
		Object declaredComponents = initArgs.get(INIT_COMPONENTS);
		List<String> first = (List<String>) initArgs.get(INIT_FIRST_COMPONENTS);
		List<String> last = (List<String>) initArgs.get(INIT_LAST_COMPONENTS);

		List<String> list = null;
		boolean makeDebugLast = true;
		if (declaredComponents == null) {
			// Use the default component list
			list = getDefaultComponents();

			if (first != null) {
				List<String> clist = first;
				clist.addAll(list);
				list = clist;
			}

			if (last != null) {
				list.addAll(last);
			}
		} else {
			list = (List<String>) declaredComponents;
			if (first != null || last != null) {
				throw new SolrException(SolrException.ErrorCode.SERVER_ERROR,
						"First/Last components only valid if you do not declare 'components'");
			}
			makeDebugLast = false;
		}

		// Build the component list
		List<SearchComponent> components = new ArrayList<>(list.size());
		DebugComponent dbgCmp = null;
		for (String c : list) {
			SearchComponent comp = core.getSearchComponent(c);
			if (comp instanceof DebugComponent && makeDebugLast == true) {
				dbgCmp = (DebugComponent) comp;
			} else {
				components.add(comp);
				log.debug("Adding  component:" + comp);
			}
		}
		if (makeDebugLast == true && dbgCmp != null) {
			components.add(dbgCmp);
			log.debug("Adding  debug component:" + dbgCmp);
		}
		this.components = components;
	}

	public List<SearchComponent> getComponents() {
		List<SearchComponent> result = components; // volatile read
		if (result == null) {
			synchronized (this) {
				if (components == null) {
					initComponents();
				}
				result = components;
			}
		}
		return result;
	}

	private void readJsonIntoContent(SolrQueryRequest req) {
		Iterable<ContentStream> streams = req.getContentStreams();

		JSONObject json = (JSONObject) req.getContext().get(
				AbstractQParser.ALFRESCO_JSON);

		if (json == null) {
			if (streams != null) {
				try {
					Reader reader = null;
					for (ContentStream stream : streams) {
						reader = new BufferedReader(new InputStreamReader(
								stream.getStream(), "UTF-8"));
					}

					// TODO - replace with streaming-based solution e.g.
					// SimpleJSON ContentHandler
					if (reader != null) {
						json = new JSONObject(new JSONTokener(reader));
						req.getContext().put(AbstractQParser.ALFRESCO_JSON,
								json);
					}
				} catch (JSONException e) {
					// This is expected when there is no json element to the
					// request
				} catch (IOException e) {
					throw new AlfrescoRuntimeException(
							"IO Error parsing query parameters", e);
				}
			}
		}
	}

	private ShardHandler getAndPrepShardHandler(SolrQueryRequest req,
			ResponseBuilder rb) {
		ShardHandler shardHandler = null;

		rb.isDistrib = req.getParams().getBool(
				"distrib",
				req.getCore().getCoreContainer().isZooKeeperAware());
		if (!rb.isDistrib) {
			// for back compat, a shards param with URLs like
			// localhost:8983/solr will mean that this
			// search is distributed.
			final String shards = req.getParams().get(ShardParams.SHARDS);
			rb.isDistrib = ((shards != null) && (shards.indexOf('/') > 0));
		}

		if (rb.isDistrib) {
			shardHandler = shardHandlerFactory.getShardHandler();
			shardHandler.prepDistributed(rb);
			if (!rb.isDistrib) {
				shardHandler = null; // request is not distributed after all and
										// so the shard handler is not needed
			}
		}

		return shardHandler;
	}

	@Override
	public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp)
			throws Exception {
		readJsonIntoContent(req);

		List<SearchComponent> components = getComponents();
		ResponseBuilder rb = new ResponseBuilder(req, rsp, components);
		if (rb.requestInfo != null) {
			rb.requestInfo.setResponseBuilder(rb);
		}

		boolean dbg = req.getParams().getBool(CommonParams.DEBUG_QUERY, false);
		rb.setDebug(dbg);
		if (dbg == false) {// if it's true, we are doing everything anyway.
			SolrPluginUtils.getDebugInterests(
					req.getParams().getParams(CommonParams.DEBUG), rb);
		}

		final RTimerTree timer = rb.isDebug() ? req.getRequestTimer() : null;

		final ShardHandler shardHandler1 = getAndPrepShardHandler(req, rb); // creates
																			// a
																			// ShardHandler
																			// object
																			// only
																			// if
																			// it's
																			// needed

		if (timer == null) {
			// non-debugging prepare phase
			for (SearchComponent c : components) {
				c.prepare(rb);
			}
		} else {
			// debugging prepare phase
			RTimerTree subt = timer.sub("prepare");
			for (SearchComponent c : components) {
				rb.setTimer(subt.sub(c.getName()));
				c.prepare(rb);
				rb.getTimer().stop();
			}
			subt.stop();
		}

		if (!rb.isDistrib) {
			// a normal non-distributed request

			long timeAllowed = req.getParams().getLong(
					CommonParams.TIME_ALLOWED, -1L);
			if (timeAllowed > 0L) {
				SolrQueryTimeoutImpl.set(timeAllowed);
			}
			try {
				// The semantics of debugging vs not debugging are different
				// enough that
				// it makes sense to have two control loops
				if (!rb.isDebug()) {
					// Process
					for (SearchComponent c : components) {
						c.process(rb);
					}
				} else {
					// Process
					RTimerTree subt = timer.sub("process");
					for (SearchComponent c : components) {
						rb.setTimer(subt.sub(c.getName()));
						c.process(rb);
						rb.getTimer().stop();
					}
					subt.stop();

					// add the timing info
					if (rb.isDebugTimings()) {
						rb.addDebugInfo("timing", timer.asNamedList());
					}
				}
			} catch (ExitableDirectoryReader.ExitingReaderException ex) {
				log.warn("Query: " + req.getParamString() + "; "
						+ ex.getMessage());
				SolrDocumentList r = (SolrDocumentList) rb.rsp.getValues().get(
						"response");
				if (r == null)
					r = new SolrDocumentList();
				r.setNumFound(0);
				rb.rsp.add("response", r);
				if (rb.isDebug()) {
					NamedList debug = new NamedList();
					debug.add("explain", new NamedList());
					rb.rsp.add("debug", debug);
				}
				rb.rsp.getResponseHeader().add("partialResults", Boolean.TRUE);
			} finally {
				SolrQueryTimeoutImpl.reset();
			}

			if (req.getParams().getBool("alfresco.getSolrDocumentList", false)) {
				NamedList values = rsp.getValues();
				ResultContext response = (ResultContext) values.get("response");
				SolrDocumentList newResponse = new SolrDocumentList();
				DocList docs = response.getDocList();
				for (DocIterator it = docs.iterator(); it.hasNext(); /**/) {
					newResponse.add(toSolrDocument(
							req.getSearcher().doc(it.nextDoc()),
							req.getSchema()));
				}
				values.add("responseSolrDocumentList", newResponse);
			}
		} else {
			// a distributed request

			if (rb.outgoing == null) {
				rb.outgoing = new LinkedList<>();
			}
			rb.finished = new ArrayList<>();

			int nextStage = 0;
			do {
				rb.stage = nextStage;
				nextStage = ResponseBuilder.STAGE_DONE;

				// call all components
				for (SearchComponent c : components) {
					// the next stage is the minimum of what all components
					// report
					nextStage = Math.min(nextStage, c.distributedProcess(rb));
				}

				// check the outgoing queue and send requests
				while (rb.outgoing.size() > 0) {

					// submit all current request tasks at once
					while (rb.outgoing.size() > 0) {
						ShardRequest sreq = rb.outgoing.remove(0);
						sreq.actualShards = sreq.shards;
						if (sreq.actualShards == ShardRequest.ALL_SHARDS) {
							sreq.actualShards = rb.shards;
						}
						sreq.responses = new ArrayList<>(
								sreq.actualShards.length); // presume we'll get
															// a response from
															// each shard we
															// send to

						// TODO: map from shard to address[]
						for (String shard : sreq.actualShards) {
							ModifiableSolrParams params = new ModifiableSolrParams(
									sreq.params);
							params.remove(ShardParams.SHARDS); // not a
																// top-level
																// request
							params.set(CommonParams.DISTRIB, "false"); // not a
																		// top-level
																		// request
							params.remove("indent");
							params.remove(CommonParams.HEADER_ECHO_PARAMS);
							params.set(ShardParams.IS_SHARD, true); // a sub
																	// (shard)
																	// request
							params.set(ShardParams.SHARDS_PURPOSE, sreq.purpose);
							params.set(ShardParams.SHARD_URL, shard); // so the
																		// shard
																		// knows
																		// what
																		// was
																		// asked
							if (req.getContext().get(
									AbstractQParser.ALFRESCO_JSON) != null) {
								params.set(
										AbstractQParser.ALFRESCO_JSON,
										((JSONObject) req.getContext().get(
												AbstractQParser.ALFRESCO_JSON))
												.toString());
							}
							if (rb.requestInfo != null) {
								// we could try and detect when this is needed,
								// but it could be tricky
								params.set("NOW", Long.toString(rb.requestInfo
										.getNOW().getTime()));
							}
							String shardQt = params.get(ShardParams.SHARDS_QT);
							if (shardQt != null) {
								params.set(CommonParams.QT, shardQt);
							} else {
								// for distributed queries that don't include
								// shards.qt, use the original path
								// as the default but operators need to update
								// their luceneMatchVersion to enable
								// this behavior since it did not work this way
								// prior to 5.1
								if (req.getCore().getSolrConfig().luceneMatchVersion
										.onOrAfter(Version.LUCENE_5_1_0)) {
									String reqPath = (String) req.getContext()
											.get(PATH);
									if (!"/select".equals(reqPath)) {
										params.set(CommonParams.QT, reqPath);
									} // else if path is /select, then the qt
										// gets passed thru if set
								} else {
									// this is the pre-5.1 behavior, which
									// translates to sending the shard request
									// to /select
									params.remove(CommonParams.QT);
								}
							}
							shardHandler1.submit(sreq, shard, params);
						}
					}

					// now wait for replies, but if anyone puts more requests on
					// the outgoing queue, send them out immediately (by exiting
					// this loop)
					boolean tolerant = rb.req.getParams().getBool(
							ShardParams.SHARDS_TOLERANT, false);
					while (rb.outgoing.size() == 0) {
						ShardResponse srsp = tolerant ? shardHandler1
								.takeCompletedIncludingErrors() : shardHandler1
								.takeCompletedOrError();
						if (srsp == null)
							break; // no more requests to wait for

						// Was there an exception?
						if (srsp.getException() != null) {
							// If things are not tolerant, abort everything and
							// rethrow
							if (!tolerant) {
								shardHandler1.cancelAll();
								if (srsp.getException() instanceof SolrException) {
									throw (SolrException) srsp.getException();
								} else {
									throw new SolrException(
											SolrException.ErrorCode.SERVER_ERROR,
											srsp.getException());
								}
							} else {
								if (rsp.getResponseHeader().get(
										"partialResults") == null) {
									rsp.getResponseHeader().add(
											"partialResults", Boolean.TRUE);
								}
							}
						}

						rb.finished.add(srsp.getShardRequest());

						// let the components see the responses to the request
						for (SearchComponent c : components) {
							c.handleResponses(rb, srsp.getShardRequest());
						}
					}
				}

				for (SearchComponent c : components) {
					c.finishStage(rb);
				}

				// we are done when the next stage is MAX_VALUE
			} while (nextStage != Integer.MAX_VALUE);
		}

		// SOLR-5550: still provide shards.info if requested even for a short
		// circuited distrib request
		if (!rb.isDistrib
				&& req.getParams().getBool(ShardParams.SHARDS_INFO, false)
				&& rb.shortCircuitedURL != null) {
			NamedList<Object> shardInfo = new SimpleOrderedMap<Object>();
			SimpleOrderedMap<Object> nl = new SimpleOrderedMap<Object>();
			if (rsp.getException() != null) {
				Throwable cause = rsp.getException();
				if (cause instanceof SolrServerException) {
					cause = ((SolrServerException) cause).getRootCause();
				} else {
					if (cause.getCause() != null) {
						cause = cause.getCause();
					}
				}
				nl.add("error", cause.toString());
				StringWriter trace = new StringWriter();
				cause.printStackTrace(new PrintWriter(trace));
				nl.add("trace", trace.toString());
			} else {
				nl.add("numFound", rb.getResults().docList.matches());
				nl.add("maxScore", rb.getResults().docList.maxScore());
			}
			nl.add("shardAddress", rb.shortCircuitedURL);
			nl.add("time", req.getRequestTimer().getTime()); // elapsed time of
																// this request
																// so far

			int pos = rb.shortCircuitedURL.indexOf("://");
			String shardInfoName = pos != -1 ? rb.shortCircuitedURL
					.substring(pos + 3) : rb.shortCircuitedURL;
			shardInfo.add(shardInfoName, nl);
			rsp.getValues().add(ShardParams.SHARDS_INFO, shardInfo);
		}
	}

	// ////////////////////// SolrInfoMBeans methods //////////////////////

	@Override
	public String getDescription() {
		StringBuilder sb = new StringBuilder();
		sb.append("Search using components: ");
		if (components != null) {
			for (SearchComponent c : components) {
				sb.append(c.getName());
				sb.append(",");
			}
		}
		return sb.toString();
	}

	public final SolrDocument toSolrDocument(Document doc, IndexSchema schema) {
		SolrDocument out = new SolrDocument();
		for (IndexableField f : doc) {
			// Make sure multivalued fields are represented as lists
			Object existing = out.get(f.name());
			if (existing == null) {
				SchemaField sf = schema.getFieldOrNull(f.name());
				if (sf != null && sf.multiValued()) {
					List<Object> vals = new ArrayList<>();
					vals.add(f);
					out.setField(f.name(), vals);
				} else {
					out.setField(f.name(), f);
				}
			} else {
				out.addField(f.name(), f);
			}
		}
		return out;
	}
}
