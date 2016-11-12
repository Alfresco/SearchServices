package org.alfresco.rest;

import org.alfresco.rest.core.RestProperties;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.utility.data.DataUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import com.jayway.restassured.RestAssured;

@ContextConfiguration("classpath:alfresco-restapi-context.xml")
public class MySampleDSL extends AbstractTestNGSpringContextTests {
  @Autowired
  RestWrapper restAPI;
  
  @Autowired
  DataUser dataUser;

  @Autowired
  protected RestProperties restProperties;

  @BeforeClass
  public void asd() {
    RestAssured.baseURI = restProperties.envProperty().getTestServerUrl();
    RestAssured.port = restProperties.envProperty().getPort();
    RestAssured.basePath = restProperties.getRestBasePath();
  }

  @Test
  public void something() throws Exception {   
    // GET /Sites/<hahaha>/containers
 
    
    restAPI.authenticateUser(dataUser.getAdminUser()).usingSite("workshop").getContainers().assertThat().entriesListIsNotEmpty();
    
    
//    restAPI.usingSite("hahaha").getContainers().assertThat().field("level1").field("level2").field("level3").is("b")
//    restAPI.usingSite("hahaha").getContainers().assertThat().field("level3").is("a");
//    restAPI.usingSite("hahaha").getContainers().assertThat().listIsNotEmpty()
//                                                            .listContains(level)
//                                                            .statusCode().is(HttpStatus.OK);
//                                                            
//    
//    
//    //GET /sites/{siteId}/containers/{containerId}
//    
//    restAPI.usingSite("haha").getContainer("c1").assertThat().file("folderId").is("jjsjsj");
//                                                             .statusCode().
//                                                             
//    
//    SiteModel site;
//    String body = JsonBodyGenerator.siteMemberhipRequest("Please accept me", site, "New request");
//    restAPI.usingPeople(people).withBody(body).addSiteMembershipRequest();
//    
//    
//    
//    
//    restAPI.usingPeople(people).withBody(body).addSiteMembershipRequest().assertthat().statusCode().is().and().fiels
//    restApi.getBodyResponse.
  }
}
