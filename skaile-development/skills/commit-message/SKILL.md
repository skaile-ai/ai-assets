---
name: commit-message
description: Generate a structured commit message with agent-readable metadata, migration guides, and architecture decisions per commit-spec.md
source: MERGED
version: 1.0.0
keywords: [commit, git, changelog, migration, decisions]
reads_from:
  - commit-spec.md
writes_to: []
---

# Structured Commit Message

You are writing a structured commit message for the skaile-dev monorepo. The message must follow the format defined in `commit-spec.md` (co-located in this skill directory).

## Instructions

### Step 1: Gather Context

1. Run `git diff --cached` (for staged commits) or `git diff main...HEAD` (for squash-merge prep) to see all changes.
2. Identify the packages modified by mapping changed file paths to their package roots (e.g. `agent-framework/session/src/foo.ts` → `agent-framework/session`).
3. For each modified package, read its `CLAUDE.md` to understand architecture and conventions.
4. Identify downstream packages that import from or depend on the modified packages — these are candidates for the `affects` field.

### Step 2: Analyze Changes

For each modified package:

1. **Changes**: List each discrete change in imperative mood.
2. **Exports**: Check if public API surface changed:
   - New types, functions, or classes exported → `+`
   - Changed signatures or behavior of existing exports → `~`
   - Removed exports → `-`
3. **Breaking**: Determine if any change breaks existing consumers.
4. **Decisions**: Reflect on architectural choices made during this session:
   - What alternatives existed?
   - Why was this approach chosen?
   - Under what conditions should it be revisited?

### Step 3: Determine Migration Impact

For each package in `affects`:

1. What would that package's maintainer need to know or do?
2. Is the migration required (breaking) or recommended (non-breaking enhancement)?
3. Write concrete, actionable instructions — not "update as needed" but specific function calls, config changes, or patterns to adopt.

### Step 4: Write the Message

Follow this exact structure:

```
<type>(<scope>): <title>

<human-description>

---agent---
scope: [<package-paths>]
type: <type>
breaking: <true|false>
affects: [<downstream-packages>]

changes:
- <change 1>
- <change 2>

decisions:
- <decision summary>
  reason: <why>
  alternatives: [<alt1>, <alt2>]
  revisit_when: <condition>

migrate:
- <package>: <what to do>

exports:
<+|~|-> <symbol> (<kind>) from|in <package>
```

### Rules

- **Title**: max 72 chars, imperative mood, lowercase, no period
- **Human description**: 1-3 sentences. What and why, not how.
- **scope**: list of package paths, not npm package names
- **type**: `feat|fix|refactor|docs|test|chore|perf|build`
- **changes**: imperative mood, each entry is one discrete change
- **decisions**: only include when a non-trivial choice was made. Omit for obvious/mechanical changes.
- **migrate**: only include when `affects` is non-empty and action is needed
- **exports**: only include when public API surface changed
- Omit optional sections entirely rather than leaving them empty

### Output

Output the complete commit message ready to be used with `git commit -m`. Do not wrap it in a code block or add commentary.
