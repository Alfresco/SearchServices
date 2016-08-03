package org.alfresco.rest.v1;

import static org.alfresco.rest.RestClientWrapper.onRestAPI;

import org.alfresco.rest.BaseRestTest;
import org.alfresco.tester.model.UserModel;
import org.testng.annotations.Test;

public class SitesTest extends BaseRestTest {

	@Test
	public void getSite() {
		UserModel admin = new UserModel("admin", "admin");
		System.out.println(onRestAPI().withAuthUser(admin).onSites().getSite("SiteName1470151654495").getTitle());

	}
}
