# Contributing to LXTK

Welcome to the [LXTK](README.md) project.

Before contributing, please notice that LXTK is licensed under the Eclipse
Public License (EPL) 2.0. As part of the contribution process, the contributor
licenses their contribution under the project license. See [LICENSE](LICENSE)
for the full license text.

By making a contribution to this project, you certify the
[Developer Certificate of Origin](DCO) (DCO).

Big or small, every contribution matters:

- [Report a Bug](#reporting-an-issue)
  or [Suggest an Enhancement](#reporting-an-issue)

- [Set up a Developer Workspace](#setting-up-a-developer-workspace)
  and [Issue a Pull Request](#making-a-pull-request)

## Reporting an Issue

LXTK tracks bugs and enhancement requests via [GitHub Issues][1].
Please search for existing issues before creating a new one to report a bug
or suggest an enhancement:

- [View Open Issues](https://github.com/lxtk-org/lxtk/issues)

- [Create a New Issue](https://github.com/lxtk-org/lxtk/issues/new)

## Setting up a Developer Workspace

[Eclipse IDE][2] is both a target platform and the development environment
for LXTK. The recommended IDE configuration is described in the
[tools/tools.p2f](tools/tools.p2f) file.

LXTK currently uses `JavaSE-11` for compilation. Please add a matching JRE.

LXTK uses specific code formatting conventions for Java. Please import and use
the formatter profile provided in [tools/formatter.xml](tools/formatter.xml).

Import all projects from a local clone of your [fork][3] of the
[LXTK GitHub repository](https://github.com/lxtk-org/lxtk) (make sure that
`Search for nested projects` option is not checked in the import wizard),
and set the target platform using a `.target` file provided in the `targets`
project.

There are currently two target platforms defined. The base platform
([base.target](targets/base/base.target)) defines the base API level, whereas
the latest platform ([latest.target](targets/latest/latest.target)) defines
the latest functional level. It is recommended that the base platform be set
as the active target platform for development.

## Making a Pull Request

LXTK accepts contributions of content (e.g. code or documentation)
to the project's Git repository via [GitHub Pull Requests][4].

### Copyright Headers

*The guidlines regarding copyright headers were adapted from the
[Eclipse Project Handbook](https://www.eclipse.org/projects/handbook/).*

Where possible, all source files (including configuration files such as `XML`
and documentation files such as `HTML`) must contain appropriate copyright and
license notices. The notice template is:

```
/******************************************************************************
 * Copyright (c) {YEAR} {INITIAL COPYRIGHT OWNER}
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   {INITIAL AUTHOR} - initial API and implementation
 ******************************************************************************/
 ```

The `{YEAR}` is either a year or a range of years with the first and last years
of the range separated by a comma, e.g. `2020` or `2019, 2020`. The first year
is when the content was initially created and the last year is when the content
was last modified.

The `{INITIAL COPYRIGHT OWNER}` is the copyright owner that created the initial
content. If the content is subsequently modified and appended to by other
copyright owners, the words `and others` are typically appended. The words
`and others` are used to avoid having to list every copyright owner and because
often, most of the content in the file was contributed by the initial copyright
owner with subsequent modifications by others being smaller. However, especially
if the number of copyright owners is small (e.g. two), there is nothing wrong
with listing all of them especially if their contributions are more
proportionately equal, e.g. `ABC Ltd. and XYZ Corp.`

*Note:* Copyright owners should not be confused with authors. Often, the
intellectual property developed by an employee is the property of the employer;
in this case, the employer should be listed as the copyright owner.

Please be consistent when expressing copyright. For example,
`Copyright (c) 2020 ABC Ltd` and  `Copyright (c) 2020 ABC Ltd.`
might potentially be considered different because of the period.

The `{INITIAL AUTHOR}` is the name of the person who wrote the initial content.
Subsequent authors are listed on the next lines. It is good practice to list
the name of the author with their employer if the latter owns the copyright,
e.g. `John Johnson (XYZ Corp.)`.

Example copyright and license notice:

```
/******************************************************************************
 * Copyright (c) 2019, 2020 ABC Ltd. and others.
 *
 * This program and the accompanying materials are made available under
 * the terms of the Eclipse Public License 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Ivan Ivanov (ABC Ltd.) - initial API and implementation
 *   Jonh Jonhson (XYZ Corp.)
 ******************************************************************************/
 ```

### GitHub Pull Requests

There are a few recommendations regarding pull requests to LXTK:

 1. Before creating a pull request, it is good practice to make sure that
    there is an open issue where the problem at hand has been clearly stated
    and the proposed solution has been sufficiently discussed.

    It is understood that a simple bug fix might not require an upfront
    discussion of the proposed solution, and fixing a typo might not merit
    opening an issue. Use your own judgement.

 2. If a pull request addresses an open issue, the pull request should be
    [linked][5] to the issue and the pull request branch should be named as
    `GH-issueNumber`. A pull request should never address multiple different
    issues.

    Do not worry about referencing the issue in a commit message. At merge time,
    the commit message will be amended with housekeeping information including
    links to the corresponding issue and pull request.

 3. A new pull request should never contain fix-up or merge commits; it should
    be complete and ready for review. In most cases, a new pull request should
    contain a single commit.

 4. It is best to avoid force-pushing to the pull request branch.

For a detailed description of general best practices on working with GitHub
pull requests, see
<https://homes.cs.washington.edu/~mernst/advice/github-pull-request.html>.

For a nice article describing how to write a good commit message, see
<https://chris.beams.io/posts/git-commit/>.

Happy contributing!

[1]: https://docs.github.com/en/github/managing-your-work-on-github/managing-your-work-with-issues
[2]: https://www.eclipse.org/eclipseide/
[3]: https://docs.github.com/en/github/getting-started-with-github/fork-a-repo
[4]: https://docs.github.com/en/github/collaborating-with-issues-and-pull-requests/proposing-changes-to-your-work-with-pull-requests
[5]: https://docs.github.com/en/github/managing-your-work-on-github/linking-a-pull-request-to-an-issue
