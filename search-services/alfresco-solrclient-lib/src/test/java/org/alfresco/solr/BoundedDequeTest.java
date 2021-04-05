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

package org.alfresco.solr;

import static java.util.Arrays.asList;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.concurrent.LinkedBlockingDeque;

import org.junit.Test;

/** Unit tests for the {@link BoundedDeque}. */
public class BoundedDequeTest
{
	/** Check that earlier entries are removed from the BoundedDeque. */
	@Test
	public void testBoundedness()
	{
		// Create a BoundedDeque with size two.
		BoundedDeque<Object> boundedDeque = new BoundedDeque<>(2);

		// Add three things.
		boundedDeque.add("A");
		boundedDeque.add("B");
		boundedDeque.add("C");

		// Check that the latest two are still there.
		LinkedBlockingDeque<Object> actual = boundedDeque.getDeque();
		assertEquals("Unexpected entries in BoundedDeque.", asList("C", "B"), new ArrayList(actual));
	}
}
