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
package org.alfresco.solr.tracker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

abstract class AbstractWorkerRunnable implements Runnable
{
	protected final static Logger log = LoggerFactory.getLogger(AbstractWorkerRunnable.class);
	
    QueueHandler queueHandler;
    
    public AbstractWorkerRunnable(QueueHandler qh)
    {
        this.queueHandler = qh;
    }
    
    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
    	boolean failed = true;
        try
        {
            doWork();
            failed = false;
        }
        catch (Exception e)
        {
        	log.warn("Index tracking batch hit an unrecoverable error ", e);
        }
        finally
        {
            // Triple check that we get the queue state right
            queueHandler.removeFromQueueAndProdHead(this);
            if(failed)
            {
            	onFail();
            }
        }
    }
    
    abstract protected void doWork() throws Exception;
    
    abstract protected void onFail();
}
