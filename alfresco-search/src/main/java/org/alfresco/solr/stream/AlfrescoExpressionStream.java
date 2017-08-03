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
import java.util.ArrayList;
import java.util.List;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.comp.StreamComparator;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.*;
import org.apache.solr.client.solrj.io.stream.expr.Explanation.ExpressionType;

public class AlfrescoExpressionStream extends TupleStream implements Expressible  {

    private static final long serialVersionUID = 1;
    private TupleStream tupleStream;
    private final AlfrescoStreamExpressionProcessor processor = new AlfrescoStreamExpressionProcessor();

    public AlfrescoExpressionStream(StreamExpression expression, StreamFactory factory) throws IOException
    {
        List<StreamExpression> streamExpressions = factory.getExpressionOperandsRepresentingTypes(expression, Expressible.class, TupleStream.class);
        if(streamExpressions.size() != 1) {
            throw new IOException("AlfrescoExprStream expects a single TupleStream parameter, found:"+streamExpressions.size());
        }

        StreamExpression streamExpression = processor.process(streamExpressions);
        init(factory.constructStream(streamExpression));
    }

    private void init(TupleStream tupleStream) throws IOException {
        this.tupleStream = tupleStream;
    }

    @Override
    public StreamExpressionParameter toExpression(StreamFactory factory) throws IOException
    {
        // function name
        StreamExpression expression = new StreamExpression(factory.getFunctionName(this.getClass()));
        expression.addParameter(((Expressible)tupleStream).toExpression(factory));
        return expression;
    }

    @Override
    public Explanation toExplanation(StreamFactory factory) throws IOException
    {
        Explanation explanation = new StreamExplanation(getStreamNodeId().toString())
                .withChildren(new Explanation[]{
                        tupleStream.toExplanation(factory)
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
       tupleStream.open();
    }


    public void close() throws IOException
    {
        tupleStream.close();
    }

    public Tuple read() throws IOException
    {
       return tupleStream.read();
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
        this.tupleStream.setStreamContext(streamContext);
    }
}

