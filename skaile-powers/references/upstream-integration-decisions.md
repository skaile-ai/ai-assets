# Upstream Integration Decisions — mattpocock/skills

Per-item record of which `mattpocock/skills` capabilities were adopted into
skaile-powers and why. Consulted during upstream sync (see DOMAIN.md →
Upstream Sync) so settled decisions are not re-litigated.

Reviewed: 2026-05-16 against mattpocock/skills (main).

## The Verdict

| mattpocock item | Verdict | Lands as |
|---|---|---|
| `improve-codebase-architecture` (+ `LANGUAGE.md`, `INTERFACE-DESIGN.md`, `DEEPENING.md`) | Adopt — new skill | 16th skill `improving-codebase-architecture`, new `architecture` group |
| `prototype` (+ `LOGIC.md`, `UI.md`) | Adopt — new skill | 17th skill `prototype`, `planning` group |
| `tdd` reference files (`mocking`, `tests`, `deep-modules`, `interface-design`, `refactoring`) | Adopt — enrich | New reference files under `test-driven-development/references/` |
| `zoom-out` | Adopt — technique | Folded into `systematic-debugging` + `improving-codebase-architecture`; no standalone skill |
| `to-issues` vertical-slice principle | Adopt — technique | One paragraph into `writing-plans` |
| `to-prd`, `to-issues`, `triage` (as skills) | Skip | spec+bead model already covers plan → grabbable units |
| `handoff` | Skip | beads + numbered specs are already durable handoff state |
| `caveman` | Skip | conflicts with skaile-powers' structured-output discipline |
| `grill-with-docs` | Already integrated | grilling + glossary + ADRs are in `brainstorming`/`grill-me` |
| `git-guardrails-claude-code`, `setup-pre-commit` | Skip | skaile-dev already has commit-msg hooks + CI; greenfield-setup skills |
| `diagnose` | Skip | `systematic-debugging` already covers root-cause-first debugging |
| `migrate-to-shoehorn`, `scaffold-exercises`, `obsidian-vault`, `edit-article`, `write-a-skill`, `setup-matt-pocock-skills` | Skip | TS-library-specific, personal, or superseded by skaile-powers equivalents (`writing-skills`, `config.md`) |

Net change: **15 → 17 skills**, plus reference enrichment, three folded-in
techniques, a fifth artifact type, and one orchestrator agent.

## Skip Rationale

**`to-prd`, `to-issues`, `triage`** — The spec+bead model already covers plan → grabbable units. These skills duplicate what `brainstorming` + `writing-plans` already do in skaile-powers.

**`handoff`** — Beads + numbered specs are already durable handoff state. The skaile-powers artifact model (spec + plan + devlog) provides everything a handoff needs.

**`caveman`** — Conflicts with skaile-powers' structured-output discipline. Skaile-powers produces typed, linkable artifacts; caveman-style freeform output undermines that.

**`grill-with-docs`** — Already integrated: grilling + glossary + ADRs are in `brainstorming`/`grill-me`. The docs-grounding technique was the origin of the skaile-powers glossary approach.

**`git-guardrails-claude-code`, `setup-pre-commit`** — Greenfield-setup skills. skaile-dev already has commit-msg hooks + CI. Not applicable to an established project.

**`diagnose`** — `systematic-debugging` already covers root-cause-first debugging. The techniques overlap; `systematic-debugging` is more detailed.

**`migrate-to-shoehorn`, `scaffold-exercises`, `obsidian-vault`, `edit-article`, `write-a-skill`, `setup-matt-pocock-skills`** — TypeScript-library-specific, personal-workflow, or superseded by skaile-powers equivalents (`writing-skills`, `config.md`).
