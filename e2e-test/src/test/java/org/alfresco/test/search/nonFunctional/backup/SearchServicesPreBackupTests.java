package org.alfresco.test.search.nonFunctional.backup;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.StringContains.containsString;

import org.alfresco.rest.model.RestHtmlResponse;
import org.testng.Assert;
import org.testng.annotations.Test;

/**
 * 
 * Documentation: http://docs.alfresco.com/6.0/tasks/solr6-backup.html
 * 
 * @author Paul Brodner
 *
 */
public class SearchServicesPreBackupTests extends AbstractBackupE2ETest
{
	/**
	 * {"responseHeader":{"status":0,"QTime":1},"exception":"org.apache.solr.common.SolrException:org.apache.solr.common.SolrException:
	 * Directory does not exist:
	 * file:///thisDoesntExist1/snapshot.20190212161435995","status":"OK"}
	 */
	@Test
	public void testIFSolrThrowsExceptioIfDestinationFolderDoesntExist() throws Exception {
		RestHtmlResponse htmlResponse = executeSolrBackupRequest("alfresco", "/thisDoesntExist", 1);

		String exception = htmlResponse.getBody().jsonPath().get("exception").toString();

		assertThat(exception, containsString(
				"org.apache.solr.common.SolrException:org.apache.solr.common.SolrException:  Directory does not exist: file:///thisDoesntExist"));

	}

	@Test
	public void createTestSiteForBackup() throws Exception {
		if (!dataSite.isSiteCreated(testSite))
			testSite = dataSite.usingAdmin().createSite(testSite);
		
		/**
		 * Site: siteForBackupTesting
		 * 			> documentLibrary
		 * 				| siteForBackupTesting
		 *              |- fileBackedUp.txt
		 */
		cmisWrapper.authenticateUser(dataSite.getAdminUser())
					.usingSite(testSite).createFolder(folder)
						.usingResource(folder)
						.createFile(file).and().assertThat()
						.existsInRepo();

	}
	
	 

	/**
	 * In case of success: {"responseHeader":{"status":0,"QTime":3},"status":"OK"}
	 */
	@Test(enabled=false)
	public void save1AlfrescoSnaphotToExistingBackupFolder() throws Exception {
		RestHtmlResponse htmlResponse = executeSolrBackupRequest("alfresco", "/backup/solr/alfresco", 1);

		String status = htmlResponse.getBody().jsonPath().get("status").toString();

		/*
		 * in case of successfull backup we have status OK, no exception AND physical
		 * file saved under {solrDockerHostPath}/alfresco as >
		 * {solrDockerHostPath}/alfresco/snapshot.20190212161552891 (not empty with a
		 * lot of lucene files)
		 */

		Assert.assertNull(htmlResponse.getBody().jsonPath().get("exception"));
		Assert.assertEquals(status, "OK");

		assertFileExistInLocalBackupFolder("solr/alfresco", "snapshot.", 1);

		// execute another request and see that files inside solr/alfresco backup folder
		// are 1
		htmlResponse = executeSolrBackupRequest("alfresco", "/backup/alfresco", 1);
		assertFileExistInLocalBackupFolder("solr/alfresco", "snapshot.", 1);

	}

}
