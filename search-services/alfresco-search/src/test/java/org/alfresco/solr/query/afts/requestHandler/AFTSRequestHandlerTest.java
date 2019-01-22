package org.alfresco.solr.query.afts.requestHandler;

import org.alfresco.solr.query.afts.TestDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;

public class AFTSRequestHandlerTest extends BaseRequestHandlerTest
{
    private static TestDataProvider DATASETS_PROVIDER;

    @BeforeClass
    public static void loadData() throws Exception
    {
        DATASETS_PROVIDER = new TestDataProvider(h);
        DATASETS_PROVIDER.loadTestSet();
        DATASETS_PROVIDER.loadSecondDataSet();

        ftsTestDate = DATASETS_PROVIDER.getFtsTestDate();
        testNodeRef = DATASETS_PROVIDER.getTestNodeRef();
        testRootNodeRef = DATASETS_PROVIDER.getRootNode();
    }

    @Test
    public void paging()
    {
        assertPage("PATH:\"//.\"",
                "DBID asc",
                16,
                1000000,
                0,
                new int[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 });

        assertPage("PATH:\"//.\"",
                "DBID asc",
                16,
                20,
                0,
                new int [] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 });

        assertPage("PATH:\"//.\"",
                "DBID asc",
                6,
                6,
                0,
                new int [] { 1, 2, 3, 4, 5, 6 });

        assertPage("PATH:\"//.\"",
                "DBID asc",
                6,
                6,
                6,
                new int [] { 7, 8, 9, 10, 11, 12 });

        assertPage("PATH:\"//.\"",
                "DBID asc",
                4,
                6,
                12,
                new int[] { 13, 14, 15, 16 });
    }

    @Test
    public void checkAncestor()
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:0*"), null),
                "*[count(//doc)=15]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:0/" + testRootNodeRef.getId()), null),
                "*[count(//doc)=15]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:F/" + testRootNodeRef.getId()), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:0/" + testRootNodeRef.getId()+"*"), null),
                "*[count(//doc)=15]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:0/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:1/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=11]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:2/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=8]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:3/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:4/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:5/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:6/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:1/" + testRootNodeRef.getId()+"/" + DATASETS_PROVIDER.getNode01().getId()), null),
                "*[count(//doc)=9]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:1/" + testRootNodeRef.getId()+"/" + DATASETS_PROVIDER.getNode01().getId()), null),
                "*[count(//doc)=9]");


        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:0/" + testRootNodeRef.getId()), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:F/" + testRootNodeRef.getId()), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:1/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=5]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:2/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:3/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:4/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=3]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:5/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:5/"+testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:6/" + testRootNodeRef.getId()+"/*"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:0/" + DATASETS_PROVIDER.getNode01().getId()), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:0/" + DATASETS_PROVIDER.getNode02().getId()), null),
                "*[count(//doc)=3]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:0/" + DATASETS_PROVIDER.getNode03().getId()), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:1/" + testRootNodeRef.getId()+"/" + DATASETS_PROVIDER.getNode01().getId()), null),
                "*[count(//doc)=2]");
    }
}