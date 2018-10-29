/*
* Copyright (C) 2005-2013 Alfresco Software Limited.
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

import org.alfresco.web.scripts.servlet.X509ServletFilterBase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.apache.solr.core.SolrResourceLoader;

import javax.management.MBeanServer;
import javax.management.MBeanServerFactory;
import javax.management.ObjectName;
import javax.management.Query;
import javax.management.QueryExp;
import javax.servlet.ServletContext;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;

import static java.util.Optional.ofNullable;

/**
 * The Solr4X509ServletFilter implements the checkEnforce method of the X509ServletFilterBase.
 * This allows the configuration of X509 authentication to be toggled on/off through a
 * configuration outside of the web.xml.
 **/

public class Solr4X509ServletFilter extends X509ServletFilterBase
{

    private static final String SECURE_COMMS = "alfresco.secureComms";

    private static Log logger = LogFactory.getLog(Solr4X509ServletFilter.class);

    static final int NOT_FOUND_HTTPS_PORT_NUMBER = -1;

    private Function<ObjectName, Integer> extractPortNumber = name ->
    {
        try
        {
            return ofNullable(mxServer().getAttribute(name, "Port"))
                    .map(Number.class::cast)
                    .map(Number::intValue)
                    .orElse(NOT_FOUND_HTTPS_PORT_NUMBER);
        }
        catch(final Exception exception)
        {
            logger.error("Error getting https port from MBean " + name, exception);
            return NOT_FOUND_HTTPS_PORT_NUMBER;
        }
    };

    @Override
    protected boolean checkEnforce(ServletContext context) throws IOException
    {
        /*
        * Rely on the SolrResourceLoader to locate the solr home directory.
        */

        int httpsPort = getHttpsPort();

        if(httpsPort > -1)
        {
            setHttpsPort(httpsPort);
        }

        String solrHome = SolrResourceLoader.locateSolrHome().toString();

        if(logger.isDebugEnabled())
        {
            logger.debug("solrHome:"+solrHome);
        }

        /*
        * Find the active cores.
        */
        List<File> cores = new ArrayList<File>();
        findCores(new File(solrHome), cores);

        /*
        * Get the alfresco.secureComms value for each core.
        */
        Set<String> secureCommsSet = new HashSet<String>();
        for(File core : cores)
        {
            collectSecureComms(core, secureCommsSet);
        }

        /*
        * alfresco.secureComms values should be in sync for each core
        */

        if(secureCommsSet.size() > 1)
        {
            StringBuilder buf = new StringBuilder();
            int i = 0;
            for(String s : secureCommsSet)
            {
                if(i > 0)
                {
                    buf.append(" | ");
                }
                buf.append(s);
                i++;
            }

            throw new IOException("More then one distinct value found for alfresco.secureComms:"+ buf.toString()+
                                  ". All alfresco.secureComms values must be set to the same value.");
        }

        if(secureCommsSet.size() == 0)
        {
            //No secureComms were found.
            return false;
        }


        String secureComms = secureCommsSet.iterator().next();

        if(logger.isDebugEnabled())
        {
            logger.debug("secureComms:"+secureComms);
        }

        if("none".equals(secureComms))
        {
            return false;
        }
        else
        {
            return true;
        }
    }


    private void findCores(File dir, List<File> cores)
    {
        File[] files = dir.listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                if (file.isDirectory())
                {
                    findCores(file, cores);
                }
                else
                {
                    if ("core.properties".equals(file.getName()))
                    {
                        if (logger.isDebugEnabled())
                        {
                            logger.debug("Found core:" + dir.getAbsolutePath());
                        }
                        
                        cores.add(dir);
                    }
                }
            }
        }
    }
    /*
    * Scan through all the files and folders under a dir looking for solrcore.properties file.
    * Gather the SECURE_COMMS property when found.
    */

    private void collectSecureComms(File base, Set<String> secureCommsSet) throws IOException
    {
        File[] files = base.listFiles();
        if (files != null)
        {
            for (File file : files)
            {
                if (file.isDirectory())
                {
                    collectSecureComms(file, secureCommsSet);
                }
                else
                {
                    
                    if (logger.isDebugEnabled())
                    {
                        logger.debug("scanning file:" + file.getAbsolutePath());
                    }
                    
                    if ("solrcore.properties".equals(file.getName()))
                    {
                        FileReader propReader = null;
                        Properties props = new Properties();
                        try
                        {
                            propReader = new FileReader(file);
                            props.load(propReader);
                            String prop = props.getProperty(SECURE_COMMS);
                            
                            if (prop != null)
                            {
                                if (logger.isDebugEnabled())
                                {
                                    logger.debug("Found alfresco.secureComms in:" + file.getAbsolutePath() + " : " + prop);
                                }
                                secureCommsSet.add(prop);
                            }
                            else
                            {
                                secureCommsSet.add("none");
                            }
                        }
                        finally
                        {
                            if (propReader != null)
                            {
                                propReader.close();
                            }
                        }
                    }
                }
            }
        }
    }

    int getHttpsPort()
    {
        return connectorMBeanName().map(extractPortNumber).orElse(NOT_FOUND_HTTPS_PORT_NUMBER);
    }

    Optional<ObjectName> connectorMBeanName()
    {
        try
        {
            final QueryExp query = Query.eq(Query.attr("Scheme"), Query.value("https"));
            final Set<ObjectName> connectors = mxServer().queryNames(null, query);
            return connectors
                    .stream()
                    .findFirst();
        }
        catch (final Exception exception)
        {
            logger.error("Error getting the Connector MBean.", exception);
            return Optional.empty();
        }
    }

    MBeanServer mxServer()
    {
        return ofNullable((MBeanServerFactory.findMBeanServer(null)))
                    .filter(servers -> !servers.isEmpty())
                    .map(Collection::iterator)
                    .map(Iterator::next)
                    .orElseThrow(NoSuchElementException::new);
    }
}