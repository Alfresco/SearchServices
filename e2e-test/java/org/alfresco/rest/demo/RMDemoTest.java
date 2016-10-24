package org.alfresco.rest.demo;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.rest.requests.RestSitesApi;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.exception.TestConfigurationException;
import org.alfresco.utility.model.SiteModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.social.alfresco.api.entities.Site.Visibility;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

@Test(groups = { "demo" })
public class RMDemoTest extends RestTest {
  @Autowired
  RestSitesApi sitesApi;

  private SiteModel siteModel;

  @BeforeClass(alwaysRun = true)
  public void dataPreparation() throws DataPreparationException {
    siteModel = dataSite.usingAdmin().createPublicRandomSite();
    restClient.authenticateUser(dataUser.getAdminUser());

    sitesApi.useRestClient(restClient);
  }

  @Test
  public void adminCanSeeDetailsOfARandomSiteCreated() throws JsonToModelConversionException, Exception 
  {
    sitesApi.getSite(siteModel)
          .assertThat().field("id").isNotNull()
          .and().field("description").is(siteModel.getDescription())
          .and().field("title").is(siteModel.getTitle());
  }

  @Test
  public void adminCanSeeSiteDetailsOfCustomSite() throws JsonToModelConversionException, Exception 
  {
    siteModel = new SiteModel("MyTitle");
    siteModel.setDescription("my description");
    siteModel.setVisibility(Visibility.PUBLIC);

    // here we create the custom siteModel defined above
    dataSite.usingAdmin().createSite(siteModel);

    sitesApi.getSite(siteModel)
              .assertThat().field("id").isNotNull()
              .and().field("description").is("my description")
              .and().field("title").is("MyTitle")
              .and().field("visibility").is(Visibility.PUBLIC);
  }

  /**
   * This will throw this error message:
   * "You missed some configuration settings in your tests: You try to assert field [fieldA] that
   * doesn't exist in class: [org.alfresco.rest.model.RestSiteModel]. Please check your code!"
   */
  @Test(expectedExceptions = TestConfigurationException.class)
  public void assertingWithFieldsThatDoesNotExist() throws JsonToModelConversionException, Exception {
    siteModel = dataSite.createPublicRandomSite();

    sitesApi.getSite(siteModel).assertThat().field("fieldA").isNotNull();
  }
  
  @Test
  public void assertingPaginationAndUsingParameters() throws Exception
  {
    /*
     * ~ each API has the possibility to pass parameters to httpMethod call
     * you can use withParams(String... parameters) method before calling the http method.
     */
    sitesApi.withParams("maxItems=2", "orderyBy=name").getSites()
            .assertThat().paginationExist()
            .and().paginationField("maxItems").is("2")
            .and().paginationField("skipCount").is("0");
      
  }
  
}