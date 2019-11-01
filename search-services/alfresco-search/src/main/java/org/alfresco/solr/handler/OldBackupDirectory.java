/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *
 * Modification copyright (C) 2005-2019 Alfresco Software Limited.
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
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>
 *
 */
package org.alfresco.solr.handler;

import org.apache.solr.handler.SnapShooter;

import java.net.URI;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


class OldBackupDirectory implements Comparable<OldBackupDirectory>
{
    private static final Pattern dirNamePattern = Pattern.compile("^snapshot[.](.*)$");

    private URI basePath;
    private String dirName;
    private Optional<Date> timestamp = Optional.empty();

    public OldBackupDirectory(URI basePath, String dirName)
    {
        this.dirName = Objects.requireNonNull(dirName);
        this.basePath = Objects.requireNonNull(basePath);
        Matcher m = dirNamePattern.matcher(dirName);
        if (m.find())
        {
            try
            {
                this.timestamp = Optional.of(new SimpleDateFormat(SnapShooter.DATE_FMT, Locale.ROOT).parse(m.group(1)));
            }
            catch (ParseException e)
            {
                this.timestamp = Optional.empty();
            }
        }
    }

    public URI getPath()
    {
        return this.basePath.resolve(dirName);
    }

    public String getDirName()
    {
        return dirName;
    }

    public Optional<Date> getTimestamp()
    {
        return timestamp;
    }

    @Override
    public int compareTo(OldBackupDirectory that)
    {
        if (this.timestamp.isPresent() && that.timestamp.isPresent())
        {
            return that.timestamp.get().compareTo(this.timestamp.get());
        }
        // Use absolute value of path in case the time-stamp is missing on either side.
        return that.getPath().compareTo(this.getPath());
    }
}
