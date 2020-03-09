# Message Driven Content Tracker v2

Date: 05/02/2020

## Status

WIP

## Context

This is a second iteration of content tracking via message bus design. See [previous version](0007-message-driven-content-tracker.md).

New content tracker implementation will be based on new Search Services architecture (SS v2.0 or next gen). Main context behind this decision is almost the same as for v1 - get more throughput by leveraging new Transform Service.

## Decision

The decision is based on [version 1](0007-message-driven-content-tracker.md). The main differences are:
* Shared File Store may not be the right option as it is only available for Enterprise. Alternatively the URL to content can point to other locations. (TBC)
* The change in behaviour requires a major release of Search Services, most likely version 2.0.
* The changes in Content Repository will be available from version 6.3.
* The synchronous transformation APIs will remain functional until 7.0.

Details of the architecture to be clarified (WIP).

## Consequences
Additional latency will be introduced due to the extra calls when using the Transform Service. Also the transformation capabilities are much more limited.

This design highlights a major difference in behaviour, which requires a major release of Search Services.
