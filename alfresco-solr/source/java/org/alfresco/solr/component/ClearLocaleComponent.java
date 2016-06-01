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
package org.alfresco.solr.component;

import java.io.IOException;

import org.apache.solr.handler.component.ResponseBuilder;
import org.apache.solr.handler.component.SearchComponent;
import org.springframework.extensions.surf.util.I18NUtil;

/**
 * @author Andy
 *
 */
public class ClearLocaleComponent extends SearchComponent
{

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#prepare(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void prepare(ResponseBuilder rb) throws IOException
    {
        // Nothing to do
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#process(org.apache.solr.handler.component.ResponseBuilder)
     */
    @Override
    public void process(ResponseBuilder rb) throws IOException
    {
        I18NUtil.setLocale(null);
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getDescription()
     */
    @Override
    public String getDescription()
    {
        return "clearLocale";
    }

    /* (non-Javadoc)
     * @see org.apache.solr.handler.component.SearchComponent#getSource()
     */
    @Override
    public String getSource()
    {
        return "";
    }

}
