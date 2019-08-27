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

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.slf4j.Logger;
import org.springframework.util.StringUtils;

/**
 * Temp files may take up a lot of space, warn administrators
 * of their existence, giving them the chance to manage them.
 * 
 * @author Matt Ward
 */
public class TempFileWarningLogger
{
    private final Logger log;
    private final Path dir;
    private final String glob;
    
    public TempFileWarningLogger(Logger log, String prefix, String[] extensions, Path dir)
    {
        this.log = log;
        this.dir = dir;
        // Match files with glob, e.g. "SomePrefix*.{tmp,~bak}" 
        glob = prefix + ".{"+ StringUtils.arrayToCommaDelimitedString(extensions) + "}"; 
    }
    
    public boolean checkFiles()
    {
        if (log.isDebugEnabled())
        {
            log.debug("Looking for temp files matching " + glob + " in directory " + dir);
        }
        
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob))
        {
            for (Path file : stream)
            {
                if (log.isDebugEnabled())
                {
                    log.debug("Solr suggester temp file found matching file pattern: " + glob + ", path: " + file);
                    log.debug("Removing suggester temp files.");
                }
                return true;
            }
            return false;
        }
        catch (IOException e)
        {
            throw new RuntimeException("Unable to create directory stream", e);
        }
    }

    public void removeFiles()
    {
        try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir, glob))
        {
            for (Path file : stream)
            {
                file.toFile().delete();
            }
        }
        catch (IOException e) 
        {
            log.debug("Unable to delete temp file", e);
        }
    }
    
    protected String getGlob()
    {
        return glob;
    }
}
