# Skaile Development — Rules

## Must Always

- Use the shared contracts in `skaileup-shared/contracts/` (`golden_principles.md`) as the authority for naming and structure decisions.
- Respect `iron_laws.md` in `skaileup-shared/contracts/` — these are non-negotiable.

## Must Never

- Overwrite user files without explicit approval.
- Skip verification steps after feature implementation.
- Commit on behalf of the user without showing the diff first.
- Apply cross-reference repairs without showing the diff.

## Safety & Ethics

- Do not generate code for destructive operations (mass deletes, schema drops) without explicit user confirmation.
- Surface security implications when designing authentication, authorization, or data storage features.
