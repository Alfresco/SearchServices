package org.alfresco.solr.query.afts.requestHandler;

import static java.util.stream.LongStream.range;
import static org.alfresco.model.ContentModel.ASSOC_CHILDREN;
import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.alfresco.model.ContentModel.PROP_NAME;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.solr.query.afts.TestDataProvider;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;

@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AFTSRequestHandlerTest extends AbstractRequestHandlerTest implements QueryConstants
{
    private static TestDataProvider DATASETS_PROVIDER;

    @BeforeClass
    public static void loadData() throws Exception
    {
        DATASETS_PROVIDER = new TestDataProvider(h);
        DATASETS_PROVIDER.loadSmallDataset();
        DATASETS_PROVIDER.loadMediumDataset();

        FTS_TEST_DATE = DATASETS_PROVIDER.getFtsTestDate();
        TEST_NODEREF = DATASETS_PROVIDER.getTestNodeRef();
        TEST_ROOT_NODEREF = DATASETS_PROVIDER.getRootNode();
    }

    @Test
    public void ancestorField()
    {
        assertResponseCardinality(FIELD_ANCESTOR, "\"" + DATASETS_PROVIDER.getTestNodeRef() + "\"", 10);
    }

    @Test
    public void qNameField()
    {
        assertResponseCardinality(FIELD_QNAME, "\"cm:one\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:two\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:three\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:four\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:five\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:six\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:seven\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:eight-0\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:eight-1\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:eight-2\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:nine\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:ten\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:eleven\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:twelve\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:thirteen\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:fourteen\"", 2);
        assertResponseCardinality(FIELD_QNAME, "\"cm:fifteen\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:common\"", 1);
        assertResponseCardinality(FIELD_QNAME, "\"cm:link\"", 1);
    }

    @Test
    public void primaryAssociationTypeQNameField()
    {
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:one\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:two\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:three\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:four\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:five\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:six\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:seven\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:eight-0\"", 0);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:eight-1\"", 0);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:eight-2\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:nine\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:ten\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:eleven\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:twelve\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:thirteen\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:fourteen\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:fifteen\"", 1);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:common\"", 0);
        assertResponseCardinality(FIELD_PRIMARYASSOCQNAME, "\"cm:link\"", 0);
        assertResponseCardinality(FIELD_PRIMARYASSOCTYPEQNAME, "\"" + ASSOC_CHILDREN.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 4);
    }

    @Test
    public void isNodeField()
    {
        assertResponseCardinality(FIELD_ISNODE, "T", 16);

    }

    @Test
    public void associationTypeQNameField()
    {
        assertResponseCardinality(FIELD_ASSOCTYPEQNAME, "\"" + ASSOC_CHILDREN.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 5);
    }

    @Test
    public void primaryParentField()
    {

        assertResponseCardinality(FIELD_PRIMARYPARENT, "\"" + TEST_NODEREF + "\"", 2);
    }

    @Test
    public void ftsStatusField()
    {
        assertResponseCardinality(FIELD_FTSSTATUS, "\"New\"", 2);
    }

    @Test
    public void dbIdField()
    {
        assertResponseCardinality(FIELD_DBID, "1", 1);
        assertResponseCardinality(FIELD_DBID, "2", 1);
        assertResponseCardinality(FIELD_DBID, "3", 1);
        assertResponseCardinality(FIELD_DBID, "4", 1);
        assertResponseCardinality(FIELD_DBID, "5", 1);
        assertResponseCardinality(FIELD_DBID, "6", 1);
        assertResponseCardinality(FIELD_DBID, "7", 1);
        assertResponseCardinality(FIELD_DBID, "8", 1);
        assertResponseCardinality(FIELD_DBID, "9", 1);
        assertResponseCardinality(FIELD_DBID, "10", 1);
        assertResponseCardinality(FIELD_DBID, "11", 1);
        assertResponseCardinality(FIELD_DBID, "12", 1);
        assertResponseCardinality(FIELD_DBID, "13", 1);
        assertResponseCardinality(FIELD_DBID, "14", 1);
        assertResponseCardinality(FIELD_DBID, "15", 1);
        assertResponseCardinality(FIELD_DBID, "16", 1);
        assertResponseCardinality(FIELD_DBID, "17", 0);
    }

    @Test
    public void authorityFilterEnabled()
    {
        checkAuthorityFilter(true);
    }

    @Test
    public void authorityFilterDisabled()
    {
        checkAuthorityFilter(false);
    }

    @Test
    public void ownerField()
    {
        assertResponseCardinality( FIELD_OWNER, "andy", 1);
        assertResponseCardinality( FIELD_OWNER, "bob", 1);
        assertResponseCardinality( FIELD_OWNER, "cid", 1);
        assertResponseCardinality( FIELD_OWNER, "dave", 1);
        assertResponseCardinality( FIELD_OWNER, "eoin", 1);
        assertResponseCardinality( FIELD_OWNER, "fred", 1);
        assertResponseCardinality( FIELD_OWNER, "gail", 1);
        assertResponseCardinality( FIELD_OWNER, "hal", 1);
        assertResponseCardinality( FIELD_OWNER, "ian", 1);
        assertResponseCardinality( FIELD_OWNER, "jake", 1);
        assertResponseCardinality( FIELD_OWNER, "kara", 1);
        assertResponseCardinality( FIELD_OWNER, "loon", 1);
        assertResponseCardinality( FIELD_OWNER, "mike", 1);
        assertResponseCardinality( FIELD_OWNER, "noodle", 1);
        assertResponseCardinality( FIELD_OWNER, "ood", 1);
    }

    @Test
    public void parentAssociationCrcField()
    {
        assertResponseCardinality( FIELD_PARENT_ASSOC_CRC, "0", 16);
    }

    @Test
    public void txIdFields()
    {
        assertResponseCardinality(FIELD_TXID, "1", 1);
        assertResponseCardinality(FIELD_INTXID, "1", 17);
        assertResponseCardinality(FIELD_ACLTXID, "1", 1);
        assertResponseCardinality(FIELD_INACLTXID, "1", 2);
        assertResponseCardinality(FIELD_INACLTXID, "2", 0);
    }

    @Test
    public void text()
    {
        assertResponseCardinality("\"lazy\"", 1);
        assertResponseCardinality("lazy and dog", 1);
        assertResponseCardinality("-lazy and -dog", 15);
        assertResponseCardinality("|lazy and |dog", 1);
        assertResponseCardinality("|eager and |dog", 1);
        assertResponseCardinality("|lazy and |wolf", 1);
        assertResponseCardinality("|eager and |wolf", 0);
        assertResponseCardinality("-lazy or -dog", 15);
        assertResponseCardinality("-eager or -dog", 16);
        assertResponseCardinality("-lazy or -wolf", 16);
        assertResponseCardinality("-eager or -wolf", 16);
        assertResponseCardinality("lazy dog", 1);
        assertResponseCardinality("lazy and not dog", 0);
        assertResponseCardinality("lazy not dog", 16);
        assertResponseCardinality("lazy and !dog", 0);
        assertResponseCardinality("lazy !dog", 16);
        assertResponseCardinality("lazy and -dog", 0);
        assertResponseCardinality("lazy -dog", 16);
        assertResponseCardinality("TEXT:\"lazy\"", 1);
        assertResponseCardinality("cm_content:\"lazy\"", 1);
        assertResponseCardinality("d:content:\"lazy\"", 1);
        assertResponseCardinality("=cm_content:\"lazy\"", 1);
        assertResponseCardinality("~cm_content:\"lazy\"", 1);
        assertResponseCardinality("cm:content:big OR cm:content:lazy", 1);
        assertResponseCardinality("cm:content:big AND cm:content:lazy", 0);
        assertResponseCardinality("{http://www.alfresco.org/model/content/1.0}content:\"lazy\"", 1);
        assertResponseCardinality("=lazy", 1);
        assertResponseCardinality("@cm:content:big OR @cm:content:lazy", 1);
        assertResponseCardinality("@cm:content:big AND @cm:content:lazy", 0);
        assertResponseCardinality("@{http://www.alfresco.org/model/content/1.0}content:\"lazy\"", 1);
        assertResponseCardinality("~@cm:content:big OR ~@cm:content:lazy", 1);
        assertResponseCardinality("brown * quick", 0);
        assertResponseCardinality("brown * dog", 1);
        assertResponseCardinality("brown *(0) dog", 0);
        assertResponseCardinality("brown *(1) dog", 0);
        assertResponseCardinality("brown *(2) dog", 0);
        assertResponseCardinality("brown *(3) dog", 0);
        assertResponseCardinality("brown *(4) dog", 0);
        assertResponseCardinality("brown *(5) dog", 1);
        assertResponseCardinality("brown *(6) dog", 1);
        assertResponseCardinality("TEXT:(\"lazy\")", 1);
        assertResponseCardinality("TEXT:(lazy and dog)", 1);
        assertResponseCardinality("TEXT:(-lazy and -dog)", 15);
        assertResponseCardinality("TEXT:(-lazy and dog)", 0);
        assertResponseCardinality("TEXT:(lazy and -dog)", 0);
        assertResponseCardinality("TEXT:(|lazy and |dog)", 1);
        assertResponseCardinality("TEXT:(|eager and |dog)", 1);
        assertResponseCardinality("TEXT:(|lazy and |wolf)", 1);
        assertResponseCardinality("TEXT:(|eager and |wolf)", 0);
        assertResponseCardinality("TEXT:(-lazy or -dog)", 15);
        assertResponseCardinality("TEXT:(-eager or -dog)", 16);
        assertResponseCardinality("TEXT:(-lazy or -wolf)", 16);
        assertResponseCardinality("TEXT:(-eager or -wolf)", 16);
        assertResponseCardinality("TEXT:(lazy dog)", 1);
        assertResponseCardinality("TEXT:(lazy and not dog)", 0);
        assertResponseCardinality("TEXT:(lazy not dog)", 16);
        assertResponseCardinality("TEXT:(lazy and !dog)", 0);
        assertResponseCardinality("TEXT:(lazy !dog)", 16);
        assertResponseCardinality("TEXT:(lazy and -dog)", 0);
        assertResponseCardinality("TEXT:(lazy -dog)", 16);
        assertResponseCardinality("cm_content:(\"lazy\")", 1);
        assertResponseCardinality("cm:content:(big OR lazy)", 1);
        assertResponseCardinality("cm:content:(big AND lazy)", 0);
        assertResponseCardinality("{http://www.alfresco.org/model/content/1.0}content:(\"lazy\")", 1);
        assertResponseCardinality("TEXT:(=lazy)", 1);
        assertResponseCardinality("@cm:content:(big) OR @cm:content:(lazy)", 1);
        assertResponseCardinality("@cm:content:(big) AND @cm:content:(lazy)", 0);
        assertResponseCardinality("@{http://www.alfresco.org/model/content/1.0}content:(\"lazy\")", 1);
        assertResponseCardinality("@cm:content:(~big OR ~lazy)", 1);
        assertResponseCardinality("TEXT:(brown * quick)", 0);
        assertResponseCardinality("TEXT:(brown * dog)", 1);
        assertResponseCardinality("TEXT:(brown *(0) dog)", 0);
        assertResponseCardinality("TEXT:(brown *(1) dog)", 0);
        assertResponseCardinality("TEXT:(brown *(2) dog)", 0);
        assertResponseCardinality("TEXT:(brown *(3) dog)", 0);
        assertResponseCardinality("TEXT:(brown *(4) dog)", 0);
        assertResponseCardinality("TEXT:(brown *(5) dog)", 1);
        assertResponseCardinality("TEXT:(brown *(6) dog)", 1);
    }

    @Test
    public void ranges()
    {
        String fieldName = QName.createQName(TEST_NAMESPACE, "float\\-ista").toString();

        assertResponseCardinality(fieldName, "3.40", 1);
        assertResponseCardinality(fieldName, "3..4", 1);
        assertResponseCardinality(fieldName, "3..3.39", 0);
        assertResponseCardinality(fieldName, "3..3.40", 1);
        assertResponseCardinality(fieldName, "3.41..3.9", 0);
        assertResponseCardinality(fieldName, "3.40..3.9", 1);

        assertResponseCardinality(fieldName, "[3 TO 4]", 1);
        assertResponseCardinality(fieldName, "[3 TO 3.39]", 0);
        assertResponseCardinality(fieldName, "[3 TO 3.4]", 1);
        assertResponseCardinality(fieldName, "[3.41 TO 4]", 0);
        assertResponseCardinality(fieldName, "[3.4 TO 4]", 1);
        assertResponseCardinality(fieldName, "[3 TO 3.4>", 0);
        assertResponseCardinality(fieldName, "<3.4 TO 4]", 0);
        assertResponseCardinality(fieldName, "<3.4 TO 3.4>", 0);

        assertResponseCardinality(fieldName, "(3.40)", 1);
        assertResponseCardinality(fieldName, "(3..4)", 1);
        assertResponseCardinality(fieldName, "(3..3.39)", 0);
        assertResponseCardinality(fieldName, "(3..3.40)", 1);
        assertResponseCardinality(fieldName, "(3.41..3.9)", 0);
        assertResponseCardinality(fieldName, "(3.40..3.9)", 1);

        assertResponseCardinality(fieldName, "([3 TO 4])", 1);
        assertResponseCardinality(fieldName, "([3 TO 3.39])", 0);
        assertResponseCardinality(fieldName, "([3 TO 3.4])", 1);
        assertResponseCardinality(fieldName, "([3.41 TO 4])", 0);
        assertResponseCardinality(fieldName, "([3.4 TO 4])", 1);
        assertResponseCardinality(fieldName, "([3 TO 3.4>)", 0);
        assertResponseCardinality(fieldName, "(<3.4 TO 4])", 0);
        assertResponseCardinality(fieldName, "(<3.4 TO 3.4>)", 0);

        assertResponseCardinality("test:float_x002D_ista","3.40", 1);
    }

    @Test
    public void txCommitTimeFields()
    {
        assertQ(areq(params("rows", "20", "qt", "/native", "q", FIELD_TXCOMMITTIME + ":*"), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/native", "q", FIELD_ACLTXCOMMITTIME + ":*"), null), "*[count(//doc)=1]");

// TODO: with native
 //       assertResponseCardinality(FIELD_TXCOMMITTIME, "*", 1);
//        assertResponseCardinality(FIELD_ACLTXCOMMITTIME, "*", 1);
    }

    @Test
    public void aclIdField()
    {
        assertResponseCardinality(FIELD_ACLID, "1", 17);
    }

    @Test
    public void readerField()
    {
        assertResponseCardinality( FIELD_READER, "\"GROUP_EVERYONE\"", 16);
    }

    @Test
    public void idField()
    {
        range(1, 16)
                .mapToObj(dbId -> escape(AlfrescoSolrDataModel.getNodeDocumentId(AlfrescoSolrDataModel.DEFAULT_TENANT, 1L, dbId)))
                .forEach(id -> assertResponseCardinality(FIELD_SOLR4_ID, id, 1));
    }

    @Test
    public void docTypeField()
    {
        assertResponseCardinality(FIELD_DOC_TYPE, "" + SolrInformationServer.DOC_TYPE_NODE, 16);
        assertResponseCardinality(FIELD_DOC_TYPE, "" + SolrInformationServer.DOC_TYPE_ACL, 1);
        assertResponseCardinality(FIELD_DOC_TYPE, "" + SolrInformationServer.DOC_TYPE_ACL_TX, 1);
        assertResponseCardinality(FIELD_DOC_TYPE, "" + SolrInformationServer.DOC_TYPE_TX, 1);
    }

    @Test
    public void parentField()
    {
        assertResponseCardinality(FIELD_PARENT, "\"" + DATASETS_PROVIDER.getTestNodeRef() + "\"", 4);
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
    public void mimetypes()
    {
        assertResponseCardinality("cm:content.mimetype:\"text/plain\"", 1);
        assertResponseCardinality("cm_content.mimetype:\"text/plain\"", 1);
        assertResponseCardinality("@cm_content.mimetype:\"text/plain\"", 1);
        assertResponseCardinality("content.mimetype:\"text/plain\"", 1);
        assertResponseCardinality("@{http://www.alfresco.org/model/content/1.0}content.mimetype:\"text/plain\"", 1);
        assertResponseCardinality("{http://www.alfresco.org/model/content/1.0}content.mimetype:\"text/plain\"", 1);
    }

    @Test
    public void lazy()
    {
        assertResponseCardinality("lazy", 1);
        assertResponseCardinality("laz*", 1);
        assertResponseCardinality("l*y", 1);
        assertResponseCardinality("l??y", 1);
        assertResponseCardinality("?az?", 1);
        assertResponseCardinality("*zy", 1);

        assertResponseCardinality("\"lazy\"", 1);
        assertResponseCardinality("\"laz*\"", 1);
        assertResponseCardinality("\"l*y\"", 1);
        assertResponseCardinality("\"l??y\"", 1);
        assertResponseCardinality("\"?az?\"", 1);
        assertResponseCardinality("\"*zy\"", 1);

        assertResponseCardinality("cm:content","lazy", 1);
        assertResponseCardinality("cm:content","laz*", 1);
        assertResponseCardinality("cm:content","l*y", 1);
        assertResponseCardinality("cm:content","l??y", 1);
        assertResponseCardinality("cm:content","?az?", 1);
        assertResponseCardinality("cm:content","*zy", 1);

        assertResponseCardinality("cm:content","\"lazy\"", 1);
        assertResponseCardinality("cm:content","\"laz*\"", 1);
        assertResponseCardinality("cm:content","\"l*y\"", 1);
        assertResponseCardinality("cm:content","\"l??y\"", 1);
        assertResponseCardinality("cm:content","\"?az?\"", 1);
        assertResponseCardinality("cm:content","\"*zy\"", 1);

        assertResponseCardinality("cm:content","(lazy)", 1);
        assertResponseCardinality("cm:content","(laz*)", 1);
        assertResponseCardinality("cm:content","(l*y)", 1);
        assertResponseCardinality("cm:content","(l??y)", 1);
        assertResponseCardinality("cm:content","(?az?)", 1);
        assertResponseCardinality("cm:content","(*zy)", 1);

        assertResponseCardinality("cm:content","(\"lazy\")", 1);
        assertResponseCardinality("cm:content","(\"laz*\")", 1);
        assertResponseCardinality("cm:content","(\"l*y\")", 1);
        assertResponseCardinality("cm:content","(\"l??y\")", 1);
        assertResponseCardinality("cm:content","(\"?az?\")", 1);
        assertResponseCardinality("cm:content","(\"*zy\")", 1);

        assertResponseCardinality("lazy^2 dog^4.2", 1);
        assertResponseCardinality("lazy~0.7", 1);
        assertResponseCardinality("cm:content","laxy~0.7", 1);
        assertResponseCardinality("laxy~0.7", 1);
        assertResponseCardinality("=laxy~0.7", 1);
        assertResponseCardinality("~laxy~0.7", 1);

        assertResponseCardinality("\"quick fox\"~0", 0);
        assertResponseCardinality("\"quick fox\"~1", 1);
        assertResponseCardinality("\"quick fox\"~2", 1);
        assertResponseCardinality("\"quick fox\"~3", 1);

        assertResponseCardinality("\"fox quick\"~0", 0);
        assertResponseCardinality("\"fox quick\"~1", 0);
        assertResponseCardinality("\"fox quick\"~2", 1);
        assertResponseCardinality("\"fox quick\"~3", 1);

        assertResponseCardinality("lazy", 1);
        assertResponseCardinality("-lazy", 15);
        assertResponseCardinality("lazy -lazy", 16);
        assertResponseCardinality("lazy^20 -lazy", 16);
        assertResponseCardinality("lazy^20 -lazy^20", 16);

        assertResponseCardinality("cm:content","lazy", 1);
        assertResponseCardinality("content","lazy", 1);
        assertResponseCardinality("PATH","\"//.\"", 16);
        assertResponseCardinality("+PATH","\"/app:company_home/st:sites/cm:rmtestnew1/cm:documentLibrary//*\"", 0);
        assertResponseCardinality("+PATH","\"/app:company_home/st:sites/cm:rmtestnew1/cm:documentLibrary//*\" -TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\"", 15);
        assertResponseCardinality("+PATH", "\"/app:company_home/st:sites/cm:rmtestnew1/cm:documentLibrary//*\" AND -TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\"", 0);

        assertResponseCardinality("(brown *(6) dog)", 1);
        assertResponseCardinality("TEXT","(brown *(6) dog)", 1);
        assertResponseCardinality("\"//.\"", 0);
        assertResponseCardinality("cm:content","brown", 1);

        assertResponseCardinality("modified", "*", 2);
        assertResponseCardinality("modified","[MIN TO NOW]", 2);

        assertResponseCardinality("TYPE", TEST_TYPE.toString(), 1);
        assertResponseCardinality("TYPE", TEST_TYPE.toString(), "mimetype():document", 0);

        assertResponseCardinality("TYPE", ContentModel.TYPE_CONTENT.toString(), 1);
        assertResponseCardinality("TYPE", ContentModel.TYPE_CONTENT.toString(), "mimetype():document", 1);
        assertResponseCardinality("TYPE", ContentModel.TYPE_CONTENT.toString(), "mimetype():\"text/plain\"", 1);
        assertResponseCardinality("TYPE", ContentModel.TYPE_CONTENT.toString(), "contentSize():[0 TO 100]", 0);
        assertResponseCardinality("TYPE", ContentModel.TYPE_CONTENT.toString(), "contentSize():[100 TO 1000]", 1);

        assertResponseCardinality("modified","[NOW/DAY-1DAY TO NOW/DAY+1DAY]", 2);
        assertResponseCardinality("modified","[NOW/DAY-1DAY TO *]", 2);
        assertResponseCardinality("modified","[* TO NOW/DAY+1DAY]", 2);
        assertResponseCardinality("modified","[* TO *]", 2);
    }

    @Test
    public void synonyms()
    {
        assertResponseCardinality("quick", 1);
        assertResponseCardinality("fast", 1);
        assertResponseCardinality("rapid", 1);
        assertResponseCardinality("speedy", 1);

        assertResponseCardinality("\"brown fox jumped\"", 1);

        assertResponseCardinality("\"leaping reynard\"", 1);
        assertResponseCardinality("\"springer\"", 1);

        assertResponseCardinality("lazy", 1);
        assertResponseCardinality("\"bone idle\"", 1);
    }

    @Test
    public void sort()
    {
        assertAQueryIsSorted("PATH:\"//.\"",
                "@" + PROP_CONTENT + ".size asc",
                null,
                16,
                new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });

        assertAQueryIsSorted("PATH:\"//.\"",
                "@" + PROP_CONTENT + ".size desc",
                null,
                16,
                new Integer[] { 15});

        assertAQueryIsSorted("PATH:\"//.\"",
                PROP_CONTENT + ".size asc",
                null,
                16,
                new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });

        assertAQueryIsSorted("PATH:\"//.\"",
                PROP_CONTENT.toString() + ".size desc",
                null,
                16,
                new Integer[] { 15});

        assertAQueryIsSorted("PATH:\"//.\"",
                "@cm:content.size asc",
                null,
                16,
                new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });

        assertAQueryIsSorted("PATH:\"//.\"",
                "@cm:content.size desc",
                null,
                16,
                new Integer[] { 15 });

        assertAQueryIsSorted("PATH:\"//.\"",
                "cm:content.size asc",
                null,
                16,
                new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });

        assertAQueryIsSorted("PATH:\"//.\"",
                "cm:content.size desc",
                null,
                16,
                new Integer[] {15});

        assertAQueryIsSorted("PATH:\"//.\"",
                "@content.size asc",
                null,
                16,
                new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });

        assertAQueryIsSorted("PATH:\"//.\"",
                "@content.size desc",
                null,
                16,
                new Integer[] {15});

        assertAQueryIsSorted("PATH:\"//.\"",
                "content.size asc",
                null,
                16,
                new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });

        assertAQueryIsSorted("PATH:\"//.\"",
                "content.size desc",
                null,
                16,
                new Integer[] {15});

        assertAQueryIsSorted("-eager or -dog",
                "@" + PROP_NAME.toString()+ " asc",
                null,
                16,
                new Integer[] { 1, 9, 12, 16, 6, 5, 15, 10, 2, 8, 7, 11, 14, 4, 13, 3 });


        assertAQueryIsSorted("-eager or -dog",
                "@" + PROP_NAME.toString() + " desc",
                null,
                16,
                new Integer[] { 3, 13, 4, 14, 11, 7, 8, 2, 10, 15, 5, 6, 16, 12, 9, 1 });

        assertAQueryIsSorted("-eager or -dog",
                PROP_NAME.toString() + " asc",
                null,
                16,
                new Integer[] { 1, 9, 12, 16, 6, 5, 15, 10, 2, 8, 7, 11, 14, 4, 13, 3 });

        assertAQueryIsSorted("-eager or -dog",
                PROP_NAME.toString() + " desc",
                null,
                16,
                new Integer[] { 3, 13, 4, 14, 11, 7, 8, 2, 10, 15, 5, 6, 16, 12, 9, 1 });

        assertAQueryIsSorted("-eager or -dog",
                "@cm:name asc",
                null,
                16,
                new Integer[] { 1, 9, 12, 16, 6, 5, 15, 10, 2, 8, 7, 11, 14, 4, 13, 3 });

        assertAQueryIsSorted("-eager or -dog",
                "@cm:name desc",
                null,
                16,
                new Integer[] { 3, 13, 4, 14, 11, 7, 8, 2, 10, 15, 5, 6, 16, 12, 9, 1 });

        assertAQueryIsSorted("-eager or -dog",
                "cm:name asc",
                null,
                16,
                new Integer[] { 1, 9, 12, 16, 6, 5, 15, 10, 2, 8, 7, 11, 14, 4, 13, 3 });

        assertAQueryIsSorted("-eager or -dog",
                "cm:name desc",
                null,
                16,
                new Integer[] { 3, 13, 4, 14, 11, 7, 8, 2, 10, 15, 5, 6, 16, 12, 9, 1 });
        assertAQueryIsSorted("-eager or -dog",
                "cm_name desc",
                null,
                16,
                new Integer[] { 3, 13, 4, 14, 11, 7, 8, 2, 10, 15, 5, 6, 16, 12, 9, 1 });
        assertAQueryIsSorted("-eager or -dog",
                "@name asc",
                null,
                16,
                new Integer[] { 1, 9, 12, 16, 6, 5, 15, 10, 2, 8, 7, 11, 14, 4, 13, 3 });

        assertAQueryIsSorted("-eager or -dog",
                "@name desc",
                null,
                16,
                new Integer[] { 3, 13, 4, 14, 11, 7, 8, 2, 10, 15, 5, 6, 16, 12, 9, 1 });

        assertAQueryIsSorted("-eager or -dog",
                "name asc",
                null,
                16,
                new Integer[] { 1, 9, 12, 16, 6, 5, 15, 10, 2, 8, 7, 11, 14, 4, 13, 3 });

        assertAQueryIsSorted("-eager or -dog",
                "name desc",
                null,
                16,
                new Integer[] { 3, 13, 4, 14, 11, 7, 8, 2, 10, 15, 5, 6, 16, 12, 9, 1 });

        assertAQueryIsSorted("PATH:\"//.\"",
                "DBID asc",
                null,
                16,
                new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 });

        assertAQueryIsSorted("PATH:\"//.\"",
                "DBID desc",
                null,
                16,
                new Integer[] { 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 });

        assertAQueryIsSorted("PATH:\"//.\"", "ID asc", null, 16, new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 });
        assertAQueryIsSorted("PATH:\"//.\"", "ID desc",null, 16, new Integer[] { 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 });

        assertAQueryIsSorted("PATH:\"//.\"", "@" + CREATED_DATE + " asc", null, 16, new Integer[]{1, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + CREATED_DATE + " desc", null, 16, new Integer[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + CREATED_TIME + " asc", null, 16, new Integer[]{1, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + CREATED_TIME + " desc", null, 16, new Integer[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1});

        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_DOUBLE + " asc", null, 16, new Integer[]{15, 13, 11, 9, 7, 5, 3, 2, 1, 4, 6, 8, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_DOUBLE + " desc", null, 16, new Integer[]{16, 14, 12, 10, 8, 6, 4, 1, 2, 3, 5, 7, 9, 11, 13, 15});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_FLOAT + " asc", null, 16, new Integer[]{15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 1, 8, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_FLOAT + " desc", null, 16, new Integer[]{16, 14, 12, 10, 8, 1, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LONG + " asc", null, 16, new Integer[]{15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 8, 1, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LONG + " desc", null, 16, new Integer[]{16, 14, 12, 10, 1, 8, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_INT + " asc", null, 16, new Integer[]{15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 1, 8, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_INT + " desc", null, 16, new Integer[]{16, 14, 12, 10, 8, 1, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15});

        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_TEXT + " asc", null, 16, new Integer[]{1, 15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 8, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_TEXT + " desc", null, 16, new Integer[]{16, 14, 12, 10, 8, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15, 1});

        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_TEXT + " asc",  Locale.ENGLISH, 16, new Integer[] {1, 10, 11, 2, 3, 4, 5, 13, 12, 6, 7, 8, 14, 15, 16, 9 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_TEXT + " desc", Locale.ENGLISH, 16, new Integer[] {9, 16, 15, 14, 8, 7, 6, 12, 13, 5, 4, 3, 2, 11, 10, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_TEXT + " asc",  Locale.FRENCH,  16, new Integer[]  {1, 10, 11, 2, 3, 4, 5, 13, 12, 6, 8, 7, 14, 15, 16, 9 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_TEXT + " desc", Locale.FRENCH,  16, new Integer[]  {9, 16, 15, 14, 7, 8, 6, 12, 13, 5, 4, 3, 2, 11, 10, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_TEXT + " asc",  Locale.GERMAN, 16, new Integer[]  {1, 10, 11, 2, 3, 4, 5, 13, 12, 6, 7, 8, 14, 15, 16, 9 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_TEXT + " desc", Locale.GERMAN, 16, new Integer[]  {9, 16, 15, 14, 8, 7, 6, 12, 13, 5, 4, 3, 2, 11, 10, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_TEXT + " asc",  new Locale("sv"), 16, new Integer[] {1, 11, 2, 3, 4, 5, 13, 6, 7, 8, 12, 14, 15, 16, 9, 10 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_TEXT + " desc", new Locale("sv"), 16, new Integer[] {10, 9, 16, 15, 14, 12, 8, 7, 6, 13, 5, 4, 3, 2, 11, 1 });

        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_ML_TEXT + " asc",  Locale.ENGLISH, 16, new Integer[] { 1, 15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 8, 10, 12, 14, 16 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_ML_TEXT + " desc", Locale.ENGLISH, 16, new Integer[] { 16, 14, 12, 10, 8, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15, 1});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_ML_TEXT + " asc",  Locale.FRENCH, 16,  new Integer[] { 1, 14, 16, 12, 10, 8, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_ML_TEXT + " desc", Locale.FRENCH, 16,  new Integer[] { 15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 8, 10, 12, 16, 14, 1 });

        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_ML_TEXT + " asc",  Locale.ENGLISH, 16, new Integer[] { 1, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_ML_TEXT + " desc", Locale.ENGLISH, 16, new Integer[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_ML_TEXT + " asc",  Locale.FRENCH, 16, new Integer[] { 1, 16, 15, 14, 13, 12, 2, 3, 4, 5, 11, 10, 9, 8, 7, 6 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_ML_TEXT + " desc", Locale.FRENCH, 16, new Integer[] { 6, 7, 8, 9, 10, 11, 5, 4, 3, 2, 12, 13, 14, 15, 16, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_ML_TEXT + " asc",  Locale.GERMAN, 16, new Integer[] { 1, 16, 15, 2, 3, 4, 5, 6, 7, 9, 8, 10, 12, 14, 11, 13 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_ML_TEXT + " desc", Locale.GERMAN, 16, new Integer[] { 13, 11, 14, 12, 10, 8, 9, 7, 6, 5, 4, 3, 2, 15, 16, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_ML_TEXT + " asc",  new Locale("es"), 16, new Integer[] { 1, 16, 15, 7, 14, 8, 9, 10, 11, 12, 13, 2, 3, 4, 5, 6 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ORDER_LOCALISED_ML_TEXT + " desc", new Locale("es"), 16, new Integer[] { 6, 5, 4, 3, 2, 13, 12, 11, 10, 9, 8, 14, 7, 15, 16, 1 });

        assertAQueryIsSorted("PATH:\"//.\"", "@" + PROP_CONTENT + ".size asc", null, 16, new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + PROP_CONTENT + ".size desc",null, 16, new Integer[] { 15 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + PROP_CONTENT + ".mimetype asc", null, 16, new Integer[] { 15 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + PROP_CONTENT + ".mimetype desc",null, 16, new Integer[] { 15 });
    }

    private void checkAuthorityFilter(boolean postFilter)
    {
        System.setProperty("alfresco.postfilter", Boolean.toString(postFilter));

        assertResponseCardinality("PATH:\"//.\"", 16);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{!afts}|DENIED:andy", 0);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{!afts}|DENYSET\":andy:bob:cid\"", 0);

        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:andy", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:bob", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:cid", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:dave", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:eoin", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:fred", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:gail", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:hal", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:ian", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:jake", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:kara", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:loon", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:mike", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:noodle", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:ood", 1);

        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:pig", 16);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|READER:pig", 16);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:pig", 0);

        // All nodes point to ACL with ID #1. The ACL explicitly lists "pig" as a READER,
        //        // however, pig does not own any nodes.

        assertResponseCardinality("PATH","\"//.\"", "{!afts}|DENIED:pig", 0);

        // When using the fq parameter for AUTHORITY related filter queries, anyDenyDenies is
        // NOT supported, captured by this test case: something is DENIED, however GROUP_EVERYONE allows it.
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:something |AUTHORITY:GROUP_EVERYONE", 16);

        // "something" has no explicity READER or OWNER entries.
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|READER:something", 0);

        assertResponseCardinality("PATH","\"//.\"", "{!afts}|OWNER:something", 0);

        // "something" is DENIED to all nodes (they all use ACL #1)
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|DENIED:something", 16);

        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"something\", \"GROUP_EVERYONE\" ], \"tenants\": [ \"\" ] }", 0);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"something\" ], \"tenants\": [ \"\" ] }", 0);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"GROUP_EVERYONE\" ], \"tenants\": [ \"\" ] }", 16);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"andy\" ], \"tenants\": [ \"\" ] }", 1);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"andy\", \"GROUP_EVERYONE\" ], \"tenants\": [ \"\" ] }", 16);

        // Even though andy, bob, cid and GROUP_EVERYONE would return docs, "something" still denied.
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"andy\", \"bob\", \"cid\", \"something\", \"GROUP_EVERYONE\" ], \"tenants\": [ \"\" ] }", 0);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"andy\", \"bob\", \"cid\" ], \"tenants\": [ \"\" ] }", 3);

        // Check that generation of filter using AUTHORITY and DENIED works (no DENYSET/AUTHSET separator available)
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"strange:,-!+=;~/\", \"andy\", \"bob\" ], \"tenants\": [ \"\" ] }", 2);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"strange:,-!+=;~/\", \"andy\", \"something\", \"GROUP_EVERYONE\" ], \"tenants\": [ \"\" ] }", 0);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"authorities\": [ \"strange:,-!+=;~/\", \"bob\", \"GROUP_EVERYONE\" ], \"tenants\": [ \"\" ] }", 16);

        // Test any allow allows.
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"anyDenyDenies\":false, \"authorities\": [ \"something\", \"GROUP_EVERYONE\" ], \"tenants\": [ \"\" ] }", 16);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"anyDenyDenies\":false, \"authorities\": [ \"andy\", \"bob\", \"cid\", \"something\" ], \"tenants\": [ \"\" ] }", 3);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"anyDenyDenies\":false, \"authorities\": [ \"something\" ], \"tenants\": [ \"\" ] }", 0);

        // Check that anyDenyDenies:true actually works (code above relies on default of true)
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"anyDenyDenies\":true, \"authorities\": [ \"something\", \"GROUP_EVERYONE\" ], \"tenants\": [ \"\" ] }", 0);

        // Check with AUTHORITY/DENIED rather than AUTHSET/DENYSET
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{ \"anyDenyDenies\":false, \"authorities\": [ \"strange:,-!+=;~/\", \"andy\", \"bob\", \"cid\", \"something\" ], \"tenants\": [ \"\" ] }", 3);


        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:andy", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:bob", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:cid", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:dave", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:eoin", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:fred", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:gail", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:hal", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:ian", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:eoin", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:jake", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:kara", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:loon", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:mike", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:noodle", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:ood", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:GROUP_EVERYONE", 16);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHORITY:andy |AUTHORITY:bob |AUTHORITY:cid", 3);

        assertResponseCardinality("PATH","\"//.\"", 16);

        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":andy\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":bob\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":cid\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":dave\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":eoin\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":fred\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":gail\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":hal\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":ian\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":jake\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":kara\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":loon\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":mike\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":noodle\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":ood\"", 1);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":GROUP_EVERYONE\"", 16);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":andy\" |AUTHSET:\":bob\" |AUTHSET:\":cid\"", 3);
        assertResponseCardinality("PATH","\"//.\"", "{!afts}|AUTHSET:\":andy:bob:cid\"", 3);
    }

    @Test
    public void crossLanguageSupportWithTokenisation()
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:English", "locale", Locale.ENGLISH.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:English123", "locale", Locale.ENGLISH.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:French", "locale", Locale.ENGLISH.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:French123", "locale", Locale.ENGLISH.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:123", "locale", Locale.ENGLISH.toString()), null), "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:English", "locale", Locale.FRENCH.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:English123", "locale", Locale.FRENCH.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:French", "locale", Locale.FRENCH.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:French123", "locale", Locale.FRENCH.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:123", "locale", Locale.FRENCH.toString()), null), "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:English", "locale", Locale.GERMAN.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:English123", "locale", Locale.GERMAN.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:French", "locale", Locale.GERMAN.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:French123", "locale", Locale.GERMAN.toString()), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "title:123", "locale", Locale.GERMAN.toString()), null), "*[count(//doc)=1]");
    }

    @Test
    public void checkAncestor()
    {
        assertResponseCardinality("APATH:0*",15);
        assertResponseCardinality("APATH:0/" + TEST_ROOT_NODEREF.getId(), 15);
        assertResponseCardinality("APATH:F/" + TEST_ROOT_NODEREF.getId(), 4);
        assertResponseCardinality("APATH:0/" + TEST_ROOT_NODEREF.getId()+"*", 15);
        assertResponseCardinality("APATH:0/" + TEST_ROOT_NODEREF.getId()+"/*", 0);
        assertResponseCardinality("APATH:1/" + TEST_ROOT_NODEREF.getId()+"/*", 11);
        assertResponseCardinality("APATH:2/" + TEST_ROOT_NODEREF.getId()+"/*", 8);
        assertResponseCardinality("APATH:3/" + TEST_ROOT_NODEREF.getId()+"/*", 4);
        assertResponseCardinality("APATH:4/" + TEST_ROOT_NODEREF.getId()+"/*", 4);
        assertResponseCardinality("APATH:5/" + TEST_ROOT_NODEREF.getId()+"/*", 1);
        assertResponseCardinality("APATH:6/" + TEST_ROOT_NODEREF.getId()+"/*", 0);
        assertResponseCardinality("APATH:1/" + TEST_ROOT_NODEREF.getId()+"/" + DATASETS_PROVIDER.getNode01().getId(), 9);
        assertResponseCardinality("APATH:1/" + TEST_ROOT_NODEREF.getId()+"/" + DATASETS_PROVIDER.getNode01().getId(), 9);
        assertResponseCardinality("ANAME:0/" + TEST_ROOT_NODEREF.getId(), 4);
        assertResponseCardinality("ANAME:F/" + TEST_ROOT_NODEREF.getId(), 4);
        assertResponseCardinality("ANAME:1/" + TEST_ROOT_NODEREF.getId()+"/*", 5);
        assertResponseCardinality("ANAME:2/" + TEST_ROOT_NODEREF.getId()+"/*", 4);
        assertResponseCardinality("ANAME:3/" + TEST_ROOT_NODEREF.getId()+"/*", 1);
        assertResponseCardinality("ANAME:4/" + TEST_ROOT_NODEREF.getId()+"/*", 3);
        assertResponseCardinality("ANAME:5/" + TEST_ROOT_NODEREF.getId()+"/*", 1);
        assertResponseCardinality("ANAME:5/"+ TEST_ROOT_NODEREF.getId()+"/*", 1);
        assertResponseCardinality("ANAME:6/" + TEST_ROOT_NODEREF.getId()+"/*", 0);
        assertResponseCardinality("ANAME:0/" + DATASETS_PROVIDER.getNode01().getId(), 2);
        assertResponseCardinality("ANAME:0/" + DATASETS_PROVIDER.getNode02().getId(), 3);
        assertResponseCardinality("ANAME:0/" + DATASETS_PROVIDER.getNode03().getId(), 0);
        assertResponseCardinality("ANAME:1/" + TEST_ROOT_NODEREF.getId()+"/" + DATASETS_PROVIDER.getNode01().getId(), 2);
    }
}