package org.alfresco.solr.client;

/**
 * Represents a property value as a string
 * 
 * @since 4.0
 */
public class StringPropertyValue extends PropertyValue
{
    private String value;

    public StringPropertyValue(String value)
    {
        super();
        this.value = value;
    }

    public String getValue()
    {
        return value;
    }

    @Override
    public String toString()
    {
        return "StringPropertyValue [value=" + value + "]";
    }
}
