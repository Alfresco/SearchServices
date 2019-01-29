/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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
package org.alfresco.solr.query.afts.qparser;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.solr.query.afts.TestDataProvider;
import org.alfresco.util.ISO9075;
import org.junit.BeforeClass;
import org.junit.Test;

public class FieldNameEscapingTest extends AbstractQParserPluginTest implements QueryConstants
{
    private static TestDataProvider DATASETS_PROVIDER;

    @BeforeClass
    public static void loadData() throws Exception
    {
        DATASETS_PROVIDER = new TestDataProvider(h);
        DATASETS_PROVIDER.loadEscapingTestData();
    }

    @Test
    public void complexLocalNameEscaping()
    {
        assertAQuery("PATH:\"/cm:" + ISO9075.encode(DATASETS_PROVIDER.getComplexLocalName()) + "\"", 1);
    }

    @Test
    public void numericLocalNameEscaping()
    {
        assertAQuery("PATH:\"/cm:" + ISO9075.encode(DATASETS_PROVIDER.getNumericLocalName()) + "\"", 1);
    }
}