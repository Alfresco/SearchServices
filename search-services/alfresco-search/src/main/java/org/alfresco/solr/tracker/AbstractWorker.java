/*-
 * #%L
 * Alfresco Solr Search
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
package org.alfresco.solr.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Synchronous Tracking Worker.
 * 
 * @author aborroy
 *
 */
public abstract class AbstractWorker
{
    protected final static Logger LOGGER = LoggerFactory.getLogger(AbstractWorker.class);

    public void run()
    {
        boolean failed = true;
        Exception failCausedBy = null;
        try
        {
            doWork();
            failed = false;
        }
        catch (Exception e)
        {
            LOGGER.warn("Index tracking batch hit an unrecoverable error ", e);
            failCausedBy = e;
        }
        finally
        {
            if (failed)
            {
                onFail(failCausedBy);
            }
        }
    }

    abstract protected void doWork() throws Exception;

    abstract protected void onFail(Throwable failCausedBy);
}
