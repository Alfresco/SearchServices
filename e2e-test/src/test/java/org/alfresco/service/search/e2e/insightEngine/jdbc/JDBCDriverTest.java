package org.alfresco.service.search.e2e.insightEngine.jdbc;

import java.sql.SQLException;
import java.util.List;

import org.alfresco.utility.network.ServerHealth;
import org.alfresco.utility.network.db.DatabaseOperationImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import static org.testng.Assert.*;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * This class is connecting via JDBC Driver (see default.properties)
 * 
 * example:
 * db.url= jdbc:alfresco://${alfresco.server}:${alfresco.port}?collection=alfresco
 * 
 * I am using the TAS DatabaseOperationImpl service to connect
 * 
 * @author Paul Brodner
 *
 */
@ContextConfiguration("classpath:alfresco-search-e2e-context.xml")
public class JDBCDriverTest extends AbstractTestNGSpringContextTests {

	@Autowired
	protected ServerHealth serverHealth;

	@Autowired
	DatabaseOperationImpl jdbcOperation;

	@BeforeClass(alwaysRun = true)
	public void checkServerHealth() throws Exception {
		serverHealth.assertServerIsOnline();
	}
		 
	/**
	 * In order to work start ACS + InsightEngine service
	 * @throws SQLException
	 */
	@Test
	public void iCanConnectWithJDBCDriver() throws SQLException {
		assertTrue(jdbcOperation.connect(), "I can connect via JDBC Driver");
	}

	@Test(dependsOnMethods="iCanConnectWithJDBCDriver")
	public void iCanQueryTheTotalNumberOfDocumentsFromAlfresco() {
		List<Object> results = jdbcOperation.executeQuery("select cm_name from alfresco where cm_Name = 'presentation*'");
		assertEquals(results.size(), 3);
	}
}
