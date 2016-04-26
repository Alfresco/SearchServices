package org.alfresco.solr.client;

import java.util.Locale;

/**
 * Represents a content property value, including locale, length, content id, encoding, mime type
 * 
 * @since 4.0
 */
public class ContentPropertyValue extends PropertyValue
{
    private Locale locale;
    private long length;
    private String encoding;
    private String mimetype;
    private Long id;
    
    public ContentPropertyValue(Locale locale, long length, String encoding, String mimetype, Long id)
    {
        super();
        this.locale = locale;
        this.length = length;
        this.encoding = encoding;
        this.mimetype = mimetype;
        this.id = id;
    }

    public String getEncoding()
    {
        return encoding;
    }

    public String getMimetype()
    {
        return mimetype;
    }

    public Locale getLocale()
    {
        return locale;
    }

    public long getLength()
    {
        return length;
    }

    public Long getId()
    {
        return id;
    }
    
    @Override
    public String toString()
    {
        return "ContentPropertyValue [locale=" + locale + ", length=" + length + ", encoding="
                + encoding + ", mimetype=" + mimetype + ", id="+id+"]";
    }
}
