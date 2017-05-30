package org.alfresco.rest.nodes;

import static org.alfresco.utility.report.log.Step.STEP;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestErrorModel;
import org.alfresco.rest.model.RestNodeModel;
import org.alfresco.rest.model.body.RestNodeLockBodyModel;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.report.Bug;
import org.alfresco.utility.report.Bug.Status;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class NodesLockTests extends RestTest
{
    private UserModel user1, user2, adminUser;
    private SiteModel publicSite;
    private FileModel file1;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        adminUser = dataUser.getAdminUser();
        publicSite = dataSite.usingUser(adminUser).createPublicRandomSite();
        user1 = dataUser.createRandomTestUser();
        user2 = dataUser.createRandomTestUser();
        dataUser.addUserToSite(user1, publicSite, UserRole.SiteCollaborator);
        user1.setUserRole(UserRole.SiteCollaborator);
        dataUser.addUserToSite(user2, publicSite, UserRole.SiteCollaborator);
        user2.setUserRole(UserRole.SiteCollaborator);

    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can not lock PERSISTENT after EPHEMERAL lock made by different user")
    public void lockEphemeralAndRelockPersistentDifferentUser() throws Exception
    {
        STEP("1. Administrator adds a file in the site.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode EPHEMERAL for 20 seconds with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(20);
        lockBodyModel.setType("FULL");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
            .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
            .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");

        STEP("5. Cannot lock the file using mode PERSISTENT with user2 while the file is still locked");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("PERSISTENT");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user2).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError()
                .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.LOCKED_NODE_SUMMARY, file1.getNodeRefWithoutVersion()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can not lock PERSISTENT after EPHEMERAL lock made by different user")
    public void lockEphemeralAndRelockEphemeralDifferentUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode EPHEMERAL with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(20);
        lockBodyModel.setType("FULL");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");

        STEP("5. Cannot lock the file using mode EPHEMERAL with user2 while the file is still locked");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("EPHEMERAL");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("FULL");
        restClient.authenticateUser(user2).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError()
                .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.LOCKED_NODE_SUMMARY, file1.getNodeRefWithoutVersion()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock EPHEMERAL after EPHEMERAL lock made by same user")
    public void lockEphemeralAndRelockEphemeralSameUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode EPHEMERAL with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(20);
        lockBodyModel.setType("FULL");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");

        STEP("5. Lock the file using mode EPHEMERAL with user1 while the file is still locked");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("EPHEMERAL");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("FULL");
        RestNodeModel file1Model3 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");
    }

    @Bug(id = "MNT-17612", status = Status.FIXED, description = "AccessDeniedException in AOS Edit Offline Upload New Version")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock PERSISTENT after EPHEMERAL lock made by same user")
    public void lockEphemeralAndRelockPersistentSameUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode EPHEMERAL with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(2);
        lockBodyModel.setType("FULL");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");

        STEP("5. Lock the file using mode PERSISTENT with user1 while the file is still locked");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("PERSISTENT");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("ALLOW_OWNER_CHANGES");
        RestNodeModel file1Model3 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");

        STEP("6. Verify that the file is locked PERSISTENT only after EPHEMERAL lock has expired");
        try{Thread.sleep(2500);}finally{}
        RestNodeModel file1Model4 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model4.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can not lock EPHEMERAL after PERSISTENT lock made by different user")
    public void lockPersistentAndRelockEphemeralDifferentUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode PERSISTENT with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("PERSISTENT");
        lockBodyModel.setTimeToExpire(20);
        lockBodyModel.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");

        STEP("5. Cannot lock the file using mode EPHEMERAL with user2 while the file is still locked");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("EPHEMERAL");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("FULL");
        restClient.authenticateUser(user2).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError()
                .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.LOCKED_NODE_SUMMARY, file1.getNodeRefWithoutVersion()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can not lock PERSISTENT after PERSISTENT lock made by different user")
    public void lockPersistentAndRelockPersistentDifferentUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode PERSISTENT with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("PERSISTENT");
        lockBodyModel.setTimeToExpire(20);
        lockBodyModel.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");

        STEP("5. Cannot lock the file using mode PERSISTENT with user2 while the file is still locked");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("PERSISTENT");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user2).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.UNPROCESSABLE_ENTITY).assertLastError()
                .containsErrorKey(RestErrorModel.API_DEFAULT_ERRORKEY)
                .containsSummary(String.format(RestErrorModel.LOCKED_NODE_SUMMARY, file1.getNodeRefWithoutVersion()))
                .descriptionURLIs(RestErrorModel.RESTAPIEXPLORER);
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock EPHEMERAL after PERSISTENT lock made by different user is expired")
    public void lockPersistentAndRelockEphemeralAfterExpiredLockDifferentUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode PERSISTENT with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("PERSISTENT");
        lockBodyModel.setTimeToExpire(1);
        lockBodyModel.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with admin that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");

        STEP("5. Lock the file using mode EPHEMERAL with user2 while the first lock has expired");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("EPHEMERAL");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("FULL");
        try{Thread.sleep(1500);}finally{}
        RestNodeModel file1Model3 = restClient.authenticateUser(user2).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");
    }

    @Bug(id = "MNT-17612", status = Status.FIXED, description = "AccessDeniedException in AOS Edit Offline Upload New Version")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock PERSISTENT after EPHEMERAL lock made by another user is expired")
    public void lockEphemeralAndRelockPersistentAfterExpiredLockDifferentUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode EPHEMERAL with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(1);
        lockBodyModel.setType("FULL");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");

        STEP("5. Lock the file using mode PERSISTENT with user2 after the first lock has expired");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("PERSISTENT");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("ALLOW_OWNER_CHANGES");
        try{Thread.sleep(1500);}finally{}
        RestNodeModel file1Model3 = restClient.authenticateUser(user2).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock EPHEMERAL after EPHEMERAL lock made by another user is expired")
    public void lockEphemeralAndRelockEphemeralAfterExpiredLockDifferentUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode EPHEMERAL with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(1);
        lockBodyModel.setType("FULL");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");

        STEP("5. Lock the file using mode EPHEMERAL with user2 while the first lock has expired");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("EPHEMERAL");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("FULL");
        try{Thread.sleep(1500);}finally{}
        RestNodeModel file1Model3 = restClient.authenticateUser(user2).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock PERSISTENT after PERSISTENT lock made by different user is expired")
    public void lockPersistentAndRelockPersistentAfterExpiredLockDifferentUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode PERSISTENT with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("PERSISTENT");
        lockBodyModel.setTimeToExpire(1);
        lockBodyModel.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with admin that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");

        STEP("5. Lock the file using mode PERSISTENT with user2 after the first lock has expired");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("PERSISTENT");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("ALLOW_OWNER_CHANGES");
        try{Thread.sleep(1500);}finally{}
        RestNodeModel file1Model3 = restClient.authenticateUser(user2).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock EPHEMERAL after EPHEMERAL lock made by same user is expired")
    public void lockEphemeralAndRelockEphemeralAfterExpiredLockSameUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode EPHEMERAL with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(1);
        lockBodyModel.setType("FULL");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");

        STEP("5. Lock the file using mode EPHEMERAL with user1 after the first lock has expired");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("EPHEMERAL");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("FULL");
        try{Thread.sleep(1500);}finally{}
        RestNodeModel file1Model3 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock PERSISTENT after PERSISTENT lock made by same user is expired")
    public void lockPersistentAndRelockPersistentAfterExpiredLockSameUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode PERSISTENT with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("PERSISTENT");
        lockBodyModel.setTimeToExpire(1);
        lockBodyModel.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true);
        file1Model2.assertThat().field("properties").contains("lockLifetime=PERSISTENT");

        STEP("5. Lock the file using mode PERSISTENT with user1 while the first lock has expired");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("PERSISTENT");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("ALLOW_OWNER_CHANGES");
        try{Thread.sleep(1500);}finally{}
        RestNodeModel file1Model3 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock EPHEMERAL after PERSISTENT lock made by same user is expired")
    public void lockPersistentAndRelockEphemeralAfterExpiredLockSameUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode PERSISTENT with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("PERSISTENT");
        lockBodyModel.setTimeToExpire(1);
        lockBodyModel.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with admin that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");

        STEP("5. Lock the file using mode EPHEMERAL with user1 while the first lock has expired");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("EPHEMERAL");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("FULL");
        try{Thread.sleep(1500);}finally{}
        RestNodeModel file1Model3 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");
    }

    @Bug(id = "MNT-17612", status = Status.FIXED, description = "AccessDeniedException in AOS Edit Offline Upload New Version")
    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock PERSISTENT after EPHEMERAL lock made by same user is expired")
    public void lockEphemeralAndRelockPersistentAfterExpiredLockSameUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1)
                .usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode EPHEMERAL with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("EPHEMERAL");
        lockBodyModel.setTimeToExpire(1);
        lockBodyModel.setType("FULL");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with useer1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");

        STEP("5. Lock the file using mode PERSISTENT with user1 while the first lock has expired");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("PERSISTENT");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("ALLOW_OWNER_CHANGES");
        try{Thread.sleep(1500);}finally{}
        RestNodeModel file1Model3 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock PERSISTENT after PERSISTENT lock made by same user")
    public void lockPersistentAndRelockPersistentSameUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode PERSISTENT with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("PERSISTENT");
        lockBodyModel.setTimeToExpire(20);
        lockBodyModel.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");

        STEP("5. Lock the file using mode PERSISTENT with user1 while the file is still locked");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("PERSISTENT");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("ALLOW_OWNER_CHANGES");
        RestNodeModel file1Model3 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true);
        file1Model3.assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");
    }

    @Test(groups = { TestGroup.REST_API, TestGroup.NODES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.REGRESSION,
            description = "Verify Collaborator can lock EPHERMERAL after PERSISTENT lock made by same user")
    public void lockPersistentAndRelockEphemeralSameUser() throws Exception
    {
        STEP("1. Adds a file in the site by administrator.");
        file1 = dataContent.usingUser(adminUser).usingSite(publicSite).createContent(DocumentType.TEXT_PLAIN);

        STEP("2. Verify with admin that the file is not locked.");
        RestNodeModel file1Model1 = restClient.authenticateUser(adminUser).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model1.assertThat().field("isLocked").is(false);

        STEP("3. Lock the file using mode PERSISTENT with user1 (POST nodes/{nodeId}/lock).");
        RestNodeLockBodyModel lockBodyModel = new RestNodeLockBodyModel();
        lockBodyModel.setLifetime("PERSISTENT");
        lockBodyModel.setTimeToExpire(1);
        lockBodyModel.setType("ALLOW_OWNER_CHANGES");
        restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include?isLocked").lockNode(lockBodyModel);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        STEP("4. Verify with user1 that the file is locked.");
        RestNodeModel file1Model2 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").getNode();
        file1Model2.assertThat().field("isLocked").is(true)
                .assertThat().field("properties").contains("lockLifetime=PERSISTENT")
                .assertThat().field("properties").contains("lockType=WRITE_LOCK");

        STEP("5. Lock the file using mode EPHERMERAL with user1 while the file is still locked");
        RestNodeLockBodyModel lockBodyModel2 = new RestNodeLockBodyModel();
        lockBodyModel2.setLifetime("EPHEMERAL");
        lockBodyModel2.setTimeToExpire(20);
        lockBodyModel2.setType("FULL");
        RestNodeModel file1Model3 = restClient.authenticateUser(user1).withCoreAPI().usingNode(file1).usingParams("include=isLocked").lockNode(lockBodyModel2);
        restClient.assertStatusCodeIs(HttpStatus.OK);
        file1Model3.assertThat().field("isLocked").is(true);
        file1Model3.assertThat().field("properties").contains("lockLifetime=EPHEMERAL")
                .assertThat().field("properties").contains("lockType=READ_ONLY_LOCK");
    }

}
