package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FolderModel;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A test for fields defined in the solr schema 
 * Based on https://issues.alfresco.com/jira/browse/SEARCH-2139
 */
public class FieldDefinitionTest extends AbstractSearchServicesE2ETest {
	
	private FolderModel Folder1;
	private FileModel File1;
		
	@BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {        
        dataContent.usingAdmin().deployContentModel("model/search-2139-model.xml");
        
        Folder1 = new FolderModel("folder1");
        
        File1 = new FileModel("standard-file1.txt");
        File1.setContent("file1");
        
    //    dataContent.usingUser(testUser).usingResource(Folder1).createCustomContent(File1, "D:text:document", new CustomObjectTypeProperties()
    //            .addProperty("allfieldtypes:mltextLOVPartial", "text file-1"));
        
     //   dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(File1, "D:allfieldtypes:document", new CustomObjectTypeProperties()
       //         .addProperty("allfieldtypes:mltextLOVPartial", "text folder-2"));
        
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(File1, "allfieldtypes:document", new CustomObjectTypeProperties());
    }
	
	@Test(priority = 1)
    public void testTextField()
    {
		SearchResponse response = queryAsUser(testUser, "allfieldtypes_mltextLOVPartial:text file-1");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);
    }
}
