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
package org.alfresco.solr.query.cmis;

import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;
@RunWith(value = Suite.class)
@SuiteClasses({
    LoadCMISData.class,
    TestAlfrescoCMISQParserPlugin.class
})
/**
 * Test CMIS query and parsing plugin.
 * The suite works by loading all the test data using LoadCMISData.
 * This step load the data directly into the harness.
 * Once the data load completes the test executes the AlfrescoCMISQParserPluginTests
 * which contains all the tests.
 * 
 * @author Michael Suzuki
 *
 */
public class CMISQParserPluginSuite extends AbstractAlfrescoSolrTests
{
    

    @BeforeClass
    public static void beforeClass() throws Exception 
    {
        initAlfrescoCore("solrconfig-afts.xml", "schema-afts.xml");
        Thread.sleep(30000);
    }

}
