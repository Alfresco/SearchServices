# Index content using Transform Service.

Date: 09/04/2019

## Status

proposed

## Context

The ability to search on content requires a content extraction process. This relies on repo getting the document, passing it through image magick and finally using tika to extract the text. This process does not scale and is a known bottle neck,as the embedded transformation is unable to cope with large volumes or large documents.

In order to scale this work, it is now off loaded to the Transform Service, which can be scaled to cope with the demand. This method of extracting the content is not integrated with Search Services and as a result any content transformed by the Transform Service is not searchable.

The following are the suggested approach to indexing with Transform Service:

* Search Service to use the get rendition V1 Public API.

* Refactor the current V0 API(in use by Search Services) to make use of RenditionService2.

* Introduce a new microservice that sits between Solr and the transformation service. The content is off loaded to a transformation service and the microservice stores the content for Solr to ingest.

* New content tracker that consumes content based on events (preferred)

## Decision

Based on the results of the investigation and time constraint we are choosing to refactor the current V0 API to work with the new RenditionService2. This requires some changes on the repo and minimal change on the Search Services code base.

The current V0 API uses a deprecated RenditionService, updating this API to use RenditionService2 will allow both community and enterprise to work as normal. There would be minimal or no change to Search as it will be using the same code and API endpoint.

The alternatives will require work or further integration with additional systems such as ActiveMQ and Identity Service. As some of this is not defined or ready to consume it adds uncertainty, complexity and poses a risk.
In addition there would be a significant amount of work to create the new content trackers to consume these events. In order to accomplish the minimum functional feature in the given time we will take the evolutionary approach and repurpose the current API to use RenditionService2.

Refactoring the current API used by Search Services provides the following benefits:
* Searchable content.
* Ability to index content using both embedded and Transform Service.
* Minimal refactoring on the Search Services code base.
* Minimum disruption to product.
* Simple setup.
* Delivery of the feature by fall 2019.

The preferred solution of using an event base would have been ideal but neither the repo, integration, or search teams are likely to have this ready by fall 2019. This will still remain a long term goal however for the short term we will progress with option 2.

## Consequences
This solution moves the load and stress from the transformation to the repo. The repository will have additional nodes to store, which may put some load on the system however we are confident that it is within reason. This would benefit a benchmark test to validate the claim. Based on previous benchmarking tests, we are confident we can store the information.

Final note is the latency introduced due to the additional network calls, this is unavoidable when using the Transform Service but should not pose a problem.
