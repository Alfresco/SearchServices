package org.alfresco.rest.queries;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestNodeModelsCollection;
import org.alfresco.utility.RetryOperation;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.FileType;
import org.alfresco.utility.model.FolderModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
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
    @TestRail(section = { TestGroup.REST_API, TestGroup.QUERIES },
            executionType = ExecutionType.REGRESSION, description = "Verify GET queries on queries/nodes returnes success status code")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.REGRESSION })
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
    
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.QUERIES }, executionType = ExecutionType.REGRESSION, description = "Verify GET queries on queries/nodes return success status code")
    @Test(groups = { TestGroup.REST_API, TestGroup.RATINGS, TestGroup.CORE })
    public void testSearchTermWhiteSpace() throws Exception
    {

        UserModel userModel = dataUser.createRandomTestUser();
        SiteModel siteModel = dataSite.usingUser(userModel).createPublicRandomSite();
        /*
         * Create the following file structure for preconditions : 
         * |- folder
         *      |--find123.txt 
         *      |-- find123 find.txt
         */
        restClient.authenticateUser(userModel).withCoreAPI().usingNode(ContentModel.my()).defineNodes();
        FolderModel folder = new FolderModel("folder" + Math.random());

        // I use "find123" and "find123 find", the search using second term must
        // return less result
        // The space must not break the query
        String childTerm = "find" + Math.random();
        String childTermWS = childTerm + " " + "find";

        dataContent.usingUser(userModel).usingSite(siteModel).createFolder(folder);
        FileModel file1 = new FileModel(childTerm + ".txt", FileType.TEXT_PLAIN, childTerm);
        FileModel file2 = new FileModel(childTermWS + ".txt", FileType.TEXT_PLAIN, childTermWS);
        ContentModel cm = new ContentModel();
        cm.setCmisLocation(folder.getCmisLocation());
        cm.setName(folder.getName());
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(cm).createContent(file1);
        dataContent.usingUser(userModel).usingSite(siteModel).usingResource(cm).createContent(file2);
        
        RetryOperation op = new RetryOperation(){
        	public void execute() throws Exception{
        		RestNodeModelsCollection nodesChildTerm = restClient.withCoreAPI().usingQueries().usingParams("term=" + childTerm).findNodes();
                // check if the search returns all nodes which contain that query term
                assertEquals(2, nodesChildTerm.getEntries().size());
                RestNodeModelsCollection nodesChildTermWS = restClient.withCoreAPI().usingQueries().usingParams("term=" + childTermWS).findNodes();
                // check if search works for words with space and the space don't break
                // the query
                assertEquals(1, nodesChildTermWS.getEntries().size());
                assertTrue(nodesChildTerm.getEntries().size() >= nodesChildTermWS.getEntries().size());
            }

        };
       Utility.sleep(10, 3500, op);// Allow indexing to complete.

    }
}
