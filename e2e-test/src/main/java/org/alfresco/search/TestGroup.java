/*
 * Copyright (C) 2018 Alfresco Software Limited.
 *
 * This file is part of Alfresco
 *
 * Alfresco is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Alfresco is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Alfresco. If not, see <http://www.gnu.org/licenses/>.
 */
package org.alfresco.search;

public class TestGroup
{
    // Used for TestRail test annotation
    public static final String SEARCH = "search";
    public static final String REST_API = "rest-api";
  
    public static final String PREUPGRADE = "pre-upgrade";
    public static final String POSTUPGRADE = "post-upgrade";

    public static final String ASS_MASTER_SLAVE = "ASS_Master_Slave"; // Alfresco Search Services using master slave configurations
    public static final String ASS_MASTER ="ASS_Master"; // Alfresco search services using master/stand alone mode
    public static final String EXPLICIT_SHARDING ="Explicit_Sharding"; // Alfresco search services using sharded environment and explicit routing

    public static final String NOT_INSIGHT_ENGINE = "Not_InsightEngine"; // When Alfresco Insight Engine 1.0 isn't running
    
    public static final String ACS_52n = "ACS_52n"; // Alfresco Content Services 5.2.n
    public static final String ACS_60n = "ACS_60n"; // Alfresco Content Services 6.0 or above
    public static final String ACS_61n = "ACS_61n"; // Alfresco Content Services 6.1 or above
    public static final String ACS_611n = "ACS_611n"; // Alfresco Content Services 6.1.1 or above
    public static final String ACS_62n = "ACS_62n"; // Alfresco Content Services 6.2 or above

    public static final String NOT_BAMBOO = "Not_Bamboo"; // The does not run on bamboo
}
