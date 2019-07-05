# Index content using Transform Service.

Date: 09/04/2019

## Status

proposed

## Context

The ability to search on content requires a content extraction process. This relies on repo getting the document, passing it through image magick and finally using Tika to extract the text. This process does not scale as the embedded transformation is unable to cope with large volumes or large documents.

In order to scale this work, it is now off loaded to the Transform Service, which can be scaled to cope with the demand. This method of extracting the content is not integrated with Search Services and as a result any content transformed by the Transform Service is not searchable.

The following are the suggested approaches to indexing with Transform Service:

* Refactor the current V0 API (in use by Search Services) to make use of RenditionService2.

* Search Service to use the get rendition V1 Public API.

* New content tracker that consumes content based on events.

* Introduce a new microservice that sits between Solr and the transformation service. The content is off loaded to a transformation service and the microservice stores the content for Solr to ingest.

## Decision

Based on the group discussion and design reviews, we have agreed to go with the event-based content tracker.
In this design the Search Services will place a request in the message queue for the Repo to consume.
The message will contain the NodeId and Solr identifier (name of the instance or Solr shard).
Once the message is consumed by Repo it will start the process to obtain the text for the content.
When the content is ready an event will be placed in the queue for Search Services to consume.

The new content tracker will asynchronously monitor the queue and consume the message. The message we expect to see in the queue will consist of an identifier, status and a url. The status of the event can be used for handling errors. The handling of such errors prompting an abort or retry will be finalised during user story creation.
On a successful completion the new content tracker will use the url to obtain the content and retrieve the text for indexing.

The benefits of this solution gives ability to index content asynchronously. Unlike the current way which is based on a synchronous calls to Repo using HTTP. This solution allows Alfresco to scale the transformation and adds the ability to index more content.

The other options have been considered but did not full fill the requirements.
Using either V0 and V1 API with RenditionService2 was rejected as it would store additional nodes with the text in the Repo. This adds a storage overhead and duplicates the text across the systems.

Using V1 API requires an authentication for SearchServices, which we do not have. There is currently no way for a system to call the V1 API with out creating a new user. Creating a new user to represent the system is not the correct way to integrate systems and services.

Creating a microservice for the content tracker adds complexity which adds little benefit as part of this delivery. There are benefits for making the trackers into a microservice but this would require a significant amount of work. The deployment of Search Services will become complicated as we would have to deploy SearchServices and a micorservice for the ContentTracker.

Adding a new content tracker based on a message queue is the preferred solution as this provides the benefits of using the Transform Service. The existing content tacker will remain so that it will work with the community.
Having the ability to configure the preferred content tracker, provides a solution to both community and enterprise.

![Component Diagram](diagrams/AsyncContentTracker.jpg)

## Consequences
This solution addresses the ability to index content asynchronously and will address the known bottle necks however we may discover new problem areas such as the message queue. We recommend doing benchmark tests to validate the claim and observe the behaviour. It is worth mentioning the latency introduced is unavoidable, due to the extra calls when using the Transform Service.

The ability to run this in enterprise or community would require the correct release version of Alfresco with the above change along with SearchServices. SearchServices will still be able to run against older versions of Alfresco but to make use of the asynchronous capability it will require a new release of Repo.

This design signifies a change in behaviour, the ContentTracker will be deprecated but will remain in Search Services to allow an upgrade path for existing customers. Once we remove the ContentTracker there won’t be an option but to use the async content tracker that relies on a message queue.
