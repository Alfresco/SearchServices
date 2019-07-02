package org.alfresco.test.search.nonFunctional.backup;

import java.io.File;
import java.nio.file.Paths;

import org.alfresco.cmis.CmisWrapper;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestHtmlResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataSite;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.network.ServerHealth;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.asserts.SoftAssert;

import com.jayway.restassured.RestAssured;

@Configuration
@ContextConfiguration("classpath:alfresco-search-e2e-context.xml")
public abstract class AbstractBackupE2ETest extends AbstractTestNGSpringContextTests
{
	protected SiteModel testSite = new SiteModel("siteForBackupTesting");
	protected FolderModel folder = new FolderModel("folderBackedUp");
	protected FileModel file = new FileModel("fileBackedUp.txt", FileType.TEXT_PLAIN, "uber important file");
	
	@Autowired
	protected ServerHealth serverHealth;
	
	@Autowired
    protected DataSite dataSite;
	
	@Autowired
    protected CmisWrapper cmisWrapper;

	@Autowired
	protected RestWrapper restWrapper;

	@Value("${solr.port}")
	int solrPort;


	@BeforeClass(alwaysRun = true)
	protected void setupSolrRequest() throws Exception {
		serverHealth.assertServerIsOnline();

		RestAssured.basePath = "solr";
		RestAssured.port = solrPort;
		restWrapper.configureRequestSpec().setPort(solrPort);
		restWrapper.configureRequestSpec().setBasePath(RestAssured.basePath);
	}
	
	 
	/**
	 * Prepare a GET "backup" command request to localhost:{solrPort}/solr
	 * 
	 * @param core : alfresco or archive
	 * @param dockerVolume : location inside docker container, should be mounted as volume
	 * @param numberToKeep : how many backup you want to keep
	 * 
	 * @return {@link RestHtmlResponse} 
	 * 
	 * example of response of HTML page:
	 *         {"responseHeader":{"status":0,"QTime":5},"exception":"org.apache.solr.common.SolrException:org.apache.solr.common.SolrException:
	 *         Directory does not exist:
	 *         file:///nop/snapshot.20190212152339023","status":"OK"}
	 */
	protected RestHtmlResponse executeSolrBackupRequest(String core, String dockerVolume, int numberToKeep)
			throws InterruptedException {

		String keep = String.valueOf(numberToKeep);
		RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "{core}/replication?command=backup&location={dockerVolume}&numberToKeep={keep}&wt=json", core,dockerVolume, keep);
		RestHtmlResponse htmlResponse = restWrapper.processHtmlResponse(request);

		// need to wait for backup data to be created or deleted based on numberToKeep
		Utility.waitToLoopTime(2, "Wait until the backup data is created/deleted on docker volume");
		return htmlResponse;
	}

	/**
	 * Will assert that @param count files starting with @param filePrefix are found
	 * inside @param folder
	 */
	protected File[] assertFileExistInLocalBackupFolder(String folder, String filePrefix, int count) {

		File backupLocation = Paths.get("./qa/search/backup/host-bkp", folder).toFile();

		Assert.assertTrue(backupLocation.exists(), "Backup location exists: " + backupLocation.getAbsolutePath());
		if (count >= 1) {
			Assert.assertEquals(backupLocation.listFiles().length, count,
					"Expected Files in folder:" + backupLocation.getPath());

			SoftAssert soft = new SoftAssert();
			for (File file : backupLocation.listFiles()) {
				soft.assertTrue(file.getName().startsWith(filePrefix),
						String.format("File [%s] starts with prefix: %s", file.getPath(), filePrefix));
			}
			soft.assertAll();

			return backupLocation.listFiles();
		} else {
			Assert.assertEquals(backupLocation.listFiles().length, 0, backupLocation.getPath() + "should be empty!");
		}

		return null;

	}
}
