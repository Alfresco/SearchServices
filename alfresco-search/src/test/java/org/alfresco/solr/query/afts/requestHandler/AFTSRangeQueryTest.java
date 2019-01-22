package org.alfresco.solr.query.afts.requestHandler;

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
import org.apache.solr.core.SolrCore;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Name;
import java.util.HashMap;

import static org.alfresco.solr.AlfrescoSolrUtils.addNode;
import static org.alfresco.solr.AlfrescoSolrUtils.addStoreRoot;
import static org.alfresco.solr.AlfrescoSolrUtils.createGUID;

/**
 * https://issues.alfresco.com/jira/browse/SEARCH-1359
 * @author elia
 *
 */
public class AFTSRangeQueryTest extends AbstractAlfrescoSolrTests {

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

        QName[] aspects = {ContentModel.ASPECT_AUDITABLE};


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

        String stringName1 = "test1";
        String rating1 = "someTest";
        String creator1 = "Mario";

        QName name1 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, stringName1);
        HashMap<QName, PropertyValue> properties1 = new HashMap<QName, PropertyValue>();
        properties1.put(ContentModel.PROP_NAME, new StringPropertyValue(stringName1));
        properties1.put(ContentModel.PROP_RATING_SCHEME, new StringPropertyValue(rating1));

        properties1.put(ContentModel.PROP_CREATOR, new StringPropertyValue(creator1));
        addNode(core, dataModel, 1, 2, 1, ContentModel.TYPE_RATING, aspects, properties1, null, "elia",
                null, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + name1.toString()}, nodeRef1, true);
        System.out.println("create node test1 " + guid);
        guid = createGUID();
        String stringName2 = "test2";
        String rating2 = "test";
        String creator2 = "Luigi";

        NodeRef nodeRef2 = new NodeRef(new StoreRef("workspace", "SpacesStore"), guid);
        QName name2 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, stringName2);
        HashMap<QName, PropertyValue> properties2 = new HashMap<QName, PropertyValue>();
        properties2.put(ContentModel.PROP_NAME, new StringPropertyValue(stringName2));
        properties2.put(ContentModel.PROP_RATING_SCHEME, new StringPropertyValue(rating2));
        properties2.put(ContentModel.PROP_CREATOR, new StringPropertyValue(creator2));
        addNode(core, dataModel, 1, 3, 1, ContentModel.TYPE_CONTENT, aspects, properties2, null, "elia",
                null, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + name2.toString()}, nodeRef2, true);
        System.out.println("create node test2 " + guid);

        guid = createGUID();
        String stringName3 = "test3";
        String rating3 = "firstString";
        String creator3 = "Wario";

        NodeRef nodeRef3 = new NodeRef(new StoreRef("workspace", "SpacesStore"), guid);
        QName name3 = QName.createQName(NamespaceService.CONTENT_MODEL_1_0_URI, stringName3);
        HashMap<QName, PropertyValue> properties3 = new HashMap<QName, PropertyValue>();
        properties3.put(ContentModel.PROP_NAME, new StringPropertyValue(stringName3));
        properties3.put(ContentModel.PROP_RATING_SCHEME, new StringPropertyValue(rating3));
        properties3.put(ContentModel.PROP_CREATOR, new StringPropertyValue(creator3));
        addNode(core, dataModel, 1, 4, 1, ContentModel.TYPE_CONTENT, aspects, properties3, null, "elia",
                null, new NodeRef[]{rootNodeRef}, new String[]{"/"
                        + name3.toString()}, nodeRef3, true);
        System.out.println("create node test3 " + guid);
    }

    @Test
    public void testRangeQueriesNonTokenised() throws Exception
    {
        // test on cm:ratingScheme that is non tokenised.
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:ratingScheme:[n TO *]"), null), "*[count(//doc)=2]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:ratingScheme:[* TO n]"), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:ratingScheme:[first TO *]"), null), "*[count(//doc)=3]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:ratingScheme:[firstt TO *]"), null), "*[count(//doc)=2]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:ratingScheme:[* TO someTest]"), null), "*[count(//doc)=2]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:ratingScheme:[kk TO someTest]"), null), "*[count(//doc)=1]");
    }

    @Test
    public void testRangeQueriesTokenised() throws Exception
    {
        // test on cm:creator that is tokenised.
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:creator:[Mario TO *]"), null), "*[count(//doc)=2]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:creator:[Lu TO *]"), null), "*[count(//doc)=3]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:creator:[Luigi2 TO Mario]"), null), "*[count(//doc)=1]");
        assertQ(areq(params("rows", "20", "qt", "/afts", "q", "cm:creator:[* TO Mario]"), null), "*[count(//doc)=2]");

    }
}
