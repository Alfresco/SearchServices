package org.alfresco.test.search.functional.searchServices.cmis;

import org.alfresco.utility.Utility;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.data.provider.XMLDataConfig;
import org.alfresco.utility.data.provider.XMLTestDataProvider;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.QueryModel;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SolrSearchByIdTests extends AbstractCmisE2ETest
{
    private FolderModel tasFolder1, tasFolder2, stdFolder3, tasSubFolder1, stdSubFolder2;
    private FileModel tasSubFile1, tasSubFile2, tasSubFile3, stdSubFile4, tasFile1, stdFile2;
    private String siteDoclibNodeRef;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {        
        dataContent.usingAdmin().deployContentModel("model/tas-model.xml");
        
        // Folders
        tasFolder1 = new FolderModel("tas-folder1");        
        tasFolder2 = new FolderModel("tas-folder2");
        stdFolder3 = new FolderModel("standard-folder3");
        
        // SubFolders
        tasSubFolder1 = new FolderModel("tas-sub-folder1");
        stdSubFolder2 = new FolderModel("standard-sub-folder2");
        
        // Files
        tasSubFile1 = new FileModel("tas-sub-file1.txt");
        tasSubFile1.setContent("content sub-file 1");

        tasSubFile2 = new FileModel("tas-sub-file2.txt");
        tasSubFile2.setContent("content sub-file 2");

        tasSubFile3 = new FileModel("tas-sub-file3.txt");
        tasSubFile3.setContent("content sub-file 3");

        stdSubFile4 = new FileModel("standard-sub-file4.txt");
        stdSubFile4.setContent("content sub-file 4");

        tasFile1 = new FileModel("tas-file1.txt");
        tasFile1.setContent("file 1 content");

        stdFile2 = new FileModel("standard-file2.txt");
        stdFile2.setContent("file 2 content");

        // Site > Doclib: Create Content and Add Aspects and Properties
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(tasFolder1, "F:tas:folder", new CustomObjectTypeProperties());

        cmisApi.authenticateUser(testUser);
        siteDoclibNodeRef = cmisApi.withCMISUtil().getObjectId(String.format("/Sites/%s/documentLibrary",testSite.getId()));
        
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(tasFolder2, "F:tas:folder", new CustomObjectTypeProperties());

        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(stdFolder3, "cmis:folder", new CustomObjectTypeProperties());

        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(tasFile1, "D:tas:document", new CustomObjectTypeProperties());

        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(stdFile2, "cmis:document", new CustomObjectTypeProperties());

        // tasFolder1 > Folders
        dataContent.usingUser(testUser).usingResource(tasFolder1).createCustomContent(tasSubFolder1, "F:tas:folder", new CustomObjectTypeProperties()
                .addProperty("tas:TextPropertyF", "text sub-folder-1"));

        dataContent.usingUser(testUser).usingResource(tasFolder1).createCustomContent(stdSubFolder2, "cmis:folder", new CustomObjectTypeProperties());

        // tasFolder1 > Files
        dataContent.usingUser(testUser).usingResource(tasFolder1).createCustomContent(tasSubFile1, "D:tas:document", new CustomObjectTypeProperties()
                .addProperty("tas:TextPropertyC", "text sub-file-1"));

        dataContent.usingUser(testUser).usingResource(tasFolder1).createCustomContent(tasSubFile2, "D:tas:document", new CustomObjectTypeProperties()
                .addProperty("tas:TextPropertyC", "text sub-file-2"));

        dataContent.usingUser(testUser).usingResource(tasFolder1).createCustomContent(tasSubFile3, "D:tas:document", new CustomObjectTypeProperties()
                .addProperty("tas:TextPropertyC", "text sub-file-3"));

        dataContent.usingUser(testUser).usingResource(tasFolder1).createCustomContent(stdSubFile4, "cmis:document", new CustomObjectTypeProperties());

        // wait for solr index
        Utility.waitToLoopTime(getSolrWaitTimeInSeconds());
    }
    
    @Test(dataProviderClass = XMLTestDataProvider.class, dataProvider = "getQueriesData")
    @XMLDataConfig(file = "src/test/resources/testdata/search-by-id.xml")
    public void executeSearchByAspect(QueryModel query) throws Exception
    {
        String currentQuery = query.getValue()
                .replace("NODE_REF[siteId]", siteDoclibNodeRef)
                .replace("NODE_REF[d1]", tasSubFile1.getNodeRefWithoutVersion())
                .replace("NODE_REF[d2]", tasSubFile2.getNodeRefWithoutVersion())
                .replace("NODE_REF[f1]", tasFolder1.getNodeRef())
                .replace("NODE_REF[f1-1]", tasSubFolder1.getNodeRef());

        cmisApi.authenticateUser(testUser).withQuery(currentQuery).assertResultsCount().equals(query.getResults());
    }
}
