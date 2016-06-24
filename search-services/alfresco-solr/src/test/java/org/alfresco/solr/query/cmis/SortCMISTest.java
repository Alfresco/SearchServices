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
package org.alfresco.solr.query.cmis;

import java.io.IOException;

import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.SimpleOrderedMap;
import org.junit.Before;
import org.junit.Test;
/**
 * Ported tests relating to cmis from AlfrescoCoreAdminTester (Legacy embedded
 * tests).
 * @author Michael Suzuki
 *
 */
public class SortCMISTest extends LoadCMISData
{
    @Before
    public void setup() throws Exception
    {
        /******** Load record ************/

        addTypeTestData(testCMISFolder00NodeRef,
                testCMISRootNodeRef,
                testCMISBaseFolderNodeRef,
                testCMISBaseFolderQName,
                testCMISFolder00QName,
                testCMISDate00);
        addTypeSortTestData(testCMISFolder00NodeRef, 
                testCMISRootNodeRef, 
                testCMISBaseFolderNodeRef, 
                testCMISBaseFolderQName,
                testCMISFolder00QName,
                testCMISDate00);
    }
    
    
    
    @Test
    public void checkOrder() throws IOException
    {

        NamedList<Object> report = new SimpleOrderedMap<Object>();
//        rsp.add("CMIS order", report);

        Integer[] asc = new Integer[] { 200, 201, 202, 1008, 1005, 1004, 1009, 1001, 1007, 1006, 1003, 1002, 100, 1000 };
        Integer[] desc = new Integer[] { 1000, 100, 1002, 1003, 1006, 1007, 1001, 1009, 1004, 1005, 1008, 202, 201, 200 };

        checkOrderableProperty( report, "cmistest:singleTextUntokenised", asc, desc);
        // checkOrderableProperty( report, "cmistest:singleTextTokenised");
        checkOrderableProperty( report, "cmistest:singleTextBoth", asc, desc);

        // testOrderablePropertyFail("test:multipleTextUntokenised");
        // testOrderablePropertyFail("test:multipleTextTokenised");
        // testOrderablePropertyFail("test:multipleTextBoth");

        asc = new Integer[] { 200, 201, 202, 1009, 100, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008 };
        desc = new Integer[] { 1008, 1007, 1006, 1005, 1004, 1003, 1002, 1001, 1000, 100, 1009, 202, 201, 200 };

        checkOrderableProperty( report, "cmistest:singleMLTextUntokenised", asc, desc);
        // testOrderablePropertyFail("cmistest:singleMLTextTokenised");
        checkOrderableProperty( report, "cmistest:singleMLTextBoth", asc, desc);

        // testOrderablePropertyFail("cmistest:multipleMLTextUntokenised");
        // testOrderablePropertyFail("cmistest:multipleMLTextTokenised");
        // testOrderablePropertyFail("cmistest:multipleMLTextBoth");

        asc = new Integer[] { 200, 1000, 201, 202, 100, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009  };     
        desc = new Integer[] { 1009, 1008, 1007, 1006, 1005, 1004, 1003, 1002, 1001, 100, 202, 201, 1000, 200 };

        checkOrderableProperty( report, "cmistest:singleFloat", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleFloat");

        checkOrderableProperty( report, "cmistest:singleDouble", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleDouble");

        asc = new Integer[] { 200, 1000, 201, 202, 100, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009  };     
        desc = new Integer[] { 1009, 1008, 1007, 1006, 1005, 1004, 1003, 1002, 1001, 100, 202, 201, 1000, 200 };

        checkOrderableProperty( report, "cmistest:singleInteger", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleInteger");

        checkOrderableProperty( report, "cmistest:singleLong", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleLong");

        asc = new Integer[] { 200, 201, 202, 100, 1000, 1001, 1002, 1003, 1004, 1005, 1006, 1007, 1008, 1009  };     
        desc = new Integer[] { 1009, 1008, 1007, 1006, 1005, 1004, 1003, 1002, 1001, 1000, 100, 202, 201, 200 };

        checkOrderableProperty( report, "cmistest:singleDate", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleDate");

        checkOrderableProperty( report, "cmistest:singleDatetime", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleDatetime");

        asc = new Integer[] { 1001, 1003, 1005, 1007, 1009, 100, 1000, 1002, 1004, 1006, 1008, 200, 201, 202,  };
        desc = new Integer[] { 1008, 1006, 1004, 1002, 1000, 100, 1009, 1007, 1005, 1003, 1001, 202, 201, 200 }; 

        checkOrderableProperty( report, "cmistest:singleBoolean", asc, desc);
        // testOrderablePropertyFail("cmistest:multipleBoolean");

    }
    private void checkOrderableProperty(NamedList<Object> report, String propertyQueryName, Integer[] asc, Integer[] desc) throws IOException
    {
//        testQueryByHandler(report, core, "/cmis", "SELECT " + propertyQueryName
//                + " FROM cmistest:extendedContent ORDER BY " + propertyQueryName + " ASC, cmis:objectId ASC", 14, null, asc, null,
//                    null, null, (String) null);
        assertQ(qurySolr("SELECT " + propertyQueryName 
                         + " FROM cmistest:extendedContent ORDER BY " 
                         + propertyQueryName 
                         + " ASC, cmis:objectId ASC"),expectedDocCount(14));
        
//        testQueryByHandler(report, core, "/cmis", "SELECT " + propertyQueryName
//                + " FROM cmistest:extendedContent ORDER BY " + propertyQueryName + " DESC, cmis:objectId DESC", 14, null, desc, null,
//                null, null, (String) null);
        assertQ(qurySolr("SELECT " + propertyQueryName
                + " FROM cmistest:extendedContent ORDER BY "
                + propertyQueryName 
                + " DESC, cmis:objectId DESC"),expectedDocCount(14));
    }
}
