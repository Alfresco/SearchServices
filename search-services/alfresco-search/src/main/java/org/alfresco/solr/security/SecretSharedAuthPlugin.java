/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

package org.alfresco.solr.security;

import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import javax.servlet.FilterChain;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.solr.security.AuthenticationPlugin;

/**
 * SOLR Authentication Plugin based in shared secret token via request header.
 * 
 * This Web Filter is loaded from SOLR_HOME/security.json file, so it will be executed 
 * for every request to SOLR. Authentication logic is only applied when Alfresco Communication
 * is using "secret" method but it doesn't apply to "none" and "https" methods.
 *
 */
public class SecretSharedAuthPlugin extends AuthenticationPlugin
{

    /**
     * Verify that request header includes "secret" word when using "secret" communication method.
     * "alfresco.secureComms.secret" value is expected as Java environment variable.
     */
    @Override
    public boolean doAuthenticate(ServletRequest request, ServletResponse response, FilterChain chain) throws Exception
    {

        if (SecretSharedPropertyCollector.isCommsSecretShared())
        {

            HttpServletRequest httpRequest = (HttpServletRequest) request;

            if (Objects.equals(httpRequest.getHeader(SecretSharedPropertyCollector.getSecretHeader()),
                        SecretSharedPropertyCollector.getSecret()))
            {
                chain.doFilter(request, response);
                return true;
            }

            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN,
                        "Authentication failure: \"" + SecretSharedPropertyCollector.SECRET_SHARED_METHOD_KEY
                                    + "\" method has been selected, use the right request header with the secret word");
            return false;
        }

        chain.doFilter(request, response);
        return true;

    }

    @Override
    public void init(Map<String, Object> parameters)
    {
    }

    @Override
    public void close() throws IOException
    {
    }

}
