/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2022 Alfresco Software Limited
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

package org.alfresco.solr.io.interceptor;

import java.io.IOException;

import org.alfresco.solr.security.SecretSharedPropertyCollector;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.message.BasicHeader;
import org.apache.http.protocol.HttpContext;
import org.apache.solr.client.solrj.impl.HttpClientUtil;

/**
 * This HttpRequestInterceptor adds the header that is required for Shared Secret Authentication with Solr
 *
 * @author Domenico Sibilio
 */
public class SharedSecretRequestInterceptor implements HttpRequestInterceptor
{

    private static volatile SharedSecretRequestInterceptor INSTANCE;

    private SharedSecretRequestInterceptor()
    {
    }

    /**
     * A typical thread-safe singleton implementation
     * @return The unique instance of this class
     */
    public static SharedSecretRequestInterceptor getInstance()
    {
        if (INSTANCE == null)
        {
            synchronized (SharedSecretRequestInterceptor.class)
            {
                if (INSTANCE == null)
                {
                    INSTANCE = new SharedSecretRequestInterceptor();
                }
            }
        }

        return INSTANCE;
    }

    /**
     * Decorates the enclosing request with the Shared Secret Authentication header
     * @param httpRequest
     * @param httpContext
     * @throws HttpException
     * @throws IOException
     */
    @Override
    public void process(HttpRequest httpRequest, HttpContext httpContext)
        throws HttpException, IOException
    {
        String secretName = SecretSharedPropertyCollector.getSecretHeader();
        String secretValue = SecretSharedPropertyCollector.getSecret();
        httpRequest.addHeader(new BasicHeader(secretName, secretValue));
    }

    /**
     * Utility method to register the unique instance of this {@link HttpRequestInterceptor}
     */
    public static void register()
    {
        HttpClientUtil.removeRequestInterceptor(getInstance());
        HttpClientUtil.addRequestInterceptor(getInstance());
    }

}