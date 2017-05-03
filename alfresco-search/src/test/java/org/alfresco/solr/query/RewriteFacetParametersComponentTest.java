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

package org.alfresco.solr.query;


import org.alfresco.solr.component.RewriteFacetParametersComponent;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Assert;
import org.junit.Test;



@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class RewriteFacetParametersComponentTest 
{


    /**
     * Search-409, mange parsing of facet fields.
     * RewriteFacetParametersComponent incorrectly splits fields when a comma
     * is present within {}. Below example uses {max=100, percentiles=90,99}
     */
    @Test
    public void parseFacetField()
    {
        String a = "created,modified";
        String b = "cm:created,modified";
        String c = "modified";
        String d = "{crazy}created,modified,updated";
        String e = "{bob:\"fred\"}created,modified,updated";
        String f = "{perc=\"3,4,5\"}created,modified";
        String g = "{perc='3,4,5'}created,modified";
        
        Assert.assertEquals(2, RewriteFacetParametersComponent.parseFacetField(a).length);
        Assert.assertEquals(2, RewriteFacetParametersComponent.parseFacetField(b).length);
        Assert.assertEquals(1, RewriteFacetParametersComponent.parseFacetField(c).length);
        Assert.assertEquals(3, RewriteFacetParametersComponent.parseFacetField(d).length);
        Assert.assertEquals(3, RewriteFacetParametersComponent.parseFacetField(e).length);
        Assert.assertEquals(2, RewriteFacetParametersComponent.parseFacetField(f).length);
        Assert.assertEquals(2, RewriteFacetParametersComponent.parseFacetField(g).length);
    }
    @Test(expected=RuntimeException.class)
    public void parseEmpty()
    {
         RewriteFacetParametersComponent.parseFacetField("");
    }
    @Test(expected=RuntimeException.class)
    public void parseNull()
    {
        RewriteFacetParametersComponent.parseFacetField(null);
    }

}