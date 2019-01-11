# 2. Search More Like This

Date: 09/01/2019

## Status

Investigation Complete

## Context

*Intro (Lucene)*

The More Like This (MLT from now on) functionality is implemented in Lucene and made available through Solr Rest API.
The main implementation code is currently in the Lucene library : org.apache.lucene.queries.mlt.MoreLikeThis_ class .
In that class there is the logic to take a document Id (or the document itself) in input and calculate a MLT query based on the significant terms extracted from the document in relation to the corpus.
Currently it operates extracting the document from the local Lucene index when standalone(Terms vectors or stored content must be available)
or using the realtime GET to fetch the input document from adjacent shard.
The MLT query is built on the assumption that it is possible to identify significant terms from a document based on the term frequencies of them in document, compared to their occurrence in the corpus.
After the significant terms are extracted, a score is assigned to each one of them and the most important are used to build a boosted query.
Which fields to take into account when extracting the terms and building the query is one of the MLT parameters.
I attach the slides from a presentation I made in 2017[1] detailing the internal of the functionality and some proposed refactor.

*Apache Solr*

Apache Solr exposes various ways to interact with the MLT library.

* MoreLikeThisHandler -> a dedicate request handler to return similar results to an input one
* MoreLikeThisComponent -> to automatically execute more like this query on each document in the result set
* MoreLikeThisQueryParser -> I tend to consider this the modern approach, that allow you to build MLT queries and debug them more easily

*More Like These*

Implementing the More Like These can be vital to offer advanced functionalities and reccomndetation to the users.
The proposed implementation approach will cover different software areas : Lucene, Solr, Alfresco APIs .

I attached an High Level T-Shirt sizing estimation to each part of the developments.

*Lucene - M*

The Lucene implementation will be the biggest part. It will require to extend the More Like This class with:
* Additional facade methods to process list of documents or list of document Ids
* Significant term extraction and scoring from the input set of documents
In particular the second bit (significant term extraction and scoring) will be the most important.
Various algorithms are available to provide such capability [2] , I recommend a first implementation based on:
JHL.
After a first investigation KLIP [3] seems a promising implementation as well.
Being Alfresco use case very generic additional variants can be added later.
Each variant could be specific to a specific scenario (big collection, kind of the collection ect)

*Solr - S*

Out of the various ways Apache Solr serves the functionality as a beginning I recommend to extend the 3) MoreLikeThisQueryParser.
We’ll re-use all the Solr MLT parameters and in addition we’ll support a different input.
These are the classes:

org.apache.solr.search.mlt.CloudMLTQParser
org.apache.solr.search.mlt.SimpleMLTQParser

The More Like These Functionality will be compatible out of the box with SolrCloud but for the Alfresco distribution model it will require some work.

*Solr - Alfresco Customisation - M*

To have the More Like These fully distributed in the Alfresco use case:
The Interesting terms extraction and query building is effectively run locally on a single Solr.
To have it properly distributed it is needed to:
* Enable distributed IDF
* customise the query parser to use the shards parameter to fetch the documents through the get request handler (that already supports it)
the customised query parser needs to be compatible with Alfresco name mapping and locale functionalities
* Acl needs to be preserved
* the seed document must be fetched from the solr content store, potentially though the realtime GET
* field parameters must be appropriately rewritten according to Alfresco mappings

*Alfresco Side - Configuration - S*

A specific request handler will be configured in the Alfresco solrconfig.xml to expose an endpoint that will use the More Like This query parser by default.
It will take in input as request parameters a set of document Ids and the document fields to use for the document similarity.
The rest of the parameters will be hardcoded in the config, only expert admin are invited to touch them (such as the algorithm for term scoring, and all the other MLT params)

*Alfresco Repository and Rest API - M*

A specific query language must be implemented:
org.alfresco.rest.api.search.impl.SearchMapper
org.alfresco.repo.search.impl.lucene.LuceneQueryLanguageSPI .
The implementation should follow Alfresco best practice and allow to interact with the dedicated request handler for the More Like These

[1] https://www.slideshare.net/AlessandroBenedetti/advanced-document-similarity-with-apache-lucene

[2] https://www.elastic.co/guide/en/elasticsearch/reference/current/search-aggregations-bucket-significantterms-aggregation.html#_parameters_5

[3] Tomokiyo, T., Hurst, M. (2003). A language model approach to keyphrase extraction. In Proceedings of the
ACL 2003 workshop on Multiword expressions: analysis, acquisition and treatment (Vol. 18,
pp. 33–40) Association for Computational Linguistics.

## Decision
Based on the results of the investigations the most critical points have been identified in:

1) Alfresco custom storing approach
2) Alfresco custom ACL filtering
3) Alfresco sharding model

The decision is to structure the developments in 3 sequential macro areas:
1) provide the More Like This functionality via API
2) provide the More Like These functionality via API
3) provide the automatic query expansion through API and configuration

The investigation was done just at API level which means no front end tasks have been taken under consideration.
You find the detailed tasks under consequences.

## Consequences
Appropriate Jiras have been created:
https://issues.alfresco.com/jira/browse/SEARCH-1385
https://issues.alfresco.com/jira/browse/SEARCH-1386
https://issues.alfresco.com/jira/browse/SEARCH-1387
