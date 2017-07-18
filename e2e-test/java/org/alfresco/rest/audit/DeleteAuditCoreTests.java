package org.alfresco.rest.audit;

import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

public class DeleteAuditCoreTests extends AuditTest
{

    @Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if deleting an audit entry for an application is only for admin user, and status code is 204")
    public void deleteAuditEntry() throws Exception
    {
        restAuditEntryCollection = restClient.authenticateUser(adminUser).withParams("maxItems=10").withCoreAPI().usingAudit()
                .listAuditEntriesForAnAuditApplication(restAuditAppModel.getId());
        restClient.assertStatusCodeIs(HttpStatus.OK);
        String entryId = restAuditEntryCollection.getEntries().get(restAuditEntryCollection.getPagination().getCount() - 1)
                .onModel().getId();

        restClient.authenticateUser(userModel).withCoreAPI().usingAudit()
                .deleteAuditEntryForAnAuditApplication(restAuditAppModel.getId(), entryId);
        restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);

        restClient.authenticateUser(adminUser).withCoreAPI().usingAudit()
                .deleteAuditEntryForAnAuditApplication(restAuditAppModel.getId(), entryId);

        restClient.assertStatusCodeIs(HttpStatus.NO_CONTENT);
        restAuditEntryModel = restClient.authenticateUser(adminUser).withCoreAPI().usingAudit()
                .getAuditEntryForAnAuditApplication(restAuditAppModel.getId(), entryId);
        restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);
    }

}
