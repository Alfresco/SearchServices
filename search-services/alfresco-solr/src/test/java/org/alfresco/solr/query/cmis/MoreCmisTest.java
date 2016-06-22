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
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmis:document"), null),
                "*[count(//doc)=12]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth = 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <> 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth LIKE 'U_ to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth NOT LIKE 't__eni%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'Un tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'V'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth < 'U'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'V'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth <= 'U'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'Un tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'V'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth > 'U'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'V'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextBoth >= 'U'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised = 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <> 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised LIKE 'U_ to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised NOT LIKE 't__eni%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'Un tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'V'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised < 'U'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'V'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised <= 'U'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'Un tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'V'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised > 'U'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'tokenised'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'V'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextUntokenised >= 'U'"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised = 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised <> 'tokenized'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised LIKE 'to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised NOT LIKE 'Ut__eniz%'"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised IN ('tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleTextTokenised NOT IN ('tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias = 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias <> 'tokenised'"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias LIKE 'U_ to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias NOT LIKE 't__eni%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextBoth as alias FROM cmistest:extendedContent as T WHERE alias NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias = 'Un tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias <> 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias LIKE 'U_ to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias NOT LIKE 't__eni%'"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:singleTextUntokenised as alias FROM cmistest:extendedContent as T WHERE alias NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'tokenised'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias <> 'tokenized'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'to%sed'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'Ut__eniz%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY cmistest:multipleTextBoth "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextBoth IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextBoth NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY cmistest:multipleTextUntokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextUntokenised IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextUntokenised NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'tokenised' =  ANY cmistest:multipleTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextTokenised IN ('tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleTextTokenised NOT IN ('tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextBoth as alias FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextBoth as alias FROM cmistest:extendedContent WHERE ANY alias IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextBoth as alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextUntokenised alias FROM cmistest:extendedContent WHERE 'Un tokenised' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('Un tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('Un tokenized')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT T.cmistest:multipleTextTokenised alias FROM cmistest:extendedContent T WHERE 'tokenised' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:multipleTextTokenised alias FROM cmistest:extendedContent T WHERE ANY alias IN ('tokenised', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT T.cmistest:multipleTextTokenised alias FROM cmistest:extendedContent T WHERE ANY alias NOT IN ('tokenized')"), null),
                "*[count(//doc)=1]");


        /******** check_D_mltext     **********/

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'AAAA'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = '%AAAA'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = '%AAA'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'BBBB'"), null),
                "*[count(//doc)=0]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'AAAA'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'CCCC'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised <> 'EEEE'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE 'A%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE '_B__'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE '%C'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE 'D%D'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised NOT LIKE 'CCCC_'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'AAAA'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'BBBB'"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'AAAA'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'BBBB'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'CCCC'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'DDDD'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'A%'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE '_B__'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE '%C'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'D%D'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'CCCC_'"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY cmistest:multipleMLTextBoth "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY cmistest:multipleMLTextBoth "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY cmistest:multipleMLTextUntokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY cmistest:multipleMLTextUntokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'AAAA' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'BBBB' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'CCCC' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'DDDD' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'AAAA' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'BBBB' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'CCCC' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'DDDD' =  ANY alias "), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");
        
        /******** check_D_mltext     **********/
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'AAAA'"), null),
                "*[count(//doc)=0]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = '%AAAA'"), null),
                "*[count(//doc)=0]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = '%AAA'"), null),
                "*[count(//doc)=0]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'BBBB'"), null),
                "*[count(//doc)=0]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextBoth NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT LIKE 'B%'"), null),
           "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextUntokenised NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'AAAA'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'BBBB'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'CCCC'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised = 'DDDD'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised <> 'EEEE'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE 'A%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE '_B__'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE '%C'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised LIKE 'D%D'"), null),
               "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised NOT LIKE 'CCCC_'"), null),
               "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('AAAA', 'Monkey')"), null),
               "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('BBBB', 'Monkey')"), null),
               "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('CCCC', 'Monkey')"), null),
               "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised IN ('DDDD', 'Monkey')"), null),
               "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT * FROM cmistest:extendedContent WHERE cmistest:singleMLTextTokenised NOT IN ('EEEE')"), null),
               "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'AAAA BBBB'"), null),
               "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'AAAA'"), null),
               "*[count(//doc)=0]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                       "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'BBBB'"), null),
               "*[count(//doc)=0]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
               "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias = 'CCCC DDDD'"), null),
               "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                            "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE FFFF'"), null),
                    "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC DDDD', 'Monkey')"), null),
                        "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextBoth as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias = 'AAAA BBBB'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias = 'CCCC DDDD'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE FFFF'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'AAA_ B%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'CCC_ D%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'B%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'D%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextUntokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'AAAA'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'BBBB'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'CCCC'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias = 'DDDD'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias <> 'EEEE'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'A%'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE '_B__'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE '%C'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias LIKE 'D%D'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT LIKE 'CCCC_'"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:singleMLTextTokenised as alias FROM cmistest:extendedContent WHERE alias NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY cmistest:multipleMLTextBoth "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY cmistest:multipleMLTextBoth "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextBoth NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY cmistest:multipleMLTextUntokenised "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY cmistest:multipleMLTextUntokenised "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextUntokenised NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'AAAA' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'BBBB' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'CCCC' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE 'DDDD' =  ANY cmistest:multipleMLTextTokenised "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                         "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT * FROM cmistest:extendedContent WHERE ANY cmistest:multipleMLTextTokenised NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY alias "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY alias "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                    "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA BBBB', 'Monkey')"), null),
                 "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextBoth alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE 'AAAA BBBB' =  ANY alias "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE 'CCCC DDDD' =  ANY alias "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                    "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC DDDD', 'Monkey')"), null),
            "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextUntokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE FFFF')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'AAAA' =  ANY alias "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'BBBB' =  ANY alias "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'CCCC' =  ANY alias "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE 'DDDD' =  ANY alias "), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('AAAA', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('BBBB', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('CCCC', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias IN ('DDDD', 'Monkey')"), null),
                "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/cmis", "q",
                        "SELECT cmistest:multipleMLTextTokenised alias FROM cmistest:extendedContent WHERE ANY alias NOT IN ('EEEE')"), null),
                "*[count(//doc)=1]");
    }
}
