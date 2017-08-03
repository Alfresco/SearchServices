/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.alfresco.solr.stream;

import org.apache.solr.client.solrj.io.Tuple;
import org.apache.solr.client.solrj.io.comp.StreamComparator;
import org.apache.solr.client.solrj.io.stream.StreamContext;
import org.apache.solr.client.solrj.io.stream.TupleStream;
import org.apache.solr.client.solrj.io.stream.expr.*;

import java.io.IOException;
import java.util.*;

public class LimitStream extends TupleStream implements Expressible {

  private final TupleStream stream;
  private final int limit;
  private int count;

  public LimitStream(TupleStream stream, int limit) {
    this.stream = stream;
    this.limit = limit;
  }

  public LimitStream(StreamExpression expression, StreamFactory factory) throws IOException {

    List<StreamExpression> streamExpressions = factory.getExpressionOperandsRepresentingTypes(expression, Expressible.class, TupleStream.class);

    if(1 != streamExpressions.size()){
      throw new IOException(String.format(Locale.ROOT,"Invalid expression %s - expecting a single stream but found %d",expression, streamExpressions.size()));
    }

    String limitStr = factory.getValueOperand(expression, 1);

    try {
      limit = Integer.parseInt(limitStr);
      if(limit <= 0) {
        throw new IOException(String.format(Locale.ROOT,"invalid expression %s - limit '%s' must be greater than 0.",expression, limitStr));
      }
    } catch(NumberFormatException e) {
      throw new IOException(String.format(Locale.ROOT,"invalid expression %s - limit '%s' is not a valid integer.",expression, limitStr));
    }

    stream = factory.constructStream(streamExpressions.get(0));
  }

  public void open() throws IOException {
    this.stream.open();
  }

  public void close() throws IOException {
    this.stream.close();
  }

  public List<TupleStream> children() {
    List<TupleStream> children = new ArrayList<>();
    children.add(stream);
    return children;
  }

  public StreamComparator getStreamSort(){
    return stream.getStreamSort();
  }

  public void setStreamContext(StreamContext context) {
    stream.setStreamContext(context);
  }

  @Override
  public StreamExpressionParameter toExpression(StreamFactory factory) throws IOException {
    StreamExpression expression = new StreamExpression(factory.getFunctionName(this.getClass()));
    expression.addParameter(String.valueOf(limit));
    return expression;
  }

  @Override
  public Explanation toExplanation(StreamFactory factory) throws IOException {
    StreamExplanation explanation = new StreamExplanation(getStreamNodeId().toString());
    explanation.setFunctionName(factory.getFunctionName(this.getClass()));
    explanation.setImplementingClass(this.getClass().getName());
    explanation.setExpressionType(Explanation.ExpressionType.STREAM_DECORATOR);
    explanation.setExpression(toExpression(factory).toString());
    return explanation;
  }

  public Tuple read() throws IOException {
    ++count;
    if(count > limit) {
      Map<String, String> fields = new HashMap<>();
      fields.put("EOF", "true");
      return new Tuple(fields);
    }

    return stream.read();
  }
}