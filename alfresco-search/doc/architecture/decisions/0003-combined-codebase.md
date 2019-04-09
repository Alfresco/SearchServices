# 3. Combined Codebase

Date: 09/04/2019

## Status

Approved

## Context

Historically Alfresco has had a bad experience of having a monolithic codebase in SVN. The main issue with this was the
inability to easily work on feature branches, resulting in frequent conflicting changes. To make matters worse this was
seen as hard to move to git, since GitHub has/had a maximum limit on repository size and there were several large
binary artifacts stored in the Alfresco history [[1]].

More recently the Alfresco codebase has been split into a large number of small git repositories [[2]].

This proposal is to merge the various git repositories together. By doing this we aim to:

1. Remove the effort of creating releases of library projects.
1. Allow tests to be written on the same branch (in the same repository) as the production code (i.e. facilitate TDD).
1. Make it easier for everyone (especially people outside the team and new starters) to find the 'right bit' of code. 

[1]: https://community.alfresco.com/community/ecm/blog/2015/04/01/so-when-is-alfresco-moving-to-github

[2]: https://ts.alfresco.com/share/proxy/alfresco/api/node/content/versionStore/version2Store/a0c2492f-6354-4b98-adfc-e63d5c2209f5/SearchCodeBase.png

## Decision

We will merge the search-related repositories together and preserve their history.  We will not attempt to merge code
that other teams also need (for example the TAS test utilities or the alfresco-data-model projects).

## Consequences

An epic has been raised containing the next steps [[3]].  Broadly speaking these are:

1. Merge the alfresco-solr-client library with the SearchServices repository.
1. Merge the insight-engine, insight-jdbc and insight-zeppelin repositories.
    1. A consequence of this is that future releases of these artifacts will share version numbers.
1. Put all the end-to-end tests in a single testing project.
1. Merge the (public) SearchServices code into the (private) InsightEngine repository and set up a mirror so the
community code is still publicly editable.
1. Merge the tests with the production code.
1. Merge the build scripts with the production code.

[3]: https://issues.alfresco.com/jira/browse/SEARCH-1393
