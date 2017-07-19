package org.alfresco.rest.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

public class DeleteAuditCoreTests extends AuditTest
{

	@Test(groups = { TestGroup.REST_API, TestGroup.AUDIT, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API,
            TestGroup.AUDIT }, executionType = ExecutionType.SANITY, description = "Verify if the admin user can delete audit application entries between the range of ids")
    public void deleteAuditApplicationEntriesBeetween() throws Exception
    {
		int numberOfAlfrescoAccessAuditEntries = restClient.authenticateUser(adminUser).withParams("maxItems=3").withCoreAPI().usingAudit().listAuditEntriesForAnAuditApplication(restAuditAppModel.getId()).getEntries().size();

		List<String> ids = new ArrayList<String>();
		for (int i=0; i<numberOfAlfrescoAccessAuditEntries; i++)
		{
			ids.add(restClient.authenticateUser(adminUser).withParams("maxItems=3").withCoreAPI().usingAudit().listAuditEntriesForAnAuditApplication(restAuditAppModel.getId()).getEntryByIndex(i).getId());

		}

		String rangeStart = ids.get(ids.indexOf(Collections.min(ids))).toString().trim();
		String rangeEnd = ids.get(ids.indexOf(Collections.max(ids))).toString().trim();

		restClient.authenticateUser(userModel).withParams("where=(id BETWEEN ("+rangeStart+","+rangeEnd+"))")
        .withCoreAPI().usingAudit().deleteAuditApplicationsEntries("alfresco-access");

		restClient.assertStatusCodeIs(HttpStatus.FORBIDDEN);

		restClient.authenticateUser(adminUser).withParams("where=(id BETWEEN ("+rangeStart+","+rangeEnd+"))")
        .withCoreAPI().usingAudit().deleteAuditApplicationsEntries("alfresco-access");

		restClient.assertStatusCodeIs(HttpStatus.OK);

		restClient.authenticateUser(adminUser)
        .withCoreAPI().usingAudit().getAuditApplicationsEntry("alfresco-access", rangeStart);

		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);

		restClient.authenticateUser(adminUser)
        .withCoreAPI().usingAudit().getAuditApplicationsEntry("alfresco-access", rangeEnd);

		restClient.assertStatusCodeIs(HttpStatus.NOT_FOUND);

    }

}
