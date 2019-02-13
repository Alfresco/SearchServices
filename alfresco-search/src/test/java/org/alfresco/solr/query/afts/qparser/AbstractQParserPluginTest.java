/*
 * Copyright (C) 2005-2016 Alfresco Software Limited.
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
package org.alfresco.solr.query.afts.qparser;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;

import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;


/**
 * Supertype layer for all AFTS QParser tests.
 *
 * @author Andrea Gazzarini
 */
public abstract class AbstractQParserPluginTest extends AbstractAlfrescoSolrTests
{
    @BeforeClass
    public static void spinUpSolr() throws Exception
    {
        initAlfrescoCore("schema.xml");
        Thread.sleep(1000);
    }
}
