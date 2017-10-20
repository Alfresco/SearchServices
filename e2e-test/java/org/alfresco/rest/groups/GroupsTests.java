package org.alfresco.rest.groups;

import java.util.UUID;

import javax.json.Json;
import javax.json.JsonObject;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestGroupsModel;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.model.UserModel;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class GroupsTests extends RestTest
{
    private UserModel adminUser, userModel;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {  
        adminUser = dataUser.getAdminUser();
        userModel = dataUser.createRandomTestUser();
    }
    @Test(groups = { TestGroup.REST_API, TestGroup.GROUPS, TestGroup.SANITY })
    @TestRail(section = { TestGroup.REST_API, TestGroup.NODES }, executionType = ExecutionType.SANITY,
            description = "Verify Collaborator can not lock PERSISTENT after EPHEMERAL lock made by different user")
    public void blabla() throws Exception
    {
        String randomUUID = UUID.randomUUID().toString();
        JsonObject groupBody = Json.createObjectBuilder().add("id", "testGroup"+randomUUID).add("displayName","TestGroup"+randomUUID).add("isRoot", true).build();
        
        String groupBodyCreate = groupBody.toString();

        restClient.authenticateUser(adminUser).withCoreAPI().usingParams("include=zones").usingGroups().createGroup(groupBodyCreate)
                                              .assertThat().field("zones").isNotNull();
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.withCoreAPI().usingParams("isRoot=true&orderBy=id DESC&maxItems=5").usingGroups().listGroups()
                                .assertThat().entriesListIsSortedDescBy("id")
//                                .and().entriesListContains("isRoot", "true")
                                .and().paginationField("maxItems").equals(5);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.withCoreAPI().usingGroups().getGroupDetail("GROUP_"+"testGroup"+randomUUID);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        JsonObject groupMembership = Json.createObjectBuilder().add("id", userModel.getUsername()).add("memberType", "PERSON").build();
        String groupMembershipBodyCreate = groupMembership.toString();
        restClient.withCoreAPI().usingGroups().createGroupMembership("GROUP_"+"testGroup"+randomUUID, groupMembershipBodyCreate);
        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        restClient.withCoreAPI().usingGroups().listGroupMemberships("GROUP_"+"testGroup"+randomUUID);
        restClient.assertStatusCodeIs(HttpStatus.OK);

        restClient.withCoreAPI().usingGroups().deleteGroupMembership("GROUP_"+"testGroup"+randomUUID, userModel.getUsername());
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);

        restClient.withCoreAPI().usingGroups().deleteGroup("GROUP_"+"testGroup"+randomUUID);
        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);


    }
}
