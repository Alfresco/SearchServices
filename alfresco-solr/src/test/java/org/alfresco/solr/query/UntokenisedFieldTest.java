/*
 * Copyright (C) 2005-206 Alfresco Software Limited.
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

import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.junit.Before;
import org.junit.Test;


/**
 * Validate schema definition of fields to return NodeRefs untokenised.
 * 
 * @author Michael Suzuki
 *
 */
public class UntokenisedFieldTest extends AbstractAlfrescoSolrTests
{
    NodeRef rootNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
    NodeRef node = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
    String nodeRefS = "noderef@s_@mytest" ;
    String nodeRefM = "noderef@m_@test";
    String boolenField = "boolean@m_@mytest";
    String booleanValue = "a-true";
    String categoryField = "category@m_@test";
    String categoryValue = "test category-1";
    String qnameField = "qname@m_@hits";
    
    
    @Before
    public void setup() throws Exception
    {
        initAlfrescoCore("solrconfig-afts.xml", "schema-afts.xml");
        assertU(adoc("id", "1", 
                nodeRefS, rootNodeRef.toString(),
                nodeRefM, node.toString(), 
                boolenField, booleanValue,
                categoryField, categoryValue,
                qnameField, ContentModel.PROP_HITS.toString(),
                "_version_","1"));
        assertU(adoc("id", "2", 
                nodeRefS, rootNodeRef.toString(),
                nodeRefM, node.toString(), 
                boolenField, booleanValue,
                categoryField, categoryValue,
                qnameField, ContentModel.PROP_HITS.toString(),
                "_version_","1"));
        assertU(commit());
    }
    
    @Test
    /**
     * Test to ensure NodeRef is not returned as:
     * <lst name="noderef@s_@mytest">
     *  <int name="0000">5</int>
     *  <int name="00000000">5</int>
     *  <int name="1">5</int>
     *  <int name="4731">5</int>
     *  <int name="78222448">5</int>
     *  <int name="spacesstore">5</int>
     *  <int name="workspace">5</int>
     * </lst>
     * It should return the as:
     * <lst name="noderef@s_@mytest">
     *  <int name="workspace://SpacesStore/00000000-0000-1-4731-76966678">5</int>
     * </lst>
     * @throws Exception
     */
    public void testNodeRef() throws Exception
    {
        assertQ(req("q", "*:*","facet", "true","facet.field", nodeRefS),
                "//*[@name = '" + rootNodeRef.toString() + "']");
        assertQ(req("q", "*:*", "facet", "true","facet.field", nodeRefM),
                "//*[@name = '" + node.toString() + "']");
    }
    @Test
    public void testBoolean() throws Exception
    {
        assertQ(req("q", "*:*","facet", "true","facet.field", boolenField),
                "//*[@name = '" + booleanValue + "']");
    }
    @Test
    public void testCategory() throws Exception
    {
        assertQ(req("q", "*:*","facet", "true","facet.field", categoryField),
                "//*[@name = '" + categoryValue + "']");
    }
    @Test
    public void testQName() throws Exception
    {
        assertQ(req("q", "*:*","facet", "true","facet.field", qnameField),
                "//*[@name = '" + ContentModel.PROP_HITS + "']");
    }
}
