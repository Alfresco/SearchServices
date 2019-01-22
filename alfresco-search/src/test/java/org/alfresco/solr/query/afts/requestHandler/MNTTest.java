package org.alfresco.solr.query.afts.requestHandler;

import org.alfresco.solr.query.afts.SharedTestDataProvider;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * Test case which groups all tests related to a maintenance ticket.
 *
 * @author Andrea Gazzarini
 */
public class MNTTest extends AbstractRequestHandlerTest
{
    @BeforeClass
    public static void loadData() throws Exception
    {
        SharedTestDataProvider dataProvider = new SharedTestDataProvider(h);
        dataProvider.loadMntTestData();
    }

    /**
     * Search issues when using lucene queries related to tokenization of file names.
     */
    @Test
    public void mnt15039()
    {
        assertResponseCardinality("cm:name:\"one_two\"", 3);
        assertResponseCardinality("cm:name:\"Print\"", 2);
        assertResponseCardinality("cm:name:\"Print-Toolkit\"", 2);
        assertResponseCardinality("cm:name:\"Print-Toolkit-3204\"", 1);
        assertResponseCardinality("cm:name:\"Print-Toolkit-3204-The-Print-Toolkit-has-a-new-look-565022.html\"", 1);
        assertResponseCardinality("cm:name:\"*20150911100000*\"", 1);
    }

    /**
     * AFTS search using a wildcard to replace no, one or more characters in a phrase
     */
    @Test
    public void mnt15222()
    {
        assertResponseCardinality("cm:name:\"apple pear peach 20150911100000.txt\"", 1);
        assertResponseCardinality("cm:name:\"apple pear * 20150911100000.txt\"", 1);
        assertResponseCardinality("cm:name:\"apple * * 20150911100000.txt\"", 1);
        assertResponseCardinality("cm:name:\"apple * 20150911100000.txt\"", 1);
    }

    /**
     * Solr search behavior on portion of file name plus extension is different on 5.1.1 when compared to 5.0.3
     */
    @Test
    public void mnt16741()
    {
        assertResponseCardinality("cm:name:\"hello.txt\"", 2);
        assertResponseCardinality("cm:name:\"Test.hello.txt\"", 1);
        assertResponseCardinality("cm:name:\"Test1.hello.txt\"", 1);
    }

    /**
     * FTS for phrases that used to work in Solr 4 are no longer working in Solr 6.
     */
    @Test
    public void mnt19714()
    {
      assertResponseCardinality("\"AnalystName Craig\"", 1);
      assertResponseCardinality("\"AnalystName\" AND \"AnalystName Craig\"", 1);
      assertResponseCardinality("\"AnalystName\" AND !\"AnalystName Craig\"", 1);
      assertResponseCardinality("cm:name:\"BASF*.txt\"", 4);
    }
}