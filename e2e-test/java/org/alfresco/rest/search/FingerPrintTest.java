/*
 * Copyright (C) 2017 Alfresco Software Limited.
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
package org.alfresco.rest.search;

import org.alfresco.utility.model.TestGroup;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * Search end point Public API test with finger print.
 * @author Michael Suzuki
 *
 */
public class FingerPrintTest extends AbstractSearchTest
{
    @Test(groups={TestGroup.SEARCH, TestGroup.REST_API})
    /**
     * Search similar document based on document finger print.
     * The data prep should have loaded a file
     * identical to the one loaded as part of this test.
     * @throws Exception
     */ 
    public void search() throws Exception
    {
        String uuid = file.getNodeRefWithoutVersion();
        Assert.assertNotNull(uuid);
        SearchResponse response = query(uuid);
        int count = response.getEntries().size();
        String fingerprint = String.format("FINGERPRINT:%s", uuid);
        Thread.sleep(25000);//Allow indexing to complete.
        response = query(fingerprint);
        count = response.getEntries().size();
        Assert.assertTrue(count > 1);
        for(SearchNodeModel m :response.getEntries())
        {
            m.getModel().assertThat().field("name").contains("pangram.txt");
        }
    }
    
}
