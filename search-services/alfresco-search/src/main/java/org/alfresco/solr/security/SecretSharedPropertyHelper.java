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
package org.alfresco.solr.security;

import org.apache.solr.core.SolrResourceLoader;

import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.alfresco.solr.security.SecretSharedPropertyCollector.SECURE_COMMS_PROPERTY;

class SecretSharedPropertyHelper
{
    private final static Function<String, Properties> toProperties =
            file -> {
                Properties props = new Properties();
                try
                {
                    props.load(new FileReader(file));
                }
                catch (Exception e)
                {
                    throw new RuntimeException(e);
                }
                return props;
            };

    /**
     * Read different values of the specified property from every "solrcore.properties" file.
     * @param name The name of the property to read
     * @param defaultValue The default value for the given property
     * @return List of different communication methods declared in SOLR Cores.
     */
    static Set<String> getPropertyFromCores(String name, String defaultValue)
    {
        try (Stream<Path> walk = Files.walk(Paths.get(SolrResourceLoader.locateSolrHome().toString())))
        {
            var solrCorePropertiesFiles =
                    walk.map(Path::toString)
                        .filter(path -> path.contains("solrcore.properties")
                                        && !path.contains("templates"))
                        .collect(toList());

            return solrCorePropertiesFiles.stream()
                    .map(toProperties)
                    .map(properties -> properties.getProperty(name, defaultValue))
                    .collect(toSet());
        }
        catch (IOException e)
        {
            throw new RuntimeException(e);
        }
    }
}
