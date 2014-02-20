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
