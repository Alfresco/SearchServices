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
package org.alfresco.repo.search.impl.lucene.analysis;

import org.apache.lucene.analysis.Analyzer;

/**
 * @author Andy
 *
 */
public class PathAnalyzer extends Analyzer
{

    /* (non-Javadoc)
     * @see org.apache.lucene.analysis.Analyzer#createComponents(java.lang.String, java.io.Reader)
     */
    @Override
    protected TokenStreamComponents createComponents(String fieldName)
    {
        return new TokenStreamComponents(new PathTokenFilter(PathTokenFilter.PATH_SEPARATOR, PathTokenFilter.SEPARATOR_TOKEN_TEXT, PathTokenFilter.NO_NS_TOKEN_TEXT, PathTokenFilter.NAMESPACE_START_DELIMITER, PathTokenFilter.NAMESPACE_END_DELIMITER, true));
    }

}
