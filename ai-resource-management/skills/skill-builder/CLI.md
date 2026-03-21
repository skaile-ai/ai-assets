# Skill Builder CLI

The Skill Builder skill provides a command-line script to scaffold new production-ready Agent skills.

## Commands

### `scaffold_skill.py`

Scaffolds a new skill directory with a standard layout including directories for scripts, examples, references, assets, and resources. It generates a well-structured `SKILL.md` template and a boilerplate PEP 723 execution script following the progressive disclosure principle.

#### Usage

```bash
uv run scripts/scaffold_skill.py <skill-name> "<description>" [scope]
```

#### Arguments

- `skill-name` (Required): The name of the skill. Must be lowercase alphanumeric with hyphens only.
- `description` (Required): A short description of what the skill does (injected into the `SKILL.md` template).
- `scope` (Optional): The scope/location of the skill. Defaults to `global`.
  - `global`: Scaffolds the skill in `~/.gemini/workspace/skills/<skill-name>`.
  - `local`: Scaffolds the skill in the current working directory under `.agent/skills/<skill-name>`.

#### Examples

Scaffold a global skill:
```bash
uv run scripts/scaffold_skill.py python-tester "generate comprehensive pytest suites"
```

Scaffold a local skill:
```bash
uv run scripts/scaffold_skill.py project-linter "lint the current project directory" local
```

## Suggested Subcommands

Here are some logical subcommands and features that could be added in the future to expand the capabilities of this tool:

1. **Convert to Typer**: Migrate the CLI from raw `sys.argv` parsing to a structured framework like `Typer` or `argparse`. This would instantly provide built-in `--help` text, robust parameter validation, and better error handling for missing arguments.
2. **`lint` / `verify` subcommand**: A command to validate an existing skill directory. It would check if the skill complies with the required structure, ensure `SKILL.md` contains mandatory sections, and verify that scripts are properly formatted with PEP 723 metadata.
3. **`add-script` subcommand**: A command to easily scaffold a new isolated Python script inside an existing skill's `scripts/` directory, automatically adding the boilerplate JSON output structure and error handling required for agent execution.
