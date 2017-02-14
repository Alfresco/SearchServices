package org.alfresco.rest.queries;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestNodeModelsCollection;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

/**
 * Handle requests on Queries
 *
 */
public class QueriesTest extends RestTest
{

    @TestRail(section = { TestGroup.REST_API,
            TestGroup.QUERIES }, executionType = ExecutionType.REGRESSION, description = "Verify GET queries on queries/nodes returnes success status code")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void getOnQueriesNodesRoute() throws Exception
    {
        restClient.authenticateUser(dataContent.getAdminUser())
                  .withCoreAPI()                 
                  .usingQueries().findNodes();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        
        restClient.assertLastError()
                        .containsErrorKey("Query 'term' not specified")
                        //and assert on summary too if you want
                        .containsSummary("Query 'term' not specified");
        
        restClient.withCoreAPI()                 
                  .usingQueries()
                  .usingParams("term=ab")
                  .findNodes();
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
        
        restClient.assertLastError()
                        .containsErrorKey("Query 'term' is too short");
        
        /*
         * now making the correct call with a valid term value 
         */
        RestNodeModelsCollection nodes = restClient.withCoreAPI().usingQueries().usingParams("term=name").findNodes();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        nodes.assertThat().entriesListIsNotEmpty();
    }
}
