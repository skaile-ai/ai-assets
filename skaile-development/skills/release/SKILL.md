---
name: "release"
description: "Changelog management, semantic versioning, and git tagging for skaile-dev domains and packages. Three modes: status (show current version + unreleased changes), bump (update version + CHANGELOG.md), tag (create annotated git tag)."
metadata:
  version: "1.0.0"
  tags:
    - "release"
    - "changelog"
    - "semver"
    - "versioning"
    - "tagging"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  user_inputs:
    dialog:
      - id: "mode"
        label: "Release mode"
        type: "select"
        options:
          - "status"
          - "bump"
          - "tag"
        required: true
        default: "status"
      - id: "target"
        label: "Target package or domain (defaults to skaile-development)"
        type: "text"
        required: false
      - id: "bump_override"
        label: "Override version bump (major/minor/patch)"
        type: "select"
        options:
          - "auto"
          - "major"
          - "minor"
          - "patch"
        required: false
        default: "auto"
    files: []
---

# Release — Changelog, Versioning, and Tagging

## Overview

Manages the release lifecycle for skaile-dev domains and packages. Three modes:

| Mode | What It Does | When to Use |
|------|-------------|-------------|
| `status` | Show current version, unreleased changes, suggested next version | Before a release to see what's pending |
| `bump` | Update version + generate CHANGELOG.md entries from structured commits | When ready to cut a release |
| `tag` | Create annotated git tag for the bumped version | After bump, to mark the release point |

## When to Use

- Before a release: `mode=status` to preview what changed
- Cutting a release: `mode=bump` to update version + changelog
- After bump: `mode=tag` to tag the release

## When NOT to Use

- For committing changes — use `git mode=commit`
- For devlog entries — use `devlog`
- For release announcements — use `notify template=release` (this skill optionally triggers it)

---

ROLE  Release manager — reads structured commit history, determines version bumps, generates changelog entries, creates git tags.

READS
  <target>/bundle.yaml OR <target>/package.json    — current version
  git log / git tag                                  — commit history and existing tags
  CHANGELOG.md                                       — existing changelog
  ---agent--- blocks in commits                      — structured change metadata

WRITES
  <target>/bundle.yaml OR <target>/package.json    — updated version
  CHANGELOG.md                                       — new version section with entries
  git tags                                           — annotated release tags

MUST  read structured commit messages (---agent--- blocks) to auto-generate changelog entries
MUST  preserve existing CHANGELOG.md content — only prepend new version section
MUST  ask for user confirmation before writing version bump
MUST  use Keep Changelog format for CHANGELOG.md
MUST  determine version source: bundle.yaml for ai-resources domains, package.json for code packages
NEVER auto-push tags — let the user push explicitly
NEVER tag without a clean working tree
NEVER overwrite or truncate existing changelog entries
NEVER bump version without analyzing commits first

EMIT [release] started mode=<mode>

---

## Mode: `status`

IF mode = status

  STEP 1: Determine current version
    - For ai-resources domains: read version from bundle.yaml
    - For code packages: read version from package.json

  STEP 2: Find last release tag
    $ git tag --list "v*" --sort=-version:refname | head -1
    IF no tags exist → all commits are unreleased

  STEP 3: Scan unreleased changes
    $ git log <last-tag>..HEAD --format="%H %s"
    - For each commit, look for ---agent--- block
    - Classify: breaking (breaking: true), feat, fix, other
    - Count changes per category

  STEP 4: Determine suggested bump
    - breaking: true anywhere → MAJOR
    - Any feat type → MINOR
    - Only fix/refactor/docs/test/chore → PATCH

  STEP 5: Report
    > "Current version: <version>
    > Last release tag: <tag> (<date>)
    > Unreleased changes: <N> features, <N> fixes, <N> breaking
    > Suggested next version: <next-version> (<reason>)"

  EMIT [release] status_complete version=<current> suggested=<next>

---

## Mode: `bump`

IF mode = bump

  STEP 1: Run status mode internally (steps 1-4 above)

  STEP 2: Determine bump level
    IF bump_override != auto → use override
    ELSE → use auto-detected level from status

  STEP 3: Calculate new version
    - Parse current version as semver (major.minor.patch)
    - Apply bump: major → (major+1).0.0, minor → major.(minor+1).0, patch → major.minor.(patch+1)

  CHECKPOINT bump_approval
    > "About to bump <current> → <new-version> (<level>).
    > This will update CHANGELOG.md with <N> entries.
    > Proceed? (yes / change to major|minor|patch / cancel)"

  STEP 4: Generate changelog entries
    - Group commits by type from ---agent--- blocks:
      - Breaking Changes: commits with breaking: true
      - Features: commits with type: feat
      - Fixes: commits with type: fix
      - Other: remaining commits (refactor, docs, test, chore, perf, build)
    - Each entry: one line from the commit's human-description (the text between title and ---agent---)
    - Omit empty groups

  STEP 5: Update CHANGELOG.md
    - Read existing CHANGELOG.md
    - Insert new version section after [Unreleased]:

    ```markdown
    ## [Unreleased]

    ## [<new-version>] — <YYYY-MM-DD>

    ### Breaking Changes
    - <entry>

    ### Features
    - <entry>

    ### Fixes
    - <entry>

    ### Other
    - <entry>
    ```

  STEP 6: Update version file
    - For ai-resources domains: update version in bundle.yaml
    - For code packages: update version in package.json

  STEP 7: Commit
    $ git add CHANGELOG.md <version-file>
    - Generate commit message: `chore(<target>): release v<new-version>`

  EMIT [release] bump_complete version=<new-version>

---

## Mode: `tag`

IF mode = tag

  STEP 1: Verify preconditions
    $ git status --short
    IF dirty working tree → STOP: "Working tree must be clean before tagging."
    - Read current version from bundle.yaml or package.json
    - Verify HEAD commit message matches "chore(*): release v<version>"
      IF not → WARN: "HEAD doesn't appear to be a release commit. Proceed? (yes/cancel)"

  STEP 2: Create annotated tag
    $ git tag -a v<version> -m "release: v<version>"
    > "Tag v<version> created. Push with: git push origin v<version>"

  STEP 3: Notify (optional)
    > "Post release notification to Mattermost? (yes/no)"
    IF yes → RUN notify template=release

  EMIT [release] tag_complete version=<version>

---

CHECKLIST
  - [ ] Current version determined from correct source (bundle.yaml vs package.json)
  - [ ] Commits analyzed for ---agent--- blocks
  - [ ] Version bump confirmed by user before writing
  - [ ] CHANGELOG.md entries grouped by type
  - [ ] Existing changelog content preserved
  - [ ] Version file updated
  - [ ] Working tree clean before tagging
  - [ ] Tag created as annotated (not lightweight)

## Common Mistakes

| Mistake | What to do instead |
|---------|-------------------|
| Bumping without checking commits | Always run status first to see what's unreleased |
| Tagging with uncommitted changes | Working tree must be clean — commit or stash first |
| Manual changelog entries | Let the skill auto-generate from structured commits |
| Pushing tags automatically | Never auto-push — let the user decide when to push |
| Using lightweight tags | Always use annotated tags (git tag -a) |

## Integration

- **Calls:** `git mode=commit` (for release commit), `notify template=release` (optional)
- **Called by:** user directly, or as part of a release workflow
- **Depends on:** Structured `---agent---` commit blocks for changelog generation
