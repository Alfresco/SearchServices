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

import static org.alfresco.solr.AlfrescoSolrUtils.addAcl;
import static org.alfresco.solr.AlfrescoSolrUtils.addNode;
import static org.alfresco.solr.AlfrescoSolrUtils.addStoreRoot;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;

import java.util.Date;
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
import org.alfresco.solr.client.ContentPropertyValue;
import org.alfresco.solr.client.MLTextPropertyValue;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.apache.solr.update.CommitUpdateCommand;
import org.junit.BeforeClass;
/**
 * Load data required by the authentication tests.
 * @author Michael Suzuki
 *
 */
public class AuthDataLoad extends AbstractAlfrescoSolrTests
{
    static int count = 100;
    static long maxReader = 1000;
    
    @BeforeClass
    public static void setup() throws Exception
    {
        //Start test haness
        initAlfrescoCore("schema.xml");
        // Root

        NodeRef rootNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        addStoreRoot(h.getCore(), dataModel, rootNodeRef, 1, 1, 1, 1);
        //        rsp.add("StoreRootNode", 1);

        // Base

        HashMap<QName, PropertyValue> baseFolderProperties = new HashMap<QName, PropertyValue>();
        baseFolderProperties.put(ContentModel.PROP_NAME, new StringPropertyValue("Base Folder"));
        NodeRef baseFolderNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName baseFolderQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "baseFolder");
        ChildAssociationRef n01CAR = new ChildAssociationRef(ContentModel.ASSOC_CHILDREN, rootNodeRef,
                    baseFolderQName, baseFolderNodeRef, true, 0);
        addNode(h.getCore(), dataModel, 1, 2, 1, ContentModel.TYPE_FOLDER, null, baseFolderProperties, null, "andy",
                    new ChildAssociationRef[] { n01CAR }, new NodeRef[] { rootNodeRef }, new String[] { "/"
                                + baseFolderQName.toString() }, baseFolderNodeRef, true);

        // Folders

        HashMap<QName, PropertyValue> folder00Properties = new HashMap<QName, PropertyValue>();
        folder00Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Folder 0"));
        NodeRef folder00NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName folder00QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Folder 0");
        ChildAssociationRef folder00CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS,
                    baseFolderNodeRef, folder00QName, folder00NodeRef, true, 0);
        addNode(h.getCore(), dataModel, 1, 3, 1, ContentModel.TYPE_FOLDER, null, folder00Properties, null, "andy",
                    new ChildAssociationRef[] { folder00CAR },
                    new NodeRef[] { baseFolderNodeRef, rootNodeRef },
                    new String[] { "/" + baseFolderQName.toString() + "/" + folder00QName.toString() },
                    folder00NodeRef, true);

        for (long i = 0; i < count; i++)
        {
            addAcl(h.getCore(), dataModel, 10 + (int) i, 10 + (int) i, (int) (i % maxReader), (int) maxReader);

            HashMap<QName, PropertyValue> content00Properties = new HashMap<QName, PropertyValue>();
            MLTextPropertyValue desc00 = new MLTextPropertyValue();
            desc00.addValue(Locale.ENGLISH, "Doc " + i);
            desc00.addValue(Locale.US, "Doc " + i);
            content00Properties.put(ContentModel.PROP_DESCRIPTION, desc00);
            content00Properties.put(ContentModel.PROP_TITLE, desc00);
            content00Properties.put(ContentModel.PROP_CONTENT, new ContentPropertyValue(Locale.UK, 0l, "UTF-8",
                        "text/plain", null));
            content00Properties.put(ContentModel.PROP_NAME, new StringPropertyValue("Doc " + i));
            content00Properties.put(ContentModel.PROP_CREATOR, new StringPropertyValue("Test"));
            content00Properties.put(ContentModel.PROP_MODIFIER, new StringPropertyValue("Test"));
            content00Properties.put(ContentModel.PROP_VERSION_LABEL, new StringPropertyValue("1.0"));
            content00Properties.put(ContentModel.PROP_OWNER, new StringPropertyValue("Test"));
            Date date00 = new Date();
            content00Properties.put(ContentModel.PROP_CREATED, new StringPropertyValue(
                        DefaultTypeConverter.INSTANCE.convert(String.class, date00)));
            content00Properties.put(ContentModel.PROP_MODIFIED, new StringPropertyValue(
                        DefaultTypeConverter.INSTANCE.convert(String.class, date00)));
            HashMap<QName, String> content00Content = new HashMap<QName, String>();
            content00Content.put(ContentModel.PROP_CONTENT, "Test doc number " + i);
            NodeRef content00NodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
            QName content00QName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "Doc-" + i);
            ChildAssociationRef content00CAR = new ChildAssociationRef(ContentModel.ASSOC_CONTAINS,
                        folder00NodeRef, content00QName, content00NodeRef, true, 0);
            addNode(h.getCore(), dataModel, 1, 10 + (int) i, 10 + (int) i, ContentModel.TYPE_CONTENT, new QName[] {
                        ContentModel.ASPECT_OWNABLE, ContentModel.ASPECT_TITLED }, content00Properties,
                        content00Content, "andy", new ChildAssociationRef[] { content00CAR }, new NodeRef[] {
                                    baseFolderNodeRef, rootNodeRef, folder00NodeRef }, new String[] { "/"
                                    + baseFolderQName.toString() + "/" + folder00QName.toString() + "/"
                                    + content00QName.toString() }, content00NodeRef, false);
        }
        h.getCore().getUpdateHandler().commit(new CommitUpdateCommand(req(), false));

    }
}
