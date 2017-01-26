package org.alfresco.rest.nodes;

import java.util.UUID;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestNodeBodyModel;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.model.RestNodeModelsCollection;
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
    public void listChildrenTest() throws Exception
    {
        /*
         * Given we have a folder hierarchy folder1/folder2/folder3 and folder3 containing 3 files file1, file2, and file3
         */
        restClient.authenticateUser(dataContent.getAdminUser());

        RestNodeModel folder1 = createTestNode(ContentModel.my().getNodeRef(), "F1", "cm:folder");
        RestNodeModel folder2 = createTestNode(folder1.getId(), "F2", "cm:folder");
        RestNodeModel folder3 = createTestNode(folder2.getId(), "F3", "cm:folder");

        RestNodeModel file1 = createTestNode(folder3.getId(), "f1", "cm:content");
        RestNodeModel file2 = createTestNode(folder3.getId(), "f2", "cm:content");
        RestNodeModel file3 = createTestNode(folder3.getId(), "f3", "cm:content");

        /*
         * When listing the children of folder1 with relative path folder2/folder3, page size 2 and skip count 1
         */
        ContentModel folder1Model = new ContentModel();
        folder1Model.setNodeRef(folder1.getId());
        RestNodeModelsCollection files = restClient.withParams("maxItems=2", "skipCount=1", "relativePath="+folder2.getName()+"/"+folder3.getName())
                                                   .withCoreAPI().usingNode(folder1Model).listChildren();
        restClient.assertStatusCodeIs(HttpStatus.OK);

        /*
         * Then I receive file2 and file3
         */
        assert(files.getEntries().size() == 2);
        assert(files.getEntries().get(0).onModel().getId().equals(file2.getId()));
        assert(files.getEntries().get(1).onModel().getId().equals(file3.getId()));
    }

    /**
     * Helper method that creates a node in a specified folder
     * 
     * @param parentId id of the parent folder
     * @param name the name if the new node (a random string will be appended at the end)
     * @param nodeType the type of the new node
     * @return the model of the created node
     * @throws Exception if the node failed to be created
     */
    private RestNodeModel createTestNode(String parentId, String name, String nodeType) throws Exception
    {
        // define the new node model
        RestNodeBodyModel model = new RestNodeBodyModel();
        model.setName(name + "-" + UUID.randomUUID().toString());
        model.setNodeType(nodeType);

        // define the parent
        ContentModel parent = new ContentModel();
        parent.setNodeRef(parentId);

        // call the create children api
        RestNodeModel newNode = restClient.withCoreAPI().usingNode(parent).createNode(model);

        // check and return the result
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        return newNode;
    }
}
