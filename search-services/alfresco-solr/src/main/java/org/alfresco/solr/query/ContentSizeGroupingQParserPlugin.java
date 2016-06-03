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
package org.alfresco.solr.query;

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;

/**
 * @author Andy
 */
public class ContentSizeGroupingQParserPlugin extends QParserPlugin
{
    private int scale = 1;
    
    private int buckets = 10;

	private NamedList<Object> args;

    /*
     * (non-Javadoc)
     * @see org.apache.solr.util.plugin.NamedListInitializedPlugin#init(org.apache.solr.common.util.NamedList)
     */
    @Override
    public void init(NamedList args)
    {
    	this.args = args;
        if (args != null)
        {
            Object val = args.get("buckets");
            if (val != null)
            {
                buckets = Integer.parseInt(val.toString());
            }
            
            val = args.get("scale");
            if (val != null)
            {
                scale = Integer.parseInt(val.toString());
            }
        }

    }

    /*
     * (non-Javadoc)
     * @see org.apache.solr.search.QParserPlugin#createParser(java.lang.String,
     * org.apache.solr.common.params.SolrParams, org.apache.solr.common.params.SolrParams,
     * org.apache.solr.request.SolrQueryRequest)
     */
    @Override
    public QParser createParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req)
    {
        return new ContentSizeGroupingQParser(qstr, localParams, params, req, args, scale, buckets);
    }

    public static class ContentSizeGroupingQParser extends AbstractQParser
    {

        private int buckets;
        
        private int scale;

        public ContentSizeGroupingQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, NamedList<Object> args, int scale, int buckets)
        {
            super(qstr, localParams, params, req, args);
            this.scale = scale;
            this.buckets = buckets;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.solr.search.QParser#parse()
         */
        @Override
        public Query parse() throws SyntaxError
        {
            return new ContentSizeGroupingAnalyticsQuery(scale, buckets);
        }
    }

}
