package people;

import org.alfresco.rest.BaseRestTest;
import org.alfresco.rest.v1.RestPeople;
import org.alfresco.tester.data.DataUser;
import org.alfresco.tester.exception.DataPreparationException;
import org.alfresco.tester.model.UserModel;
import org.apache.commons.lang3.RandomStringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

public class PeopleTest extends BaseRestTest
{
    @Autowired
    RestPeople onPeople;

    @Autowired
    DataUser dataUser;

    private UserModel userModel;

    @BeforeClass
    public void setUp() throws DataPreparationException
    {
        userModel = dataUser.createUser(RandomStringUtils.randomAlphanumeric(20));
    }

    @Test
    public void getPersonResponseNotNull()
    {
        Assert.assertNotNull(onPeople.withAuthUser(userModel).getPerson(userModel.getUsername()), "Get person response should not be null");
    }

    @Test
    public void getPersonCheckStatusCode()
    {
        onPeople.withAuthUser(userModel).getPerson(userModel.getUsername());
        Assert.assertEquals(onPeople.usingRestWrapper().getStatusCode(), HttpStatus.OK.toString(), "Get person response status code is not correct");
    }

}