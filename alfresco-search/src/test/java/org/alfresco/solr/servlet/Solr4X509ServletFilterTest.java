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

package org.alfresco.solr.servlet;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import java.util.NoSuchElementException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

/**
 * Test cases for {@link Solr4X509ServletFilter}.
 */
public class Solr4X509ServletFilterTest
{

    public interface TestConnectorMBean
    {

        String getScheme();

        void setScheme(String scheme);

        int getPort();

        void setPort(int port);
    }

    private class TestConnector implements TestConnectorMBean
    {

        private int port;
        private String scheme;

        TestConnector(final int port, final String scheme)
        {
            this.port = port;
            this.scheme = scheme;
        }

        @Override
        public String getScheme()
        {
            return scheme;
        }

        @Override
        public void setScheme(final String scheme)
        {
            this.scheme = scheme;
        }

        @Override
        public int getPort()
        {
            return port;
        }

        @Override
        public void setPort(final int port)
        {
            this.port = port;
        }
    }

    private Solr4X509ServletFilter cut;
    private MBeanServer mxServer;

    @Before
    public void setUp()
    {
        cut = new Solr4X509ServletFilter();
        try
        {
            mxServer = cut.mxServer();
        }
        catch (final NoSuchElementException exception)
        {
            mxServer = MBeanServerFactory.createMBeanServer("A_DOMAIN");
        }
    }

    @After
    public void tearDown()
    {
        try
        {
            cut.mxServer().unregisterMBean(cut.connectorMBeanName().get());
        }
        catch (final Exception exception) {
            // Ignore
        }
    }

    @Test(expected = NoSuchElementException.class)
    public void mxServerNotFound()
    {
        MBeanServerFactory.releaseMBeanServer(mxServer);
        cut.mxServer();
    }

    @Test
    public void noAvailableConnectors()
    {
        assertFalse(cut.connectorMBeanName().isPresent());
        assertEquals(Solr4X509ServletFilter.NOT_FOUND_HTTPS_PORT_NUMBER, cut.getHttpsPort());
    }

    @Test
    public void oneAvailableHttpsConnector() throws Exception
    {
        final int expectedPort = 8443;

        registerConnector("https", expectedPort);

        assertEquals(expectedPort, cut.getHttpsPort());
    }

    @Test
    public void httpAndHttpsConnectors() throws Exception
    {
        final int expectedPort = 8443;

        registerConnector("http", 80);
        registerConnector("https", expectedPort);

        assertEquals(expectedPort, cut.getHttpsPort());
    }

    @Test
    public void cannotGetHttpsPort() throws Exception
    {
        registerTouchyHttpsConnector();

        assertEquals(Solr4X509ServletFilter.NOT_FOUND_HTTPS_PORT_NUMBER, cut.getHttpsPort());
    }

    @Test
    public void onlyHttpConnector() throws Exception
    {
        registerConnector("http", 8080);

        assertFalse(cut.connectorMBeanName().isPresent());
        assertEquals(Solr4X509ServletFilter.NOT_FOUND_HTTPS_PORT_NUMBER, cut.getHttpsPort());
    }

    /**
     * Registers a Connector MBean with the given scheme and port.
     *
     * @param scheme the scheme.
     * @param port the port number.
     * @throws Exception hopefully never, otherwise the test fails.
     */
    private void registerConnector(final String scheme, final int port) throws Exception
    {
        final ObjectName name = new ObjectName("A_DOMAIN", "created",  String.valueOf(System.nanoTime()));
        cut.mxServer().registerMBean(new TestConnector(port, scheme), name);
    }

    /**
     * Registers a HTTPS Connector MBean which throws an exception when the getPort method is called.
     *
     * @throws Exception hopefully never, otherwise the test fails.
     */
    private void registerTouchyHttpsConnector() throws Exception
    {
        final ObjectName name = new ObjectName("A_DOMAIN", "created",  String.valueOf(System.nanoTime()));
        cut.mxServer().registerMBean(
                new TestConnector(0, "https")
                {
                    @Override
                    public int getPort()
                    {
                        throw new RuntimeException("Don't ask me the port number!");
                    }
                }, name);
    }
}
