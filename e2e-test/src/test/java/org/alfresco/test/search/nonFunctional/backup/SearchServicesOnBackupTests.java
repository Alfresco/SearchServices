package org.alfresco.test.search.nonFunctional.backup;

import org.testng.annotations.Test;

/**
 * 
 * Documentation: http://docs.alfresco.com/6.0/tasks/solr6-backup.html
 * 
 * @author Paul Brodner
 *
 */
public class SearchServicesOnBackupTests extends AbstractBackupE2ETest
{
	@Test
	public void deleteBackupFolder() throws Exception {
		
		/*
		 * Site: siteForBackupTesting
		 * 			> documentLibrary
		 * 				| siteForBackupTesting
		 *              |- fileBackedUp.txt
		 */
		cmisWrapper.authenticateUser(dataSite.getAdminUser())		
				   .usingSite(testSite).setLastContentModel(folder);
		cmisWrapper.deleteFolderTree().and().assertThat().doesNotExistInRepo();
	}
}
