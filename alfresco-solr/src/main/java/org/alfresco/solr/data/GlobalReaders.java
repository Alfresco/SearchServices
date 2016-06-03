/*
 * Copyright (C) 2005-2014 Alfresco Software Limited.
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
package org.alfresco.solr.data;

import java.util.HashSet;

import org.alfresco.repo.security.authentication.AuthenticationUtil;
import org.alfresco.service.cmr.security.PermissionService;

/**
 * Statically configured set of authorities that always have read access.
 * 
 * @author Matt Ward
 */
public class GlobalReaders
{
    private static HashSet<String> readers = new HashSet<String>();
    
    static
    {        
        readers.add(PermissionService.OWNER_AUTHORITY);
        readers.add(PermissionService.ADMINISTRATOR_AUTHORITY);
        readers.add(AuthenticationUtil.getSystemUserName());
    }
    
    public static HashSet<String> getReaders()
    {
        return readers;
    }
}
