package org.alfresco.rest.demo.workshop;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.constants.UserRole;
import org.alfresco.utility.model.SiteModel;
import org.alfresco.utility.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.annotations.Test;

public class RestApiDemoTests extends RestTest
{
    @Autowired
    RestSitesApi sitesAPI;
    
    @Test
    public void verifyGetSiteMembersRestApiCall() throws Exception
    {
        UserModel user = dataUser.createRandomTestUser();
        UserModel member = dataUser.createRandomTestUser();
        SiteModel site = dataSite.usingUser(user).createPublicRandomSite();
        dataUser.usingUser(user).addUserToSite(member, site, UserRole.SiteCollaborator);

        restClient.authenticateUser(user);
        
        sitesAPI.useRestClient(restClient);        
        sitesAPI.getSiteMembers(site).assertThat().entriesListIsNotEmpty()
            .and().entriesListContains("id", member.getUsername())
            .and().entriesListContains("role", member.getUserRole().toString());
        
        sitesAPI.usingRestWrapper().assertStatusCodeIs(HttpStatus.OK);

    }
    
    @Test
    public void verifyGetASiteMemberApiCall() throws Exception
    {
        UserModel user = dataUser.createRandomTestUser();
        UserModel member = dataUser.createRandomTestUser();
        SiteModel site = dataSite.usingUser(user).createPublicRandomSite();
        dataUser.usingUser(user).addUserToSite(member, site, UserRole.SiteCollaborator);

        sitesAPI.useRestClient(restClient);
        restClient.authenticateUser(user);
        sitesAPI.getSiteMember(site, member)
            .assertThat().field("id").is(member.getUsername())
            .assertThat().field("role").is(member.getUserRole().toString());

        restClient.assertStatusCodeIs(HttpStatus.OK);
    }
}
