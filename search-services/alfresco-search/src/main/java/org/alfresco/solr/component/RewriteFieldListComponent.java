/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

package org.alfresco.solr.component;

import org.alfresco.solr.AlfrescoSolrDataModel;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.transform.DocTransformer;
import org.apache.solr.search.SolrReturnFields;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static java.util.Optional.ofNullable;

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
                .map(DocTransformer::getName)
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
                        .filter(allowedNonCachedFields::contains)
                        .collect(Collectors.toSet()));
            }

            if (fieldListSet.isEmpty())
            {
                fieldListSet.addAll(defaultNonCachedFields);
            }

            params.set("fl", String.join(",", fieldListSet));
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
                        .filter(Objects::nonNull)
                        .map(schemaFieldName -> schemaFieldName.chars()
                                .mapToObj(c -> (char) c)
                                .map(c -> Character.isJavaIdentifierPart(c)? c : '?')
                                .map(Object::toString)
                                .collect(Collectors.joining()))
                        .collect(Collectors.toSet()));
            }

            params.add("fl", String.join(",", fieldListSet));
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
