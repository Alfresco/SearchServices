/*
 * Copyright (C) 2005-2017 Alfresco Software Limited.
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
package org.alfresco.solr.component;

import org.apache.solr.common.params.CommonParams;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.component.AlfrescoLukeRequestHandler;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.util.plugin.SolrCoreAware;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.lang.invoke.MethodHandles;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

/**
 * Includes and excludes parameters from a comman seperated list
 *
 * If works on a comma seperated list of writers
 *
 * @author Gethin James
 */
public class RewriteParamListComponent extends SearchComponent implements SolrCoreAware {

    private static final Logger logger = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

    public static final String PARAM_WRITERS = "writers";
    public static final String PARAM_EXCLUDE = "excludes";
    public static final String PARAM_INCLUDE = "includes";
    public static final String PARAM_REQUEST = "requestParam";
    public static final String CACHED_FIELD = "[cached]";

    public static final String DEFAULT_PARAM_NAME = CommonParams.FL;
    public static final String DEFAULT_SEPERATOR = ",";
    public static final List<String> DEFAULT_WRITERS = Arrays.asList("csv");
    public static final List<String> DEFAULT_EXCLUDED_FIELDS = Arrays.asList("FIELDS");
    public static final List<String> DEFAULT_INCLUDED_FIELDS = Arrays.asList("*",CACHED_FIELD);


    private SolrParams initArgs = null;
    private String paramName = DEFAULT_PARAM_NAME;
    private List<String> writers;
    private List<String> excludes;
    private List<String> includes;

    @Override
    public void prepare(ResponseBuilder rb) throws IOException {

        SolrQueryRequest req = rb.req;
        SolrParams params = req.getParams();

        String paramList = params.get(paramName);
        String writer = params.get(CommonParams.WT);

        if(writer != null && writers.contains(writer.toLowerCase()) && paramList != null)
        {
            String rewritten = rewrite(paramList);
            ModifiableSolrParams modParams = new ModifiableSolrParams(params);
            modParams.set(paramName, String.join(DEFAULT_SEPERATOR, includes));
            modParams.set("alfresco_"+paramName, rewritten);
            req.setParams(modParams);
        }
    }

    public String rewrite(String paramList) {
        return rewrite(paramList, this.excludes);
    }

    /**
     * Rewrites the supplied list based on the config.
     * @param paramList
     * @return the rewritten list
     */
    public static String rewrite(String paramList, List<String> allExcludes) {
        if (!paramList.isEmpty()) {
            List<String> split = new ArrayList<>(Arrays.asList(paramList.split(DEFAULT_SEPERATOR)));

            split.removeAll(allExcludes);
            if (logger.isDebugEnabled()) {
                logger.debug("Rewriting "+paramList+ " as "+String.join(DEFAULT_SEPERATOR, split));
            }
            return String.join(DEFAULT_SEPERATOR, split);
        }
        return paramList;
    }

    @Override
    public void process(ResponseBuilder rb) throws IOException {

    }

    @Override
    public String getDescription() {
        return "Rewrite the field list";
    }

    @Override
    public void init(NamedList args) {
        this.initArgs = SolrParams.toSolrParams(args);
    }

    @Override
    public void inform(SolrCore core) {
        writers  = setParam(PARAM_WRITERS, DEFAULT_WRITERS);
        excludes = setParam(PARAM_EXCLUDE, DEFAULT_EXCLUDED_FIELDS);
        includes = setParam(PARAM_INCLUDE, DEFAULT_INCLUDED_FIELDS);
        String requestParam = initArgs.get(PARAM_REQUEST);
        if (requestParam != null && !requestParam.isEmpty()) {
            paramName = requestParam;
        } else {
            paramName = DEFAULT_PARAM_NAME;
        }
    }

    protected List<String> setParam(String paramName, List<String> defaults) {
        String paramList = initArgs.get(paramName);
        if (paramList != null) {
            String[] paramSplit = paramList.split(",");
            return Arrays.asList(paramSplit);
        } else {
            logger.info("RewriteParamListComponent is missing property: "+paramName +" so using defaults "+defaults);
            return defaults;
        }
    }
}
