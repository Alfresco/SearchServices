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
package org.alfresco.solr.tracker.pool;

import java.util.Properties;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.alfresco.util.DynamicallySizedThreadPoolExecutor;
import org.alfresco.util.TraceableThreadFactory;

/**
 * Implementation of the {@link TrackerPoolFactory}.
 * 
 * @author Matt Ward
 */
public class DefaultTrackerPoolFactory implements TrackerPoolFactory
{
    private static final int DEFAULT_CORE_POOL_SIZE = 4;

    private static final int DEFAULT_MAXIMUM_POOL_SIZE = -1; // -1 is a sign that it must match the core pool size

    private static final int DEFAULT_KEEP_ALIVE_TIME = 120; // seconds

    private static final int DEFAULT_THREAD_PRIORITY = Thread.NORM_PRIORITY;

    private static final boolean DEFAULT_THREAD_DAEMON = Boolean.TRUE;

    private static final int DEFAULT_WORK_QUEUE_SIZE = -1;
    
    private static final RejectedExecutionHandler DEFAULT_REJECTED_EXECUTION_HANDLER = new ThreadPoolExecutor.CallerRunsPolicy();
    
    private String poolName = "";

    private int corePoolSize = DEFAULT_CORE_POOL_SIZE;

    private int maximumPoolSize = DEFAULT_MAXIMUM_POOL_SIZE;

    private int keepAliveTime = DEFAULT_KEEP_ALIVE_TIME;

    private int threadPriority = DEFAULT_THREAD_PRIORITY;

    private boolean threadDaemon = DEFAULT_THREAD_DAEMON;

    private int workQueueSize = DEFAULT_WORK_QUEUE_SIZE;
    
    private RejectedExecutionHandler rejectedExecutionHandler = DEFAULT_REJECTED_EXECUTION_HANDLER;
    
    private Integer parseConfig(String trackerConfig, Properties p, Integer ori)
    {
        if(p.getProperty(trackerConfig) != null && !p.getProperty(trackerConfig).isEmpty())
        {
            return  Integer.parseInt(p.getProperty(trackerConfig));
        }
        return ori;
    }
    
    private Boolean parseConfigBoolean(String trackerConfig, Properties p)
    {
        if(p.getProperty(trackerConfig) !=null && !p.getProperty(trackerConfig).isEmpty())
        {
            return Boolean.parseBoolean(p.getProperty(trackerConfig));
        }
        String param = "alfresco" + trackerConfig.substring(trackerConfig.lastIndexOf("."));
        return Boolean.parseBoolean(p.getProperty(param,"true"));
    }

    public DefaultTrackerPoolFactory(Properties p, String coreName, String trackerName)
    {
        corePoolSize = Integer.parseInt(p.getProperty("alfresco.corePoolSize", "4"));
        maximumPoolSize = Integer.parseInt(p.getProperty("alfresco.maximumPoolSize", "-1"));
        keepAliveTime = Integer.parseInt(p.getProperty("alfresco.keepAliveTime", "120"));
        threadPriority = Integer.parseInt(p.getProperty("alfresco.threadPriority", "5"));
        threadDaemon = Boolean.parseBoolean(p.getProperty("alfresco.threadDaemon", "true"));
        workQueueSize = Integer.parseInt(p.getProperty("alfresco.workQueueSize", "-1"));
        if(trackerName != null && !trackerName.isEmpty())
        {
            switch (trackerName)
            {
            case "AclTracker":
                corePoolSize = parseConfig("alfresco.acl.tracker.corePoolSize", p, corePoolSize);
                 maximumPoolSize = parseConfig("alfresco.acl.tracker.maximumPoolSize", p, maximumPoolSize);
                keepAliveTime = parseConfig("alfresco.acl.tracker.keepAliveTime", p, keepAliveTime);
                threadPriority = parseConfig("alfresco.acl.tracker.threadPriority", p, threadPriority);
                threadDaemon = parseConfigBoolean("alfresco.acl.tracker.threadDaemon", p);
                workQueueSize = parseConfig("alfresco.acl.tracker.workQueueSize", p, workQueueSize);
                break;
            case "ContentTracker":
                corePoolSize = parseConfig("alfresco.content.tracker.corePoolSize", p, corePoolSize);
                maximumPoolSize = parseConfig("alfresco.content.tracker.maximumPoolSize", p, maximumPoolSize);
                keepAliveTime = parseConfig("alfresco.content.tracker.keepAliveTime", p, keepAliveTime);
                threadPriority = parseConfig("alfresco.content.tracker.threadPriority", p, threadPriority);
                threadDaemon = parseConfigBoolean("alfresco.content.tracker.threadDaemon", p);
                workQueueSize = parseConfig("alfresco.content.tracker.workQueueSize", p, workQueueSize);
                break;
            case "MetadataTracker":
                corePoolSize = parseConfig("alfresco.metadata.tracker.corePoolSize", p, corePoolSize);
                maximumPoolSize = parseConfig("alfresco.metadata.tracker.maximumPoolSize", p, maximumPoolSize);
                keepAliveTime = parseConfig("alfresco.metadata.tracker.keepAliveTime", p, keepAliveTime);
                threadPriority = parseConfig("alfresco.metadata.tracker.threadPriority", p, threadPriority);
                threadDaemon = parseConfigBoolean("alfresco.metadata.tracker.threadDaemon", p);
                workQueueSize = parseConfig("alfresco.metadata.tracker.workQueueSize", p, workQueueSize);
                break;
            default:
                break;
            }
        }

        
        poolName = "SolrTrackingPool-" + coreName + "-" + trackerName + "-";

        // if the maximum pool size has not been set, change it to match the core pool size
        if (maximumPoolSize == DEFAULT_MAXIMUM_POOL_SIZE)
        {
            maximumPoolSize = corePoolSize;
        }
    }
    
    @Override
    public ThreadPoolExecutor create()
    {
        // We need a thread factory
        TraceableThreadFactory threadFactory = new TraceableThreadFactory();
        threadFactory.setThreadDaemon(threadDaemon);
        threadFactory.setThreadPriority(threadPriority);

        if (poolName.length() > 0)
        {
            threadFactory.setNamePrefix(poolName);
        }

        BlockingQueue<Runnable> workQueue;
        if (workQueueSize < 0)
        {
            // We can have an unlimited queue, as we have a sensible thread pool!
            workQueue = new LinkedBlockingQueue<Runnable>();
        }
        else
        {
            // Use an array one for consistent performance on a small queue size
            workQueue = new ArrayBlockingQueue<Runnable>(workQueueSize);
        }
        
        
        ThreadPoolExecutor threadPoolExecutor = new DynamicallySizedThreadPoolExecutor(
                    corePoolSize, maximumPoolSize, keepAliveTime, TimeUnit.SECONDS,
                    workQueue, threadFactory, rejectedExecutionHandler);

        return threadPoolExecutor;
    }
}
