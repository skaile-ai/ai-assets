---
name: "skaildev-mattermost"
description: "Post messages, announcements, and devlog summaries to the skaile Mattermost instance. Supports markdown-formatted posts, threads, reactions, file uploads, and channel discovery. Uses mcp2cli with the baked @mattermost tool."
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
    files: []
---

# Mattermost — Team Messaging for skaile-dev

## Overview

Post messages to the skaile Mattermost instance via `mcp2cli @mattermost`. This skill
handles channel discovery, markdown-safe message posting, threading, reactions, and file uploads.

Depends on the `mcp2cli` skill for the underlying CLI tool. The `@mattermost` baked configuration
provides pre-configured auth and connection settings.

## When to Use

- Publishing a devlog summary or proposal to Town Square
- Posting build/deploy status updates to a channel
- Sharing a formatted report, analysis, or announcement
- Threading a follow-up reply on an existing post
- Uploading files to a channel

## When NOT to Use

- For git operations (use `skaildev-git-workflow`)
- For writing the devlog entry itself (use `skaildev-devlog`, then this skill to share it)

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

EMIT [skaildev-mattermost] started

# ── Channel Resolution ──────────────────────────────────────────

STEP 1: Resolve channel ID

  IF channel input is a 26-character ID → use directly
  IF channel input is a name (e.g. "town-square") → look up in Known Channels table above
  IF not in table → resolve dynamically:

  ```bash
  uvx mcp2cli @mattermost get-channel-by-name \
    --team-id dwauaegfaffxfc9u3uthgs1oze \
    --channel-name <name>
  ```

  Extract `id` from the JSON response.

# ── Message Posting ─────────────────────────────────────────────

STEP 2: Prepare the message

  IF message input is provided → use it
  IF message input is empty → generate from context (e.g. summarize a devlog entry, format a report)

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

EMIT [skaildev-mattermost] posted channel=<channel> post_id=<id>

# ── Markdown Formatting Rules ───────────────────────────────────

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

# ── Threading ───────────────────────────────────────────────────

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

# ── Message Management ──────────────────────────────────────────

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

# ── File Uploads ────────────────────────────────────────────────

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

# ── Reading Messages ────────────────────────────────────────────

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

EMIT [skaildev-mattermost] completed

CHECKLIST
  - [ ] Channel resolved (ID, not just name)
  - [ ] Message uses `--stdin` JSON for multi-line content
  - [ ] Tables have blank line before header row
  - [ ] Tables use `|:---` alignment markers
  - [ ] Post ID saved from response (for edits/threads/reactions)

## Integration

- **Called by:** `skaildev-devlog` (optional: share summary to Mattermost after writing devlog)
- **Depends on:** `mcp2cli` skill (baked `@mattermost` tool)
- **References:** Mattermost instance at `https://mattermost.ecsplico.de`
