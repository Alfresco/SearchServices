## SolrContentStore Removal

### Status

![Completeness Badge](https://img.shields.io/badge/Document_Level-Completed-green.svg?style=flat-square)

### Context
SearchServices is a set of Alfresco specific customisations built on top of Apache Solr, a highly reliable, scalable and 
fault tolerant search platform.  
Apache Solr provides efficient search services on a given set of data composed by atomic units of work called "documents". 

Data managed in Alfresco that needs to be "searchable" must be sent to Solr for "indexing". In the indexing phase Solr 
stores the incoming data and organises it in an immutable data structure called "Inverted Index" plus some additional data 
structures needed for other complementary services offered by the platform (e.g. highlighting, storage, more like this).    

The following picture illustrates the content of the Solr data directory

![Apache Solr Data Directory](solr.data.dir.png) 

SearchServices adds a complementary and auxiliary data organisation structure, based on filesystem, called **Solr Content Store**.
The following picture illustrates the content and the structure of the Solr Content Store. 

![SolrContentStore](solrcontentstore.png)  

### What is the SolrContentStore?
The _SolrContentStore_ is a logical extension of the Apache Solr Index used by SearchServices for maintaining a verbatim copy of 
each incoming data. It is a local folder organised by tenant which contains all input documents indexed in Solr. 
Within that folder, each file   

- is organised hierarchically, under a root folder called "contentstore", and per tenant 
- represents a single document sent to Solr for indexing
- is compressed (.gz) and serialised. Specifically, it consists of the serialised form of a _SolrInputDocument_ instance,
 the Java class used by Solr for representing an incoming document that is going to be indexed.  

Data that needs to be indexed is retrieved from Alfresco (_Node_ is the composite class representing the main Alfresco 
Domain Object) and then 

- each _Node_ instance is converted to a _SolrInputDocument_ instance
- each _SolrInputDocument_ instance is compressed, serialised and then stored in the content store
- each _SolrInputDocument_ instance is sent to Solr 

![SolrContentStore](stored_content_in_searchservices.png)  

With that flow in mind, at a given time T, the main difference between a document D in the content store and in the Solr 
index is that: 

- the content store file represents a verbatim copy of the _SolrInputDocument_ created starting from the corresponding _Node_
- it can be easily individuated because it corresponds to a single file in the content store; the Solr document definition 
instead doesn't have a "single" representation in the filesystem because it has been passed through the text analysis process. 

### Apache Solr Domain Model
In order to understand the reason why the content store approach has been adopted until SearchServices 1.4.x, we need to 
briefly describes how Solr manages the fields of the managed documents.

In Solr, the configuration file where fields are declared and configured is called "schema.xml". Each field can have 
different attributes that define 

- how it is internally organised
- what search features are enabled (for that specific field)

In this context we are interested in two specific attributes: "indexed" and "stored". A field in Solr schema can be declared
as "stored" and/or "indexed":

- if the field is indexed (i.e. indexed="true") that means search features are enabled for that field (i.e. search, faceting, sorting)
- if the field is stored (i.e. stored="true") the verbatim copy of the incoming field value is stored and can be returned as part of search results. 

In the SearchServices 1.4.x schema: 

- all fields are marked as indexed: this is quite obvious because we want to enable search features on them
- 99% of fields are marked as **non stored**: this because SearchServices **retrieves the stored content from the Solr Content Store**  

There are actually only three fields marked as stored: id, DBID and _version_. The last one is a Solr internal field used for some 
features like atomic updates and optimistic locking (both of them are not used in SearchServices 1.4.x).

### When the Solr Content Store is used
As described above, SearchServices doesn't make use of Solr storage capabilities, so the rule is: the Solr 
Content Store is involved on each interaction which requires the stored content. That includes: 

- **Fields retrieval**: Solr stored only DBID, id and version fields; in search results we want to be able to retrieve 
also other fields (e.g. name, title, LID, PATH) 
- **Highlighting**: highlighted snippets are built using the fields stored value(s)  
- **Clustering**: runtime clusters generation use the fields stored value(s), as well  
- **Fingerprint**: the Fingerprint (custom) SearchComponent returns the (stored value of the) MINHASH field computed from the text content associated 
with a given document    
- **Text Content Management**: this is strictly connected with how the _ContentTracker_ works. See this [ADR](../trackers/00001-content-tracker.md) for a detailed exaplanation about the text content lifecycle in SearchServices.

### Read/Write Path on Solr Content Store 
Every time a search request involves one of the points listed in the previous section we need to interact

- with the Solr index
- with the Solr Content Store

The Solr Content Store interaction can have two purposes:    

- **Read only**: we need to read the stored fields associated to one or more documents 
- **Read/Write**: we need to read and update the document definition (i.e. some field has been updated)

The two paths execution requires additional I/O and CPU work on top of what Solr already normally does; Specifically:

The **Read Path** consists of the following steps (remember, this needs to be done for each match produced by a query): 

- Locate the .gz file corresponding to a given DBID
- Uncompress the .gz file
- Deserialise the file in a _SolrInputDocument_ instance
- Use the fields values in the instance in order to perform the required task (e.g. fields retrieval, highlighting)   

A first important thing about the flow above: it's not possible to load in memory only the fields we need.
Every time the document D is needed (even if our interaction requires just one field) the whole document definition is 

- located (file seek)
- uncompressed
- deserialised
- read

Such capability is instead possible using Lucene: the IndexSearcher class can load a partial document definition which
contains only fields actually needed. For example, if we want to highlight search terms in two fields, let's say 
"name" and "title"

- the _AlfrescoHighlighter_ loads the whole document in memory
- the _SolrHighlighter_ loads only those two fields

This can make a relevant difference if we are in a context where the fields cardinality for each document is high, or if 
we have one or more big fields (not needed) with a lot of text content.   

The **Write Path** is even worst because it adds to the list above the following steps: 

- Update the _SolrInputDocument_ instance with updated values
- Delete the old compressed file in the filesystem
- Serialise the updated _SolrInputDocument_ instance
- Compress the serialised image
- Write a new .gz file

### Solr Content Store Removal Benefits

#### Use as much as possible Solr built-in capabilities
The main reason why an open source platform is chosen as underlying framework is its popularity. That means a lot of 
advantages in terms of 

- community and non-commercial support
- product improvements with short iterations (e.g. enhancements, bug fixing)
    
Although the underlying reasons for introducing a customisation could be perfectly acceptable, it's important to keep in 
mind that increasing such customisation level necessarily creates a gap, a distance with the open source product.   
From one side, the customisation allows to implement some functional requirement not covered by the open source version, 
on the other side the same customisation won't have the required support from the community. 

The initial approach to this task consisted of a verification [Spike](https://issues.alfresco.com/jira/browse/SEARCH-1669) where 
we investigated pro and cons about having/removing the _SolrContentStore_.   
Summarised, the output has been in favour of the removal, because the Solr storage capabilities are definitely more efficient 
than the approach adopted in the _SolrContentStore_.         
    
#### Less Solr customisations 
This is a direct consequence of the preceding point. As you can read below, when we describe the major components affected 
by the removal task, some customised component (e.g. Clustering) has been removed at all while some other else (e.g. Highlighter) 
has been simplified a lot, leveraging the Solr built-in capabilities as much as possible.    

### Only Solr data files
SearchServices no longer has to manage external files or folders. In SearchServices 1.4.x the content store required a 
relevant effort for [customising]((https://issues.alfresco.com/jira/browse/SEARCH-1669)) the built-in Solr Replication 
mechanism that doesn't take in account the Alfresco SolrContentStore.

![Solr Replication](replication.png) 

Note that such customisation has been removed in this branch and it has been replaced by the built-in Solr Replication Handler: 
the whole stored content management has been centralised in Solr; as consequence of that, the Read/Write paths described above 
are no longer valid.   
 
### Better compression 
Compressing at single document level is not very efficient because the small amount of data available. Moving such task 
at Solr level can deliver very good results for two main reason: 

- data cardinality is higher, so that means the compression algorithm can work with more representative and efficient stats
- data compression and index organisation is one area where the Solr community dedicated and dedicates a considerable amount of effort        

### Less, more efficient I/O and CPU(compress/decompress) resources usage
This is again related with the Read/Write paths we described above: once the _SolrContentStore_ has been removed, we do not have to 
deal with external files and folders and the read, write, compress, uncompress, serialise, deserialise tasks will be no longer needed. 

### Better OS Page Cache usage
The OS Page Cache is used for storing and caching files required by the application processes running on a given machine. 
In an ideal context the OS would put the entire Solr index in the page cache so every further read operation won't require any disk seek.
Unfortunately, the cache size is usually smaller than that, so a certain amount of time is spent by the OS in order to load/unload the
requested files.   

In a context like that, the less number of files we have to manage, the better: having a component like the content store 
which requires a relevant amount of I/O operations, it means a significant impact on the hardware resources (e.g. disk, cpu) 
and a less efficient usage of the OS Page cache (e.g. the OS could unload the Solr datafiles for working with Solr content store files).  

## Major Changes
This section provides a high-level description of the components / area that have been affected by the SolrContentStore removal. 

### Solr Schema 
Jira Ticket: [SEARCH-1707](https://issues.alfresco.com/jira/browse/SEARCH-1707)

The Solr schema (schema.xml) includes the following changes:  

- **stored fields**: every field is marked as stored. Since this is something we'd want to apply to all fields, the stored
attribute has been defined at field type level.
- **cleanup and new field types**: there are several new field types that declare the default values applied to a field. 
The naming is quite intuitive (e.g. "long" is a single value numeric field, "longs" is for multiValued numeric fields). 
That change allowed a more clear fields definitions (i.e. fields definitions that don't override default values are very short and concise)

![Field Types](schema_field_types.png)         

- **comments and examples**: sometimes it is very hard to understand the purpose of a field and what is its runtime content. 
For each field the current schema provides a description about its intent and one or more examples.

![Comments in schema](field_content_example.png)  

### Highlighting
Jira Ticket: [SEARCH-1693](https://issues.alfresco.com/jira/browse/SEARCH-1693)  

Before the content store removal, the _AlfrescoSolrHighlighter_ class was a custom "copy" of the _DefaultSolrHighlighter_. 
Instead of extending the Solr component, at time of writing that class had been 

- copied 
- renamed in _AlfrescoSolrHighlighter_
- customised 
 
As consequence of that, the class was a mix of Alfresco and Solr code. Specifically, the custom code (and this is valid for all the customised 
components mentioned in this document) was there mainly for two reasons: 

- SolrContentStore interaction: every time the component needed to access to the stored content of a document
- Field renaming/mapping between Alfresco and Solr: for example a "cm_name" or "name" Alfresco field in the highlighting 
request needs to be translated in the corresponding Solr field (e.g. text@s___t@....@name)          

The new _AlfrescoSolrHighlighter_ 

- removes any interactions with the content store
- extends the _DefaultSolrHighlighter_
- contains at 95% the Alfresco specific logic (mainly related with the field mapping/renaming). Each time it needs to execute
the highlighting logic, it delegates the Solr superclass.
- it still has a 5% of code copied from the superclass. That because sometime it has't been possible to decorate 
Solr methods from the superclass (see _getSpanQueryScorer_ or _getHighlighter_ methods)

The field mapping/renaming didn't allow to remove completely the custom component. However, the refactoring described above could be 
a first step for externalising (in an intermediate REST layer) that logic. Once did that, the custom highlighter could be removed and replaced with 
the plain Solr built-in component.  

### Clustering
Jira Ticket: [SEARCH-1688](https://issues.alfresco.com/jira/browse/SEARCH-1688) 

The _AlfrescoClusteringComponent_ has been removed because it was a raw copy of the corresponding Solr component 
and the only customisation was related with the content store interaction.   

### Fingerprint
Jira Ticket: [SEARCH-1694](https://issues.alfresco.com/jira/browse/SEARCH-1694)

Two components have been affected by the content store removal: 

- the [_Solr4QueryParser_](https://issues.alfresco.com/jira/browse/SEARCH-1694?focusedCommentId=622599&page=com.atlassian.jira.plugin.system.issuetabpanels%3Acomment-tabpanel#comment-622599): the component which is in charge to parse incoming queries (FINGERPRINT queries in this case) 
- _FingerprintComponent_: this is a custom _SearchComponent_ which accepts in input a node identifier and returns a response consisting of the corresponding fingerprint (i.e. the MINHASH multivalued field). Note that the MINHASH value(s) is not computed on the fly. Instead it is computed at index time when the text context is indexed.      

### CachedDocTransformer

Jira Ticket: [SEARCH-1689](https://issues.alfresco.com/jira/browse/SEARCH-1689)

We said above that before the content store removal we had only three stored fields: DBID, id and version. 
If that could be perfectly reasonable from a "search" execution perspective because we didn't need stored fields at all in the 
matching and scoring phases, that becomes a problem when we have to return the search results to the caller: 

- a search client would probably need some other field like a title, a name. 
- we couldn't use Solr for retrieving those fields/values because we didn't store them

The only place where we had the stored content was the _SolrContentStore_, but Solr didn't know how to interact with it.
For this reason Alfresco introduced a custom _DocTransformer_. A _DocTransformer_ is an extension point provided by Solr for 
introducing a custom transformation logic at document level. Once the search has been executed, for each matching document
the transformer is invoked and it can manipulate it.

This was one of the customisations strictly tied with the content store. Even after the content store removal, the doc 
transformer is still there because the field mapping/renaming executed at document/field level is crucial for decoupling 
the Alfresco Data Model from the Solr schema.

The _DocTransformer_ could be referred using the "[cached]" mnemonic code which no longer communicate the new purpose. 
For that reason a new alias "[fmap]" has been introduced. The old "[cached]" code is still working but will be deprecated.     

The same consideration we did for the highlighter is valid for this component as well: if the field mapping/renaming is 
moved outside Solr, this component could be easily removed.

### DateFormatDocTransformer

Jira Ticket: [SEARCH-2044](https://issues.alfresco.com/jira/browse/SEARCH-2044)

This is a new _DocTransfomer_ introduced for maintaining the retro-compatibility with date/datetime fields management 
in InsightEngine. 

The InsightEngine SQL interface uses a hybrid language for expressing queries. Specifically, while the most part of the 
query language is a plain standard SQL, everything related with date/datetime fields or expressions follows the Solr semantic.   
For example, the Solr DateMath expressions can be used in SQL queries:

- select cm_created_month, count(*) as ct from alfresco where cm_owner = 'jimmy' and cm_created >= **'NOW/MONTH-6MONTHS'** group by cm_created_month      
- select cm_created_year, count(*) as ct from alfresco where cm_owner = 'morton' and cm_created >= **'NOW/YEAR-4YEARS'** group by cm_created_year 

Those expressions are not valid in SQL, so we must force the Calcite parser behaviour in order to consider them as "opaque" values. 
In other words, everything related with date/datetime fields/expressions are considered (opaque) strings and aren't parsed by the 
Calcite SQLParser: they are directly forwarded to Solr.

A _SolrInputDocument_ instance in the content store was composed by a set of fields whose values were exclusively a string 
or list of strings. After the content store removal SearchServices retrieves the stored content from Solr, and if a field 
is declared as having a Date or DateTime field type, Solr will return its value as a _java.util.Date_.

The _DateFormatDocTransformer_ is a simple transformer which replaces the Date value of such fields with the corresponding UTC 
string representation.   

### SolrInformationServer

Jira Ticket: [SEARCH-1702](https://issues.alfresco.com/jira/browse/SEARCH-1702)

The _SolrInformationServer_ is a kind of Mediator/Facade between SearchServices and the underlying search platform. This is 
huge class which contains all methods for manipulating the index. Those methods are mainly called by the trackers
subsystem. 

It had a strong connection/interaction with the content store because it represents the central point where the three 
different representations of the same data 

- the incoming Node representing new or updated data which will create the "updated" version of document D
- the document D in the content store 
- the document D in the Solr index

are managed, manipulated, updated or deleted, and finally indexed. 
A first big change which affected the _SolrInformationServer_ has been the removal of all interactions with the content store. 

#### Atomic Updates
  
An important change has been the introduction of partial/atomic updates.    
Imagine an update path: 

- an incoming Node arrives. It contains data that needs to be updated
- the _Node_ is not the exact copy of the Solr document. For example there are some fields that have been computed at indexing time (e.g MINHASH)
- the _SolrContentStore_ contains the exact copy of the last version of that Solr document which has been previously sent to index
- that document is loaded from the content store
- it merged/updated with the data in the incoming _Node_
- the entry on the content store is overwritten
- the updated document is then sent to Solr 

Without the _SolrContentStore_ that path is no longer possible and it will be simplified a lot with the introduction of Atomic Updates.
  
Atomic Updates are a way to execute indexing commands on the client side using an “update” semantic, by applying/indexing 
a document which represents a partial state of a domain object (the incoming _Node_, in our case).

One of the main reason why the _SolrInformationServer_ code has been widely changed is related with the Atomic Updates 
introduction. More information about this change can be found [here](https://sease.io/2020/01/apache-solr-atomic-updates-polymorphic-approach.html)

Note that enabling the atomic updates requires also a major change in the configuration: the **UpdateLog** 
must be enabled in order to make sure the updates are always applied to the latest version of the indexed document. 

##### Dirty Text Content Detection    
Another change introduced in _SolrInformationServer_ class is related with how SearchServices (specifically
the _ContentTracker_) detects the documents whose text content needs to be updated.

Previously, we had a field in the Solr schema called **FTSSTATUS** that could have the following domain: 

- **Clean**: the text content of the document is in sync, no update is needed
- **New**: the document has been just created, it has to be updated with the corresponding text content
- **Dirty**: the text content of the document changed, the new content needs to be retrieved and the document updated   

After the content store removal, the FTSSTATUS field has been removed. This because the field value was set depending on 
the document state in the content store: 

- if the incoming node didn't have a corresponding entry in the content store, then it was set to **New**
- if the incoming node had a corresponding entry in the content store a DOCID field value was compared between the node and the stored document. In case 
the two values were different then the FTSSTATUS was set to **Dirty**
- Once the _ContentTracker_ updated the document with the new text content, the FTSSTATUS was set to **Clean**    

We no longer have the content store, so the comparison above cannot be done. For example, when a _Node_ arrives we
cannot know if that corresponds to an existing document or if it is the first time we see it. 
We could request that information to Solr but that would mean one query for each incoming _Node_, and that wouldn't be efficient. 
  
The new approach uses two fields: 

- **LATEST_APPLIED_CONTENT_VERSION_ID**: it corresponds to the identifier of the latest applied content property id 
(content@s__docid@* or content@m__docid@*). It can be null (i.e. the incoming node doesn't have a value for that property, 
even if it requires content indexing)

- **LAST_INCOMING_CONTENT_VERSION_ID**: If the field has the same value of the previous one (or it is equal to _SolrInformationServer.CONTENT_UPDATED_MARKER_),
        then the content is supposed to be in synch. Otherwise, if the value is different, it is not _SolrInformationServer.CONTENT_UPDATED_MARKER_
        or it is _SolrInformationServer.CONTENT_OUTDATED_MARKER_ the content is intended as outdated and therefore it will
        be selected (later) by the _ContentTracker_.

### AlfrescoReplicationHandler

This set of components, [introduced in SearchServices 1.4.x](https://issues.alfresco.com/jira/browse/SEARCH-1850) for including the content store in the Solr replication mechanism, has been removed 
because we no longer have any external folder/file to be synched between master and slave(s). As consequence of that 
the built-in Solr ReplicationHandler is used. 

### Content Store Package and Tests

Jira Tickets: [SEARCH-1692](https://issues.alfresco.com/jira/browse/SEARCH-1692),[SEARCH-2025](https://issues.alfresco.com/jira/browse/SEARCH-2025)

Once the content store references have been removed from the components listed in the sections above, the _org.alfresco.solr.content_
package has been completely removed.  


      