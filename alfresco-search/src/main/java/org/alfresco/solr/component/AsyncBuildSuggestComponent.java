package org.alfresco.solr.component;

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
 * 
 * 
 * This file is based upon code copied from the Solr project and modified.
 * The original code is licensed to the Apache Software Foundation. Please see:
 * 
 *   http://lucene.apache.org/solr/
 *   http://www.apache.org/licenses/LICENSE-2.0
 */

import java.io.File;
import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.alfresco.util.cache.AbstractAsynchronouslyRefreshedCache;
import org.alfresco.util.cache.DefaultAsynchronouslyRefreshedCacheRegistry;
import org.apache.lucene.search.suggest.Lookup;
import org.apache.lucene.search.suggest.Lookup.LookupResult;
import org.apache.lucene.util.Accountable;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRef;
import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.apache.solr.core.CloseHook;
import org.apache.solr.core.SolrCore;
import org.apache.solr.core.SolrEventListener;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.handler.component.ShardResponse;
import org.apache.solr.handler.component.SuggestComponent;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.solr.spelling.suggest.SolrSuggester;
import org.apache.solr.spelling.suggest.SuggesterOptions;
import org.apache.solr.spelling.suggest.SuggesterParams;
import org.apache.solr.spelling.suggest.SuggesterResult;
import org.apache.solr.util.RefCounted;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Originally taken from {@link SuggestComponent} and modified, this
 * class provides a {@link SearchComponent} that builds {@link SolrSuggester}
 * objects asynchronously to avoid blocking search
 * requests or increasing startup time of the servlet container.
 * 
 * @see org.apache.solr.handler.component.SuggestComponent
 */
public class AsyncBuildSuggestComponent extends SearchComponent implements SolrCoreAware, SuggesterParams, Accountable {
	private static final Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  
  /** Name used to identify whether the user query concerns this component */
  public static final String COMPONENT_NAME = "suggest";
  
  /** Name assigned to an unnamed suggester (at most one suggester) can be unnamed */
  private static final String DEFAULT_DICT_NAME = SolrSuggester.DEFAULT_DICT_NAME;
  
  /** SolrConfig label to identify  Config time settings */
  private static final String CONFIG_PARAM_LABEL = "suggester";
  
  /** SolrConfig label to identify boolean value to build suggesters on commit */
  private static final String BUILD_ON_COMMIT_LABEL = "buildOnCommit";
  
  /** SolrConfig label to identify boolean value to build suggesters on optimize */
  private static final String BUILD_ON_OPTIMIZE_LABEL = "buildOnOptimize";
  
	/**
	 * SolrConfig label to identify boolean value to build suggesters on startup
	 */
	private static final String BUILD_ON_STARTUP_LABEL = "buildOnStartup";

  /** SolrConfig label to identify boolean value describing whether suggesters should be built at all. */
  private static final String ENABLED_LABEL = "enabled";
  
  private static final String ASYNC_CACHE_KEY = "suggester";

  private static final String MIN_SECS_BETWEEN_BUILDS = "solr.suggester.minSecsBetweenBuilds";
  
  private static File offlineSorterTempDir;
  
  static
  {
      try
      {
    	  String tempDirPath = System.getProperty("java.io.tmpdir");
    	  Path tempDirectory = Paths.get(tempDirPath);
    	  if(!Files.exists(tempDirectory) 
    			  && (!tempDirectory.toFile().mkdirs()))
    	  {
    		  throw new RuntimeException("No OfflineSorter temp directory");
    	  }
    	  if(!Files.isDirectory(tempDirectory) 
    			  || Files.isHidden(tempDirectory)
    			  || !Files.isReadable(tempDirectory)
             )
    	  {
    		  throw new RuntimeException("No OfflineSorter temp directory");
    	  } 
    			  
          //offlineSorterTempDir = OfflineSorter.getDefaultTempDir().toFile();
          offlineSorterTempDir = tempDirectory.toFile();
      }
      catch (IOException e)
      {
          throw new RuntimeException("No OfflineSorter temp directory", e);
      }
  }
  
  protected static TempFileWarningLogger tempFileWarningLogger =
              new TempFileWarningLogger(
                          LOG,
                          "WFSTInputIterator*",
                          new String[] { "input", "sorted" },
                          offlineSorterTempDir.toPath());
  
 
  protected NamedList initParams;
  
  /**
   * Key is the dictionary name used in SolrConfig, value is the corresponding {@link SolrSuggester}
   */
  protected Map<String, SuggesterCache> suggesters = new ConcurrentHashMap<>();
  
  /** Container for various labels used in the responses generated by this component */
  private static class SuggesterResultLabels {
    static final String SUGGEST = "suggest";
    static final String SUGGESTIONS = "suggestions";
    static final String SUGGESTION_NUM_FOUND = "numFound";
    static final String SUGGESTION_TERM = "term";
    static final String SUGGESTION_WEIGHT = "weight";
    static final String SUGGESTION_PAYLOAD = "payload";
  }
  
  @Override
  @SuppressWarnings("unchecked")
  public void init(NamedList args) {
    super.init(args);
    this.initParams = args;
  }
  
  @Override
  public void inform(SolrCore core) {
    if (initParams != null) {
      LOG.info("Initializing SuggestComponent");
      boolean hasDefault = false;
      for (int i = 0; i < initParams.size(); i++) {
        if (initParams.getName(i).equals(CONFIG_PARAM_LABEL)) {
          NamedList suggesterParams = (NamedList) initParams.getVal(i);
          SolrSuggester suggester = new SolrSuggester();
          boolean buildOnStartup;
		  Object buildOnStartupObj = suggesterParams.get(BUILD_ON_STARTUP_LABEL);
		  if (buildOnStartupObj == null) {
		    File storeFile = suggester.getStoreFile();
			buildOnStartup = storeFile == null  || !storeFile.exists();
		  } else {
			buildOnStartup = Boolean.parseBoolean((String) buildOnStartupObj);
	      }
          boolean buildOnCommit = Boolean.parseBoolean((String) suggesterParams.get(BUILD_ON_COMMIT_LABEL));
          boolean buildOnOptimize = Boolean.parseBoolean((String) suggesterParams.get(BUILD_ON_OPTIMIZE_LABEL));
          boolean enabled = Boolean.parseBoolean((String) suggesterParams.get(ENABLED_LABEL));
          long minSecsBetweenBuilds = Long.parseLong(core.getCoreDescriptor().getCoreProperty(MIN_SECS_BETWEEN_BUILDS, "-1")); 
          SuggesterCache suggesterCache = new SuggesterCache(core, suggesterParams, enabled, buildOnCommit, buildOnOptimize, buildOnStartup);
          
          String dictionary = suggester.init(suggesterParams, core);
          if (dictionary != null) {
            boolean isDefault = dictionary.equals(DEFAULT_DICT_NAME);
            if (isDefault && !hasDefault) {
              hasDefault = true;
            } else if (isDefault){
              throw new RuntimeException("More than one dictionary is missing name.");
            }
            suggesterCache.setBeanName(dictionary);
            suggesters.put(dictionary, suggesterCache);
          } else {
            if (!hasDefault){
              suggesterCache.setBeanName(DEFAULT_DICT_NAME);
              suggesters.put(DEFAULT_DICT_NAME, suggesterCache);
              hasDefault = true;
            } else {
              throw new RuntimeException("More than one dictionary is missing name.");
            }
          }
          
          try
          {
            suggesterCache.afterPropertiesSet();
          }
          catch (Exception e)
          {
            LOG.error("Unable to initialise SuggesterCache.", e);
            throw new RuntimeException("Unable to initialise SuggesterCache.", e);
          }
          
          // Register event listeners for this Suggester
          core.registerFirstSearcherListener(new SuggesterListener(suggesterCache, minSecsBetweenBuilds));
          if (buildOnCommit || buildOnOptimize) {
            LOG.debug("Registering newSearcher listener for suggester: " + suggester.getName());
            core.registerNewSearcherListener(new SuggesterListener(suggesterCache, minSecsBetweenBuilds));
          }
        }
      }
    }
  }

  /** Responsible for issuing build and rebload command to the specified {@link SolrSuggester} */
  @Override
  public void prepare(ResponseBuilder rb) throws IOException {
    SolrParams params = rb.req.getParams();
    LOG.debug("SuggestComponent prepare with : " + params);
    if (!params.getBool(COMPONENT_NAME, false)) {
      return;
    }
    
    boolean buildAll = params.getBool(SUGGEST_BUILD_ALL, false);
    boolean reloadAll = params.getBool(SUGGEST_RELOAD_ALL, false);
    
    final Collection<SolrSuggester> querysuggesters;
    if (buildAll || reloadAll) {
      Collection<SuggesterCache> suggesterCaches = suggesters.values();
      querysuggesters = new ArrayList<SolrSuggester>(suggesterCaches.size());
      for (SuggesterCache cache : suggesterCaches)
      {
          querysuggesters.add(cache.get(ASYNC_CACHE_KEY));
      }
    } else {
      querysuggesters = getSuggesters(params);
    }
    
    if (params.getBool(SUGGEST_BUILD, false) || buildAll) {
      for (SolrSuggester suggester : querysuggesters) {
        suggester.build(rb.req.getCore(), rb.req.getSearcher());
      }
      rb.rsp.add("command", (!buildAll) ? "build" : "buildAll");
    } else if (params.getBool(SUGGEST_RELOAD, false) || reloadAll) {
      for (SolrSuggester suggester : querysuggesters) {
    		reloadSuggester(suggester, rb.req.getCore(),
					rb.req.getSearcher());
      }
      rb.rsp.add("command", (!reloadAll) ? "reload" : "reloadAll");
    }
  }
  
	private void reloadSuggester(SolrSuggester suggester, SolrCore core,
			SolrIndexSearcher searcher) throws IOException {
		File storeFile = suggester.getStoreFile();
		if (storeFile == null || !storeFile.exists()) {
			suggester.build(core, searcher);
		} else {
			suggester.reload(core, searcher);
		}
	}
  
  /** Dispatch shard request in <code>STAGE_EXECUTE_QUERY</code> stage */
  @Override
  public int distributedProcess(ResponseBuilder rb) {
    SolrParams params = rb.req.getParams();
    LOG.debug("SuggestComponent distributedProcess with : " + params);
    if (rb.stage < ResponseBuilder.STAGE_EXECUTE_QUERY) 
      return ResponseBuilder.STAGE_EXECUTE_QUERY;
    if (rb.stage == ResponseBuilder.STAGE_EXECUTE_QUERY) {
      ShardRequest sreq = new ShardRequest();
      sreq.purpose = ShardRequest.PURPOSE_GET_TOP_IDS;
      sreq.params = new ModifiableSolrParams(rb.req.getParams());
      sreq.params.remove(ShardParams.SHARDS);
      rb.addRequest(this, sreq);
      return ResponseBuilder.STAGE_GET_FIELDS;
    }

    return ResponseBuilder.STAGE_DONE;
  }

  /** 
   * Responsible for using the specified suggester to get the suggestions 
   * for the query and write the results 
   * */
  @Override
  public void process(ResponseBuilder rb) throws IOException {
    SolrParams params = rb.req.getParams();
    LOG.debug("SuggestComponent process with : " + params);
    if (!params.getBool(COMPONENT_NAME, false) || suggesters.isEmpty()) {
      return;
    }
    
    boolean buildAll = params.getBool(SUGGEST_BUILD_ALL, false);
    boolean reloadAll = params.getBool(SUGGEST_RELOAD_ALL, false);
    Set<SolrSuggester> querySuggesters;
    try {
      querySuggesters = getSuggesters(params);
    } catch(IllegalArgumentException ex) {
      if (!buildAll && !reloadAll) {
        throw ex;
      } else {
        querySuggesters = new HashSet<>();
      }
    }
    
    String query = params.get(SUGGEST_Q);
    if (query == null) {
      query = rb.getQueryString();
      if (query == null) {
        query = params.get(CommonParams.Q);
      }
    }
    
    if (query != null) {
      int count = params.getInt(SUGGEST_COUNT, 1);
      
      boolean highlight = params.getBool(SUGGEST_HIGHLIGHT, false);
		boolean allTermsRequired = params.getBool(
				SUGGEST_ALL_TERMS_REQUIRED, true);
		String contextFilter = params.get(SUGGEST_CONTEXT_FILTER_QUERY);
		if (contextFilter != null) {
			contextFilter = contextFilter.trim();
			if (contextFilter.length() == 0) {
				contextFilter = null;
			}
		}
      
      SuggesterOptions options = new SuggesterOptions(new CharsRef(query), count, contextFilter, allTermsRequired, highlight);
      Map<String, SimpleOrderedMap<NamedList<Object>>> namedListResults = 
          new HashMap<>();
      for (SolrSuggester suggester : querySuggesters) {
        SuggesterResult suggesterResult = suggester.getSuggestions(options);
        toNamedList(suggesterResult, namedListResults);
      }
      rb.rsp.add(SuggesterResultLabels.SUGGEST, namedListResults);
    }
  }
  
  /** 
   * Used in Distributed Search, merges the suggestion results from every shard
   * */
  @Override
  public void finishStage(ResponseBuilder rb) {
    SolrParams params = rb.req.getParams();
    LOG.debug("SuggestComponent finishStage with : " + params);
    if (!params.getBool(COMPONENT_NAME, false) || rb.stage != ResponseBuilder.STAGE_GET_FIELDS)
      return;
    int count = params.getInt(SUGGEST_COUNT, 1);
    
    List<SuggesterResult> suggesterResults = new ArrayList<>();
    
    // Collect Shard responses
    for (ShardRequest sreq : rb.finished) {
      for (ShardResponse srsp : sreq.responses) {
        NamedList<Object> resp;
        if((resp = srsp.getSolrResponse().getResponse()) != null) {
          @SuppressWarnings("unchecked")
          Map<String, SimpleOrderedMap<NamedList<Object>>> namedList = 
              (Map<String, SimpleOrderedMap<NamedList<Object>>>) resp.get(SuggesterResultLabels.SUGGEST);
          LOG.debug(srsp.getShard() + " : " + namedList);
          suggesterResults.add(toSuggesterResult(namedList));
        }
      }
    }
    
    // Merge Shard responses
    SuggesterResult suggesterResult = merge(suggesterResults, count);
    Map<String, SimpleOrderedMap<NamedList<Object>>> namedListResults = 
        new HashMap<>();
    toNamedList(suggesterResult, namedListResults);
    
    rb.rsp.add(SuggesterResultLabels.SUGGEST, namedListResults);
  }

  /** 
   * Given a list of {@link SuggesterResult} and <code>count</code>
   * returns a {@link SuggesterResult} containing <code>count</code>
   * number of {@link LookupResult}, sorted by their associated 
   * weights
   * */
  private static SuggesterResult merge(List<SuggesterResult> suggesterResults, int count) {
    SuggesterResult result = new SuggesterResult();
    Set<String> allTokens = new HashSet<>();
    Set<String> suggesterNames = new HashSet<>();
    
    // collect all tokens
    for (SuggesterResult shardResult : suggesterResults) {
      for (String suggesterName : shardResult.getSuggesterNames()) {
        allTokens.addAll(shardResult.getTokens(suggesterName));
        suggesterNames.add(suggesterName);
      }
    }
    
    // Get Top N for every token in every shard (using weights)
    for (String suggesterName : suggesterNames) {
      for (String token : allTokens) {
        Lookup.LookupPriorityQueue resultQueue = new Lookup.LookupPriorityQueue(
            count);
        for (SuggesterResult shardResult : suggesterResults) {
          List<LookupResult> suggests = shardResult.getLookupResult(suggesterName, token);
          if (suggests == null) {
            continue;
          }
          for (LookupResult res : suggests) {
            resultQueue.insertWithOverflow(res);
          }
        }
        List<LookupResult> sortedSuggests = new LinkedList<>();
        Collections.addAll(sortedSuggests, resultQueue.getResults());
        result.add(suggesterName, token, sortedSuggests);
      }
    }
    return result;
  }
  
  @Override
  public String getDescription() {
    return "Suggester component";
  }

  @Override
  public String getSource() {
    return "$URL: https://svn.apache.org/repos/asf/lucene/dev/branches/lucene_solr_4_9/solr/core/src/java/org/apache/solr/handler/component/SuggestComponent.java $";
  }
  
  @Override
  public NamedList<String> getStatistics() {
    NamedList<String> stats = new SimpleOrderedMap<>();
    stats.add("totalSizeInBytes", String.valueOf(ramBytesUsed()));
    for (Map.Entry<String, SuggesterCache> entry : suggesters.entrySet()) {
      SolrSuggester suggester = entry.getValue().get(entry.getKey());
      stats.add(entry.getKey(), suggester.toString());
    }
    return stats;
  }
  
  @Override
  public long ramBytesUsed() {
    long sizeInBytes = 0;
    for (String key : suggesters.keySet()) {
      sizeInBytes += suggesters.get(key).get(ASYNC_CACHE_KEY).ramBytesUsed();
    }
    return sizeInBytes;
  }
  
  private Set<SolrSuggester> getSuggesters(SolrParams params) {
    Set<SolrSuggester> solrSuggesters = new HashSet<>();
    for(String suggesterName : getSuggesterNames(params)) {
      SolrSuggester curSuggester = suggesters.get(suggesterName).get(ASYNC_CACHE_KEY);
      if (curSuggester != null) {
        solrSuggesters.add(curSuggester);
      } else {
        throw new IllegalArgumentException("No suggester named " + suggesterName +" was configured");
      }
    }
    if (solrSuggesters.size() == 0) {
        throw new IllegalArgumentException("No default suggester was configured");
    }
    return solrSuggesters;
    
  }
  
  private Set<String> getSuggesterNames(SolrParams params) {
    Set<String> suggesterNames = new HashSet<>();
    String[] suggesterNamesFromParams = params.getParams(SUGGEST_DICT);
    if (suggesterNamesFromParams == null) {
      suggesterNames.add(DEFAULT_DICT_NAME);
    } else {
      for (String name : suggesterNamesFromParams) {
        suggesterNames.add(name);
      }
    }
    return suggesterNames;   
  }
  
  /** Convert {@link SuggesterResult} to NamedList for constructing responses */
  private void toNamedList(SuggesterResult suggesterResult, Map<String, SimpleOrderedMap<NamedList<Object>>> resultObj) {
    for(String suggesterName : suggesterResult.getSuggesterNames()) {
      SimpleOrderedMap<NamedList<Object>> results = new SimpleOrderedMap<>();
      for (String token : suggesterResult.getTokens(suggesterName)) {
        SimpleOrderedMap<Object> suggestionBody = new SimpleOrderedMap<>();
        List<LookupResult> lookupResults = suggesterResult.getLookupResult(suggesterName, token);
        suggestionBody.add(SuggesterResultLabels.SUGGESTION_NUM_FOUND, lookupResults.size());
        List<SimpleOrderedMap<Object>> suggestEntriesNamedList = new ArrayList<>();
        for (LookupResult lookupResult : lookupResults) {
          String suggestionString = lookupResult.key.toString();
          long weight = lookupResult.value;
          String payload = (lookupResult.payload != null) ? 
              lookupResult.payload.utf8ToString()
              : "";
          
          SimpleOrderedMap<Object> suggestEntryNamedList = new SimpleOrderedMap<>();
          suggestEntryNamedList.add(SuggesterResultLabels.SUGGESTION_TERM, suggestionString);
          suggestEntryNamedList.add(SuggesterResultLabels.SUGGESTION_WEIGHT, weight);
          suggestEntryNamedList.add(SuggesterResultLabels.SUGGESTION_PAYLOAD, payload);
          suggestEntriesNamedList.add(suggestEntryNamedList);
          
        }
        suggestionBody.add(SuggesterResultLabels.SUGGESTIONS, suggestEntriesNamedList);
        results.add(token, suggestionBody);
      }
      resultObj.put(suggesterName, results);
    }
  }
  
  /** Convert NamedList (suggester response) to {@link SuggesterResult} */
  private SuggesterResult toSuggesterResult(Map<String, SimpleOrderedMap<NamedList<Object>>> suggestionsMap) {
    SuggesterResult result = new SuggesterResult();
    if (suggestionsMap == null) {
      return result;
    }
    // for each token
    for(Map.Entry<String, SimpleOrderedMap<NamedList<Object>>> entry : suggestionsMap.entrySet()) {
      String suggesterName = entry.getKey();
      for (Iterator<Map.Entry<String, NamedList<Object>>> suggestionsIter = entry.getValue().iterator(); suggestionsIter.hasNext();) {
        Map.Entry<String, NamedList<Object>> suggestions = suggestionsIter.next(); 
        String tokenString = suggestions.getKey();
        List<LookupResult> lookupResults = new ArrayList<>();
        NamedList<Object> suggestion = suggestions.getValue();
        // for each suggestion
        for (int j = 0; j < suggestion.size(); j++) {
          String property = suggestion.getName(j);
          if (property.equals(SuggesterResultLabels.SUGGESTIONS)) {
            @SuppressWarnings("unchecked")
            List<NamedList<Object>> suggestionEntries = (List<NamedList<Object>>) suggestion.getVal(j);
            for(NamedList<Object> suggestionEntry : suggestionEntries) {
              String term = (String) suggestionEntry.get(SuggesterResultLabels.SUGGESTION_TERM);
              Long weight = (Long) suggestionEntry.get(SuggesterResultLabels.SUGGESTION_WEIGHT);
              String payload = (String) suggestionEntry.get(SuggesterResultLabels.SUGGESTION_PAYLOAD);
              LookupResult res = new LookupResult(new CharsRef(term), weight, new BytesRef(payload));
              lookupResults.add(res);
            }
          }
          result.add(suggesterName, tokenString, lookupResults);
        }
      }
    }
    return result;
  }
  
  /** Listener to build or reload the maintained {@link SolrSuggester} by this component */
  private static class SuggesterListener implements SolrEventListener {
    private final SuggesterCache suggesterCache;
    private final long minSecsBetweenBuilds;
    
    public SuggesterListener(SuggesterCache suggesterCache, long minSecsBetweenBuilds) {
      this.suggesterCache = suggesterCache;
      this.minSecsBetweenBuilds = minSecsBetweenBuilds;
    }

    @Override
    public void init(NamedList args) {}

    @Override
    public void newSearcher(SolrIndexSearcher newSearcher,
                            SolrIndexSearcher currentSearcher) {
        if (currentSearcher == null)
        {
            // firstSearcher event - always queue a suggester build.
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Scheduling first searcher's suggester build, core: " + newSearcher.getCore().getName());
            }
            suggesterCache.refresh(ASYNC_CACHE_KEY);
        }
        else
        {
            // only queue a build if the designated time has passed.
            long now = System.currentTimeMillis();
            long lastBuild = suggesterCache.getLastBuild();
            long elapsedTimeMillis = (lastBuild == 0) ? 0 : (now - lastBuild);
            long elapsedTimeSecs = (lastBuild == 0) ? 0 : (elapsedTimeMillis / 1000);
            if (elapsedTimeSecs > minSecsBetweenBuilds)
            {
                if (LOG.isDebugEnabled())
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Scheduling async suggester build");
                    if (lastBuild > 0)
                    {
                        sb.append(", time since last build: ").
                            append(elapsedTimeSecs).append("s (").append(elapsedTimeMillis).append("ms)");
                    }
                    sb.append(", core: ").append(newSearcher.getCore().getName());
                    LOG.debug(sb.toString());
                }
                suggesterCache.refresh(ASYNC_CACHE_KEY);
            }
            else
            {
                if (LOG.isDebugEnabled())
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("Skipping async suggester build");
                    if (lastBuild > 0)
                    {
                        sb.append(", time since last build: ").
                            append(elapsedTimeSecs).append("s (").append(elapsedTimeMillis).append("ms)");
                    }
                    sb.append(", core: ").append(newSearcher.getCore().getName());
                    LOG.debug(sb.toString());
                }
            }
        }
    }

    @Override
    public void postCommit() {}

    @Override
    public void postSoftCommit() {}
    
  }
  
  
  static class SuggesterCache extends AbstractAsynchronouslyRefreshedCache<SolrSuggester>
  {
    private final SolrCore core;
    private final AtomicBoolean isNewSearcher = new AtomicBoolean(false);
    private final NamedList suggesterParams;
    private final boolean buildOnCommit;
    private final boolean buildOnOptimize;
    private final boolean buildOnStartup;
    private final boolean enabled;
    private final SolrSuggester initialSuggester;
    private long lastBuild = 0;
    
    public SuggesterCache(SolrCore core, NamedList suggesterParams, boolean enabled, boolean buildOnCommit, boolean buildOnOptimize, boolean buildOnStartup)
    {
        this.core = core;
        this.suggesterParams = suggesterParams;
        this.enabled = enabled;
        this.buildOnCommit = buildOnCommit;
        this.buildOnOptimize = buildOnOptimize;
        this.buildOnStartup = buildOnStartup;
      
        setRegistry(new DefaultAsynchronouslyRefreshedCacheRegistry());
        BlockingQueue<Runnable> threadPool = new LinkedBlockingQueue<Runnable>();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(1, 1, 0L, TimeUnit.MILLISECONDS, threadPool, getThreadFactory(core));
        setThreadPoolExecutor(executor);

        //Shutdown the executor on core close
        core.addCloseHook(new CloseHook() {
          @Override
          public void preClose(SolrCore core) {
            executor.shutdown();
          }

          @Override
          public void postClose(SolrCore core) {

          }
        });

        // Create and configure the initial empty suggester
        initialSuggester = new SolrSuggester();
        initialSuggester.init(suggesterParams, core);
    }
    
    /**
     * @return
     */
    private ThreadFactory getThreadFactory(SolrCore core)
    {
       return new SuggestorThreadFactory("Suggestor-"+core.getName()+"-");
    }

    /**
     * The abstract base class' get() method will block if a value for key has
     * not previously been calculated (e.g. there is no 'live' value).
     * <p>
     * We don't want to block, we just want to return an empty suggester. The
     * newSearcher events will make sure that a suggester is built and put live
     * when possible, so it is fine to do this.
     */
    @Override
    public SolrSuggester get(String key)
    {
        liveLock.readLock().lock();
        try
        {
            if (live.get(key) == null)
            {
                if (LOG.isDebugEnabled())
                {
                    LOG.debug("Cache has no live suggester yet, return empty one while we wait.");
                }
                return initialSuggester;
            }
            else
            {
                // Since we have a live suggester, return it.
                return super.get(key);
            }
        }
        finally
        {
            liveLock.readLock().unlock();
        }
    }


    @Override
    protected SolrSuggester buildCache(String key)
    {
        if (!enabled)
        {
            // When disabled, provide an empty, yet initialised suggester.
            if (LOG.isDebugEnabled())
            {
                LOG.debug("Disabled suggester builder, returning empty suggester: " + core.getName());
            }
            return initialSuggester;
        }
        
        tempFileWarningLogger.checkFiles();

        RefCounted<SolrIndexSearcher> refCountedSearcher = core.getSearcher();
        try
        {
            SolrIndexSearcher searcher = refCountedSearcher.get();
            
            // Create and configure the suggester
            SolrSuggester suggester = new SolrSuggester();
            suggester.init(suggesterParams, core);
            
            if (!isNewSearcher.getAndSet(true)) {
              // firstSearcher event
              try {
                LOG.info("Loading suggester index for: " + suggester.getName());
                final long startMillis = System.currentTimeMillis();
                if(buildOnStartup)
                {
                	 buildSuggesterIndex(suggester, searcher);
                }
                else
                {
                    suggester.reload(core, searcher);
                }
                final long timeTakenMillis = System.currentTimeMillis() - startMillis;
                LOG.info("Loaded suggester " + suggester.getName() + ", took " + timeTakenMillis + " ms");
              } catch (IOException e) {
                LOG.error("Exception in reloading suggester index for: " + suggester.getName(), e);
              }
            } else {
              // newSearcher event
              if (buildOnCommit)  {
                buildSuggesterIndex(suggester, searcher);
              } else if (buildOnOptimize) {
              if (searcher.getIndexReader().leaves().size() == 1)  {
                buildSuggesterIndex(suggester, searcher);
              } else  {
                LOG.info("Index is not optimized therefore skipping building suggester index for: " 
                        + suggester.getName());
              }
            }
          }
          lastBuild = System.currentTimeMillis();
          return suggester;
        }
        finally
        {
            refCountedSearcher.decref();
        }
    }
    
    /**
     * Returns the time stamp of the last completed suggester build.
     * If no real build has taken place yet, zero is returned.
     *  
     * @return long
     */
    public long getLastBuild()
    {
        return lastBuild;
    }
    
    private void buildSuggesterIndex(SolrSuggester suggester, SolrIndexSearcher newSearcher) {
      try {
        LOG.info("Building suggester index for: " + suggester.getName());
        final long startMillis = System.currentTimeMillis();
        suggester.build(core, newSearcher);
        final long timeTakenMillis = System.currentTimeMillis() - startMillis;
        LOG.info("Built suggester " + suggester.getName() + ", took " + timeTakenMillis + " ms");
      } catch (Exception e) {
        LOG.error("Exception in building suggester index for: " + suggester.getName(), e);
      }
    }
  }
  
  static class SuggestorThreadFactory implements ThreadFactory {
      private final ThreadGroup group;
      private final AtomicInteger threadNumber = new AtomicInteger(1);
      private final String namePrefix;

      SuggestorThreadFactory(String namePrefix) 
      {
          SecurityManager s = System.getSecurityManager();
          group = (s != null) ? s.getThreadGroup() :
                                Thread.currentThread().getThreadGroup();
          
         this.namePrefix = namePrefix;
      }

      public Thread newThread(Runnable r) {
          Thread t = new Thread(group, r,
                                namePrefix + threadNumber.getAndIncrement(),
                                0);
          t.setDaemon(true);
          return t;
      }
  }

@Override
public Collection<Accountable> getChildResources() {
	return Collections.<Accountable>emptyList();
}



}
