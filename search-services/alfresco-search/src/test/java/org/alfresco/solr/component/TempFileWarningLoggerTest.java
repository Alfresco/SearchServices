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
package org.alfresco.solr.component;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;

public class TempFileWarningLoggerTest
{
    private Logger log;
    private Path path;
    
    @Before
    public void setUp()
    {
        path = Paths.get(System.getProperty("java.io.tmpdir"));
        
        // Simulate warn-level logging (Although currently we only log debug messages).
        log = mock(Logger.class, withSettings().lenient());
        when(log.isWarnEnabled()).thenReturn(true);
        when(log.isErrorEnabled()).thenReturn(true);
    }
    
    @Test
    public void checkGlobBuiltCorrectly()
    {
        TempFileWarningLogger warner = 
                    new TempFileWarningLogger(
                                log,
                                "MyPrefix*",
                                new String[] { "temp", "remove-me", "~notrequired" },
                                path);
        
        assertEquals("MyPrefix*.{temp,remove-me,~notrequired}", warner.getGlob());
    }
    
    @Test
    public void checkFindFiles() throws IOException
    {
        File f = File.createTempFile("MyPrefix", ".remove-me", path.toFile());
        f.deleteOnExit();
        
        try
        {
            TempFileWarningLogger warner = 
                        new TempFileWarningLogger(
                                    log,
                                    "MyPrefix*",
                                    new String[] { "temp", "remove-me", "~notrequired" },
                                    path);
            
            boolean found = warner.checkFiles();
            
            assertTrue("Should have found matching files", found);
            // Should be a warn-level log message.
            verify(log, never()).warn(anyString());
        }
        finally
        {
            f.delete();
        }
    }
    
    
    @Test
    public void checkWhenNoFilesToFind() throws IOException
    {
        File f = new File(path.toFile(), "TestFile.random");
        
        // It would be very odd if this file exists!
        assertFalse("Unable to perform test as file exists: " + f, f.exists());
                
        TempFileWarningLogger warner = 
                    new TempFileWarningLogger(
                                log,
                                "TestFile",
                                new String[] { "random" },
                                path);
        
        boolean found = warner.checkFiles();
        
        assertFalse("Should NOT have found matching file", found);
        // Should be no warn-level log message.
        verify(log, never()).warn(anyString());
    }    
    
    @Test
    public void removeManyFiles() throws IOException 
    {
        File f = File.createTempFile("WFSTInputIterator", ".input", path.toFile());
        File f2 = File.createTempFile("WFSTInputIterator", ".sorted", path.toFile());
        f.deleteOnExit();
        f2.deleteOnExit();
        
        TempFileWarningLogger warner = new TempFileWarningLogger(log,
                                                                 "WFSTInputIterator*",
                                                                 new String[] { "input", "sorted" },
                                                                 path);
        boolean found = warner.checkFiles();
        assertTrue("Should have found matching file", found);
        assertTrue(f.exists());
        assertTrue(f2.exists());
        if(found)
        {
            warner.removeFiles();
        }
        assertFalse(f.exists());
        assertFalse(f2.exists());
        
        boolean found2 = warner.checkFiles();
        assertFalse("Should NOT have found a matching file", found2);
        
    }
    @Test
    public void notToRemoveFilesThatDontMatch() throws IOException 
    {
        File f = File.createTempFile("someotherfile", ".input", path.toFile());
        File f2 = File.createTempFile("someotherfile", ".sorted", path.toFile());
        f.deleteOnExit();
        f2.deleteOnExit();
        
        TempFileWarningLogger warner = new TempFileWarningLogger(log,
                                                                 "WFSTInputIterator*",
                                                                 new String[] { "input", "sorted" },
                                                                 path);

        
        assertTrue(f.exists());
        assertTrue(f2.exists());
        warner.removeFiles();
        assertTrue(f.exists());
        assertTrue(f2.exists());
        
        boolean found2 = warner.checkFiles();
        assertFalse("Should NOT have found a matching file", found2);
        
    }
    
}
