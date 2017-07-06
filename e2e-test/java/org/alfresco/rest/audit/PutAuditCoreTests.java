package org.alfresco.rest.audit;

import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

public class PutAuditCoreTests extends AuditTest
{
	  @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
	  @TestRail(section = { TestGroup.REST_API,
	          TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify that the admin user can enable/disable sync application auditing")
	  public void enableDisableSyncApplicationAuditingAsAdminUser() throws Exception
	  {
		  //disable sync audit app    
		  syncRestAuditAppModel = getSyncRestAuditAppModel(dataUser.getAdminUser());
	      restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit().updateAuditApp(syncRestAuditAppModel, "isEnabled", "false");
	      
	      //check isEnabled=false
 	      syncRestAuditAppModel = getSyncRestAuditAppModel(dataUser.getAdminUser());
	      syncRestAuditAppModel.assertThat().field("isEnabled").is(false);
	      syncRestAuditAppModel.assertThat().field("name").is("Alfresco Sync Service");
	      syncRestAuditAppModel.assertThat().field("id").is("sync");
	     
		  //enable sync audit app
	      restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit().updateAuditApp(syncRestAuditAppModel, "isEnabled", "true");
	      
		  //check isEnabled=true
	      syncRestAuditAppModel = getSyncRestAuditAppModel(dataUser.getAdminUser());
	      syncRestAuditAppModel.assertThat().field("isEnabled").is(true);
	      syncRestAuditAppModel.assertThat().field("name").is("Alfresco Sync Service");
	      syncRestAuditAppModel.assertThat().field("id").is("sync");
		 
	  }
	  
	  @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
	  @TestRail(section = { TestGroup.REST_API,
	          TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify that the admin user can enable/disable tagging application auditing")
	  public void enableDisableTaggingApplicationAuditingAsAdminUser() throws Exception
	  {
		  //disable tagging audit app    
		  taggingRestAuditAppModel = getTaggingRestAuditAppModel(dataUser.getAdminUser());
	      restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit().updateAuditApp(taggingRestAuditAppModel, "isEnabled", "false");
	      
	      //check isEnabled=false
	      taggingRestAuditAppModel = getTaggingRestAuditAppModel(dataUser.getAdminUser());
	      taggingRestAuditAppModel.assertThat().field("isEnabled").is(false);
	      taggingRestAuditAppModel.assertThat().field("name").is("Alfresco Tagging Service");
	      taggingRestAuditAppModel.assertThat().field("id").is("tagging");
	      
		  //enable tagging audit app
	      restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit().updateAuditApp(taggingRestAuditAppModel, "isEnabled", "true");
	      
		  //check isEnabled=true
	      taggingRestAuditAppModel = getTaggingRestAuditAppModel(dataUser.getAdminUser());
	      taggingRestAuditAppModel.assertThat().field("isEnabled").is(true);
	      taggingRestAuditAppModel.assertThat().field("name").is("Alfresco Tagging Service");
	      taggingRestAuditAppModel.assertThat().field("id").is("tagging");
		 
	  }


	  @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
	  @TestRail(section = { TestGroup.REST_API,
	          TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify that the normal user can't enable/disable sync application auditing")
	  public void enableDisableSyncApplicationAuditingAsNormalUser() throws Exception
	  {
		  //disable sync audit app    
		  syncRestAuditAppModel = getSyncRestAuditAppModel(dataUser.getAdminUser());
	      restClient.authenticateUser(userModel).withCoreAPI().usingAudit().updateAuditApp(syncRestAuditAppModel, "isEnabled", "false");
	      
	      //permission denied
	      restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
	      
		  //enable sync audit app    
	      restClient.authenticateUser(userModel).withCoreAPI().usingAudit().updateAuditApp(syncRestAuditAppModel, "isEnabled", "true");
	      
	      //permission denied
	      restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
	  }
	  
	  @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
	  @TestRail(section = { TestGroup.REST_API,
	          TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify that the normal user can't enable/disable tagging application auditing")
	  public void enableDisableTaggingApplicationAuditingAsNormalUser() throws Exception
	  {
		  //disable tagging audit app    
		  taggingRestAuditAppModel = getTaggingRestAuditAppModel(dataUser.getAdminUser());
	      restClient.authenticateUser(userModel).withCoreAPI().usingAudit().updateAuditApp(taggingRestAuditAppModel, "isEnabled", "false");
	      
	      //permission denied
	      restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);
	      
		  //disable tagging audit app    
	      restClient.authenticateUser(userModel).withCoreAPI().usingAudit().updateAuditApp(taggingRestAuditAppModel, "isEnabled", "true");
	      
	      //permission denied
	      restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);

	  }

}
