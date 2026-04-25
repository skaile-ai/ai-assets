---
name: skaile-safety
type: ruleset
version: 1.0.0
description: Non-negotiable operational rules — non-destructive defaults, artifact validation, gate checking.
---

# Skaile — Rules

## Must Always

- Validate `agent.yaml` with `npx gitagent validate` before declaring any agent configuration complete.
- Read existing `_concept/` artifacts before starting conceptualization or implementation work.
- Check `pipeline.json` for dependency gates before dispatching a skill.
- Surface blocked gates with explicit reasons before stopping.
- Use the shared contracts in `ai-assets/skaileup-shared/contracts/` as the authority for all schema and naming decisions.
- Respect `iron_laws.md` in `skaileup-shared/contracts/` — these are non-negotiable.

## Must Never

- Overwrite user files without explicit approval.
- Invent stack-specific implementations during conceptualization — only semantic types.
- Skip verification steps after feature implementation.
- Merge or rename shared contracts without updating all referencing skills.
- Commit on behalf of the user without showing the diff first.
- Apply cross-reference repairs without showing the diff.

## Output Constraints

- All concept artifacts must include YAML frontmatter with `last_updated` and `cross_refs`.
- Feature and screen groups must share numbering (e.g., `02_features/01_user_auth/` <> `03_screens/01_user_auth/`).
- Entities use PascalCase, fields use snake_case, enums use SCREAMING_SNAKE_CASE.

## Interaction Boundaries

- Do not perform work outside the target `_concept/` or project directories.
- Do not call external APIs during concept generation — only during `_grounding/` research mode.

## Safety & Ethics

- Do not generate code for destructive operations (mass deletes, schema drops) without explicit user confirmation.
- Surface security implications when designing authentication, authorization, or data storage features.
