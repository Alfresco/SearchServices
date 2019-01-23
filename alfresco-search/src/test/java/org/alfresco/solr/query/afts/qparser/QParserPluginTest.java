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

package org.alfresco.solr.query.afts.qparser;


import static java.util.Arrays.stream;
import static org.alfresco.model.ContentModel.PROP_CONTENT;
import static org.alfresco.model.ContentModel.PROP_CREATED;
import static org.alfresco.model.ContentModel.PROP_DESCRIPTION;
import static org.alfresco.model.ContentModel.PROP_NAME;
import static org.alfresco.model.ContentModel.TYPE_CONTENT;
import static org.alfresco.model.ContentModel.TYPE_THUMBNAIL;

import org.alfresco.repo.search.adaptor.lucene.QueryConstants;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.query.afts.SharedTestDataProvider;
import org.alfresco.util.CachingDateFormat;
import org.apache.solr.SolrTestCaseJ4;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

@SolrTestCaseJ4.SuppressSSL
public class QParserPluginTest extends AbstractQParserPluginTest implements QueryConstants
{
    private final long [] msecs = {
            333,
            20000,
            20 * 60 * 1000, // 20 minutes
            8 * 60 * 60 * 1000, // 8 hours
            10 * 24 * 60 * 60 * 1000, // 10 days
            4L * 30 * 24 * 60 * 60 * 1000, // 120 days
            10L * 12 * 30 * 24 * 60 * 60 * 1000 // 10 years
    };

    @BeforeClass
    public static void loadData() throws Exception
    {
        SharedTestDataProvider dataProvider = new SharedTestDataProvider(h);

        // For this test we need both small and medium datasets
        dataProvider.loadSmallDataset();
        dataProvider.loadMediumDataset();

        FTS_TEST_DATE = dataProvider.getFtsTestDate();
        TEST_NODEREF = dataProvider.getTestNodeRef();
        TEST_ROOT_NODEREF = dataProvider.getRootNode();
    }

    @Test
    public void rootNodes() 
    {
        assertAQuery("PATH:\"/\"", 1);
        assertAQuery("PATH:\"/.\"", 1);
    }

    @Test
    public void qNames()  
    {
        assertAQuery("QNAME:\"nine\"",1);
        assertAQuery("PRIMARYASSOCTYPEQNAME:\"cm:contains\"",11);
        assertAQuery("PRIMARYASSOCTYPEQNAME:\"sys:children\"",4);
        assertAQuery("ASSOCTYPEQNAME:\"cm:contains\"",11);
        assertAQuery("ASSOCTYPEQNAME:\"sys:children\"",5);
    }

    @Test
    public void pathsWithoutNamespace() 
    {
        assertAQuery("PATH:\"/one\"", 1);
        assertAQuery("PATH:\"/two\"", 1);
        assertAQuery("PATH:\"/three\"", 1);
        assertAQuery("PATH:\"/four\"", 1);
    }

    @Test
    public void oneLevelPaths() 
    {
        assertAQuery("PATH:\"/cm:one\"", 1);
        assertAQuery("PATH:\"/cm:two\"", 1);
        assertAQuery("PATH:\"/cm:three\"", 1);
        assertAQuery("PATH:\"/cm:four\"", 1);
        assertAQuery("PATH:\"/cm:eight-0\"", 1);
        assertAQuery("PATH:\"/cm:five\"", 0);
    }

    @Test
    public void twoLevelsPaths() 
    {
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
    }

    @Test
    public void threeLevelsPaths() 
    {
        assertAQuery("PATH:\"/cm:one/cm:five/cm:nine\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:ten\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:eleven\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve\"", 1);
    }

    @Test
    public void fourLevelsPaths() 
    {
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve/cm:thirteen\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve/cm:common\"", 1);
    }

    @Test
    public void fiveLevelsPaths() 
    {
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve/cm:thirteen/cm:fourteen\"", 1);
        assertAQuery("PATH:\"/cm:one/cm:five/cm:twelve/cm:thirteen/cm:common\"", 1);
    }

    @Test
    public void pathsWithWildcards()
    {
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
    }

    @Test
    public void descendantsOrCurrentNodeExpressionInPaths() 
    {
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
    }

    @Test
    public void typesAndAspects()
    {
        assertAQuery("TYPE:\"" + TEST_TYPE + "\"", 1);
        assertAQuery("TYPE:\"" + TEST_TYPE.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);
        assertAQuery("EXACTTYPE:\"" + TEST_TYPE + "\"", 1);
        assertAQuery("EXACTTYPE:\"" + TEST_TYPE.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);
        assertAQuery("TYPE:\"" + TEST_SUPER_TYPE + "\"", 13);
        assertAQuery("TYPE:\"" + TEST_SUPER_TYPE.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 13);
        assertAQuery("TYPE:\"" + TYPE_CONTENT + "\"", 1);
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
        assertAQuery("TYPE:\"" + TYPE_THUMBNAIL.toString() + "\"", 1);
        assertAQuery("TYPE:\"" + TYPE_THUMBNAIL.toString() + "\" TYPE:\"" + TYPE_CONTENT + "\"", 2);
        assertAQuery("EXACTTYPE:\"" + TEST_SUPER_TYPE + "\"", 12);
        assertAQuery("EXACTTYPE:\"" + TEST_SUPER_TYPE.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 12);
        assertAQuery("ASPECT:\"" + TEST_ASPECT + "\"", 1);
        assertAQuery("ASPECT:\"" + TEST_ASPECT.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);
        assertAQuery("EXACTASPECT:\"" + TEST_ASPECT + "\"", 1);
        assertAQuery("EXACTASPECT:\"" + TEST_ASPECT.toPrefixString(dataModel.getNamespaceDAO()) + "\"", 1);
    }

    @Test
    public void dataTypes() 
    {
        assertAQuery("d\\:double:\"5.6\"", 1);
        assertAQuery("d\\:int:\"1\"", 1);
        assertAQuery("d\\:float:\"3.4\"", 1);
        assertAQuery("d\\:content:\"fox\"", 1);
        assertAQuery("d\\:content:\"fox\"", 1, Locale.US, null, null);
    }

    @Test
    public void fulltext()
    {
        assertAQuery("TEXT:fox AND TYPE:\"" + PROP_CONTENT + "\"", 1);
        assertAQuery("TEXT:fox @cm\\:name:fox", 1);
        assertAQuery("d\\:content:fox d\\:text:fox", 1);
        assertAQuery("TEXT:fo AND TYPE:\"" + PROP_CONTENT + "\"", 0);
        assertAQuery("TEXT:\"fox\"", 1);
        assertAQuery("TEXT:\"fox\"", 0, null, new String[]{ "@" + PROP_NAME }, null);
        assertAQuery("TEXT:\"fox\"", 1, null, new String[]{ "@" + PROP_NAME, "@" + PROP_CONTENT }, null);
        assertAQuery("TEXT:\"cabbage\"", 15, null, new String[]{ "@" + ORDER_TEXT }, null);
        assertAQuery("TEXT:\"aeidnouy\"", 1);

        // Depends on the configuration
        assertAQuery("TEXT:\"the\"", 1);
        assertAQuery("TEXT:\"and\"", 1);
        assertAQuery("TEXT:\"the lazy dog\"", 1);
        assertAQuery("TEXT:\"over lazy\"", 0);
        assertAQuery("TEXT:\"over the lazy dog\"", 1);
        assertAQuery("TEXT:\"over the lazy\"", 1);

        //With no shared.properties this falls back to 5.0 cross locale support
        assertAQuery("TEXT:\"over a lazy\"", 1);

        String contentFieldName = escape(PROP_CONTENT);
        assertAQuery("@" + contentFieldName + ":\"fox\"", 1);
        assertAQuery("@" + contentFieldName + ".mimetype:\"text/plain\"", 1);
        assertAQuery("@" + contentFieldName + ".locale:\"en_GB\"", 1);
        assertAQuery("@" + contentFieldName + ".size:\"298\"", 1);
    }

    @Test
    public void wildcards()
    {
        final String indexedStoredTokenisedAtomicFieldName = escape(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-atomic"));
        assertAQuery("\\@" + indexedStoredTokenisedAtomicFieldName + ":*a*", 1);
        assertAQuery("\\@" + indexedStoredTokenisedAtomicFieldName + ":*A*", 1);
        assertAQuery("\\@" + indexedStoredTokenisedAtomicFieldName + ":\"*a*\"", 1);
        assertAQuery("\\@" + indexedStoredTokenisedAtomicFieldName + ":\"*A*\"", 1);
        assertAQuery("\\@" + indexedStoredTokenisedAtomicFieldName + ":*s*", 1);
        assertAQuery("\\@" + indexedStoredTokenisedAtomicFieldName + ":*S*", 1);
        assertAQuery("\\@" + indexedStoredTokenisedAtomicFieldName + ":\"*s*\"", 1);
        assertAQuery("\\@" + indexedStoredTokenisedAtomicFieldName + ":\"*S*\"", 1);
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
        assertAQuery("TEXT:\"*bage\"", 15, null, new String[]{ "@" + ORDER_TEXT }, null);
        assertAQuery("TEXT:\"*ba*\"", 15, null, new String[]{ "@" + ORDER_TEXT }, null);
        assertAQuery("TEXT:cabbage", 15, null, new String[]{ "@" + ORDER_TEXT }, null);
        assertAQuery("TEXT:*cab*", 15, Locale.ENGLISH, new String[]{ "@" + ORDER_TEXT }, null);
        assertAQuery("TEXT:*bage", 15, null, new String[]{ "@" + ORDER_TEXT }, null);

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
        assertAQuery("TEXT:\"??lf**sc***\"", 0);
        assertAQuery("TEXT:\"alfresc*tutorial\"", 0);
        assertAQuery("TEXT:\"alf* tut*\"", 1);
        assertAQuery("TEXT:\"*co *al\"", 1);

        assertAQuery("TEXT:\"cab*\"", 15, null, new String[]{ "@" + ORDER_TEXT }, null);

        String descriptionFieldName = escape(PROP_DESCRIPTION);
        assertAQuery("@" + descriptionFieldName + ":\"alfresco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"alfresc?\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"alfres??\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"alfre???\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"alfr????\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"alf?????\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"al??????\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"a???????\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"????????\"", 1);

        assertAQuery("@" + descriptionFieldName + ":\"a??re???\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"?lfresco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"??fresco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"???resco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"????esco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"?????sco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"??????co\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"???????o\"", 1);

        assertAQuery("@" + descriptionFieldName + ":\"???resco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"???res?o\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"????e?co\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"????e?c?\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"???re???\"", 1);

        assertAQuery("@" + descriptionFieldName + ":\"alfresc*\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"alfres*\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"alfre*\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"alfr*\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"alf*\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"al*\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"a*\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"a*****\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"*lfresco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"*fresco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"*resco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"*esco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"*sco\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"*co\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"*o\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"****lf**sc***\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"*??*lf**sc***\"", 0);
        assertAQuery("@" + descriptionFieldName + ":\"Alfresc*tutorial\"", 0);
        assertAQuery("@" + descriptionFieldName + ":\"Alf* tut*\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"*co *al\"", 1);
    }

    @Test
    public void accents()
    {
        assertAQuery("TEXT:\"\u00E0\u00EA\u00EE\u00F0\u00F1\u00F6\u00FB\u00FF\"", 1);
    }

    @Test
    public void mlText() 
    {
        String contentFieldName = escape(PROP_CONTENT);
        assertAQuery("@" + contentFieldName + ".locale:en_*", 1);
        assertAQuery("@" + contentFieldName + ".locale:e*_GB", 1);

        String fieldName = escape(QName.createQName(TEST_NAMESPACE, "ml"));

        assertAQuery("@" + fieldName + ":and", 0);
        assertAQuery("@" + fieldName + ":\"and\"", 0);
        assertAQuery("@" + fieldName + ":banana", 1);

        assertAQuery("@" + fieldName + ":banana", 1, Locale.UK,null, null);
        assertAQuery("@" + fieldName + ":banana", 1, Locale.ENGLISH, null, null);
        assertAQuery("@" + fieldName + ":banane", 1, Locale.FRENCH, null, null);
        assertAQuery("@" + fieldName + ":香蕉", 1, Locale.CHINESE, null, null);
        assertAQuery("@" + fieldName + ":banaan", 1, new Locale("nl"), null, null);
        assertAQuery("@" + fieldName + ":banane", 1, Locale.GERMAN, null, null);
        assertAQuery("@" + fieldName + ":μπανάνα", 1, new Locale("el"), null, null);
        assertAQuery("@" + fieldName + ":banana", 1, Locale.ITALIAN, null, null);
        assertAQuery("@" + fieldName    + ":バナナ", 1, Locale.JAPANESE, null, null);
        assertAQuery("@" + fieldName + ":바나나", 1, new Locale("ko"), null, null);
        assertAQuery("@" + fieldName + ":banana", 1, new Locale("pt"), null, null);
        assertAQuery("@" + fieldName + ":банан", 1, new Locale("ru"), null, null);
        assertAQuery("@" + fieldName + ":plátano", 1, new Locale("es"), null, null);
    }

    @Test
    public void textRanges()
    {
        assertAQuery("\\@" + escape(ORDER_TEXT) + ":[a TO b]", 1);
        assertAQuery("\\@" + escape(ORDER_TEXT) + ":[a TO \uFFFF]", 14);
        assertAQuery("\\@" + escape(ORDER_TEXT) + ":[* TO b]", 2);
        assertAQuery("\\@" + escape(ORDER_TEXT) + ":[\u0000 TO b]", 2);
        assertAQuery("\\@" + escape(ORDER_TEXT) + ":[d TO \uFFFF]", 12);
        assertAQuery("\\@" + escape(ORDER_TEXT) + ":[d TO *]", 12);
    }

    @Test
    public void nonFields() 
    {
        assertAQuery("TEXT:fox", 1);
        assertAQuery("TEXT:fo*", 1);
        assertAQuery("TEXT:f*x", 1);
        assertAQuery("TEXT:*ox", 1);

        String contentFieldName = escape(PROP_CONTENT);

        assertAQuery("@" + contentFieldName + ":fox", 1);
        assertAQuery("@" + contentFieldName + ":fo*", 1);
        assertAQuery("@" + contentFieldName + ":f*x", 1);
        assertAQuery("@" + contentFieldName + ":*ox", 1);
        assertAQuery("@" + escape(PROP_CONTENT.toPrefixString(dataModel.getNamespaceDAO())) + ":fox", 1);
        assertAQuery("@" + escape(PROP_CONTENT.toPrefixString(dataModel.getNamespaceDAO())) + ":fo*", 1);
        assertAQuery("@" + escape(PROP_CONTENT.toPrefixString(dataModel.getNamespaceDAO())) + ":f*x", 1);
        assertAQuery("@" + escape(PROP_CONTENT.toPrefixString(dataModel.getNamespaceDAO())) + ":*ox", 1);
    }

    @Test
    public void nullAndUnsetFields() 
    {
        assertAQuery("ISUNSET:\"" + QName.createQName(TEST_NAMESPACE, "null") + "\"", 0);
        assertAQuery("ISNULL:\"" + QName.createQName(TEST_NAMESPACE, "null") + "\"", 1);
        assertAQuery("EXISTS:\"" + QName.createQName(TEST_NAMESPACE, "null") + "\"", 1);
        assertAQuery("ISNOTNULL:\"" + QName.createQName(TEST_NAMESPACE, "null") + "\"", 0);

        assertAQuery("ISUNSET:\"" + QName.createQName(TEST_NAMESPACE, "path-ista") + "\"", 0);
        assertAQuery("ISNULL:\"" + QName.createQName(TEST_NAMESPACE, "path-ista") + "\"", 0);
        assertAQuery("ISNOTNULL:\"" + QName.createQName(TEST_NAMESPACE, "path-ista") + "\"", 1);
        assertAQuery("EXISTS:\"" + QName.createQName(TEST_NAMESPACE, "path-ista") + "\"", 1);

        assertAQuery("ISUNSET:\"" + QName.createQName(TEST_NAMESPACE, "aspectProperty") + "\"", 0);
        assertAQuery("ISNULL:\"" + QName.createQName(TEST_NAMESPACE, "aspectProperty") + "\"", 0);
        assertAQuery("ISNOTNULL:\"" + QName.createQName(TEST_NAMESPACE, "aspectProperty") + "\"", 1);
        assertAQuery("EXISTS:\"" + QName.createQName(TEST_NAMESPACE, "aspectProperty") + "\"", 1);
    }

    @Test
    public void intPropertyType() 
    {
        String intFieldName = escape(QName.createQName(TEST_NAMESPACE, "int-ista"));

        assertAQuery("\\@" + intFieldName + ":\"1\"", 1);
        assertAQuery("\\@" + intFieldName + ":1", 1);
        assertAQuery("\\@" + intFieldName + ":\"01\"", 1);
        assertAQuery("\\@" + intFieldName + ":01", 1);
        assertAQuery("\\@" + intFieldName + ":\"001\"", 1);
        assertAQuery("\\@" + intFieldName + ":\"0001\"", 1);

        assertAQuery("\\@" + intFieldName + ":[A TO 2]", 1);
        assertAQuery("\\@" + intFieldName + ":[0 TO 2]", 1);
        assertAQuery("\\@" + intFieldName + ":[0 TO A]", 1);

        assertAQuery("\\@" + intFieldName + ":{A TO 1}", 0);
        assertAQuery("\\@" + intFieldName + ":{0 TO 1}", 0);
        assertAQuery("\\@" + intFieldName + ":{0 TO A}", 1);

        assertAQuery("\\@" + intFieldName + ":{A TO 2}", 1);
        assertAQuery("\\@" + intFieldName + ":{1 TO 2}", 0);
        assertAQuery("\\@" + intFieldName + ":{1 TO A}", 0);

    }

    @Test
    public void longPropertyType() 
    {
        String longFieldName = escape(QName.createQName(TEST_NAMESPACE, "long-ista"));

        assertAQuery("\\@" + longFieldName + ":\"2\"", 1);
        assertAQuery("\\@" + longFieldName + ":\"02\"", 1);
        assertAQuery("\\@" + longFieldName + ":\"002\"", 1);

        assertAQuery("\\@" + longFieldName + ":\"0002\"", 1);
        assertAQuery("\\@" + longFieldName + ":[A TO 2]", 1);
        assertAQuery("\\@" + longFieldName + ":[0 TO 2]", 1);

        assertAQuery("\\@" + longFieldName + ":[0 TO A]", 1);
        assertAQuery("\\@" + longFieldName + ":{A TO 2}", 0);
        assertAQuery("\\@" + longFieldName + ":{0 TO 2}", 0);

        assertAQuery("\\@" + longFieldName + ":{0 TO A}", 1);
        assertAQuery("\\@" + longFieldName + ":{A TO 3}", 1);
        assertAQuery("\\@" + longFieldName + ":{2 TO 3}", 0);

        assertAQuery("\\@" + longFieldName + ":{2 TO A}", 0);
    }

    @Test
    public void floatPropertyType()
    {
        String floatFieldName = escape(QName.createQName(TEST_NAMESPACE, "float-ista"));

        assertAQuery("\\@" + floatFieldName + ":\"3.4\"", 1);
        assertAQuery("\\@" + floatFieldName + ":[A TO 4]", 1);
        assertAQuery("\\@" + floatFieldName + ":[3 TO 4]", 1);

        assertAQuery("\\@" + floatFieldName + ":[3 TO A]", 1);
        assertAQuery("\\@" + floatFieldName + ":[A TO 3.4]", 1);
        assertAQuery("\\@" + floatFieldName + ":[3.3 TO 3.4]", 1);

        assertAQuery("\\@" + floatFieldName + ":[3.3 TO A]", 1);
        assertAQuery("\\@" + floatFieldName + ":{A TO 3.4}", 0);
        assertAQuery("\\@" + floatFieldName + ":{3.3 TO 3.4}", 0);

        assertAQuery("\\@" + floatFieldName + ":{3.3 TO A}", 1);
        assertAQuery("\\@" + floatFieldName + ":\"3.40\"", 1);
        assertAQuery("\\@" + floatFieldName + ":\"03.4\"", 1);

        assertAQuery("\\@" + floatFieldName + ":\"03.40\"", 1);
    }

    @Test
    public void missingProperty()
    {
        assertAQuery("\\@" + escape("My-funny&MissingProperty:woof"), 0);
    }

    @Test
    public void doublePropertyType()
    {
        String doubleFieldName = escape(QName.createQName(TEST_NAMESPACE, "double-ista"));

        assertAQuery("\\@" + doubleFieldName + ":\"5.6\"", 1);
        assertAQuery("\\@" + doubleFieldName + ":\"05.6\"", 1);
        assertAQuery("\\@" + doubleFieldName + ":\"5.60\"", 1);

        assertAQuery("\\@" + doubleFieldName + ":\"05.60\"", 1);
        assertAQuery("\\@" + doubleFieldName + ":[A TO 5.7]", 1);
        assertAQuery("\\@" + doubleFieldName + ":[5.5 TO 5.7]", 1);

        assertAQuery("\\@" + doubleFieldName + ":[5.5 TO A]", 1);
        assertAQuery("\\@" + doubleFieldName + ":{A TO 5.6}", 0);
        assertAQuery("\\@" + doubleFieldName + ":{5.5 TO 5.6}", 0);

        assertAQuery("\\@" + doubleFieldName + ":{5.6 TO A}", 0);
    }

    @Test
    public void dateLiterals()
    {
        stream(CachingDateFormat.getLenientFormatters())
            .filter(formatter -> formatter.getResolution() < Calendar.DAY_OF_MONTH)
            .map(formatter -> formatter.getSimpleDateFormat().format(FTS_TEST_DATE))
            .filter(date -> date.length() >= 9)
            .forEach(date -> assertAQuery("\\@" + escape(QName.createQName(TEST_NAMESPACE, "date-ista")) + ":\"" + date + "\"", 1));
    }

    @Test
    public void dateTimeLiterals()
    {
        stream(CachingDateFormat.getLenientFormatters())
                .filter(formatter -> formatter.getResolution() > Calendar.DAY_OF_MONTH)
                .map(formatter -> formatter.getSimpleDateFormat().format(FTS_TEST_DATE))
                .forEach(date -> assertAQuery("\\@" + escape(QName.createQName(TEST_NAMESPACE, "datetime-ista")) + ":\"" + date + "\"", 1));
    }


    @Test
    public void fieldNamesAreCaseInsensitive()
    {
        Date now = new Date();
        stream(CachingDateFormat.getLenientFormatters())
                .filter(formatter -> formatter.getResolution() > Calendar.DAY_OF_MONTH)
                .map(formatter -> formatter.getSimpleDateFormat().format(now))
                .forEach(date -> {
                    assertAQuery("\\@cM\\:CrEaTeD:[MIN TO " + date + "]", 1);
                    assertAQuery("\\@cM\\:CrEaTeD:[MIN TO " + date + "]", 1);
                    assertAQuery("\\@CM\\:CrEaTeD:[MIN TO " + date + "]", 1);
                    assertAQuery("\\@cm\\:created:[MIN TO NOW]", 1);
                    assertAQuery("\\@" + escape(PROP_CREATED) + ":[MIN TO " + date + "]", 1);
                });
    }

    @Test
    public void dateRanges()
    {
        String fieldName = escape(QName.createQName(TEST_NAMESPACE, "date-ista"));
        stream(CachingDateFormat.getLenientFormatters())
                .filter(formatter -> formatter.getResolution() > Calendar.DAY_OF_MONTH)
                .map(formatter -> formatter.getSimpleDateFormat().format(FTS_TEST_DATE))
                .filter(date -> date.length() >= 9)
                .forEach(date -> {
                    assertAQuery("\\@" + fieldName + ":[" + date + " TO " + date + "]", 1);
                    assertAQuery("\\@" + fieldName + ":[MIN  TO " + date + "]", 1);
                    assertAQuery("\\@" + fieldName + ":[" + date + " TO MAX]", 1);
                });
    }

    @Test
    public void dateTimeRanges()
    {
        String fieldName = escape(QName.createQName(TEST_NAMESPACE, "datetime-ista"));
        stream(CachingDateFormat.getLenientFormatters())
                .filter(formatter -> formatter.getResolution() > Calendar.DAY_OF_MONTH)
                .forEach(formatter -> {
                    String date = formatter.getSimpleDateFormat().format(FTS_TEST_DATE);

                    stream(msecs)
                            .forEach(milliseconds ->
                            {
                                String startDate = formatter.getSimpleDateFormat().format(new Date(FTS_TEST_DATE.getTime() - milliseconds));
                                String endDate = formatter.getSimpleDateFormat().format(new Date(FTS_TEST_DATE.getTime() + milliseconds));

                                assertAQuery("\\@" + fieldName + ":[" + startDate + " TO " + endDate + "]", 1);
                                assertAQuery("\\@" + fieldName + ":[" + date + " TO " + endDate + "]", 1);
                                assertAQuery("\\@" + fieldName + ":[" + startDate + " TO " + date + "]", 1);
                                assertAQuery("\\@" + fieldName + ":{" + date + " TO " + endDate + "}", 0);
                                assertAQuery("\\@" + fieldName + ":{" + startDate + " TO " + date + "}", 0);
                            });
                });
    }

    @Test
    public void booleanPropertyType()
    {
        String booleanFieldName = escape(QName.createQName(TEST_NAMESPACE, "boolean-ista"));
        assertAQuery("\\@" + booleanFieldName + ":\"true\"", 1);
        assertAQuery("\\@" + booleanFieldName + ":\"false\"", 0);
    }

    @Test
    public void anyPropertyType() 
    {
        String anyFieldName = escape(QName.createQName(TEST_NAMESPACE, "any-many-ista"));
        assertAQuery("\\@" + anyFieldName + ":\"100\"", 1);
        assertAQuery("\\@" + anyFieldName + ":\"anyValueAsString\"", 1);
    }

    @Test
    public void proximity() 
    {
        assertAQuery("TEXT:\"Tutorial Alfresco\"~0", 0);
        assertAQuery("TEXT:\"Tutorial Alfresco\"~1", 0);
        assertAQuery("TEXT:\"Tutorial Alfresco\"~2", 1);
        assertAQuery( "TEXT:\"Tutorial Alfresco\"~3", 1);

        String descriptionFieldName = escape(PROP_DESCRIPTION);
        assertAQuery("@" + descriptionFieldName + ":\"Alfresco Tutorial\"", 1);
        assertAQuery("@" + descriptionFieldName + ":\"Tutorial Alfresco\"", 0);
        assertAQuery("@" + descriptionFieldName + ":\"Tutorial Alfresco\"~0", 0);
        assertAQuery("@" + descriptionFieldName + ":\"Tutorial Alfresco\"~1", 0);
        assertAQuery("@" + descriptionFieldName + ":\"Tutorial Alfresco\"~2", 1);
        assertAQuery("@" + descriptionFieldName + ":\"Tutorial Alfresco\"~3", 1);
    }

    @Test
    public void mlPropertyType()
    {
        String mlTextFieldName = escape(QName.createQName(TEST_NAMESPACE, "mltext-many-ista"));
        assertAQuery("@" + mlTextFieldName + ":лемур", 1, new Locale("ru"), null, null);
        assertAQuery("@" + mlTextFieldName + ":lemur", 1, new Locale("en"), null, null);
        assertAQuery("@" + mlTextFieldName + ":chou", 1, new Locale("fr"), null, null);
        assertAQuery("@" + mlTextFieldName + ":cabbage", 1, new Locale("en"), null, null);
        assertAQuery("@" + mlTextFieldName + ":cabba*", 1, new Locale("en"), null, null);
        assertAQuery("@" + mlTextFieldName + ":ca*ge", 1, new Locale("en"), null, null);
        assertAQuery("@" + mlTextFieldName + ":*bage", 1, new Locale("en"), null, null);
        assertAQuery("@" + mlTextFieldName + ":cabage~", 1, new Locale("en"), null, null);
        assertAQuery("@" + mlTextFieldName + ":*b?ag?", 1, new Locale("en"), null, null);
        assertAQuery("@" + mlTextFieldName + ":cho*", 1, new Locale("fr"), null, null);

        String localeFieldName = escape(QName.createQName(TEST_NAMESPACE,"locale-ista"));
        assertAQuery("@" + localeFieldName + ":\"en_GB_\"", 1);
        assertAQuery("@" + localeFieldName + ":en_GB_", 1);
        assertAQuery("@" + localeFieldName + ":en_*", 1);
        assertAQuery("@" + localeFieldName + ":*_GB_*", 1);
        assertAQuery("@" + localeFieldName + ":*_gb_*", 1);

        assertAQuery("@" + escape(QName.createQName(TEST_NAMESPACE, "period-ista")) + ":\"period|12\"", 1);
    }
}