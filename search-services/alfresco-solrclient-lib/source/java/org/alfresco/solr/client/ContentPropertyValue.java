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
