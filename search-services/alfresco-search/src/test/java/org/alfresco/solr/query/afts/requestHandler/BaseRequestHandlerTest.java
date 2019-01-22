package org.alfresco.solr.query.afts.requestHandler;

import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.junit.BeforeClass;

import java.util.stream.Stream;

import static java.util.stream.IntStream.range;

public abstract class BaseRequestHandlerTest extends AbstractAlfrescoSolrTests
{
    @BeforeClass
    public static void spinUpSolr() throws Exception
    {
        initAlfrescoCore("schema.xml");
        Thread.sleep(1000);
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
