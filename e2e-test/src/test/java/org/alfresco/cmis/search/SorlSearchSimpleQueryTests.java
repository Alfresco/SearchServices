package org.alfresco.cmis.search;

import org.alfresco.cmis.CmisTest;
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
