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
