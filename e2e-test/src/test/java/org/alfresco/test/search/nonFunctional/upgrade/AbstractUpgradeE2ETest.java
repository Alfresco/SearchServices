/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
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

package org.alfresco.test.search.nonFunctional.upgrade;

import org.alfresco.cmis.CmisWrapper;
import org.alfresco.utility.data.DataContent;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.data.provider.XMLTestData;
import org.alfresco.utility.network.ServerHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

/**
 * We can use this class for both SearchService and InsightEngine.
 *
 * @author Paul Brodner
 */
@ContextConfiguration("classpath:alfresco-search-e2e-context.xml")
public abstract class AbstractUpgradeE2ETest extends AbstractTestNGSpringContextTests
{
    @Autowired
    protected ServerHealth serverHealth;

    @Autowired
    protected DataUser dataUser;
    
    @Autowired
    protected DataSite dataSite;

    @Autowired
    protected DataContent dataContent;
    
    @Autowired
    protected CmisWrapper cmisAPI;
    
    protected XMLTestData testData;
        
    @BeforeClass(alwaysRun = true)
    public void checkServerHealth()
    {
        serverHealth.assertServerIsOnline();        
    }      
}