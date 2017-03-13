package org.alfresco.rest.nodes;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestNodeBodyModel;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.model.RestNodeModelsCollection;
import org.alfresco.rest.model.builder.NodesBuilder;
import org.alfresco.utility.Utility;
import org.alfresco.utility.model.ContentModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

/**
 * Handles tests related to api-explorer/#!/nodes/children
 */
public class NodesChildrenTests extends RestTest
{
    @TestRail(section = { TestGroup.REST_API,TestGroup.NODES }, executionType = ExecutionType.SANITY,
            description = "Verify new folder node is created as children on -my- posting as JSON content type")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.SANITY})    
    public void createNewFolderNodeViaJason() throws Exception
    {
        restClient.authenticateUser(dataContent.getAdminUser());

        RestNodeBodyModel node = new RestNodeBodyModel();
        node.setName("My Folder");
        node.setNodeType("cm:folder");
        
        RestNodeModel newNode = restClient.withParams("autoRename=true").withCoreAPI().usingNode(ContentModel.my()).createNode(node);        
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        
        newNode.assertThat().field("aspectNames").contains("cm:auditable")
               .assertThat().field("isFolder").is(true)
               .assertThat().field("isFile").is(false)
               .assertThat().field("name").contains(node.getName());        
    }
    
    @TestRail(section = { TestGroup.REST_API,TestGroup.NODES }, executionType = ExecutionType.SANITY,
            description = "Verify new folder node is created as children on -my- posting as MultiPart content type")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.SANITY})
    public void createNewFolderNodeWithMultiPartForms() throws Exception
    {
        //configuring multipart form
        restClient.authenticateUser(dataContent.getAdminUser())
                  .configureRequestSpec() 
                    .addMultiPart("filedata", Utility.getResourceTestDataFile("restapi-resource"))
                    .addFormParam("renditions", "doclib")
                    .addFormParam("autoRename", true);
        
        RestNodeModel newNode = restClient.withCoreAPI().usingNode(ContentModel.my()).createNode();
        restClient.assertStatusCodeIs(HttpStatus.CREATED); 
        newNode.assertThat().field("aspectNames").contains("cm:auditable")
               .assertThat().field("isFolder").is(false)
               .assertThat().field("isFile").is(true)
               .assertThat().field("name").contains("restapi-resource");   
    }

    @TestRail(section = { TestGroup.REST_API,TestGroup.NODES }, executionType = ExecutionType.SANITY,
            description = "Verify list children when listing with relativePath and pagination")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.SANITY})
    public void checkRelativePathAndPaginationOnCreateChildrenNode() throws Exception
    {
        /*
         * Given we have a folder hierarchy folder1/folder2/folder3 and folder3 containing 3 files file1, file2, and file3
         */
        NodesBuilder nodesBuilder = restClient.authenticateUser(dataUser.getAdminUser())
                                              .withCoreAPI().usingNode(ContentModel.my())
                                              .defineNodes();        
        nodesBuilder
            .folder("F1")
            .folder("F2")
            .folder("F3")            
                .file("f1")
                .file("f2")
                .file("f3");
              
        RestNodeModelsCollection returnedFiles = restClient.withParams("maxItems=2", 
                                                               "skipCount=1", 
                                                               String.format("relativePath=%s/%s", nodesBuilder.getNode("F2").getName(), nodesBuilder.getNode("F3").getName()))                                                               
                                                               .withCoreAPI().usingNode(nodesBuilder.getNode("F1").toContentModel()).listChildren();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        /*
         * Then I receive file2 and file3
         */
        returnedFiles.assertThat().entriesListCountIs(2);
        returnedFiles.getEntries().get(0).onModel().assertThat().field("id").is(nodesBuilder.getNode("f2").getId());
        returnedFiles.getEntries().get(1).onModel().assertThat().field("id").is(nodesBuilder.getNode("f3").getId());        
    }
    
}
