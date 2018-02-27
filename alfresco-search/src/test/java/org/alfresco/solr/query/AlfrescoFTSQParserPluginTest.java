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


import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import org.alfresco.model.ContentModel;
import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.SolrInformationServer;
import org.alfresco.util.CachingDateFormat;
import org.alfresco.util.CachingDateFormat.SimpleDateFormatAndResolution;
import org.alfresco.util.ISO9075;
import org.alfresco.util.SearchLanguageConversion;
import org.apache.lucene.util.LuceneTestCase;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.Test;


@LuceneTestCase.SuppressCodecs({"Appending","Lucene3x","Lucene40","Lucene41","Lucene42","Lucene43", "Lucene44", "Lucene45","Lucene46","Lucene47","Lucene48","Lucene49"})
@SolrTestCaseJ4.SuppressSSL
public class AlfrescoFTSQParserPluginTest extends LoadAFTSTestData implements QueryConstants {



    @Test
    public void dataChecks() throws Exception {

        checkRootNodes();
        checkpaths();
        checkQNames();
        checkType();
        checkDataType();
        checkText();
        checkMLText();
        checkRanges();
        checkNonField();
        checkNullAndUnset();
        checkInternalFields();
        checkAuthorityFilter(false); //PostFilter false
        checkAuthorityFilter(true); //PostFilter true
        checkPropertyTypes();
        testAFTS();
        testAFTSandSort();
        testSort();
        testCMIS();
        checkPaging();
        checkAncestor();

        loadSecondDataSet();

        checkRootNodes();
        checkpaths();
        checkQNames();
        checkType();
        checkDataType();
        checkText();
        checkMLText();
        checkRanges();
        checkNonField();
        checkNullAndUnset();
        checkInternalFields();
        checkAuthorityFilter(true);
        checkAuthorityFilter(false);
        checkPropertyTypes();
        testAFTS();
        testAFTSandSort();
        testSort();
        testCMIS();
        checkPaging();
       

        loadEscapingTestData();
        testChildNameEscaping();
        
        loadMntTestData();
        testMnt();
    }

    private void checkRootNodes() throws Exception {
        assertAQuery("PATH:\"/\"", 1);
        assertAQuery("PATH:\"/.\"", 1);
    }

    private void checkpaths() throws Exception {

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
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve/cm:thirteen/cm:fourteen\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve/cm:thirteen/cm:common\"", 1);
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
    }

    private void checkQNames()  throws Exception {
        assertAQuery("QNAME:\"nine\"",1);
        assertAQuery("PRIMARYASSOCTYPEQNAME:\"cm:contains\"",11);
        assertAQuery("PRIMARYASSOCTYPEQNAME:\"sys:children\"",4);
        assertAQuery("ASSOCTYPEQNAME:\"cm:contains\"",11);
        assertAQuery("ASSOCTYPEQNAME:\"sys:children\"",5);
    }

    private void checkType() throws Exception {
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
        assertAQuery("TYPE:\"" + ContentModel.TYPE_THUMBNAIL.toString() + "\"", 1);
        assertAQuery("TYPE:\"" + ContentModel.TYPE_THUMBNAIL.toString() + "\" TYPE:\"" + ContentModel.TYPE_CONTENT.toString() + "\"", 2);
        assertAQuery("EXACTTYPE:\"" + testSuperType.toString() + "\"", 12);
        assertAQuery("EXACTTYPE:\"" + testSuperType.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 12);
        assertAQuery("ASPECT:\"" + testAspect.toString() + "\"", 1);
        assertAQuery("ASPECT:\"" + testAspect.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);
        assertAQuery("EXACTASPECT:\"" + testAspect.toString() + "\"", 1);
        assertAQuery("EXACTASPECT:\"" + testAspect.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);
    }

    private void checkDataType() throws Exception {
        assertAQuery("d\\:double:\"5.6\"", 1);
        assertAQuery("d\\:content:\"fox\"", 1);
        assertAQuery("d\\:content:\"fox\"", 1, Locale.US, null, null);
    }

    private void checkText() throws Exception {
        /*********** Check text **************/
        assertAQuery("TEXT:fox AND TYPE:\"" + ContentModel.PROP_CONTENT.toString() + "\"", 1);
        assertAQuery("TEXT:fox @cm\\:name:fox", 1);
        assertAQuery("d\\:content:fox d\\:text:fox", 1);
        assertAQuery("TEXT:fo AND TYPE:\"" + ContentModel.PROP_CONTENT.toString() + "\"", 0);

        // Depends on the configuration
        assertAQuery("TEXT:\"the\"", 1);
        assertAQuery("TEXT:\"and\"", 1);
        assertAQuery("TEXT:\"the lazy dog\"", 1);
        assertAQuery("TEXT:\"over lazy\"", 0);
        assertAQuery("TEXT:\"over the lazy dog\"", 1);
        assertAQuery("TEXT:\"over the lazy\"", 1);
        //With no shared.properties this falls back to 5.0 cross locale support
        assertAQuery("TEXT:\"over a lazy\"", 1);

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

        /**************** Check text *********************/

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

        assertAQuery("TEXT:\"fox\"", 0, null, new String[]{"@"
                + ContentModel.PROP_NAME.toString()}, null);
        assertAQuery("TEXT:\"fox\"", 1, null, new String[]{
                "@" + ContentModel.PROP_NAME.toString(), "@" + ContentModel.PROP_CONTENT.toString()}, null);
        assertAQuery("TEXT:\"cabbage\"", 15, null, new String[]{"@"
                + orderText.toString()}, null);
        assertAQuery("TEXT:\"cab*\"", 15, null,
                new String[]{"@" + orderText.toString()}, null);
        assertAQuery("TEXT:\"*bage\"", 15, null,
                new String[]{"@" + orderText.toString()}, null);
        assertAQuery("TEXT:\"*ba*\"", 15, null,
                new String[]{"@" + orderText.toString()}, null);
        assertAQuery("TEXT:cabbage", 15, null,
                new String[]{"@" + orderText.toString()}, null);
        assertAQuery("TEXT:*cab*", 15, Locale.ENGLISH, new String[]{"@"
                + orderText.toString()}, null);
        assertAQuery("TEXT:*bage", 15, null,
                new String[]{"@" + orderText.toString()}, null);

//NA            assertAQuery("TEXT:dabbage~0.3", 15, null, new String[] { "@"
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
    }


    private void checkMLText() throws Exception {
        /********* check mltext ************/

        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alfresco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alfresc?\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alfres??\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alfre???\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alfr????\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alf?????\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"al??????\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"a???????\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"????????\"", 1);

        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"a??re???\"", 1);

        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"?lfresco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"??fresco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"???resco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"????esco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"?????sco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"??????co\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"???????o\"", 1);

        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"???resco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"???res?o\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"????e?co\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"????e?c?\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"???re???\"", 1);

        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alfresc*\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alfres*\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alfre*\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alfr*\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"alf*\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"al*\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"a*\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"a*****\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"*lfresco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"*fresco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"*resco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"*esco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"*sco\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"*co\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"*o\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"****lf**sc***\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"*??*lf**sc***\"", 0);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"Alfresc*tutorial\"", 0);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"Alf* tut*\"", 1);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"*co *al\"", 1);

        QName mlQName = QName.createQName(TEST_NAMESPACE, "ml");
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":and", 0);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":\"and\"", 0);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":banana", 1);

        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":banana", 1, Locale.UK,
                null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":banana", 1,
                Locale.ENGLISH, null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":banane", 1,
                Locale.FRENCH, null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":香蕉", 1,
                Locale.CHINESE, null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":banaan", 1,
                new Locale("nl"), null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":banane", 1,
                Locale.GERMAN, null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":μπανάνα", 1,
                new Locale("el"), null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":banana", 1,
                Locale.ITALIAN, null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString())    + ":バナナ", 1, Locale.JAPANESE, null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":바나나", 1, new Locale(
                        "ko"), null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":banana", 1,
                new Locale("pt"), null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":банан", 1, new Locale(
                        "ru"), null, null);
        assertAQuery(
                "@" + SearchLanguageConversion.escapeLuceneQuery(mlQName.toString()) + ":plátano", 1,
                new Locale("es"), null, null);
    }

    private void checkRanges() throws Exception {
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[a TO b]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[a TO \uFFFF]", 14);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[* TO b]", 2);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[\u0000 TO b]", 2);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[d TO \uFFFF]", 12);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(orderText.toString()) + ":[d TO *]", 12);
    }

    private void checkNonField() throws Exception {

        assertAQuery("TEXT:fox", 1);
        assertAQuery("TEXT:fo*", 1);
        assertAQuery("TEXT:f*x", 1);
        assertAQuery("TEXT:*ox", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString()) + ":fox", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString()) + ":fo*", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString()) + ":f*x", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT.toString()) + ":*ox", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT
                .toPrefixString(dataModel.getNamespaceDAO())) + ":fox", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT
                .toPrefixString(dataModel.getNamespaceDAO())) + ":fo*", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT
                .toPrefixString(dataModel.getNamespaceDAO())) + ":f*x", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CONTENT
                .toPrefixString(dataModel.getNamespaceDAO())) + ":*ox", 1);

    }

    private void checkNullAndUnset() throws Exception {

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

    private void checkInternalFields() throws Exception{

        final Long aclId = new Long(1);

        for (int i = 1; i < 16; i++) {
            Long dbId = new Long(i);
            String id = SearchLanguageConversion.escapeLuceneQuery(AlfrescoSolrDataModel.getNodeDocumentId(AlfrescoSolrDataModel.DEFAULT_TENANT, aclId, dbId));
            assertQ(areq(params("q", FIELD_SOLR4_ID + ":" + id, "qt", "/afts"), null),
                    "*[count(//doc)=1]");
        }

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", SearchLanguageConversion.escapeLuceneQuery(FIELD_DOC_TYPE) + ":" + SolrInformationServer.DOC_TYPE_NODE), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", SearchLanguageConversion.escapeLuceneQuery(FIELD_DOC_TYPE) + ":" + SolrInformationServer.DOC_TYPE_ACL), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", SearchLanguageConversion.escapeLuceneQuery(FIELD_DOC_TYPE) + ":" + SolrInformationServer.DOC_TYPE_ACL_TX), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", SearchLanguageConversion.escapeLuceneQuery(FIELD_DOC_TYPE) + ":" + SolrInformationServer.DOC_TYPE_TX), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PARENT + ":\"" + testNodeRef + "\""), null),
                "*[count(//doc)=4]");

        // AbstractLuceneQueryParser.FIELD_LINKASPECT is not used for SOLR

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_ANCESTOR + ":\"" + testNodeRef + "\""), null),
                "*[count(//doc)=10]");

        // AbstractLuceneQueryParser.FIELD_ISCONTAINER is not used for SOLR
        // AbstractLuceneQueryParser.FIELD_ISCATEGORY is not used for SOLR

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:one\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:two\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:three\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:four\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:five\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:six\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:seven\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:eight-0\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:eight-1\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:eight-2\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:nine\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:ten\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:eleven\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:twelve\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:thirteen\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:fourteen\""), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:fifteen\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:common\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_QNAME + ":\"cm:link\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:one\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:two\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:three\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:four\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:five\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:six\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:seven\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:eight-0\""), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:eight-1\""), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:eight-2\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:nine\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:ten\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:eleven\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:twelve\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:thirteen\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:fourteen\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:fifteen\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:common\""), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCQNAME + ":\"cm:link\""), null),
                "*[count(//doc)=0]");

        // AbstractLuceneQueryParser.FIELD_ISROOT is not used in SOLR

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYASSOCTYPEQNAME + ":\""
                        + ContentModel.ASSOC_CHILDREN.toPrefixString(dataModel.getNamespaceDAO()) + "\""), null),
                "*[count(//doc)=4]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_ISNODE + ":T"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_ASSOCTYPEQNAME + ":\""
                        + ContentModel.ASSOC_CHILDREN.toPrefixString(dataModel.getNamespaceDAO()) + "\""), null),
                "*[count(//doc)=5]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_PRIMARYPARENT + ":\"" + testNodeRef + "\""), null),
                "*[count(//doc)=2]");

        // TYPE and ASPECT is covered in other tests

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_FTSSTATUS + ":\"New\""), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":1"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":2"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":3"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":4"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":5"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":6"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":7"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":8"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":9"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":10"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":11"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":12"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":13"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":14"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":15"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":16"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_DBID + ":17"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/native", "q", FIELD_DBID + ":*"), null), "*[count(//doc)=16]");
        assertQ(areq(params("rows", "20", "qt", "/native", "q", FIELD_DBID + ":[3 TO 4]"), null), "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_TXID + ":1"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_INTXID + ":1"), null),
                "*[count(//doc)=17]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_ACLTXID + ":1"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_INACLTXID + ":1"), null),
                "*[count(//doc)=2]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_INACLTXID + ":2"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/native", "q", FIELD_TXCOMMITTIME + ":*"), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/native", "q", FIELD_ACLTXCOMMITTIME + ":*"), null), "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", FIELD_ACLID + ":1"), null),
                "*[count(//doc)=17]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_READER + ":\"GROUP_EVERYONE\""), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":andy"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":bob"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":cid"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":dave"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":eoin"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":fred"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":gail"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":hal"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":ian"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":jake"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":kara"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":loon"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":mike"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":noodle"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_OWNER + ":ood"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q",  FIELD_PARENT_ASSOC_CRC + ":0"), null),
                "*[count(//doc)=16]");

        /*

        // AbstractLuceneQueryParser.FIELD_EXCEPTION_MESSAGE
        // addNonDictionaryField(AbstractLuceneQueryParser.FIELD_EXCEPTION_STACK

        */
    }

    private void testAFTS() throws Exception {

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "\"lazy\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "lazy and dog"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "-lazy and -dog"), null),
                "*[count(//doc)=15]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "|lazy and |dog"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "|eager and |dog"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "|lazy and |wolf"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "|eager and |wolf"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "-lazy or -dog"), null),
                "*[count(//doc)=15]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "-eager or -dog"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "-lazy or -wolf"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "-eager or -wolf"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "lazy dog"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "lazy and not dog"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "lazy not dog"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "lazy and !dog"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "lazy !dog"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "lazy and -dog"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "lazy -dog"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:\"lazy\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm_content:\"lazy\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "d:content:\"lazy\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "=cm_content:\"lazy\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "~cm_content:\"lazy\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:content:big OR cm:content:lazy"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:content:big AND cm:content:lazy"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "{http://www.alfresco.org/model/content/1.0}content:\"lazy\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "=lazy"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "@cm:content:big OR @cm:content:lazy"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "@cm:content:big AND @cm:content:lazy"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "@{http://www.alfresco.org/model/content/1.0}content:\"lazy\""), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "~@cm:content:big OR ~@cm:content:lazy"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "brown * quick"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "brown * dog"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "brown *(0) dog"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "brown *(1) dog"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "brown *(2) dog"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "brown *(3) dog"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "brown *(4) dog"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "brown *(5) dog"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "brown *(6) dog"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(\"lazy\")"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(lazy and dog)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(-lazy and -dog)"), null),
                "*[count(//doc)=15]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(-lazy and dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(lazy and -dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(|lazy and |dog)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(|eager and |dog)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(|lazy and |wolf)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(|eager and |wolf)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(-lazy or -dog)"), null),
                "*[count(//doc)=15]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(-eager or -dog)"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(-lazy or -wolf)"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(-eager or -wolf)"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(lazy dog)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(lazy and not dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(lazy not dog)"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(lazy and !dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(lazy !dog)"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(lazy and -dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(lazy -dog)"), null),
                "*[count(//doc)=16]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm_content:(\"lazy\")"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:content:(big OR lazy)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:content:(big AND lazy)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "{http://www.alfresco.org/model/content/1.0}content:(\"lazy\")"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(=lazy)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "@cm:content:(big) OR @cm:content:(lazy)"), null),
                "*[count(//doc)=1]");


        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "@cm:content:(big) AND @cm:content:(lazy)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "@{http://www.alfresco.org/model/content/1.0}content:(\"lazy\")"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "@cm:content:(~big OR ~lazy)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(brown * quick)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(brown * dog)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(brown *(0) dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(brown *(1) dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(brown *(2) dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(brown *(3) dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(brown *(4) dog)"), null),
                "*[count(//doc)=0]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(brown *(5) dog)"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "TEXT:(brown *(6) dog)"), null),
                "*[count(//doc)=1]");

        //Mimetype
        assertAQueryHasNumberOfDocs("cm:content.mimetype:\"text/plain\"", 1);
        assertAQueryHasNumberOfDocs("cm_content.mimetype:\"text/plain\"", 1);
        assertAQueryHasNumberOfDocs("@cm_content.mimetype:\"text/plain\"", 1);
        assertAQueryHasNumberOfDocs("content.mimetype:\"text/plain\"", 1);
        assertAQueryHasNumberOfDocs("@{http://www.alfresco.org/model/content/1.0}content.mimetype:\"text/plain\"", 1);
        assertAQueryHasNumberOfDocs("{http://www.alfresco.org/model/content/1.0}content.mimetype:\"text/plain\"", 1);

        //Numbers
        QName qname = QName.createQName(TEST_NAMESPACE, "float\\-ista");

        assertAQueryHasNumberOfDocs(qname + ":3.40", 1);
        assertAQueryHasNumberOfDocs(qname + ":3..4", 1);
        assertAQueryHasNumberOfDocs(qname + ":3..3.39", 0);
        assertAQueryHasNumberOfDocs(qname + ":3..3.40", 1);
        assertAQueryHasNumberOfDocs(qname + ":3.41..3.9", 0);
        assertAQueryHasNumberOfDocs(qname + ":3.40..3.9", 1);

        assertAQueryHasNumberOfDocs(qname + ":[3 TO 4]", 1);
        assertAQueryHasNumberOfDocs(qname + ":[3 TO 3.39]", 0);
        assertAQueryHasNumberOfDocs(qname + ":[3 TO 3.4]", 1);
        assertAQueryHasNumberOfDocs(qname + ":[3.41 TO 4]", 0);
        assertAQueryHasNumberOfDocs(qname + ":[3.4 TO 4]", 1);
        assertAQueryHasNumberOfDocs(qname + ":[3 TO 3.4>", 0);
        assertAQueryHasNumberOfDocs(qname + ":<3.4 TO 4]", 0);
        assertAQueryHasNumberOfDocs(qname + ":<3.4 TO 3.4>", 0);

        assertAQueryHasNumberOfDocs(qname + ":(3.40)", 1);
        assertAQueryHasNumberOfDocs(qname + ":(3..4)", 1);
        assertAQueryHasNumberOfDocs(qname + ":(3..3.39)", 0);
        assertAQueryHasNumberOfDocs(qname + ":(3..3.40)", 1);
        assertAQueryHasNumberOfDocs(qname + ":(3.41..3.9)", 0);
        assertAQueryHasNumberOfDocs(qname + ":(3.40..3.9)", 1);

        assertAQueryHasNumberOfDocs(qname + ":([3 TO 4])", 1);
        assertAQueryHasNumberOfDocs(qname + ":([3 TO 3.39])", 0);
        assertAQueryHasNumberOfDocs(qname + ":([3 TO 3.4])", 1);
        assertAQueryHasNumberOfDocs(qname + ":([3.41 TO 4])", 0);
        assertAQueryHasNumberOfDocs(qname + ":([3.4 TO 4])", 1);
        assertAQueryHasNumberOfDocs(qname + ":([3 TO 3.4>)", 0);
        assertAQueryHasNumberOfDocs(qname + ":(<3.4 TO 4])", 0);
        assertAQueryHasNumberOfDocs(qname + ":(<3.4 TO 3.4>)", 0);

        assertAQueryHasNumberOfDocs("test:float_x002D_ista:3.40", 1);

        //Test Lazy
        assertAQueryHasNumberOfDocs("lazy", 1);
        assertAQueryHasNumberOfDocs("laz*", 1);
        assertAQueryHasNumberOfDocs("l*y", 1);
        assertAQueryHasNumberOfDocs("l??y", 1);
        assertAQueryHasNumberOfDocs("?az?", 1);
        assertAQueryHasNumberOfDocs("*zy", 1);

        assertAQueryHasNumberOfDocs("\"lazy\"", 1);
        assertAQueryHasNumberOfDocs("\"laz*\"", 1);
        assertAQueryHasNumberOfDocs("\"l*y\"", 1);
        assertAQueryHasNumberOfDocs("\"l??y\"", 1);
        assertAQueryHasNumberOfDocs("\"?az?\"", 1);
        assertAQueryHasNumberOfDocs("\"*zy\"", 1);

        assertAQueryHasNumberOfDocs("cm:content:lazy", 1);
        assertAQueryHasNumberOfDocs("cm:content:laz*", 1);
        assertAQueryHasNumberOfDocs("cm:content:l*y", 1);
        assertAQueryHasNumberOfDocs("cm:content:l??y", 1);
        assertAQueryHasNumberOfDocs("cm:content:?az?", 1);
        assertAQueryHasNumberOfDocs("cm:content:*zy", 1);

        assertAQueryHasNumberOfDocs("cm:content:\"lazy\"", 1);
        assertAQueryHasNumberOfDocs("cm:content:\"laz*\"", 1);
        assertAQueryHasNumberOfDocs("cm:content:\"l*y\"", 1);
        assertAQueryHasNumberOfDocs("cm:content:\"l??y\"", 1);
        assertAQueryHasNumberOfDocs("cm:content:\"?az?\"", 1);
        assertAQueryHasNumberOfDocs("cm:content:\"*zy\"", 1);

        assertAQueryHasNumberOfDocs("cm:content:(lazy)", 1);
        assertAQueryHasNumberOfDocs("cm:content:(laz*)", 1);
        assertAQueryHasNumberOfDocs("cm:content:(l*y)", 1);
        assertAQueryHasNumberOfDocs("cm:content:(l??y)", 1);
        assertAQueryHasNumberOfDocs("cm:content:(?az?)", 1);
        assertAQueryHasNumberOfDocs("cm:content:(*zy)", 1);

        assertAQueryHasNumberOfDocs("cm:content:(\"lazy\")", 1);
        assertAQueryHasNumberOfDocs("cm:content:(\"laz*\")", 1);
        assertAQueryHasNumberOfDocs("cm:content:(\"l*y\")", 1);
        assertAQueryHasNumberOfDocs("cm:content:(\"l??y\")", 1);
        assertAQueryHasNumberOfDocs("cm:content:(\"?az?\")", 1);
        assertAQueryHasNumberOfDocs("cm:content:(\"*zy\")", 1);

        assertAQueryHasNumberOfDocs("lazy^2 dog^4.2", 1);
        assertAQueryHasNumberOfDocs("lazy~0.7", 1);
        assertAQueryHasNumberOfDocs("cm:content:laxy~0.7", 1);
        assertAQueryHasNumberOfDocs("laxy~0.7", 1);
        assertAQueryHasNumberOfDocs("=laxy~0.7", 1);
        assertAQueryHasNumberOfDocs("~laxy~0.7", 1);

        assertAQueryHasNumberOfDocs("\"quick fox\"~0", 0);
        assertAQueryHasNumberOfDocs("\"quick fox\"~1", 1);
        assertAQueryHasNumberOfDocs("\"quick fox\"~2", 1);
        assertAQueryHasNumberOfDocs("\"quick fox\"~3", 1);

        assertAQueryHasNumberOfDocs("\"fox quick\"~0", 0);
        assertAQueryHasNumberOfDocs("\"fox quick\"~1", 0);
        assertAQueryHasNumberOfDocs("\"fox quick\"~2", 1);
        assertAQueryHasNumberOfDocs("\"fox quick\"~3", 1);

        assertAQueryHasNumberOfDocs("lazy", 1);
        assertAQueryHasNumberOfDocs("-lazy", 15);
        assertAQueryHasNumberOfDocs("lazy -lazy", 16);
        assertAQueryHasNumberOfDocs("lazy^20 -lazy", 16);
        assertAQueryHasNumberOfDocs("lazy^20 -lazy^20", 16);

        assertAQueryHasNumberOfDocs("cm:content:lazy", 1);
//NA        assertAQueryHasNumberOfDocs("ANDY:lazy", 1);
        assertAQueryHasNumberOfDocs("content:lazy", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", 16);
        assertAQueryHasNumberOfDocs("+PATH:\"/app:company_home/st:sites/cm:rmtestnew1/cm:documentLibrary//*\"", 0);
        assertAQueryHasNumberOfDocs("+PATH:\"/app:company_home/st:sites/cm:rmtestnew1/cm:documentLibrary//*\" -TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\"", 15);
        assertAQueryHasNumberOfDocs("+PATH:\"/app:company_home/st:sites/cm:rmtestnew1/cm:documentLibrary//*\" AND -TYPE:\"{http://www.alfresco.org/model/content/1.0}thumbnail\"", 0);

        assertAQueryHasNumberOfDocs("(brown *(6) dog)", 1);
        assertAQueryHasNumberOfDocs("TEXT:(brown *(6) dog)", 1);
        assertAQueryHasNumberOfDocs("\"//.\"", 0);
        assertAQueryHasNumberOfDocs("cm:content:brown", 1);

        //NA assertAQueryHasNumberOfDocs("ANDY:brown", 1);
        //NA assertAQueryHasNumberOfDocs("andy:brown", 1);
        //NA  testQueryByHandler(report, core, "/afts", "ANDY", "brown", 1, null, (String) null);
        //NA testQueryByHandler(report, core, "/afts", "andy", "brown", 1, null, (String) null);

        assertAQueryHasNumberOfDocs("modified:*", 2);
        assertAQueryHasNumberOfDocs("modified:[MIN TO NOW]", 2);

        assertAQueryHasNumberOfDocs("TYPE:" + testType.toString(), 1);
        assertAQueryHasNumberOfDocs("TYPE:" + testType.toString(), "mimetype():document", 0);

        assertAQueryHasNumberOfDocs("TYPE:" + ContentModel.TYPE_CONTENT.toString(), 1);
        assertAQueryHasNumberOfDocs("TYPE:" + ContentModel.TYPE_CONTENT.toString(), "mimetype():document", 1);
        assertAQueryHasNumberOfDocs("TYPE:" + ContentModel.TYPE_CONTENT.toString(), "mimetype():\"text/plain\"", 1);
        assertAQueryHasNumberOfDocs("TYPE:" + ContentModel.TYPE_CONTENT.toString(), "contentSize():[0 TO 100]", 0);
        assertAQueryHasNumberOfDocs("TYPE:" + ContentModel.TYPE_CONTENT.toString(), "contentSize():[100 TO 1000]", 1);

        assertAQueryHasNumberOfDocs("modified:[NOW/DAY-1DAY TO NOW/DAY+1DAY]", 2);
        assertAQueryHasNumberOfDocs("modified:[NOW/DAY-1DAY TO *]", 2);
        assertAQueryHasNumberOfDocs("modified:[* TO NOW/DAY+1DAY]", 2);
        assertAQueryHasNumberOfDocs("modified:[* TO *]", 2);

        // Synonym
        // 1 in text -  1 in query
        assertAQueryHasNumberOfDocs("quick", 1);
        assertAQueryHasNumberOfDocs("fast", 1);
        assertAQueryHasNumberOfDocs("rapid", 1);
        assertAQueryHasNumberOfDocs("speedy", 1);

        // 3 words in test - 1..3 in query
        assertAQueryHasNumberOfDocs("\"brown fox jumped\"", 1);

        // Synonyms
        assertAQueryHasNumberOfDocs("\"leaping reynard\"", 1);
        assertAQueryHasNumberOfDocs("\"springer\"", 1);

        // 1 word in text 1..2 in query
        assertAQueryHasNumberOfDocs("lazy", 1);
        assertAQueryHasNumberOfDocs("\"bone idle\"", 1);

        // Cross language support and tokenisation part and full.
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


    private void checkAuthorityFilter(boolean postFilter) throws Exception {
        System.setProperty("alfresco.postfilter", new Boolean(postFilter).toString());
        /***** checkAuthorityFilter **********/
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", 16);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{!afts}|DENIED:andy", 0);
        assertAQueryHasNumOfDocsWithJson("PATH:\"//.\"", "{!afts}|DENYSET:\":andy:bob:cid\"", 0);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:andy", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:bob", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:cid", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:dave", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:eoin", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:fred", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:gail", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:hal", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:ian", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:jake", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:kara", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:loon", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:mike", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:noodle", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:ood", 1);

        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:pig", 16);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|READER:pig", 16);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:pig", 0);

        // All nodes point to ACL with ID #1. The ACL explicitly lists "pig" as a READER,
        // however, pig does not own any nodes.

        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|DENIED:pig", 0);

        // When using the fq parameter for AUTHORITY related filter queries, anyDenyDenies is
        // NOT supported, captured by this test case: something is DENIED, however GROUP_EVERYONE allows it.
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:something |AUTHORITY:GROUP_EVERYONE", 16);

        // "something" has no explicity READER or OWNER entries.
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|READER:something", 0);

        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|OWNER:something", 0);

        // "something" is DENIED to all nodes (they all use ACL #1)
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|DENIED:something", 16);

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


        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:andy", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:bob", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:cid", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:dave", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:eoin", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:fred", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:gail", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:hal", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:ian", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:eoin", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:jake", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:kara", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:loon", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:mike", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:noodle", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:ood", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:GROUP_EVERYONE", 16);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHORITY:andy |AUTHORITY:bob |AUTHORITY:cid", 3);

        assertAQueryHasNumberOfDocs("PATH:\"//.\"", 16);

        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":andy\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":bob\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":cid\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":dave\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":eoin\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":fred\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":gail\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":hal\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":ian\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":jake\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":kara\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":loon\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":mike\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":noodle\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":ood\"", 1);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":GROUP_EVERYONE\"", 16);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":andy\" |AUTHSET:\":bob\" |AUTHSET:\":cid\"", 3);
        assertAQueryHasNumberOfDocs("PATH:\"//.\"", "{!afts}|AUTHSET:\":andy:bob:cid\"", 3);
    }

    private void testSort() throws Exception {
        //Test Sorting
        assertAQueryIsSorted("PATH:\"//.\"", "ID asc", null, 16, new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 });
        assertAQueryIsSorted("PATH:\"//.\"", "ID desc",null, 16, new Integer[] { 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2, 1 });

        assertAQueryIsSorted("PATH:\"//.\"", "@" + createdDate + " asc", null, 16, new Integer[]{1, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + createdDate + " desc", null, 16, new Integer[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + createdTime + " asc", null, 16, new Integer[]{1, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + createdTime + " desc", null, 16, new Integer[]{2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1});

        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderDouble + " asc", null, 16, new Integer[]{15, 13, 11, 9, 7, 5, 3, 2, 1, 4, 6, 8, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderDouble + " desc", null, 16, new Integer[]{16, 14, 12, 10, 8, 6, 4, 1, 2, 3, 5, 7, 9, 11, 13, 15});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderFloat + " asc", null, 16, new Integer[]{15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 1, 8, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderFloat + " desc", null, 16, new Integer[]{16, 14, 12, 10, 8, 1, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLong + " asc", null, 16, new Integer[]{15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 8, 1, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLong + " desc", null, 16, new Integer[]{16, 14, 12, 10, 1, 8, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderInt + " asc", null, 16, new Integer[]{15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 1, 8, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderInt + " desc", null, 16, new Integer[]{16, 14, 12, 10, 8, 1, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15});

        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderText + " asc", null, 16, new Integer[]{1, 15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 8, 10, 12, 14, 16});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderText + " desc", null, 16, new Integer[]{16, 14, 12, 10, 8, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15, 1});

        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedText + " asc",  Locale.ENGLISH, 16, new Integer[] {1, 10, 11, 2, 3, 4, 5, 13, 12, 6, 7, 8, 14, 15, 16, 9 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedText + " desc", Locale.ENGLISH, 16, new Integer[] {9, 16, 15, 14, 8, 7, 6, 12, 13, 5, 4, 3, 2, 11, 10, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedText + " asc",  Locale.FRENCH,  16, new Integer[]  {1, 10, 11, 2, 3, 4, 5, 13, 12, 6, 8, 7, 14, 15, 16, 9 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedText + " desc", Locale.FRENCH,  16, new Integer[]  {9, 16, 15, 14, 7, 8, 6, 12, 13, 5, 4, 3, 2, 11, 10, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedText + " asc",  Locale.GERMAN, 16, new Integer[]  {1, 10, 11, 2, 3, 4, 5, 13, 12, 6, 7, 8, 14, 15, 16, 9 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedText + " desc", Locale.GERMAN, 16, new Integer[]  {9, 16, 15, 14, 8, 7, 6, 12, 13, 5, 4, 3, 2, 11, 10, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedText + " asc",  new Locale("sv"), 16, new Integer[] {1, 11, 2, 3, 4, 5, 13, 6, 7, 8, 12, 14, 15, 16, 9, 10 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedText + " desc", new Locale("sv"), 16, new Integer[] {10, 9, 16, 15, 14, 12, 8, 7, 6, 13, 5, 4, 3, 2, 11, 1 });

        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderMLText + " asc",  Locale.ENGLISH, 16, new Integer[] { 1, 15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 8, 10, 12, 14, 16 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderMLText + " desc", Locale.ENGLISH, 16, new Integer[] { 16, 14, 12, 10, 8, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15, 1});
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderMLText + " asc",  Locale.FRENCH, 16,  new Integer[] { 1, 14, 16, 12, 10, 8, 6, 4, 2, 3, 5, 7, 9, 11, 13, 15 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderMLText + " desc", Locale.FRENCH, 16,  new Integer[] { 15, 13, 11, 9, 7, 5, 3, 2, 4, 6, 8, 10, 12, 16, 14, 1 });

        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedMLText + " asc",  Locale.ENGLISH, 16, new Integer[] { 1, 16, 15, 14, 13, 12, 11, 10, 9, 8, 7, 6, 5, 4, 3, 2 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedMLText + " desc", Locale.ENGLISH, 16, new Integer[] { 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedMLText + " asc",  Locale.FRENCH, 16, new Integer[] { 1, 16, 15, 14, 13, 12, 2, 3, 4, 5, 11, 10, 9, 8, 7, 6 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedMLText + " desc", Locale.FRENCH, 16, new Integer[] { 6, 7, 8, 9, 10, 11, 5, 4, 3, 2, 12, 13, 14, 15, 16, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedMLText + " asc",  Locale.GERMAN, 16, new Integer[] { 1, 16, 15, 2, 3, 4, 5, 6, 7, 9, 8, 10, 12, 14, 11, 13 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedMLText + " desc", Locale.GERMAN, 16, new Integer[] { 13, 11, 14, 12, 10, 8, 9, 7, 6, 5, 4, 3, 2, 15, 16, 1 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedMLText + " asc",  new Locale("es"), 16, new Integer[] { 1, 16, 15, 7, 14, 8, 9, 10, 11, 12, 13, 2, 3, 4, 5, 6 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + orderLocalisedMLText + " desc", new Locale("es"), 16, new Integer[] { 6, 5, 4, 3, 2, 13, 12, 11, 10, 9, 8, 14, 7, 15, 16, 1 });

        assertAQueryIsSorted("PATH:\"//.\"", "@" + ContentModel.PROP_CONTENT + ".size asc", null, 16, new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ContentModel.PROP_CONTENT + ".size desc",null, 16, new Integer[] { 15 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ContentModel.PROP_CONTENT + ".mimetype asc", null, 16, new Integer[] { 15 });
        assertAQueryIsSorted("PATH:\"//.\"", "@" + ContentModel.PROP_CONTENT + ".mimetype desc",null, 16, new Integer[] { 15 });
    }

    private void assertAQueryHasNumOfDocsWithJson(String query, String json, int num)
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "fq", "{!afts}AUTHORITY_FILTER_FROM_JSON", "q", query), json), "*[count(//doc)="+num+"]");
    }

    private void assertAQueryHasNumberOfDocs(String query, int num)
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", query), null), "*[count(//doc)="+num+"]");
    }

    private void assertAQueryHasNumberOfDocs(String query, String filter, int num)
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", query, "fq", filter), null), "*[count(//doc)="+num+"]");
    }

    private void assertAQueryIsSorted(String query, String sort, Locale aLocale, int num, Integer[] sortOrder)
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

    private void assertPage(String query, String sort, int num, int rows, int start, Integer[] sortOrder)
    {
        List<String> xpaths = new ArrayList();
        xpaths.add("*[count(//doc)=" + num + "]");
        for (int i = 1; i <= sortOrder.length; i++)
        {
            if(sortOrder[i - 1] != null) {
                xpaths.add("//result/doc[" + i + "]/long[@name='DBID'][.='" + sortOrder[i - 1] + "']");
            }
        }

        String[] params = new String[] {"start", Integer.toString(start),
                                        "rows", Integer.toString(rows),
                                        "qt", "/afts",
                                        "q", query,
                                        "sort", sort};

        assertQ(areq(params(params), null), xpaths.toArray(new String[0]));

    }


    private void testCMIS() throws Exception {

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "select * from cmis:document"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q", "select * from cmis:document D WHERE CONTAINS(D,'lazy')"), null),
                "*[count(//doc)=1]");

        assertQ(areq(params("rows", "20", "qt", "/cmis", "q","SELECT * FROM cmis:document D JOIN cm:ownable O ON D.cmis:objectId = O.cmis:objectId"), null),
                "*[count(//doc)=0]");

    }

    private void checkPropertyTypes() throws Exception{
        QName qname = QName.createQName(TEST_NAMESPACE, "int-ista");

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"1\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":1", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"01\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":01", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"001\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"0001\"", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[A TO 2]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[0 TO 2]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[0 TO A]", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{A TO 1}", 0);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{0 TO 1}", 0);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{0 TO A}", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{A TO 2}", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{1 TO 2}", 0);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{1 TO A}", 0);

        qname = QName.createQName(TEST_NAMESPACE, "long-ista");

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"2\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"02\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"002\"", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"0002\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[A TO 2]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[0 TO 2]", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[0 TO A]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{A TO 2}", 0);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{0 TO 2}", 0);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{0 TO A}", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{A TO 3}", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{2 TO 3}", 0);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{2 TO A}", 0);

        qname = QName.createQName(TEST_NAMESPACE, "float-ista");

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"3.4\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[A TO 4]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[3 TO 4]", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[3 TO A]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[A TO 3.4]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[3.3 TO 3.4]", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[3.3 TO A]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{A TO 3.4}", 0);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{3.3 TO 3.4}", 0);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{3.3 TO A}", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"3.40\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"03.4\"", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"03.40\"", 1);

        qname = QName.createQName(TEST_NAMESPACE, "double-ista");

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"5.6\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"05.6\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"5.60\"", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"05.60\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[A TO 5.7]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[5.5 TO 5.7]", 1);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":[5.5 TO A]", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{A TO 5.6}", 0);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{5.5 TO 5.6}", 0);

        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":{5.6 TO A}", 0);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery("My-funny&MissingProperty:woof"), 0);

        Date date = new Date();


        for (SimpleDateFormatAndResolution df : CachingDateFormat.getLenientFormatters()) {
            if (df.getResolution() < Calendar.DAY_OF_MONTH) {
                continue;
            }

            String sDate = df.getSimpleDateFormat().format(ftsTestDate);

            if (sDate.length() >= 9) {
                assertAQuery("\\@"
                        + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(
                        TEST_NAMESPACE, "date-ista").toString()) + ":\"" + sDate + "\"", 1);
            }

            assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                    "datetime-ista").toString()) + ":\"" + sDate + "\"", 1);

            sDate = df.getSimpleDateFormat().format(date);

            assertAQuery("\\@cm\\:CrEaTeD:[MIN TO " + sDate + "]", 1);
            assertAQuery("\\@cm\\:created:[MIN TO NOW]", 1);

            assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_CREATED.toString())
                    + ":[MIN TO " + sDate + "]", 1);

            if (sDate.length() >= 9) {
                sDate = df.getSimpleDateFormat().format(ftsTestDate);

                assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(
                        TEST_NAMESPACE, "date-ista").toString()) + ":[" + sDate
                        + " TO " + sDate + "]", 1);

                assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(
                        TEST_NAMESPACE, "date-ista").toString()) + ":[MIN  TO " + sDate
                        + "]", 1);

                assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(
                        TEST_NAMESPACE, "date-ista").toString()) + ":[" + sDate
                        + " TO MAX]", 1);


            }

            sDate = CachingDateFormat.getDateFormat().format(ftsTestDate);

            assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(TEST_NAMESPACE,
                    "datetime-ista").toString()) + ":[MIN TO " + sDate + "]", 1);


            sDate = df.getSimpleDateFormat().format(ftsTestDate);
            for (long i : new long[]{333, 20000, 20 * 60 * 1000, 8 * 60 * 60 * 1000, 10 * 24 * 60 * 60 * 1000,
                    4 * 30 * 24 * 60 * 60 * 1000, 10 * 12 * 30 * 24 * 60 * 60 * 1000}) {

                String startDate = df.getSimpleDateFormat().format(new Date(ftsTestDate.getTime() - i));
                String endDate = df.getSimpleDateFormat().format(new Date(ftsTestDate.getTime() + i));


                assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(
                                TEST_NAMESPACE, "datetime-ista").toString()) + ":[" + startDate
                                + " TO " + endDate + "]", 1);
                assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(
                                TEST_NAMESPACE, "datetime-ista").toString()) + ":[" + sDate
                                + " TO " + endDate + "]", 1);
                assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(
                                TEST_NAMESPACE, "datetime-ista").toString()) + ":[" + startDate
                                + " TO " + sDate + "]", 1);
                assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(
                                TEST_NAMESPACE, "datetime-ista").toString()) + ":{" + sDate
                                + " TO " + endDate + "}", 0);
                assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(QName.createQName(
                                TEST_NAMESPACE, "datetime-ista").toString()) + ":{" + startDate
                                + " TO " + sDate + "}", 0);
            }
        }

        qname = QName.createQName(TEST_NAMESPACE, "boolean-ista");
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"true\"", 1);

        /**
         * //TODO:
        qname = QName.createQName(TEST_NAMESPACE, "noderef-ista");
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"" + testNodeRef + "\"", 1);

        qname = QName.createQName(TEST_NAMESPACE, "qname-ista");
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"{wibble}wobble\"", 1);

        qname = QName.createQName(TEST_NAMESPACE, "category-ista");
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\""
                        + DefaultTypeConverter.INSTANCE.convert(String.class,
                        new NodeRef(new StoreRef("proto", "id"), "CategoryId")) + "\"", 1);

        qname = QName.createQName(TEST_NAMESPACE, "path-ista");
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"/{"
                        + NamespaceService.CONTENT_MODEL_1_0_URI + "}three\"", 1);
        **/

        qname = QName.createQName(TEST_NAMESPACE, "any-many-ista");
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"100\"", 1);
        assertAQuery("\\@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"anyValueAsString\"", 1);

        assertAQuery("TEXT:\"Tutorial Alfresco\"~0", 0);
        assertAQuery("TEXT:\"Tutorial Alfresco\"~1", 0);
        assertAQuery("TEXT:\"Tutorial Alfresco\"~2", 1);
        assertAQuery( "TEXT:\"Tutorial Alfresco\"~3", 1);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"Alfresco Tutorial\"", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"Tutorial Alfresco\"", 0);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"Tutorial Alfresco\"~0", 0);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"Tutorial Alfresco\"~1", 0);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"Tutorial Alfresco\"~2", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(ContentModel.PROP_DESCRIPTION.toString())
                        + ":\"Tutorial Alfresco\"~3", 1);


        qname = QName.createQName(TEST_NAMESPACE, "mltext-many-ista");
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":лемур", 1, (new Locale(
                        "ru")), null, null);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":lemur", 1, (new Locale(
                        "en")), null, null);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":chou", 1, (new Locale(
                        "fr")), null, null);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":cabbage", 1,
                (new Locale("en")), null, null);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":cabba*", 1, (new Locale(
                        "en")), null, null);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":ca*ge", 1, (new Locale(
                        "en")), null, null);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":*bage", 1, (new Locale(
                        "en")), null, null);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":cabage~", 1,
                (new Locale("en")), null, null);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":*b?ag?", 1, (new Locale(
                        "en")), null, null);

        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":cho*", 1, (new Locale(
                        "fr")), null, null);

        
        qname = QName.createQName(TEST_NAMESPACE, "locale-ista");
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"en_GB_\"", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":en_GB_", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":en_*", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":*_GB_*", 1);
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":*_gb_*", 1);

        qname = QName.createQName(TEST_NAMESPACE, "period-ista");
        assertAQuery("@" + SearchLanguageConversion.escapeLuceneQuery(qname.toString()) + ":\"period|12\"", 1);

    }

    private void testAFTSandSort() {

        assertAQueryIsSorted("PATH:\"//.\"",
                             "@" + ContentModel.PROP_CONTENT.toString() + ".size asc",
                              null,
                              16,
                              new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });

        assertAQueryIsSorted("PATH:\"//.\"",
                "@" + ContentModel.PROP_CONTENT.toString() + ".size desc",
                null,
                16,
                new Integer[] { 15});

        assertAQueryIsSorted("PATH:\"//.\"",
                ContentModel.PROP_CONTENT.toString() + ".size asc",
                null,
                16,
                new Integer[] { null, null, null, null, null, null, null, null, null, null, null, null, null, null, null, 15 });

        assertAQueryIsSorted("PATH:\"//.\"",
                ContentModel.PROP_CONTENT.toString() + ".size desc",
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
                "@" + ContentModel.PROP_NAME.toString()+ " asc",
                null,
                16,
                new Integer[] { 1, 9, 12, 16, 6, 5, 15, 10, 2, 8, 7, 11, 14, 4, 13, 3 });


        assertAQueryIsSorted("-eager or -dog",
                "@" + ContentModel.PROP_NAME.toString() + " desc",
                null,
                16,
                new Integer[] { 3, 13, 4, 14, 11, 7, 8, 2, 10, 15, 5, 6, 16, 12, 9, 1 });

        assertAQueryIsSorted("-eager or -dog",
                ContentModel.PROP_NAME.toString() + " asc",
                null,
                16,
                new Integer[] { 1, 9, 12, 16, 6, 5, 15, 10, 2, 8, 7, 11, 14, 4, 13, 3 });

        assertAQueryIsSorted("-eager or -dog",
                ContentModel.PROP_NAME.toString() + " desc",
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
    }

    private void checkPaging() throws Exception {

        //This test is questionable
        assertPage("PATH:\"//.\"",
                "DBID asc",
                16,
                1000000,
                0,
                new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 });


        assertPage("PATH:\"//.\"",
                   "DBID asc",
                   16,
                   20,
                   0,
                   new Integer[] { 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16 });

        assertPage("PATH:\"//.\"",
                "DBID asc",
                6,
                6,
                0,
                new Integer[] { 1, 2, 3, 4, 5, 6 });

        assertPage("PATH:\"//.\"",
                "DBID asc",
                6,
                6,
                6,
                new Integer[] { 7, 8, 9, 10, 11, 12 });

        assertPage("PATH:\"//.\"",
                "DBID asc",
                4,
                6,
                12,
                new Integer[] { 13, 14, 15, 16 });
    }
    
    
    private void checkAncestor() throws Exception {

    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:0*"), null),
                 "*[count(//doc)=15]");	
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:0/"+rootNodeRef.getId()), null),
                 "*[count(//doc)=15]");	
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:F/"+rootNodeRef.getId()), null),
                 "*[count(//doc)=4]");	
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:0/"+rootNodeRef.getId()+"*"), null),
                 "*[count(//doc)=15]");	
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:0/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=0]");	
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:1/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=11]");	
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:2/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=8]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:3/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=4]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:4/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=4]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:5/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=1]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:6/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=0]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:1/"+rootNodeRef.getId()+"/"+n01NodeRef.getId()), null),
                 "*[count(//doc)=9]");	
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "APATH:1/"+rootNodeRef.getId()+"/"+n01NodeRef.getId()), null),
                 "*[count(//doc)=9]");	
    	 
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:0/"+rootNodeRef.getId()), null),
                 "*[count(//doc)=4]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:F/"+rootNodeRef.getId()), null),
                 "*[count(//doc)=4]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:1/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=5]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:2/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=4]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:3/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=1]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:4/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=3]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:5/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=1]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:5/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=1]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:6/"+rootNodeRef.getId()+"/*"), null),
                 "*[count(//doc)=0]");

    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:0/"+n01NodeRef.getId()), null),
                 "*[count(//doc)=2]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:0/"+n02NodeRef.getId()), null),
                 "*[count(//doc)=3]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:0/"+n03NodeRef.getId()), null),
                 "*[count(//doc)=0]");
    	 
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "ANAME:1/"+rootNodeRef.getId()+"/"+n01NodeRef.getId()), null),
                 "*[count(//doc)=2]");   
    }

    private void testChildNameEscaping() throws Exception {
        assertAQuery("PATH:\"/cm:" + ISO9075.encode(COMPLEX_LOCAL_NAME) + "\"", 1);
        assertAQuery("PATH:\"/cm:" + ISO9075.encode(NUMERIC_LOCAL_NAME) + "\"", 1);
    }
    
    private void testMnt() throws Exception 
    {
    	 assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"one_two\""), null),
                 "*[count(//doc)=3]");	
    	 	  
         assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"Print\""), null),
                  "*[count(//doc)=2]");	
         
         assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"Print-Toolkit\""), null),
                 "*[count(//doc)=2]");	
         
         assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"Print-Toolkit-3204\""), null),
                 "*[count(//doc)=1]");	
    	 
    	  assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"Print-Toolkit-3204-The-Print-Toolkit-has-a-new-look-565022.html\""), null),
                  "*[count(//doc)=1]");	
    	  
    	  assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"*20150911100000*\""), null),
                  "*[count(//doc)=1]");	
    	  
       	  assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"apple pear peach 20150911100000.txt\""), null),
                  "*[count(//doc)=1]");

    	  assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"apple pear * 20150911100000.txt\""), null),
                  "*[count(//doc)=1]");	
    	  
    	  assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"apple * * 20150911100000.txt\""), null),
                  "*[count(//doc)=1]");	
    	  
    	  assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"apple * 20150911100000.txt\""), null),
                  "*[count(//doc)=1]");	
    	 
    	  assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"hello.txt\""), null),
                  "*[count(//doc)=2]");	
    	  
    	  assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"Test.hello.txt\""), null),
                  "*[count(//doc)=1]");	
    	  
    	  assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"Test1.hello.txt\""), null),
                  "*[count(//doc)=1]");

    	  /*
      assertQ(areq(params("rows", "20", "qt", "/afts", "q", "\"AnalystName Craig\""), null), "*[count(//doc)=1]");

      assertQ(areq(params("rows", "20", "qt", "/afts", "q", "\"AnalystName\" AND \"AnalystName Craig\""), null), "*[count(//doc)=1]");

      assertQ(areq(params("rows", "20", "qt", "/afts", "q", "\"AnalystName\" AND !\"AnalystName Craig\""), null), "*[count(//doc)=1]");

      assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:\"BASF*.txt\""), null), "*[count(//doc)=4]");
      */
    }
}