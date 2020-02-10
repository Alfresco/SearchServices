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
    public static final String SEARCH = "search";
    public static final String REST_API = "rest-api";
  
    public static final String PREUPGRADE = "pre-upgrade";
    public static final String POSTUPGRADE = "post-upgrade";

    public static final String ASS_MASTER_SLAVE = "ASS_Master_Slave"; // Alfresco Search Services using master slave configurations
    public static final String ASS_MASTER ="ASS_Master"; // Alfresco Search Services using master/stand alone mode 
    public static final String ASS_SHARDING = "ASS_Sharding"; // Alfresco Search Services using Sharding
    public static final String ASS_SHARDING_DB_ID_RANGE = "ASS_Sharding_DB_ID_RANGE"; // Alfresco Search Services using Sharding with DB_ID_RANGE
    
    public static final String ACS_52n = "ACS_52n"; // Alfresco Content Services 5.2.n
    public static final String ACS_60n = "ACS_60n"; // Alfresco Content Services 6.0 or above
    public static final String ACS_61n = "ACS_61n"; // Alfresco Content Services 6.1 or above
    public static final String ACS_611n = "ACS_611n"; // Alfresco Content Services 6.1.1 or above
    public static final String ACS_62n = "ACS_62n"; // Alfresco Content Services 6.2 or above
    
}
