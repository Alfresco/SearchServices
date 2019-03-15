package org.alfresco.service.search.cmis;

import org.alfresco.service.search.CmisTest;
import org.testng.annotations.Test;

public class SorlSearchSimpleQueryTests extends CmisTest
{
    @Test
    public void simpleQueryOnFolderDesc() throws Exception
    {
        cmisApi.authenticateUser(dataUser.getAdminUser())
               .withQuery("SELECT * FROM cmis:folder ORDER BY cmis:createdBy DESC").assertResultsCount().isLowerThan(101);
    }
}
