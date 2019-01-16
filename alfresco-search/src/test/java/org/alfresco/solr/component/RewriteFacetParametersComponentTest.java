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

import java.util.HashMap;
import java.util.Map;

import com.google.common.collect.ImmutableMap;
import com.sun.xml.xsom.impl.scd.Iterators;

import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.params.ShardParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.handler.component.ShardRequest;
import org.apache.solr.request.SolrQueryRequest;
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
    @Mock
    SolrQueryRequest mockRequest;

    @Before
    public void setUp()
    {
        initMocks(this);
        when(mockRequest.getParams()).thenReturn(mockParams);
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
    public void rewriteMincountFacetFieldOption_mincountMissing_shouldSetGenericMinCountToOne()
    {
        // There are no existing facet parameters.
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        when(mockParams.getParameterNamesIterator()).thenReturn(Iterators.empty());
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(null);

        Map<String, String> fieldMappings = new HashMap<>();
        
        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is set to 1.
        String actual = fixed.get("facet.mincount");
        assertEquals("Expected the existing mincount to be preserved.", "1", actual);
    }

    /** Check that if the mincount is set as 0 then it is updated to be 1. */
    @Test
    public void rewriteMincountFacetFieldOption_mincountSetZero_shouldSetMincountToOne()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has tried to set the mincount to zero.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("facet.mincount").iterator());
        when(mockParams.get("facet.mincount")).thenReturn("0");
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(null);
        
        Map<String, String> fieldMappings = new HashMap<>();
        
        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is set to 1 and the field name is converted to the format stored by Solr.
        String actualCount = fixed.get("facet.mincount");
        assertEquals("Expected the mincount to be 1.", "1", actualCount);
    }

    @Test
    public void rewriteShardedRequestParameters_mincountSetZero_shouldKeepMincountToZero()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has tried to set the mincount to zero.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("facet.mincount").iterator());
        when(mockParams.get("facet.mincount")).thenReturn("0");
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(String.valueOf(ShardRequest.PURPOSE_GET_FACETS));

        Map<String, String> fieldMappings = new HashMap<>();

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is set to 1 and the field name is converted to the format stored by Solr.
        String actualCount = fixed.get("facet.mincount");
        assertEquals("Expected no fixed value", null, actualCount);
    }

    @Test
    public void rewriteMincountFacetFieldOption_mincountSetTwo_shouldKeepIt()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has tried to set the mincount to zero.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("facet.mincount").iterator());
        when(mockParams.get("facet.mincount")).thenReturn("2");
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(null);

        Map<String, String> fieldMappings = new HashMap<>();

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is set to 1 and the field name is converted to the format stored by Solr.
        String actualCount = fixed.get("facet.mincount");
        assertEquals("Expected the mincount to be 2.", "2", actualCount);
    }

    @Test
    public void rewriteMincountFacetFieldOption_perFieldMincountSetZero_shouldSetPerFieldMincountAndMincountToOne()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has tried to set the mincount to zero.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("f.NAME.facet.mincount","f.CONTENT.facet.mincount").iterator());
        when(mockParams.getParams("f.NAME.facet.mincount")).thenReturn(new String[]{"0"});
        when(mockParams.getParams("f.CONTENT.facet.mincount")).thenReturn(new String[]{"0"});
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(null);

        Map<String, String> fieldMappings = ImmutableMap.of(
            "NAME",
            "{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name","CONTENT",
            "{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}content");

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is set to 1 and the field name is converted to the format stored by Solr.
        String actualNameCount = fixed.get("f.{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name.facet.mincount");
        assertEquals("Expected the mincount to be 1.", "1", actualNameCount);
        String actualContentCount = fixed.get("f.{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}content.facet.mincount");
        assertEquals("Expected the mincount to be 1.", "1", actualContentCount);
        String actualCount = fixed.get("facet.mincount");
        assertEquals("Expected the mincount to be 1.", "1", actualCount);
    }
    
    /** Check that if the mincount is set as 0 then it is updated to be 1. */
    @Test
    public void rewriteMincountFacetFieldOption_perFieldMincountSetZero_shouldSetPerFieldMincountToOne()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has tried to set the mincount to zero.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("f.NAME.facet.mincount","f.CONTENT.facet.mincount").iterator());
        when(mockParams.getParams("f.NAME.facet.mincount")).thenReturn(new String[]{"0"});
        when(mockParams.getParams("f.CONTENT.facet.mincount")).thenReturn(new String[]{"0"});
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(null);

        Map<String, String> fieldMappings = ImmutableMap.of(
            "NAME",
                    "{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name","CONTENT",
            "{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}content");

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is set to 1 and the field name is converted to the format stored by Solr.
        String actualNameCount = fixed.get("f.{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name.facet.mincount");
        assertEquals("Expected the mincount to be 1.", "1", actualNameCount);
        String actualContentCount = fixed.get("f.{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}content.facet.mincount");
        assertEquals("Expected the mincount to be 1.", "1", actualContentCount);
    }

    /** Check that if the user supplies a mincount of 2 then this is not changed. */
    @Test
    public void rewriteMincountFacetFieldOption_perFieldMincountSetTwo_shouldKeepIt()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has tried to set the mincount to zero.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("f.NAME.facet.mincount","f.CONTENT.facet.mincount").iterator());
        when(mockParams.getParams("f.NAME.facet.mincount")).thenReturn(new String[]{"2"});
        when(mockParams.getParams("f.CONTENT.facet.mincount")).thenReturn(new String[]{"0"});
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(null);

        Map<String, String> fieldMappings = ImmutableMap.of(
            "NAME",
            "{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name","CONTENT",
            "{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}content");

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is kept as 2 and the field name is converted to the format stored by Solr.
        String actualNameCount = fixed.get("f.{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}name.facet.mincount");
        assertEquals("Expected the mincount to be 2.", "2", actualNameCount);
        String actualContentCount = fixed.get("f.{!afts key=SEARCH.FACET_FIELDS.LOCATION}text@s____@{http://www.alfresco.org/model/content/1.0}content.facet.mincount");
        assertEquals("Expected the mincount to be 1.", "1", actualContentCount);
    }

    
    @Test
    public void rewriteMincountFacetPivotOption_mincountMissing_shouldSetPivotMinCountToOne()
    {
        // There are no existing facet parameters.
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        when(mockParams.getParameterNamesIterator()).thenReturn(Iterators.empty());
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(null);

        Map<String, String> fieldMappings = new HashMap<>();

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.pivot.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is set to 1.
        String actual = fixed.get("facet.pivot.mincount");
        assertEquals("Expected the existing mincount to be preserved.", "1", actual);
    }

    /** Check that if the mincount is set as 0 then it is updated to be 1. */
    @Test
    public void rewriteMincountFacetPivotOption_mincountSetZero_shouldSetMincountToOne()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has tried to set the mincount to zero.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("facet.pivot.mincount").iterator());
        when(mockParams.get("facet.pivot.mincount")).thenReturn("0");
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(null);

        Map<String, String> fieldMappings = new HashMap<>();

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.pivot.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is set to 1 and the field name is converted to the format stored by Solr.
        String actualCount = fixed.get("facet.pivot.mincount");
        assertEquals("Expected the mincount to be 1.", "1", actualCount);
    }

    @Test
    public void rewriteMincountFacetPivotOption_mincountSetTwo_shouldKeepIt()
    {
        ModifiableSolrParams fixed = new ModifiableSolrParams();
        // The user has tried to set the mincount to zero.
        when(mockParams.getParameterNamesIterator()).thenReturn(asList("facet.pivot.mincount").iterator());
        when(mockParams.get("facet.pivot.mincount")).thenReturn("2");
        when(mockParams.get(ShardParams.SHARDS_PURPOSE)).thenReturn(null);

        Map<String, String> fieldMappings = new HashMap<>();

        // Call the method under test.
        rewriteFacetParametersComponent.rewriteMincountFacetFieldOption(fixed, mockParams, "facet.pivot.mincount", fieldMappings,
            mockRequest);

        // Check that the mincount is set to 1 and the field name is converted to the format stored by Solr.
        String actualCount = fixed.get("facet.pivot.mincount");
        assertEquals("Expected the mincount to be 2.", "2", actualCount);
    }
    
}