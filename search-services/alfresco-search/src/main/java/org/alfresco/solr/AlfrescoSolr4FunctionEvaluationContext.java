/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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
package org.alfresco.solr;

import org.alfresco.repo.search.impl.parsers.AlfrescoFunctionEvaluationContext;
import org.alfresco.repo.search.impl.parsers.FTSQueryException;
import org.alfresco.service.cmr.dictionary.DictionaryService;
import org.alfresco.service.namespace.NamespacePrefixResolver;
import org.apache.solr.schema.IndexSchema;

/**
 * @author Andy
 *
 */
public class AlfrescoSolr4FunctionEvaluationContext extends AlfrescoFunctionEvaluationContext
{

    private IndexSchema indexSchema;

    /**
     * @param namespacePrefixResolver
     * @param dictionaryService
     * @param defaultNamespace
     * @param indexSchema 
     */
    public AlfrescoSolr4FunctionEvaluationContext(NamespacePrefixResolver namespacePrefixResolver, DictionaryService dictionaryService, String defaultNamespace, IndexSchema indexSchema)
    {
        super(namespacePrefixResolver, dictionaryService, defaultNamespace);
        this.indexSchema = indexSchema;
    }

    public String getLuceneFieldName(String propertyName)
    {
     
        if(indexSchema.getFieldOrNull(propertyName) != null)
        {
            return propertyName;
        }
        else
        {
            try
            {
                return super.getLuceneFieldName(propertyName);
            }
            catch(FTSQueryException e)
            {
                // unknown
                return "_dummy_";
            }
        }
    }
}
