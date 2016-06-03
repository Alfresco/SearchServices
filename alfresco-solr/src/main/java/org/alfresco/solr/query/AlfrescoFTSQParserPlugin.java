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
package org.alfresco.solr.query;

import org.alfresco.repo.search.impl.parsers.FTSQueryParser.RerankPhase;
import org.alfresco.service.cmr.search.SearchParameters;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.util.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Andy
 */
public class AlfrescoFTSQParserPlugin extends QParserPlugin
{
    protected final static Logger log = LoggerFactory.getLogger(AlfrescoFTSQParserPlugin.class);
   
	private NamedList<Object> args;
	
    /*
     * (non-Javadoc)
     * @see org.apache.solr.search.QParserPlugin#createParser(java.lang.String,
     * org.apache.solr.common.params.SolrParams, org.apache.solr.common.params.SolrParams,
     * org.apache.solr.request.SolrQueryRequest)
     */
    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req)
    {
        return new AlfrescoFTSQParser(qstr, localParams, params, req, args);
    }

    /*
     * (non-Javadoc)
     * @see org.apache.solr.util.plugin.NamedListInitializedPlugin#init(org.apache.solr.common.util.NamedList)
     */
    @Override
    public void init(NamedList args)
    {
    	this.args = args;
    }

    public static class AlfrescoFTSQParser extends AbstractQParser
    {
    	private RerankPhase rerankPhase = RerankPhase.SINGLE_PASS_WITH_AUTO_PHRASE;
        private boolean postfilter;

		public AlfrescoFTSQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, NamedList<Object> args)
        {
            super(qstr, localParams, params, req, args);
            Object arg = args.get("rerankPhase");
        	if(arg != null)
        	{
                rerankPhase = RerankPhase.valueOf(arg.toString());
        	}

            postfilter = Boolean.parseBoolean(req.getCore().getCoreDescriptor().getCoreProperty("alfresco.postfilter", System.getProperty("alfresco.postfilter", "true")));
        }

        /*
         * (non-Javadoc)
         * @see org.apache.solr.search.QParser#parse()
         */
        @Override
        public Query parse() throws SyntaxError
        {
            try
            {
                Pair<SearchParameters, Boolean> searchParametersAndFilter = getSearchParameters();

                Query query = AlfrescoSolrDataModel.getInstance().getFTSQuery(searchParametersAndFilter, req, rerankPhase);
                if(log.isDebugEnabled())
                {
                    log.debug("AFTS QP query as lucene:\t    "+query);
                }

                if(authset && postfilter)
                {
                    return new PostFilterQuery(200, query);
                }

                return query;
            }
            catch(ParseException e)
            {
                throw new SyntaxError(e);
            }
        }
    }

}
