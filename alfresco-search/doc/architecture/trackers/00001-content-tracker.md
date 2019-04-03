## Content Tracker

![Completeness Badge](https://img.shields.io/badge/Document_Level-In_Progress-yellow.svg?style=flat-square)

### Purpose
The _ContentTracker_ queries for documents with "unclean" content (that is, data whose content has been modified in Alfresco), and then updates them.
Periodically, at a configurable frequency, the ContentTracker checks for transactions containing data that has been marked as "Dirty" (changed) or "New". 
Then, 

- it retrieves the cached version of that data from the ContentStore
- it retrieves the text content from Alfresco 
- it updates the ContentStore 
- it re-indexes the data in Solr

The _ContentTracker_ is part of the Alfresco Tracker Subsystem which is composed by the following members: 

- **ModelTracker**: for listening on model changes  
- **ContentTracker**: described in this document
- **MetadataTracker**: for tracking changes in transactions and metadata nodes
- **AclTracker**: for listening on ACLs changes
- **CascadeTracker**: which manages cascade updates (i.e. updates related with the nodes hierarchy)
- **CommitTracker**: which provides cross-cutting commit / rollback capabilities

Each running Solr (regardless it holds a monolithic core or a shard) has a singleton instance for each tracker type which is 
registered, configured and scheduled at _SolrCore_ startup. The SolrCore holds a _TrackerRegistry_ which contains those "active" instances.

*** 

### Technical Overview 
The [Tracker Class Diagram Overview](trackers_class_diagram_overview.png) provides a high-level overview about the main classes involved in the Tracker subsystem.
The most part of the Tracker classes are in the org.alfresco.solr.tracker package. 

As you can see from the diagram, there's an abstract interface definition (_Tracker_) which declares what is expected by a Tracker 
and a Layer Supertype (_AbstractTracker_) which adopts a TemplateMethod [1] approach. It provides the common behavior and features inherited by all trackers, mainly in terms of: 

- Configuration (see the section below)
- State definition (e.g. isSlave, isMaster, isInRollbackMode, isShutdown)
- Constraints (e.g. there must be only one running instance of a given tracker in a given moment)
- Locking: the two Semaphore instances depicted in the diagram used for implementing the constraint described in the previous point and for providing an inter-trackers synchronization mechanism. 

The _Tracker_ behavior is defined in the track() method that each tracker must implement. 
As said above, the _AbstractTracker_ forces a common behavior on all trackers by implementing a final version of that method, 
and then it delegates to the concrete trackers (subclasses) the specific logic by requiring them the implementation of the _doTrack_() method.

Each tracker is a stateful object which is initialized, registered in a TrackerRegistry and scheduled at startup in the SolrCoreLoadRegistration class. 
The other relevant classes depicted in the diagram are: 

- **SolrCore**: the dashed dependency relationship means that a Tracker doesn't hold a stable reference to the SolrCore: It obtains that reference each time it's needed. 
- **ThreadHandler**: The ThreadExecutionPool manager which holds a pool of threads needed for scheduling asynchronous tasks (i.e. unclean content reindexing) 
- **TrackerState**: being a shared instance across all trackers, it would have been called something like _TrackersState_ or _TrackerSubsystemState_. It is used for holding the **trackers** state (e.g. lastTxIdOnServer, trackerCycles, lastStartTime)
- **TrackerStats**: maintains a global stats about all trackers. Following the same approach of the TrackerState, it is a shared instance and therefore the name is a little bit misleading because it is related to all trackers
- **SOLRAPIClient**: this is the HTTP proxy / facade towards Alfresco REST API: in the sequence diagrams these interactions are depicted in green
- **SolrInformationServer**: The Solr binding of the InformationServer interface, which defines an abstract definition of the underlying search infrastructure 


### Startup and Shutdown 
The trackers startup and registration flow is depicted in the [StartupAndRegistration](trackers_startup_and_registration.png) sequence diagram. 
Solr provides, through the interface _SolrEventListener_, a notification mechanism for registering custom plugins during a _SolrCore_ lifecycle. 
The Tracker Subsystem is initialized, configured and scheduled in the _SolrCoreLoadListener_ which delegates the concrete work to _SolrCoreLoadRegistration_. 
Here, a new instance of each tracker is created, configured, registered and then scheduled by means of a Quartz Scheduler. 
Trackers can share a common frequency (as defined in the _alfresco.cron_ property) or they can have a specific configuration (e.g. _alfresco.content.tracker.cron_).

The _SolrCoreLoadRegistration_ also registers a shutdown hook which makes sure all registered trackers will follow the _SolrCore_ lifecycle. 

*** 

### Content Tracking
The [ContentTracking](content-tracking.png) sequence diagram details what happens in a single tracking task executed by the _ContentTracker_.
At a given frequency (which again, can be the same for each tracker or overriden per tracker type) the Quartz Scheduler invokes the doTrack() method of the ContentTracker. 
Prior to that, the logic in the AbstractTracker is executed following the TemplateMethod [1] described above; specifically the "Running" lock is acquired and the tracker is put in a "Running" state. 

The at high level, the ContentTracker does the following:

- get documents with "unclean" content
- if that list is not empty, each document is scheduled (asynchronously) for being updated, in the content store and in the index

In order to do that, the _ContentTracker_ never uses directly the proxy towards ACS (i.e. SOLRAPIClient); instead, it delegates that logic to the _SolrInformationServer_ instance. 
The first step (_getDocsWithUncleanContent_) searches in the local index all transactions which are associated to documents that have been marked as "Dirty" or "New". 
The field where this information is recorded is FTSSTATUS; it could have one of the following values: 

- **Dirty**: content has been updated / changed
- **New**: content is new
- **Clean**: content is up to date, there's no need to refresh it

The "dirty" documents are returned as triples containing the tenant, the ACL identifier and the DB identifier (see the _org.alfresco.solr.AlfrescoSolrDataModel.TenantAclIdDbId_ class).  

_NOTE: this first phase uses only the local index, no remote call is involved._

If the list of Tenant/ACLID/DBID triples is not empty, that means we need to fetch and update the text content of the corresponding documents. 
In order to do that, each document is wrapped in a _Runnable_ object (see _ContentTracker.ContentIndexWorkerRunnable_ inner class) and submitted to a thread pool executor. 
That makes each document content processing asynchronous.

The _ContentIndexWorkerRunnable_ once executed, delegates the actual update to the _SolrInformationServer_ which, as said above, contains the logic needed for dealing with the Solr infrastructure.
In the _SolrInformationServer::updateContentToIndexAndCache_ 

- the document that needs to be refreshed, uniquely identified by the tenant and the db identifier, is retrieved from the local content store. In case the cached document cannot be found in the content store, the _/api/solr/metadata_ remote API is contacted in order to rebuild the document (only metadata) from scratch. 
- the _api/solr/textContent_ is called in order to fetch the text content associated with the node, plus the transformation metadata (e.g, status, exception, elapsed time)
- if the _alfresco.fingerprint_ configuration property is set to true and the retrieved text is not empty the fingerprint is computed and stored in the MINHASH field of the document
- the content fields are set
- the document is marked as clean (i.e. FTSSTATUS = "Clean") since its content is now up to date
- the cached version is overwritten in the content store with the up to date definition
- the document (which is a _SolrInputDocument_ instance) is indexed in Solr

### Rollback
The [Rollback](trackers_rollback_sequence.png) sequence diagram illustrates how the rollback process works. 
The commit/rollback process is a responsibility of the _CommitTracker_, so the _ContentTracker_ is involved in these processes only indirectly.

When it is executed, the _CommitTracker_ acquires the execution locks from the _MetadataTracker_ and the _AclTracker_. Then it 
checks if one of them is in a rollback state. As we can imagine, that check will return true if some unhandled exception has occurred during indexing.

If one of the two trackers above reports an active rollback state, the CommitTracker lists all trackers, invalidates their state and issues a rollback command to Solr. 
That means any update sent to Solr by any tracker will be reverted. 

### How does the ContentTracker work in shard mode?
The only source that the _ContentTracker_ checks in order to determine the "unclean" content that needs to be updated is the local index. 
As consequence of that, the _ContentTracker_ behavior is the same regardless the search infrastructure shape and the context where the hosting Solr instance lives. 
That is, if we are running a standalone Solr instance there will be one a _ContentTracker_ for each core watching the corresponding (monolithic) index. 
If instead we are in a sharded scenario, each shard will have a _ContentTracker_ instance that will use the local shard index. 

*** 

### How does the ContentTracker work in a Master/Slave context?
In order to properly work in a Master/Slave infrastructure, the Tracker Subsystem (not the only ContentTracker) needs to be

- enabled on Master(s)
- disabled on Slaves

The only exceptions to that rule are about: 

- The **MetadataTracker**: only if the search infrastructure uses dynamic sharding [2] the Metadata tracker is in charge 
to register the Solr instance (the Shard) to Alfresco so it will be included in the subsequent queries. The tracker itself, in this scenario, won't track anything.
- The **ModelTracker**: each Solr instance pulls, by means of this tracker, the custom models from Alfresco, so it must be enabled in any case.

*** 

### Configuration
The Tracker Subsystem can be configured through the _solrcore.properties_ configuration file which can be found within the core configuration folder.   
The following table illustrates the configuration properties used by the Tracker Subsystem. For each property the table indicates the default value (if exists), a brief description and the trackers that directly or indirectly use that property. 

| Property                  | Default    | Description                         | ModelTracker| ContentTracker | MetadataTracker| AclTracker | CommitTracker| CascadeTracker|      
| --------------------------|:----------:|------------------------------------|------------:|:-----------:|:-----------:|:-----------:|:-----------:|:-----------:|
|enable.alfresco.tracker    |false       |Enables or disables the tracking subsystem|Y|Y|Y|Y|Y|Y|
|alfresco.cron              |0/10 * * * * ? *|Trackers default cron expression.|Y|Y|Y|Y|Y|Y|
|alfresco.acl.tracker.cron  |                |ACL Tracker cron expression| | | |Y| | |
|alfresco.model.tracker.cron|                |Model Tracker cron expression|Y | | || | |
|alfresco.content.tracker.cron|                |Content Tracker cron expression| |Y| | | | |
|alfresco.metadata.tracker.cron|                |Metadata Tracker cron expression| | |Y| | | |
|alfresco.cascade.tracker.cron|                |Cascade Tracker cron expression| | | | | |Y|
|alfresco.commit.tracker.cron|                |Content Tracker cron expression| | | | |Y| |
|alfresco.stores|workspace://SpacesStore|The reference to a node store| | |Y|Y| | |
|batch.count|5000|UpSert batch size (e.g. metadata docs, acls)| | |Y|Y| | |
|alfresco.maxLiveSearchers|2|Max allowed number of active searchers|Y| |Y|Y| | |
|enable.slave|false|Indicates if the hosting instance is a slave| | |Y|Y| | |
|enable.master|true|Indicates if the hosting instance is a master| | |Y|Y| | |
|shard.count|1|The total number of shards that compose the Solr infrastructure|| |Y|Y| | |
|shard.instance|0|The unique shard identifier assigned to this instance|| |Y|Y| | |
|shard.method|"DB_ID"|Data (Documents, ACLs) Routing criteria among shards| | |Y|Y| | |
|alfresco.fingerprint|true|true if we want to compute the content Fingerprint| | |Y|| | |
|alfresco.index.transformContent|true| | | |Y|| | |
|alfresco.version|5.0.0|The target Alfresco version| | | | | | |
|alfresco.corePoolSize|4|The number of threads to keep in the pool, even if they are idle|Y|Y|Y|Y|Y|Y|
|alfresco.maximumPoolSize|-1|The maximum number of threads allowed in the pool|Y|Y|Y|Y|Y|Y|
|alfresco.keepAliveTime|120|When the number of threads is greater than the core pool size, this is the maximum time that excess idle threads will wait for new tasks before terminating|Y|Y|Y|Y|Y|Y|
|alfresco.threadPriority|5|The thread priority assigned to threads in the pool|Y|Y|Y|Y|Y|Y|
|alfresco.threadDaemon|true|Thread type (daemon/user) assigned to threads in the pool|Y|Y|Y|Y|Y|Y|
|alfresco.workQueueSize|-1|ACLTracker specific configuration. See above|Y|Y|Y|Y|Y|Y|
|alfresco.acl.tracker.corePoolSize| |ACL Tracker specific configuration. See above| | | |Y| | |
|alfresco.acl.tracker.maximumPoolSize| |ACL Tracker specific configuration. See above| | | |Y| | |
|alfresco.acl.tracker.keepAliveTime| |ACL Tracker specific configuration. See above| | | |Y| | |
|alfresco.acl.tracker.threadPriority| |ACL Tracker specific configuration. See above| | | |Y| | |
|alfresco.acl.tracker.threadDaemon| |ACL Tracker specific configuration. See above| | | |Y| | |
|alfresco.acl.tracker.workQueueSize| |ACL Tracker specific configuration. See above| | | |Y| | |
|alfresco.content.tracker.corePoolSize| |Content Tracker specific configuration. See above| |Y| | | | |
|alfresco.content.tracker.maximumPoolSize| |Content Tracker specific configuration. See above| |Y| | | | |
|alfresco.content.tracker.keepAliveTime| |Content Tracker specific configuration. See above| |Y| | | | |
|alfresco.content.tracker.threadPriority| |Content Tracker specific configuration. See above| |Y| | | | |
|alfresco.content.tracker.threadDaemon| |Content Tracker specific configuration. See above| |Y| | | | |
|alfresco.content.tracker.workQueueSize| |Content Tracker specific configuration. See above| |Y| | | | |
|alfresco.metadata.tracker.corePoolSize| |Metadata Tracker specific configuration. See above| | |Y| | | |
|alfresco.metadata.tracker.maximumPoolSize| |Metadata Tracker specific configuration. See above| | |Y| | | |
|alfresco.metadata.tracker.keepAliveTime| |Metadata Tracker specific configuration. See above| | |Y| | | |
|alfresco.metadata.tracker.threadPriority| |Metadata Tracker specific configuration. See above| | |Y| | | |
|alfresco.metadata.tracker.threadDaemon| |Metadata Tracker specific configuration. See above| | |Y| | | |
|alfresco.metadata.tracker.workQueueSize| |Metadata Tracker specific configuration. See above| | |Y| | | |


***
[1] [Template Method](https://en.wikipedia.org/wiki/Template_method_pattern) 
[2] [Dynamic Sharding](http://docs.alfresco.com/5.1/concepts/solr-shard-config.html)