# 6. Release Synchronisation

Date: 24/04/2019

## Status

Approved

## Context

In [ADR 3: "Combined Codebase"](0003-combined-codebase.md) we decided to merge the Search Services and Insight Engine
repositories.  In [ADR 4: "Community Mirror"](0004-community-mirror.md) we discussed how we would set up a build job to
ensure community code is available to the community. In particular we decided:

> We will mirror `master` and all branches starting with `release/` to a branch with the same name on GitHub.

## Decision

During the combining of the Search Services and Insight Engine codebases we will create a branch `master` by merging
the existing `master` branches of those two projects. We will update the version of Insight Engine to match Search Services - that
is 1.4.0-SNAPSHOT. 

## Consequences

The next minor release of Insight Engine will be 1.4.0 which will happen at the same time as the release of Search
Services 1.4.0. There will be no Insight Engine releases with the versions 1.2.x or 1.3.x.

Here are a few examples to illustrate the process for future releases. Note that other releases (including service
packs, etc.) will follow a similar logic based on whether they are pre- or post- 1.4.0.

* SS 1.3.0 needs a hotfix

We release from the `release/alfresco-search-services/V1.3.0.x` branch.

* IE 1.1.0 needs a hotfix

We release from the `release/alfresco-insight-engine/V1.1.0.x` branch. We need to use the maven version 1.1.0.2 for the
IE modules and 1.4.0-IE1.1.0.2 for the SS modules.

* SS 1.4.0 needs a hotfix

We'll create a hotfix release for both SS and IE (since they will both have the same issue). This will be from the
`release/V1.4.0.x` branch (which would have already been created if SS/IE 1.4.0 had been released).

* IE 1.4.0 needs a hotfix

We'll create a hotfix release for both SS and IE from `release/V1.4.0.x` (and not publicise SS 1.4.0.1).
