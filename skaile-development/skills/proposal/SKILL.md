---
name: "skaile-dev-design-spec"
description: "Create, review, or update a design spec / proposal for new features, architectural changes, or cross-package work in the skaile-dev monorepo. Use when work is too complex to jump straight into code — the spec captures the problem, design, alternatives, and approval status before implementation begins."
metadata:
  version: "1.0.0"
  tags:
    - "proposal"
    - "spec"
    - "design"
    - "architecture"
    - "rfc"
    - "skaile-development"
  source: "MERGED"
  stage: "beta"
  prerequisites:
    files:
      - path: "skaile-dev/CLAUDE.md"
        gate: soft
        description: "Monorepo CLAUDE.md for context on package structure and conventions"
    inputs_required:
      - id: topic
        label: "What is the proposal about? (plain language)"
        type: text
      - id: mode
        label: "Mode"
        type: select
        options:
          - "create"
          - "review"
          - "update"
        default: "create"
    inputs_optional:
      - id: spec_path
        label: "Path to existing spec (for review/update mode)"
        type: text
      - id: affected_packages
        label: "Affected packages if known"
        type: text
  produces:
    - path: "docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md"
      description: "The design spec with structured frontmatter"
    - path: "_devlog/reports/YYYY-MM-DD-<topic>.md"
      description: "Detailed devlog report (for architectural proposals)"
    - path: "_devlog/DEVLOG.md"
      description: "Short devlog entry linking to spec and report"
---

ROLE  Design spec author — creates well-structured proposals that capture the problem,
explore alternatives, present a design with concrete types and interfaces, and track
authorship and review status through frontmatter metadata.

READS
  skaile-dev/CLAUDE.md                       — monorepo structure and conventions
  <affected-package>/CLAUDE.md               — package architecture for context
  docs/superpowers/specs/                    — existing specs for format reference
  _devlog/DEVLOG.md                          — recent changes for context
  references/spec-template.md                — the canonical spec template

WRITES
  docs/superpowers/specs/<date>-<topic>-design.md  — the spec itself
  _devlog/reports/<date>-<topic>.md          — detailed report (if architectural)
  _devlog/DEVLOG.md                          — short entry (prepended)

MUST  use the frontmatter format from references/spec-template.md
MUST  set author field to the person creating the spec
MUST  leave reviewer field empty until someone explicitly reviews
MUST  set status to 'draft' when creating, 'review' when requesting review
MUST  explore the existing codebase before designing — read CLAUDE.md files, key source files
MUST  propose 2-3 alternatives with trade-offs before settling on a design
MUST  include concrete TypeScript types/interfaces for all new APIs
MUST  include a dependency graph showing how new packages relate to existing ones
NEVER  write implementation code — specs are design documents
NEVER  skip the alternatives section — even if one approach is obvious, document why

# ── Modes ────────────────────────────────────────────────────────

## Mode: create

STEP 1: Understand the problem
  - Read the monorepo CLAUDE.md for package structure
  - Read CLAUDE.md files for all affected packages
  - Read relevant source files to understand current implementation
  - Identify what exists today and what gaps this proposal fills

STEP 2: Explore alternatives
  - Identify 2-3 viable approaches
  - For each: describe the approach, list pros/cons, state your recommendation
  - Present to the user and discuss before proceeding to detailed design

STEP 3: Design
  - Write concrete TypeScript interfaces and types for all new APIs
  - Draw dependency graphs showing package relationships
  - Describe data flow with ASCII diagrams where helpful
  - Document key design decisions inline (what was decided and why)
  - Address integration points: how does this connect to existing code?
  - List non-goals explicitly

STEP 4: Write the spec
  - Use the template from references/spec-template.md
  - Fill in all frontmatter fields:
    - title: 3-8 words, plain language
    - date: today
    - status: draft
    - author: the person creating this (ask if not known)
    - reviewer: leave empty
    - scope: affected packages
    - type: proposal | design | rfc | adr
  - Save to docs/superpowers/specs/YYYY-MM-DD-<topic>-design.md

STEP 5: Spec review
  - Run a spec-document-reviewer subagent to check for:
    - Circular dependencies
    - Type inconsistencies across sections
    - Missing integration points
    - Feasibility against existing codebase
  - Fix issues found, re-review (max 3 iterations)

STEP 6: Devlog
  - If the proposal is architectural (new packages, new patterns, breaking changes):
    - Write a detailed report to _devlog/reports/<date>-<topic>.md
    - Include "Thought Process" section documenting the reasoning path
  - Always write a short entry to _devlog/DEVLOG.md with status "(proposal)"
  - Trigger the `skaile-dev-devlog` skill or write entries directly

STEP 7: Commit
  - Stage spec + devlog files
  - Commit with: `docs: <topic> design spec (<status>)`
  - Use the `skaile-dev-git` skill's structured commit format

## Mode: review

For reviewing an existing spec. Changes status from 'draft' to 'review' or 'approved'.

STEP 1: Read the spec at the provided path
STEP 2: Read all CLAUDE.md files for affected packages
STEP 3: Read relevant source files to validate feasibility
STEP 4: Check for:
  - Circular dependencies in the proposed package graph
  - Type/interface inconsistencies across sections
  - Missing error cases or edge cases
  - Feasibility given current codebase state
  - Over-engineering (unnecessary abstractions)
  - Under-specification (ambiguous interfaces)
STEP 5: Report findings as Blocker / Issue / Suggestion
STEP 6: If approving:
  - Update frontmatter: status → 'approved', reviewer → reviewer name
  - Commit the change

## Mode: update

For revising an existing spec based on feedback or changed requirements.

STEP 1: Read the existing spec
STEP 2: Understand what changed and why
STEP 3: Update the relevant sections
STEP 4: If status was 'approved', change to 'review' (re-review needed)
STEP 5: Update the devlog entry if the change is significant
STEP 6: Commit with: `docs: update <topic> design spec`

# ── Frontmatter Fields ───────────────────────────────────────────

## Status Lifecycle

```
draft → review → approved → implementing → complete
                    ↓
                abandoned
```

| Status | Meaning |
|--------|---------|
| `draft` | Author is still writing or iterating |
| `review` | Ready for review by another developer |
| `approved` | Reviewed and approved — ready for implementation |
| `implementing` | Implementation is in progress |
| `complete` | Implementation is done and verified |
| `abandoned` | Spec was rejected or superseded |

## Type Field

| Type | When to use |
|------|------------|
| `proposal` | New feature or capability that needs discussion |
| `design` | Detailed technical design for an approved feature |
| `rfc` | Request for comments — soliciting broad input |
| `adr` | Architecture Decision Record — documenting a specific decision |

## Author and Reviewer

- **author**: The person (or agent session) that created the spec. Set on creation, never changed.
- **reviewer**: The person who reviewed and approved the spec. Empty until reviewed. Set when status changes to 'approved'. Multiple reviewers can be comma-separated.

These fields create accountability: you can always trace who proposed what and who approved it.

# ── Quality Checklist ────────────────────────────────────────────

Before marking a spec as ready for review:

- [ ] Problem section describes the pain without mentioning the solution
- [ ] Context section links to relevant CLAUDE.md sections and source files
- [ ] At least 2 alternatives were considered with trade-offs documented
- [ ] All new types/interfaces have concrete TypeScript definitions
- [ ] Dependency graph is acyclic and follows existing conventions
- [ ] Non-goals are explicitly stated
- [ ] Open questions have recommendations and "resolve before" notes
- [ ] Frontmatter is complete (title, date, status, author, scope, type)
