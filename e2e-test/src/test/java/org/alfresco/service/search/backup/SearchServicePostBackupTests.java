package org.alfresco.service.search.backup;

import org.testng.annotations.Test;

/**
 * 
 * Documentation: http://docs.alfresco.com/6.0/tasks/solr6-backup.html
 * 
 * @author Paul Brodner
 *
 */
public class SearchServicePostBackupTests extends AbstractBackupTest {
	
	
	@Test
	public void testIFBackupDataExist() throws Exception {
		cmisWrapper.authenticateUser(dataSite.getAdminUser())
				.usingSite(testSite)
					.setLastContentModel(folder);
		cmisWrapper.assertThat().existsInRepo();
	}

}
