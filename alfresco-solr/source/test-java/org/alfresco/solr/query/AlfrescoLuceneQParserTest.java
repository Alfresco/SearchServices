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
package org.alfresco.solr.query;

import static org.junit.Assert.assertEquals;

import org.alfresco.solr.query.AlfrescoLuceneQParserPlugin.AlfrescoLuceneQParser;
import org.junit.Before;
import org.junit.Test;


public class AlfrescoLuceneQParserTest
{
    private AlfrescoLuceneQParser parser;
    
    @Before
    public void setUp() throws Exception
    {
        parser = new AlfrescoLuceneQParser(null, null, null, null, null);
    }

    @Test
    public void forwardSlashesEscapedCorrectly()
    {
        assertEquals("Nothing to escape", "abcdef", parser.escape("abcdef"));
        assertEquals("Escape single slash", "abc\\/def", parser.escape("abc/def"));
        assertEquals("Do not escape quoted slash", "abc\"/\"def", parser.escape("abc\"/\"def"));
        assertEquals("Do not escape escaped slash", "abc\\/def", parser.escape("abc\\/def"));
        assertEquals("Do not escape quoted escaped slash", "abc\"\\/\"def", parser.escape("abc\"\\/\"def"));
        assertEquals("Escape multiple consecutive slashes", "abc\\/\\/def", parser.escape("abc//def"));
        assertEquals("abc\\/\\/\\/\\/\\/def\\/\\/\\/:\"bl/ah\"", parser.escape("abc/////def/\\//:\"bl/ah\""));
        
        // Test case from ACE-3071
        final String input = "(@\\{http\\://www.alfresco.org/model/imap/1.0\\}messageId:\"000000005E157E8B296B6B4DA1F00A30E4F21FE1C4002000\" OR @\\{http\\://www.westernacher.com/alfresco/models/wpsmail\\-v2\\}messageId:\"000000005E157E8B296B6B4DA1F00A30E4F21FE1C4002000\")";
        final String expected = "(@\\{http\\:\\/\\/www.alfresco.org\\/model\\/imap\\/1.0\\}messageId:\"000000005E157E8B296B6B4DA1F00A30E4F21FE1C4002000\" OR @\\{http\\:\\/\\/www.westernacher.com\\/alfresco\\/models\\/wpsmail\\-v2\\}messageId:\"000000005E157E8B296B6B4DA1F00A30E4F21FE1C4002000\")";
        assertEquals(expected, parser.escape(input));
    }
}
