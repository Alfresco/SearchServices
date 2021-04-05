/*
 * #%L
 * Alfresco Search Services
 * %%
 * Copyright (C) 2005 - 2021 Alfresco Software Limited
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

package org.alfresco.solr.tracker;

import static org.alfresco.solr.tracker.DocRouterFactory.SHARD_RANGE_KEY;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Properties;

import org.alfresco.error.AlfrescoRuntimeException;
import org.alfresco.repo.index.shard.ShardMethodEnum;
import org.junit.Test;

/** Unit tests for the {@link DocRouterFactory}. */
public class DocRouterFactoryTest
{
	/** Check that a DB_ID_RANGE router can be created. */
	@Test
	public void testDBIDRANGEWithShardRangeKey()
	{
		Properties mockProperties = mock(Properties.class);
		when(mockProperties.containsKey(SHARD_RANGE_KEY)).thenReturn(true);
		when(mockProperties.getProperty(SHARD_RANGE_KEY)).thenReturn("100000000-150000000");

		// Call the method under test.
		DocRouter docRouter = DocRouterFactory.getRouter(mockProperties, ShardMethodEnum.DB_ID_RANGE);

		assertTrue("Expected to get a DBIDRangeRouter.", docRouter instanceof DBIDRangeRouter);
		DBIDRangeRouter dbidRangeRouter = (DBIDRangeRouter) docRouter;
		assertEquals("Unexpected start of range.", dbidRangeRouter.getStartRange(), 100000000L);
		assertEquals("Unexpected end of range.", dbidRangeRouter.getEndRange(), 150000000L);
	}

	/** Check that an exception is raised if the range information is missing. */
	@Test(expected = AlfrescoRuntimeException.class)
	public void testDBIDRANGEWithoutShardRangeKey()
	{
		Properties mockProperties = mock(Properties.class);
		DocRouterFactory.getRouter(mockProperties, ShardMethodEnum.DB_ID_RANGE);
	}
}
