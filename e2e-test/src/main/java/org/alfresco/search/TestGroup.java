package org.alfresco.search;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.METHOD })
public @interface TestGroup
{
    public static final String SANITY = "sanity";
    public static final String REGRESSION = "regression";
    public static final String ENTERPRISE = "enterprise";

  
    public static final String SEARCH = "search";
  
    public static final String CMIS = "cmis";
    public static final String REST_API = "rest-api";
  
    public static final String PREUPGRADE = "pre-upgrade";
    public static final String POSTUPGRADE = "post-upgrade";

    // Search: Minimum Version Required
    public static final String ASS_1 = "ASS_1.0.0"; // Alfresco Search Services 1.0. Does not work with Solr4
    public static final String ASS_112 = "ASS_1.1.2"; // Alfresco Search Services 1.1.2
    public static final String ASS_12 = "ASS_1.2.0"; // Alfresco Search Services 1.2
    public static final String PreASS_121 = "PreASS_1.2.1"; // Alfresco Search Services Prior to ASS 1.2.1
    public static final String ASS_121 = "ASS_1.2.1"; // Alfresco Search Services 1.2.1
    public static final String ASS_13 = "ASS_1.3.0"; // Alfresco Search Services 1.3
    public static final String ASS_1302 = "ASS_1.3.0.2"; // Alfresco Search Services 1.3.0.2 (Fingerprint MNT)
    public static final String ASS_14 = "ASS_1.4.0"; // Alfresco Search Services 1.4
    public static final String ASS_MASTER_SLAVE = "ASS_Master_Slave"; // Alfresco Search Services using master slave configurations
    public static final String ASS_MASTER ="ASS_Master"; // Alfresco search services using master/stand alone mode 
    
    public static final String INSIGHT_10 = "InsightEngine_1.0.0"; // Alfresco Insight Engine 1.0
    public static final String INSIGHT_11 = "InsightEngine_1.1.0"; // Alfresco Insight Engine 1.1
    public static final String INSIGHT_12 = "InsightEngine_1.2.0"; // Alfresco Insight Engine 1.2
    public static final String NOT_INSIGHT_ENGINE = "Not_InsightEngine"; // When Alfresco Insight Engine 1.0 isn't running
    public static final String SOLR = "SOLR"; //To be used for tests for /solr/alfresco/* end-points
    
    public static final String ACS_52n = "ACS_52n"; // Alfresco Content Services 5.2.n
    public static final String ACS_60n = "ACS_60n"; // Alfresco Content Services 6.0 or above
    public static final String ACS_61n = "ACS_61n"; // Alfresco Content Services 6.1 or above
}
