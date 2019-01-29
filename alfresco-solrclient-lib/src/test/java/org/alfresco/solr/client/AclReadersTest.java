/*
 * #%L
 * Alfresco Solr Client
 * %%
 * Copyright (C) 2005 - 2016 Alfresco Software Limited
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
package org.alfresco.solr.client;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/**
 * Some simple sanity tests for the {@link AclReaders} class.
 * 
 * @author Matt Ward
 */
public class AclReadersTest
{    
    @Test
    public void testHashCode()
    {
        // We only care about ID for equals() and hashCode()
        
        // The same ID
        assertEquals(new AclReaders(123, null, null, 0, null).hashCode(),
                     new AclReaders(123, null, null, 0, null).hashCode());
        
        // Different ID
        assertNotEquals(new AclReaders(123, null, null, 0, null).hashCode(),
                        new AclReaders(124, null, null, 0, null).hashCode());
    }

    @Test
    public void testEqualsObject()
    {
        // The very same
        final AclReaders aclReaders = new AclReaders(0, null, null, 0, null);
        assertTrue(aclReaders.equals(aclReaders));
        
        // The same ID
        assertEquals(new AclReaders(123, null, null, 0, null),
                        new AclReaders(123, null, null, 0, null));
        
        // Different ID
        assertNotEquals(new AclReaders(123, null, null, 0, null),
                        new AclReaders(124, null, null, 0, null));
    }

    @Test
    public void testGetReaders()
    {
        AclReaders aclReaders = new AclReaders(0, null, asList("d1", "d2"), 0, null);
        assertEquals(asList("d1", "d2"), aclReaders.getDenied());
    }

    @Test
    public void testGetDenied()
    {
        AclReaders aclReaders = new AclReaders(0, asList("r1", "r2", "r3"), null, 0, null);
        assertEquals(asList("r1", "r2", "r3"), aclReaders.getReaders());
    }
}
