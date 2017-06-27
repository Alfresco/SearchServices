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
package org.alfresco.solr.tracker.pool;

import static org.junit.Assert.assertEquals;

import java.util.Properties;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests for the {@link DefaultTrackerPoolFactory}.
 * 
 * @author Matt Ward
 */
public class DefaultTrackerPoolFactoryTest
{
    private DefaultTrackerPoolFactory poolFactory;
    private Properties properties;
    ThreadPoolExecutor tpe;
    @Before
    public void setup()
    {
        poolFactory = null; // Ensure we don't accidentally reuse between runs.
        properties = new Properties();
    }
    @After
    public void teardown()
    {
        tpe.shutdownNow();
        tpe = null;
    }
    
    @Test
    public void testDefaults()
    {
        poolFactory = new DefaultTrackerPoolFactory(properties, "TheCore", "TrackerName");
        
        tpe = poolFactory.create();
        
        assertEquals(3, tpe.getCorePoolSize());
        assertEquals(3, tpe.getMaximumPoolSize());
        assertEquals(120, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }
    
    @Test
    public void testNonDefaultProperties()
    {
        properties.put("alfresco.corePoolSize", "30");
        properties.put("alfresco.maximumPoolSize", "40");
        properties.put("alfresco.keepAliveTime", "200");
        
        poolFactory = new DefaultTrackerPoolFactory(properties, "TheCore", "TrackerName");
        
        tpe = poolFactory.create();
        
        assertEquals(30, tpe.getCorePoolSize());
        assertEquals(40, tpe.getMaximumPoolSize());
        assertEquals(200, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }
    @Test
    public void testAclDefaultProperties()
    {
        poolFactory = new DefaultTrackerPoolFactory(properties, "TheCore", "AclTracker");
        
        tpe = poolFactory.create();
        
        assertEquals(4, tpe.getCorePoolSize());
        assertEquals(10, tpe.getMaximumPoolSize());
        assertEquals(120, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }
    @Test
    public void testAclProperties()
    {
        properties.put("alfresco.acl.tracker.corePoolSize", "30");
        properties.put("alfresco.acl.tracker.maximumPoolSize", "40");
        properties.put("alfresco.acl.tracker.keepAliveTime", "200");
        poolFactory = new DefaultTrackerPoolFactory(properties, "TheCore", "AclTracker");
        tpe = poolFactory.create();
        
        assertEquals(30, tpe.getCorePoolSize());
        assertEquals(40, tpe.getMaximumPoolSize());
        assertEquals(200, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }
    @Test
    public void testContentDefaultProperties()
    {
        poolFactory = new DefaultTrackerPoolFactory(properties, "TheCore", "ContentTracker");
        
        tpe = poolFactory.create();
        
        assertEquals(12, tpe.getCorePoolSize());
        assertEquals(12, tpe.getMaximumPoolSize());
        assertEquals(120, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }
    @Test
    public void testContentProperties()
    {
        properties.put("alfresco.content.tracker.corePoolSize", "100");
        properties.put("alfresco.content.tracker.maximumPoolSize", "140");
        properties.put("alfresco.content.tracker.keepAliveTime", "201");
        poolFactory = new DefaultTrackerPoolFactory(properties, "TheCore", "ContentTracker");
        tpe = poolFactory.create();
        
        assertEquals(100, tpe.getCorePoolSize());
        assertEquals(140, tpe.getMaximumPoolSize());
        assertEquals(201, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }
    @Test
    public void testMetaDataDefaultProperties()
    {
        poolFactory = new DefaultTrackerPoolFactory(properties, "TheCore", "MetadataTracker");
        
        tpe = poolFactory.create();
        
        assertEquals(5, tpe.getCorePoolSize());
        assertEquals(5, tpe.getMaximumPoolSize());
        assertEquals(120, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }
    @Test
    public void testMetaDataProperties()
    {
        properties.put("alfresco.metadata.tracker.corePoolSize", "100");
        properties.put("alfresco.metadata.tracker.maximumPoolSize", "140");
        properties.put("alfresco.metadata.tracker.keepAliveTime", "201");
        poolFactory = new DefaultTrackerPoolFactory(properties, "TheCore", "MetadataTracker");
        
        tpe = poolFactory.create();
        
        assertEquals(100, tpe.getCorePoolSize());
        assertEquals(140, tpe.getMaximumPoolSize());
        assertEquals(201, tpe.getKeepAliveTime(TimeUnit.SECONDS));
    }
}
