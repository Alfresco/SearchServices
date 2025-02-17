/*
 * #%L
 * Alfresco Search Services
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

package org.alfresco.solr.query.afts.requestHandler;

import org.alfresco.solr.dataload.TestDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case which groups all tests related to a maintenance ticket.
 *
 * @author Andrea Gazzarini
 */
public class MNTIT extends AbstractRequestHandlerIT
{
    @BeforeClass
    public static void loadData() throws Exception
    {
        TestDataProvider dataProvider = new TestDataProvider(h);
        dataProvider.loadMntTestData();
    }

    /**
     * Search issues when using lucene queries related to tokenization of file names.
     */
    @Test
    public void mnt15039()
    {
        assertResponseCardinality("cm:name:\"one_two\"", 3);
        assertResponseCardinality("cm:name:\"Print\"", 2);
        assertResponseCardinality("cm:name:\"Print-Toolkit\"", 2);
        assertResponseCardinality("cm:name:\"Print-Toolkit-3204\"", 1);
        assertResponseCardinality("cm:name:\"Print-Toolkit-3204-The-Print-Toolkit-has-a-new-look-565022.html\"", 1);
        assertResponseCardinality("cm:name:\"*20150911100000*\"", 1);
    }

    /**
     * AFTS search using a wildcard to replace no, one or more characters in a phrase
     */
    @Test
    public void mnt15222()
    {
        assertResponseCardinality("cm:name:\"apple pear peach 20150911100000.txt\"", 1);
        assertResponseCardinality("cm:name:\"apple pear * 20150911100000.txt\"", 1);
        assertResponseCardinality("cm:name:\"apple * * 20150911100000.txt\"", 1);
        assertResponseCardinality("cm:name:\"apple * 20150911100000.txt\"", 1);
    }

    /**
     * Solr search behavior on portion of file name plus extension is different on 5.1.1 when compared to 5.0.3
     */
    @Test
    public void mnt16741()
    {
        assertResponseCardinality("cm:name:\"hello.txt\"", 2);
        assertResponseCardinality("cm:name:\"Test.hello.txt\"", 1);
        assertResponseCardinality("cm:name:\"Test1.hello.txt\"", 1);
    }

    /**
     * FTS for phrases that used to work in Solr 4 are no longer working in Solr 6.
     */
    @Test
    public void mnt19714()
    {
      assertResponseCardinality("\"AnalystName Craig\"", 1);
      assertResponseCardinality("\"AnalystName\" AND \"AnalystName Craig\"", 1);
      assertResponseCardinality("\"AnalystName\" AND !\"AnalystName Craig\"", 1);
      assertResponseCardinality("cm:name:\"BASF*.txt\"", 4);
    }

    @Test
    public void mnt24377()
    {
        assertResponseCardinality("acme:date:*", 2); // sanity check to make sure test nodes are indexed

        assertResponseCardinality("acme:date:NOW/DAY+1DAY", 0);
        assertResponseCardinality("acme:date:NOW/DAY", 1);
        assertResponseCardinality("acme:date:NOW/DAY-1DAY", 1);
        assertResponseCardinality("acme:date:NOW/DAY-2DAY", 0);

        assertResponseCardinality("acme:date:TODAY", 1);
        assertResponseCardinality("acme:date:NOW", 0);
    }
}
