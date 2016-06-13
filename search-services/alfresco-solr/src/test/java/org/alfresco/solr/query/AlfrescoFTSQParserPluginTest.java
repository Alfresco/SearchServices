/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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

package org.alfresco.solr.query;

import org.alfresco.model.ContentModel;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrTestCaseJ4;
import org.alfresco.util.SearchLanguageConversion;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Locale;


@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AlfrescoFTSQParserPluginTest extends AlfrescoSolrTestCaseJ4 {

    @BeforeClass
    public static void beforeClass() throws Exception {
        initAlfrescoCore("solrconfig-afts.xml", "schema-afts.xml");
        Thread.sleep(30000);
        loadTestSet();
    }

    @Override
    @Before
    public void setUp() throws Exception {
        // if you override setUp or tearDown, you better callf
        // the super classes version
        super.setUp();
        //clearIndex();
        //assertU(commit());
    }

    /*
    @Test
    public void testAftsQueries() throws Exception {

        //assertU(delQ("*:*"));
        //assertU(commit());

        String[] doc = {"id", "1",  "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc));
        assertU(commit());
        String[] doc1 = {"id", "2", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc1));

        String[] doc2 = {"id", "3", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc2));
        assertU(commit());
        String[] doc3 = {"id", "4", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc3));

        String[] doc4 = {"id", "5", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc4));
        assertU(commit());

        String[] doc5 = {"id", "6", "content@s___t@{http://www.alfresco.org/model/content/1.0}content", "YYYY"};
        assertU(adoc(doc5));
        assertU(commit());


        ModifiableSolrParams params = new ModifiableSolrParams();
        params.add("q", "t1:YYYY");
        params.add("qt", "/afts");
        params.add("start", "0");
        params.add("rows", "6");
        SolrServletRequest req = areq(params, "{\"locales\":[\"en\"], \"templates\": [{\"name\":\"t1\", \"template\":\"%cm:content\"}]}");
        assertQ(req, "*[count(//doc)=6]");

    }
*/
    @Test
    public void dataChecks() throws Exception {
        //Check the root node
        assertAQuery("PATH:\"/\"", 1);
        assertAQuery("PATH:\"/.\"", 1);
        
        //Check paths
        assertAQuery("PATH:\"/cm:one\"", 1);
        assertAQuery("PATH:\"/cm:two\"", 1);
        assertAQuery("PATH:\"/cm:three\"", 1);
        assertAQuery("PATH:\"/cm:four\"", 1);
        assertAQuery("PATH:\"/cm:eight-0\"", 1);
        assertAQuery("PATH:\"/cm:five\"", 0);
        assertAQuery("PATH:\"/cm:one/cm:one\"", 0);
        assertAQuery("PATH:\"/cm:one/cm:two\"", 0);
        assertAQuery("PATH:\"/cm:two/cm:one\"", 0);
        assertAQuery("PATH:\"/cm:two/cm:two\"", 0);
        assertAQuery("PATH:\"/cm:one/cm:five\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:six\"", 1);
        assertAQuery("PATH:\"/cm:two/cm:seven\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:eight-1\"", 1);
        assertAQuery("PATH:\"/cm:two/cm:eight-2\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:eight-2\"", 0);
        assertAQuery("PATH:\"/cm:two/cm:eight-1\"", 0);
        assertAQuery("PATH:\"/cm:two/cm:eight-0\"", 0);
        assertAQuery("PATH:\"/cm:one/cm:eight-0\"", 0);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:nine\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:ten\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:eleven\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve/cm:thirteen\"", 1);
        assertAQuery( 
              "PATH:\"/cm:one/cm:five/cm:twelve/cm:thirteen/cm:fourteen\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve/cm:thirteen/cm:common\"",
                1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve/cm:common\"", 1);
        assertAQuery("PATH:\"/cm:*\"", 5);
        assertAQuery("PATH:\"/cm:*/cm:*\"", 6);
        assertAQuery("PATH:\"/cm:*/cm:five\"", 1);
        assertAQuery("PATH:\"/cm:*/cm:*/cm:*\"", 6);
        assertAQuery("PATH:\"/cm:one/cm:*\"", 4);
        assertAQuery("PATH:\"/cm:*/cm:five/cm:*\"", 5);
        assertAQuery("PATH:\"/cm:one/cm:*/cm:nine\"", 1);
        assertAQuery("PATH:\"/*\"", 5);
        assertAQuery("PATH:\"/*/*\"", 6);
        assertAQuery("PATH:\"/*/cm:five\"", 1);
        assertAQuery("PATH:\"/*/*/*\"", 6);
        assertAQuery("PATH:\"/cm:one/*\"", 4);
        assertAQuery("PATH:\"/*/cm:five/*\"", 5);
        assertAQuery("PATH:\"/cm:one/*/cm:nine\"", 1);
        assertAQuery("PATH:\"//.\"", 16);
        assertAQuery("PATH:\"//*\"", 15);
        assertAQuery("PATH:\"//*/.\"", 15);
        assertAQuery("PATH:\"//*/./.\"", 15);
        assertAQuery("PATH:\"//./*\"", 15);
        assertAQuery("PATH:\"//././*/././.\"", 15);
        assertAQuery("PATH:\"//cm:common\"", 1);
        assertAQuery("PATH:\"/one//common\"", 1);
        assertAQuery("PATH:\"/one/five//*\"", 7);
        assertAQuery("PATH:\"/one/five//.\"", 8);
        assertAQuery("PATH:\"/one//five/nine\"", 1);
        assertAQuery("PATH:\"/one//thirteen/fourteen\"", 1);
        assertAQuery("PATH:\"/one//thirteen/fourteen/.\"", 1);
        assertAQuery("PATH:\"/one//thirteen/fourteen//.\"", 1);
        assertAQuery("PATH:\"/one//thirteen/fourteen//.//.\"", 1);

        assertAQuery("PATH:\"/one\"", 1);
        assertAQuery("PATH:\"/two\"", 1);
        assertAQuery("PATH:\"/three\"", 1);
        assertAQuery("PATH:\"/four\"", 1);
        
        // Check qNames
        assertAQuery("QNAME:\"nine\"", 1);
        assertAQuery("PRIMARYASSOCTYPEQNAME:\"cm:contains\"", 11);
        assertAQuery("PRIMARYASSOCTYPEQNAME:\"sys:children\"", 4);
        assertAQuery("ASSOCTYPEQNAME:\"cm:contains\"", 11);
        assertAQuery("ASSOCTYPEQNAME:\"sys:children\"", 5);
        
        //check type
        assertAQuery("TYPE:\"" + testType.toString() + "\"", 1);
        assertAQuery("TYPE:\"" + testType.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);
        assertAQuery("EXACTTYPE:\"" + testType.toString() + "\"", 1);
        assertAQuery("EXACTTYPE:\"" + testType.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);
        assertAQuery("TYPE:\"" + testSuperType.toString() + "\"", 13);
        assertAQuery("TYPE:\"" + testSuperType.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 13);
        assertAQuery("TYPE:\"" + ContentModel.TYPE_CONTENT.toString() + "\"", 1);
        assertAQuery("TYPE:\"cm:content\"", 1);
        assertAQuery("TYPE:\"cm:content0\"", 0);
        assertAQuery("TYPE:\"cm:CONTENT\"", 1);
        assertAQuery("TYPE:\"cm:CONTENT1\"", 0);
        assertAQuery("TYPE:\"CM:CONTENT\"", 1);
        assertAQuery("TYPE:\"CM:CONTENT1\"", 0);
        assertAQuery("TYPE:\"CONTENT\"", 1);
        assertAQuery("TYPE:\"CONTENT1\"", 0);
        assertAQuery("TYPE:\"content\"", 1);
        assertAQuery("TYPE:\"content0\"", 0);
        assertAQuery("ASPECT:\"flubber\"", 0);
        assertAQuery("TYPE:\"" + ContentModel.TYPE_THUMBNAIL.toString() + "\"",
                1);
        assertAQuery("TYPE:\"" + ContentModel.TYPE_THUMBNAIL.toString()
                + "\" TYPE:\"" + ContentModel.TYPE_CONTENT.toString() + "\"", 2);
        assertAQuery("EXACTTYPE:\"" + testSuperType.toString() + "\"", 12);
        assertAQuery("EXACTTYPE:\"" + testSuperType.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 12);
        assertAQuery("ASPECT:\"" + testAspect.toString() + "\"", 1);
        assertAQuery("ASPECT:\"" + testAspect.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);
        assertAQuery("EXACTASPECT:\"" + testAspect.toString() + "\"", 1);
        assertAQuery("EXACTASPECT:\"" + testAspect.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);


        // Check text
        assertAQuery("TEXT:fox AND TYPE:\"" + ContentModel.PROP_CONTENT.toString() + "\"", 1);
        assertAQuery("TEXT:fox @cm\\:name:fox", 1);
        assertAQuery("d\\:content:fox d\\:text:fox", 1);
        assertAQuery("TEXT:fo AND TYPE:\"" + ContentModel.PROP_CONTENT.toString() + "\"", 0);

        assertAQuery("TEXT:\"the\"", 1);
        assertAQuery("TEXT:\"and\"", 1);

        //TODO fix this assert
        //assertAQuery("TEXT:\"over the lazy\"", 1);
        // Depends on stop words being removed .... which depends on the configuration
        //assertAQuery("TEXT:\"over a lazy\"", 1);

        assertAQuery("\\@"
                        + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                        "text-indexed-stored-tokenised-atomic").toString()) + ":*a*", 1);
        assertAQuery("\\@"
                        + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                        "text-indexed-stored-tokenised-atomic").toString()) + ":*A*", 1);
        assertAQuery("\\@"
                        + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                        "text-indexed-stored-tokenised-atomic").toString()) + ":\"*a*\"", 1);
        assertAQuery("\\@"
                        + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                        "text-indexed-stored-tokenised-atomic").toString()) + ":\"*A*\"", 1);
        assertAQuery("\\@"
                        + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                        "text-indexed-stored-tokenised-atomic").toString()) + ":*s*", 1);
        assertAQuery("\\@"
                        + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                        "text-indexed-stored-tokenised-atomic").toString()) + ":*S*", 1);
        assertAQuery("\\@"
                        + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                        "text-indexed-stored-tokenised-atomic").toString()) + ":\"*s*\"", 1);
        assertAQuery("\\@"
                        + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                        "text-indexed-stored-tokenised-atomic").toString()) + ":\"*S*\"", 1);
        assertAQuery("TEXT:*A*", 1);
        assertAQuery("TEXT:\"*a*\"", 1);
        assertAQuery("TEXT:\"*A*\"", 1);
        assertAQuery("TEXT:*a*", 1);
        assertAQuery("TEXT:*Z*", 1);
        assertAQuery("TEXT:\"*z*\"", 1);
        assertAQuery("TEXT:\"*Z*\"", 1);
        assertAQuery("TEXT:*z*", 1);
        assertAQuery("TEXT:laz*", 1);
        assertAQuery("TEXT:laz~", 1);
        assertAQuery("TEXT:la?y", 1);
        assertAQuery("TEXT:?a?y", 1);
        assertAQuery("TEXT:*azy", 1);
        assertAQuery("TEXT:*az*", 1);

        // Accents

        assertAQuery("TEXT:\"\u00E0\u00EA\u00EE\u00F0\u00F1\u00F6\u00FB\u00FF\"", 1);
        assertAQuery("TEXT:\"aeidnouy\"", 1);

        // FTS

        assertAQuery("TEXT:\"fox\"", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString())
                        + ":\"fox\"", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString())
                        + ".mimetype:\"text/plain\"", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString())
                        + ".locale:\"en_GB\"", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString())
                        + ".locale:en_*", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString())
                        + ".locale:e*_GB", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString())
                        + ".size:\"298\"", 1);

        //TODO Fix these tests
        /*
        assertAQuery("TEXT:\"fox\"", 0, null, new String[] { "@"
                + ContentModel.PROP_NAME.toString() }, null);
        assertAQuery("TEXT:\"fox\"", 1, null, new String[] {
                "@" + ContentModel.PROP_NAME.toString(), "@" + ContentModel.PROP_CONTENT.toString() }, null);
        assertAQuery("TEXT:\"cabbage\"", 15, null, new String[] { "@"
                + orderText.toString() }, null);
        assertAQuery("TEXT:\"cab*\"", 15, null,
                new String[] { "@" + orderText.toString() }, null);
        assertAQuery("TEXT:\"*bage\"", 15, null,
                new String[] { "@" + orderText.toString() }, null);
        assertAQuery("TEXT:\"*ba*\"", 15, null,
                new String[] { "@" + orderText.toString() }, null);
        assertAQuery("TEXT:cabbage", 15, null,
                new String[] { "@" + orderText.toString() }, null);
        assertAQuery("TEXT:*cab*", 15, Locale.ENGLISH, new String[] { "@"
                + orderText.toString() }, null);
        assertAQuery("TEXT:*bage", 15, null,
                new String[] { "@" + orderText.toString() }, null);
        */
//            assertAQuery("TEXT:dabbage~0.3", 15, null, new String[] { "@"
//                        + orderText.toString() }, null);

        assertAQuery("TEXT:\"alfresco\"", 1);
        assertAQuery("TEXT:\"alfresc?\"", 1);
        assertAQuery("TEXT:\"alfres??\"", 1);
        assertAQuery("TEXT:\"alfre???\"", 1);
        assertAQuery("TEXT:\"alfr????\"", 1);
        assertAQuery("TEXT:\"alf?????\"", 1);
        assertAQuery("TEXT:\"al??????\"", 1);
        assertAQuery("TEXT:\"a???????\"", 1);
        assertAQuery("TEXT:\"????????\"", 1);
        assertAQuery("TEXT:\"a??re???\"", 1);
        assertAQuery("TEXT:\"?lfresco\"", 1);
        assertAQuery("TEXT:\"??fresco\"", 1);
        assertAQuery("TEXT:\"???resco\"", 1);
        assertAQuery("TEXT:\"????esco\"", 1);
        assertAQuery("TEXT:\"?????sco\"", 1);
        assertAQuery("TEXT:\"??????co\"", 1);
        assertAQuery("TEXT:\"???????o\"", 1);
        assertAQuery("TEXT:\"???res?o\"", 1);
        assertAQuery("TEXT:\"????e?co\"", 1);
        assertAQuery("TEXT:\"????e?c?\"", 1);
        assertAQuery("TEXT:\"???re???\"", 1);

        assertAQuery("TEXT:\"alfresc*\"", 1);
        assertAQuery("TEXT:\"alfres*\"", 1);
        assertAQuery("TEXT:\"alfre*\"", 1);
        assertAQuery("TEXT:\"alfr*\"", 1);
        assertAQuery("TEXT:\"alf*\"", 1);
        assertAQuery("TEXT:\"al*\"", 1);
        assertAQuery("TEXT:\"a*\"", 1);
        assertAQuery("TEXT:\"a****\"", 1);
        assertAQuery("TEXT:\"*lfresco\"", 1);
        assertAQuery("TEXT:\"*fresco\"", 1);
        assertAQuery("TEXT:\"*resco\"", 1);
        assertAQuery("TEXT:\"*esco\"", 1);
        assertAQuery("TEXT:\"*sco\"", 1);
        assertAQuery("TEXT:\"*co\"", 1);
        assertAQuery("TEXT:\"*o\"", 1);
        assertAQuery("TEXT:\"****lf**sc***\"", 1);
        // Lucene wildcard bug matches when it should not ....
        //assertAQuery("TEXT:\"*??*lf**sc***\"", 0);
        assertAQuery("TEXT:\"??lf**sc***\"", 0);
        assertAQuery("TEXT:\"alfresc*tutorial\"", 0);
        assertAQuery("TEXT:\"alf* tut*\"", 1);
        assertAQuery("TEXT:\"*co *al\"", 1);

        
        //Check ranges

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[a TO b]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[a TO \uFFFF]", 14);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[* TO b]", 2);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[\u0000 TO b]", 2);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[d TO \uFFFF]", 12);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[d TO *]", 12);
        
        //Check non-field

        assertAQuery("TEXT:fox", 1);
        assertAQuery("TEXT:fo*", 1);
        assertAQuery("TEXT:f*x", 1);
        assertAQuery("TEXT:*ox", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString()) + ":fox",
                1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString()) + ":fo*",
                1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString()) + ":f*x",
                1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString()) + ":*ox",
                1);
        assertAQuery(
                "@"
                        + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT
                        .toPrefixString(dataModel.getNamespaceDAO())) + ":fox", 1);
        assertAQuery(
                "@"
                        + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT
                        .toPrefixString(dataModel.getNamespaceDAO())) + ":fo*", 1);
        assertAQuery(
                "@"
                        + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT
                        .toPrefixString(dataModel.getNamespaceDAO())) + ":f*x", 1);
        assertAQuery(
                "@"
                        + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT
                        .toPrefixString(dataModel.getNamespaceDAO())) + ":*ox", 1);
        
        // Check null and unset

        assertAQuery("ISUNSET:\""
                + QName.createQName(TEST_NAMESPACE, "null").toString() + "\"", 0);
        assertAQuery("ISNULL:\"" + QName.createQName(TEST_NAMESPACE,
                "null").toString() + "\"", 1);
        assertAQuery("EXISTS:\"" + QName.createQName(TEST_NAMESPACE,
                "null").toString() + "\"", 1);
        assertAQuery("ISNOTNULL:\""
                + QName.createQName(TEST_NAMESPACE, "null").toString() + "\"", 0);

        assertAQuery("ISUNSET:\"" + QName.createQName(TEST_NAMESPACE, "path-ista").toString() + "\"", 0);
        assertAQuery("ISNULL:\"" + QName.createQName(TEST_NAMESPACE,
                "path-ista").toString() + "\"", 0);
        assertAQuery("ISNOTNULL:\"" + QName.createQName(TEST_NAMESPACE, "path-ista").toString() + "\"", 1);
        assertAQuery("EXISTS:\"" + QName.createQName(TEST_NAMESPACE, "path-ista").toString() + "\"", 1);

        assertAQuery("ISUNSET:\"" + QName.createQName(TEST_NAMESPACE, "aspectProperty").toString() + "\"", 0);
        assertAQuery("ISNULL:\"" + QName.createQName(TEST_NAMESPACE,
                "aspectProperty").toString() + "\"", 0);
        assertAQuery("ISNOTNULL:\"" + QName.createQName(TEST_NAMESPACE, "aspectProperty").toString() + "\"", 1);
        assertAQuery("EXISTS:\"" + QName.createQName(TEST_NAMESPACE, "aspectProperty").toString() + "\"", 1);
    }


}