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

package org.alfresco.solr.query.cmis;

import static org.alfresco.solr.AlfrescoSolrUtils.addNode;
import static org.alfresco.solr.AlfrescoSolrUtils.addStoreRoot;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;

import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Locale;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.cmr.repository.datatype.DefaultTypeConverter;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.ContentPropertyValue;
import org.alfresco.solr.client.MLTextPropertyValue;
import org.alfresco.solr.client.MultiPropertyValue;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.apache.solr.core.SolrCore;
import org.junit.BeforeClass;
/**
 * CMIS test data load, prepare test suite with data
 * @author Michael Suzuki
 *
 */
public class LoadCMISData extends AbstractAlfrescoSolrTests
{
    protected static NodeRef testCMISContent00NodeRef;
    protected static NodeRef testCMISRootNodeRef;
    protected static NodeRef testCMISBaseFolderNodeRef;
    protected static NodeRef testCMISFolder00NodeRef;
    protected static QName testCMISBaseFolderQName;
    protected static QName testCMISFolder00QName;
    protected static Date testCMISDate00;
    
    private static String[] mlOrderable_en = new String[] { "AAAA BBBB", "EEEE FFFF", "II", "KK", "MM", "OO", "QQ",
            "SS", "UU", "AA", "CC" };
    
    private static String[] mlOrderable_fr = new String[] { "CCCC DDDD", "GGGG HHHH", "JJ", "LL", "NN", "PP", "RR",
            "TT", "VV", "BB", "DD" };
    
    protected static void addTypeTestData(NodeRef folder00NodeRef,
            NodeRef rootNodeRef,
            NodeRef baseFolderNodeRef,
            Object baseFolderQName,
            Object folder00QName,
            Date date1)throws IOException
    {
        HashMap<QName, PropertyValue> content00Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc00 = new MLTextPropertyValue();
        desc00.addValue(Locale.ENGLISH, "Test One");
        desc00.addValue(Locale.US, "Test 1");
        content00Properties.put(ContentModel.PROP_DESCRIPTION, desc00);
        content00Properties.put(ContentModel.PROP_TITLE, desc00);
        content00Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Test One"));
        content00Properties.put(ContentModel.PROP_CREATED,
        new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date1)));
        
        StringPropertyValue single = new StringPropertyValue("Un tokenised");
        content00Properties.put(singleTextUntokenised, single);
        content00Properties.put(singleTextTokenised, single);
        content00Properties.put(singleTextBoth, single);
        MultiPropertyValue multi = new MultiPropertyValue();
        multi.addValue(single);
        multi.addValue(new StringPropertyValue("two parts"));
        content00Properties.put(multipleTextUntokenised, multi);
        content00Properties.put(multipleTextTokenised, multi);
        content00Properties.put(multipleTextBoth, multi);
        content00Properties.put(singleMLTextUntokenised, makeMLText());
        content00Properties.put(singleMLTextTokenised, makeMLText());
        content00Properties.put(singleMLTextBoth, makeMLText());
        content00Properties.put(multipleMLTextUntokenised, makeMLTextMVP());
        content00Properties.put(multipleMLTextTokenised, makeMLTextMVP());
        content00Properties.put(multipleMLTextBoth, makeMLTextMVP());
        StringPropertyValue one = new StringPropertyValue("1");
        StringPropertyValue two = new StringPropertyValue("2");
        MultiPropertyValue multiDec = new MultiPropertyValue();
        multiDec.addValue(one);
        multiDec.addValue(new StringPropertyValue("1.1"));
        content00Properties.put(singleFloat, one);
        content00Properties.put(multipleFloat, multiDec);
        content00Properties.put(singleDouble, one);
        content00Properties.put(multipleDouble, multiDec);
        MultiPropertyValue multiInt = new MultiPropertyValue();
        multiInt.addValue(one);
        multiInt.addValue(two);
        content00Properties.put(singleInteger, one);
        content00Properties.put(multipleInteger, multiInt);
        content00Properties.put(singleLong, one);
        content00Properties.put(multipleLong, multiInt);
        
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date1);
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.add(Calendar.DAY_OF_MONTH, 2);
        Date date2 = cal.getTime();
        StringPropertyValue d1 = new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date1));
        StringPropertyValue d2 = new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date2));
        MultiPropertyValue multiDate = new MultiPropertyValue();
        multiDate.addValue(d1);
        multiDate.addValue(d2);
        content00Properties.put(singleDate, d1);
        content00Properties.put(multipleDate, multiDate);
        content00Properties.put(singleDatetime, d1);
        content00Properties.put(multipleDatetime, multiDate);
        
        StringPropertyValue bTrue = new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, true));
        StringPropertyValue bFalse = new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, false));
        MultiPropertyValue multiBool = new MultiPropertyValue();
        multiBool.addValue(bTrue);
        multiBool.addValue(bFalse);
        
        content00Properties.put(singleBoolean, bTrue);
        content00Properties.put(multipleBoolean, multiBool);
        
        NodeRef content00NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content00QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Test One");
        ChildAssociationRef content00CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder00NodeRef,
        content00QName, content00NodeRef, true, 0);
        addNode(h.getCore(),
        dataModel,
        1,
        100,
        1,
        extendedContent,
        new QName[] { ContentModel.ASPECT_OWNABLE, ContentModel.ASPECT_TITLED },
        content00Properties,
        null,
        "andy",
        new ChildAssociationRef[] { content00CAR },
        new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef },
        new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/" + content00QName.toString() },
        content00NodeRef,
        true);
    }
   
  
    protected static MLTextPropertyValue makeMLText()
    {
        return makeMLText(0);
    }
    protected static MLTextPropertyValue makeMLText(int position)
    {
        MLTextPropertyValue ml = new MLTextPropertyValue();
        ml.addValue(Locale.ENGLISH, mlOrderable_en[position]);
        ml.addValue(Locale.FRENCH, mlOrderable_fr[position]);
        return ml;
    }

    protected static MultiPropertyValue makeMLTextMVP()
    {
        return makeMLTextMVP(0);
    }

    protected static MultiPropertyValue makeMLTextMVP(int position)
    {
        MLTextPropertyValue m1 = new MLTextPropertyValue();
        m1.addValue(Locale.ENGLISH, mlOrderable_en[position]);
        MLTextPropertyValue m2 = new MLTextPropertyValue();
        m2.addValue(Locale.FRENCH, mlOrderable_fr[position]);
        MultiPropertyValue answer = new MultiPropertyValue();
        answer.addValue(m1);
        answer.addValue(m2);
        return answer;
    }
    @BeforeClass
    public static void loadCMISTestSet() throws Exception 
    {
        initAlfrescoCore("schema.xml");
        SolrCore core = h.getCore();
        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        dataModel.getNamespaceDAO().removePrefix("");
        dataModel.setCMDefaultUri();
        NodeRef rootNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        testCMISRootNodeRef = rootNodeRef;
        addStoreRoot(core, dataModel, rootNodeRef, 1, 1, 1, 1);

        // Base

        HashMap<QName, PropertyValue> baseFolderProperties = new HashMap<QName, PropertyValue>();
        baseFolderProperties.put(ContentModel.PROP_NAME, new StringPropertyValue("Base Folder"));
        // This variable is never used. What was it meant to be used for?
        NodeRef baseFolderNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        testCMISBaseFolderNodeRef = baseFolderNodeRef;
        QName baseFolderQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "baseFolder");
        testCMISBaseFolderQName = baseFolderQName;
        ChildAssociationRef n01CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef,
                baseFolderQName, baseFolderNodeRef, true, 0);
        addNode(core, dataModel, 1, 2, 1, ContentModel.TYPE_FOLDER, null, baseFolderProperties, null, "andy",
                new ChildAssociationRef[] { n01CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                        + baseFolderQName.toString() }, baseFolderNodeRef, true);

        // Folders

        HashMap<QName, PropertyValue> folder00Properties = new HashMap<QName, PropertyValue>();
        folder00Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 0"));
        NodeRef folder00NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        testCMISFolder00NodeRef = folder00NodeRef;
        QName folder00QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 0");
        testCMISFolder00QName = folder00QName;
        ChildAssociationRef folder00CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, baseFolderNodeRef,
                folder00QName, folder00NodeRef, true, 0);
        addNode(core, dataModel, 1, 3, 1, ContentModel.TYPE_FOLDER, null, folder00Properties, null, "andy",
                new ChildAssociationRef[] { folder00CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() },
                folder00NodeRef, true);

        HashMap<QName, PropertyValue> folder01Properties = new HashMap<QName, PropertyValue>();
        folder01Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 1"));
        NodeRef folder01NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder01QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 1");
        ChildAssociationRef folder01CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, baseFolderNodeRef,
                folder01QName, folder01NodeRef, true, 0);
        addNode(core, dataModel, 1, 4, 1, ContentModel.TYPE_FOLDER, null, folder01Properties, null, "bob",
                new ChildAssociationRef[] { folder01CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder01QName.toString() },
                folder01NodeRef, true);

        HashMap<QName, PropertyValue> folder02Properties = new HashMap<QName, PropertyValue>();
        folder02Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 2"));
        NodeRef folder02NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder02QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 2");
        ChildAssociationRef folder02CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, baseFolderNodeRef,
                folder02QName, folder02NodeRef, true, 0);
        addNode(core, dataModel, 1, 5, 1, ContentModel.TYPE_FOLDER, null, folder02Properties, null, "cid",
                new ChildAssociationRef[] { folder02CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder02QName.toString() },
                folder02NodeRef, true);

        HashMap<QName, PropertyValue> folder03Properties = new HashMap<QName, PropertyValue>();
        folder03Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 3"));
        NodeRef folder03NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder03QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 3");
        ChildAssociationRef folder03CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, baseFolderNodeRef,
                folder03QName, folder03NodeRef, true, 0);
        addNode(core, dataModel, 1, 6, 1, ContentModel.TYPE_FOLDER, null, folder03Properties, null, "dave",
                new ChildAssociationRef[] { folder03CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder03QName.toString() },
                folder03NodeRef, true);

        HashMap<QName, PropertyValue> folder04Properties = new HashMap<QName, PropertyValue>();
        folder04Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 4"));
        NodeRef folder04NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder04QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 4");
        ChildAssociationRef folder04CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder00NodeRef,
                folder04QName, folder04NodeRef, true, 0);
        addNode(core, dataModel, 1, 7, 1, ContentModel.TYPE_FOLDER, null, folder04Properties, null, "eoin",
                new ChildAssociationRef[] { folder04CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef,
                        folder00NodeRef }, new String[] { "/" + baseFolderQName.toString() + "/"
                        + folder00QName.toString() + "/" + folder04QName.toString() }, folder04NodeRef,
                true);

        HashMap<QName, PropertyValue> folder05Properties = new HashMap<QName, PropertyValue>();
        folder05Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 5"));
        NodeRef folder05NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder05QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 5");
        ChildAssociationRef folder05CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder00NodeRef,
                folder05QName, folder05NodeRef, true, 0);
        addNode(core, dataModel, 1, 8, 1, ContentModel.TYPE_FOLDER, null, folder05Properties, null, "fred",
                new ChildAssociationRef[] { folder05CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef,
                        folder00NodeRef }, new String[] { "/" + baseFolderQName.toString() + "/"
                        + folder00QName.toString() + "/" + folder05QName.toString() }, folder05NodeRef,
                true);

        HashMap<QName, PropertyValue> folder06Properties = new HashMap<QName, PropertyValue>();
        folder06Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 6"));
        NodeRef folder06NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder06QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 6");
        ChildAssociationRef folder06CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder05NodeRef,
                folder06QName, folder06NodeRef, true, 0);
        addNode(core, dataModel, 1, 9, 1, ContentModel.TYPE_FOLDER, null, folder06Properties, null, "gail",
                new ChildAssociationRef[] { folder06CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef,
                        folder00NodeRef, folder05NodeRef }, new String[] { "/" + baseFolderQName.toString()
                        + "/" + folder00QName.toString() + "/" + folder05QName.toString() + "/"
                        + folder06QName.toString() }, folder06NodeRef, true);

        HashMap<QName, PropertyValue> folder07Properties = new HashMap<QName, PropertyValue>();
        folder07Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 7"));
        NodeRef folder07NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder07QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 7");
        ChildAssociationRef folder07CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder06NodeRef,
                folder07QName, folder07NodeRef, true, 0);
        addNode(core,
                dataModel,
                1,
                10,
                1,
                ContentModel.TYPE_FOLDER,
                null,
                folder07Properties,
                null,
                "hal",
                new ChildAssociationRef[] { folder07CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef, folder05NodeRef,
                        folder06NodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                        + folder05QName.toString() + "/" + folder06QName.toString() + "/"
                        + folder07QName.toString() }, folder07NodeRef, true);

        HashMap<QName, PropertyValue> folder08Properties = new HashMap<QName, PropertyValue>();
        folder08Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 8"));
        NodeRef folder08NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder08QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 8");
        ChildAssociationRef folder08CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder07NodeRef,
                folder08QName, folder08NodeRef, true, 0);
        addNode(core,
                dataModel,
                1,
                11,
                1,
                ContentModel.TYPE_FOLDER,
                null,
                folder08Properties,
                null,
                "ian",
                new ChildAssociationRef[] { folder08CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef, folder05NodeRef,
                        folder06NodeRef, folder07NodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                        + folder05QName.toString() + "/" + folder06QName.toString() + "/"
                        + folder07QName.toString() + "/" + folder08QName.toString() }, folder08NodeRef,
                true);

        HashMap<QName, PropertyValue> folder09Properties = new HashMap<QName, PropertyValue>();
        folder09Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 9'"));
        NodeRef folder09NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder09QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 9'");
        ChildAssociationRef folder09CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder08NodeRef,
                folder09QName, folder09NodeRef, true, 0);
        addNode(core,
                dataModel,
                1,
                12,
                1,
                ContentModel.TYPE_FOLDER,
                null,
                folder09Properties,
                null,
                "jake",
                new ChildAssociationRef[] { folder09CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef, folder05NodeRef,
                        folder06NodeRef, folder07NodeRef, folder08NodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                        + folder05QName.toString() + "/" + folder06QName.toString() + "/"
                        + folder07QName.toString() + "/" + folder08QName.toString() + "/"
                        + folder09QName.toString() }, folder09NodeRef, true);

        // content

        HashMap<QName, PropertyValue> content00Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc00 = new MLTextPropertyValue();
        desc00.addValue(Locale.ENGLISH, "Alfresco tutorial");
        desc00.addValue(Locale.US, "Alfresco tutorial");
        content00Properties.put(ContentModel.PROP_DESCRIPTION, desc00);
        content00Properties.put(ContentModel.PROP_TITLE, desc00);
        content00Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                "text/plain", null));
        content00Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Alfresco Tutorial"));
        content00Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content00Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        content00Properties.put(ContentModel.PROP_VERSION_LABEL, new StringPropertyValue("1.0"));
        content00Properties.put(ContentModel.PROP_OWNER, new StringPropertyValue("andy"));
        Date date00 = new Date();
        testCMISDate00 = date00;

        content00Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date00)));
        content00Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date00)));
        HashMap<QName, String> content00Content = new HashMap<QName, String>();
        content00Content
                .put(ContentModel.PROP_CONTENT,
                        "The quick brown fox jumped over the lazy dog and ate the Alfresco Tutorial, in pdf format, along with the following stop words;  a an and are"
                                + " as at be but by for if in into is it no not of on or such that the their then there these they this to was will with: "
                                + " and random charcters \u00E0\u00EA\u00EE\u00F0\u00F1\u00F6\u00FB\u00FF score");
        NodeRef content00NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        testCMISContent00NodeRef = content00NodeRef;
        QName content00QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Alfresco Tutorial");
        ChildAssociationRef content00CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder00NodeRef,
                content00QName, content00NodeRef, true, 0);
        addNode(core, dataModel, 1, 13, 1, ContentModel.TYPE_CONTENT, new QName[] { ContentModel.ASPECT_OWNABLE,
                        ContentModel.ASPECT_TITLED }, content00Properties, content00Content, "andy",
                new ChildAssociationRef[] { content00CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef,
                        folder00NodeRef }, new String[] { "/" + baseFolderQName.toString() + "/"
                        + folder00QName.toString() + "/" + content00QName.toString() }, content00NodeRef,
                true);

        HashMap<QName, PropertyValue> content01Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc01 = new MLTextPropertyValue();
        desc01.addValue(Locale.ENGLISH, "One");
        desc01.addValue(Locale.US, "One");
        content01Properties.put(ContentModel.PROP_DESCRIPTION, desc01);
        content01Properties.put(ContentModel.PROP_TITLE, desc01);
        content01Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                "text/plain", null));
        content01Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("AA%"));
        content01Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content01Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date01 = new Date(date00.getTime() + 1000);
        content01Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date01)));
        content01Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date01)));
        HashMap<QName, String> content01Content = new HashMap<QName, String>();
        content01Content.put(ContentModel.PROP_CONTENT, "One Zebra Apple score score score score score score score score score score score");
        NodeRef content01NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content01QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "AA%");
        ChildAssociationRef content01CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder01NodeRef,
                content01QName, content01NodeRef, true, 0);
        addNode(core, dataModel, 1, 14, 1, ContentModel.TYPE_CONTENT, new QName[] { ContentModel.ASPECT_TITLED },
                content01Properties, content01Content, "cmis", new ChildAssociationRef[] { content01CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder01NodeRef }, new String[] { "/"
                        + baseFolderQName.toString() + "/" + folder01QName.toString() + "/"
                        + content01QName.toString() }, content01NodeRef, true);

        HashMap<QName, PropertyValue> content02Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc02 = new MLTextPropertyValue();
        desc02.addValue(Locale.ENGLISH, "Two");
        desc02.addValue(Locale.US, "Two");
        content02Properties.put(ContentModel.PROP_DESCRIPTION, desc02);
        content02Properties.put(ContentModel.PROP_TITLE, desc02);
        content02Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                "text/plain", null));
        content02Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("BB_"));
        content02Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content02Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date02 = new Date(date01.getTime() + 1000);
        content02Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date02)));
        content02Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date02)));
        HashMap<QName, String> content02Content = new HashMap<QName, String>();
        content02Content.put(ContentModel.PROP_CONTENT, "Two Zebra Banana score score score score score score score score score score pad");
        NodeRef content02NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content02QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "BB_");
        ChildAssociationRef content02CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder02NodeRef,
                content02QName, content02NodeRef, true, 0);
        addNode(core, dataModel, 1, 15, 1, ContentModel.TYPE_CONTENT, new QName[] { ContentModel.ASPECT_TITLED },
                content02Properties, content02Content, "cmis", new ChildAssociationRef[] { content02CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder02NodeRef }, new String[] { "/"
                        + baseFolderQName.toString() + "/" + folder02QName.toString() + "/"
                        + content02QName.toString() }, content02NodeRef, true);

        HashMap<QName, PropertyValue> content03Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc03 = new MLTextPropertyValue();
        desc03.addValue(Locale.ENGLISH, "Three");
        desc03.addValue(Locale.US, "Three");
        content03Properties.put(ContentModel.PROP_DESCRIPTION, desc03);
        content03Properties.put(ContentModel.PROP_TITLE, desc03);
        content03Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                "text/plain", null));
        content03Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("CC\\"));
        content03Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content03Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date03 = new Date(date02.getTime() + 1000);
        content03Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date03)));
        content03Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date03)));
        HashMap<QName, String> content03Content = new HashMap<QName, String>();
        content03Content.put(ContentModel.PROP_CONTENT, "Three Zebra Clementine score score score score score score score score score pad pad");
        NodeRef content03NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content03QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "CC\\");
        ChildAssociationRef content03CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder03NodeRef,
                content03QName, content03NodeRef, true, 0);
        addNode(core, dataModel, 1, 16, 1, ContentModel.TYPE_CONTENT, new QName[] { ContentModel.ASPECT_TITLED },
                content03Properties, content03Content, "cmis", new ChildAssociationRef[] { content03CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder03NodeRef }, new String[] { "/"
                        + baseFolderQName.toString() + "/" + folder03QName.toString() + "/"
                        + content03QName.toString() }, content03NodeRef, true);

        HashMap<QName, PropertyValue> content04Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc04 = new MLTextPropertyValue();
        desc04.addValue(Locale.ENGLISH, "Four");
        desc04.addValue(Locale.US, "Four");
        content04Properties.put(ContentModel.PROP_DESCRIPTION, desc04);
        content04Properties.put(ContentModel.PROP_TITLE, desc04);
        content04Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                "text/plain", null));
        content04Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("DD\'"));
        content04Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content04Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date04 = new Date(date03.getTime() + 1000);
        content04Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date04)));
        content04Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date04)));
        HashMap<QName, String> content04Content = new HashMap<QName, String>();
        content04Content.put(ContentModel.PROP_CONTENT, "Four zebra durian score score score score score score score score pad pad pad");
        NodeRef content04NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content04QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "DD\'");
        ChildAssociationRef content04CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder04NodeRef,
                content04QName, content04NodeRef, true, 0);
        addNode(core, dataModel, 1, 17, 1, ContentModel.TYPE_CONTENT, new QName[] { ContentModel.ASPECT_TITLED },
                content04Properties, content04Content, null, new ChildAssociationRef[] { content04CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef, folder04NodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                        + folder04QName.toString() + "/" + content04QName.toString() }, content04NodeRef,
                true);

        HashMap<QName, PropertyValue> content05Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc05 = new MLTextPropertyValue();
        desc05.addValue(Locale.ENGLISH, "Five");
        desc05.addValue(Locale.US, "Five");
        content05Properties.put(ContentModel.PROP_DESCRIPTION, desc05);
        content05Properties.put(ContentModel.PROP_TITLE, desc05);
        content05Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                "text/plain", null));
        content05Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("EE.aa"));
        content05Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content05Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date05 = new Date(date04.getTime() + 1000);
        content05Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date05)));
        content05Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date05)));
        content05Properties.put(
                ContentModel.PROP_EXPIRY_DATE,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class,
                        DefaultTypeConverter.INSTANCE.convert(Date.class, "2012-12-12T12:12:12.012Z"))));
        content05Properties.put(ContentModel.PROP_LOCK_OWNER, new StringPropertyValue("andy"));
        content05Properties.put(ContentModel.PROP_LOCK_TYPE, new StringPropertyValue("WRITE_LOCK"));
        HashMap<QName, String> content05Content = new HashMap<QName, String>();
        content05Content.put(ContentModel.PROP_CONTENT, "Five zebra Ebury score score score score score score score pad pad pad pad");
        NodeRef content05NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content05QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "EE.aa");
        ChildAssociationRef content05CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder05NodeRef,
                content05QName, content05NodeRef, true, 0);
        addNode(core, dataModel, 1, 18, 1, ContentModel.TYPE_CONTENT, new QName[] { ContentModel.ASPECT_TITLED,
                        ContentModel.ASPECT_LOCKABLE }, content05Properties, content05Content, null,
                new ChildAssociationRef[] { content05CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef,
                        folder00NodeRef, folder05NodeRef }, new String[] { "/" + baseFolderQName.toString()
                        + "/" + folder00QName.toString() + "/" + content05QName.toString() },
                content05NodeRef, true);

        HashMap<QName, PropertyValue> content06Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc06 = new MLTextPropertyValue();
        desc06.addValue(Locale.ENGLISH, "Six");
        desc06.addValue(Locale.US, "Six");
        content06Properties.put(ContentModel.PROP_DESCRIPTION, desc06);
        content06Properties.put(ContentModel.PROP_TITLE, desc06);
        content06Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                "text/plain", null));
        content06Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("FF.EE"));
        content06Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content06Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date06 = new Date(date05.getTime() + 1000);
        content06Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date06)));
        content06Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date06)));
        HashMap<QName, String> content06Content = new HashMap<QName, String>();
        content06Content.put(ContentModel.PROP_CONTENT, "Six zebra fig score score score score score score pad pad pad pad pad");
        NodeRef content06NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content06QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "FF.EE");
        ChildAssociationRef content06CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder06NodeRef,
                content06QName, content06NodeRef, true, 0);
        addNode(core,
                dataModel,
                1,
                19,
                1,
                ContentModel.TYPE_CONTENT,
                new QName[] { ContentModel.ASPECT_TITLED },
                content06Properties,
                content06Content,
                null,
                new ChildAssociationRef[] { content06CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef, folder05NodeRef,
                        folder06NodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                        + folder05QName.toString() + "/" + folder06QName.toString() + "/"
                        + content06QName.toString() }, content06NodeRef, true);

        HashMap<QName, PropertyValue> content07Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc07 = new MLTextPropertyValue();
        desc07.addValue(Locale.ENGLISH, "Seven");
        desc07.addValue(Locale.US, "Seven");
        content07Properties.put(ContentModel.PROP_DESCRIPTION, desc07);
        content07Properties.put(ContentModel.PROP_TITLE, desc07);
        content07Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                "text/plain", null));
        content07Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("GG*GG"));
        content07Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content07Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date07 = new Date(date06.getTime() + 1000);
        content07Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date07)));
        content07Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date07)));
        HashMap<QName, String> content07Content = new HashMap<QName, String>();
        content07Content.put(ContentModel.PROP_CONTENT, "Seven zebra grapefruit score score score score score pad pad pad pad pad pad");
        NodeRef content07NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content07QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "GG*GG");
        ChildAssociationRef content07CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder07NodeRef,
                content07QName, content07NodeRef, true, 0);
        addNode(core,
                dataModel,
                1,
                20,
                1,
                ContentModel.TYPE_CONTENT,
                new QName[] { ContentModel.ASPECT_TITLED },
                content07Properties,
                content07Content,
                null,
                new ChildAssociationRef[] { content07CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef, folder05NodeRef,
                        folder06NodeRef, folder07NodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                        + folder05QName.toString() + "/" + folder06QName.toString() + "/"
                        + folder07QName.toString() + "/" + content07QName.toString() }, content07NodeRef,
                true);

        HashMap<QName, PropertyValue> content08Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc08 = new MLTextPropertyValue();
        desc08.addValue(Locale.ENGLISH, "Eight");
        desc08.addValue(Locale.US, "Eight");
        content08Properties.put(ContentModel.PROP_DESCRIPTION, desc08);
        content08Properties.put(ContentModel.PROP_TITLE, desc08);
        content08Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                "text/plain", null));
        content08Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("HH?HH"));
        content08Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content08Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date08 = new Date(date07.getTime() + 1000);
        content08Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date08)));
        content08Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date08)));
        HashMap<QName, String> content08Content = new HashMap<QName, String>();
        content08Content.put(ContentModel.PROP_CONTENT, "Eight zebra jackfruit score score score score pad pad pad pad pad pad pad");
        NodeRef content08NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content08QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "HH?HH");
        ChildAssociationRef content08CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder08NodeRef,
                content08QName, content08NodeRef, true, 0);
        addNode(core,
                dataModel,
                1,
                21,
                1,
                ContentModel.TYPE_CONTENT,
                new QName[] { ContentModel.ASPECT_TITLED },
                content08Properties,
                content08Content,
                null,
                new ChildAssociationRef[] { content08CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef, folder05NodeRef,
                        folder06NodeRef, folder07NodeRef, folder08NodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                        + folder05QName.toString() + "/" + folder06QName.toString() + "/"
                        + folder07QName.toString() + "/" + folder08QName.toString() + "/"
                        + content08QName.toString() }, content08NodeRef, true);

        HashMap<QName, PropertyValue> content09Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc09 = new MLTextPropertyValue();
        desc09.addValue(Locale.ENGLISH, "Nine");
        desc09.addValue(Locale.US, "Nine");
        content09Properties.put(ContentModel.PROP_DESCRIPTION, desc09);
        content09Properties.put(ContentModel.PROP_TITLE, desc09);
        content09Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-9",
                "text/plain", null));
        content09Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("aa"));
        content09Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content09Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date09 = new Date(date08.getTime() + 1000);
        content09Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date09)));
        content09Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date09)));
        content09Properties.put(ContentModel.PROP_VERSION_LABEL, new StringPropertyValue("label"));
        HashMap<QName, String> content09Content = new HashMap<QName, String>();
        content09Content.put(ContentModel.PROP_CONTENT, "Nine zebra kiwi score score score pad pad pad pad pad pad pad pad");
        NodeRef content09NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content09QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "aa");
        ChildAssociationRef content09CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder09NodeRef,
                content09QName, content09NodeRef, true, 0);
        addNode(core,
                dataModel,
                1,
                22,
                1,
                ContentModel.TYPE_CONTENT,
                new QName[] { ContentModel.ASPECT_TITLED },
                content09Properties,
                content09Content,
                null,
                new ChildAssociationRef[] { content09CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef, folder05NodeRef,
                        folder06NodeRef, folder07NodeRef, folder08NodeRef, folder09NodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                        + folder05QName.toString() + "/" + folder06QName.toString() + "/"
                        + folder07QName.toString() + "/" + folder08QName.toString() + "/"
                        + folder09QName.toString() + "/" + content09QName.toString() }, content09NodeRef,
                true);

        HashMap<QName, PropertyValue> content10Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc10 = new MLTextPropertyValue();
        desc10.addValue(Locale.ENGLISH, "Ten");
        desc10.addValue(Locale.US, "Ten");
        content10Properties.put(ContentModel.PROP_DESCRIPTION, desc10);
        content10Properties.put(ContentModel.PROP_TITLE, desc10);
        content10Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-9",
                "text/plain", null));
        content10Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("aa-thumb"));
        content10Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("System"));
        content10Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("System"));
        Date date10 = new Date(date09.getTime() + 1000);
        content10Properties.put(ContentModel.PROP_CREATED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date10)));
        content10Properties.put(ContentModel.PROP_MODIFIED,
                new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date10)));
        content10Properties.put(ContentModel.PROP_VERSION_LABEL, new StringPropertyValue("label"));
        HashMap<QName, String> content10Content = new HashMap<QName, String>();
        content10Content.put(ContentModel.PROP_CONTENT, "Ten zebra kiwi thumb score pad pad pad pad pad pad pad pad pad");
        NodeRef content10NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content10QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "aa-thumb");
        ChildAssociationRef content10CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder09NodeRef,
                content10QName, content10NodeRef, true, 0);
        addNode(core,
                dataModel,
                1,
                23,
                1,
                ContentModel.TYPE_DICTIONARY_MODEL,
                new QName[] { ContentModel.ASPECT_TITLED },
                content10Properties,
                content10Content,
                null,
                new ChildAssociationRef[] { content10CAR },
                new NodeRef[] { baseFolderNodeRef, rootNodeRef, folder00NodeRef, folder05NodeRef,
                        folder06NodeRef, folder07NodeRef, folder08NodeRef, folder09NodeRef },
                new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                        + folder05QName.toString() + "/" + folder06QName.toString() + "/"
                        + folder07QName.toString() + "/" + folder08QName.toString() + "/"
                        + folder09QName.toString() + "/" + content10QName.toString() }, content10NodeRef,
                true);
    }
    /**
     * Builds a query request to solr server.
     * @param query
     * @return
     */
    protected SolrServletRequest qurySolr(String query)
    {
        return areq(params("rows", "20", "qt", "/cmis", "q",query),null);
    }
    /**
     * Builds an Xpath query to verify document count.
     * @param count
     * @return xpath query.
     */
    protected String expectedDocCount(int count)
    {
        return String.format("*[count(//doc)=%d]", count);
    }
    
    protected void addSortableNode(NodeRef folder00NodeRef,
            NodeRef rootNodeRef, NodeRef baseFolderNodeRef, Object baseFolderQName, Object folder00QName,
            Date date1, int position) throws IOException
    {
        HashMap<QName, PropertyValue> content00Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc00 = new MLTextPropertyValue();
        desc00.addValue(Locale.ENGLISH, "Test " + position);
        content00Properties.put(ContentModel.PROP_DESCRIPTION, desc00);
        content00Properties.put(ContentModel.PROP_TITLE, desc00);
        content00Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Test " + position));
        content00Properties.put(ContentModel.PROP_CREATED,
                    new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date1)));
    
        StringPropertyValue single = new StringPropertyValue(orderable[position]);
        content00Properties.put(singleTextUntokenised, single);
        content00Properties.put(singleTextTokenised, single);
        content00Properties.put(singleTextBoth, single);
        MultiPropertyValue multi = new MultiPropertyValue();
        multi.addValue(single);
        multi.addValue(new StringPropertyValue(orderable[position + 1]));
        content00Properties.put(multipleTextUntokenised, multi);
        content00Properties.put(multipleTextTokenised, multi);
        content00Properties.put(multipleTextBoth, multi);
        content00Properties.put(singleMLTextUntokenised, makeMLText(position));
        content00Properties.put(singleMLTextTokenised, makeMLText(position));
        content00Properties.put(singleMLTextBoth, makeMLText(position));
        content00Properties.put(multipleMLTextUntokenised, makeMLTextMVP(position));
        content00Properties.put(multipleMLTextTokenised, makeMLTextMVP(position));
        content00Properties.put(multipleMLTextBoth, makeMLTextMVP());
        StringPropertyValue one = new StringPropertyValue("" + (1.1 * position));
        StringPropertyValue two = new StringPropertyValue("" + (2.2 * position));
        MultiPropertyValue multiDec = new MultiPropertyValue();
        multiDec.addValue(one);
        multiDec.addValue(two);
        content00Properties.put(singleFloat, one);
        content00Properties.put(multipleFloat, multiDec);
        content00Properties.put(singleDouble, one);
        content00Properties.put(multipleDouble, multiDec);
        one = new StringPropertyValue("" + (1 * position));
        two = new StringPropertyValue("" + (2 * position));
        MultiPropertyValue multiInt = new MultiPropertyValue();
        multiInt.addValue(one);
        multiInt.addValue(two);
        content00Properties.put(singleInteger, one);
        content00Properties.put(multipleInteger, multiInt);
        content00Properties.put(singleLong, one);
        content00Properties.put(multipleLong, multiInt);
    
        GregorianCalendar cal = new GregorianCalendar();
        cal.setTime(date1);
        cal.add(Calendar.DAY_OF_MONTH, position);
    
        Date newdate1 = cal.getTime();
        cal.add(Calendar.DAY_OF_MONTH, -1);
        cal.add(Calendar.DAY_OF_MONTH, 2);
        Date date2 = cal.getTime();
        StringPropertyValue d1 = new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, newdate1));
        StringPropertyValue d2 = new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date2));
        MultiPropertyValue multiDate = new MultiPropertyValue();
        multiDate.addValue(d1);
        multiDate.addValue(d2);
        content00Properties.put(singleDate, d1);
        content00Properties.put(multipleDate, multiDate);
        content00Properties.put(singleDatetime, d1);
        content00Properties.put(multipleDatetime, multiDate);
    
        StringPropertyValue b = new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class,
                    position % 2 == 0 ? true : false));
        StringPropertyValue bTrue = new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, true));
        StringPropertyValue bFalse = new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, false));
        MultiPropertyValue multiBool = new MultiPropertyValue();
        multiBool.addValue(bTrue);
        multiBool.addValue(bFalse);
    
        content00Properties.put(singleBoolean, b);
        content00Properties.put(multipleBoolean, multiBool);
    
        NodeRef content00NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content00QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Test " + position);
        ChildAssociationRef content00CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder00NodeRef,
                content00QName, content00NodeRef, true, 0);
        addNode(h.getCore(),
                dataModel, 1, 1000 + position, 1, extendedContent, new QName[]{ContentModel.ASPECT_OWNABLE,
                        ContentModel.ASPECT_TITLED}, content00Properties, null, "andy",
                new ChildAssociationRef[]{content00CAR}, new NodeRef[]{baseFolderNodeRef, rootNodeRef,
                        folder00NodeRef}, new String[]{"/" + baseFolderQName.toString() + "/"
                        + folder00QName.toString() + "/" + content00QName.toString() }, content00NodeRef, true);
    }
    
    private static String[] orderable = new String[] { "zero loons", "one banana", "two apples", "three fruit",
            "four lemurs", "five rats", "six badgers", "seven cards", "eight cabbages", "nine zebras", "ten lemons" };
    /**
     * 
     * @param folder00NodeRef
     * @param rootNodeRef
     * @param baseFolderNodeRef
     * @param baseFolderQName
     * @param folder00QName
     * @param date1
     * @throws IOException
     */
    protected void addTypeSortTestData(NodeRef folder00NodeRef, NodeRef rootNodeRef, NodeRef baseFolderNodeRef, Object baseFolderQName, Object folder00QName, Date date1)
            throws IOException
    {
        addSortableNull(folder00NodeRef, rootNodeRef, baseFolderNodeRef, baseFolderQName,
                folder00QName, date1, "start", 0);
        for (int i = 0; i < 10; i++)
        {
            addSortableNode(folder00NodeRef, rootNodeRef, baseFolderNodeRef, baseFolderQName,
                        folder00QName, date1, i);
            if (i == 5)
            {
                addSortableNull(folder00NodeRef, rootNodeRef, baseFolderNodeRef, baseFolderQName,
                            folder00QName, date1, "mid", 1);
            }
        }

        addSortableNull(folder00NodeRef, rootNodeRef, baseFolderNodeRef, baseFolderQName,
                    folder00QName, date1, "end", 2);
    }
    /**
     * 
     * @param folder00NodeRef
     * @param rootNodeRef
     * @param baseFolderNodeRef
     * @param baseFolderQName
     * @param folder00QName
     * @param date1
     * @param id
     * @param offset
     * @throws IOException
     */
    private void addSortableNull(NodeRef folder00NodeRef,
            NodeRef rootNodeRef, NodeRef baseFolderNodeRef, Object baseFolderQName, Object folder00QName,
            Date date1, String id, int offset) throws IOException
    {
        HashMap<QName, PropertyValue> content00Properties = new HashMap<QName, PropertyValue>();
        MLTextPropertyValue desc00 = new MLTextPropertyValue();
        desc00.addValue(Locale.ENGLISH, "Test null");
        content00Properties.put(ContentModel.PROP_DESCRIPTION, desc00);
        content00Properties.put(ContentModel.PROP_TITLE, desc00);
        content00Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Test null"));
        content00Properties.put(ContentModel.PROP_CREATED,
                    new StringPropertyValue(DefaultTypeConverter.INSTANCE.convert(String.class, date1)));
    
        NodeRef content00NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName content00QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Test null");
        ChildAssociationRef content00CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS, folder00NodeRef,
                    content00QName, content00NodeRef, true, 0);
        addNode(h.getCore(), dataModel, 1, 200 + offset, 1, extendedContent, new QName[] { ContentModel.ASPECT_OWNABLE,
                    ContentModel.ASPECT_TITLED }, content00Properties, null, "andy",
                    new ChildAssociationRef[] { content00CAR }, new NodeRef[] { baseFolderNodeRef, rootNodeRef,
                                folder00NodeRef }, new String[] { "/" + baseFolderQName.toString() + "/"
                                + folder00QName.toString() + "/" + content00QName.toString() }, content00NodeRef, true);
    }
}
