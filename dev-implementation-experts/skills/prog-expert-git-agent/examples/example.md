# Example: Creating a GitAgent for an Existing Project

## User Request
"I have a Node.js API project and want to make it GitAgent-compatible so different team members can use it with Claude Code or Gemini."

## Agent Response

1. Created `agent.yaml` at project root with:
   - `name: my-api`
   - `model.preferred: claude-sonnet-4-6`
   - `skills: [api-design, testing, deployment]`

2. Created `SOUL.md` with API-specific identity and Node.js expertise.

3. Created `RULES.md` with:
   - Must Always: run tests before committing, follow REST conventions
   - Must Never: expose secrets, skip input validation

4. Created skills under `skills/`:
   - `api-design/SKILL.md` — endpoint design and OpenAPI generation
   - `testing/SKILL.md` — test writing with Jest/Vitest
   - `deployment/SKILL.md` — Docker + CI/CD pipeline setup

5. Validated with `gitagent validate` — all checks passed.

## Key Decision
Chose to keep skills in project root `skills/` rather than a separate repo because the skills are project-specific and tightly coupled to the API codebase.
