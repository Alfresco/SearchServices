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

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.search.GeneralHighlightParameters;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.junit.Before;
import org.junit.Test;


/**
 * Validate schema changes, ensures field return both tokenised and untokenised value
 * for NodeRef, boolean,category and qname.
 * 
 * @author Michael Suzuki
 *
 */
public class UntokenisedFieldTest extends AbstractAlfrescoSolrTests
{
    String nodeRef = "workspace://SpacesStore/00000000-0000-1-4731-76966678";
    String nodeRefS = "noderef@s_@mytest" ;
    String nodeRefSD = "noderef@sd@mytest" ;
    String nodeRefM = "noderef@m_@test";
    String nodeRefMD = "noderef@md@test";
    String boolenField = "boolean@m_@mytest";
    String booleanValue = "aaa-true";
    String boolenDocField = "boolean@md@mytest";
    String categoryMultiField = "category@m_@test";
    String categoryMultiDocField = "category@md@test";
    String categorySingleDocField = "category@sd@test";
    String categoryValue = "test category-1";
    
    @Before
    public void setup() throws Exception
    {
        initAlfrescoCore("schema.xml");
        assertU(adoc("id", "1", 
                nodeRefS, nodeRef,
                nodeRefM, nodeRef, 
                nodeRefMD, nodeRef, 
                nodeRefSD, nodeRef, 
                boolenField, booleanValue,
                boolenDocField, booleanValue,
                categoryMultiField, categoryValue,
                categoryMultiDocField, categoryValue,
                categorySingleDocField, categoryValue,
                "_version_","1"
                ));
        assertU(commit());
    }
    
    @Test
    /**
     * Test to ensure NodeRef is index as:
     * <lst name="noderef@d_@mytest">
     *  <int name="0000">5</int>
     *  <int name="00000000">5</int>
     *  <int name="1">5</int>
     *  <int name="4731">5</int>
     *  <int name="78222448">5</int>
     *  <int name="spacesstore">5</int>
     *  <int name="workspace">5</int>
     * </lst>
     * and:
     * <lst name="noderef@dm@mytest">
     *  <int name="workspace://SpacesStore/00000000-0000-1-4731-76966678">5</int>
     * </lst>
     * @throws Exception
     */
    public void testNodeRef() throws Exception
    {
        assertQ(req("q", "*:*","facet", "true","facet.field", nodeRefS),
                "//*[@name = '4731']");
        assertQ(req("q", "*:*","facet", "true","facet.field", nodeRefSD),
                "//*[@name = '" + nodeRef + "']");
        assertQ(req("q", "*:*", "facet", "true","facet.field", nodeRefM),
                "//*[@name = '76966678']");
        assertQ(req("q", "*:*","facet", "true","facet.field", nodeRefMD),
                "//*[@name = '" + nodeRef + "']");
    }
    @Test
    public void testBoolean() throws Exception
    {
        assertQ(req("q", "*:*","facet", "true","facet.field", boolenDocField),
                "//*[@name = '" + booleanValue + "']");
        assertQ(req("q", "*:*","facet", "true","facet.field", boolenField),
                "//*[@name = 'aaa']");
    }
    @Test
    public void testCategory() throws Exception
    {
        assertQ(req("q", "*:*","facet", "true","facet.field", categoryMultiDocField),
                "//*[@name = '" + categoryValue + "']");
        assertQ(req("q", "*:*","facet", "true","facet.field", categorySingleDocField),
                "//*[@name = '" + categoryValue + "']");
        assertQ(req("q", "*:*","facet", "true","facet.field", categoryMultiField),
                "//*[@name = 'test']");
    }
}
