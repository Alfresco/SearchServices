package org.alfresco.rest.audit;

import static org.hamcrest.Matchers.is;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestAuditAppModelsCollection;
import org.alfresco.utility.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.testng.annotations.BeforeClass;

import com.jayway.restassured.RestAssured;

public abstract class AuditTest extends RestTest
{

    @Autowired
    protected RestWrapper restAPI;
    
    protected UserModel userModel;
    protected RestAuditAppModelsCollection restAuditCollection;

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        userModel = dataUser.createRandomTestUser();
        RestAssured.basePath = "";
        restAPI.configureRequestSpec().setBasePath(RestAssured.basePath);
        RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "alfresco/service/api/audit/control");
        RestResponse response = restAPI.authenticateUser(dataUser.getAdminUser()).process(request);
        response.assertThat().body("enabled", is(true));
    }
}
