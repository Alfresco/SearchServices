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

package org.alfresco.solr.schema.highlight;

import org.alfresco.solr.AlfrescoAnalyzerWrapper;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.schema.TextField;

import java.util.Map;

/**
 * A custom {@link TextField} type which is able to set at runtime the analyzer to be used.
 * Although the core part of the logic needed for that dynamic assignment is in {@link LanguagePrefixedTokenStream}
 * the actors group is also composed by:
 *
 * <ul>
 *     <li>a {@link TextField} subclass (this class)</li>
 *     <li>a custom {@link org.apache.lucene.analysis.Analyzer}</li>
 *     <li>a custom token stream {@link LanguagePrefixedTokenStream}</li>
 * </ul>
 *
 * Both of them have no specific logic: they exist only because the components involved in the analysis chain don't have
 * access to the {@link IndexSchema} instance (e.g. a {@link org.apache.lucene.analysis.Tokenizer} is a schema concept,
 * while {@link IndexSchema} belongs to Solr classes).
 * On top of that, the purpose of this class is to associate to this field type the index and query time analyzer.
 *
 * @see LanguagePrefixedTokenStream
 * @see LanguagePrefixedTextAnalyzer
 * @author Andrea Gazzarini
 */
public class LanguagePrefixedTextField extends TextField
{
	@Override
	protected final void init(IndexSchema schema, Map<String,String> args)
    {
	    super.init(schema, args);
		setIndexAnalyzer(new LanguagePrefixedTextAnalyzer(schema, AlfrescoAnalyzerWrapper.Mode.INDEX));
		setQueryAnalyzer(new LanguagePrefixedTextAnalyzer(schema, AlfrescoAnalyzerWrapper.Mode.QUERY));
	}
}
