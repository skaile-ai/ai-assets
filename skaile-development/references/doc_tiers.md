# Documentation Tier Roles in skaile-dev

Every change to the skaile-dev monorepo may need to be reflected in up to five different
documentation surfaces. Each has a distinct audience, scope, and update trigger.

> **TypeScript source documentation and README structure:** See `references/doc_pattern.md`.
> That reference defines TSDoc conventions (Tier 0 — prerequisite before any surface update)
> and the mandatory README.md Purpose section. This file covers which surface to update;
> `doc_pattern.md` covers how to write it.

---

## The Five Tiers

### 1. README.md — User-Facing Overview

**Audience:** End users, package consumers, anyone evaluating the package.
**Location:** `<package>/README.md`
**Purpose:** What the package is, how to install it, how to use it. Quick-start focused.

**Update when:**
- Public API, commands, or usage patterns change
- Installation steps change
- New capabilities are added that users need to know about

**Do NOT include:**
- Architecture internals
- Debugging tips
- Environment variable tables (link to CLAUDE.md for those)
- Anything a user doesn't need to operate the package

**Format:** No frontmatter. H1 = package name. Code blocks for every command.

---

### 2. CLAUDE.md — Developer and AI Agent Guide

**Audience:** Developers working on the package, AI coding agents.
**Location:** `<package>/CLAUDE.md`
**Purpose:** How the package is built, what conventions to follow, what not to break.

**Update when:**
- Architecture or module structure changes
- A new non-obvious convention is established
- Environment variables are added or changed
- The build/test/run commands change
- A gotcha or pitfall is discovered

**Do NOT include:**
- User-facing usage instructions (those go in README.md)
- Long conceptual background (brief it; link to Starlight docs for depth)

**Format:** No frontmatter. Sections: What This Is, Architecture, Tech Stack, Key Conventions, Environment Variables, Common Tasks.

---

### 3. Starlight Docs — Reference Documentation

**Audience:** All users and developers needing in-depth reference.
**Location:** `docs/src/content/docs/<pkg>/` (registered in `docs/astro.config.mjs`)
**Purpose:** Authoritative reference — commands, configuration, API, tutorials.

**Update when:**
- New commands or flags are added to a CLI tool
- Configuration options change (env vars, config files)
- A new public API surface is introduced
- Existing behavior changes in a documented way

**Uses `_sources` frontmatter** to track which source files each page documents.
This allows `update-skaile-docs` to detect stale sections automatically.

```yaml
---
title: Page Title
description: One-line description.
_sources:
  - path: "agent-framework/cli/src/commands/run.ts"
    sections: ["## Run Command"]
    description: "run command implementation"
_source_hash: "a1b2c3d4"
_last_synced: "2026-03-25"
---
```

**Do NOT create a Starlight page** for every change. Add to existing pages first.
Only create new pages when there is enough material (≥ 3 significant topics).

---

### 4. DOMAIN.md / SKILL.md — AI Resource Guides

**Audience:** AI agents browsing the skill catalog, skill authors.
**Location:** `ai-assets/<domain>/DOMAIN.md` or `ai-assets/<domain>/skills/<skill>/SKILL.md`
**Purpose:** Explain what a domain/skill does, when to use it, what it reads/writes.

**Update when:**
- A skill's behavior, inputs, outputs, or `when to use` conditions change
- A new skill is added to a domain (update the domain's skills table)
- A skill is renamed or removed
- Prerequisites or produces paths change

**DOMAIN.md sections:** Purpose, Skills table, Contracts, Notes.
**SKILL.md sections:** YAML frontmatter + body with ROLE, READS, WRITES, MUST/NEVER rules, workflow.

---

### 5. _devlog — Development Log (Plain Language)

**Audience:** Human developers reviewing what changed and why; AI agents needing session context.
**Location:** `_devlog/DEVLOG.md` (short entries) + `_devlog/reports/<date>-<topic>.md` (detailed reports)
**Purpose:** Human-readable record of what changed, why it changed, and what the implications are.

**Update after EVERY meaningful change** — no exceptions. This is the institutional memory.

**Short entry:** 1–3 sentences per change. Plain language. No jargon. Who-what-why-implications.
**Detailed report:** Only when a **concept or architecture changes fundamentally**. Full analysis,
migration guidance, rationale, and downstream effects.

**What makes a change "report-worthy":**
- A shared contract changes (affects all skills using it)
- A core architectural pattern is replaced
- A breaking API change affects multiple packages
- A new development paradigm is introduced
- A security-related change is made

**What makes a change "short entry only":**
- A bug fix
- A new skill or domain added (unless it changes how the system works)
- A feature added to a single package
- Dependency updates
- Documentation-only changes

---

## Decision Table: Which Tiers to Update?

| Change type | README | CLAUDE.md | Starlight | DOMAIN/SKILL | _devlog |
|-------------|--------|-----------|-----------|--------------|---------|
| New or modified exported TypeScript symbol | No | No | Yes (api-reference, generated) | No | No |
| New public CLI command | Yes | Maybe | Yes | No | Yes |
| New env variable | No | Yes | Yes (if configurable) | No | Yes |
| Architecture refactor | No | Yes | Maybe | No | Yes + Report |
| New AI skill | No | No | No | Yes (SKILL.md + DOMAIN.md) | Yes |
| Shared contract change | No | No | No | Yes (all affected SKILL.md) | Yes + Report |
| Bug fix | No | No | No | No | Yes |
| New package added | Yes | Yes | Yes | No | Yes |
| Breaking API change | Yes | Yes | Yes | No | Yes + Report |
| Test-only change | No | No | No | No | Yes (brief) |
| Dependency update | No | No | No | No | Yes (brief) |

> **TSDoc prerequisite:** For any change involving TypeScript exported symbols, annotate with
> TSDoc (`/** */`) *before* consulting this table. See `doc_pattern.md` for conventions.

---

## The Devlog is NOT a Substitute

The devlog answers "what changed and why in plain language." It does NOT replace:
- README.md (users need current instructions, not history)
- CLAUDE.md (agents need current architecture, not change history)
- Starlight docs (reference must be accurate and current)

The devlog is the **human-readable audit trail**. The other tiers are the **living documentation**.
Both must be maintained.
