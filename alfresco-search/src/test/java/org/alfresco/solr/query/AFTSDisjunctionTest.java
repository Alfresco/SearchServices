package org.alfresco.solr.query;

import static org.alfresco.solr.AlfrescoSolrUtils.addNode;
import static org.alfresco.solr.AlfrescoSolrUtils.addStoreRoot;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;

import java.util.HashMap;
import java.util.Locale;

import org.alfresco.model.ContentModel;
import org.alfresco.service.cmr.repository.ChildAssociationRef;
import org.alfresco.service.cmr.repository.NodeRef;
import org.alfresco.service.cmr.repository.StoreRef;
import org.alfresco.service.namespace.NamespaceService;
import org.alfresco.service.namespace.QName;
import org.alfresco.solr.AbstractAlfrescoSolrTests;
import org.alfresco.solr.AlfrescoSolrDataModel;
import org.alfresco.solr.client.PropertyValue;
import org.alfresco.solr.client.StringPropertyValue;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.SolrCore;
import org.apache.solr.request.SolrQueryRequest;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * https://issues.alfresco.com/jira/browse/SEARCH-556
 * @author msuzuki
 *
 */
public class AFTSDisjunctionTest extends AbstractAlfrescoSolrTests
{
    @BeforeClass
    public static void beforeClass() throws Exception
    {
        initAlfrescoCore("schema.xml");
        Thread.sleep(1000);
        // Root
        SolrCore core = h.getCore();
        AlfrescoSolrDataModel dataModel = AlfrescoSolrDataModel.getInstance();
        dataModel.getNamespaceDAO().removePrefix("");
        dataModel.setCMDefaultUri();

        NodeRef rootNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        addStoreRoot(core, dataModel, rootNodeRef, 1, 1, 1, 1);
        
        NodeRef baseFolderNodeRef = new NodeRef(new StoreRef("workspace", "SpacesStore"), createGUID());
        QName baseFolderQName = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "baseFolder");
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

        // 1
        String guid = createGUID();
        NodeRef nodeRef1 = new NodeRef(new StoreRef("workspace", "SpacesStore"), guid);
        QName name1 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "test1");
        HashMap<QName, PropertyValue> properties1 = new HashMap<QName, PropertyValue>();
        properties1.put(ContentModel.PROP_NAME, new StringPropertyValue("test1"));
        addNode(core, dataModel, 1, 2, 1, ContentModel.TYPE_CONTENT, null, properties1, null, "michael",
                null, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + name1.toString()}, nodeRef1, true);
        System.out.println("create node test1 " + guid);
        
        guid = createGUID();
        NodeRef nodeRef2 = new NodeRef(new StoreRef("workspace", "SpacesStore"), guid);
        QName name2 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "test2");
        HashMap<QName, PropertyValue> properties2 = new HashMap<QName, PropertyValue>();
        properties2.put(ContentModel.PROP_NAME, new StringPropertyValue("test2"));
        addNode(core, dataModel, 1, 3, 1, ContentModel.TYPE_CONTENT, null, properties2, null, "michael",
                null, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + name2.toString()}, nodeRef2, true);
        System.out.println("create node test2 " + guid);
        
        guid = createGUID();
        NodeRef nodeRef3 = new NodeRef(new StoreRef("workspace", "SpacesStore"), guid);
        QName name3 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, "test3");
        HashMap<QName, PropertyValue> properties3 = new HashMap<QName, PropertyValue>();
        properties3.put(ContentModel.PROP_NAME, new StringPropertyValue("test3"));
        addNode(core, dataModel, 1, 4, 1, ContentModel.TYPE_CONTENT, null, properties3, null, "michael",
                null, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + name3.toString()}, nodeRef3, true);
        System.out.println("create node test3 " + guid);
    }
    @Test
    public void testDisjunction() throws Exception
    {
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "*"), null), "*[count(//doc)=4]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:test*"), null), "*[count(//doc)=3]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:test* AND NOT id:1"), null), "*[count(//doc)=3]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:test* AND NOT (id:1)"), null), "*[count(//doc)=3]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:name:test* AND (NOT id:1)"), null), "*[count(//doc)=3]");
    }

}
