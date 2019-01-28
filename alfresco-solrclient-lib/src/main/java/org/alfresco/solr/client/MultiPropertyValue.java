/*
 * #%L
 * Alfresco Solr Client
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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
package org.alfresco.solr.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a multi property value, comprising a list of other property values
 * 
 * @since 4.0
 */
public class MultiPropertyValue extends PropertyValue
{
    private List<PropertyValue> values;

    public MultiPropertyValue()
    {
        super();
        this.values = new ArrayList<PropertyValue>(10);
    }
    
    public MultiPropertyValue(List<PropertyValue> values)
    {
        super();
        this.values = values;
    }

    public void addValue(PropertyValue value)
    {
        values.add(value);
    }
    
    public List<PropertyValue> getValues()
    {
        return values;
    }

    @Override
    public String toString()
    {
        return "MultiPropertyValue [values=" + values + "]";
    }
}
