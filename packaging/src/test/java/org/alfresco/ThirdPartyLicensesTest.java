/*
 * Copyright (C) 2014 Alfresco Software Limited.
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
package org.alfresco;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.nio.file.Files;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.Test;
import org.reflections.Reflections;
import org.reflections.scanners.ResourcesScanner;
import org.reflections.util.ClasspathHelper;
import org.reflections.util.ConfigurationBuilder;

/**
 * Tests of the declared third party licenses.
 */
public class ThirdPartyLicensesTest
{
    /** A list of jars that should be ignored as they will be excluded from the released artifacts. */
    private static final List<String> IGNORED = Arrays.asList("net/sf/ehcache/pool/sizeof/sizeof-agent.jar");
    /** Libraries produced by Alfresco will start with "alfresco-". */
    private static final String ALFRESCO_PREFIX = "alfresco-";
    /** Spring Surf is also produced by Alfresco (it lives at https://github.com/Alfresco/surf). */
    private static final String SPRING_SURF_PREFIX = "spring-surf-";

    /**
     * Test that the dependencies in notice.txt match the actual dependencies, to ensure we've included third party
     * licenses for all dependencies.
     *
     * @throws Exception Unexpected
     */
    @Test
    public void testLicensesDeclared() throws Exception
    {
        // Get the jar files found on the classpath.
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                    .setUrls(ClasspathHelper.forClassLoader())
                    .setScanners(new ResourcesScanner()));

        List<String> jars = reflections.getResources(Pattern.compile(".*\\.jar")).stream()
                    .map(path -> path.replace("libs/", ""))
                    .filter(name -> !name.startsWith(ALFRESCO_PREFIX)) // We don't need a declaration for alfresco libraries.
                    .filter(name -> !name.startsWith(SPRING_SURF_PREFIX))
                    .filter(name -> !IGNORED.contains(name))
                    .sorted()
                    .collect(Collectors.toList());

        // Get the dependencies referenced in notice.txt.
        List<String> lines = Files.readAllLines(
                    new File(this.getClass().getClassLoader().getResource("licenses/notice.txt").getFile())
                    .toPath());
        // Skip the header, which is all lines before the first "=== License Type ===" line.
        int headerSize = 0;
        for (String line : lines)
        {
            if (line.startsWith("==="))
            {
                break;
            }
            headerSize++;
        }
        List<String> declared = lines.stream()
                    .skip(headerSize)
                    .filter(line -> !line.isEmpty() && !line.startsWith("==="))
                    .map(line -> line.split(" ")[0])
                    .sorted()
                    .collect(Collectors.toList());

        assertEquals("Jar files on classpath do not match those declared in notice.txt file.", jars, declared);
    }
}
