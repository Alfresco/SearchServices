/*-
 * #%L
 * Alfresco Solr Search
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
package org.alfresco.solr.query.afts.requestHandler;

import org.alfresco.solr.AbstractAlfrescoSolrIT;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.stream.IntStream.range;

public abstract class AbstractRequestHandlerIT extends AbstractAlfrescoSolrIT
{
    @BeforeClass
    public static void spinUpSolr() throws Exception
    {
        initAlfrescoCore("schema.xml");
        Thread.sleep(1000);
    }

    void assertAQueryHasNumOfDocsWithJson(String query, String json, int num)
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "fq", "{!afts}AUTHORITY_FILTER_FROM_JSON", "q", query), json), "*[count(//doc)="+num+"]");
    }

    void assertResponseCardinality(String query, int num)
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", query), null), "*[count(//doc)="+num+"]");
    }

    void assertResponseCardinality(String field, String value, int expectedCardinality)
    {
        assertResponseCardinality(field + ":" + value, expectedCardinality);
    }

    void assertResponseCardinality(String field, String value, String filterQuery, int expectedCardinality)
    {
        String query = field + ":" + value;
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", query, "fq", filterQuery), null), "*[count(//doc)=" + expectedCardinality + "]");
    }

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

    void assertPage(String query, String sort, int num, int rows, int start, int [] sortOrder)
    {
        String[] params =
                {
                        "start", Integer.toString(start),
                        "rows", Integer.toString(rows),
                        "qt", "/afts",
                        "q", query,
                        "sort", sort
                };

        assertQ(areq(params(params), null), Stream.concat(
                Stream.of("*[count(//doc)=" + num + "]"),
                range(1, sortOrder.length)
                        .mapToObj(index -> "//result/doc[" + index + "]/long[@name='DBID'][.='" + sortOrder[index - 1] + "']"))
                .toArray(String[]::new));
    }
}
