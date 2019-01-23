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
package org.alfresco.solr.query.afts;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.alfresco.model.ContentModel.ASSOC_CONTAINS;
import static org.alfresco.model.ContentModel.ASSOC_CHILDREN;
import static org.alfresco.model.ContentModel.PROP_NAME;
import static org.alfresco.model.ContentModel.TYPE_CONTENT;
import static org.alfresco.service.namespace.NamespaceService.CONTENT_MODEL_1_0_URI;
import static org.alfresco.solr.AlfrescoSolrUtils.addNode;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;
import static org.alfresco.solr.AlfrescoSolrUtils.addStoreRoot;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.Period;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.cmr.repository.datatype.Duration;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AlfrescoSolrConstants;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.ContentPropertyValue;
import org.alfresco.solr.client.MLTextPropertyValue;
import org.alfresco.solr.client.MultiPropertyValue;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.alfresco.util.ISO9075;
import org.apache.solr.core.SolrCore;
import org.apache.solr.util.TestHarness;

import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Test datasets provider.
 * The data used in a test is usually managed within the test itself.
 * However, sometimes a dataset is used by more than one test, and in this case we centralized its management in this class.
 * The class also offers some abstraction for creating things like nodes, qname.
 *
 * @author Michael Suzuki
 * @author Andrea Gazzarini
 */
public class TestDataProvider implements AlfrescoSolrConstants
{
    private final String complexLocalName = "\u0020\u0060\u00ac\u00a6\u0021\"\u00a3\u0024\u0025\u005e\u0026\u002a\u0028\u0029\u002d\u005f\u003d\u002b\t\n\\\u0000\u005b\u005d\u007b\u007d\u003b\u0027\u0023\u003a\u0040\u007e\u002c\u002e\u002f\u003c\u003e\u003f\\u007c\u005f\u0078\u0054\u0036\u0035\u0041\u005f";
    private final String numericLocalName = "12Woof12";

    private static String[] ORDER_NAMES =
    {
        "one", "two", "three", "four", "five", "six", "seven", "eight", "nine",
        "ten", "eleven", "twelve", "thirteen", "fourteen", "fifteen", "sixteen"
    };

    // Spanish- Eng, French-English, Swedish German, English
    private static String[] ORDER_LOCALISED_NAMES =
    {
        "chalina", "curioso", "llama", "luz", "peach", "péché",
        "pêche", "sin", "\u00e4pple", "banan", "p\u00e4ron", "orange",
        "rock", "rôle", "rose", "filler"
    };

    private static String[] ORDER_LOCALISED_MLTEXT_DE =
    {
        "Arg", "Ärgerlich", "Arm", "Assistent", "Aßlar", "Assoziation", "Udet", "Übelacker", "Uell", "Ülle",
        "Ueve", "Üxküll", "Uffenbach", "apple", "and", "aardvark"
    };

    private static String[] ORDER_LOCALISED_MLTEXT_FR =
    {
        "cote", "côte", "coté", "côté", "rock", "lemur", "lemonade", "lemon", "kale", "guava",
        "cheese", "beans", "bananana", "apple", "and", "aardvark"
    };

    private static String[] ORDER_LOCALISED_MLTEXT_EN =
    {
        "zebra", "tiger", "rose", "rôle", "rock", "lemur", "lemonade", "lemon", "kale", "guava",
        "cheese", "beans", "bananana", "apple", "and", "aardvark"
    };

    private static String[] ORDER_LOCALISED_MLTEXT_ES =
    {
        "radio", "ráfaga", "rana", "rápido", "rastrillo", "arroz", "campo", "chihuahua", "ciudad",
        "limonada", "llaves", "luna", "bananana", "apple", "and", "aardvark"
    };

    private int orderTextCount;
    private Date orderDate = new Date();

    private ChildAssociationRef n01CAR;
    private ChildAssociationRef n02CAR;
    private ChildAssociationRef n03CAR;
    private ChildAssociationRef n04CAR;
    private ChildAssociationRef n05CAR;
    private ChildAssociationRef n06CAR;
    private ChildAssociationRef n07CAR;
    private ChildAssociationRef n08CAR_0;
    private ChildAssociationRef n08CAR_1;
    private ChildAssociationRef n08CAR_2;
    private ChildAssociationRef n09CAR;
    private ChildAssociationRef n10CAR;
    private ChildAssociationRef n11CAR;
    private ChildAssociationRef n12CAR;
    private ChildAssociationRef n13CAR;
    private ChildAssociationRef n13CARLink;
    private ChildAssociationRef n14CAR;
    private ChildAssociationRef n14CAR_1;
    private ChildAssociationRef n14CAR_2;
    private ChildAssociationRef n14CAR_5;
    private ChildAssociationRef n14CAR_6;
    private ChildAssociationRef n14CAR_12;
    private ChildAssociationRef n14CAR_13;
    private ChildAssociationRef n15CAR;

    private NodeRef n01NodeRef;
    private NodeRef n02NodeRef;
    private NodeRef n03NodeRef;
    private NodeRef n04NodeRef;
    private NodeRef n05NodeRef;
    private NodeRef n06NodeRef;
    private NodeRef n07NodeRef;
    private NodeRef n08NodeRef;
    private NodeRef n09NodeRef;
    private NodeRef n10NodeRef;
    private NodeRef n11NodeRef;
    private NodeRef n12NodeRef;
    private NodeRef n13NodeRef;
    private NodeRef n14NodeRef;
    private NodeRef n15NodeRef;

    private NodeRef rootNodeRef;
    private NodeRef testNodeRef;
    private Date ftsTestDate = new Date(System.currentTimeMillis() - 10000);

    private QName n01QName;
    private QName n02QName;
    private QName n03QName;
    private QName n04QName;
    private QName n05QName;
    private QName n06QName;
    private QName n07QName;
    private QName n08QName_0;
    private QName n08QName_1;
    private QName n08QName_2;
    private QName n09QName;
    private QName n10QName;
    private QName n11QName;
    private QName n12QName;
    private QName n13QName;
    private QName n13QNameLink;
    private QName n14QName;
    private QName n14QNameCommon;
    private QName n15QName;

    private Map<QName, PropertyValue> properties04;
    private Map<QName, String> content04;
    private Map<QName, PropertyValue> properties14;
    private Map<QName, String> content14;
    private Map<QName, PropertyValue> properties15;
    private Map<QName, String> content15;

    private final SolrCore core;
    private final AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();

    public TestDataProvider(final TestHarness testHarness) throws Exception
    {
        this.core = testHarness.getCore();
        dataModel.getNamespaceDAO().removePrefix("");
        dataModel.setCMDefaultUri();

        rootNodeRef = newNodeRef();
        addStoreRoot(core, dataModel, rootNodeRef, 1, 1, 1, 1);
    }

    public NodeRef getTestNodeRef()
    {
        return testNodeRef;
    }

    public NodeRef getNode01()
    {
        return n01NodeRef;
    }

    public NodeRef getNode02()
    {
        return n02NodeRef;
    }

    public NodeRef getNode03()
    {
        return n03NodeRef;
    }

    public NodeRef getRootNode()
    {
        return rootNodeRef;
    }

    public Date getFtsTestDate()
    {
        return ftsTestDate;
    }

    public String getComplexLocalName()
    {
        return complexLocalName;
    }

    public String getNumericLocalName()
    {
        return numericLocalName;
    }

    /**
     * Loads in Solr a small dataset composed by 15 nodes.
     */
    public void loadSmallDataset() throws Exception
    {
        // 1

        n01NodeRef = newNodeRef();
        n01QName = QName.createQName(CONTENT_MODEL_1_0_URI, "one");
        n01CAR = new ChildAssociationRef(ASSOC_CHILDREN, rootNodeRef, n01QName, n01NodeRef, true, 0);
        addNode(
                core,
                dataModel,
                1,
                2,
                1,
                TEST_SUPER_TYPE,
                null,
                getOrderProperties(),
                null,
                "andy",
                new ChildAssociationRef[]{n01CAR},
                new NodeRef[]{rootNodeRef},
                new String[]{"/" + n01QName.toString()}, n01NodeRef, true);

        testNodeRef = n01NodeRef;

        // 2

        n02NodeRef = newNodeRef();
        n02QName = QName.createQName(CONTENT_MODEL_1_0_URI, "two");
        n02CAR = new ChildAssociationRef(ASSOC_CHILDREN, rootNodeRef, n02QName, n02NodeRef, true, 0);
        addNode(core, dataModel, 1, 3, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "bob",
                new ChildAssociationRef[]{n02CAR}, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + n02QName.toString()}, n02NodeRef, true);



        // 3

        n03NodeRef = newNodeRef();
        n03QName = QName.createQName(CONTENT_MODEL_1_0_URI, "three");
        n03CAR = new ChildAssociationRef(ASSOC_CHILDREN, rootNodeRef, n03QName,
                n03NodeRef, true, 0);
        addNode(core, dataModel, 1, 4, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "cid",
                new ChildAssociationRef[]{n03CAR}, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + n03QName.toString()}, n03NodeRef, true);

        // Node #4

        content04 = new HashMap<>();
        properties04 = new HashMap<>(getOrderProperties());
        properties04.put(
                qName("text-indexed-stored-tokenised-atomic"),
                value("TEXT THAT IS INDEXED STORED AND TOKENISED ATOMICALLY KEYONE"));

        properties04.put(
                qName("text-indexed-unstored-tokenised-atomic"),
                value("TEXT THAT IS INDEXED STORED AND TOKENISED ATOMICALLY KEYUNSTORED"));

        properties04.put(
                qName("text-indexed-stored-tokenised-nonatomic"),
                value("TEXT THAT IS INDEXED STORED AND TOKENISED BUT NOT ATOMICALLY KEYTWO"));

        properties04.put(qName("int-ista"), value("1"));
        properties04.put(qName("long-ista"), value("2"));
        properties04.put(qName("float-ista"), value("3.4"));
        properties04.put(qName("double-ista"), value("5.6"));
        properties04.put(qName("date-ista"), value(ftsTestDate));
        properties04.put(qName("datetime-ista"), value(ftsTestDate));
        properties04.put(qName("boolean-ista"), value(Boolean.TRUE));
        properties04.put(qName("qname-ista"), value(QName.createQName("{wibble}wobble")));
        properties04.put(qName("category-ista"), value(new NodeRef(new StoreRef("proto", "id"), "CategoryId")));
        properties04.put(qName("noderef-ista"), value(n01NodeRef));
        properties04.put(qName("path-ista"), value("/" + n03QName.toString()));
        properties04.put(qName("locale-ista"), value(Locale.UK));
        properties04.put(qName("period-ista"), value(new Period("period|12")));
        properties04.put(qName("null"), null);

        properties04.put(qName("list"), new MultiPropertyValue(asList(value("one"), value("two"))));

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
        properties04.put(qName("ml"), mlText);

        properties04.put(qName("any-many-ista"), new MultiPropertyValue(asList(value("100"), value("anyValueAsString"))));

        MultiPropertyValue list_2 =
                new MultiPropertyValue(
                        singletonList(
                                new ContentPropertyValue(Locale.ENGLISH, 12L, "UTF-16", "text/plain", null)));

        properties04.put(qName("content-many-ista"), list_2);

        content04.put(qName("content-many-ista"), "multicontent");

        MLTextPropertyValue mlText1 = new MLTextPropertyValue();
        mlText1.addValue(Locale.ENGLISH, "cabbage");
        mlText1.addValue(Locale.FRENCH, "chou");

        MLTextPropertyValue mlText2 = new MLTextPropertyValue();
        mlText2.addValue(Locale.ENGLISH, "lemur");
        mlText2.addValue(new Locale("ru"), "лемур");

        properties04.put(qName("mltext-many-ista"), new MultiPropertyValue(asList(mlText1, mlText2)));
        properties04.put(qName("nullist"), new MultiPropertyValue(singletonList(value(null))));

        n04NodeRef = newNodeRef();
        n04QName = qName(CONTENT_MODEL_1_0_URI, "four");
        n04CAR = new ChildAssociationRef(ASSOC_CHILDREN, rootNodeRef, n04QName, n04NodeRef, true, 0);

        properties04.put(qName("aspectProperty"), value(("")));
        addNode(core, dataModel, 1, 5, 1, TEST_TYPE, new QName[]{TEST_ASPECT}, properties04, content04, "dave",
                new ChildAssociationRef[]{n04CAR}, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + n04QName.toString()}, n04NodeRef, true);

        // 5

        n05NodeRef = newNodeRef();
        n05QName = QName.createQName(CONTENT_MODEL_1_0_URI, "five");
        n05CAR = new ChildAssociationRef(ASSOC_CONTAINS, n01NodeRef, n05QName,
                n05NodeRef, true, 0);
        addNode(core, dataModel, 1, 6, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "eoin",
                new ChildAssociationRef[]{n05CAR}, new NodeRef[]{rootNodeRef, n01NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString()}, n05NodeRef, true);

        // 6

        n06NodeRef = newNodeRef();
        n06QName = QName.createQName(CONTENT_MODEL_1_0_URI, "six");
        n06CAR = new ChildAssociationRef(ASSOC_CONTAINS, n01NodeRef, n06QName,
                n06NodeRef, true, 0);
        addNode(core, dataModel, 1, 7, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "fred",
                new ChildAssociationRef[]{n06CAR}, new NodeRef[]{rootNodeRef, n01NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n06QName.toString()}, n06NodeRef, true);

        // 7

        n07NodeRef = newNodeRef();
        n07QName = QName.createQName(CONTENT_MODEL_1_0_URI, "seven");
        n07CAR = new ChildAssociationRef(ASSOC_CONTAINS, n02NodeRef, n07QName,
                n07NodeRef, true, 0);
        addNode(core, dataModel, 1, 8, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "gail",
                new ChildAssociationRef[]{n07CAR}, new NodeRef[]{rootNodeRef, n02NodeRef},
                new String[]{"/" + n02QName.toString() + "/" + n07QName.toString()}, n07NodeRef, true);

        // 8

        n08NodeRef = newNodeRef();
        n08QName_0 = qName(CONTENT_MODEL_1_0_URI, "eight-0");
        n08QName_1 = qName(CONTENT_MODEL_1_0_URI, "eight-1");
        n08QName_2 = qName(CONTENT_MODEL_1_0_URI, "eight-2");
        n08CAR_0 = new ChildAssociationRef(ASSOC_CHILDREN, rootNodeRef, n08QName_0, n08NodeRef, false, 2);
        n08CAR_1 = new ChildAssociationRef(ASSOC_CONTAINS, n01NodeRef, n08QName_1, n08NodeRef, false, 1);
        n08CAR_2 = new ChildAssociationRef(ASSOC_CONTAINS, n02NodeRef, n08QName_2, n08NodeRef, true, 0);

        addNode(core, dataModel, 1, 9, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "hal",
                new ChildAssociationRef[]{n08CAR_0, n08CAR_1, n08CAR_2}, new NodeRef[]{rootNodeRef,
                        rootNodeRef, n01NodeRef, rootNodeRef, n02NodeRef}, new String[]{
                        "/" + n08QName_0, "/" + n01QName.toString() + "/" + n08QName_1.toString(),
                        "/" + n02QName.toString() + "/" + n08QName_2.toString()}, n08NodeRef, true);

        // 9

        n09NodeRef = newNodeRef();
        n09QName = QName.createQName(CONTENT_MODEL_1_0_URI, "nine");
        n09CAR = new ChildAssociationRef(ASSOC_CONTAINS, n05NodeRef, n09QName,
                n09NodeRef, true, 0);
        addNode(core, dataModel, 1, 10, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "ian",
                new ChildAssociationRef[]{n09CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n09QName},
                n09NodeRef, true);

        // 10

        n10NodeRef = newNodeRef();
        n10QName = QName.createQName(CONTENT_MODEL_1_0_URI, "ten");
        n10CAR = new ChildAssociationRef(ASSOC_CONTAINS, n05NodeRef, n10QName,
                n10NodeRef, true, 0);
        addNode(core, dataModel, 1, 11, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "jake",
                new ChildAssociationRef[]{n10CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n10QName},
                n10NodeRef, true);

        // 11

        n11NodeRef = newNodeRef();
        n11QName = QName.createQName(CONTENT_MODEL_1_0_URI, "eleven");
        n11CAR = new ChildAssociationRef(ASSOC_CONTAINS, n05NodeRef, n11QName,
                n11NodeRef, true, 0);
        addNode(core, dataModel, 1, 12, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "kara",
                new ChildAssociationRef[]{n11CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n11QName},
                n11NodeRef, true);

        // 12

        n12NodeRef = newNodeRef();
        n12QName = QName.createQName(CONTENT_MODEL_1_0_URI, "twelve");
        n12CAR = new ChildAssociationRef(ASSOC_CONTAINS, n05NodeRef, n12QName,
                n12NodeRef, true, 0);
        addNode(core, dataModel, 1, 13, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "loon",
                new ChildAssociationRef[]{n12CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef},
                new String[]{"/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName},
                n12NodeRef, true);

        // 13

        n13NodeRef = newNodeRef();
        n13QName = qName(CONTENT_MODEL_1_0_URI, "thirteen");
        n13QNameLink = qName(CONTENT_MODEL_1_0_URI, "link");
        n13CAR = new ChildAssociationRef(ASSOC_CONTAINS, n12NodeRef, n13QName, n13NodeRef, true, 0);
        n13CARLink = new ChildAssociationRef(ASSOC_CONTAINS, n02NodeRef, n13QName, n13NodeRef, false, 0);
        addNode(core, dataModel, 1, 14, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "mike",
                new ChildAssociationRef[]{n13CAR, n13CARLink}, new NodeRef[]{rootNodeRef, n01NodeRef,
                        n05NodeRef, n12NodeRef, rootNodeRef, n02NodeRef},
                new String[]{
                        "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                + n13QName, "/" + n02QName.toString() + "/" + n13QNameLink},
                n13NodeRef, true);

        // 14

        properties14 = new HashMap<>();
        properties14.putAll(getOrderProperties());
        content14 = new HashMap<>();

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
        properties14.put(ContentModel.PROP_CREATED, value(explicitCreatedDate));
        properties14.put(ContentModel.PROP_MODIFIED, value(explicitCreatedDate));

        MLTextPropertyValue title = new MLTextPropertyValue();
        title.addValue(Locale.ENGLISH, "English123");
        title.addValue(Locale.FRENCH, "French123");

        properties14.put(ContentModel.PROP_TITLE, title);

        n14NodeRef = newNodeRef();
        n14QName = qName(CONTENT_MODEL_1_0_URI, "fourteen");
        n14QNameCommon = qName(CONTENT_MODEL_1_0_URI, "common");
        n14CAR = new ChildAssociationRef(ASSOC_CONTAINS, n13NodeRef, n14QName,
                n14NodeRef, true, 0);
        n14CAR_1 = new ChildAssociationRef(ASSOC_CONTAINS, n01NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_2 = new ChildAssociationRef(ASSOC_CONTAINS, n02NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_5 = new ChildAssociationRef(ASSOC_CONTAINS, n05NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_6 = new ChildAssociationRef(ASSOC_CONTAINS, n06NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_12 = new ChildAssociationRef(ASSOC_CONTAINS, n12NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        n14CAR_13 = new ChildAssociationRef(ASSOC_CONTAINS, n13NodeRef,
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

        properties15 = new HashMap<>();
        properties15.putAll(getOrderProperties());
        properties15.put(ContentModel.PROP_MODIFIED, value(explicitCreatedDate));
        content15 = new HashMap<>();
        content15.put(ContentModel.PROP_CONTENT, "          ");
        n15NodeRef = newNodeRef();
        n15QName = qName(CONTENT_MODEL_1_0_URI, "fifteen");
        n15CAR = new ChildAssociationRef(ASSOC_CONTAINS, n13NodeRef, n15QName,
                n15NodeRef, true, 0);
        addNode(core, dataModel, 1, 16, 1, ContentModel.TYPE_THUMBNAIL, null, properties15, content15, "ood",
                new ChildAssociationRef[]{n15CAR}, new NodeRef[]{rootNodeRef, n01NodeRef, n05NodeRef,
                        n12NodeRef, n13NodeRef},
                new String[]{
                        "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                + n13QName + "/" + n15QName,
                        "/" + n02QName.toString() + "/" + n13QNameLink + "/" + n14QName}, n15NodeRef, true);
    }

    private Map<QName, PropertyValue> getOrderProperties()
    {
        double orderDoubleCount = -0.11d + orderTextCount * ((orderTextCount % 2 == 0) ? 0.1d : -0.1d);
        float orderFloatCount = -3.5556f + orderTextCount * ((orderTextCount % 2 == 0) ? 0.82f : -0.82f);
        long orderLongCount = -1999999999999999L + orderTextCount * ((orderTextCount % 2 == 0) ? 299999999999999L : -299999999999999L);
        int orderIntCount = -45764576 + orderTextCount * ((orderTextCount % 2 == 0) ? 8576457 : -8576457);

        Map<QName, PropertyValue> testProperties = new HashMap<>();
        testProperties.put(CREATED_DATE, value(orderDate));
        testProperties.put(CREATED_TIME, value(orderDate));
        testProperties.put(ORDER_DOUBLE, value(orderDoubleCount));
        testProperties.put(ORDER_FLOAT, value(orderFloatCount));
        testProperties.put(ORDER_LONG, value(orderLongCount));
        testProperties.put(ORDER_INT, value(orderIntCount));
        testProperties.put(ORDER_TEXT, value(new String(
                        new char[] { (char) ('l' + ((orderTextCount % 2 == 0) ? orderTextCount
                                : -orderTextCount)) })
                        + " cabbage"));

        testProperties.put(PROP_NAME, value(ORDER_NAMES[orderTextCount]));
        testProperties.put(ORDER_LOCALISED_TEXT, value(ORDER_LOCALISED_NAMES[orderTextCount]));

        MLTextPropertyValue mlTextPropLocalisedOrder = new MLTextPropertyValue();
        if (ORDER_LOCALISED_MLTEXT_EN[orderTextCount].length() > 0)
        {
            mlTextPropLocalisedOrder.addValue(Locale.ENGLISH, ORDER_LOCALISED_MLTEXT_EN[orderTextCount]);
        }
        if (ORDER_LOCALISED_MLTEXT_FR[orderTextCount].length() > 0)
        {
            mlTextPropLocalisedOrder.addValue(Locale.FRENCH, ORDER_LOCALISED_MLTEXT_FR[orderTextCount]);
        }
        if (ORDER_LOCALISED_MLTEXT_ES[orderTextCount].length() > 0)
        {
            mlTextPropLocalisedOrder.addValue(new Locale("es"), ORDER_LOCALISED_MLTEXT_ES[orderTextCount]);
        }
        if (ORDER_LOCALISED_MLTEXT_DE[orderTextCount].length() > 0)
        {
            mlTextPropLocalisedOrder.addValue(Locale.GERMAN, ORDER_LOCALISED_MLTEXT_DE[orderTextCount]);
        }
        testProperties.put(ORDER_LOCALISED_ML_TEXT, mlTextPropLocalisedOrder);

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
        testProperties.put(ORDER_ML_TEXT, mlTextPropVal);

        orderDate = Duration.subtract(orderDate, new Duration("P1D"));
        orderTextCount++;
        return testProperties;
    }

    /**
     * Loads a medium-size dataset composed by 250 nodes.
     */
    public void loadMediumDataset() throws Exception
    {
        for (int i = 0; i < 100; i++)
        {
            orderDate = new Date();
            orderTextCount = 0;
            addNode(core, dataModel, 1, 2, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "andy",
                    new ChildAssociationRef[] { n01CAR }, new NodeRef[] {rootNodeRef}, new String[] { "/"
                            + n01QName.toString() }, n01NodeRef, true);
        }


        for (int i = 0; i < 10; i++)
        {
            orderDate = new Date();
            orderTextCount = 0;

            addNode(core, dataModel, 1, 2, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "andy",
                    new ChildAssociationRef[] { n01CAR }, new NodeRef[] {rootNodeRef}, new String[] { "/"
                            + n01QName.toString() }, n01NodeRef, true);
            addNode(core, dataModel, 1, 3, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "bob",
                    new ChildAssociationRef[] { n02CAR }, new NodeRef[] {rootNodeRef}, new String[] { "/"
                            + n02QName.toString() }, n02NodeRef, true);
            addNode(core, dataModel, 1, 4, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "cid",
                    new ChildAssociationRef[] { n03CAR }, new NodeRef[] {rootNodeRef}, new String[] { "/"
                            + n03QName.toString() }, n03NodeRef, true);
            properties04.putAll(getOrderProperties());
            addNode(core, dataModel, 1, 5, 1, TEST_TYPE, new QName[] {TEST_ASPECT}, properties04, content04,
                    "dave", new ChildAssociationRef[] { n04CAR }, new NodeRef[] {rootNodeRef},
                    new String[] { "/" + n04QName.toString() }, n04NodeRef, true);
            addNode(core, dataModel, 1, 6, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "eoin",
                    new ChildAssociationRef[] { n05CAR }, new NodeRef[] {rootNodeRef, n01NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() }, n05NodeRef, true);
            addNode(core, dataModel, 1, 7, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "fred",
                    new ChildAssociationRef[] { n06CAR }, new NodeRef[] {rootNodeRef, n01NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n06QName.toString() }, n06NodeRef, true);
            addNode(core, dataModel, 1, 8, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "gail",
                    new ChildAssociationRef[] { n07CAR }, new NodeRef[] {rootNodeRef, n02NodeRef },
                    new String[] { "/" + n02QName.toString() + "/" + n07QName.toString() }, n07NodeRef, true);
            addNode(core, dataModel, 1, 9, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "hal",
                    new ChildAssociationRef[] { n08CAR_0, n08CAR_1, n08CAR_2 }, new NodeRef[] {rootNodeRef,
                            rootNodeRef, n01NodeRef, rootNodeRef, n02NodeRef }, new String[] {
                            "/" + n08QName_0, "/" + n01QName.toString() + "/" + n08QName_1.toString(),
                            "/" + n02QName.toString() + "/" + n08QName_2.toString() }, n08NodeRef, true);
            addNode(core, dataModel, 1, 10, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "ian",
                    new ChildAssociationRef[] { n09CAR },
                    new NodeRef[] {rootNodeRef, n01NodeRef, n05NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n09QName },
                    n09NodeRef, true);
            addNode(core, dataModel, 1, 11, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "jake",
                    new ChildAssociationRef[] { n10CAR },
                    new NodeRef[] {rootNodeRef, n01NodeRef, n05NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n10QName },
                    n10NodeRef, true);
            addNode(core, dataModel, 1, 12, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "kara",
                    new ChildAssociationRef[] { n11CAR },
                    new NodeRef[] {rootNodeRef, n01NodeRef, n05NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n11QName },
                    n11NodeRef, true);
            addNode(core, dataModel, 1, 13, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "loon",
                    new ChildAssociationRef[] { n12CAR },
                    new NodeRef[] {rootNodeRef, n01NodeRef, n05NodeRef },
                    new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName },
                    n12NodeRef, true);
            addNode(core, dataModel, 1, 14, 1, TEST_SUPER_TYPE, null, getOrderProperties(), null, "mike",
                    new ChildAssociationRef[] { n13CAR, n13CARLink }, new NodeRef[] {rootNodeRef, n01NodeRef,
                            n05NodeRef, n12NodeRef, rootNodeRef, n02NodeRef }, new String[] {
                            "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                    + n13QName, "/" + n02QName.toString() + "/" + n13QNameLink },
                    n13NodeRef, true);
            properties14.putAll(getOrderProperties());
            addNode(core, dataModel, 1, 15, 1, ContentModel.TYPE_CONTENT, null, properties14, content14, "noodle",
                    new ChildAssociationRef[] { n14CAR, n14CAR_1, n14CAR_2, n14CAR_5, n14CAR_6, n14CAR_12,
                            n14CAR_13 }, new NodeRef[] {rootNodeRef, n01NodeRef, n05NodeRef, n12NodeRef,
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
                    new ChildAssociationRef[] { n15CAR }, new NodeRef[] {rootNodeRef, n01NodeRef, n05NodeRef,
                            n12NodeRef, n13NodeRef }, new String[] {
                            "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                    + n13QName + "/" + n15QName,
                            "/" + n02QName.toString() + "/" + n13QNameLink + "/" + n14QName }, n15NodeRef,
                    true);
        }
    }

    /**
     * Loads a very small dataset (just 2 nodes) with escaped qNames.
     */
    public void loadEscapingTestData() throws Exception
    {
        NodeRef childNameEscapingNodeRef = newNodeRef();
        QName childNameEscapingQName = qName(CONTENT_MODEL_1_0_URI, complexLocalName);
        QName pathChildNameEscapingQName = qName(CONTENT_MODEL_1_0_URI, ISO9075.encode(complexLocalName));

        ChildAssociationRef complexCAR =
                new ChildAssociationRef(
                        ASSOC_CHILDREN,
                        rootNodeRef,
                        childNameEscapingQName,
                        childNameEscapingNodeRef,
                        true,
                        0);

        addNode(
                core,
                dataModel,
                1,
                17,
                1,
                TEST_SUPER_TYPE,
                null,
                null,
                null,
                "system",
                new ChildAssociationRef[] { complexCAR },
                new NodeRef[] {rootNodeRef},
                new String[] { "/" + pathChildNameEscapingQName.toString() },
                childNameEscapingNodeRef,
                true);

        NodeRef numericNameEscapingNodeRef = newNodeRef();
        QName numericNameEscapingQName = qName(CONTENT_MODEL_1_0_URI, numericLocalName);
        QName pathNumericNameEscapingQName = qName(CONTENT_MODEL_1_0_URI, ISO9075.encode(numericLocalName));
        ChildAssociationRef numericCAR =
                new ChildAssociationRef(
                        ASSOC_CHILDREN,
                        rootNodeRef,
                        numericNameEscapingQName,
                        numericNameEscapingNodeRef,
                        true,
                        0);

        addNode(
                core,
                dataModel,
                1,
                18,
                1,
                TEST_SUPER_TYPE,
                null,
                null,
                null,
                "system",
                new ChildAssociationRef[] { numericCAR },
                new NodeRef[] {rootNodeRef},
                new String[] { "/" + pathNumericNameEscapingQName.toString() },
                numericNameEscapingNodeRef,
                true);
    }

    /**
     * Loads the dataset used in MNT tests.
     */
    public void loadMntTestData() throws Exception
    {
        Map<QName, PropertyValue> properties19 = new HashMap<>();
        properties19.put(
                PROP_NAME,
                value("Test.hello.txt"));

        NodeRef n19NodeRef = newNodeRef();
        QName n19QName = qName(CONTENT_MODEL_1_0_URI, "nineteen");
        ChildAssociationRef n19CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n19QName, n19NodeRef, true, 0);

        addNode(core, dataModel, 1, 19, 1,  TYPE_CONTENT, null, properties19, null, "system",
                new ChildAssociationRef[] {n19CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/"
                        + n19QName.toString() }, n19NodeRef, true);


        Map<QName, PropertyValue> properties20 = new HashMap<>();
        properties20.put(
                PROP_NAME,
                value("Test1.hello.txt"));

        NodeRef n20NodeRef = newNodeRef();
        QName n20QName = qName(CONTENT_MODEL_1_0_URI, "twenty");
        ChildAssociationRef n20CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n20QName, n20NodeRef, true, 0);
        addNode(core, dataModel, 1, 20, 1,  TYPE_CONTENT, null, properties20, null, "system",
                new ChildAssociationRef[] {n20CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/"
                        + n20QName.toString() }, n20NodeRef, true);

        Map<QName, PropertyValue> properties21 = new HashMap<>();
        properties21.put(
                PROP_NAME,
                value("one_two_three.txt"));
        NodeRef n21NodeRef = newNodeRef();
        QName n21QName = qName(CONTENT_MODEL_1_0_URI, "twentyone");
        ChildAssociationRef n21CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n21QName,
                n21NodeRef, true, 0);
        addNode(core, dataModel, 1, 21, 1,  TYPE_CONTENT, null, properties21, null, "system",
                new ChildAssociationRef[] {n21CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/"
                        + n21QName.toString() }, n21NodeRef, true);

        Map<QName, PropertyValue> properties22 = new HashMap<>();
        properties22.put(
                PROP_NAME,
                value("one_two_four.txt"));
        NodeRef n22NodeRef = newNodeRef();
        QName n22QName = QName.createQName(CONTENT_MODEL_1_0_URI, "twnetytwo");
        ChildAssociationRef n22CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n22QName,
                n22NodeRef, true, 0);
        addNode(core, dataModel, 1, 22, 1,  TYPE_CONTENT, null, properties22, null, "system",
                new ChildAssociationRef[] {n22CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/"
                        + n22QName.toString() }, n22NodeRef, true);

        Map<QName, PropertyValue> properties23 = new HashMap<>();
        properties23.put(
                PROP_NAME,
                value("one_two.txt"));
        NodeRef n23NodeRef = newNodeRef();
        QName n23QName = QName.createQName(CONTENT_MODEL_1_0_URI, "twentythree");
        ChildAssociationRef n23CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n23QName,
                n23NodeRef, true, 0);
        addNode(core, dataModel, 1, 23, 1,  TYPE_CONTENT, null, properties23, null, "system",
                new ChildAssociationRef[] {n23CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/"
                        + n23QName.toString() }, n23NodeRef, true);

        Map<QName, PropertyValue> properties24 = new HashMap<>();
        properties24.put(
                PROP_NAME,
                value("Print-Toolkit-3204-The-Print-Toolkit-has-a-new-look-565022.html"));
        NodeRef n24NodeRef = newNodeRef();
        QName n24QName = QName.createQName(CONTENT_MODEL_1_0_URI, "twentyfour");
        ChildAssociationRef n24CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n24QName,
                n24NodeRef, true, 0);
        addNode(core, dataModel, 1, 24, 1,  TYPE_CONTENT, null, properties24, null, "system",
                new ChildAssociationRef[] {n24CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/"
                        + n24QName.toString() }, n24NodeRef, true);


        Map<QName, PropertyValue> properties25 = new HashMap<>();
        properties25.put(
                PROP_NAME,
                value("Print-Toolkitf-3204-The-Print-Toolkit-has-a-new-look-565022.html"));
        NodeRef n25NodeRef = newNodeRef();
        QName n25QName = QName.createQName(CONTENT_MODEL_1_0_URI, "twentyfive");
        ChildAssociationRef n25CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n25QName,
                n25NodeRef, true, 0);
        addNode(core, dataModel, 1, 25, 1,  TYPE_CONTENT, null, properties25, null, "system",
                new ChildAssociationRef[] {n25CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/"
                        + n25QName.toString() }, n25NodeRef, true);

        Map<QName, PropertyValue> properties26 = new HashMap<>();
        properties26.put(
                PROP_NAME,
                value("apple pear peach 20150911100000.txt"));
        NodeRef n26NodeRef = newNodeRef();
        QName n26QName = QName.createQName(CONTENT_MODEL_1_0_URI, "twentysix");
        ChildAssociationRef n26CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n26QName,
                n26NodeRef, true, 0);
        addNode(core, dataModel, 1, 26, 1,  TYPE_CONTENT, null, properties26, null, "system",
                new ChildAssociationRef[] {n26CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/"
                        + n26QName.toString() }, n26NodeRef, true);

        Map<QName, PropertyValue> properties27 = new HashMap<>();
        properties27.put(PROP_NAME, value("Craig.txt"));
        properties27.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0L, "UTF-8", "text/plain", null));
        Map<QName, String> content27 = new HashMap<>();
        content27.put(ContentModel.PROP_CONTENT, "AnalystName Craig");
        NodeRef n27NodeRef = newNodeRef();
        QName n27QName = QName.createQName(CONTENT_MODEL_1_0_URI, "twentyseven");
        ChildAssociationRef n27CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n27QName, n27NodeRef, true, 0);
        addNode(core, dataModel, 1, 27, 1, TYPE_CONTENT, null, properties27, content27, "system", new ChildAssociationRef[] {n27CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/" + n27QName.toString() }, n27NodeRef, true);

        Map<QName, PropertyValue> properties28 = new HashMap<>();
        properties28.put(PROP_NAME, value("Scott.txt"));
        properties28.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0L, "UTF-8", "text/plain", null));
        Map<QName, String> content28 = new HashMap<>();
        content28.put(ContentModel.PROP_CONTENT, "AnalystName Scott \n Craig");
        NodeRef n28NodeRef = newNodeRef();
        QName n28QName = QName.createQName(CONTENT_MODEL_1_0_URI, "twentyeight");
        ChildAssociationRef n28CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n28QName, n28NodeRef, true, 0);
        addNode(core, dataModel, 1, 28, 1, TYPE_CONTENT, null, properties28, content28, "system", new ChildAssociationRef[] {n28CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/" + n28QName.toString() }, n28NodeRef, true);

        Map<QName, PropertyValue> properties29 = new HashMap<>();
        properties29.put(PROP_NAME, value("BASF_2016.txt"));
        NodeRef n29NodeRef = newNodeRef();
        QName n29QName = QName.createQName(CONTENT_MODEL_1_0_URI, "twentynine");
        ChildAssociationRef n29CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n29QName, n29NodeRef, true, 0);
        addNode(core, dataModel, 1, 29, 1,  TYPE_CONTENT, null, properties29, null, "system", new ChildAssociationRef[] {n29CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/" + n29QName.toString() }, n29NodeRef, true);

        Map<QName, PropertyValue> properties30 = new HashMap<>();
        properties30.put(PROP_NAME, value("BASF_2016(GMT0800).txt"));
        NodeRef n30NodeRef = newNodeRef();
        QName n30QName = QName.createQName(CONTENT_MODEL_1_0_URI, "thirty");
        ChildAssociationRef n30CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n30QName, n30NodeRef, true, 0);
        addNode(core, dataModel, 1, 30, 1,  TYPE_CONTENT, null, properties30, null, "system", new ChildAssociationRef[] {n30CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/" + n30QName.toString() }, n30NodeRef, true);

        Map<QName, PropertyValue> properties31 = new HashMap<>();
        properties31.put(PROP_NAME, value("BASF_2016-03-08-10-42-19.txt"));
        NodeRef n31NodeRef = newNodeRef();
        QName n31QName = QName.createQName(CONTENT_MODEL_1_0_URI, "thirtyone");
        ChildAssociationRef n31CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n31QName, n31NodeRef, true, 0);
        addNode(core, dataModel, 1, 31, 1,  TYPE_CONTENT, null, properties31, null, "system", new ChildAssociationRef[] {n31CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/" + n31QName.toString() }, n31NodeRef, true);

        Map<QName, PropertyValue> properties32 = new HashMap<>();
        properties32.put(PROP_NAME, value("BASF_2016 GMT 0800.txt"));
        NodeRef n32NodeRef = newNodeRef();
        QName n32QName = QName.createQName(CONTENT_MODEL_1_0_URI, "thirtytwo");
        ChildAssociationRef n32CAR = new ChildAssociationRef(ASSOC_CONTAINS, rootNodeRef, n32QName, n32NodeRef, true, 0);
        addNode(core, dataModel, 1, 32, 1,  TYPE_CONTENT, null, properties32, null, "system", new ChildAssociationRef[] {n32CAR}, new NodeRef[] {rootNodeRef}, new String[] { "/" + n32QName.toString() }, n32NodeRef, true);
    }

    public NodeRef newNodeRef()
    {
        return new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
    }

    public QName qName(String localName)
    {
        return qName(TEST_NAMESPACE, localName);
    }

    public QName qName(String namespace, String localName)
    {
        return QName.createQName(namespace, localName);
    }

    private StringPropertyValue value(Object value)
    {
        return new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, value));
    }

    private StringPropertyValue value(String value)
    {
        return new StringPropertyValue(value);
    }
}