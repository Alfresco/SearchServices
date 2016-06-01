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
package org.alfresco.solr.query;

import java.io.IOException;

/**
 * Search for a term relative to the last one found.
 * 
 * @author andyh
 */
public class RelativeStructuredFieldPosition extends AbstractStructuredFieldPosition
{

    int relativePosition;

    /**
     * 
     */
    public RelativeStructuredFieldPosition(String termText)
    {
        super(getTermText(termText), true, false);
        relativePosition = 1;
        
    }

    private static String getTermText(String termText)
    {
        if((termText != null) && (termText.equals("*")))
        {
            return null;
        }
        else
        {
            return termText;
        }
    }
    
    public RelativeStructuredFieldPosition()
    {
        super(null, false, false);
        relativePosition = 1;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.alfresco.lucene.extensions.StructuredFieldPosition#matches(int,
     *      int, org.apache.lucene.index.TermPositions)
     */
    public int matches(int start, int end, int offset) throws IOException
    {

        if (getCachingTermPositions() != null)
        {
            // Doing "termText"
            getCachingTermPositions().reset();
            int count = getCachingTermPositions().freq();
            int requiredPosition = offset + relativePosition;
            int realPosition = 0;
            int adjustedPosition = 0;
            for (int i = 0; i < count; i++)
            {
                realPosition = getCachingTermPositions().nextPosition();
                adjustedPosition = realPosition - start;
                if ((end != -1) && (realPosition > end))
                {
                    return -1;
                }
                if (adjustedPosition == requiredPosition)
                {
                    return adjustedPosition;
                }
                if (adjustedPosition > requiredPosition)
                {
                    return -1;
                }
            }
        }
        else
        {
            // Doing "*";
            return offset + 1;
        }
        return -1;
    }
    
    public String getDescription()
    {
        return "Relative Named child";
    }

    @Override
    public int hashCode()
    {
        final int prime = 31;
        int result = super.hashCode();
        result = prime * result + relativePosition;
        return result;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (this == obj)
            return true;
        if (!super.equals(obj))
            return false;
        if (getClass() != obj.getClass())
            return false;
        RelativeStructuredFieldPosition other = (RelativeStructuredFieldPosition) obj;
        if (relativePosition != other.relativePosition)
            return false;
        return true;
    }
    
    
}
