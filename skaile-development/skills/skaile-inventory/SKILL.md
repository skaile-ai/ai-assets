---
name: "skaile-inventory"
description: "[skaile-development] Index of what already exists across the skaile-ai GitHub
  org: about 85 repos (platform, workspaces, sync, store, the forge monorepo of @skaile/*
  packages, apps and templates, MCP servers, skill collections, customer pilots), each with
  its status and what it replaces. Use before building a new capability, package, app,
  integration, connector, MCP server or skill; when a plan or design note names something to
  build; and when asked \"do we already have X?\" or \"where does X live?\"."
version: 1.0.0
metadata:
  tags:
  - "inventory"
  - "reuse"
  - "architecture"
  - "skaile-development"
  source: "NEW"
  stage: "beta"
---

# Skaile inventory

The skaile-ai org already holds most of the building blocks a new feature reaches for:
chat UIs, action catalogues, approval inboxes, provider settings, connectors, MCP servers,
editors, report and deck renderers. This skill makes the reuse check a fixed step, so new
work starts from what exists rather than beside it.

The index itself is [`references/inventory.md`](references/inventory.md). Read it in full
the first time in a session; it is about 250 lines.

## Steps

1. **Name the need** in one plain sentence about the outcome ("an agent asks a person for
   approval and waits"), not the implementation ("a new table and a polling endpoint").
2. **Look it up.** Scan the *I need… → reach for* table in the inventory, then the repo
   section for the stack you are in. A need often has a hit in a stack other than yours
   (a forge package solving the problem a platform lib is about to solve); list it anyway.
3. **Read the candidate.** For every hit, read its README and `AGENTS.md` / `CLAUDE.md`
   (`gh api repos/skaile-ai/<repo>/readme -H 'Accept: application/vnd.github.raw'`, and
   `gh api repos/skaile-ai/<repo>/contents/AGENTS.md -H 'Accept: application/vnd.github.raw'`,
   or the local checkout). The inventory is a pointer, and the repo is the truth; trust the repo where
   they disagree.
4. **Decide per candidate**: *reuse* (depend on it as is), *extend* (change it upstream so it
   covers the need), *port* (the idea fits, the stack does not: copy the design, cite the
   source) or *build* (nothing fits). A *build* names the reason the candidates fail.
5. **Record the verdict** where the work is planned: the plan, design note, issue or PR body
   gets one line per candidate, `<candidate> — reuse | extend | port | build: <reason>`.

**Done when** every candidate the inventory and step 2 surfaced has a recorded verdict, or
the record says "no candidate in skaile-inventory" for a need the index does not cover.

## Superseded repos

Fifteen single-package and app repos (mostly `forge-*`, plus `skaile-forge`
and `skaile-model`) were imported into the `skaile-ai/forge` monorepo on
2026-11-09 and are frozen, without an archive flag or a README notice. A change to one of
those packages goes to `forge/packages/<name>` or `forge/apps/<name>`. The inventory's
*Superseded and archived* section lists each repo and where it went.

## Keeping it current

Each inventory section carries the date it was surveyed. When a repo you read contradicts
its row, or `gh repo list skaile-ai` shows a repo the index lacks, fix the row in the same
change that discovered it (this skill lives in `skaile-ai/ai-assets`,
`skaile-development/skills/skaile-inventory/`).
