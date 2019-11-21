package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.rest.model.RestNodeChildAssociationModel;
import org.alfresco.test.search.functional.AbstractE2EFunctionalTest;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * Test class tests content in the secondary parent is found too
 * Created for Search-1313
 * 
 * @author Meenal Bhave
 */
public class SearchSecondaryAssociationTests extends AbstractE2EFunctionalTest
{
    private FolderModel testFolder1, testFolder2;
    private FileModel file1;
    
    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {        
        // Folders
        testFolder1 = new FolderModel("folder1");        
        testFolder2 = new FolderModel("folder2");

        // File(s)
        file1 = new FileModel("file1.txt");
        file1.setContent("content file 1");      

        // Create folder1
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(testFolder1, "cmis:folder", new CustomObjectTypeProperties());

        // Create file1
        dataContent.usingUser(testUser).usingResource(testFolder1).createCustomContent(file1, "cmis:document", new CustomObjectTypeProperties());

        // Create folder2
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(testFolder2, "cmis:folder", new CustomObjectTypeProperties());

        // Create Secondary association
        RestNodeChildAssociationModel childAssoc1 = new RestNodeChildAssociationModel(file1.getNodeRefWithoutVersion(), "cm:contains");
        String secondaryChildrenBody = "[" + childAssoc1.toJson() + "]";
        
        restClient.authenticateUser(testUser).withCoreAPI().usingResource(testFolder2).createSecondaryChildren(secondaryChildrenBody);

        // wait for solr index
        waitForMetadataIndexing(file1.getName(), true);
    }
    
    @Test(priority = 1)
    public void testPathForSecondaryAssociation()
    {
        String queryPathFolder1 = "PATH:\"/app:company_home/st:sites/cm:" + testSite.getTitle() +
                "/cm:documentLibrary/cm:" + testFolder1.getName() + "/cm:" + file1.getName() + "\"";

        // Test if file can be found in Primary Parent
        boolean found = isContentInSearchResults(queryPathFolder1, file1.getName(), true);
        Assert.assertTrue(found, "File found using Primary Parent Path");

        String queryPathFolder2 = "PATH:\"/app:company_home/st:sites/cm:" + testSite.getTitle() +
                "/cm:documentLibrary/cm:" + testFolder2.getName() + "/cm:" + file1.getName() + "\"";

        // Test if file can be found in Secondary Parent
        found = isContentInSearchResults(queryPathFolder2, file1.getName(), true);
        Assert.assertTrue(found, "File found using Secondary Parent Path");
    }
}
