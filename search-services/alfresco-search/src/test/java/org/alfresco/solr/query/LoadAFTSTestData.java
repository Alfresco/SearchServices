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
package org.alfresco.solr.query;

import static org.alfresco.solr.AlfrescoSolrUtils.addNode;
import static org.alfresco.solr.AlfrescoSolrUtils.addStoreRoot;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.Duration;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.AlfrecsoSolrConstants;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.ContentPropertyValue;
import org.alfresco.solr.client.MLTextPropertyValue;
import org.alfresco.solr.client.MultiPropertyValue;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.util.ISO9075;
import org.apache.solr.core.SolrCore;
import org.junit.BeforeClass;
/**
 * Load test data as part of legacy test.
 * @author Michael Suzuki
 *
 */
public class LoadAFTSTestData extends AbstractAlfrescoSolrTests implements AlfrecsoSolrConstants
{

    protected static final String COMPLEX_LOCAL_NAME = "\u0020\u0060\u00ac\u00a6\u0021\"\u00a3\u0024\u0025\u005e\u0026\u002a\u0028\u0029\u002d\u005f\u003d\u002b\t\n\\\u0000\u005b\u005d\u007b\u007d\u003b\u0027\u0023\u003a\u0040\u007e\u002c\u002e\u002f\u003c\u003e\u003f\\u007c\u005f\u0078\u0054\u0036\u0035\u0041\u005f";
    protected static final String NUMERIC_LOCAL_NAME = "12Woof12";
    private static ChildAssociationRef n01CAR;
    private static ChildAssociationRef n02CAR;
    private static ChildAssociationRef n03CAR;
    private static ChildAssociationRef n04CAR;
    private static ChildAssociationRef n05CAR;
    private static ChildAssociationRef n06CAR;
    private static ChildAssociationRef n07CAR;
    private static ChildAssociationRef n08CAR_0;
    private static ChildAssociationRef n08CAR_1;
    private static ChildAssociationRef n08CAR_2;
    private static ChildAssociationRef n09CAR;
    private static ChildAssociationRef n10CAR;
    private static ChildAssociationRef n11CAR;
    private static ChildAssociationRef n12CAR;
    private static ChildAssociationRef n13CAR;
    private static ChildAssociationRef n13CARLink;
    private static ChildAssociationRef n14CAR;
    private static ChildAssociationRef n14CAR_1;
    private static ChildAssociationRef n14CAR_2;
    private static ChildAssociationRef n14CAR_5;
    private static ChildAssociationRef n14CAR_6;
    private static ChildAssociationRef n14CAR_12;
    private static ChildAssociationRef n14CAR_13;
    private static ChildAssociationRef n15CAR;
    private static ChildAssociationRef n19CAR;
    private static ChildAssociationRef n20CAR;
    private static ChildAssociationRef n21CAR;
    private static ChildAssociationRef n22CAR;
    private static ChildAssociationRef n23CAR;
    private static ChildAssociationRef n24CAR;
    private static ChildAssociationRef n25CAR;
    private static ChildAssociationRef n26CAR;
    

    protected static NodeRef n01NodeRef;
    protected static NodeRef n02NodeRef;
    protected static NodeRef n03NodeRef;
    protected static NodeRef n04NodeRef;
    protected static NodeRef n05NodeRef;
    protected static NodeRef n06NodeRef;
    protected static NodeRef n07NodeRef;
    protected static NodeRef n08NodeRef;
    protected static NodeRef n09NodeRef;
    protected static NodeRef n10NodeRef;
    protected static NodeRef n11NodeRef;
    protected static NodeRef n12NodeRef;
    protected static NodeRef n13NodeRef;
    protected static NodeRef n14NodeRef;
    protected static NodeRef n15NodeRef;
    protected static NodeRef n19NodeRef;
    protected static NodeRef n20NodeRef;
    protected static NodeRef n21NodeRef;
    protected static NodeRef n22NodeRef;
    protected static NodeRef n23NodeRef;
    protected static NodeRef n24NodeRef;
    protected static NodeRef n25NodeRef;
    protected static NodeRef n26NodeRef;

    protected static NodeRef rootNodeRef;
    private static QName n01QName;
    private static QName n02QName;
    private static QName n03QName;
    private static QName n04QName;
    private static QName n05QName;
    private static QName n06QName;
    private static QName n07QName;
    private static QName n08QName_0;
    private static QName n08QName_1;
    private static QName n08QName_2;
    private static QName n09QName;
    private static QName n10QName;
    private static QName n11QName;
    private static QName n12QName;
    private static QName n13QName;
    private static QName n13QNameLink;
    private static QName n14QName;
    private static QName n14QNameCommon;
    private static QName n15QName;
    private static QName n19QName;
    private static QName n20QName;
    private static QName n21QName;
    private static QName n22QName;
    private static QName n23QName;
    private static QName n24QName;
    private static QName n25QName;
    private static QName n26QName;



    private static HashMap<QName, PropertyValue> properties04;
    private static HashMap<QName, String> content04;
    private static HashMap<QName, PropertyValue> properties14;
    private static HashMap<QName, String> content14;
    private static HashMap<QName, PropertyValue> properties15;
    private static HashMap<QName, String> content15;
    private static HashMap<QName, PropertyValue> properties19;
    private static HashMap<QName, PropertyValue> properties20;
    private static HashMap<QName, PropertyValue> properties21;
    private static HashMap<QName, PropertyValue> properties22;
    private static HashMap<QName, PropertyValue> properties23;
    private static HashMap<QName, PropertyValue> properties24;
    private static HashMap<QName, PropertyValue> properties25;
    private static HashMap<QName, PropertyValue> properties26;


    @BeforeClass
    public static void loadTestSet() throws Exception 
    {

        initAlfrescoCore("schema.xml");
        Thread.sleep(1000);
        // Root
        SolrCore core = h.getCore();
        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        dataModel.getNamespaceDAO().removePrefix("");
        dataModel.setCMDefaultUri();

        rootNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        addStoreRoot(core, dataModel, rootNodeRef, 1, 1, 1, 1);

        // 1
        n01NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n01QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "one");
        n01CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n01QName,
                n01NodeRef, true, 0);
        addNode(core, dataModel, 1, 2, 1, testSuperType, null, getOrderProperties(), null, "andy",
                new ChildAssociationRef[]{n01CAR}, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + n01QName.toString()}, n01NodeRef, true);

        testNodeRef = n01NodeRef;

        // 2

        n02NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n02QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "two");
        n02CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n02QName,
                n02NodeRef, true, 0);
        addNode(core, dataModel, 1, 3, 1, testSuperType, null, getOrderProperties(), null, "bob",
                new ChildAssociationRef[]{n02CAR}, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + n02QName.toString()}, n02NodeRef, true);

        // 3

        n03NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n03QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "three");
        n03CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n03QName,
                n03NodeRef, true, 0);
        addNode(core, dataModel, 1, 4, 1, testSuperType, null, getOrderProperties(), null, "cid",
                new ChildAssociationRef[]{n03CAR}, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + n03QName.toString()}, n03NodeRef, true);

        // 4

        properties04 = new HashMap<QName, PropertyValue>();
        content04 = new HashMap<QName, String>();
        properties04.putAll(getOrderProperties());
        properties04.put(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-atomic"),
                new StringPropertyValue("TEXT THAT IS INDEXED STORED AND TOKENISED ATOMICALLY KEYONE"));
        properties04.put(QName.createQName(TEST_NAMESPACE, "text-indexed-unstored-tokenised-atomic"),
                new StringPropertyValue("TEXT THAT IS INDEXED STORED AND TOKENISED ATOMICALLY KEYUNSTORED"));
        properties04.put(QName.createQName(TEST_NAMESPACE, "text-indexed-stored-tokenised-nonatomic"),
                new StringPropertyValue("TEXT THAT IS INDEXED STORED AND TOKENISED BUT NOT ATOMICALLY KEYTWO"));
        properties04.put(QName.createQName(TEST_NAMESPACE, "int-ista"), new StringPropertyValue("1"));
        properties04.put(QName.createQName(TEST_NAMESPACE, "long-ista"), new StringPropertyValue("2"));
        properties04.put(QName.createQName(TEST_NAMESPACE, "float-ista"), new StringPropertyValue("3.4"));
        properties04.put(QName.createQName(TEST_NAMESPACE, "double-ista"), new StringPropertyValue("5.6"));

        Calendar c = new GregorianCalendar();
        c.setTime(new Date(((new Date().getTime() - 10000))));
        Date testDate = c.getTime();
        ftsTestDate = testDate;
        properties04.put(QName.createQName(TEST_NAMESPACE, "date-ista"), new StringPropertyValue(
                DefaultTypeConverter.INSTANCE.convert(String.class, testDate)));
        properties04.put(QName.createQName(TEST_NAMESPACE, "datetime-ista"), new StringPropertyValue(
                DefaultTypeConverter.INSTANCE.convert(String.class, testDate)));
        properties04.put(QName.createQName(TEST_NAMESPACE, "boolean-ista"), new StringPropertyValue(
                DefaultTypeConverter.INSTANCE.convert(String.class, Boolean.valueOf(true))));
        properties04.put(QName.createQName(TEST_NAMESPACE, "qname-ista"), new StringPropertyValue(
                DefaultTypeConverter.INSTANCE.convert(String.class, QName.createQName("{wibble}wobble"))));
        properties04.put(
                QName.createQName(TEST_NAMESPACE, "category-ista"),
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, new NodeRef(
                        new StoreRef("proto", "id"), "CategoryId"))));
        properties04.put(QName.createQName(TEST_NAMESPACE, "noderef-ista"), new StringPropertyValue(
                DefaultTypeConverter.INSTANCE.convert(String.class, n01NodeRef)));
        properties04.put(QName.createQName(TEST_NAMESPACE, "path-ista"),
                new StringPropertyValue("/" + n03QName.toString()));
        properties04.put(QName.createQName(TEST_NAMESPACE, "locale-ista"), new StringPropertyValue(
                DefaultTypeConverter.INSTANCE.convert(String.class, Locale.UK)));
        properties04.put(QName.createQName(TEST_NAMESPACE, "period-ista"), new StringPropertyValue(
                DefaultTypeConverter.INSTANCE.convert(String.class, new Period("period|12"))));
        properties04.put(QName.createQName(TEST_NAMESPACE, "null"), null);
        MultiPropertyValue list_0 = new MultiPropertyValue();
        list_0.addValue(new StringPropertyValue("one"));
        list_0.addValue(new StringPropertyValue("two"));
        properties04.put(QName.createQName(TEST_NAMESPACE, "list"), list_0);
        MLTextPropertyValue mlText = new MLTextPropertyValue();
        mlText.addValue(Locale.ENGLISH, "banana");
        mlText.addValue(Locale.FRENCH, "banane");
        mlText.addValue(Locale.CHINESE, "香蕉");
        mlText.addValue(new Locale("nl"), "banaan");
        mlText.addValue(Locale.GERMAN, "banane");
        mlText.addValue(new Locale("el"), "μπανάνα");
        mlText.addValue(Locale.ITALIAN, "banana");
        mlText.addValue(new Locale("ja"), "バナナ");
        mlText.addValue(new Locale("ko"), "바나나");
        mlText.addValue(new Locale("pt"), "banana");
        mlText.addValue(new Locale("ru"), "банан");
        mlText.addValue(new Locale("es"), "plátano");
        properties04.put(QName.createQName(TEST_NAMESPACE, "ml"), mlText);
        MultiPropertyValue list_1 = new MultiPropertyValue();
        list_1.addValue(new StringPropertyValue("100"));
        list_1.addValue(new StringPropertyValue("anyValueAsString"));
        properties04.put(QName.createQName(TEST_NAMESPACE, "any-many-ista"), list_1);
        MultiPropertyValue list_2 = new MultiPropertyValue();
        list_2.addValue(new ContentPropertyValue(Locale.ENGLISH, 12L, "UTF-16", "text/plain", null));
        properties04.put(QName.createQName(TEST_NAMESPACE, "content-many-ista"), list_2);
        content04.put(QName.createQName(TEST_NAMESPACE, "content-many-ista"), "multicontent");

        MLTextPropertyValue mlText1 = new MLTextPropertyValue();
        mlText1.addValue(Locale.ENGLISH, "cabbage");
        mlText1.addValue(Locale.FRENCH, "chou");

        MLTextPropertyValue mlText2 = new MLTextPropertyValue();
        mlText2.addValue(Locale.ENGLISH, "lemur");
        mlText2.addValue(new Locale("ru"), "лемур");

        MultiPropertyValue list_3 = new MultiPropertyValue();
        list_3.addValue(mlText1);
        list_3.addValue(mlText2);

        properties04.put(QName.createQName(TEST_NAMESPACE, "mltext-many-ista"), list_3);

        MultiPropertyValue list_4 = new MultiPropertyValue();
        list_4.addValue(null);
        properties04.put(QName.createQName(TEST_NAMESPACE, "nullist"), list_4);

        n04NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n04QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "four");
        n04CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n04QName,
                n04NodeRef, true, 0);

        properties04.put(QName.createQName(TEST_NAMESPACE, "aspectProperty"), new StringPropertyValue(""));
        addNode(core, dataModel, 1, 5, 1, testType, new QName[]{testAspect}, properties04, content04, "dave",
                new ChildAssociationRef[]{n04CAR}, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + n04QName.toString()}, n04NodeRef, true);

        // 5

        n05NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n05QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "five");
        n05CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n01NodeRef, n05QName,
                n05NodeRef, true, 0);
        addNode(core, dataModel, 1, 6, 1, testSuperType, null, getOrderProperties(), null, "eoin",
                new ChildAssociationRef[]{n05CAR}, new NodeRef[]{rootNodeRef, n01NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString()}, n05NodeRef, true);

        // 6

        n06NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n06QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "six");
        n06CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n01NodeRef, n06QName,
                n06NodeRef, true, 0);
        addNode(core, dataModel, 1, 7, 1, testSuperType, null, getOrderProperties(), null, "fred",
                new ChildAssociationRef[]{n06CAR}, new NodeRef[]{rootNodeRef, n01NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n06QName.toString()}, n06NodeRef, true);

        // 7

        n07NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n07QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "seven");
        n07CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n02NodeRef, n07QName,
                n07NodeRef, true, 0);
        addNode(core, dataModel, 1, 8, 1, testSuperType, null, getOrderProperties(), null, "gail",
                new ChildAssociationRef[]{n07CAR}, new NodeRef[]{rootNodeRef, n02NodeRef},
                new String[]{"/" + n02QName.toString() + "/" + n07QName.toString()}, n07NodeRef, true);

        // 8

        n08NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n08QName_0 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "eight-0");
        n08QName_1 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "eight-1");
        n08QName_2 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "eight-2");
        n08CAR_0 = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef,
                n08QName_0, n08NodeRef, false, 2);
        n08CAR_1 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n01NodeRef, n08QName_1,
                n08NodeRef, false, 1);
        n08CAR_2 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n02NodeRef, n08QName_2,
                n08NodeRef, true, 0);

        addNode(core, dataModel, 1, 9, 1, testSuperType, null, getOrderProperties(), null, "hal",
                new ChildAssociationRef[]{n08CAR_0, n08CAR_1, n08CAR_2}, new NodeRef[]{rootNodeRef,
                        rootNodeRef, n01NodeRef, rootNodeRef, n02NodeRef}, new String[]{
                        "/" + n08QName_0, "/" + n01QName.toString() + "/" + n08QName_1.toString(),
                        "/" + n02QName.toString() + "/" + n08QName_2.toString()}, n08NodeRef, true);

        // 9

        n09NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n09QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "nine");
        n09CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef, n09QName,
                n09NodeRef, true, 0);
        addNode(core, dataModel, 1, 10, 1, testSuperType, null, getOrderProperties(), null, "ian",
                new ChildAssociationRef[]{n09CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n09QName},
                n09NodeRef, true);

        // 10

        n10NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n10QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "ten");
        n10CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef, n10QName,
                n10NodeRef, true, 0);
        addNode(core, dataModel, 1, 11, 1, testSuperType, null, getOrderProperties(), null, "jake",
                new ChildAssociationRef[]{n10CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n10QName},
                n10NodeRef, true);

        // 11

        n11NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n11QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "eleven");
        n11CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef, n11QName,
                n11NodeRef, true, 0);
        addNode(core, dataModel, 1, 12, 1, testSuperType, null, getOrderProperties(), null, "kara",
                new ChildAssociationRef[]{n11CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n11QName},
                n11NodeRef, true);

        // 12

        n12NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n12QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "twelve");
        n12CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef, n12QName,
                n12NodeRef, true, 0);
        addNode(core, dataModel, 1, 13, 1, testSuperType, null, getOrderProperties(), null, "loon",
                new ChildAssociationRef[]{n12CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName},
                n12NodeRef, true);

        // 13

        n13NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n13QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "thirteen");
        n13QNameLink = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "link");
        n13CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n12NodeRef, n13QName,
                n13NodeRef, true, 0);
        n13CARLink = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n02NodeRef, n13QName,
                n13NodeRef, false, 0);
        addNode(core, dataModel, 1, 14, 1, testSuperType, null, getOrderProperties(), null, "mike",
                new ChildAssociationRef[]{n13CAR, n13CARLink}, new NodeRef[]{rootNodeRef, n01NodeRef,
                        n05NodeRef, n12NodeRef, rootNodeRef, n02NodeRef},
                new String[]{
                        "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                + n13QName, "/" + n02QName.toString() + "/" + n13QNameLink},
                n13NodeRef, true);

        // 14

        properties14 = new HashMap<QName, PropertyValue>();
        properties14.putAll(getOrderProperties());
        content14 = new HashMap<QName, String>();
        MLTextPropertyValue desc1 = new MLTextPropertyValue();
        desc1.addValue(Locale.ENGLISH, "Alfresco tutorial");
        desc1.addValue(Locale.US, "Alfresco tutorial");

        Date explicitCreatedDate = new Date();
        try {
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        properties14.put(ContentModel.PROP_CONTENT,
                new ContentPropertyValue(Locale.UK, 298L, "UTF-8", "text/plain", null));
        content14.put(
                ContentModel.PROP_CONTENT,
                "The quick brown fox jumped over the lazy dog and ate the Alfresco Tutorial, in pdf format, along with the following stop words;  a an and are"
                        + " as at be but by for if in into is it no not of on or such that the their then there these they this to was will with: "
                        + " and random charcters \u00E0\u00EA\u00EE\u00F0\u00F1\u00F6\u00FB\u00FF");
        properties14.put(ContentModel.PROP_DESCRIPTION, desc1);
        properties14.put(
                ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE
                        .convert(String.class, explicitCreatedDate)));
        properties14.put(
                ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE
                        .convert(String.class, explicitCreatedDate)));
        MLTextPropertyValue title = new MLTextPropertyValue();
        title.addValue(Locale.ENGLISH, "English123");
        title.addValue(Locale.FRENCH, "French123");
        properties14.put(ContentModel.PROP_TITLE, title);
        n14NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n14QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "fourteen");
        n14QNameCommon = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "common");
        n14CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n13NodeRef, n14QName,
                n14NodeRef, true, 0);
        n14CAR_1 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n01NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_2 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n02NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_5 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_6 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n06NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_12 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n12NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_13 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n13NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        addNode(core, dataModel, 1, 15, 1, ContentModel.TYPE_CONTENT, new QName[]{ContentModel.ASPECT_TITLED}, properties14, content14, "noodle",
                new ChildAssociationRef[]{n14CAR, n14CAR_1, n14CAR_2, n14CAR_5, n14CAR_6, n14CAR_12,
                        n14CAR_13}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef, n12NodeRef,
                        n13NodeRef}, new String[]{
                        "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                + n13QName + "/" + n14QName,
                        "/" + n02QName.toString() + "/" + n13QNameLink + "/" + n14QName,
                        "/" + n01QName + "/" + n14QNameCommon,
                        "/" + n02QName + "/" + n14QNameCommon,
                        "/" + n01QName + "/" + n05QName + "/" + n14QNameCommon,
                        "/" + n01QName + "/" + n06QName + "/" + n14QNameCommon,
                        "/" + n01QName + "/" + n05QName + "/" + n12QName + "/" + n14QNameCommon,
                        "/" + n01QName + "/" + n05QName + "/" + n12QName + "/" + n13QName + "/"
                                + n14QNameCommon}, n14NodeRef, true);

        // 15

        properties15 = new HashMap<QName, PropertyValue>();
        properties15.putAll(getOrderProperties());
        properties15.put(
                ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE
                        .convert(String.class, explicitCreatedDate)));
        content15 = new HashMap<QName, String>();
        content15.put(ContentModel.PROP_CONTENT, "          ");
        n15NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n15QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "fifteen");
        n15CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n13NodeRef, n15QName,
                n15NodeRef, true, 0);
        addNode(core, dataModel, 1, 16, 1, ContentModel.TYPE_THUMBNAIL, null, properties15, content15, "ood",
                new ChildAssociationRef[]{n15CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef,
                        n12NodeRef, n13NodeRef},
                new String[]{
                        "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                + n13QName + "/" + n15QName,
                        "/" + n02QName.toString() + "/" + n13QNameLink + "/" + n14QName}, n15NodeRef, true);

    }

    private static  Map<QName, PropertyValue> getOrderProperties()
    {
        double orderDoubleCount = -0.11d + orderTextCount * ((orderTextCount % 2 == 0) ? 0.1d : -0.1d);
        float orderFloatCount = -3.5556f + orderTextCount * ((orderTextCount % 2 == 0) ? 0.82f : -0.82f);
        long orderLongCount = -1999999999999999l + orderTextCount
                * ((orderTextCount % 2 == 0) ? 299999999999999l : -299999999999999l);
        int orderIntCount = -45764576 + orderTextCount * ((orderTextCount % 2 == 0) ? 8576457 : -8576457);

        Map<QName, PropertyValue> testProperties = new HashMap<QName, PropertyValue>();
        testProperties.put(createdDate,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, orderDate)));
        testProperties.put(createdTime,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, orderDate)));
        testProperties.put(orderDouble,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, orderDoubleCount)));
        testProperties.put(orderFloat,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, orderFloatCount)));
        testProperties.put(orderLong,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, orderLongCount)));
        testProperties.put(orderInt,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, orderIntCount)));
        testProperties.put(
                orderText,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, new String(
                        new char[] { (char) ('l' + ((orderTextCount % 2 == 0) ? orderTextCount
                                : -orderTextCount)) })
                        + " cabbage")));

        testProperties.put(ContentModel.PROP_NAME, new StringPropertyValue(orderNames[orderTextCount]));
        testProperties.put(orderLocalisedText, new StringPropertyValue(orderLocalisedNames[orderTextCount]));

        MLTextPropertyValue mlTextPropLocalisedOrder = new MLTextPropertyValue();
        if (orderLocaliseMLText_en[orderTextCount].length() > 0)
        {
            mlTextPropLocalisedOrder.addValue(Locale.ENGLISH, orderLocaliseMLText_en[orderTextCount]);
        }
        if (orderLocaliseMLText_fr[orderTextCount].length() > 0)
        {
            mlTextPropLocalisedOrder.addValue(Locale.FRENCH, orderLocaliseMLText_fr[orderTextCount]);
        }
        if (orderLocaliseMLText_es[orderTextCount].length() > 0)
        {
            mlTextPropLocalisedOrder.addValue(new Locale("es"), orderLocaliseMLText_es[orderTextCount]);
        }
        if (orderLocaliseMLText_de[orderTextCount].length() > 0)
        {
            mlTextPropLocalisedOrder.addValue(Locale.GERMAN, orderLocaliseMLText_de[orderTextCount]);
        }
        testProperties.put(orderLocalisedMLText, mlTextPropLocalisedOrder);

        MLTextPropertyValue mlTextPropVal = new MLTextPropertyValue();
        mlTextPropVal.addValue(Locale.ENGLISH, new String(
                new char[]{(char) ('l' + ((orderTextCount % 2 == 0) ? orderTextCount : -orderTextCount))})
                + " banana");
        mlTextPropVal.addValue(Locale.FRENCH, new String(
                new char[]{(char) ('L' + ((orderTextCount % 2 == 0) ? -orderTextCount : orderTextCount))})
                + " banane");
        mlTextPropVal.addValue(Locale.CHINESE, new String(
                new char[]{(char) ('香' + ((orderTextCount % 2 == 0) ? orderTextCount : -orderTextCount))})
                + " 香蕉");
        testProperties.put(orderMLText, mlTextPropVal);

        orderDate = Duration.subtract(orderDate, new Duration("P1D"));
        orderTextCount++;
        return testProperties;
    }

    public static void loadSecondDataSet() throws Exception {

        SolrCore core = h.getCore();
        for (int i = 0; i < 100; i++)
        {
            orderDate = new Date();
            orderTextCount = 0;
            addNode(core, dataModel, 1, 2, 1, testSuperType, null, getOrderProperties(), null, "andy",
                    new ChildAssociationRef[] { n01CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                            + n01QName.toString() }, n01NodeRef, true);
        }


        for (int i = 0; i < 10; i++)
        {
            orderDate = new Date();
            orderTextCount = 0;

            addNode(core, dataModel, 1, 2, 1, testSuperType, null, getOrderProperties(), null, "andy",
                    new ChildAssociationRef[] { n01CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                            + n01QName.toString() }, n01NodeRef, true);
            addNode(core, dataModel, 1, 3, 1, testSuperType, null, getOrderProperties(), null, "bob",
                    new ChildAssociationRef[] { n02CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                            + n02QName.toString() }, n02NodeRef, true);
            addNode(core, dataModel, 1, 4, 1, testSuperType, null, getOrderProperties(), null, "cid",
                    new ChildAssociationRef[] { n03CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                            + n03QName.toString() }, n03NodeRef, true);
            properties04.putAll(getOrderProperties());
            addNode(core, dataModel, 1, 5, 1, testType, new QName[] { testAspect }, properties04, content04,
                    "dave", new ChildAssociationRef[] { n04CAR }, new NodeRef[] { rootNodeRef },
                    new String[] { "/" + n04QName.toString() }, n04NodeRef, true);
            addNode(core, dataModel, 1, 6, 1, testSuperType, null, getOrderProperties(), null, "eoin",
                    new ChildAssociationRef[] { n05CAR }, new NodeRef[] { rootNodeRef, n01NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() }, n05NodeRef, true);
            addNode(core, dataModel, 1, 7, 1, testSuperType, null, getOrderProperties(), null, "fred",
                    new ChildAssociationRef[] { n06CAR }, new NodeRef[] { rootNodeRef, n01NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n06QName.toString() }, n06NodeRef, true);
            addNode(core, dataModel, 1, 8, 1, testSuperType, null, getOrderProperties(), null, "gail",
                    new ChildAssociationRef[] { n07CAR }, new NodeRef[] { rootNodeRef, n02NodeRef },
                    new String[] { "/" + n02QName.toString() + "/" + n07QName.toString() }, n07NodeRef, true);
            addNode(core, dataModel, 1, 9, 1, testSuperType, null, getOrderProperties(), null, "hal",
                    new ChildAssociationRef[] { n08CAR_0, n08CAR_1, n08CAR_2 }, new NodeRef[] { rootNodeRef,
                            rootNodeRef, n01NodeRef, rootNodeRef, n02NodeRef }, new String[] {
                            "/" + n08QName_0, "/" + n01QName.toString() + "/" + n08QName_1.toString(),
                            "/" + n02QName.toString() + "/" + n08QName_2.toString() }, n08NodeRef, true);
            addNode(core, dataModel, 1, 10, 1, testSuperType, null, getOrderProperties(), null, "ian",
                    new ChildAssociationRef[] { n09CAR },
                    new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n09QName },
                    n09NodeRef, true);
            addNode(core, dataModel, 1, 11, 1, testSuperType, null, getOrderProperties(), null, "jake",
                    new ChildAssociationRef[] { n10CAR },
                    new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n10QName },
                    n10NodeRef, true);
            addNode(core, dataModel, 1, 12, 1, testSuperType, null, getOrderProperties(), null, "kara",
                    new ChildAssociationRef[] { n11CAR },
                    new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n11QName },
                    n11NodeRef, true);
            addNode(core, dataModel, 1, 13, 1, testSuperType, null, getOrderProperties(), null, "loon",
                    new ChildAssociationRef[] { n12CAR },
                    new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName },
                    n12NodeRef, true);
            addNode(core, dataModel, 1, 14, 1, testSuperType, null, getOrderProperties(), null, "mike",
                    new ChildAssociationRef[] { n13CAR, n13CARLink }, new NodeRef[] { rootNodeRef, n01NodeRef,
                            n05NodeRef, n12NodeRef, rootNodeRef, n02NodeRef }, new String[] {
                            "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                    + n13QName, "/" + n02QName.toString() + "/" + n13QNameLink },
                    n13NodeRef, true);
            properties14.putAll(getOrderProperties());
            addNode(core, dataModel, 1, 15, 1, ContentModel.TYPE_CONTENT, null, properties14, content14, "noodle",
                    new ChildAssociationRef[] { n14CAR, n14CAR_1, n14CAR_2, n14CAR_5, n14CAR_6, n14CAR_12,
                            n14CAR_13 }, new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef, n12NodeRef,
                            n13NodeRef },
                    new String[] {
                            "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                    + n13QName + "/" + n14QName,
                            "/" + n02QName.toString() + "/" + n13QNameLink + "/" + n14QName,
                            "/" + n01QName + "/" + n14QNameCommon,
                            "/" + n02QName + "/" + n14QNameCommon,
                            "/" + n01QName + "/" + n05QName + "/" + n14QNameCommon,
                            "/" + n01QName + "/" + n06QName + "/" + n14QNameCommon,
                            "/" + n01QName + "/" + n05QName + "/" + n12QName + "/" + n14QNameCommon,
                            "/" + n01QName + "/" + n05QName + "/" + n12QName + "/" + n13QName + "/"
                                    + n14QNameCommon }, n14NodeRef, true);
            properties14.putAll(getOrderProperties());
            addNode(core, dataModel, 1, 16, 1, ContentModel.TYPE_THUMBNAIL, null, properties15, content15, "ood",
                    new ChildAssociationRef[] { n15CAR }, new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef,
                            n12NodeRef, n13NodeRef }, new String[] {
                            "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                    + n13QName + "/" + n15QName,
                            "/" + n02QName.toString() + "/" + n13QNameLink + "/" + n14QName }, n15NodeRef,
                    true);
        }

    }

    public static void loadEscapingTestData() throws Exception {
        SolrCore core = h.getCore();

        NodeRef childNameEscapingNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName childNameEscapingQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, COMPLEX_LOCAL_NAME);
        QName pathChildNameEscapingQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
                ISO9075.encode(COMPLEX_LOCAL_NAME));
        ChildAssociationRef complexCAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef,
                childNameEscapingQName, childNameEscapingNodeRef, true, 0);
        addNode(core, dataModel, 1, 17, 1, testSuperType, null, null, null, "system",
                new ChildAssociationRef[] { complexCAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + pathChildNameEscapingQName.toString() }, childNameEscapingNodeRef, true);

        NodeRef numericNameEscapingNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName numericNameEscapingQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, NUMERIC_LOCAL_NAME);
        QName pathNumericNameEscapingQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI,
                ISO9075.encode(NUMERIC_LOCAL_NAME));
        ChildAssociationRef numericCAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef,
                numericNameEscapingQName, numericNameEscapingNodeRef, true, 0);
        addNode(core, dataModel, 1, 18, 1, testSuperType, null, null, null, "system",
                new ChildAssociationRef[] { numericCAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + pathNumericNameEscapingQName.toString() }, numericNameEscapingNodeRef, true);
    }
    
    public static void loadMntTestData() throws Exception {
        SolrCore core = h.getCore();

        properties19= new HashMap<QName, PropertyValue>();
        properties19.put(
                ContentModel.PROP_NAME,
                new StringPropertyValue("Test.hello.txt"));
        n19NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n19QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "nineteen");
        n19CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, rootNodeRef, n19QName,
        		n19NodeRef, true, 0);
        addNode(core, dataModel, 1, 19, 1,  ContentModel.TYPE_CONTENT, null, properties19, null, "system",
                new ChildAssociationRef[] { n19CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n19QName.toString() }, n19NodeRef, true);
        
        
        properties20 = new HashMap<QName, PropertyValue>();
        properties20.put(
                ContentModel.PROP_NAME,
                new StringPropertyValue("Test1.hello.txt"));
        n20NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n20QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "twenty");
        n20CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, rootNodeRef, n20QName,
        		n20NodeRef, true, 0);
        addNode(core, dataModel, 1, 20, 1,  ContentModel.TYPE_CONTENT, null, properties20, null, "system",
                new ChildAssociationRef[] { n20CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n20QName.toString() }, n20NodeRef, true);
        
        
        properties21 = new HashMap<QName, PropertyValue>();
        properties21.put(
                ContentModel.PROP_NAME,
                new StringPropertyValue("one_two_three.txt"));
        n21NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n21QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "twentyone");
        n21CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, rootNodeRef, n21QName,
        		n21NodeRef, true, 0);
        addNode(core, dataModel, 1, 21, 1,  ContentModel.TYPE_CONTENT, null, properties21, null, "system",
                new ChildAssociationRef[] { n21CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n21QName.toString() }, n21NodeRef, true);
        
        properties22 = new HashMap<QName, PropertyValue>();
        properties22.put(
                ContentModel.PROP_NAME,
                new StringPropertyValue("one_two_four.txt"));
        n22NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n22QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "twnetytwo");
        n22CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, rootNodeRef, n22QName,
        		n22NodeRef, true, 0);
        addNode(core, dataModel, 1, 22, 1,  ContentModel.TYPE_CONTENT, null, properties22, null, "system",
                new ChildAssociationRef[] { n22CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n22QName.toString() }, n22NodeRef, true);
        
        properties23 = new HashMap<QName, PropertyValue>();
        properties23.put(
                ContentModel.PROP_NAME,
                new StringPropertyValue("one_two.txt"));
        n23NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n23QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "twentythree");
        n23CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, rootNodeRef, n23QName,
        		n23NodeRef, true, 0);
        addNode(core, dataModel, 1, 23, 1,  ContentModel.TYPE_CONTENT, null, properties23, null, "system",
                new ChildAssociationRef[] { n23CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n23QName.toString() }, n23NodeRef, true);
        
        properties24 = new HashMap<QName, PropertyValue>();
        properties24.put(
                ContentModel.PROP_NAME,
                new StringPropertyValue("Print-Toolkit-3204-The-Print-Toolkit-has-a-new-look-565022.html"));
        n24NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n24QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "twentyfour");
        n24CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, rootNodeRef, n24QName,
        		n24NodeRef, true, 0);
        addNode(core, dataModel, 1, 24, 1,  ContentModel.TYPE_CONTENT, null, properties24, null, "system",
                new ChildAssociationRef[] { n24CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n24QName.toString() }, n24NodeRef, true);
        
        
        properties25 = new HashMap<QName, PropertyValue>();
        properties25.put(
                ContentModel.PROP_NAME,
                new StringPropertyValue("Print-Toolkitf-3204-The-Print-Toolkit-has-a-new-look-565022.html"));
        n25NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n25QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "twentyfive");
        n25CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, rootNodeRef, n25QName,
        		n25NodeRef, true, 0);
        addNode(core, dataModel, 1, 25, 1,  ContentModel.TYPE_CONTENT, null, properties25, null, "system",
                new ChildAssociationRef[] { n25CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n25QName.toString() }, n25NodeRef, true);
        
        properties26 = new HashMap<QName, PropertyValue>();
        properties26.put(
                ContentModel.PROP_NAME,
                new StringPropertyValue("apple pear peach 20150911100000.txt"));
        n26NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        n26QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "twentysix");
        n26CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, rootNodeRef, n26QName,
        		n26NodeRef, true, 0);
        addNode(core, dataModel, 1, 26, 1,  ContentModel.TYPE_CONTENT, null, properties26, null, "system",
                new ChildAssociationRef[] { n26CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n26QName.toString() }, n26NodeRef, true);


    }



    private static int orderTextCount = 0;
    private static Date orderDate = new Date();
    private static String[] orderNames = new String[] { "one", "two", "three", "four", "five", "six", "seven", "eight",
            "nine", "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen" };

    // Spanish- Eng, French-English, Swedish German, English
    private static String[] orderLocalisedNames = new String[] { "chalina", "curioso", "llama", "luz", "peach", "péché",
            "pêche", "sin", "\u00e4pple", "banan", "p\u00e4ron", "orange", "rock", "rôle", "rose", "filler" };

    private static String[] orderLocaliseMLText_de = new String[] { "Arg", "Ärgerlich", "Arm", "Assistent", "Aßlar",
            "Assoziation", "Udet", "Übelacker", "Uell", "Ülle", "Ueve", "Üxküll", "Uffenbach", "apple", "and",
            "aardvark" };

    private static String[] orderLocaliseMLText_fr = new String[] { "cote", "côte", "coté", "côté", "rock", "lemur",
            "lemonade", "lemon", "kale", "guava", "cheese", "beans", "bananana", "apple", "and", "aardvark" };

    private static String[] orderLocaliseMLText_en = new String[] { "zebra", "tiger", "rose", "rôle", "rock", "lemur",
            "lemonade", "lemon", "kale", "guava", "cheese", "beans", "bananana", "apple", "and", "aardvark" };

    private static String[] orderLocaliseMLText_es = new String[] { "radio", "ráfaga", "rana", "rápido", "rastrillo", "arroz",
            "campo", "chihuahua", "ciudad", "limonada", "llaves", "luna", "bananana", "apple", "and", "aardvark" };
}