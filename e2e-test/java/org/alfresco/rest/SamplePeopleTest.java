package org.alfresco.rest;

import org.alfresco.utility.data.DataUser;
import org.alfresco.utility.exception.DataPreparationException;
import org.alfresco.utility.model.UserModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
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
    public void getPersonCheckResponseAndStatus() throws Exception
    {
        peopleAPI.getPerson(userModel.getUsername())
                    .assertResponseIsNotEmpty();        
        
        peopleAPI.usingRestWrapper()
                    .assertStatusCodeIs(HttpStatus.OK.toString());
    }

    @Test
    public void getPersonCheckStatusCode1() throws Exception
    {
        peopleAPI.getPerson(userModel.getUsername())
                    .assertPersonHasName(userModel.getUsername());
    }

}