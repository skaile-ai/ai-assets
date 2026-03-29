---
name: mm-post-to-thread
description: Use when you have completed significant work and need to post a structured progress update or completion summary to the Mattermost thread that initiated this session.
license: MIT
metadata:
  version: "1.0.0"
  tags: [mattermost, reporting, completion, progress, thread, communication]
  stage: stable
  source: CF
  prerequisites:
    inputs_optional:
      - id: pr_url
        label: "PR URL (if a PR was created)"
        type: text
        required: false
      - id: summary
        label: "Summary of what was accomplished"
        type: textarea
        required: true
---

## Overview

This skill defines how to structure and deliver output back to the Mattermost thread.
You do not call any API. You write formatted Markdown text — the bot infrastructure
automatically posts everything you output to the thread.

## When to Use

- After creating a pull request (include the PR URL)
- After completing a significant implementation step worth reporting
- When hitting a blocker that requires user input
- At the end of every session turn, if meaningful work was done

## When NOT to Use

- For every minor action (reading a file, running a small command)
- Mid-implementation when the work is not yet in a reportable state
- When the content would duplicate what was already streamed in the turn

## Completion Report Format

After creating a PR, post a completion report with this structure:

```
**Task complete.**

**PR:** <pr_url>

**Summary:**
- <What was changed, in 1-3 bullet points>

**Branch:** `<branch-name>`
**Repo:** `<owner/repo>`
```

Keep it short. The PR description has the full details.

## Progress Update Format

For intermediate status updates (e.g. "cloned repo, exploring codebase"):

```
**Status:** <one-sentence description of current state>
```

Reserve these for long-running tasks where the user would otherwise see nothing for >30 seconds.

## Blocker Format

When you cannot proceed without user input:

```
**Blocked:** <one sentence describing the blocker>

<The specific question you need answered.>
```

## Common Mistakes

| Rationalization | Reality |
|---|---|
| "I'll post a status update every 30 seconds" | Reserve updates for meaningful milestones — too many posts are noise |
| "I need to explain every step" | Lead with the result, not the process |
| "The PR URL is in the output already" | Always post a dedicated completion report — it anchors the thread |
