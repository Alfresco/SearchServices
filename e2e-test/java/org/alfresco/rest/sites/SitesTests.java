package org.alfresco.rest.sites;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.model.RestSiteBodyCreateModel;
import org.alfresco.rest.model.RestSiteModel;
import org.alfresco.utility.data.RandomData;
import org.alfresco.utility.model.TestGroup;
import org.alfresco.utility.testrail.ExecutionType;
import org.alfresco.utility.testrail.annotation.TestRail;
import org.springframework.http.HttpStatus;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.Test;

/**
 * Handles tests related to api-explorer/#!/sites
 * 
 * @author Ana Bozianu
 */
public class SitesTests extends RestTest
{

    @Test(groups = { TestGroup.REST_API, TestGroup.SITES, TestGroup.CORE })
    @TestRail(section = { TestGroup.REST_API, TestGroup.SITES }, executionType = ExecutionType.SANITY, description = "Tests the creation of a site")
    public void testCreateSite() throws Exception
    {
        RestSiteBodyCreateModel site = new RestSiteBodyCreateModel();
        site.setId(RandomData.getRandomName("siteId"));
        site.setTitle(RandomData.getRandomName("siteTitle"));
        site.setDescription(RandomData.getRandomName("siteDescription"));
        site.setVisibility(Visibility.PUBLIC);

        RestSiteModel createdSite = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingSite("").createSite(site);

        restClient.assertStatusCodeIs(HttpStatus.CREATED);

        createdSite.assertThat().field("id").is(site.getId())
                   .assertThat().field("title").is(site.getTitle())
                   .assertThat().field("description").is(site.getDescription())
                   .assertThat().field("visibility").is(site.getVisibility());
    }

}
