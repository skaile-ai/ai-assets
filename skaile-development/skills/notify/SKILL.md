---
name: "notify"
description: "Team notifications and messaging for skaile-dev. Supports free-form Mattermost messages plus structured templates for plans, breaking changes, releases, and devlog summaries."
metadata:
  version: "1.0.0"
  tags:
    - "mattermost"
    - "messaging"
    - "notifications"
    - "team-communication"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  dependencies:
    tools:
      - "mcp2cli @mattermost (baked tool, requires uvx mcp2cli + baked config)"
  user_inputs:
    dialog:
      - id: "channel"
        label: "Channel name (e.g. town-square)"
        type: "text"
        required: false
        default: "town-square"
      - id: "message"
        label: "Message content (markdown supported)"
        type: "textarea"
        required: false
        hint: "Leave empty if posting from a file or generating from context"
      - id: "template"
        label: "Notification template (optional)"
        type: "select"
        options: ["plan", "breaking-change", "release", "devlog-summary"]
        required: false
        hint: "Structured notification. Leave empty for free-form messages."
    files: []
---

# Notify — Team Messaging for skaile-dev

## Overview

Post messages to the skaile Mattermost instance via `mcp2cli @mattermost`. This skill
handles channel discovery, markdown-safe message posting, threading, reactions, and file uploads.

Additionally provides structured notification templates for common development events:
plans, breaking changes, releases, and devlog summaries.

Depends on the `mcp2cli` skill for the underlying CLI tool. The `@mattermost` baked configuration
provides pre-configured auth and connection settings.

## When to Use

- Publishing a devlog summary or proposal to Town Square
- Posting build/deploy status updates to a channel
- Sharing a formatted report, analysis, or announcement
- Threading a follow-up reply on an existing post
- Uploading files to a channel
- Sending structured notifications for plans, breaking changes, releases, or devlog reports

## When NOT to Use

- For git operations (use `git`)
- For writing the devlog entry itself (use `devlog`, then this skill to share it)

---

ROLE  Team communicator — posts well-formatted markdown messages to the skaile Mattermost instance.

REQUIRES
  tool: uvx mcp2cli @mattermost    — baked MCP tool with auth configured

## Instance Details

| Field | Value |
|-------|-------|
| URL | `https://mattermost.ecsplico.de` |
| Team | `skaile` |
| Team ID | `dwauaegfaffxfc9u3uthgs1oze` |

### Known Channels

| Channel | ID | Use |
|---------|-----|-----|
| Town Square | `jxfi9jeycpbnbq5eq3r8bu4hio` | Announcements, devlogs, proposals |

To discover additional channels:

```bash
# List your channels
uvx mcp2cli @mattermost list-my-channels --team-id dwauaegfaffxfc9u3uthgs1oze --pretty

# Get a channel by name
uvx mcp2cli @mattermost get-channel-by-name \
  --team-id dwauaegfaffxfc9u3uthgs1oze \
  --channel-name <name>

# List all public channels
uvx mcp2cli @mattermost list-public-channels --team-id dwauaegfaffxfc9u3uthgs1oze --pretty
```

EMIT [notify] started

# -- Channel Resolution -------------------------------------------------------

STEP 1: Resolve channel ID

  IF channel input is a 26-character ID -> use directly
  IF channel input is a name (e.g. "town-square") -> look up in Known Channels table above
  IF not in table -> resolve dynamically:

  ```bash
  uvx mcp2cli @mattermost get-channel-by-name \
    --team-id dwauaegfaffxfc9u3uthgs1oze \
    --channel-name <name>
  ```

  Extract `id` from the JSON response.

# -- Message Posting -----------------------------------------------------------

STEP 2: Prepare the message

  IF template input is provided -> go to the Templates section below and construct the message
  IF message input is provided -> use it
  IF message input is empty -> generate from context (e.g. summarize a devlog entry, format a report)

  Format rules:
  - Use Mattermost-compatible markdown (headers, bold, lists, tables, code blocks)
  - Keep posts focused — one topic per message
  - For long content, lead with a summary then expand in sections

STEP 3: Post the message

  CRITICAL: Always use `--stdin` with JSON for multi-line messages. Shell argument
  expansion destroys newlines and special characters in markdown content.

  ```bash
  python3 -c "
  import json, sys
  msg = '''<message content here>'''
  sys.stdout.write(json.dumps({'channel_id': '<channel-id>', 'message': msg}))
  " | uvx mcp2cli @mattermost post-message --stdin
  ```

  Save the `id` field from the JSON response — needed for edits, threads, and reactions.

  For short single-line messages only, the flag form is acceptable:
  ```bash
  uvx mcp2cli @mattermost post-message --channel-id <id> --message "Short message"
  ```

EMIT [notify] posted channel=<channel> post_id=<id>

# -- Markdown Formatting Rules -------------------------------------------------

Mattermost has specific markdown rendering requirements:

MUST  leave a blank line before every table header row
MUST  use `|:---` alignment markers in table separator rows for reliable rendering
MUST  use `--stdin` JSON for any message containing newlines, tables, or code blocks
NEVER pass multi-line content via shell `--message "$VAR"` — newlines get collapsed
NEVER use `>` blockquote characters inside table cells — they break table rendering
AVOID `--` (double dash) in message text when passing via shell arguments

Example of a correctly formatted table in a message:

```
Some text before the table.

| Column A | Column B |
|:---------|:---------|
| value 1  | value 2  |
| value 3  | value 4  |

Text after the table.
```

# -- Notification Templates ----------------------------------------------------

When the `template` input is set, construct the message using the corresponding template
below. All templates post to the default channel (Town Square) unless a different channel
is specified.

## Template: `plan`

**Purpose:** Announce a new implementation plan to the team.

**Required inputs:**
- Task title (from the plan)
- Branch name
- Complexity tier (from the plan)
- Package list (affected packages)
- Task list (numbered steps from the plan)

**Triggered by:** `implement` Phase 1 (after plan approval), or invoked standalone.

**Channel:** Town Square (default)

**Message format:**

```markdown
## [Plan] <task title>

**Branch:** `<branch-name>`
**Complexity:** <tier>
**Packages:** `<package-list>`

### Tasks
1. <task 1>
2. <task 2>
...
```

**Workflow:**
1. Receive or extract the plan details: task title, branch name, complexity tier, package list, and task list.
2. Substitute the values into the template above.
3. Post using `--stdin` JSON (the message is always multi-line).

---

## Template: `breaking-change`

**Purpose:** Alert the team that a breaking change has been committed.

**Required inputs:**
- Commit title (from the commit message)
- Package scope (affected packages)
- Affects list (downstream packages impacted)
- List of what broke (changes that are breaking)
- Migration instructions per package

**Triggered by:** `implement` Phase 6b when `breaking: true`.

**Channel:** Town Square (default)

**Message format:**

```markdown
## [Breaking Change] <commit title>

**Packages:** `<scope>`
**Affects:** `<affects list>`

### What Broke
- <change 1>
- <change 2>

### Migration
- <package>: <what to do>
```

**Workflow:**
1. Extract breaking change details from the commit's agent block: title, scope, affects, changes, and migrate sections.
2. Substitute the values into the template above.
3. Post using `--stdin` JSON (the message is always multi-line).

---

## Template: `release`

**Purpose:** Announce a new version release to the team.

**Required inputs:**
- Version number (new)
- Domain or package name
- Previous version number
- Highlights (notable features or fixes)
- Breaking changes (if any)

**Triggered by:** `release` skill after a successful bump+tag.

**Channel:** Town Square (default)

**Message format:**

```markdown
## [Release] v<version>

**Domain/Package:** `<name>`
**Previous:** v<old-version>

### Highlights
- <feature 1>
- <feature 2>

### Breaking Changes
- <breaking change> (if any)
```

**Workflow:**
1. Receive or extract release details: new version, package name, previous version, highlights, and any breaking changes.
2. Substitute the values into the template above.
3. If there are no breaking changes, omit the "Breaking Changes" section entirely.
4. Post using `--stdin` JSON (the message is always multi-line).

---

## Template: `devlog-summary`

**Purpose:** Notify the team that a detailed devlog report has been written.

**Required inputs:**
- Report title
- Report type (one of: contract, architecture, breaking-api, paradigm, security)
- Package list (affected packages)
- Summary (2-3 sentences from the report)
- Filename (path to the report under `_devlog/reports/`)

**Triggered by:** `devlog` when a detailed report is generated (optional).

**Channel:** Town Square (default)

**Message format:**

```markdown
## [Devlog] <report title>

**Type:** <contract | architecture | breaking-api | paradigm | security>
**Packages:** `<list>`

### Summary
<2-3 sentences from the report>

See full report: `_devlog/reports/<filename>`
```

**Workflow:**
1. Extract report metadata: title, type, affected packages, and a 2-3 sentence summary.
2. Substitute the values into the template above.
3. Post using `--stdin` JSON (the message is always multi-line).

# -- Threading -----------------------------------------------------------------

STEP 4 (optional): Reply in a thread

  ```bash
  python3 -c "
  import json, sys
  msg = '''<reply content>'''
  sys.stdout.write(json.dumps({
      'channel_id': '<channel-id>',
      'root_id': '<parent-post-id>',
      'message': msg
  }))
  " | uvx mcp2cli @mattermost post-message --stdin
  ```

# -- Message Management --------------------------------------------------------

Edit a posted message:
```bash
uvx mcp2cli @mattermost update-message --post-id <id> --message "Updated content"
```

For multi-line edits, use `--stdin`:
```bash
python3 -c "
import json, sys
msg = '''<updated content>'''
sys.stdout.write(json.dumps({'post_id': '<id>', 'message': msg}))
" | uvx mcp2cli @mattermost update-message --stdin
```

Delete a message:
```bash
uvx mcp2cli @mattermost delete-message --post-id <id>
```

Pin/unpin:
```bash
uvx mcp2cli @mattermost pin-message --post-id <id>
uvx mcp2cli @mattermost unpin-message --post-id <id>
```

Reactions:
```bash
uvx mcp2cli @mattermost add-reaction --post-id <id> --emoji-name thumbsup
uvx mcp2cli @mattermost remove-reaction --post-id <id> --emoji-name thumbsup
```

# -- File Uploads --------------------------------------------------------------

STEP 5 (optional): Upload and attach a file

  ```bash
  # Upload returns file metadata including the file ID
  uvx mcp2cli @mattermost upload-file --channel-id <id> --file /path/to/file.pdf
  ```

  Extract the file ID from the response, then attach to a message:

  ```bash
  python3 -c "
  import json, sys
  sys.stdout.write(json.dumps({
      'channel_id': '<channel-id>',
      'message': 'See attached report',
      'file_ids': ['<file-id-from-upload>']
  }))
  " | uvx mcp2cli @mattermost post-message --stdin
  ```

# -- Reading Messages ----------------------------------------------------------

```bash
# Recent messages in a channel
uvx mcp2cli @mattermost get-channel-messages --channel-id <id> --pretty

# Search across the team
uvx mcp2cli @mattermost search-messages \
  --team-id dwauaegfaffxfc9u3uthgs1oze \
  --terms "search query" --pretty

# Get a full thread
uvx mcp2cli @mattermost get-thread --post-id <id> --pretty
```

EMIT [notify] completed

CHECKLIST
  - [ ] Channel resolved (ID, not just name)
  - [ ] Message uses `--stdin` JSON for multi-line content
  - [ ] Tables have blank line before header row
  - [ ] Tables use `|:---` alignment markers
  - [ ] Post ID saved from response (for edits/threads/reactions)
  - [ ] Template message matches the expected format (if template was used)

## Integration

- **Called by:** `devlog`, `implement`, `release`
- **Depends on:** `mcp2cli` skill (baked `@mattermost` tool)
- **References:** Mattermost instance at `https://mattermost.ecsplico.de`
