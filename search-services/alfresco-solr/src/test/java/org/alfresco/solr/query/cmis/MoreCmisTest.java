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

import org.junit.Before;
import org.junit.Test;

public class MoreCmisTest extends LoadCMISData
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
}
