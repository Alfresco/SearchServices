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
public abstract class BaseQParserPluginTest extends AbstractAlfrescoSolrTests
{
    @BeforeClass
    public static void spinUpSolr() throws Exception
    {
        initAlfrescoCore("schema.xml");
        Thread.sleep(1000);
    }

    // TODO Integer-> int and use streams for checking
    void assertAQueryIsSorted(String query, String sort, Locale aLocale, int num, Integer [] sortOrder)
    {
        List<String> xpaths =
                Stream.concat(
                    Stream.of("*[count(//doc)=" + num + "]"),
                    range(1, sortOrder.length)
                        .filter(index -> sortOrder[index - 1] != null)
                        .mapToObj(index -> "//result/doc[" + index + "]/long[@name='DBID'][.='" + sortOrder[index - 1] + "']"))
                .collect(Collectors.toList());

        String[] params = new String[] {"rows", "20", "qt", "/afts", "q", query, "sort", sort};

        if (aLocale != null)
        {
            List<String> localparams = new ArrayList<>(asList(params));
            localparams.add("locale");
            localparams.add(aLocale.toString());
            assertQ(areq(params(localparams.toArray(new String[0])), null), xpaths.toArray(new String[0]));
        }
        else
        {
            assertQ(areq(params(params), null), xpaths.toArray(new String[0]));
        }
    }

    void assertAQueryHasNumOfDocsWithJson(String query, String json, int num)
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "fq", "{!afts}AUTHORITY_FILTER_FROM_JSON", "q", query), json), "*[count(//doc)="+num+"]");
    }

    void assertAQueryHasNumberOfDocs(String query, int num)
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", query), null), "*[count(//doc)="+num+"]");
    }

    void assertAQueryHasNumberOfDocs(String query, String filter, int num)
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", query, "fq", filter), null), "*[count(//doc)="+num+"]");
    }
}
