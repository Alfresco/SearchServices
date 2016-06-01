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
package org.alfresco.solr.content;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.runners.MockitoJUnitRunner;

/**
 * Tests {@link SolrContentUrlBuilder}
 * 
 * @author Derek Hulley
 * @since 5.0
 */
@RunWith(MockitoJUnitRunner.class)
public class SolrContentUrlBuilderTest
{
    @Test
    public void miscellaneousMetadata()
    {
        String url1 = SolrContentUrlBuilder
                .start()
                .add("AAA", "AAA-VALUE")
                .add("BBB", "BBB-VALUE")
                .get();
        String url1Check = SolrContentUrlBuilder
                .start()
                .add("BBB", "BBB-VALUE")
                .add("AAA", "AAA-VALUE")
                .get();
        Assert.assertEquals(url1, url1Check);
        
        Assert.assertTrue("Incorrect URL: " + url1, url1.startsWith("solr://default/misc/"));
        Assert.assertTrue("Incorrect URL: " + url1, url1.endsWith(".gz"));
    }

    @Test
    public void tenantOnly()
    {
        String url1 = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_TENANT, "bob")
                .get();
        String url1Check = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_TENANT, "bob")
                .get();
        Assert.assertEquals(url1, url1Check);
        String url2 = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_TENANT, "jake")
                .get();
        Assert.assertNotEquals(url1, url2);
        
        Assert.assertTrue("Incorrect URL: " + url1, url1.startsWith("solr://bob/misc/"));
        Assert.assertTrue("File extension not present.", url1.endsWith(".gz"));
    }

    @Test
    public void noTenant()
    {
        String url1 = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_DB_ID, "12345")
                .get();
        String url1Check = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_DB_ID, "12345")
                .get();
        Assert.assertEquals(url1, url1Check);
        String url2 = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_DB_ID, "123456")
                .get();
        Assert.assertNotEquals(url1, url2);
        
        Assert.assertEquals("Incorrect URL: " + url1, "solr://default/db/1234/5.gz", url1);
    }

    @Test
    public void dbId()
    {
        String url1 = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_TENANT, "bob")
                .add(SolrContentUrlBuilder.KEY_DB_ID, "123456")
                .get();
        String url1Check = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_DB_ID, "123456")
                .add(SolrContentUrlBuilder.KEY_TENANT, "bob")
                .get();
        Assert.assertEquals(url1, url1Check);
        
        Assert.assertEquals("Incorrect URL: " + url1, "solr://bob/db/1234/56.gz", url1);
    }

    @Test
    public void aclId()
    {
        String url1 = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_TENANT, "bob")
                .add(SolrContentUrlBuilder.KEY_ACL_ID, "123456")
                .get();
        String url1Check = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_ACL_ID, "123456")
                .add(SolrContentUrlBuilder.KEY_TENANT, "bob")
                .get();
        Assert.assertEquals(url1, url1Check);
        
        Assert.assertEquals("Incorrect URL: " + url1, "solr://bob/acl/1234/56.gz", url1);
    }

    @Test
    public void aclIdAndDbId()
    {
        String url1 = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_TENANT, "bob")
                .add(SolrContentUrlBuilder.KEY_ACL_ID, "12345")
                .add(SolrContentUrlBuilder.KEY_DB_ID, "54321")
                .get();
        String url1Check = SolrContentUrlBuilder
                .start()
                .add(SolrContentUrlBuilder.KEY_DB_ID, "54321")
                .add(SolrContentUrlBuilder.KEY_ACL_ID, "12345")
                .add(SolrContentUrlBuilder.KEY_TENANT, "bob")
                .get();
        Assert.assertEquals(url1, url1Check);
        
        Assert.assertEquals("Incorrect URL: " + url1, "solr://bob/db/5432/1.gz", url1);
    }

    @Test
    public void failures()
    {
        try
        {
            SolrContentUrlBuilder
                    .start()
                    .get();
            Assert.fail("Did not detect empty metadata.");
        }
        catch (IllegalStateException e)
        {
            // Expected
        }
        try
        {
            SolrContentUrlBuilder
                    .start()
                    .add(null, "oops")
                    .get();
            Assert.fail("Did not detect null key.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        try
        {
            SolrContentUrlBuilder
                    .start()
                    .add(SolrContentUrlBuilder.KEY_TENANT, "")
                    .get();
            Assert.fail("Did not detect empty known value.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        try
        {
            SolrContentUrlBuilder
                    .start()
                    .add(SolrContentUrlBuilder.KEY_DB_ID, "")
                    .get();
            Assert.fail("Did not detect empty known value.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
        try
        {
            SolrContentUrlBuilder
                    .start()
                    .add(SolrContentUrlBuilder.KEY_ACL_ID, "")
                    .get();
            Assert.fail("Did not detect empty known value.");
        }
        catch (IllegalArgumentException e)
        {
            // Expected
        }
    }
}
