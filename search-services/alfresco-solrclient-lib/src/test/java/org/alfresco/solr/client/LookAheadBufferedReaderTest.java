/*-
 * #%L
 * Alfresco Remote API
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
package org.alfresco.solr.client;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.Reader;
import java.io.StringReader;

import static java.util.stream.IntStream.range;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(MockitoJUnitRunner.class)
public class LookAheadBufferedReaderTest
{
    @Mock
    Reader reader;

    private final String data = "1234567890ABCDEFGHILMNOPQRSTUVYXZ";

    @Test
    public void windowingModeEnabled()
    {
        LookAheadBufferedReader classUnderTest = new LookAheadBufferedReader(reader, data.length(), true, false);
        assertTrue(classUnderTest.isInWindowingMode());
        assertFalse(classUnderTest.isInCollectEverythingMode());
        assertFalse(classUnderTest.isBufferingDisabled());
    }

    @Test
    public void collectEverythingModeEnabled()
    {
        LookAheadBufferedReader classUnderTest = new LookAheadBufferedReader(reader, data.length(), false, true);
        assertTrue(classUnderTest.isInCollectEverythingMode());
        assertFalse(classUnderTest.isInWindowingMode());
        assertFalse(classUnderTest.isBufferingDisabled());
    }

    @Test
    public void bufferingDisabled()
    {
        LookAheadBufferedReader classUnderTest = new LookAheadBufferedReader(reader, data.length(), false, false);
        assertTrue(classUnderTest.isBufferingDisabled());
        assertFalse(classUnderTest.isInCollectEverythingMode());
        assertFalse(classUnderTest.isInWindowingMode());
    }

    @Test
    public void collectEverythingWinsOverWindowing()
    {
        LookAheadBufferedReader classUnderTest = new LookAheadBufferedReader(reader, data.length(), true, true);
        assertTrue(classUnderTest.isInCollectEverythingMode());
        assertFalse(classUnderTest.isInWindowingMode());
        assertFalse(classUnderTest.isBufferingDisabled());
    }

    @Test
    public void windowingModeShouldCollectPartialWindowsOfData()
    {
        int windowSize = 10;
        Reader reader = new StringReader(data);
        LookAheadBufferedReader classUnderTest = new LookAheadBufferedReader(reader, windowSize, true, false);

        // Read only 12 chars from the underlying stream
        range(0, 12).forEach(index -> consume(classUnderTest));

        String collectedWindow = classUnderTest.lookAheadAndGetBufferedContent();

        assertEquals(windowSize * 2, collectedWindow.length());
        assertEquals("4567890ABCDEFGHILMNO", collectedWindow);
    }

    @Test
    public void notEnoughCharsForTheWindow()
    {
        int windowSize = 10;
        Reader reader = new StringReader(data);
        LookAheadBufferedReader classUnderTest = new LookAheadBufferedReader(reader, windowSize, true, false);

        range(0, data.length() - 3).forEach(index -> consume(classUnderTest));

        String collectedWindow = classUnderTest.lookAheadAndGetBufferedContent();

        assertEquals(windowSize + 2, collectedWindow.length());
        assertEquals("NOPQRSTUVYXZ", collectedWindow);
    }

    @Test
    public void emptyDataShouldCollectEmptyWindow()
    {
        Reader reader = new StringReader("");
        LookAheadBufferedReader classUnderTest = new LookAheadBufferedReader(reader, 10, true, false);

        range(0, data.length() - 3).forEach(index -> consume(classUnderTest));

        String collectedWindow = classUnderTest.lookAheadAndGetBufferedContent();

        assertEquals(0, collectedWindow.length());
        assertEquals("", collectedWindow);
    }

    @Test
    public void collectEverythingModeShouldCollectTheWholeStream()
    {
        Reader reader = new StringReader(data);
        LookAheadBufferedReader classUnderTest =
                new LookAheadBufferedReader(
                        reader,
                        10, // this has no effect
                        false,
                        true);

        // Read only 12 chars from the underlying stream
        range(0, 12).forEach(index -> consume(classUnderTest));

        String collectedData = classUnderTest.lookAheadAndGetBufferedContent();

        assertEquals(data.length(), collectedData.length());
        assertEquals(data, collectedData);
    }

    @Test
    public void emptyDataShouldCollectEmptyStringInCollectEverythingMode()
    {
        Reader reader = new StringReader("");
        LookAheadBufferedReader classUnderTest =
                new LookAheadBufferedReader(
                        reader,
                        10, // this has no effect
                        false,
                        true);

        range(0, data.length() - 3).forEach(index -> consume(classUnderTest));

        String collectedData = classUnderTest.lookAheadAndGetBufferedContent();

        assertEquals(0, collectedData.length());
        assertEquals("", collectedData);
    }

    @Test
    public void bufferingModeDisableShouldCollectNoData()
    {
        int windowSize = 10;
        Reader reader = new StringReader(data);
        LookAheadBufferedReader classUnderTest = new LookAheadBufferedReader(reader, windowSize, false, false);

        // Read only 12 chars from the underlying stream
        range(0, 12).forEach(index -> consume(classUnderTest));
        assertEquals(LookAheadBufferedReader.BUFFERING_DISABLED_INFO_MESSAGE, classUnderTest.lookAheadAndGetBufferedContent());
    }

    @Test
    public void emptyDataShouldCollectEmptyDataWhenBufferingIsDisabled()
    {
        Reader reader = new StringReader("");
        LookAheadBufferedReader classUnderTest =
                new LookAheadBufferedReader(
                        reader,
                        10, // this has no effect
                        false,
                        false);

        range(0, data.length() - 3).forEach(index -> consume(classUnderTest));

        String collectedData = classUnderTest.lookAheadAndGetBufferedContent();

        assertEquals(LookAheadBufferedReader.BUFFERING_DISABLED_INFO_MESSAGE, collectedData);
    }

    private void consume(Reader reader)
    {
        try
        {
            reader.read();
        }
        catch (Exception exception)
        {
            throw new RuntimeException(exception);
        }
    }
}