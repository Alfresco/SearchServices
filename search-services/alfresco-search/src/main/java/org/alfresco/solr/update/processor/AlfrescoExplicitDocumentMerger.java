/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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
package org.alfresco.solr.update.processor;

import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.SolrInputField;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.update.processor.AtomicUpdateDocumentMerger;

import java.util.Map;

/**
 * @Author elia
 *
 * Provide the possibility to perform a an explicit atomic update:
 *     We need to specify all the fields that we want to use from the old documents. The other fields will be discarded.
 *
 * In order to distinguish between standard atomic updates and explicit atomic updates we search for a keep entry in
 * the input document. If the input document contains an entry map with "keep" as a key, then the update is managed
 * as an explicit atomic update.
 */
public class AlfrescoExplicitDocumentMerger extends AtomicUpdateDocumentMerger {
    public AlfrescoExplicitDocumentMerger(SolrQueryRequest queryReq)
    {
        super(queryReq);
    }

    @Override
    public SolrInputDocument merge(final SolrInputDocument fromDoc, SolrInputDocument toDoc)
    {
        if (isExplicitAtomicUpdate(fromDoc))
        {
            processInputDocuments(fromDoc, toDoc);
        }
        return super.merge(fromDoc, toDoc);
    }

    /**
     * Process the input documents:
     *   all fields that are not contained in fromDocu are removed from toDoc.
     *   all fields with keep key are removed from fromDoc in order to avoid warnings.
     */
    private void processInputDocuments(final SolrInputDocument fromDoc, SolrInputDocument toDoc)
    {
        toDoc.entrySet().removeIf(entry -> fromDoc.getField(entry.getKey()) == null);
        fromDoc.values().removeIf(sif ->
            sif.getValue() instanceof Map &&
                    ((Map<String, Object>)sif.getValue()).get("keep") != null
        );
    }

    /**
     * Any value including a value with Map having "keep" key is marking an explicitAtomicUpdate
     */
    private boolean isExplicitAtomicUpdate(final SolrInputDocument fromDoc)
    {
        return fromDoc.values()
                .stream()
                .map(SolrInputField::getValue)
                .filter(value -> value instanceof Map)
                .anyMatch(value -> ((Map) value).get("keep") != null);
    }
}
