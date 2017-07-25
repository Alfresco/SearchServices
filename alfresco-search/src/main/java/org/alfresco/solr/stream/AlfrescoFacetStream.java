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

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.comp.FieldComparator;
import org.apache.solr.client.solrj.io.comp.StreamComparator;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.*;
import org.apache.solr.client.solrj.io.stream.expr.Explanation.ExpressionType;
import org.apache.solr.client.solrj.io.stream.metrics.*;
import org.apache.solr.common.params.ModifiableSolrParams;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AlfrescoFacetStream extends TupleStream implements Expressible  {

    private static final long serialVersionUID = 1;
    private FacetStream facetStream;

    public AlfrescoFacetStream(StreamExpression expression, StreamFactory factory) throws IOException
    {
        List<StreamExpression> streamExpressions = factory.getExpressionOperandsRepresentingTypes(expression, Expressible.class, FacetStream.class);
        if(streamExpressions.size() != 1) {
            throw new IOException("AlfrescoFacetStream expects a single facets parameter, found:"+streamExpressions.size());
        }

        init((FacetStream)factory.constructStream(streamExpressions.get(0)));
    }

    private void init(FacetStream facetStream) throws IOException {
        this.facetStream = facetStream;
    }

    @Override
    public StreamExpressionParameter toExpression(StreamFactory factory) throws IOException
    {
        // function name
        StreamExpression expression = new StreamExpression(factory.getFunctionName(this.getClass()));
        expression.addParameter(facetStream.toExpression(factory));
        return expression;
    }

    @Override
    public Explanation toExplanation(StreamFactory factory) throws IOException
    {
        Explanation explanation = new StreamExplanation(getStreamNodeId().toString())
                .withChildren(new Explanation[]{
                        facetStream.toExplanation(factory)
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
        ModifiableSolrParams solrParams = (ModifiableSolrParams)facetStream.getParams();
        solrParams.add("defType", "afts");
        Metric[] metrics = facetStream.getMetrics();
        Map<String, String> reverseLookup = facetStream.getReverseLookup();


        for(int i=0; i<metrics.length; i++) {
            Metric metric = metrics[i];

            if(metric instanceof  CountMetric) {
                continue;
            }

            String column = metric.getColumns()[0];
            String newColumn = "field("+AlfrescoStreamHandler.getIndexedField(column)+")";

           // reverseLookup.put(newColumn, column);
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

        Bucket[] buckets = facetStream.getBuckets();
        for (int i = 0; i <buckets.length ; i++) {
            String newColumn = AlfrescoStreamHandler.getIndexedField(buckets[i].toString());
            reverseLookup.put(newColumn, buckets[i].toString());
            buckets[i] = new Bucket(newColumn);
        }

        FieldComparator[] bucketSorts = facetStream.getBucketSorts();
        for (int i = 0; i <bucketSorts.length ; i++) {
            bucketSorts[i] =  new FieldComparator(getAlfrescoIndexedFieldName(bucketSorts[i].getLeftFieldName()), bucketSorts[i].getOrder());
        }

        this.facetStream.open();
    }

    /**
     * Used to retrieve a sort field name from within a metric or index field
     * @return A string name
     */
    public static String getAlfrescoIndexedFieldName(String theField)
    {
        if (theField != null && !theField.trim().isEmpty())
        {
           int bracketIndex = theField.indexOf("(");
           int endIndex = theField.indexOf(")");
           if (bracketIndex != -1 && endIndex != -1)
           {
               String newField = theField.substring(bracketIndex+1, endIndex);
               return theField.substring(0, bracketIndex+1)
                       + AlfrescoStreamHandler.getIndexedField(newField)
                       + theField.substring(endIndex);
           }
        }
        return AlfrescoStreamHandler.getIndexedField(theField);
    }

    public void close() throws IOException
    {
        facetStream.close();
    }

    public Tuple read() throws IOException
    {
        Tuple tuple = facetStream.read();
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
        this.facetStream.setStreamContext(streamContext);
    }
}

