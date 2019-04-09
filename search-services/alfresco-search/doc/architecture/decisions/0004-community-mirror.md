# 4. Community Mirror

Date: 09/04/2019

## Status

Proposed

## Context

In [ADR 3: "Combined Codebase"](0003-combined-codebase.md) we decided to merge the Search Services and Insight Engine
repositories.  Since we want to enable the community to submit pull requests to the Search Services project we need a
way to keep this code up to date on GitHub.

## Decision

We will mirror the alfresco-search-parent submodule of `master` along with all branches starting with `release/` to a
branch with the same name on GitHub.  We will include this command as part of our build to do this:

```git subtree push -P alfresco-search-parent out $targetBranch```

## Consequences

A ticket has been raised to track this [[1]]. A script has been written to do the initial codebase merge and
mirroring [[2]].

We will rewrite the whole history of the SearchServices repository using the mirroring command (the only change will be
the commit ids).

Any changes within a module called alfresco-search-parent will be mirrored. Any changes outside this will not be
mirrored. 

!!! TODO: Check if the community can build the submodule without access to the parent pom file !!!

[1]: https://issues.alfresco.com/jira/browse/SEARCH-1397
[2]: https://git.alfresco.com/search_discovery/combinerScript/blob/master/combineSearch.sh
