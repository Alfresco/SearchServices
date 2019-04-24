# 5. Merge Tests And Production Code

Date: 09/04/2019

## Status

Approved

## Context

In [ADR 3: "Combined Codebase"](0003-combined-codebase.md) we decided to merge the production and end-to-end test
repositories. In [ADR 4: "Community Mirror"](0004-community-mirror.md) we discuss setting up a mirror for the community
code.

## Decision

We will separate the end-to-end test code in half so that any code solely related to Insight Engine won't be mirrored.

We will remove the existing test groups for the different versions of Search Services and Insight Engine, and instead
delete any tests from branches where they should not be run.

## Consequences

It will be possible to include production code changes along with all required test changes in the same merge request. 
It will be easy to get new tests running before production code is written without causing other branches to fail.
