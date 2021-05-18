/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2020 Alfresco Software Limited
 * %%
 * This file is part of the Alfresco software. 
 * If the software was purchased under a paid Alfresco license, the terms of 
 * the paid license agreement will prevail.  Otherwise, the software is 
 * provided under the following open source license terms:
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
 * #L%
 */

package org.alfresco.solr.cache;

/**
 * Constants for per-searcher cache names and keys.
 * 
 * @author Matt Ward
 */
public class CacheConstants
{
    // Cache names
//    public static String ALFRESCO_CACHE = "alfrescoCache";
//    public static String ALFRESCO_ARRAYLIST_CACHE = "alfrescoArrayListCache";
//    public static String ALFRESCO_PATH_CACHE = "alfrescoPathCache";
//    public static String ALFRESCO_READER_TO_ACL_IDS_CACHE = "alfrescoReaderToAclIdsCache";
//    public static String ALFRESCO_DENY_TO_ACL_IDS_CACHE = "alfrescoDenyToAclIdsCache";

    public final static String ALFRESCO_AUTHORITY_CACHE = "alfrescoAuthorityCache";
    public final static String ALFRESCO_OWNERLOOKUP_CACHE = "alfrescoOwnerCache";
    public final static String ALFRESCO_READER_CACHE = "alfrescoReaderCache";
    public final static String ALFRESCO_DENIED_CACHE = "alfrescoDeniedCache";
    public final static String ALFRESCO_PATH_CACHE = "alfrescoPathCache";
}
