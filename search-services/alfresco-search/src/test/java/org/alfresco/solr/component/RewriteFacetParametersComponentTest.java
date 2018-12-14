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

package org.alfresco.solr.component;

import static java.util.Arrays.asList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import java.util.List;
import java.util.Map;

import com.carrotsearch.ant.tasks.junit4.dependencies.com.google.common.collect.ImmutableMap;
import com.sun.xml.xsom.impl.scd.Iterators;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.junit.Before;
import org.junit.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class RewriteFacetParametersComponentTest 
{
    @InjectMocks
    RewriteFacetParametersComponent rewriteFacetParametersComponent;
    @Mock
    SolrParams mockParams;

    @Before
    public void setUp()
    {
        initMocks(this);
    }

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
        
        assertEquals(2, RewriteFacetParametersComponent.parseFacetField(a).length);
        assertEquals(2, RewriteFacetParametersComponent.parseFacetField(b).length);
        assertEquals(1, RewriteFacetParametersComponent.parseFacetField(c).length);
        assertEquals(3, RewriteFacetParametersComponent.parseFacetField(d).length);
        assertEquals(3, RewriteFacetParametersComponent.parseFacetField(e).length);
        assertEquals(2, RewriteFacetParametersComponent.parseFacetField(f).length);
        assertEquals(2, RewriteFacetParametersComponent.parseFacetField(g).length);
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

    /** Check that if no mincount is supplied for a field facet then it gets defaulted to 1. */
    @Test
    public void rewriteMincountFacetFieldOption_MincountMissing()
    {
        // There are no existing facet parameters.
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        when(mockParams.getParameterNamesIterator()).thenReturn(Iterators.empty());

        Map<String, String> fieldMappings = ImmutableMap.of("NAME",
                    "{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name");
        List<String> facetNames = asList("{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name");

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings, facetNames);

        // Check that the mincount is set to 1.
        String actual = fixed.get("f.{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name.facet.mincount");
        assertEquals("Expected the existing mincount to be preserved.", "1", actual);
    }

    /** Check that if the mincount is set as 0 then it is updated to be 1. */
    @Test
    public void rewriteMincountFacetFieldOption_MincountSetZero()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has tried to set the mincount to zero.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("f.NAME.facet.mincount").iterator());
        when(mockParams.getParams("f.NAME.facet.mincount")).thenReturn(new String[]{"0"});

        Map<String, String> fieldMappings = ImmutableMap.of("NAME",
                    "{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name");
        List<String> facetNames = asList("{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name");

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings, facetNames);

        // Check that the mincount is kept as 2 and the field name is converted to the format stored by Solr.
        String actual = fixed.get("f.{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name.facet.mincount");
        assertEquals("Expected the existing mincount to be preserved.", "1", actual);
    }

    /** Check that if the user supplies a mincount of 2 then this is not changed. */
    @Test
    public void rewriteMincountFacetFieldOption_MincountSetTwo()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has set the mincount to 2.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("f.NAME.facet.mincount").iterator());
        when(mockParams.getParams("f.NAME.facet.mincount")).thenReturn(new String[]{"2"});

        Map<String, String> fieldMappings = ImmutableMap.of("NAME",
                    "{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name");
        List<String> facetNames = asList("{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name");

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings, facetNames);

        // Check that the mincount is kept as 2 and the field name is converted to the format stored by Solr.
        String actual = fixed.get("f.{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name.facet.mincount");
        assertEquals("Expected the existing mincount to be preserved.", "2", actual);
    }
}