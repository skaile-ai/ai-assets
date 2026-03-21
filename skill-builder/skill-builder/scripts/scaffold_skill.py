# /// script
# requires-python = ">=3.12"
# dependencies = [
#     "typer>=0.12.0",
#     "rich>=13.0.0",
#     "InquirerPy>=0.3.4",
#     "pyyaml>=6.0.0",
# ]
# ///
"""
scaffold_skill.py — Skill Builder CLI

Subcommands:
  detect          Detect available skill targets from the current directory
  list-domains    List domains inside an ai-resources folder
  set-default     Save a target as the preferred default
  clear-default   Remove the saved default
  show-default    Print the current saved default
  create          Scaffold a new skill at a resolved target path
"""

import json
import os
import sys
from pathlib import Path
from typing import Optional

import typer
import yaml
from InquirerPy import prompt as inquirer_prompt
from rich.console import Console
from rich.table import Table
from rich.tree import Tree

app = typer.Typer(
    help="Skill Builder — scaffold, detect targets, and manage skill placement.",
    add_completion=False,
)
console = Console()

CONFIG_PATH = Path.home() / ".config" / "skill-builder" / "config.json"

# ---------------------------------------------------------------------------
# Interactive helpers (PyInquirer) — only used when stdin is a TTY
# ---------------------------------------------------------------------------

def _is_tty() -> bool:
    return sys.stdin.isatty() and sys.stdout.isatty()


def _inquire_select(message: str, choices: list[str]) -> str | None:
    """Single-item list selection. Returns chosen value or None if not a TTY."""
    if not _is_tty():
        return None
    answers = inquirer_prompt([{"type": "list", "name": "v", "message": message, "choices": choices}])
    return answers.get("v") if answers else None


def _inquire_confirm(message: str, default: bool = True) -> bool:
    """Yes/no confirmation. Returns default if not a TTY."""
    if not _is_tty():
        return default
    answers = inquirer_prompt([{"type": "confirm", "name": "v", "message": message, "default": default}])
    return answers.get("v", default) if answers else default


def _inquire_input(message: str, default: str = "") -> str:
    """Free-text input. Returns default if not a TTY."""
    if not _is_tty():
        return default
    answers = inquirer_prompt([{"type": "input", "name": "v", "message": message, "default": default}])
    return answers.get("v", default) if answers else default


def _interactive_resolve_target(targets: list[dict], saved: dict | None) -> dict | None:
    """
    Guide the user through target selection interactively.
    Returns a dict with keys: type, path, domain (optional).
    Returns None if the user aborts.
    """
    if not _is_tty():
        return None

    # If saved default is valid, offer it first
    if saved and not saved.get("_stale"):
        label = f"{saved['type']}  {saved['path']}"
        if saved.get("domain"):
            label += f"  (domain: {saved['domain']})"
        use_saved = _inquire_confirm(f"Use saved default: {label}?", default=True)
        if use_saved:
            return saved

    if not targets:
        console.print("[yellow]No targets detected. Enter a custom path.[/yellow]")
        custom_path = _inquire_input("Path to skill folder:")
        if not custom_path:
            return None
        return {"type": "custom", "path": custom_path}

    # Build choice labels → map back to target dicts
    labels = [t["label"] for t in targets] + ["[ custom path ]"]
    chosen_label = _inquire_select("Select skill target:", labels)
    if not chosen_label:
        return None

    if chosen_label == "[ custom path ]":
        custom_path = _inquire_input("Path to skill folder:")
        return {"type": "custom", "path": custom_path} if custom_path else None

    selected = next(t for t in targets if t["label"] == chosen_label)
    return selected


def _interactive_resolve_domain(ai_resources_path: Path, saved_domain: str | None) -> str | None:
    """
    Guide the user through domain selection for an ai-resources target.
    Returns the domain name or None.
    """
    if not _is_tty():
        return saved_domain

    domains = list_domains(ai_resources_path)
    if not domains:
        console.print("[yellow]No domains found. Use domain-builder to create one first.[/yellow]")
        return None

    # If saved domain is still present, offer it first
    if saved_domain and any(d["name"] == saved_domain for d in domains):
        use_saved = _inquire_confirm(f"Use saved domain: {saved_domain}?", default=True)
        if use_saved:
            return saved_domain

    choices = [d["name"] for d in domains] + ["[ create new domain ]"]
    chosen = _inquire_select("Select domain:", choices)
    if not chosen:
        return None
    if chosen == "[ create new domain ]":
        console.print("[bold]Run [cyan]domain-builder[/cyan] to create a new domain first.[/bold]")
        return None
    return chosen


# ---------------------------------------------------------------------------
# Config helpers
# ---------------------------------------------------------------------------

def load_config() -> dict:
    if CONFIG_PATH.exists():
        try:
            return json.loads(CONFIG_PATH.read_text())
        except (json.JSONDecodeError, OSError):
            return {}
    return {}


def save_config(data: dict) -> None:
    CONFIG_PATH.parent.mkdir(parents=True, exist_ok=True)
    CONFIG_PATH.write_text(json.dumps(data, indent=2))


# ---------------------------------------------------------------------------
# Detection helpers
# ---------------------------------------------------------------------------

def find_ai_resources(start: Path) -> list[Path]:
    """Walk up from start and search common locations for ai-resources folders."""
    candidates: list[Path] = []
    current = start.resolve()
    # Walk upward
    for ancestor in [current, *current.parents]:
        candidate = ancestor / "ai-resources"
        if candidate.is_dir() and _looks_like_ai_resources(candidate):
            candidates.append(candidate)
    return candidates


def _looks_like_ai_resources(path: Path) -> bool:
    """Heuristic: has at least one subdir with DOMAIN.md or a skills/ subfolder."""
    for child in path.iterdir():
        if child.is_dir():
            if (child / "DOMAIN.md").exists():
                return True
            if (child / "skills").is_dir():
                return True
    return False


def list_domains(ai_resources_path: Path) -> list[dict]:
    """Return domain entries: {name, path, has_domain_md, skills_count}."""
    domains = []
    for child in sorted(ai_resources_path.iterdir()):
        if not child.is_dir():
            continue
        has_domain_md = (child / "DOMAIN.md").exists()
        skills_dir = child / "skills"
        skills_count = len(list(skills_dir.iterdir())) if skills_dir.is_dir() else 0
        if has_domain_md or skills_dir.is_dir():
            domains.append({
                "name": child.name,
                "path": str(child),
                "has_domain_md": has_domain_md,
                "skills_count": skills_count,
            })
    return domains


def detect_targets(cwd: Path) -> list[dict]:
    """Detect all candidate skill targets from cwd."""
    targets = []

    # ai-resources folders (walk up)
    for ar_path in find_ai_resources(cwd):
        targets.append({
            "type": "ai-resources",
            "path": str(ar_path),
            "label": f"ai-resources  {ar_path}",
            "valid": True,
        })

    # Local .claude/skills
    local_claude = cwd / ".claude" / "skills"
    if not local_claude.exists():
        # Search upward one level (project root)
        for ancestor in cwd.parents:
            candidate = ancestor / ".claude" / "skills"
            if candidate.exists():
                local_claude = candidate
                break
    if local_claude.exists():
        targets.append({
            "type": "local-claude",
            "path": str(local_claude),
            "label": f"local-claude  {local_claude}",
            "valid": True,
        })

    # Local .agents/skills
    local_agents = cwd / ".agents" / "skills"
    if not local_agents.exists():
        for ancestor in cwd.parents:
            candidate = ancestor / ".agents" / "skills"
            if candidate.exists():
                local_agents = candidate
                break
    if local_agents.exists():
        targets.append({
            "type": "local-agents",
            "path": str(local_agents),
            "label": f"local-agents  {local_agents}",
            "valid": True,
        })

    # Global ~/.claude/skills
    global_claude = Path.home() / ".claude" / "skills"
    if global_claude.exists():
        targets.append({
            "type": "global-claude",
            "path": str(global_claude),
            "label": f"global-claude {global_claude}",
            "valid": True,
        })

    return targets


# ---------------------------------------------------------------------------
# Subcommands
# ---------------------------------------------------------------------------

@app.command()
def detect(
    cwd: Optional[Path] = typer.Option(None, "--cwd", help="Directory to detect from (default: current dir)"),
    output_json: bool = typer.Option(False, "--json", help="Output as JSON"),
    interactive: bool = typer.Option(False, "--interactive", "-i",
        help="Interactively select a target and optionally save as default"),
) -> None:
    """Detect available skill targets from the current directory."""
    base = (cwd or Path.cwd()).resolve()
    config = load_config()
    saved = config.get("default_target")

    targets = detect_targets(base)

    # Validate saved default still exists
    if saved:
        saved_path = Path(saved.get("path", ""))
        if not saved_path.exists():
            saved["_stale"] = True

    result = {
        "saved_default": saved,
        "detected": targets,
    }

    if output_json:
        print(json.dumps(result, indent=2))
        return

    # Human-readable output
    if saved:
        stale = saved.get("_stale", False)
        status = "[red](stale — path no longer exists)[/red]" if stale else "[green](valid)[/green]"
        console.print(f"\n[bold]Saved default:[/bold] {status}")
        console.print(f"  type   : {saved.get('type')}")
        console.print(f"  path   : {saved.get('path')}")
        if saved.get("domain"):
            console.print(f"  domain : {saved.get('domain')}")
    else:
        console.print("\n[dim]No saved default.[/dim]")

    if targets:
        console.print("\n[bold]Detected targets:[/bold]")
        for i, t in enumerate(targets, 1):
            console.print(f"  [{i}] {t['label']}")
    else:
        console.print("\n[yellow]No skill targets detected in current directory tree.[/yellow]")
        console.print("  Provide a path explicitly with --base-path when running [bold]create[/bold].")

    # Interactive mode: guide through selection and optional save
    if interactive:
        if not _is_tty():
            typer.secho("Error: --interactive requires a TTY.", err=True, fg=typer.colors.RED)
            raise typer.Exit(1)

        selected = _interactive_resolve_target(targets, saved)
        if not selected:
            console.print("[dim]No target selected.[/dim]")
            return

        # If ai-resources, also pick domain
        domain = selected.get("domain")
        if selected.get("type") == "ai-resources":
            domain = _interactive_resolve_domain(Path(selected["path"]), domain)
            if domain:
                selected["domain"] = domain

        console.print(f"\n[green]Selected:[/green] {selected['type']}  {selected['path']}")
        if selected.get("domain"):
            console.print(f"  domain: {selected['domain']}")

        if _inquire_confirm("Save as default target?", default=True):
            config["default_target"] = {
                "type": selected["type"],
                "path": selected["path"],
                **({"domain": selected["domain"]} if selected.get("domain") else {}),
            }
            save_config(config)
            console.print(f"[green]✓ Saved to {CONFIG_PATH}[/green]")


@app.command(name="list-domains")
def list_domains_cmd(
    base_path: Path = typer.Option(..., "--base-path", "-b", help="Path to an ai-resources folder"),
    output_json: bool = typer.Option(False, "--json", help="Output as JSON"),
) -> None:
    """List domains available inside an ai-resources folder."""
    if not base_path.exists():
        typer.secho(f"Error: path does not exist: {base_path}", err=True, fg=typer.colors.RED)
        raise typer.Exit(1)

    domains = list_domains(base_path.resolve())

    if output_json:
        print(json.dumps(domains, indent=2))
        return

    if not domains:
        console.print(f"[yellow]No domains found in {base_path}[/yellow]")
        console.print("Use [bold]domain-builder[/bold] to create one.")
        return

    table = Table(title=f"Domains in {base_path}")
    table.add_column("#", style="dim")
    table.add_column("Domain", style="cyan bold")
    table.add_column("DOMAIN.md", justify="center")
    table.add_column("Skills", justify="right")

    for i, d in enumerate(domains, 1):
        table.add_row(
            str(i),
            d["name"],
            "✓" if d["has_domain_md"] else "—",
            str(d["skills_count"]),
        )

    console.print(table)


@app.command(name="set-default")
def set_default(
    target_type: str = typer.Option(..., "--type", "-t",
        help="Target type: ai-resources | local-claude | local-agents | global-claude | custom"),
    path: Path = typer.Option(..., "--path", "-p", help="Absolute path to the target folder"),
    domain: Optional[str] = typer.Option(None, "--domain", "-d",
        help="Domain name (required when type is ai-resources)"),
) -> None:
    """Save a target as the preferred default for future skill scaffolding."""
    if target_type == "ai-resources" and not domain:
        typer.secho(
            "Error: --domain is required when type is ai-resources",
            err=True, fg=typer.colors.RED,
        )
        raise typer.Exit(1)

    if not path.exists():
        typer.secho(f"Warning: path does not exist yet: {path}", err=True, fg=typer.colors.YELLOW)

    config = load_config()
    config["default_target"] = {
        "type": target_type,
        "path": str(path.resolve()),
        **({"domain": domain} if domain else {}),
    }
    save_config(config)

    console.print(f"[green]✓ Default saved to {CONFIG_PATH}[/green]")
    console.print(f"  type  : {target_type}")
    console.print(f"  path  : {path.resolve()}")
    if domain:
        console.print(f"  domain: {domain}")


@app.command(name="clear-default")
def clear_default() -> None:
    """Remove the saved default target."""
    config = load_config()
    if "default_target" in config:
        del config["default_target"]
        save_config(config)
        console.print("[green]✓ Default target cleared.[/green]")
    else:
        console.print("[dim]No default target was set.[/dim]")


@app.command(name="show-default")
def show_default(
    output_json: bool = typer.Option(False, "--json"),
) -> None:
    """Print the current saved default target."""
    config = load_config()
    saved = config.get("default_target")
    if output_json:
        print(json.dumps(saved or {}, indent=2))
        return
    if not saved:
        console.print("[dim]No default target saved.[/dim]")
    else:
        console.print(f"type  : {saved.get('type')}")
        console.print(f"path  : {saved.get('path')}")
        if saved.get("domain"):
            console.print(f"domain: {saved.get('domain')}")
        console.print(f"\n[dim]Config: {CONFIG_PATH}[/dim]")


@app.command()
def check(
    base_path: Optional[Path] = typer.Option(None, "--base-path", "-b",
        help="Base path to scan (default: walk upward from cwd to find ai-resources root)"),
    output_json: bool = typer.Option(False, "--json", help="Output as JSON"),
) -> None:
    """Scan all SKILL.md files and report duplicates, broken requires, and missing metadata."""

    # Resolve base path: explicit > walk upward for ai-resources
    if base_path is not None:
        scan_root = base_path.resolve()
    else:
        # Walk upward from cwd looking for a directory named "ai-resources"
        scan_root = None
        current = Path.cwd().resolve()
        for ancestor in [current, *current.parents]:
            candidate = ancestor / "ai-resources"
            if candidate.is_dir():
                scan_root = candidate
                break
            if ancestor.name == "ai-resources" and ancestor.is_dir():
                scan_root = ancestor
                break
        if scan_root is None:
            typer.secho(
                "Error: could not find an 'ai-resources' directory by walking up from cwd. "
                "Use --base-path to specify one explicitly.",
                err=True, fg=typer.colors.RED,
            )
            raise typer.Exit(1)

    # Collect all SKILL.md files
    skill_files = sorted(scan_root.rglob("SKILL.md"))

    # Parse frontmatter for each file
    def _parse_fm(text: str) -> dict | None:
        """Parse YAML frontmatter. Returns dict or None."""
        if not text.startswith("---"):
            return None
        rest = text[3:]
        if rest.startswith("\n"):
            rest = rest[1:]
        idx = rest.find("\n---")
        if idx == -1:
            return None
        fm_str = rest[:idx]
        try:
            return yaml.safe_load(fm_str) or {}
        except Exception:
            return None

    skills: list[dict] = []
    for sf in skill_files:
        try:
            text = sf.read_text(encoding="utf-8")
            fm = _parse_fm(text) or {}
        except Exception:
            fm = {}
        skills.append({
            "path": str(sf),
            "rel_path": str(sf.relative_to(scan_root)) if sf.is_relative_to(scan_root) else str(sf),
            "name": fm.get("name", ""),
            "metadata": fm.get("metadata") or {},
        })

    # Build set of all known names
    all_names: set[str] = {s["name"] for s in skills if s["name"]}

    issues: list[dict] = []

    # Check 1: DUPLICATE NAMES
    from collections import defaultdict
    name_to_paths: dict[str, list[str]] = defaultdict(list)
    for s in skills:
        if s["name"]:
            name_to_paths[s["name"]].append(s["rel_path"])
    for name, paths in name_to_paths.items():
        if len(paths) > 1:
            issues.append({
                "type": "DUPLICATE",
                "name": name,
                "paths": paths,
                "message": f'[DUPLICATE] name "{name}": ' + ", ".join(paths),
            })

    # Check 2: BROKEN REQUIRES
    for s in skills:
        requires = s["metadata"].get("requires") or []
        if isinstance(requires, list):
            for req in requires:
                if req and req not in all_names:
                    issues.append({
                        "type": "BROKEN_REQ",
                        "skill": s["name"] or s["rel_path"],
                        "path": s["rel_path"],
                        "missing": req,
                        "message": f'[BROKEN REQ] skill "{s["name"] or s["rel_path"]}" requires "{req}" — not found in tree',
                    })

    # Check 3: MISSING METADATA
    for s in skills:
        meta = s["metadata"]
        if not isinstance(meta, dict) or "stage" not in meta:
            issues.append({
                "type": "MISSING_META",
                "path": s["rel_path"],
                "field": "metadata.stage",
                "message": f'[MISSING META] {s["rel_path"]} — missing metadata.stage',
            })
        if not isinstance(meta, dict) or "requires" not in meta:
            issues.append({
                "type": "MISSING_META",
                "path": s["rel_path"],
                "field": "metadata.requires",
                "message": f'[MISSING META] {s["rel_path"]} — missing metadata.requires',
            })

    if output_json:
        print(json.dumps({"skills": len(skills), "issues": issues}, indent=2))
        raise typer.Exit(1 if issues else 0)

    if not issues:
        console.print(f"[green]✓ {len(skills)} skills scanned, no issues[/green]")
        raise typer.Exit(0)

    console.print(f"[red]✗ {len(issues)} issues found:[/red]")
    for issue in issues:
        console.print(f"  {issue['message']}")
    raise typer.Exit(1)


@app.command()
def create(
    skill_name: str = typer.Argument(..., help="Skill name in kebab-case"),
    description: str = typer.Argument(..., help="One-line trigger description"),
    target_type: str = typer.Option("ai-resources", "--target-type", "-t",
        help="ai-resources | local-claude | local-agents | global-claude | custom"),
    base_path: Optional[Path] = typer.Option(None, "--base-path", "-b",
        help="Base path for the target (overrides saved default)"),
    domain: Optional[str] = typer.Option(None, "--domain", "-d",
        help="Domain name (for ai-resources target)"),
    force: bool = typer.Option(False, "--force", help="Overwrite existing skill (requires explicit user confirmation)"),
) -> None:
    """Scaffold a new skill at the resolved target path."""

    # Validate skill name
    if not skill_name.replace("-", "").replace("_", "").isalnum() or skill_name != skill_name.lower():
        typer.secho(
            f"Error: skill name must be lowercase kebab-case (got: {skill_name!r})",
            err=True, fg=typer.colors.RED,
        )
        raise typer.Exit(1)

    # Resolve base path: explicit > saved default > interactive > error
    resolved_base: Optional[Path] = base_path
    config = load_config()

    if resolved_base is None:
        saved = config.get("default_target")
        if saved and not Path(saved.get("path", "")).exists():
            saved["_stale"] = True

        if saved and not saved.get("_stale"):
            resolved_base = Path(saved["path"])
            if target_type == "ai-resources" and domain is None:
                domain = saved.get("domain")
                target_type = saved.get("type", target_type)
        elif _is_tty():
            # Interactive fallback: guide the user
            console.print("[yellow]No saved default — selecting target interactively.[/yellow]\n")
            detected = detect_targets(Path.cwd())
            selected = _interactive_resolve_target(detected, saved)
            if not selected:
                typer.secho("Aborted — no target selected.", err=True, fg=typer.colors.RED)
                raise typer.Exit(1)
            resolved_base = Path(selected["path"])
            target_type = selected["type"]
            if target_type == "ai-resources" and domain is None:
                domain = selected.get("domain") or _interactive_resolve_domain(resolved_base, None)
            if _inquire_confirm("Save as default for future skills?", default=True):
                config["default_target"] = {
                    "type": target_type,
                    "path": str(resolved_base),
                    **({"domain": domain} if domain else {}),
                }
                save_config(config)
                console.print(f"[green]✓ Default saved.[/green]")
        else:
            typer.secho(
                "Error: no --base-path provided and no saved default. "
                "Run 'detect --interactive' first or pass --base-path.",
                err=True, fg=typer.colors.RED,
            )
            raise typer.Exit(1)

    resolved_base = resolved_base.resolve()

    # Build final skill path depending on target type
    if target_type == "ai-resources":
        if not domain:
            typer.secho(
                "Error: --domain is required when target-type is ai-resources",
                err=True, fg=typer.colors.RED,
            )
            raise typer.Exit(1)
        skill_path = resolved_base / domain / "skills" / skill_name
    else:
        skill_path = resolved_base / skill_name

    # Guard: existing skill
    if skill_path.exists() and not force:
        if _is_tty():
            confirmed = _inquire_confirm(
                f"Skill '{skill_name}' already exists at {skill_path}. Overwrite?",
                default=False,
            )
            if not confirmed:
                console.print("[dim]Aborted.[/dim]")
                raise typer.Exit(0)
        else:
            typer.secho(
                f"Error: skill '{skill_name}' already exists at {skill_path}\n"
                "Use --force to overwrite (only after explicit user confirmation).",
                err=True, fg=typer.colors.RED,
            )
            raise typer.Exit(1)

    is_prog_expert = skill_name.startswith("prog-expert-")
    title = skill_name.replace("-", " ").replace("_", " ").title()

    # --- Create directories ---
    subdirs = ["scripts", "examples", "references", "assets", "resources"]
    if is_prog_expert:
        subdirs += ["atomic-examples", "recipes"]
    for sub in subdirs:
        (skill_path / sub).mkdir(parents=True, exist_ok=True)

    # --- SKILL.md ---
    if is_prog_expert:
        tech = skill_name.split("-")[-1].title()
        skill_md = f"""\
---
name: {skill_name}
description: "Use when you need to {description.lower()}. Expert-level programming and pattern management."
metadata:
  type: expert
  technology: {skill_name.split("-")[-1]}
  discovery_keywords: [{skill_name.split("-")[-1]}]
  stage: alpha
  requires: []
---

# {title}

## Goal
Expert-level implementation guidance for {tech}. Handles complex integrations, gathers knowledge from docs and web research, and maintains reusable patterns, recipes, and atomic examples.

## Core Workflow (Progressive Disclosure)
1. **Context Analysis**: Analyze the current codebase and project state.
2. **Knowledge Retrieval**:
   - Check `recipes/` for recurring patterns.
   - Fetch latest versions: `uv run scripts/track_versions.py`.
   - Research new patterns: `uv run scripts/research_knowledge.py "<query>"`.
3. **Implementation**: Execute core logic using `atomic-examples/` and `recipes/` for guidance.
4. **Learning**: Extract patterns via `uv run scripts/learn_from_success.py <file>`.

## Instructions
- Always check `recipes/` before starting any task.
- Create a new recipe after each successful implementation of a recurring pattern.
- Keep library version references up to date in `references/versions.json`.

## Self-Learning & Research
- Use `use-context7-api` skill for library documentation lookups.
- Refine recipes based on implementation experience.

## Auto-Improvement
After each use, analyze the chat for improvement opportunities. Ask the user. If approved, append to `resources/improvement_ideas.md`.

## Script Integration
- `uv run scripts/research_knowledge.py "<query>"`
- `uv run scripts/track_versions.py`
- `uv run scripts/manage_recipes.py <action>`
- `uv run scripts/learn_from_success.py <path>`
"""
    else:
        skill_md = f"""\
---
name: {skill_name}
description: "Use when you need to {description.lower()}."
keywords: []
metadata:
  stage: alpha
  requires: []
---

# {title}

## Goal
<!-- TODO: One clear statement of what this skill achieves. -->

## Core Workflow (Progressive Disclosure)
1. Determine the initial state.
2. Execute core logic. Delegate fragile steps to `scripts/execute.py`.
3. For complex data handling, refer to [References](references/README.md).

## Instructions
- Delegate repetitive/fragile tasks: `uv run scripts/execute.py <args>`
- Use assets from `assets/` when generating output templates.

## Auto-Improvement
After each use, analyze the chat for improvement opportunities. Ask the user. If approved, append to `resources/improvement_ideas.md`.

## Constraints
- Do not perform unauthorized or destructive actions.
- Do not overwrite existing files without explicit user confirmation.

## Script Integration
```bash
uv run scripts/execute.py
```
"""

    (skill_path / "SKILL.md").write_text(skill_md)

    # --- Scripts ---
    if is_prog_expert:
        _write_prog_expert_scripts(skill_path, skill_name)
        (skill_path / "references" / "patterns.md").write_text(
            "# Patterns\n\nReusable code patterns extracted from successful implementations.\n"
        )
        (skill_path / "references" / "versions.json").write_text("{}\n")
        (skill_path / "recipes" / "README.md").write_text(
            "# Recipes\n\nCoding recipes for recurring tasks.\n\n"
            "Each recipe: frontmatter (name, description, libraries_used) + markdown + code examples.\n"
        )
    else:
        execute_py = f"""\
# /// script
# requires-python = ">=3.12"
# dependencies = []
# ///

import json
import sys


def main() -> None:
    \"\"\"Main execution logic for {skill_name}. Prints JSON to stdout.\"\"\"
    try:
        result = {{"status": "success"}}
        print(json.dumps(result, indent=2))
    except Exception as exc:
        print(f"Error: {{exc}}", file=sys.stderr)
        sys.exit(1)


if __name__ == "__main__":
    main()
"""
        (skill_path / "scripts" / "execute.py").write_text(execute_py)
        (skill_path / "references" / "README.md").write_text(
            "# References\n\nLarge documentation, schemas, or domain knowledge goes here.\n"
        )

    # --- Shared files ---
    (skill_path / "assets" / "README.md").write_text(
        "# Assets\n\nOutput templates, icons, or boilerplate code.\n"
    )
    (skill_path / "examples" / "example.md").write_text(
        f"# Example: {title}\n\n## Trigger\n<!-- Describe what the user says to invoke this skill -->\n\n"
        "## Expected Output\n<!-- Describe the result -->\n"
    )
    (skill_path / ".gitignore").write_text(
        ".venv/\n__pycache__/\n*.pyc\n.pytest_cache/\n.mypy_cache/\n.DS_Store\n.env\n*.log\n"
    )

    # --- Print result ---
    console.print(f"\n[bold green]✓ Skill scaffolded:[/bold green] [cyan]{skill_name}[/cyan]")
    console.print(f"  Path: {skill_path}\n")

    tree = Tree(f"[bold]{skill_name}/[/bold]")
    tree.add("[yellow]SKILL.md[/yellow]")
    tree.add("scripts/")
    tree.add("examples/")
    tree.add("references/")
    tree.add("assets/")
    tree.add("resources/")
    if is_prog_expert:
        tree.add("atomic-examples/")
        tree.add("recipes/")
    console.print(tree)

    console.print("\n[bold]Next steps:[/bold]")
    console.print(f"  1. Fill in TODO sections in [cyan]{skill_path}/SKILL.md[/cyan]")
    console.print("  2. Implement [cyan]scripts/execute.py[/cyan] or delegate to [bold]uv-cli-implementer[/bold]")


def _write_prog_expert_scripts(skill_path: Path, skill_name: str) -> None:
    scripts = {
        "research_knowledge.py": """\
# /// script
# requires-python = ">=3.12"
# dependencies = ["typer>=0.12.0", "rich>=13.0.0"]
# ///
import typer
from rich.console import Console

app = typer.Typer(add_completion=False)
console = Console()

@app.command()
def main(query: str = typer.Argument(..., help="Research query")) -> None:
    \"\"\"Research knowledge via Context7 and web. Replace with actual integration.\"\"\"
    console.print(f"[cyan]Researching:[/cyan] {query}")
    console.print("[yellow]TODO: integrate use-context7-api and web research.[/yellow]")

if __name__ == "__main__":
    app()
""",
        "track_versions.py": """\
# /// script
# requires-python = ">=3.12"
# dependencies = ["typer>=0.12.0", "rich>=13.0.0"]
# ///
import typer
from rich.console import Console

app = typer.Typer(add_completion=False)
console = Console()

@app.command()
def main() -> None:
    \"\"\"Check and update library version references.\"\"\"
    console.print("[cyan]Checking versions...[/cyan]")
    console.print("[yellow]TODO: implement version tracking.[/yellow]")

if __name__ == "__main__":
    app()
""",
        "manage_recipes.py": """\
# /// script
# requires-python = ">=3.12"
# dependencies = ["typer>=0.12.0", "rich>=13.0.0"]
# ///
import typer
from rich.console import Console

app = typer.Typer(add_completion=False)
console = Console()

@app.command()
def main(action: str = typer.Argument(..., help="Action: list | create | refine")) -> None:
    \"\"\"Manage coding recipes for recurring tasks.\"\"\"
    console.print(f"[cyan]Recipes — {action}[/cyan]")
    console.print("[yellow]TODO: implement recipe management.[/yellow]")

if __name__ == "__main__":
    app()
""",
        "learn_from_success.py": """\
# /// script
# requires-python = ">=3.12"
# dependencies = ["typer>=0.12.0", "rich>=13.0.0"]
# ///
import typer
from rich.console import Console
from pathlib import Path

app = typer.Typer(add_completion=False)
console = Console()

@app.command()
def main(file: Path = typer.Argument(..., help="Path to a successful implementation file")) -> None:
    \"\"\"Extract patterns from a successful implementation.\"\"\"
    console.print(f"[cyan]Learning from:[/cyan] {file}")
    console.print("[yellow]TODO: implement pattern extraction.[/yellow]")

if __name__ == "__main__":
    app()
""",
    }
    for filename, content in scripts.items():
        (skill_path / "scripts" / filename).write_text(content)


if __name__ == "__main__":
    app()
