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

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import org.alfresco.util.ISO8601DateFormat;
import org.junit.Before;
import org.junit.Test;
/**
 * Ported tests relating to cmis from AlfrescoCoreAdminTester (Legacy embedded
 * tests).
 * @author Michael Suzuki
 *
 */
public class CmisTest extends LoadCMISData
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
    }

        /******* check_D_text *******/
    @Test
    public void checkDtext()
    {
        assertQ(qurySolr("SELECT * FROM cmis:document"), expectedDocCount(12));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"), expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth = 'Un tokenised'"),
                         expectedDocCount(1));
        
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <> 'tokenised'"),
                expectedDocCount(1));
        
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth LIKE 'U_ to%sed'"),
                expectedDocCount(1));
        
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth NOT LIKE 't__eni%'"),
                expectedDocCount(1));
        
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth IN ('Un tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth NOT IN ('Un tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'Un tokenised'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'V'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'U'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'Un tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'V'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'U'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'tokenised'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'Un tokenised'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'V'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'U'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'tokenised'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'Un tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'V'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'U'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised = 'Un tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <> 'tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised LIKE 'U_ to%sed'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised NOT LIKE 't__eni%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised IN ('Un tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised NOT IN ('Un tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'Un tokenised'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'V'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'U'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'Un tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'V'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'U'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'tokenised'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'Un tokenised'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'V'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'U'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'tokenised'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'Un tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'V'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'U'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised = 'tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised <> 'tokenized'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised LIKE 'to%sed'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised NOT LIKE 'Ut__eniz%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised IN ('tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised NOT IN ('tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias = 'Un tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias <> 'tokenised'"),
                expectedDocCount(1));


        assertQ(qurySolr("SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias LIKE 'U_ to%sed'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias NOT LIKE 't__eni%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias IN ('Un tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias NOT IN ('Un tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias = 'Un tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias <> 'tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias LIKE 'U_ to%sed'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias NOT LIKE 't__eni%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias IN ('Un tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias NOT IN ('Un tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'tokenised'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias <> 'tokenized'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'to%sed'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'Ut__eniz%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY cmistest:multipleTextBoth "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextBoth IN ('Un tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextBoth NOT IN ('Un tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY cmistest:multipleTextUntokenised "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextUntokenised IN ('Un tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextUntokenised NOT IN ('Un tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'tokenised' =  ANY cmistest:multipleTextTokenised "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextTokenised IN ('tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextTokenised NOT IN ('tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleTextBoth as alias FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleTextBoth as alias FROM cmistest:extendedContent WHERE ANY alias IN ('Un tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleTextBoth as alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('Un tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleTextUntokenised alias FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('Un tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('Un tokenized')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:multipleTextTokenised alias FROM cmistest:extendedContent T WHERE 'tokenised' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:multipleTextTokenised alias FROM cmistest:extendedContent T WHERE ANY alias IN ('tokenised', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT T.cmistest:multipleTextTokenised alias FROM cmistest:extendedContent T WHERE ANY alias NOT IN ('tokenized')"),
                expectedDocCount(1));
    }
    @Test
    public void checkDmltext()
    {
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'AAAA BBBB'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'AAAA'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = '%AAAA'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = '%AAA'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'BBBB'"),
                expectedDocCount(0));


        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'CCCC DDDD'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth <> 'EEEE FFFF'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth LIKE 'AAA_ B%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth LIKE 'CCC_ D%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT LIKE 'B%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT LIKE 'D%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth IN ('AAAA BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth IN ('CCCC DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT IN ('EEEE FFFF')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised = 'AAAA BBBB'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised = 'CCCC DDDD'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised <> 'EEEE FFFF'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised LIKE 'AAA_ B%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised LIKE 'CCC_ D%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT LIKE 'B%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT LIKE 'D%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised IN ('AAAA BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised IN ('CCCC DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT IN ('EEEE FFFF')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'AAAA'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'BBBB'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'CCCC'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'DDDD'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised <> 'EEEE'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE 'A%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE '_B__'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE '%C'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE 'D%D'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised NOT LIKE 'CCCC_'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('AAAA', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('CCCC', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised NOT IN ('EEEE')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'AAAA BBBB'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'AAAA'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'BBBB'"),
                expectedDocCount(0));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'CCCC DDDD'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE FFFF'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias LIKE 'AAA_ B%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias LIKE 'CCC_ D%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'B%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'D%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE FFFF')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias = 'AAAA BBBB'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias = 'CCCC DDDD'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE FFFF'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'AAA_ B%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'CCC_ D%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'B%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'D%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE FFFF')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'AAAA'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'BBBB'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'CCCC'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'DDDD'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'A%'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE '_B__'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE '%C'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'D%D'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'CCCC_'"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY cmistest:multipleMLTextBoth "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY cmistest:multipleMLTextBoth "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth IN ('AAAA BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth IN ('CCCC DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth NOT IN ('EEEE FFFF')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY cmistest:multipleMLTextUntokenised "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY cmistest:multipleMLTextUntokenised "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised IN ('AAAA BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised IN ('CCCC DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised NOT IN ('EEEE FFFF')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'AAAA' =  ANY cmistest:multipleMLTextTokenised "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'BBBB' =  ANY cmistest:multipleMLTextTokenised "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'CCCC' =  ANY cmistest:multipleMLTextTokenised "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE 'DDDD' =  ANY cmistest:multipleMLTextTokenised "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('AAAA', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('CCCC', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr(
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised NOT IN ('EEEE')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE FFFF')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE FFFF')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'AAAA' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'BBBB' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'CCCC' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'DDDD' =  ANY alias "),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('BBBB', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('DDDD', 'Monkey')"),
                expectedDocCount(1));

        assertQ(qurySolr("SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE')"),
                expectedDocCount(1));
    }
    @Test
    public void checkDfloat()
    {
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"), expectedDocCount(1));
    
        // d:float single
    
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat = 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat = 1.1"), expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat <> 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat <> 1.1"), expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat < 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat < 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat <= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat <= 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat > 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat > 0.9"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat >= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat >= 0.9"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleFloat NOT IN (1.1)"),expectedDocCount(1));
    
        // d:float single by alias
    
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias = 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias = 1.1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias <> 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias <> 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias < 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias < 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias <= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias <= 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias > 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias > 0.9"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias >= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias >= 0.9"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleFloat as alias FROM cmistest:extendedContent WHERE alias NOT IN (1.1)"),expectedDocCount(1));
    
        // d:float multiple
    
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '1' =  ANY cmistest:multipleFloat "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '1.1' =  ANY cmistest:multipleFloat "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleFloat IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleFloat IN (1.1, 2.2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleFloat NOT IN (1.1, 2.2)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleFloat NOT IN (1.3, 2.3)"),expectedDocCount(1));
    
        // d:float multiple by alias
    
        assertQ(qurySolr("SELECT cmistest:multipleFloat as alias FROM cmistest:extendedContent WHERE '1' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleFloat as alias FROM cmistest:extendedContent WHERE '1.1' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleFloat as alias FROM cmistest:extendedContent WHERE ANY alias IN (1, 2)"),expectedDocCount(1)); 
        assertQ(qurySolr("SELECT cmistest:multipleFloat as alias FROM cmistest:extendedContent WHERE ANY alias IN (1.1, 2.2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleFloat as alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (1.1, 2.2)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:multipleFloat as alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (1.3, 2.3)"),expectedDocCount(1));
    }
    @Test
    public void checkDdouble()
    {
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"),expectedDocCount(1));
        // d:double single
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble = 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble = 1.1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble <> 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble <> 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble < 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble < 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble <= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble <= 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble > 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble > 0.9"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble >= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble >= 0.9"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDouble NOT IN (1.1)"),expectedDocCount(1));
    
        // d:double single by alias
    
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias = 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias = 1.1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias <> 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias <> 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias < 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias < 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias <= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias <= 1.1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias > 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias > 0.9"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias >= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias >= 0.9"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleDouble alias FROM cmistest:extendedContent WHERE alias NOT IN (1.1)"),expectedDocCount(1));
    
        // d:double multiple
    
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '1' =  ANY cmistest:multipleDouble "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '1.1' =  ANY cmistest:multipleDouble "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDouble IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDouble IN (1.1, 2.2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDouble NOT IN (1.1, 2.2)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDouble NOT IN (1.3, 2.3)"),expectedDocCount(1));
    
        // d:double multiple by alias
    
        assertQ(qurySolr("SELECT cmistest:multipleDouble alias FROM cmistest:extendedContent WHERE '1' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleDouble alias FROM cmistest:extendedContent WHERE '1.1' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleDouble alias FROM cmistest:extendedContent WHERE ANY alias IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleDouble alias FROM cmistest:extendedContent WHERE ANY alias IN (1.1, 2.2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleDouble alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (1.1, 2.2)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:multipleDouble alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (1.3, 2.3)"),expectedDocCount(1));
    }
    @Test 
    public void checkDint()
    {
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"),expectedDocCount(1));
    
        // d:int single
    
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger = 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger = 2"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger <> 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger <> 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger < 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger < 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger <= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger <= 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger > 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger > 0"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger >= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger >= 0"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleInteger NOT IN (2)"),expectedDocCount(1));
    
        // d:int single by alias
    
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias = 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias = 2"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias <> 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias <> 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias < 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias < 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias <= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias <= 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias > 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias > 0"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias >= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias >= 0"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleInteger alias FROM cmistest:extendedContent WHERE alias NOT IN (2)"),expectedDocCount(1));
    
        // d:int multiple
    
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '1' =  ANY cmistest:multipleInteger "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '2' =  ANY cmistest:multipleInteger "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleInteger IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleInteger IN (2, 3)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleInteger NOT IN (1, 2)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleInteger NOT IN (2, 3)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleInteger NOT IN (3, 4)"),expectedDocCount(1));
    
        // d:int multiple by alias
    
        assertQ(qurySolr("SELECT cmistest:multipleInteger as alias FROM cmistest:extendedContent WHERE '1' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleInteger as alias FROM cmistest:extendedContent WHERE '2' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleInteger as alias FROM cmistest:extendedContent WHERE ANY alias IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleInteger as alias FROM cmistest:extendedContent WHERE ANY alias IN (2, 3)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleInteger as alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (1, 2)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:multipleInteger as alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (2, 3)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:multipleInteger as alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (3, 4)"),expectedDocCount(1));
    }
    @Test
    public void checkDLong()
    {
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"),expectedDocCount(1));

        // d:long single
    
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong = 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong = 2"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong <> 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong <> 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong < 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong < 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong <= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong <= 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong > 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong > 0"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong >= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong >= 0"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleLong NOT IN (2)"),expectedDocCount(1));
    
        // d:long single by alias
    
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias = 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias = 2"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias <> 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias <> 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias < 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias < 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias <= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias <= 2"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias > 1"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias > 0"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias >= 1"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias >= 0"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:singleLong as alias FROM cmistest:extendedContent WHERE alias NOT IN (2)"),expectedDocCount(1));
    
        // d:long multiple
    
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '1' =  ANY cmistest:multipleLong "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '2' =  ANY cmistest:multipleLong "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleLong IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleLong IN (2, 3)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleLong NOT IN (1, 2)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleLong NOT IN (2, 3)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleLong NOT IN (3, 4)"),expectedDocCount(1));
    
        // d:long multiple by alias
    
        assertQ(qurySolr("SELECT cmistest:multipleLong alias FROM cmistest:extendedContent WHERE '1' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleLong alias FROM cmistest:extendedContent WHERE '2' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleLong alias FROM cmistest:extendedContent WHERE ANY alias IN (1, 2)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleLong alias FROM cmistest:extendedContent WHERE ANY alias IN (2, 3)"),expectedDocCount(1));
        assertQ(qurySolr("SELECT cmistest:multipleLong alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (1, 2)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:multipleLong alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (2, 3)"),expectedDocCount(0));
        assertQ(qurySolr("SELECT cmistest:multipleLong alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (3, 4)"),expectedDocCount(1));
    }
    @Test
    public void checkDdate()
    {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(testCMISDate00);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date date0 = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 2);
        Date date2 = cal.getTime();


        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"),expectedDocCount(1));

        // d:date single

        String d0 = ISO8601DateFormat.format(date0);
        String d1 = ISO8601DateFormat.format(testCMISDate00);
        String d2 = ISO8601DateFormat.format(date2);

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate = TIMESTAMP '" + d1 + "'"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate = TIMESTAMP '" + d2 + "'"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate <> TIMESTAMP '" + d1 + "'"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate <> TIMESTAMP '" + d2 + "'"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate < TIMESTAMP '" + d1 + "'"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate < TIMESTAMP '" + d2 + "'"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate <= TIMESTAMP '" + d1 + "'"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate <= TIMESTAMP '" + d2 + "'"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate > TIMESTAMP '" + d1 + "'"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate > TIMESTAMP '" + d0 + "'"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate >= TIMESTAMP '" + d1 + "'"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate >= TIMESTAMP '" + d0 + "'"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate IN (TIMESTAMP '" + d0+ "' ,TIMESTAMP '" + d1 + "')"),expectedDocCount(1)); 
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDate NOT IN (TIMESTAMP '" + d2 + "')"),expectedDocCount(1));

        // d:date single by alias

        assertQ(qurySolr("SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias = TIMESTAMP '" + d1 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias = TIMESTAMP '" + d2
                                + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias <> TIMESTAMP '" + d1
                                + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias <> TIMESTAMP '" + d2
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias < TIMESTAMP '" + d1
                                + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias < TIMESTAMP '" + d2
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias <= TIMESTAMP '" + d1
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias <= TIMESTAMP '" + d2
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias > TIMESTAMP '" + d1
                                + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias > TIMESTAMP '" + d0
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias >= TIMESTAMP '" + d1
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias >= TIMESTAMP '" + d0
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias IN (TIMESTAMP '"
                                + d0 + "' ,TIMESTAMP '" + d1 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDate as alias FROM cmistest:extendedContent WHERE alias NOT IN (TIMESTAMP '"
                                + d2 + "')"),expectedDocCount(1));

        // d:date multiple

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE TIMESTAMP '" + d1
                    + "' =  ANY cmistest:multipleDate "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE TIMESTAMP '" + d2
                    + "' =  ANY cmistest:multipleDate "),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDate IN (TIMESTAMP '" + d1
                                + "', TIMESTAMP '" + d2 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDate IN (TIMESTAMP '" + d2
                                + "', TIMESTAMP '" + d0 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDate NOT IN (TIMESTAMP '" + d0
                                + "', TIMESTAMP '" + d1 + "')"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDate NOT IN (TIMESTAMP '" + d1
                                + "', TIMESTAMP '" + d2 + "')"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDate NOT IN (TIMESTAMP '" + d0
                                + "')"),expectedDocCount(1));

        // d:date multiple by alias

        assertQ(qurySolr(
                    "SELECT cmistest:multipleDate alias FROM cmistest:extendedContent WHERE TIMESTAMP '" + d1
                                + "' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDate alias FROM cmistest:extendedContent WHERE TIMESTAMP '" + d2
                                + "' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDate alias FROM cmistest:extendedContent WHERE ANY alias IN (TIMESTAMP '"
                                + d1 + "', TIMESTAMP '" + d2 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDate alias FROM cmistest:extendedContent WHERE ANY alias IN (TIMESTAMP '"
                                + d2 + "', TIMESTAMP '" + d0 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDate alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (TIMESTAMP '"
                                + d0 + "', TIMESTAMP '" + d1 + "')"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDate alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (TIMESTAMP '"
                                + d1 + "', TIMESTAMP '" + d2 + "')"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDate alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (TIMESTAMP '"
                                + d0 + "')"),expectedDocCount(1));
    }
    public void check_D_datetime()
    {
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(testCMISDate00);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        Date date0 = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, 2);
        Date date2 = cal.getTime();


        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"),expectedDocCount(1));

        String d0 = ISO8601DateFormat.format(date0);
        String d1 = ISO8601DateFormat.format(testCMISDate00);
        String d2 = ISO8601DateFormat.format(date2);

        // d:datetime single

        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime = TIMESTAMP '" + d1 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime = TIMESTAMP '" + d2 + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime <> TIMESTAMP '" + d1 + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime <> TIMESTAMP '" + d2 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime < TIMESTAMP '" + d1 + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime < TIMESTAMP '" + d2 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime <= TIMESTAMP '" + d1 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime <= TIMESTAMP '" + d2 + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime > TIMESTAMP '" + d1 + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime > TIMESTAMP '" + d0 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime >= TIMESTAMP '" + d1 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime >= TIMESTAMP '" + d0 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime IN (TIMESTAMP '" + d0
                                + "' ,TIMESTAMP '" + d1 + "')"),expectedDocCount(1)); 
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleDatetime NOT IN (TIMESTAMP '" + d2
                                + "')"),expectedDocCount(1));

        // d:datetime single by alias

        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias = TIMESTAMP '" + d1
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias = TIMESTAMP '" + d2
                                + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias <> TIMESTAMP '"
                                + d1 + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias <> TIMESTAMP '"
                                + d2 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias < TIMESTAMP '" + d1
                                + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias < TIMESTAMP '" + d2
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias <= TIMESTAMP '"
                                + d1 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias <= TIMESTAMP '"
                                + d2 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias > TIMESTAMP '" + d1
                                + "'"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias > TIMESTAMP '" + d0
                                + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias >= TIMESTAMP '"
                                + d1 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias >= TIMESTAMP '"
                                + d0 + "'"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias IN (TIMESTAMP '"
                                + d0 + "' ,TIMESTAMP '" + d1 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:singleDatetime alias FROM cmistest:extendedContent WHERE alias NOT IN (TIMESTAMP '"
                                + d2 + "')"),expectedDocCount(1));

        // d:date multiple

        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE TIMESTAMP '" + d1
                    + "' =  ANY cmistest:multipleDatetime "),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE TIMESTAMP '" + d2
                    + "' =  ANY cmistest:multipleDatetime "),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDatetime IN (TIMESTAMP '" + d1
                                + "', TIMESTAMP '" + d2 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDatetime IN (TIMESTAMP '" + d2
                                + "', TIMESTAMP '" + d0 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDatetime NOT IN (TIMESTAMP '"
                                + d0 + "', TIMESTAMP '" + d1 + "')"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDatetime NOT IN (TIMESTAMP '"
                                + d1 + "', TIMESTAMP '" + d2 + "')"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleDatetime NOT IN (TIMESTAMP '"
                                + d0 + "')"),expectedDocCount(1));

        // d:date multiple by alias

        assertQ(qurySolr(
                    "SELECT cmistest:multipleDatetime alias FROM cmistest:extendedContent WHERE TIMESTAMP '" + d1
                                + "' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDatetime alias FROM cmistest:extendedContent WHERE TIMESTAMP '" + d2
                                + "' =  ANY alias "),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDatetime alias FROM cmistest:extendedContent WHERE ANY alias IN (TIMESTAMP '"
                                + d1 + "', TIMESTAMP '" + d2 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDatetime alias FROM cmistest:extendedContent WHERE ANY alias IN (TIMESTAMP '"
                                + d2 + "', TIMESTAMP '" + d0 + "')"),expectedDocCount(1));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDatetime alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (TIMESTAMP '"
                                + d0 + "', TIMESTAMP '" + d1 + "')"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDatetime alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (TIMESTAMP '"
                                + d1 + "', TIMESTAMP '" + d2 + "')"),expectedDocCount(0));
        assertQ(qurySolr(
                    "SELECT cmistest:multipleDatetime alias FROM cmistest:extendedContent WHERE ANY alias NOT IN (TIMESTAMP '"
                                + d0 + "')"),expectedDocCount(1));
    }
    @Test
    public void checkDboolean()
    {
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"),expectedDocCount(1));
    // d:boolean single
    assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleBoolean = TRUE"),expectedDocCount(1));
    assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleBoolean = true"),expectedDocCount(1));
    assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleBoolean = FALSE"),expectedDocCount(0));
    assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleBoolean = false"),expectedDocCount(0));
    // not strictly compliant...
    assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE cmistest:singleBoolean = TRue"),expectedDocCount(1));
    // d:boolean single by alias
    assertQ(qurySolr("SELECT cmistest:singleBoolean alias FROM cmistest:extendedContent WHERE alias = TRUE"),expectedDocCount(1));
    assertQ(qurySolr("SELECT cmistest:singleBoolean alias FROM cmistest:extendedContent WHERE alias = true"),expectedDocCount(1));
    assertQ(qurySolr("SELECT cmistest:singleBoolean alias FROM cmistest:extendedContent WHERE alias = FALSE"),expectedDocCount(0));
    assertQ(qurySolr("SELECT cmistest:singleBoolean alias FROM cmistest:extendedContent WHERE alias = false"),expectedDocCount(0));
    // not strictly compliant...
    assertQ(qurySolr("SELECT cmistest:singleBoolean alias FROM cmistest:extendedContent WHERE alias = TRue"),expectedDocCount(1));
    // d:boolean multiple
    assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '1' =  ANY cmistest:multipleBoolean "),expectedDocCount(1));
    assertQ(qurySolr("SELECT * FROM cmistest:extendedContent WHERE '2' =  ANY cmistest:multipleBoolean "),expectedDocCount(1)); 
    // d:boolean multiple by alias
    assertQ(qurySolr("SELECT cmistest:multipleBoolean as alias FROM cmistest:extendedContent WHERE '1' =  ANY alias "),expectedDocCount(1));
    assertQ(qurySolr("SELECT cmistest:multipleBoolean as alias FROM cmistest:extendedContent WHERE '2' =  ANY alias "),expectedDocCount(1));
    }
    @Test
    public void checkContainsSyntax()
    {
        assertQ(qurySolr("SELECT * FROM cmistest:extendedContent"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('two')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('-quick')"),expectedDocCount(11));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick brown fox')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick two')"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick -two')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('-quick two')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('-quick -two')"),expectedDocCount(10));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('fox brown quick')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick OR two')"),expectedDocCount(2));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick OR -two')"),expectedDocCount(11));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('-quick OR -two')"),expectedDocCount(12));
        //TODO FIXME        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('\\'quick brown fox\\'')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('\\'fox brown quick\\'')"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('\\'quick brown fox\\' two')"),expectedDocCount(0));
        //TODO FIXME        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('\\'quick brown fox\\' -two')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('-\\'quick brown fox\\' two')"),expectedDocCount(1));
      //TODO FIXME  assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('-\\'quick brown fox\\' -two')"),expectedDocCount(10));

        // escaping
        assertQ(qurySolr("SELECT * FROM cmis:folder WHERE CONTAINS('cmis:name:\\'Folder 9\\\\\\'\\'')"),expectedDocCount(1));

        // precedence
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick OR brown two')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick OR brown AND two')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick OR (brown AND two)')"),expectedDocCount(1));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('(quick OR brown) AND two')"),expectedDocCount(0));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick OR brown OR two')"),expectedDocCount(2));
        assertQ(qurySolr("SELECT * FROM cmis:document WHERE CONTAINS('quick OR brown two')"),expectedDocCount(1));
    }
}
