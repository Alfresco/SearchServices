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

package org.alfresco.solr.io.interceptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import java.util.stream.IntStream;

import org.alfresco.httpclient.HttpClientFactory;
import org.apache.http.Header;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.impl.client.SystemDefaultHttpClient;
import org.apache.http.message.BasicHttpRequest;
import org.apache.solr.client.solrj.impl.HttpClientUtil;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit tests for {@link SharedSecretRequestInterceptor}.
 */
public class SharedSecretRequestInterceptorTest
{

    private static final String SECRET_HEADER_PROPERTY = "alfresco.secureComms.secret.header";
    private static final String SECRET_HEADER_VALUE = "X-My-Secret-Header";
    private static final String SECRET_PROPERTY = "alfresco.secureComms.secret";
    private static final String SECRET_VALUE = "my-secret";

    @Before
    public void setUp()
    {
        System.clearProperty(SECRET_HEADER_PROPERTY);
        System.clearProperty(SECRET_PROPERTY);
    }

    @Test
    public void theInterceptor_shouldBeSingleton()
    {
        SharedSecretRequestInterceptor interceptor1 = SharedSecretRequestInterceptor.getInstance();
        SharedSecretRequestInterceptor interceptor2 = SharedSecretRequestInterceptor.getInstance();

        assertSame("There should only be one instance of the interceptor.", interceptor1, interceptor2);
    }

    @Test
    public void registeringTheInterceptor_shouldAddOneInterceptor()
    {
        SharedSecretRequestInterceptor.register();

        SystemDefaultHttpClient client = (SystemDefaultHttpClient) HttpClientUtil.createClient(null);
        long sharedSecretInterceptorsCount = getSharedSecretInterceptorsCount(client);

        assertEquals("There should be one Shared Secret request interceptor.", 1, sharedSecretInterceptorsCount);
    }

    @Test
    public void registeringTheInterceptorMultipleTimes_shouldAddOnlyOneInterceptor()
    {
        IntStream.range(0, 5).forEach(i -> SharedSecretRequestInterceptor.register());

        SystemDefaultHttpClient client = (SystemDefaultHttpClient) HttpClientUtil.createClient(null);
        long sharedSecretInterceptorsCount = getSharedSecretInterceptorsCount(client);

        assertEquals("There should be only one Shared Secret request interceptor.", 1, sharedSecretInterceptorsCount);
    }

    @Test
    public void requestProcessing_shouldAddDefaultSecretHeaderToOutgoingRequests() throws Exception
    {
        System.setProperty(SECRET_PROPERTY, SECRET_VALUE);
        BasicHttpRequest httpRequest = new BasicHttpRequest("", "");

        SharedSecretRequestInterceptor.getInstance().process(httpRequest, null);
        Header[] headers = httpRequest.getHeaders(HttpClientFactory.DEFAULT_SHAREDSECRET_HEADER);

        assertEquals("There should be only one secret header.", 1, headers.length);
        assertEquals("The secret header should have the expected value.", SECRET_VALUE, headers[0].getValue());
    }

    @Test
    public void requestProcessing_shouldAddCustomSecretHeaderToOutgoingRequests() throws Exception
    {
        System.setProperty(SECRET_HEADER_PROPERTY, SECRET_HEADER_VALUE);
        System.setProperty(SECRET_PROPERTY, SECRET_VALUE);
        BasicHttpRequest httpRequest = new BasicHttpRequest("", "");

        SharedSecretRequestInterceptor.getInstance().process(httpRequest, null);
        Header[] headers = httpRequest.getHeaders(SECRET_HEADER_VALUE);

        assertEquals("There should be only one secret header.", 1, headers.length);
        assertEquals("The secret header should have the expected value.", SECRET_VALUE, headers[0].getValue());
    }

    @Test(expected = RuntimeException.class)
    public void requestProcessing_shouldFailWhenMissingSecretValue() throws Exception
    {
        BasicHttpRequest httpRequest = new BasicHttpRequest("", "");

        SharedSecretRequestInterceptor.getInstance().process(httpRequest, null);
    }

    private static long getSharedSecretInterceptorsCount(SystemDefaultHttpClient client)
    {
        return IntStream.range(0, client.getRequestInterceptorCount())
            .mapToObj(client::getRequestInterceptor)
            .map(HttpRequestInterceptor::getClass)
            .filter(clazz -> clazz == SharedSecretRequestInterceptor.class)
            .count();
    }

}