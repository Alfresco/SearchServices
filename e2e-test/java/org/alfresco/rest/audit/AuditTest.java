package org.alfresco.rest.audit;

import static org.hamcrest.Matchers.is;

import org.alfresco.rest.RestTest;
import org.alfresco.rest.core.RestRequest;
import org.alfresco.rest.core.RestResponse;
import org.alfresco.rest.core.RestWrapper;
import org.alfresco.rest.model.RestAuditAppModel;
import org.alfresco.rest.model.RestAuditAppModelsCollection;
import org.alfresco.rest.model.RestAuditEntryModel;
import org.alfresco.rest.model.RestAuditEntryModelsCollection;
import org.alfresco.utility.model.UserModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.testng.annotations.BeforeClass;

import com.jayway.restassured.RestAssured;

public abstract class AuditTest extends RestTest
{

    @Autowired
    protected RestWrapper restAPI;
    
    protected UserModel userModel,adminUser;
    protected RestAuditAppModelsCollection restAuditCollection;
    protected RestAuditAppModel restAuditAppModel;
    protected RestAuditEntryModel restAuditEntryModel;
    protected RestAuditEntryModelsCollection restAuditEntryCollection;
    protected RestAuditAppModel syncRestAuditAppModel;
    protected RestAuditAppModel taggingRestAuditAppModel;
    

    @BeforeClass(alwaysRun = true)
    public void dataPreparation() throws Exception
    {
        //Using two users, because audit API is designed for users with admin rights.
        userModel = dataUser.createRandomTestUser();
        adminUser=dataUser.getAdminUser();
        //GET /alfresco/service/api/audit/control to verify if Audit is enabled on the system.
        RestAssured.basePath = "";
        restAPI.configureRequestSpec().setBasePath(RestAssured.basePath);
        RestRequest request = RestRequest.simpleRequest(HttpMethod.GET, "alfresco/service/api/audit/control");
        RestResponse response = restAPI.authenticateUser(dataUser.getAdminUser()).process(request);
        response.assertThat().body("enabled", is(true));
        //GET /audit-applications and verify that there are audit applications in the system.
        restAuditCollection = restClient.authenticateUser(dataUser.getAdminUser()).withCoreAPI().usingAudit().getAuditApplications();
        restClient.assertStatusCodeIs(HttpStatus.OK);
        restAuditCollection.assertThat().entriesListIsNotEmpty();
        //
        int i=0;
        do
        {
            restAuditAppModel = restAuditCollection.getEntries().get(i++).onModel();
        }while (!restAuditAppModel.getName().equals("alfresco-access"));

    }
    
    protected RestAuditAppModel getSyncRestAuditAppModel(UserModel userModel) throws Exception
    {
	    restAuditCollection = restClient.authenticateUser(userModel).withCoreAPI().usingAudit().getAuditApplications();
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    restAuditCollection.assertThat().entriesListIsNotEmpty();
	    RestAuditAppModel syncRestAuditAppModel = restAuditCollection.getEntries().get(0).onModel();
		return syncRestAuditAppModel;    	
    }
    
    protected RestAuditAppModel getTaggingRestAuditAppModel(UserModel userModel) throws Exception
    {
	    restAuditCollection = restClient.authenticateUser(userModel).withCoreAPI().usingAudit().getAuditApplications();
	    restClient.assertStatusCodeIs(HttpStatus.OK);
	    restAuditCollection.assertThat().entriesListIsNotEmpty();
	    RestAuditAppModel taggingRestAuditAppModel = restAuditCollection.getEntries().get(1).onModel();
		return taggingRestAuditAppModel;    	
    }

}
