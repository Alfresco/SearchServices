# 7. Event Content Tracker

Date: 08/05/2019

## Status

In progress

## Context

The current approach of the *Content Tracker* in *Search Service* is to query SOLR for any `dirty` documents which it then fetches from Alfresco. Once the content is successfully obtained from Alfresco, it marks it `clean` which eventually get committed to the index. This approach will need to be modified as it applies pressure on Alfresco on every call to get the content. Taking an event based approach where the *Content Tracker* subscribes to a topic with policy the specific behaviour will allow to get the extracted content when ready.

Different alternatives have been evaluated at [Event Based Content Tracker Spike](https://github.com/Alfresco/SearchServices/blob/master/alfresco-search/doc/architecture/event-trackers/event-based-content-tracker-spike.md)

This proposal is to develop a new Content Tracker that consumes content based on events.

## Decision

We will use an Event Oriented Content Tracking based in Apache Kafka. This product supports recovering from a previous event, so catching up with the Repository and re-indexing operations are granted.

## Consequences

A complete PoC will be developed, including following use cases:

1. Indexing Content on events, including asynchronous Tranforms Service integration.
1. Rebuilding content indexes from scratch.
1. Recovering from a previous content indexation status.
