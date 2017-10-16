package org.alfresco.rest;

import org.alfresco.dataprep.CMISUtil.DocumentType;
import org.alfresco.rest.model.RestNetworkModel;
import org.alfresco.utility.model.FileModel;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;

public abstract class NetworkDataPrep extends RestTest
{
    protected static UserModel adminUserModel;
    protected static UserModel adminTenantUser, secondAdminTenantUser;
    protected static UserModel tenantUser, secondTenantUser, differentNetworkTenantUser;
    protected static UserModel tenantUserWithBad;
    protected static UserModel userModel;
    protected static RestNetworkModel restNetworkModel;
    protected static String tenantDomain;
    protected static SiteModel siteModel;
    protected static FileModel document, document2;
    private static boolean isInitialized = false;
    
    public void init()
    {
        if(isInitialized)
        {
            
        }
        else
        {
            isInitialized = true;
            try
            {
                initialization();
            }
            catch (Exception e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
    }

    public void initialization() throws Exception
    {
        adminUserModel = dataUser.getAdminUser();
        //create first tenant Admin User.
        adminTenantUser = UserModel.getAdminTenantUser();
        restClient.authenticateUser(adminUserModel);
            restClient.usingTenant().createTenant(adminTenantUser);

        tenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("uTenant");
        secondTenantUser = dataUser.usingUser(adminTenantUser).createUserWithTenant("sTenant");
        //create second tenant Admin User.
        secondAdminTenantUser = UserModel.getAdminTenantUser();
            restClient.usingTenant().createTenant(secondAdminTenantUser);

        tenantDomain = tenantUser.getDomain();
        differentNetworkTenantUser = dataUser.usingUser(secondAdminTenantUser).createUserWithTenant("dTenant");

        userModel = dataUser.createRandomTestUser();

        siteModel = dataSite.usingUser(adminUserModel).createPublicRandomSite();
        document = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
        document2 = dataContent.usingSite(siteModel).createContent(DocumentType.TEXT_PLAIN);
    }
}
