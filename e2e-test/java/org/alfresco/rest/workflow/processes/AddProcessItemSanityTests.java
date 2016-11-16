package org.alfresco.rest.workflow.processes;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestWorkflowTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.model.RestItemModel;
import org.alfresco.rest.model.RestProcessModel;
import org.alfresco.utility.model.*;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

/**
 * @author iulia.cojocea
 */
@Test(groups = { TestGroup.REST_API, TestGroup.WORKFLOW, TestGroup.PROCESSES, TestGroup.SANITY})
public class AddProcessItemSanityTests extends RestWorkflowTest
{
    private FileModel document, document2;
    private SiteModel siteModel;
    private UserModel userWhoStartsTask, assignee, adminUser;
    private RestProcessModel processModel;
    private RestItemModel processItem;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        userWhoStartsTask = dataUser.createRandomTestUser();
        assignee = dataUser.createRandomTestUser();
        siteModel = dataSite.usingUser(adminUser).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.XML);
        dataWorkflow.usingUser(userWhoStartsTask).usingSite(siteModel).usingResource(document).createNewTaskAndAssignTo(assignee);
    }

    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY,
            description = "Create non-existing process item")
    public void addProcessItem() throws JsonToModelConversionException, Exception
    {
        document2 = FileModel.getRandomFileModel(FileType.TEXT_PLAIN, "file content");
        dataContent.usingSite(siteModel).createContent(document2);
        
        processModel = restClient.authenticateUser(adminUser).getProcesses().getOneRandomEntry().onModel();
        processItem = restClient.usingProcess(processModel).addProcessItem(document2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItem.assertThat().field("createdAt").isNotEmpty()
                   .and().field("size").is(document2.getContent().length())
                   .and().field("createdBy").is(adminUser.getUsername())
                   .and().field("modifiedAt").isNotEmpty()
                   .and().field("name").is(document2.getName())
                   .and().field("modifiedBy").is(adminUser.getUsername())
                   .and().field("id").isNotEmpty()
                   .and().field("mimeType").is(document2.getFileType().mimeType);

        restClient.usingProcess(processModel).getProcessItems()
                .assertThat().entriesListContains("id", processItem.getId());
    }

    @Bug(id= "MNT-16966")
    @TestRail(section = {TestGroup.REST_API, TestGroup.PROCESSES }, executionType = ExecutionType.SANITY,
            description = "Add process item that already exists")
    public void addProcessItemThatAlreadyExists() throws JsonToModelConversionException, Exception
    {
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.XML);

        processModel = restClient.authenticateUser(adminUser).getProcesses().getOneRandomEntry().onModel();
        processItem = restClient.usingProcess(processModel).addProcessItem(document2);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);
        processItem.assertThat().field("createdAt").isNotEmpty()
                .and().field("size").is("19")
                .and().field("createdBy").is(adminUser.getUsername())
                .and().field("modifiedAt").isNotEmpty()
                .and().field("name").is(document2.getName())
                .and().field("modifiedBy").is(adminUser.getUsername())
                .and().field("id").isNotEmpty()
                .and().field("mimeType").is(document2.getFileType().mimeType);

        restClient.usingProcess(processModel).getProcessItems()
                .assertThat().entriesListContains("id", processItem.getId())
                .and().entriesListContains("name", document2.getName());
        restClient.usingProcess(processModel).addProcessItem(document2);
        restClient.assertStatusCodeIs(HttpStatus.BAD_REQUEST);
    }
}
