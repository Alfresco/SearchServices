package org.alfresco.rest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.testng.AbstractTestNGSpringContextTests;
import org.testng.annotations.BeforeClass;

import com.jayway.restassured.RestAssured;

@ContextConfiguration("classpath:alfresco-restapi-context.xml")
public class BaseRestTest extends AbstractTestNGSpringContextTests {

	@Autowired
	protected RestProperties restProperties;

	@BeforeClass
	public void setup() {
		RestAssured.baseURI = restProperties.envProperty().getTestServerUrl();
		RestAssured.port = restProperties.envProperty().getPort();
		RestAssured.basePath = restProperties.getRestBasePath();
	}
}
