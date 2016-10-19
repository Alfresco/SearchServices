package org.alfresco.rest.model;

import org.alfresco.rest.core.ModelAssertion;
import org.alfresco.utility.exception.TestConfigurationException;
import org.testng.annotations.Test;

public class ModelAssertionTest {
  @Test
  public void iCanAssertExistingProperty() throws Exception {
    Person p = new Person();
    p.and().assertField("id").is("1234");
  }

  @Test
  public void iCanAssertExistingPropertyNegative() throws Exception {
    Person p = new Person();
    p.and().assertField("id").isNot("12342");
    RestPersonModel rp = new RestPersonModel();
    
    rp.getFirstName();
  }

  @Test(expectedExceptions = TestConfigurationException.class)
  public void iHaveOneExceptionThrownWithSelfExplanatoryMessageOnMissingField() throws Exception {
    Person p = new Person();
    p.and().assertField("id2").is("12342");

  }

  @Test
  public void iCanTakeTheValueOfFieldsThatDoesntHaveGetters() throws Exception {
    Person p = new Person();

    p.and().assertField("name").is("test");

  }

  public class Person {
    private String id = "1234";

    @SuppressWarnings("unused")
    private String name = "test";

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }
    
    public ModelAssertion<Person> and()
    {
      return new ModelAssertion<Person>(this);
    }
  }

}
