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
import org.apache.solr.core.SolrCore;
import org.junit.BeforeClass;
/**
 * Load test data as part of legacy test.
 * @author Michael Suzuki
 *
 */
public class LoadAFTSTestData extends AbstractAlfrescoSolrTests implements AlfrecsoSolrConstants
{
    @BeforeClass
    public static void loadTestSet() throws Exception 
    {
        
        initAlfrescoCore("solrconfig-afts.xml", "schema-afts.xml");
        Thread.sleep(30000);
        // Root
        SolrCore core = h.getCore();
        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        dataModel.setCMDefaultUri();

        NodeRef rootNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        addStoreRoot(core, dataModel, rootNodeRef, 1, 1, 1, 1);

        // 1
        NodeRef n01NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n01QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "one");
        ChildAssociationRef n01CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n01QName,
                n01NodeRef, true, 0);
        addNode(core, dataModel, 1, 2, 1, testSuperType, null, getOrderProperties(), null, "andy",
                new ChildAssociationRef[] { n01CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n01QName.toString() }, n01NodeRef, true);
        
        testNodeRef = n01NodeRef;

        // 2

        NodeRef n02NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n02QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "two");
        ChildAssociationRef n02CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n02QName,
                n02NodeRef, true, 0);
        addNode(core, dataModel, 1, 3, 1, testSuperType, null, getOrderProperties(), null, "bob",
                new ChildAssociationRef[] { n02CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n02QName.toString() }, n02NodeRef, true);

        // 3

        NodeRef n03NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n03QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "three");
        ChildAssociationRef n03CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n03QName,
                n03NodeRef, true, 0);
        addNode(core, dataModel, 1, 4, 1, testSuperType, null, getOrderProperties(), null, "cid",
                new ChildAssociationRef[] { n03CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n03QName.toString() }, n03NodeRef, true);

        // 4

        HashMap<QName, PropertyValue> properties04 = new HashMap<QName, PropertyValue>();
        HashMap<QName, String> content04 = new HashMap<QName, String>();
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
        mlText.addValue(new Locale("ja"), "�?ナナ");
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

        NodeRef n04NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n04QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "four");
        ChildAssociationRef n04CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef, n04QName,
                n04NodeRef, true, 0);

        properties04.put(QName.createQName(TEST_NAMESPACE, "aspectProperty"), new StringPropertyValue(""));
        addNode(core, dataModel, 1, 5, 1, testType, new QName[] { testAspect }, properties04, content04, "dave",
                new ChildAssociationRef[] { n04CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + n04QName.toString() }, n04NodeRef, true);

        // 5

        NodeRef n05NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n05QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "five");
        ChildAssociationRef n05CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n01NodeRef, n05QName,
                n05NodeRef, true, 0);
        addNode(core, dataModel, 1, 6, 1, testSuperType, null, getOrderProperties(), null, "eoin",
                new ChildAssociationRef[] { n05CAR }, new NodeRef[] { rootNodeRef, n01NodeRef },
                new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() }, n05NodeRef, true);

        // 6

        NodeRef n06NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n06QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "six");
        ChildAssociationRef n06CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n01NodeRef, n06QName,
                n06NodeRef, true, 0);
        addNode(core, dataModel, 1, 7, 1, testSuperType, null, getOrderProperties(), null, "fred",
                new ChildAssociationRef[] { n06CAR }, new NodeRef[] { rootNodeRef, n01NodeRef },
                new String[] { "/" + n01QName.toString() + "/" + n06QName.toString() }, n06NodeRef, true);

        // 7

        NodeRef n07NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n07QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "seven");
        ChildAssociationRef n07CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n02NodeRef, n07QName,
                n07NodeRef, true, 0);
        addNode(core, dataModel, 1, 8, 1, testSuperType, null, getOrderProperties(), null, "gail",
                new ChildAssociationRef[] { n07CAR }, new NodeRef[] { rootNodeRef, n02NodeRef },
                new String[] { "/" + n02QName.toString() + "/" + n07QName.toString() }, n07NodeRef, true);

        // 8

        NodeRef n08NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n08QName_0 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "eight-0");
        QName n08QName_1 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "eight-1");
        QName n08QName_2 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "eight-2");
        ChildAssociationRef n08CAR_0 = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef,
                n08QName_0, n08NodeRef, false, 2);
        ChildAssociationRef n08CAR_1 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n01NodeRef, n08QName_1,
                n08NodeRef, false, 1);
        ChildAssociationRef n08CAR_2 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n02NodeRef, n08QName_2,
                n08NodeRef, true, 0);

        addNode(core, dataModel, 1, 9, 1, testSuperType, null, getOrderProperties(), null, "hal",
                new ChildAssociationRef[] { n08CAR_0, n08CAR_1, n08CAR_2 }, new NodeRef[] { rootNodeRef,
                        rootNodeRef, n01NodeRef, rootNodeRef, n02NodeRef }, new String[] {
                        "/" + n08QName_0, "/" + n01QName.toString() + "/" + n08QName_1.toString(),
                        "/" + n02QName.toString() + "/" + n08QName_2.toString() }, n08NodeRef, true);

        // 9

        NodeRef n09NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n09QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "nine");
        ChildAssociationRef n09CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef, n09QName,
                n09NodeRef, true, 0);
        addNode(core, dataModel, 1, 10, 1, testSuperType, null, getOrderProperties(), null, "ian",
                new ChildAssociationRef[] { n09CAR }, new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef },
                new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n09QName },
                n09NodeRef, true);

        // 10

        NodeRef n10NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n10QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "ten");
        ChildAssociationRef n10CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef, n10QName,
                n10NodeRef, true, 0);
        addNode(core, dataModel, 1, 11, 1, testSuperType, null, getOrderProperties(), null, "jake",
                new ChildAssociationRef[] { n10CAR }, new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef },
                new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n10QName },
                n10NodeRef, true);

        // 11

        NodeRef n11NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n11QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "eleven");
        ChildAssociationRef n11CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef, n11QName,
                n11NodeRef, true, 0);
        addNode(core, dataModel, 1, 12, 1, testSuperType, null, getOrderProperties(), null, "kara",
                new ChildAssociationRef[] { n11CAR }, new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef },
                new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n11QName },
                n11NodeRef, true);

        // 12

        NodeRef n12NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n12QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "twelve");
        ChildAssociationRef n12CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef, n12QName,
                n12NodeRef, true, 0);
        addNode(core, dataModel, 1, 13, 1, testSuperType, null, getOrderProperties(), null, "loon",
                new ChildAssociationRef[] { n12CAR }, new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef },
                new String[] { "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName },
                n12NodeRef, true);

        // 13

        NodeRef n13NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n13QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "thirteen");
        QName n13QNameLink = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "link");
        ChildAssociationRef n13CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n12NodeRef, n13QName,
                n13NodeRef, true, 0);
        ChildAssociationRef n13CARLink = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n02NodeRef, n13QName,
                n13NodeRef, false, 0);
        addNode(core, dataModel, 1, 14, 1, testSuperType, null, getOrderProperties(), null, "mike",
                new ChildAssociationRef[] { n13CAR, n13CARLink }, new NodeRef[] { rootNodeRef, n01NodeRef,
                        n05NodeRef, n12NodeRef, rootNodeRef, n02NodeRef },
                new String[] {
                        "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                + n13QName, "/" + n02QName.toString() + "/" + n13QNameLink },
                n13NodeRef, true);

        // 14

        HashMap<QName, PropertyValue> properties14 = new HashMap<QName, PropertyValue>();
        properties14.putAll(getOrderProperties());
        HashMap<QName, String> content14 = new HashMap<QName, String>();
        MLTextPropertyValue desc1 = new MLTextPropertyValue();
        desc1.addValue(Locale.ENGLISH, "Alfresco tutorial");
        desc1.addValue(Locale.US, "Alfresco tutorial");

        Date explicitCreatedDate = new Date();
        try
        {
            Thread.sleep(2000);
        }
        catch (InterruptedException e)
        {
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
        NodeRef n14NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n14QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "fourteen");
        QName n14QNameCommon = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "common");
        ChildAssociationRef n14CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n13NodeRef, n14QName,
                n14NodeRef, true, 0);
        ChildAssociationRef n14CAR_1 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n01NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        ChildAssociationRef n14CAR_2 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n02NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        ChildAssociationRef n14CAR_5 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n05NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        ChildAssociationRef n14CAR_6 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n06NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        ChildAssociationRef n14CAR_12 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n12NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        ChildAssociationRef n14CAR_13 = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n13NodeRef,
                n14QNameCommon, n14NodeRef, false, 0);
        addNode(core, dataModel, 1, 15, 1, ContentModel.TYPE_CONTENT, new QName[] {ContentModel.ASPECT_TITLED }, properties14, content14, "noodle",
                new ChildAssociationRef[] { n14CAR, n14CAR_1, n14CAR_2, n14CAR_5, n14CAR_6, n14CAR_12,
                        n14CAR_13 }, new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef, n12NodeRef,
                        n13NodeRef }, new String[] {
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

        // 15

        HashMap<QName, PropertyValue> properties15 = new HashMap<QName, PropertyValue>();
        properties15.putAll(getOrderProperties());
        properties15.put(
                ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE
                        .convert(String.class, explicitCreatedDate)));
        HashMap<QName, String> content15 = new HashMap<QName, String>();
        content15.put(ContentModel.PROP_CONTENT, "          ");
        NodeRef n15NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName n15QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "fifteen");
        ChildAssociationRef n15CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, n13NodeRef, n15QName,
                n15NodeRef, true, 0);
        addNode(core, dataModel, 1, 16, 1, ContentModel.TYPE_THUMBNAIL, null, properties15, content15, "ood",
                new ChildAssociationRef[] { n15CAR }, new NodeRef[] { rootNodeRef, n01NodeRef, n05NodeRef,
                        n12NodeRef, n13NodeRef },
                new String[] {
                        "/" + n01QName.toString() + "/" + n05QName.toString() + "/" + n12QName + "/"
                                + n13QName + "/" + n15QName,
                        "/" + n02QName.toString() + "/" + n13QNameLink + "/" + n14QName }, n15NodeRef, true);
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
