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
