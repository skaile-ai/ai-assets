# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "typer>=0.12.0",
#     "rich>=13.0.0",
# ]
# ///

import json
import sys
from pathlib import Path
import typer
from rich.console import Console
from rich.tree import Tree

app = typer.Typer(
    help="Scaffold a new skill domain with DOMAIN.md, full folder structure, and an empty contract skill.",
    add_completion=False,
)
console = Console()


def to_title(name: str) -> str:
    return name.replace("-", " ").replace("_", " ").title()


def write_file(path: Path, content: str) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(content)


@app.command()
def main(
    domain_name: str = typer.Argument(..., help="Domain name in kebab-case (e.g. knowledge-research)"),
    description: str = typer.Argument(..., help="One-sentence purpose of this domain"),
    base_path: Path = typer.Option(Path("."), "--base-path", "-b", help="Parent directory to create the domain in"),
    force: bool = typer.Option(False, "--force", help="Overwrite existing domain (requires explicit confirmation)"),
) -> None:
    """
    Scaffold a new skill domain folder with DOMAIN.md, contracts/, docs/,
    skills/, agents/, prompts/, tools/, and an empty <domain>-contract skill.
    """
    # Validate name
    sanitized = domain_name.replace("-", "").replace("_", "")
    if not sanitized.isalnum() or not domain_name == domain_name.lower():
        typer.secho(
            f"Error: domain name must be lowercase kebab-case (got: {domain_name!r})",
            err=True, fg=typer.colors.RED,
        )
        raise typer.Exit(code=1)

    domain_path = base_path.resolve() / domain_name

    if domain_path.exists() and not force:
        typer.secho(
            f"Error: domain '{domain_name}' already exists at {domain_path}\n"
            "Use --force to overwrite (only after explicit user confirmation).",
            err=True, fg=typer.colors.RED,
        )
        raise typer.Exit(code=1)

    title = to_title(domain_name)
    contract_name = f"{domain_name}-contract"

    # --- DOMAIN.md ---
    domain_md = f"""\
---
name: {domain_name}
description: "{description}"
type: domain
building_blocks:
  contracts: "Shared knowledge: file structures, naming rules, and conventions all domain skills read before operating"
  skills: "Invocable agent skills — user-facing, trigger-driven workflows"
  agents: "Autonomous agents — subagent-dispatched, long-running, or orchestration roles"
  prompts: "Reusable prompt fragments — system prompts, persona definitions, instruction blocks"
  tools: "CLI tools — uv-runnable scripts that skills invoke as shell commands"
  docs: "Domain-level documentation — architecture notes, decision records, and reference material"
stage: alpha
---

# {title}

{description}

<!-- TODO: Expand with 2-3 sentences describing domain scope, what problems it solves, and who uses it. -->

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `contracts/` | Shared contracts — file structures, naming rules, conventions all domain skills read |
| `skills/` | Invocable skills — user-facing, trigger-driven agent workflows |
| `agents/` | Autonomous agents — subagent-dispatched, long-running, or orchestration roles |
| `prompts/` | Reusable prompt fragments — system prompts, persona definitions, instruction blocks |
| `tools/` | CLI tools — `uv`-runnable scripts that skills invoke as shell commands |

## Contract

The `{contract_name}` skill in `contracts/` is the shared bridge for this domain:
- Describes file structures and artifact locations
- Defines naming conventions and frontmatter fields used by all skills
- Is `do_not_invoke: true` — loaded as context, never triggered directly

All skills in this domain must read `contracts/{contract_name}/SKILL.md` before operating.
"""

    # --- Contract SKILL.md ---
    contract_md = f"""\
---
name: {contract_name}
description: "Shared contract for all {domain_name} skills. Describes [TODO: file structures, naming conventions, artifact locations, and shared rules]. REQUIRED reading for all skills in this domain."
metadata:
  type: system
  do_not_invoke: true
  stage: alpha
  requires: []
---

# {title} — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `{domain_name}` skills read this before operating.

## Scope

<!-- TODO: Describe what this contract covers and what is excluded (point to dev-shared for truly cross-domain conventions). -->

## File Structure

<!-- TODO: Document the folder layout and artifact paths this domain reads/writes. -->

```
# Example:
_output/
└── ...
```

## Naming Conventions

<!-- TODO: File naming rules, skill naming prefix, frontmatter fields specific to this domain. -->

## Reads / Writes Protocol

<!-- TODO: What paths skills read from, what paths they write to, and any ordering rules. -->

## Shared Frontmatter Fields

<!-- TODO: YAML fields that every file produced by this domain must include. -->
"""

    # --- Placeholder READMEs ---
    readmes = {
        "docs/README.md": f"# {title} — Docs\n\nDomain-level documentation for `{domain_name}`.\n\n## Contents\n\n<!-- Add links to architecture notes, decision records, and reference material as they are created. -->\n",
        "skills/README.md": f"# {title} — Skills\n\nInvocable agent skills for the `{domain_name}` domain. Each skill lives in its own subdirectory with a `SKILL.md`.\n",
        "agents/README.md": f"# {title} — Agents\n\nAutonomous agents for the `{domain_name}` domain. Use for long-running, subagent-dispatched, or orchestration workflows.\n",
        "prompts/README.md": f"# {title} — Prompts\n\nReusable prompt fragments for the `{domain_name}` domain: system prompts, persona definitions, instruction blocks.\n",
        "tools/README.md": f"# {title} — Tools\n\nCLI tools for the `{domain_name}` domain. All tools are `uv`-runnable single-file Python scripts.\n\n## Usage\n\n```bash\nuv run tools/<tool-name>.py [args]\n```\n",
    }

    # --- Write all files ---
    write_file(domain_path / "DOMAIN.md", domain_md)
    write_file(domain_path / f"contracts/{contract_name}/SKILL.md", contract_md)
    for rel, content in readmes.items():
        write_file(domain_path / rel, content)

    # --- Print result tree ---
    console.print(f"\n[bold green]✓ Domain scaffolded:[/bold green] [cyan]{domain_name}[/cyan]")
    console.print(f"  Path: {domain_path}\n")

    tree = Tree(f"[bold]{domain_name}/[/bold]")
    tree.add("[yellow]DOMAIN.md[/yellow]  ← domain manifest")
    contracts_branch = tree.add("contracts/")
    contract_branch = contracts_branch.add(f"{contract_name}/")
    contract_branch.add("[yellow]SKILL.md[/yellow]  ← scaffold contract (fill in TODO sections)")
    tree.add("docs/    ← domain documentation")
    tree.add("skills/  ← invocable agent skills")
    tree.add("agents/  ← autonomous / subagent workflows")
    tree.add("prompts/ ← reusable prompt fragments")
    tree.add("tools/   ← uv-runnable CLI tools")
    console.print(tree)

    console.print("\n[bold]Next steps:[/bold]")
    console.print(f"  1. Fill in TODO sections in [cyan]{domain_name}/DOMAIN.md[/cyan]")
    console.print(f"  2. Fill in TODO sections in [cyan]contracts/{contract_name}/SKILL.md[/cyan]")
    console.print(f"  3. Use [bold]skill-builder[/bold] to scaffold the first skill in [cyan]{domain_name}/skills/[/cyan]")


if __name__ == "__main__":
    app()
