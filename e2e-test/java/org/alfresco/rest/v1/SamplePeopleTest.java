package org.alfresco.rest.v1;

import org.alfresco.rest.RestPeopleApi;
import org.alfresco.rest.exception.JsonToModelConversionException;
import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.UserModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class SamplePeopleTest extends RestTest
{
    @Autowired
    RestPeopleApi peopleAPI;

    @Autowired
    DataUser dataUser;

    private UserModel userModel;

    @BeforeClass
    public void setUp() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
        restClient.authenticateUser(userModel);
        peopleAPI.useRestClient(restClient);
    }

    @Test
    public void getPerson() throws JsonToModelConversionException
    {
        Assert.assertNotNull(peopleAPI.getPerson(userModel.getUsername()), "Get person response should not be null");
        Assert.assertEquals(peopleAPI.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get person response status code is not correct");
    }

    @Test
    public void getPersonResponseNotNull() throws JsonToModelConversionException
    {
        Assert.assertNotNull(peopleAPI.getPerson(userModel.getUsername()), "Get person response should not be null");
    }

    @Test
    public void getPersonCheckStatusCode() throws JsonToModelConversionException
    {
        peopleAPI.getPerson(userModel.getUsername());
        Assert.assertEquals(peopleAPI.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get person response status code is not correct");
    }

    @Test
    public void getPersonResponseNotNull1() throws JsonToModelConversionException
    {
        Assert.assertNotNull(peopleAPI.getPerson(userModel.getUsername()), "Get person response should not be null");
    }

    @Test
    public void getPersonCheckStatusCode1() throws JsonToModelConversionException
    {
        peopleAPI.getPerson(userModel.getUsername());
        Assert.assertEquals(peopleAPI.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get person response status code is not correct");
    }

}