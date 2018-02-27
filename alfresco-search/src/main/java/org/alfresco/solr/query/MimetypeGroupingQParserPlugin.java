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

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;

import org.apache.lucene.search.Query;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.core.SolrResourceLoader;
import org.apache.solr.internal.csv.CSVParser;
import org.apache.solr.internal.csv.CSVStrategy;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.QParser;
import org.apache.solr.search.QParserPlugin;
import org.apache.solr.search.SyntaxError;

/**
 * @author Andy
 */
public class MimetypeGroupingQParserPlugin extends QParserPlugin
{
    private static HashMap<String, String> mappings = new HashMap<>();
    
    private static  HashMap<String, ArrayList<String>> reverseMappings = new HashMap<>();

    public static synchronized HashMap<String, ArrayList<String>> getReverseMappings()
    {
        return reverseMappings;
    }
    
    private static synchronized void initMap(String mappingFile)
    {
        String solrHome = SolrResourceLoader.locateSolrHome().toString();
        File file = new File(solrHome, mappingFile);

        CSVParser parser;
        try
        {
            parser = new CSVParser(new FileReader(file), CSVStrategy.DEFAULT_STRATEGY);
            // parse the fieldnames from the header of the file
            parser.getLine();

            // read the rest of the CSV file
            for (;;)
            {
                String[] vals = null;

                vals = parser.getLine();

                if (vals == null)
                    break;

                mappings.put(vals[1], vals[2]);
                
                ArrayList<String> grouped = reverseMappings.get(vals[2]);
                if(grouped == null)
                {
                    grouped = new ArrayList<>();
                    reverseMappings.put(vals[2], grouped);
                }
                if (!grouped.contains(vals[1]))
                {
                    grouped.add(vals[1]);
                }
            }
        }
        catch (FileNotFoundException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
        catch (IOException e1)
        {
            // TODO Auto-generated catch block
            e1.printStackTrace();
        }
    }

	private NamedList<Object> args;

    /*
     * (non-Javadoc)
     * @see org.apache.solr.util.plugin.NamedListInitializedPlugin#init(org.apache.solr.common.util.NamedList)
     */
    @Override
    public void init(NamedList args)
    {
    	this.args = args;
        String mappingFile = (String) args.get("mapping");
        initMap(mappingFile);

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
        return new MimetypeGroupingQParser(qstr, localParams, params, req, args, mappings);
    }

    public static class MimetypeGroupingQParser extends AbstractQParser
    {
        private HashMap<String, String> mappings;
        
        private boolean group = true;

        public MimetypeGroupingQParser(String qstr, SolrParams localParams, SolrParams params, SolrQueryRequest req, NamedList<Object> args, HashMap<String, String> mappings)
        {
            super(qstr, localParams, params, req, args);
            Boolean doGroup = localParams.getBool("group");
            if(doGroup != null)
            {
                group = doGroup.booleanValue();
            }
               
            this.mappings = mappings;
        }

        /*
         * (non-Javadoc)
         * @see org.apache.solr.search.QParser#parse()
         */
        @Override
        public Query parse() throws SyntaxError
        {
            return new MimetypeGroupingAnalyticsQuery(mappings, group);
        }
    }

}
