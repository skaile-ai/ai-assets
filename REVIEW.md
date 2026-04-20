# Review Instructions

## What Important means here

Reserve Important for findings that would break skill execution, produce
incorrect agent behavior, or corrupt shared contracts: invalid SKILL.md
frontmatter, broken reads_from/writes_to references, stale contract
paths, and prompts that instruct the agent to do something destructive.
Style and wording suggestions are Nit at most.

## Cap the nits

Report at most five Nits per review. If you found more, say "plus N
similar items" in the summary instead of posting them inline.

## Do not report

- Anything the lint workflow already enforces (lint_concept.py, validate_skill_rules.py)
- Minor wording preferences in skill prompts
- Formatting of markdown body content (as long as it parses correctly)

## Always check

- SKILL.md frontmatter has all required fields (name, description, source, version, keywords)
- reads_from and writes_to match what the skill body actually references
- No hallucinated file paths, function names, or API endpoints in prompts
- Changes to skaileup-shared/contracts/ are backward compatible with existing skills
- DOMAIN.md is updated when skills are added or removed from a domain
- No duplicate skills covering the same use case in the same domain
- source field is correct: MERGED for unified skills, CF/SAXE for variant-specific
- Skills do not reference archived cf/ or saxe/ contracts (use merged versions)

## Content-specific

- Skill prompts use imperative mood and the ROLE/READS/WRITES/MUST/NEVER pattern
- user_inputs have clear prompts and sensible required/optional flags
- Flow definitions (flows/*.json) validate against flow.schema.json
- Contract changes include a note in MIGRATION.md if they rename or remove fields
