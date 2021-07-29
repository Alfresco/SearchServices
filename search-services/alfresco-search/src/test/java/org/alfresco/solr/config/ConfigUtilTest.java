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

package org.alfresco.solr.config;

import java.io.File;
import java.util.HashMap;
import java.util.Properties;

import org.alfresco.solr.AlfrescoCoreAdminHandler;
import org.alfresco.solr.SolrInformationServer;
import org.apache.solr.core.CoreDescriptor;
import org.apache.solr.core.CoreDescriptorDecorator;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Tests configuring and setup of Alfresco and Solr properties
 *
 * @author Gethin James
 */

public class ConfigUtilTest {

    @Test
    public void locateProperty() throws Exception {
        assertEquals("king", ConfigUtil.locateProperty("find.me", "king"));
        System.setProperty("solr.find.me", "iamfound");
        assertEquals("iamfound", ConfigUtil.locateProperty("find.me", "king"));
        System.clearProperty("solr.find.me");
        assertEquals("king", ConfigUtil.locateProperty("find.me", "king"));
        System.setProperty("find.me", "iamfoundagain");
        assertEquals("iamfoundagain", ConfigUtil.locateProperty("find.me", "king"));
        System.clearProperty("find.me");
    }

    @Test
    public void convertPropertyNameToJNDIPath() throws Exception {
        assertEquals("java:comp/env/gethin",ConfigUtil.convertPropertyNameToJNDIPath("gethin"));
        assertEquals("java:comp/env/solr/content/dir",ConfigUtil.convertPropertyNameToJNDIPath("solr.content.dir"));
        assertEquals("java:comp/env/solr/model/dir",ConfigUtil.convertPropertyNameToJNDIPath("solr.model.dir"));
        assertEquals("java:comp/env/",ConfigUtil.convertPropertyNameToJNDIPath(""));
        assertEquals("java:comp/env/",ConfigUtil.convertPropertyNameToJNDIPath(null));
    }

    @Test
    public void convertPropertyNameToEnvironmentParam() throws Exception {
        assertEquals("SOLR_GETHIN",ConfigUtil.convertPropertyNameToEnvironmentParam("gethin"));
        assertEquals("SOLR_SOLR_CONTENT_DIR",ConfigUtil.convertPropertyNameToEnvironmentParam("solr.content.dir"));
        assertEquals("SOLR_SOLR_MODEL_DIR",ConfigUtil.convertPropertyNameToEnvironmentParam("solr.model.dir"));
        assertEquals("SOLR_CREATE_ALFRESCO_DEFAULTS",ConfigUtil.convertPropertyNameToEnvironmentParam(AlfrescoCoreAdminHandler.ALFRESCO_DEFAULTS));
        assertEquals("SOLR_SOLR_HOST",ConfigUtil.convertPropertyNameToEnvironmentParam(SolrInformationServer.SOLR_HOST));
        assertEquals("SOLR_SOLR_PORT",ConfigUtil.convertPropertyNameToEnvironmentParam(SolrInformationServer.SOLR_PORT));
        assertEquals("SOLR_SOLR_BASEURL",ConfigUtil.convertPropertyNameToEnvironmentParam(SolrInformationServer.SOLR_BASEURL));
    }
    
    /**
     * See https://github.com/Alfresco/SearchServices/pull/382
     */
    @Test
    public void testMissingCoreProperty()
    {
        HashMap<String, String> coreProps = new HashMap<String, String>();
        Properties saved = (Properties) System.getProperties().clone();
        System.setProperty("alfresco.secureComms", "https");
        for (String key : CoreDescriptorDecorator.SUBSTITUTABLE_PROPERTIES_SECURE)
        {
            // Intentionally omit store provider settings
            if (!key.endsWith("store.provider"))
            {
                // Set store type as system property
                if (key.endsWith("store.type"))
                {
                    System.setProperty(key, "JKS");
                }
                coreProps.put(key, "irrelevant");
            }
        }
        Properties contProps = new Properties();
        File tmp = new File("/tmp");
        CoreDescriptor coreDesc = new CoreDescriptor("test", tmp.toPath(), coreProps, contProps, false);
        CoreDescriptorDecorator decorator = new CoreDescriptorDecorator(coreDesc);
        Properties decProps = decorator.getProperties();
        // Verify store types are in system property
        assertEquals("JKS", ConfigUtil.locateProperty("alfresco.encryption.ssl.keystore.type", null));
        assertEquals("JKS", ConfigUtil.locateProperty("alfresco.encryption.ssl.truststore.type", null));
        // Verify store types in CoreDescriptor came from system property
        assertEquals("JKS", decProps.get("alfresco.encryption.ssl.keystore.type"));
        assertEquals("JKS", decProps.get("alfresco.encryption.ssl.truststore.type"));
        System.setProperties(saved);
    }

}