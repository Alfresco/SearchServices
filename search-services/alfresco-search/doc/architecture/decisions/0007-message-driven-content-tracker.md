# Message Driven Content Tracker

Date: 09/04/2019

## Status

~~Approved~~ Postponed, see [version 2](0009-message-driven-content-tracker-next-gen.md)

## Context

The ability to search on content requires a content extraction process. This relies on repo getting the document, passing it to one or multiple transformers, and finally returning the plain text content. This process does not scale as the embedded transformation is unable to cope with large volumes or large documents. Embedded transformations in general come with multiple problems, security related and scaling, which led to the introduction of the transformation service with 6.1

Since transformations to text for content indexing makes up a major portion of the transformation workload, it has always been intended to move these transformations to the new transformation service as well.

The following are the suggested approaches to indexing with Transform Service:

* Refactor the current V0 API (in use by Search Services) to make use of RenditionService2.

* Introduce a new microservice that sits between Solr and the transformation service. The content is off loaded to the transformation service asynchronously while providing the same synchronous API for Search Services.

* Search Service to use the get rendition V1 Public API.

* New content tracker that communicates with the repository asynchronously by messages.

## Decision

Based on the group discussion and design reviews, we have agreed to go with the asynchronous content tracker.
In this design the Search Services will place a request in the message queue for the Repo to consume.
The message will contain the NodeId and Solr identifier (name of the instance or Solr shard).
Once the message is consumed by Repo it will start the process to obtain the text for the content.
When the content is ready a response message will be placed in the queue for Search Services to consume.

The new content tracker will monitor the response queue and consume incoming messages. The message we expect to see in the queue will consist of an identifier, status and a URL. The status of the event can be used for handling errors. The handling of such errors prompting an abort or retry will be finalised during user story creation.
On a successful completion the new content tracker will use the URL to obtain the content and retrieve the text for indexing.

We use a URL in the response message rather than an identifier so that the repository can choose where to store the intermediate content at its own discretion. This will also provide the ability to leverage direct access URLs to cloud storage in the future (e.g. S3 signed URLs).

The benefits of this solution gives ability to index content asynchronously. Unlike the current way which is based on a synchronous call to Repo using HTTP. This solution allows Alfresco to scale the transformation and adds the ability to index more content.

![Component Diagram](/search-services/alfresco-search/doc/architecture/decisions/diagrams/AsyncContentTrackerComponentDiagram.png)

The other options have been considered but did not full fill the requirements.

Refactor the current V0 API (in use by Search Services) to make use of RenditionService2:  
The thread in the repository will still be blocked. Although the new transform service has a higher throughput, it can have a slightly longer delay. This blocks HTTP threads even longer, or they could even time out.  Using async HTTP introduced with servlet 3.0 has been considered, but this would need to be implemented throughout the entire webscript framework.

Using V1 API requires an authentication for SearchServices, which needs to be configured. There is currently no way for a system to call the V1 API without creating a new user. Creating a new user to represent the system is not the correct way to integrate systems and services. In addition, the V1 API uses the renditions for text which covered below.

Using renditions for text extraction:  
Renditions are stored long term in the repository as nodes. Using this mechanism for ephemeral text extractions would require ta new retention mechanism for renditions. All of this would put additional workload on the node tree, which defeats the design goal of handling high workloads.

Introduce a new microservice:  
This has only been considered as an interim solution if it is not possible to change the content tracker in Search Services. It is essentially the solution above (blocking the sync request from Search Services until transformation is ready) but moved to its own micro service. This solution is slightly better as it does not block repository threads but suffers from the same problems.

Creating a microservice for the content tracker adds complexity which adds little benefit as part of this delivery. There are benefits for making the trackers into a microservice, but this would require a significant amount of work. The deployment of Search Services will become complicated as we would have to deploy SearchServices and a micorservice for the new ContentTracker.

The current ContentTracker will remain so that the Community version continues to work with SearchServices.
The V0 content tracking webscript and the ContentTracker will be removed with the next major upgrade in favour of the message base API. At this stage both Enterprise and Community will make use of the message base API. Please note that the communication via message queues between Repo and Search Service, will be configured by default in the ACS (reference) deployment templates. As for the Alfresco 6.x and SearchServices 1.x distribution zip files, will default to the legacy configuration. This would provide the customer different options to upgrade, as we have an overlap between versions.

Adding a new content tracker based on a message queue is the preferred solution as this provides the benefits of using the Transform Service.


## Consequences
This solution addresses the ability to index content asynchronously and will address the known bottle necks however we may discover new problem areas such as the message queue. We recommend doing benchmark tests to validate the claim and observe the behaviour. It is worth mentioning the latency introduced is unavoidable, due to the extra calls when using the Transform Service.

The ability to run this in enterprise or community would require the correct release version of Alfresco with the above change along with SearchServices. SearchServices will still be able to run against older versions of Alfresco but to make use of the asynchronous capability it will require a new release of Repo.

The Shared File Store will need to be open source so that it will work against both community and enterprise versions of Alfresco. We assume that Shared File Store will have its own zip distribution in addition to its  docker container, allowing customers to choose the preferred deployment.

This design signifies a change in behaviour, the ContentTracker will be deprecated but will remain in Search Services to allow an upgrade path for existing customers. Once we remove the ContentTracker there won’t be an option but to use the async content tracker that relies on a message queue.
