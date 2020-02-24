package org.alfresco.solr.component;

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.search.SolrReturnFields;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;
import static org.alfresco.solr.AlfrescoSolrDataModel.FieldUse.FACET;
import static org.alfresco.solr.AlfrescoSolrDataModel.FieldUse.FTS;
import static org.alfresco.solr.AlfrescoSolrDataModel.FieldUse.ID;
import static org.alfresco.solr.AlfrescoSolrDataModel.FieldUse.SORT;

/**
 * @Author elia
 */

/**
 * Transform the fieldlist depending on the use of cached transformer:
 * [cached] -> add to the field list the translations of the fiels to the internal schema notation
 * otherwise -> modify the field list in order to contains a subset of the following fields:
 * 		id, DBID, _version_ and score
 */
public class RewriteFieldListComponent extends SearchComponent {

    private boolean checkParamsHaveBeenRewritten(SolrParams params)
    {
        return (params.get("originalFl") != null);
    }

    private void transformFieldList(SolrQueryRequest req)
    {


        Set<String> fieldListSet = new HashSet<>();

        Set<String> defaultNonCachedFields = Set.of("id","DBID", "_version_");
        Set<String> allowedNonCachedFields = new HashSet<>(defaultNonCachedFields);
        allowedNonCachedFields.add("score");

        SolrReturnFields solrReturnFields = new SolrReturnFields(req);
        String originalFieldList = req.getParams().get("fl");

        boolean cacheTransformer = ofNullable(solrReturnFields.getTransformer())
                .map(t -> t.getName())
                .map(name -> name.contains("fmap"))
                .orElse(false);

        ModifiableSolrParams params = new ModifiableSolrParams(req.getParams());


        // In case cache transformer is no set, we need to modify the field list in order return
        // only id, DBID and _version_ fields
        if (!cacheTransformer){
            if (!solrReturnFields.wantsAllFields())
            {
                fieldListSet.addAll(solrReturnFields.getLuceneFieldNames()
                        .stream()
                        .filter(field -> allowedNonCachedFields.contains(field))
                        .collect(Collectors.toSet()));
            }

            if (fieldListSet.isEmpty())
            {
                fieldListSet.addAll(defaultNonCachedFields);
            }

            params.set("fl", fieldListSet.stream().collect(Collectors.joining(",")));
        }
        else
        {
            if (solrReturnFields.wantsAllFields() || solrReturnFields.hasPatternMatching())
            {
                fieldListSet.add("*");
            }
            else
            {



                fieldListSet.addAll(solrReturnFields.getLuceneFieldNames().stream()
                        .map( field -> AlfrescoSolrDataModel.getInstance()
                                                .mapStoredProperty(field, req))
                        .filter(schemaFieldName -> schemaFieldName != null)
                        .map(schemaFieldName -> schemaFieldName.chars()
                                .mapToObj(c -> (char) c)
                                .map(c -> Character.isJavaIdentifierPart(c)? c : '?')
                                .map(Object::toString)
                                .collect(Collectors.joining()))
                        .collect(Collectors.toSet()));
            }

            params.add("fl", fieldListSet.stream().collect(Collectors.joining(",")));
        }

        // This is added for filtering the fields in the cached transformer.
        params.set("originalFl", originalFieldList);
        req.setParams(params);
    }

    @Override
    public void prepare(ResponseBuilder responseBuilder) {

        SolrQueryRequest req = responseBuilder.req;
        if (checkParamsHaveBeenRewritten(req.getParams()))
            return;

        transformFieldList(req);
    }

    @Override
    public void process(ResponseBuilder responseBuilder)
    {

    }

    @Override
    public String getDescription()
    {
        return null;
    }
}
