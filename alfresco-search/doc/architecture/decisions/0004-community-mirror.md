# 4. Community Mirror

Date: 09/04/2019

## Status

Approved

## Context

In [ADR 3: "Combined Codebase"](0003-combined-codebase.md) we decided to merge the Search Services and Insight Engine
repositories.  Since we want to enable the community to submit pull requests to the Search Services project we need a
way to keep this code up to date on GitHub.

## Decision

We will mirror `master` and all branches starting with `release/` to a branch with the same name on GitHub.  We will
exclude the alfresco-insight-engine-parent directory. We will include these commands as part of our build to do this:

```
# This avoids making changes to the original branch.
get checkout -b tempBranch
# This strips all enterprise changes (in a reproducible way) and pushes any updates to the mirror.
git filter-branch -f --prune-empty --index-filter 'git rm -r --cached --ignore-unmatch alfresco-insight-engine-parent'
git push out HEAD:$branch
# This resets us back to where we were before the filtering.
git checkout $branch
```

## Consequences

A ticket has been raised to track this [[1]]. A script has been written to do the initial codebase merge and
mirroring [[2]].

We will rewrite the whole history of the SearchServices repository using the mirroring command. This will result in the
new repository containing a new root pom file and the search services code beneath that.

All changes except those within the module called insight-engine will be mirrored.

The root pom file in the community project will include a reference to the insight-engine pom file, so community users
will have to build from within the alfresco-search-parent directory. 

[1]: https://issues.alfresco.com/jira/browse/SEARCH-1397
[2]: https://git.alfresco.com/search_discovery/combinerScript/blob/master/combineSearch.sh
