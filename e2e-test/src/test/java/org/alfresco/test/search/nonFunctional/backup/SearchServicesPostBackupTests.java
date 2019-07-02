package org.alfresco.test.search.nonFunctional.backup;

import org.testng.annotations.Test;

/**
 * 
 * Documentation: http://docs.alfresco.com/6.0/tasks/solr6-backup.html
 * 
 * @author Paul Brodner
 *
 */
public class SearchServicesPostBackupTests extends AbstractBackupE2ETest
{
	@Test
	public void testIFBackupDataExist() throws Exception {
		cmisWrapper.authenticateUser(dataSite.getAdminUser())
				.usingSite(testSite)
					.setLastContentModel(folder);
		cmisWrapper.assertThat().existsInRepo();
	}

}
