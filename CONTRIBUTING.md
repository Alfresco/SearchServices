# Contributing

Thanks for your interest in contributing to this project!

The following is a set of guidelines for contributing to this library. Most of them will
make the life of the reviewer easier and therefore decrease the time required for the
patch be included in the next version.

Alfresco has an [active forum](http://community.alfresco.com/community/ecm) to support
community users of our products. If you have any questions then this is the fastest method
of getting an answer.

## Raising issues

If you want to raise an issue then please use the [issue tracker on GitHub](https://github.com/Alfresco/SearchServices/issues).
We may convert these to Jira tickets before working on them, as this is the system we use
internally for tracking development.  The Jira project for this codebase is [SEARCH](https://issues.alfresco.com/jira/projects/SEARCH/issues)
and you may want to look here first to see if your issue has previously been encountered.[^jiraaccess] 
There are some guidelines for raising a good issue [here](https://hub.alfresco.com/t5/alfresco-content-services-hub/reporting-an-issue/ba-p/289727).

[^jiraaccess]: Note that while we try to keep our Jira issues visible to everyone, some
are restricted as they relate to specific customers or security issues. Some older issues
are also restricted simply because we have not been through to check if they contain
sensitive information or not.

## Submitting changes

We have a [coding standards guidelines page](https://hub.alfresco.com/t5/alfresco-content-services-hub/coding-standards-for-alfresco-content-services/ba-p/290457)
although you will find numerous examples where we have not adhered to them.  Please try to
maintain consistency with the guidelines for new code, but avoid reformatting large
blocks of code if these are not related to your change.

## Branches

Our codebase consists of long-lived release branches and short-lived feature branches. The
code that we expect to include in the next minor version is stored on `master`.  All other
release branches have the prefix `release/`. Feature branches may have any other prefix,
but we usually use `feature/` or `fix/`. We expect code on release branches to be ready
to release, and in the rare occasion when a release branch is broken then we try to revert
changes to fix the branch as soon as possible.

As bug fixes often also need a change to ACS then we use a cherry-pick strategy to get the
fix to all necessary release branches.  The fix should initially be merged to `master` and
it can then be cherry-picked back by using:

```git cherry-pick -x -m 1 [mergeCommitId]```

## Community Mirror

Pull requests to our community mirror will be accepted in our enterprise codebase and then
mirrored back to the community.  You will always be credited with your commits, although if
you [sign your commits](https://git-scm.com/book/en/v2/Git-Tools-Signing-Your-Work) then the
signature will be stripped by the mirroring process.[^dependabot]

[^dependabot]: This is the reason that pull requests submitted by Dependabot appear closed
rather than merged.

## Builds

Our builds are currently in our internal Bamboo instance.  If you have access to the
internal code then you can find some more links in the [insight engine module](insight-engine/CONTRIBUTING.md).

Although the build results are not visible externally, it should be possible to run most of
the tests locally.  We have divided our tests into unit tests, integration tests and
end-to-end tests.  The unit and integration tests can be run using the maven `test` and
`verify` goals respectively.  The end-to-end tests cannot currently be run externally as
they require some dependencies stored in our internal Nexus.
