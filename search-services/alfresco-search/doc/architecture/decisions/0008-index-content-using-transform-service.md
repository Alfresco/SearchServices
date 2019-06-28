# Index content using Transform Service.

Date: 09/04/2019

## Status

proposed

## Context

The ability to search on content requires a content extraction process. This relies on repo getting the document, passing it through image magick and finally using tika to extract the text. This process does not scale and is a known bottle neck, as the embedded transformation is unable to cope with large volumes or large documents.

In order to scale this work, it is now off loaded to the Transform Service, which can be scaled to cope with the demand. This method of extracting the content is not integrated with Search Services and as a result any content transformed by the Transform Service is not searchable.

The following are the suggested approach to indexing with Transform Service:

* Search Service to use the get rendition V1 Public API.

* Refactor the current V0 API(in use by Search Services) to make use of RenditionService2.

* Introduce a new microservice that sits between Solr and the transformation service. The content is off loaded to a transformation service and the microservice stores the content for Solr to ingest.

* New content tracker that consumes content based on events.

## Decision

Based on the group discussion and design reviews, we have agreed to go with the event based content tracker.
In this design the Search Services will place a request in the message queue for the Repo to consume.
The message will contain the NodeId and Solr identifier (name of the instance or Solr shard).
Once the message is consumed by Repo it will start the process to obtain the text for the content.
When the content is ready an event will be placed in the queue for Search Services to consume.

The new async content tracker will monitor the queue and consume the message. Note that using the queue instead of of a Rest API, will make this operation asynchronous. The message we expect to see in the queue from Repo will consist of an identifier, status and a url. The async content tracker will use the url to obtain the content and retrieve the text for indexing.

This approach will require changes to both Search Services and Repo to adapt to a message queue.
The use of a message queue by Transform Service has influenced the solution to reuse the event queue.

The benefits of this solution would provide:
* Ability to index content asynchronously.
* Improved performance and storage capacity.
* No additional nodes are created to store text on the Repo.
* Ability to index content via Repo using Transform Service (Enterprise), or fall back to local embedded (Community). 
* Backward compatible with older versions of Alfresco.


## Consequences
This solution address the ability to index content asynchronously and will address the known bottle necks however we may discover new problem areas such as the message queue. We recommend doing benchmark tests to validate the claim and observe the behaviour. It is worth mentioning the latency introduced is unavoidable, due to the extra calls when using the Transform Service.

This design signifies a change in behaviour, the ContentTracker will be deprecated but will remain in Search Services to allow an upgrade path for existing customers. Once we remove the ContentTracker there wont be an option but to use the async content tracker that relies on a message queue.
