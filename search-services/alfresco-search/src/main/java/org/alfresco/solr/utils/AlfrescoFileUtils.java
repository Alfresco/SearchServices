/*
 * Copyright (C) 2005-2019 Alfresco Software Limited.
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
package org.alfresco.solr.utils;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;


/**
 * @author Elia Porciani
 */
public class AlfrescoFileUtils {

    /**
     * Check if two directories contains the same files
     *
     * @param dir
     * @param dir2
     * @param extensions Limits the search to the extensions provided
     * @param recursive Check recursively in all subdirs
     * @return
     */
    public static boolean areDirectoryEquals(Path dir, Path dir2, String[] extensions, boolean recursive) {

        Map<String, File> filesDir1 = FileUtils.listFiles(new File(dir.toUri()), extensions, recursive)
                .stream().collect(Collectors.toMap(f -> f.getName(), f -> f));
        Map<String, File> filesDir2 = FileUtils.listFiles(new File(dir2.toUri()), extensions, recursive)
                .stream().collect(Collectors.toMap(f -> f.getName(), f -> f));

        if (filesDir1.size() != filesDir2.size())
            return false;

        return filesDir1.entrySet().stream().allMatch(e -> {
            File fileDir2 = filesDir2.get(e.getKey());
            if (fileDir2 == null) {
                return false;
            }
            try {
                byte[] otherBytes = Files.readAllBytes(e.getValue().toPath());
                byte[] thisBytes = Files.readAllBytes(fileDir2.toPath());

                return (Arrays.equals(otherBytes, thisBytes));
            } catch (IOException ex) {
                return false;
            }
        });
    }
}
