/*
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
 *
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
 */
package org.alfresco.solr;

import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;

import static org.junit.Assert.*;

/**
 * Tests HandlerOfResources
 */
public class HandlerOfResourcesTest {


    @Test
    public void allowedPropertiesTest() throws Exception {
        assertTrue(HandlerOfResources.allowedProperties(null,null));
        assertTrue(HandlerOfResources.allowedProperties(new Properties(),null));
        assertTrue(HandlerOfResources.allowedProperties(new Properties(), new ArrayList<String>()));

        Properties props = new Properties();
        props.setProperty("king", "kong");
        props.setProperty("barbie", "doll");
        assertFalse(HandlerOfResources.allowedProperties(props, Arrays.asList("bar")));
        assertTrue( HandlerOfResources.allowedProperties(props, Arrays.asList("bark")));

        assertTrue(HandlerOfResources.allowedProperties(props, HandlerOfResources.DISALLOWED_SHARED_UPDATES));

        props.setProperty("solr.host", "me");
        props.setProperty("solr.port", "233");
        assertTrue(HandlerOfResources.allowedProperties(props, HandlerOfResources.DISALLOWED_SHARED_UPDATES));

        props.setProperty("alfresco.identifier.property.0", "xy");
        assertFalse(HandlerOfResources.allowedProperties(props, HandlerOfResources.DISALLOWED_SHARED_UPDATES));
        props.remove("alfresco.identifier.property.0");

        props.setProperty("alfresco.suggestable.property.1", "xy");
        assertFalse(HandlerOfResources.allowedProperties(props, HandlerOfResources.DISALLOWED_SHARED_UPDATES));
        props.remove("alfresco.suggestable.property.1");

        props.setProperty("alfresco.cross.locale.property.0", "xy");
        assertFalse(HandlerOfResources.allowedProperties(props, HandlerOfResources.DISALLOWED_SHARED_UPDATES));
        props.remove("alfresco.cross.locale.property.0");

        props.setProperty("alfresco.cross.locale.datatype.2", "xy");
        assertFalse(HandlerOfResources.allowedProperties(props, HandlerOfResources.DISALLOWED_SHARED_UPDATES));
        props.remove("alfresco.cross.locale.datatype.2");
    }

}