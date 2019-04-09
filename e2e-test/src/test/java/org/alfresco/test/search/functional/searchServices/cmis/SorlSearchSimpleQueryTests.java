package org.alfresco.test.search.functional.searchServices.cmis;

import org.testng.annotations.Test;

public class SorlSearchSimpleQueryTests extends AbstractCmisE2ETest
{
    @Test
    public void simpleQueryOnFolderDesc() throws Exception
    {
        cmisApi.authenticateUser(dataUser.getAdminUser())
               .withQuery("SELECT * FROM cmis:folder ORDER BY cmis:createdBy DESC").assertResultsCount().isLowerThan(101);
    }
}
