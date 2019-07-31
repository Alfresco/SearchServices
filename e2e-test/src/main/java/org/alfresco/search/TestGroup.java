package org.alfresco.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface TestGroup
{
	// Used for TestRail test annotation
    String SEARCH = "search";
    String REST_API = "rest-api";
  
    String PREUPGRADE = "pre-upgrade";
    String POSTUPGRADE = "post-upgrade";

    String ASS_MASTER_SLAVE = "ASS_Master_Slave"; // Alfresco Search Services using master slave configurations
    String ASS_MASTER ="ASS_Master"; // Alfresco search services using master/stand alone mode
    String EXPLICIT_SHARDING ="Explicit_Sharding"; // Alfresco search services using sharded environment and explicit routing

    String NOT_INSIGHT_ENGINE = "Not_InsightEngine"; // When Alfresco Insight Engine 1.0 isn't running
    
    String ACS_52n = "ACS_52n"; // Alfresco Content Services 5.2.n
    String ACS_60n = "ACS_60n"; // Alfresco Content Services 6.0 or above
    String ACS_61n = "ACS_61n"; // Alfresco Content Services 6.1 or above
    String ACS_611n = "ACS_611n"; // Alfresco Content Services 6.1.1 or above
    String ACS_62n = "ACS_62n"; // Alfresco Content Services 6.2 or above

    String NOT_BAMBOO = "Not_Bamboo"; // The does not run on bamboo

}
