package org.alfresco.solr.query.afts.qparser;

import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.junit.BeforeClass;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Arrays.stream;
import static java.util.stream.IntStream.range;

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

    void assertAQueryIsSorted(String query, String sort, Locale aLocale, int num, Integer[] sortOrder)
    {
        List<String> xpaths = new ArrayList();
        xpaths.add("*[count(//doc)=" + num + "]");
        for (int i = 1; i <= sortOrder.length; i++)
        {
            if(sortOrder[i - 1] != null) {
                xpaths.add("//result/doc[" + i + "]/long[@name='DBID'][.='" + sortOrder[i - 1] + "']");
            }
        }

        String[] params = new String[] {"rows", "20", "qt", "/afts", "q", query, "sort", sort};


        if (aLocale != null)
        {
            List<String> localparams = new ArrayList<>(params.length+2);
            localparams.addAll(Arrays.asList(params));
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
