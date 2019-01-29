/*
 * Copyright (C) 2005-2010 Alfresco Software Limited.
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

import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.StopFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilterFactory;
import org.apache.lucene.analysis.synonym.SynonymGraphFilter;
import org.apache.lucene.analysis.tokenattributes.PositionLengthAttribute;
import org.apache.lucene.analysis.tokenattributes.TypeAttribute;

import java.util.Map;

/**
 * A {@link StopFilter} which doesn't remove stop tokens previously marked as part of a synonym.
 * When using a stop filter in a query analyzer which already includes a synonym filter, there are two options:
 * the filter could be defined before or after the synonym filter.
 *
 * However, the first way (before) doesn’t make so much sense, because terms that are stopwords and that are, at the same
 * time, part of a synonym will be removed before the synonym detection. As consequence of that no synonym detection will happen.
 *
 * If we have
 *
 * <ul>
 *     <li>a stopwords list consisting of one term (of)</li>
 *     <li>and a single synonym definition like "out of warranty,oow"</li>
 *     <li>a query which should match the synonym: q=out of warranty</li>
 * </ul>
 *
 * a stop filter defined before the synonym filter would remove the "of" term from the query, therefore causing the missing
 * synonym detection. If we postpone the stop filter after the synonym filter, the synonym will be detected, but the
 * stop filter later would still remove the "of" token causing the open issue well described in https://issues.apache.org/jira/browse/LUCENE-4065
 *
 * Being related with FilteringTokenFilter (the StopFilter superclass) LUCENE-4065 has a wider scope which doesn't affect only
 * stopwords removal. However, in order to mitigate the issue above, this custom stop filter takes in account the typeAttribute of the
 * incoming tokens; it removes them only if they are stopwords and if they haven't been marked as part of a synonym ({@link SynonymGraphFilter#TYPE_SYNONYM})
 *
 * @see SynonymGraphFilter
 * @author Andrea Gazzarini
 */
public class SynonymAwareStopFilterFactory extends StopFilterFactory
{
    class SynonymAwareStopFilter extends StopFilter
    {

        private TypeAttribute typeAttribute = addAttribute(TypeAttribute.class);
        private PositionLengthAttribute positionLengthAttribute = addAttribute(PositionLengthAttribute.class);

        private int synonymSpans;

        SynonymAwareStopFilter(TokenStream in, CharArraySet stopwords)
        {
            super(in, stopwords);
        }

        @Override
        protected boolean accept()
        {
            if (isSynonymToken())
            {
                synonymSpans = positionLengthAttribute.getPositionLength() > 1
                        ? positionLengthAttribute.getPositionLength()
                        : 0;
                return true;
            }

            return (--synonymSpans > 0) || super.accept();
        }

        private boolean isSynonymToken()
        {
            return SynonymGraphFilter.TYPE_SYNONYM.equals(typeAttribute.type());
        }
    }
    
    public SynonymAwareStopFilterFactory(Map<String, String> args)
    {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream input)
    {
        return new SynonymAwareStopFilter(input, getStopWords());
    }
}