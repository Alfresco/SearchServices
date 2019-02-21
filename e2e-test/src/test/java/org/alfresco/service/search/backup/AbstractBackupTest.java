package org.alfresco.service.search.backup;

import java.io.File;
import java.nio.file.Paths;

import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestHtmlResponse;
import org.alfresco.utility.Utility;
import org.alfresco.utility.data.DataSite;
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
public abstract class AbstractBackupTest extends AbstractTestNGSpringContextTests {

	@Autowired
	protected ServerHealth serverHealth;
	
	@Autowired
    protected DataSite dataSite;

	@Autowired
	RestWrapper restWrapper;

	@Value("${solr.port}")
	int solrPort;

	@Value("${solr.docker.hostPath}")
	String solrDockerHostPath;

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
	 * @param location : location inside docker container, should be mounted as volume
	 * @param numberToKeep : how many backup you want to keep
	 * 
	 * @return {@link RestHtmlResponse} 
	 * 
	 * example of response of HTML page:
	 *         {"responseHeader":{"status":0,"QTime":5},"exception":"org.apache.solr.common.SolrException:org.apache.solr.common.SolrException:
	 *         Directory does not exist:
	 *         file:///nop/snapshot.20190212152339023","status":"OK"}
	 * @throws InterruptedException
	 */
	protected RestHtmlResponse executeSolrBackupRequest(String core, String location, int numberToKeep)
			throws InterruptedException {

		String keep = String.valueOf(numberToKeep);
		RestRequest request = RestRequest.simpleRequest(HttpMethod.GET,
				"{core}/replication?command=backup&location={location}&numberToKeep={keep}&wt=json", core,
				location, keep);
		RestHtmlResponse htmlResponse = restWrapper.processHtmlResponse(request);
		// need to wait for backup data to be created or deleted based on numberToKeep
		Utility.waitToLoopTime(2, "Wait until the backup data is created/deleted");
		return htmlResponse;

	}

	/**
	 * Will assert that @param count files starting with @param filePrefix are found
	 * inside @param folder
	 */
	protected File[] assertFileExistInLocalBackupFolder(String folder, String filePrefix, int count) {

		File backupLocation = Paths.get(solrDockerHostPath, folder).toFile();

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
