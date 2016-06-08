/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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
package org.alfresco.solr.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.naming.NoInitialContextException;

/**
 * Helps with configuring and setup of Alfresco and Solr.
 *
 * @author Gethin James
 */
public class ConfigUtil {

    protected final static Logger log = LoggerFactory.getLogger(ConfigUtil.class);

    private static final String JNDI_PREFIX = "java:comp/env/";
    /**
     * Finds the property based on looking up the value in one of three places (in order of preference):
     * <ol>
     *  <li>JNDI: via java:comp/env/{propertyName/converted/to/slash}</li>
     *  <li>A Java system property or a Java system property prefixed with solr.</li>
     *  <li>OS environment variable</li>
     * </ol>
     *
     * @return A property
     */
    public static String locateProperty(String propertyName, String defaultValue)
    {

        String propertyValue = null;
        String propertyKey = propertyName.toLowerCase();
        String jndiKey =  convertPropertyNameToJNDIPath(propertyKey);
        String envVar = convertPropertyNameToEnvironmentParam(propertyKey);

        // Try JNDI
        try {
            Context c = new InitialContext();
            propertyValue = (String) c.lookup(jndiKey);
            log.info("Using JNDI key: "+jndiKey+": "+propertyValue );
            return propertyValue;
        } catch (NoInitialContextException e) {
            log.info("JNDI not configured (NoInitialContextEx)");
        } catch (NamingException e) {
            log.info("No "+jndiKey+" in JNDI");
        } catch( RuntimeException ex ) {
            log.warn("Odd RuntimeException while testing for JNDI: " + ex.getMessage());
        }

        // Now try system property
        propertyValue = System.getProperty(propertyKey);
        if( propertyValue != null ) {
            log.info("Using system property "+propertyKey+": " + propertyValue );
            return propertyValue;
        }

        //try system property again with a solr. prefix
        propertyValue = System.getProperty("solr."+propertyKey);
        if( propertyValue != null ) {
            log.info("Using system property "+"solr."+propertyKey+": " + propertyValue );
            return propertyValue;
        }

        // Now try an environment variable
        propertyValue = System.getenv(envVar);
        if( propertyValue != null ) {
            log.info("Using environment variable "+envVar+": " + propertyValue );
            return propertyValue;
        }

        //if all else fails then return the default
        return defaultValue;
    }

    /**
     * Takes a property name and splits it via / instead of .
     * @param propertyName
     * @return the property name as a jndi path
     */
    protected static String convertPropertyNameToJNDIPath(String propertyName)
    {
        if (propertyName == null) propertyName = "";
        return JNDI_PREFIX+propertyName.replace('.','/');
    }

    protected static String convertPropertyNameToEnvironmentParam(String propertyName)
    {
        if (propertyName == null) propertyName = "";
        return propertyName.replace('.','_').toUpperCase();
    }
}
