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

package org.alfresco.solr;

import org.alfresco.solr.io.interceptor.SharedSecretRequestInterceptor;
import org.alfresco.solr.security.SecretSharedPropertyCollector;
import org.apache.http.HttpRequestInterceptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InterceptorRegistry
{
        protected static final Logger LOGGER = LoggerFactory.getLogger(InterceptorRegistry.class);
        /**
         * Register the required {@link HttpRequestInterceptor}s
         */
        public static void registerSolrClientInterceptors()
        {
                try
                {
                        if (SecretSharedPropertyCollector.isCommsSecretShared())
                        {
                                SharedSecretRequestInterceptor.register();
                        }
                }
                catch (Throwable t)
                {
                        LOGGER.warn("It was not possible to add the Shared Secret Authentication interceptor. "
                                + "Please make sure to pass the required -Dalfresco.secureComms=secret and "
                                + "-Dalfresco.secureComms.secret=my-secret-value JVM args if trying to use Secret Authentication with Solr.");
                }
        }
}
