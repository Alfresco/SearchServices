package org.alfresco.solr.stream;

import org.apache.solr.client.solrj.io.stream.expr.StreamExpression;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionNamedParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionParameter;
import org.apache.solr.client.solrj.io.stream.expr.StreamExpressionValue;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Evaluates StreamExpressions
 */
public class AlfrescoStreamExpressionProcessor {

    /**
     * Processes the streaming expression
     * @param streamExpressions
     * @return StreamExpression
     * @throws IOException
     */
    public StreamExpression process(List<StreamExpression> streamExpressions) throws IOException {
        if(streamExpressions.size() != 1) {
            throw new IOException("Expecting a single TupleStream parameter, found:"+streamExpressions.size());
        }
        StreamExpression streamExpression = streamExpressions.get(0);
        return this.process(streamExpression);
    }

    /**
     * Processes a streaming Expression
     * @param streamExpression
     * @return returns the result of the processing
     */
    public StreamExpression process(StreamExpression streamExpression)
    {
        String functionName = streamExpression.getFunctionName();
        switch (functionName)
        {
            case "timeSeries":
                return wrap("alfrescoTimeSeries", streamExpression);
            case "stats":
                return wrap("alfrescoStats", streamExpression);
            case "facet":
                return wrap("alfrescoFacets", streamExpression);
            default:
                return processChildren(streamExpression);
        }
    }

    /**
     * Loops through the parameters for the stream expression, evaluating each one.
     * @param streamExpression
     * @param parent
     */
    protected StreamExpression processChildren(StreamExpression streamExpression) {
        List<StreamExpressionParameter> newParameters = new ArrayList(streamExpression.getParameters());
        List<StreamExpressionParameter> oldParameters = streamExpression.getParameters();

        for(int i=0; i<oldParameters.size(); i++) {
            StreamExpressionParameter streamExpressionParameter = oldParameters.get(i);
            if (streamExpressionParameter instanceof StreamExpressionValue)
            {
                continue;
            }
            if(streamExpressionParameter instanceof StreamExpressionNamedParameter)
            {
                StreamExpressionNamedParameter namedParameter = (StreamExpressionNamedParameter)streamExpressionParameter;
                StreamExpressionParameter aParam = namedParameter.getParameter();
                if (aParam instanceof StreamExpression)
                {
                    namedParameter.setParameter(this.process((StreamExpression)aParam));
                }
            }
            else if (streamExpressionParameter instanceof StreamExpression)
            {
                StreamExpression anExpression = this.process((StreamExpression)streamExpressionParameter);
                newParameters.set(i, anExpression);
            }
        }
        return streamExpression.withParameters(newParameters);
    }

    /**
     * Wraps the streaming expression in another one
     * @param wrappingExpression
     * @param streamExpression
     * @return StreamExpression
     */
    protected StreamExpression wrap(String wrappingExpression, StreamExpression streamExpression) {
        StreamExpression wrapper = new StreamExpression(wrappingExpression);
        wrapper.addParameter(streamExpression);
        return wrapper;
    }
}
