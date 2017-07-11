/*-
 * #%L
 * Alfresco Remote API
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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
package org.alfresco.solr.stream;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.impl.CloudSolrClient;
import org.apache.solr.client.solrj.io.SolrClientCache;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.comp.StreamComparator;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.Explanation;
import org.apache.solr.client.solrj.io.stream.expr.Explanation.ExpressionType;
import org.apache.solr.client.solrj.io.stream.expr.Expressible;
import org.apache.solr.client.solrj.io.stream.expr.StreamExplanation;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;
import org.apache.solr.client.solrj.io.stream.expr.StreamFactory;
import org.apache.solr.client.solrj.request.QueryRequest;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;

public class SearchStream extends TupleStream implements Expressible  {

    private static final long serialVersionUID = 1;
    private String zkHost;
    private SolrParams params;
    private String collection;
    private StreamContext streamContext;
    private Iterator<Tuple> tuples;
    protected transient CloudSolrClient cloudSolrClient;


    public SearchStream(String zkHost,
                            String collection,
                            SolrParams params) throws IOException
    {
        init(collection, params, zkHost);
    }

    public SearchStream(StreamExpression expression, StreamFactory factory) throws IOException
    {
        // grab all parameters out
        String collectionName = factory.getValueOperand(expression, 0);
        List<StreamExpressionNamedParameter> namedParams = factory.getNamedOperands(expression);


        StreamExpressionNamedParameter zkHostExpression = factory.getNamedOperand(expression, "zkHost");

        // Collection Name
        if(null == collectionName)
        {
            throw new IOException(String.format(Locale.ROOT,"invalid expression %s - collectionName expected as first operand",expression));
        }

        // Named parameters - passed directly to solr as solrparams
        if(0 == namedParams.size())
        {
            throw new IOException(String.format(Locale.ROOT,"invalid expression %s - at least one named parameter expected. eg. 'q=*:*'",expression));
        }


        // pull out known named params
        ModifiableSolrParams params = new ModifiableSolrParams();
        for(StreamExpressionNamedParameter namedParam : namedParams){
            if(!namedParam.getName().equals("zkHost"))
            {
                params.add(namedParam.getName(), namedParam.getParameter().toString().trim());
            }
        }

        // zkHost, optional - if not provided then will look into factory list to get
        String zkHost = null;
        if(null == zkHostExpression)
        {
            zkHost = factory.getCollectionZkHost(collectionName);
            if(zkHost == null)
            {
                zkHost = factory.getDefaultZkHost();
            }
        }
        else if(zkHostExpression.getParameter() instanceof StreamExpressionValue)
        {
            zkHost = ((StreamExpressionValue)zkHostExpression.getParameter()).getValue();
        }
        /*
        if(null == zkHost)
        {
            throw new IOException(String.format(Locale.ROOT,"invalid expression %s - zkHost not found for collection '%s'",expression,collectionName));
        }
        */
        // We've got all the required items
        init(collectionName, params, zkHost);
    }

    public String getCollection() {
        return this.collection;
    }


    public SolrParams getParams() {
        return params;
    }

    public void setParams(SolrParams params) {
        this.params = params;
    }


    private void init(String collection,
                      SolrParams params,
                      String zkHost) throws IOException {
        this.zkHost  = zkHost;
        this.collection = collection;
        this.params = params;
    }

    @Override
    public StreamExpressionParameter toExpression(StreamFactory factory) throws IOException
    {
        // function name
        StreamExpression expression = new StreamExpression(factory.getFunctionName(this.getClass()));
        // collection
        expression.addParameter(collection);

        // parameters
        ModifiableSolrParams tmpParams = new ModifiableSolrParams(params);

        for (Entry<String, String[]> param : tmpParams.getMap().entrySet())
        {
            expression.addParameter(new StreamExpressionNamedParameter(param.getKey(),
                    String.join(",", param.getValue())));
        }

        // zkHost
        expression.addParameter(new StreamExpressionNamedParameter("zkHost", zkHost));

        return expression;
    }

    @Override
    public Explanation toExplanation(StreamFactory factory) throws IOException
    {
        StreamExplanation explanation = new StreamExplanation(getStreamNodeId().toString());
        explanation.setFunctionName(factory.getFunctionName(this.getClass()));
        explanation.setImplementingClass(this.getClass().getName());
        explanation.setExpressionType(ExpressionType.STREAM_SOURCE);
        explanation.setExpression(toExpression(factory).toString());

        // child is a datastore so add it at this point
        StreamExplanation child = new StreamExplanation(getStreamNodeId() + "-datastore");
        child.setFunctionName(String.format(Locale.ROOT, "solr (%s)", collection));
        // TODO: fix this so we know the # of workers - check with Joel about a Topic's ability to be in a
        // parallel stream.

        child.setImplementingClass("Solr/Lucene");
        child.setExpressionType(ExpressionType.DATASTORE);
        ModifiableSolrParams tmpParams = new ModifiableSolrParams(SolrParams.toMultiMap(params.toNamedList()));

        child.setExpression(tmpParams.getMap().entrySet().stream().map(e -> String.format(Locale.ROOT, "%s=%s", e.getKey(), e.getValue())).collect(Collectors.joining(",")));

        explanation.addChild(child);

        return explanation;
    }

    public List<TupleStream> children()
    {
        return new ArrayList();
    }

    public void open() throws IOException
    {
        SolrClient solrClient = null;
        ModifiableSolrParams paramsLoc = new ModifiableSolrParams(params);
        Map<String, List<String>> shardsMap = (Map<String, List<String>>)streamContext.get("shards");
        SolrClientCache cache = streamContext.getSolrClientCache();
        if(shardsMap == null) {
            solrClient = cache.getCloudSolrClient(zkHost);
        } else {
            List<String> shards = shardsMap.get(collection);
            solrClient = cache.getHttpSolrClient(shards.get(0));
            if(shards.size() > 1) {
                String shardsParam = getShardString(shardsMap.get(collection));
                paramsLoc.add("shards", shardsParam);
                paramsLoc.add("distrib", "true");
            }
        }

        RequestFactory requestFactory = (RequestFactory)streamContext.get("request-factory");
        QueryRequest request = requestFactory.getRequest(paramsLoc);

        try
        {
            if(shardsMap == null) {
                //Cloud request
                QueryResponse response = request.process(solrClient, collection);
                getTuples(response.getResults());
            } else {
                QueryResponse response = request.process(solrClient);
                List<Tuple> tupleList = getTuples(response.getResults());
                this.tuples = tupleList.iterator();
            }
        }
        catch (Exception e)
        {
            throw new IOException(e);
        }
    }

    public List<Tuple> getTuples(SolrDocumentList docs) {
        List<Tuple> tuples = new ArrayList();
        int size = docs.size();
        for(int i=0; i<size; i++) {
            SolrDocument doc = docs.get(i);
            Map map = new HashMap();
            fill(map, doc);
            Tuple tup = new Tuple(map);
            tuples.add(tup);
        }

        return tuples;
    }

    private void fill(Map map, SolrDocument doc) {
        Collection<String> keys = doc.getFieldNames();
        for(String key : keys) {
            Object o = doc.getFieldValue(key);
            map.put(key, o);
        }
    }

    private String getShardString(List<String> shards) {
        StringBuilder builder = new StringBuilder();
        for(String shard : shards) {
            if(builder.length() > 0) {
                builder.append(",");
            }
            builder.append(shard);
        }
        return builder.toString();
    }

    public void close() throws IOException
    {

    }

    public Tuple read() throws IOException
    {
        if(tuples.hasNext()) {
            Tuple tuple = tuples.next();
            return tuple;
        } else {
            Map fields = new HashMap();
            fields.put("EOF", true);
            Tuple tuple = new Tuple(fields);
            return tuple;
        }
    }

    public int getCost()
    {
        return 0;
    }

    @Override
    public StreamComparator getStreamSort()
    {
        return null;
    }


    @Override
    public void setStreamContext(StreamContext streamContext)
    {
        this.streamContext = streamContext;
    }
}

