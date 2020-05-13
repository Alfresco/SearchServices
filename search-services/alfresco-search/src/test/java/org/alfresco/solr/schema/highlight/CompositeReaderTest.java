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

package org.alfresco.solr.schema.highlight;

import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.CharArrayReader;
import java.io.IOException;
import java.io.Reader;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.Assert.assertEquals;

public class CompositeReaderTest
{
    List<String> data =
            asList("This is the",
                    " whole string content ",
                    " we are expecting as final result.");

    @Test(expected=IllegalArgumentException.class)
    public void noReadersSupplied_shouldThrowAnException()
    {
        CompositeReader reader = new CompositeReader(null);
    }

    @Test(expected=IllegalArgumentException.class)
    public void emptyReadersListSupplied_shouldThrowAnException()
    {
        CompositeReader reader = new CompositeReader();
    }

    @Test
    public void oneWrappedReader() throws IOException
    {
        String expected = String.join("", data);

        try(CompositeReader classUnderTest = new CompositeReader(new CharArrayReader(expected.toCharArray())))
        {
            assertEquals(expected, IOUtils.toString(classUnderTest));
        }
    }

    @Test
    public void threeWrappedReader_firstOneConsumed() throws IOException
    {
        List<String> chunks =
                asList("This is the",
                        " whole string content ",
                        " we are expecting as final result.");

        String expected = String.join("", chunks.subList(1, 3));

        Reader [] readers =
                chunks.stream()
                        .map(String::toCharArray)
                        .map(CharArrayReader::new)
                        .toArray(CharArrayReader[]::new);

        assertEquals(chunks.iterator().next(), IOUtils.toString(readers[0]));

        try(CompositeReader classUnderTest = new CompositeReader(readers))
        {
            assertEquals(expected, IOUtils.toString(classUnderTest));
        }
    }

    @Test
    public void threeWrappedReaders() throws IOException
    {
        List<String> chunks =
                asList("This is the",
                        " whole string content ",
                        " we are expecting as final result.");

        String expected = String.join("", chunks);

        Reader [] readers =
                chunks.stream()
                    .map(String::toCharArray)
                    .map(CharArrayReader::new)
                    .toArray(CharArrayReader[]::new);

        try(CompositeReader classUnderTest = new CompositeReader(readers))
        {
            assertEquals(expected, IOUtils.toString(classUnderTest));
        }
    }
}
