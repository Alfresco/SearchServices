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

import org.alfresco.opencmis.dictionary.CMISStrictDictionaryService;
import org.alfresco.repo.dictionary.NamespaceDAO;
import org.alfresco.repo.search.impl.QueryParserUtils;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.cmr.dictionary.PropertyDefinition;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.comp.StreamComparator;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.*;
import org.apache.solr.client.solrj.io.stream.expr.Explanation.ExpressionType;
import org.apache.solr.client.solrj.io.stream.metrics.*;
import org.apache.solr.common.params.ModifiableSolrParams;

import java.io.IOException;
import java.util.*;

public class AlfrescoStatsStream extends TupleStream implements Expressible  {

    private static final long serialVersionUID = 1;
    private StatsStream statsStream;

    public AlfrescoStatsStream(StreamExpression expression, StreamFactory factory) throws IOException
    {
        List<StreamExpression> streamExpressions = factory.getExpressionOperandsRepresentingTypes(expression, Expressible.class, StatsStream.class);
        if(streamExpressions.size() != 1) {
            throw new IOException("AlfrescoStatsStream expects a single Stas parameter, found:"+streamExpressions.size());
        }

        init((StatsStream)factory.constructStream(streamExpressions.get(0)));
    }

    private void init(StatsStream statsStream) throws IOException {
        this.statsStream = statsStream;
    }

    @Override
    public StreamExpressionParameter toExpression(StreamFactory factory) throws IOException
    {
        // function name
        StreamExpression expression = new StreamExpression(factory.getFunctionName(this.getClass()));
        expression.addParameter(statsStream.toExpression(factory));
        return expression;
    }

    @Override
    public Explanation toExplanation(StreamFactory factory) throws IOException
    {
        Explanation explanation = new StreamExplanation(getStreamNodeId().toString())
                .withChildren(new Explanation[]{
                        statsStream.toExplanation(factory)
                })
                .withFunctionName(factory.getFunctionName(this.getClass()))
                .withImplementingClass(this.getClass().getName())
                .withExpressionType(ExpressionType.STREAM_DECORATOR)
                .withExpression(toExpression(factory).toString());

        return explanation;
    }

    public List<TupleStream> children()
    {
        return new ArrayList();
    }

    public void open() throws IOException
    {
        ModifiableSolrParams solrParams = (ModifiableSolrParams)statsStream.getParams();
        solrParams.add("defType", "afts");
        Metric[] metrics = statsStream.getMetrics();
        Map<String, String> reverseLookup = statsStream.getReverseLookup();

        for(int i=0; i<metrics.length; i++) {
            Metric metric = metrics[i];

            if(metric instanceof  CountMetric) {
                continue;
            }

            String column = metric.getColumns()[0];
            String newColumn = AlfrescoStreamHandler.getIndexedField(column);
            reverseLookup.put(newColumn, column);
            if(metric.getFunctionName().equals("sum"))
            {
                metrics[i] = new SumMetric(newColumn);
            }
            else if(metric.getFunctionName().equals("min"))
            {
                metrics[i] = new MinMetric(newColumn);
            }
            else if(metric.getFunctionName().equals("max"))
            {
                metrics[i] = new MaxMetric(newColumn);
            }
            else if(metric.getFunctionName().equals("avg"))
            {
                metrics[i] = new MeanMetric(newColumn);
            }
        }

        this.statsStream.open();
    }

    public void close() throws IOException
    {
        statsStream.close();
    }

    public Tuple read() throws IOException
    {
        Tuple tuple = statsStream.read();
        return tuple;
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
        this.statsStream.setStreamContext(streamContext);
    }
}

