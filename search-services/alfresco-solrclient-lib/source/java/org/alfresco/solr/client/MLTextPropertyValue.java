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

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Represents a multi-lingual property value, comprising a map from locale to string value
 * 
 * @since 4.0
 */
public class MLTextPropertyValue extends PropertyValue
{
    private Map<Locale, String> values;

    public MLTextPropertyValue()
    {
        super();
        values = new HashMap<Locale, String>(10);
    }
    
    public MLTextPropertyValue(Map<Locale, String> values)
    {
        super();
        this.values = values;
    }

    public void addValue(Locale locale, String value)
    {
        values.put(locale, value);
    }
    
    public Map<Locale, String> getValues()
    {
        return values;
    }
    
    public Set<Locale> getLocales()
    {
        return values.keySet();
    }
    
    public String getValue(Locale locale)
    {
        return values.get(locale);
    }

    @Override
    public String toString()
    {
        return "MLTextPropertyValue [values=" + values + "]";
    }
}
