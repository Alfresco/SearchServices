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

package org.alfresco.solr;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.alfresco.service.namespace.QName;
import org.junit.Test;

/**
 * @author Andy
 *
 */
public class SolrDataModelTest
{
    private static final QName CONTENT_STREAM_LENGTH = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}contentStreamLength");
    private static final QName IS_PRIVATE_WOKING_COPY = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}isPrivateWorkingCopy");
    private static final QName IS_IMMUTABLE = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}isImmutable");
    private static final QName CREATION_DATE = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}creationDate");
    private static final QName NAME = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}name");
    private static QName OBJECT_ID = QName.createQName("{http://www.alfresco.org/model/cmis/1.0/cs01}objectId");

    @Test
    public void testParseTransactionId()
    {
        Long expectedId = 94032903249l;
        String id = AlfrescoSolrDataModel.getTransactionDocumentId(expectedId);
        Long actualId = AlfrescoSolrDataModel.parseTransactionId(id);
        assertEquals(expectedId, actualId);
    }
}
