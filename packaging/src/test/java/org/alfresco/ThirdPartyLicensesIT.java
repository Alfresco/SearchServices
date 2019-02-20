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

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

import static org.junit.Assert.fail;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import com.google.common.collect.Sets;

import org.junit.Test;

/**
 * Integration tests of the declared third party licenses.
 */
public class ThirdPartyLicensesIT
{
    /** Libraries produced by Alfresco will start with "alfresco-". */
    private static final String ALFRESCO_PREFIX = "alfresco-";
    /** Spring Surf is also produced by Alfresco (it lives at https://github.com/Alfresco/surf). */
    private static final String SPRING_SURF_PREFIX = "spring-surf-";
    /** Start of the name of the zip file. */
    private static final String ALFRESCO_SEARCH_SERVICES = "alfresco-search-services";

    /**
     * Test that the dependencies in notice.txt match the actual dependencies, to ensure we've included third party
     * licenses for all dependencies.
     *
     * @throws Exception Unexpected
     */
    @Test
    public void testLicensesDeclared() throws Exception
    {
        // Get the path to the target directory.
        Path targetPath = Paths.get(getClass().getProtectionDomain().getCodeSource().getLocation().getPath(), "..");

        // Try to find the zip file in the target directory.
        Path zipPath = Files.find(targetPath, 1,
                    (path, attribute) -> path.getFileName().toString().startsWith(ALFRESCO_SEARCH_SERVICES + "-")
                                && path.toString().endsWith(".zip"))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Could not find " + ALFRESCO_SEARCH_SERVICES
                                + "-*.zip in target directory. Is test being run from maven?"));
        // Look through the zip manifest and find all the third party jar files listed.
        Set<String> jars;
        try (java.util.zip.ZipFile zipFile = new ZipFile(zipPath.toFile()))
        {
            jars = zipFile.stream()
                        .map(ZipEntry::getName)
                        .map(Paths::get)
                        .map(Path::getFileName)
                        .map(Path::toString)
                        .filter(name -> name.endsWith(".jar"))
                        .filter(name -> !name.startsWith(ALFRESCO_PREFIX)) // We don't need a declaration for alfresco libraries.
                        .filter(name -> !name.startsWith(SPRING_SURF_PREFIX))
                        .collect(toSet());
        }

        // Get the dependencies referenced in notice.txt.
        Path noticePath = Paths.get(targetPath.toString(), "classes", "licenses", "notice.txt");
        List<String> lines = Files.readAllLines(noticePath);
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
        Set<String> declared = lines.stream()
                    .skip(headerSize)
                    .filter(line -> !line.isEmpty() && !line.startsWith("==="))
                    .map(line -> line.split(" ")[0])
                    .collect(toSet());

        // If the two lists don't match then fail the test and provide information about what's wrong.
        if (!jars.equals(declared))
        {
            List<String> onlyInTarget = Sets.difference(jars, declared).stream().sorted().collect(toList());
            List<String> onlyInNotice = Sets.difference(declared, jars).stream().sorted().collect(toList());

            fail("Jar files in zip do not match those declared in notice.txt file.\n"
                        + "Jars found but not declared: " + onlyInTarget + "\n"
                        + "Jars declared but not found: " + onlyInNotice);
        }
    }
}
