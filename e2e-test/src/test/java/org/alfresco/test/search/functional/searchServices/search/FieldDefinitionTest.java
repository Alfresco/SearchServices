/*
 * #%L
 * Alfresco Search Services E2E Test
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.alfresco.test.search.functional.searchServices.search;

import org.alfresco.rest.search.SearchResponse;
import org.alfresco.utility.data.CustomObjectTypeProperties;
import org.alfresco.utility.model.FileModel;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * A test for fields defined in the solr schema 
 * Based on https://issues.alfresco.com/jira/browse/SEARCH-2139
 */
public class FieldDefinitionTest extends AbstractSearchServicesE2ETest {
	
	private FileModel File1, File2;
		
	@BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {        
        dataContent.usingAdmin().deployContentModel("model/search-2139-model.xml");
        
        File1 = new FileModel("standard-file1.txt");
        
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(File1, "cmis:document", new CustomObjectTypeProperties());

        cmisApi.authenticateUser(testUser).usingResource(File1).addSecondaryTypes("P:allfieldtypes:text")
                .updateProperty("allfieldtypes:mltextLOVPartial", "file1")
                .updateProperty("allfieldtypes:textPatternUnique", "file2")
		        .updateProperty("allfieldtypes:mltextFree", "file3")
		        .updateProperty("allfieldtypes:textLOVPartial", "file4");
        
        File2 = new FileModel("standard-file2.txt");
        
        dataContent.usingUser(testUser).usingSite(testSite).createCustomContent(File2, "cmis:document", new CustomObjectTypeProperties());

        cmisApi.authenticateUser(testUser).usingResource(File2).addSecondaryTypes("P:allfieldtypes:text")
                .updateProperty("allfieldtypes:textFree", "text field definition test")
                .updateProperty("allfieldtypes:textPatternMany", "mltext field definition test")
		        .updateProperty("allfieldtypes:textLOVWhole", "text field not tokenised")
		        .updateProperty("allfieldtypes:mltextLOVWhole", "mltext field not tokenised");
        
        waitForMetadataIndexing(File1.getName(), true);
        waitForMetadataIndexing(File2.getName(), true);
    }
	
	// A test to test the text field in the solr schema, using a single word 
	@Test(priority = 1)
    public void testTextField()
    {
		SearchResponse response = queryAsUser(testUser, "allfieldtypes_textPatternUnique:file2");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);
        
        response = queryAsUser(testUser, "allfieldtypes_textLOVPartial:file4");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);
    }
	
	// A test to test the mltext field in the solr schema, using a single word 
	@Test(priority = 2)
    public void testmlTextField()
    {
		SearchResponse response = queryAsUser(testUser, "allfieldtypes_mltextLOVPartial:file1");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);
        
        response = queryAsUser(testUser, "allfieldtypes_mltextFree:file3");
        restClient.assertStatusCodeIs(HttpStatus.OK);
        Assert.assertEquals(response.getPagination().getCount(), 1);
    }

	// A test to test the text field in the solr schema, where the field hasn't been defined
	@Test(priority = 3)
	public void testTextFieldNotDefined()
	{
		SearchResponse response = queryAsUser(testUser, "allfieldtypes_textPatternMany:file1");
		restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 0);
	    
	    response = queryAsUser(testUser, "allfieldtypes:textNone:file2");
		restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 0);
	}
	
	// A test to test the mltext field in the solr schema, where the field hasn't been defined
	@Test(priority = 4)
	public void testmlTextFieldNotDefined()
	{
		SearchResponse response = queryAsUser(testUser, "allfieldtypes_mltextNone:file1");
		restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 0);
	    
	    response = queryAsUser(testUser, "allfieldtypes_mltextPatternUnique:file2");
		restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 0);
	}
	
	// A test to test the tokenised text field in the solr schema
	@Test(priority = 5)
	public void testTextFieldTokenised()
	{
		SearchResponse response = queryAsUser(testUser, "allfieldtypes_textFree:\"text field definition test\"");
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 1);
	    
	    response = queryAsUser(testUser, "allfieldtypes_textFree:text");
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 1);
	    
	    response = queryAsUser(testUser, "allfieldtypes_textFree:\"definition test\"");
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 1);
	    
	    response = queryAsUser(testUser, "allfieldtypes_textPatternMany:definition");
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(response.getPagination().getCount(), 1);
	}
	
	// A test to test a non tokenised text field in the solr schema
	@Test(priority = 6)
	public void testTextFieldNotTokenised()
	{
		SearchResponse response = queryAsUser(testUser, "allfieldtypes_textLOVWhole:\"text field not tokenised\"");
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 1);
	    
	    response = queryAsUser(testUser, "allfieldtypes_textLOVWhole:text");
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 0);
	    
	    response = queryAsUser(testUser, "allfieldtypes_textLOVWhole:\"field not\"");
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    Assert.assertEquals(response.getPagination().getCount(), 0);
	}
	
	// A test to test the tokenised mltext field in the solr schema
	@Test(priority = 7)
	public void testmlTextFieldTokenised()
	{
		SearchResponse response = queryAsUser(testUser, "allfieldtypes_textPatternMany:\"mltext field definition test\"");
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(response.getPagination().getCount(), 1);
		
		response = queryAsUser(testUser, "allfieldtypes_textPatternMany:mltext");
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(response.getPagination().getCount(), 1);
		
		response = queryAsUser(testUser, "allfieldtypes_textPatternMany:\"field definition\"");
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(response.getPagination().getCount(), 1);
		
		response = queryAsUser(testUser, "allfieldtypes_textPatternMany:field");
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(response.getPagination().getCount(), 1);
	}
	
	// A test to test the non tokenised mltext field in the solr schema
	@Test(priority = 7)
	public void testmlTextFieldNotTokenised()
	{
		SearchResponse response = queryAsUser(testUser, "allfieldtypes_mltextLOVWhole:\"mltext field not tokenised\"");
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(response.getPagination().getCount(), 1);
		
		response = queryAsUser(testUser, "allfieldtypes_mltextLOVWhole:mltext");
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(response.getPagination().getCount(), 0);
		
		response = queryAsUser(testUser, "allfieldtypes_mltextLOVWhole:\"not tokenised\"");
		restClient.assertStatusCodeIs(HttpStatus.OK);
		Assert.assertEquals(response.getPagination().getCount(), 0);
	}
}
