# /// script
# requires-python = ">=3.11"
# dependencies = ["rich"]
# ///

import os
import sys
from rich.console import Console

console = Console()
RECIPES_DIR = os.path.join(os.path.dirname(__file__), "../recipes")

def list_recipes():
    files = [f for f in os.listdir(RECIPES_DIR) if f.endswith(".md") and f != "README.md"]
    console.print("[bold cyan]Available recipes:[/bold cyan]")
    for f in files:
        console.print(f"  - {f[:-3]}")

def create_recipe(name: str, description: str):
    path = os.path.join(RECIPES_DIR, f"{name}.md")
    if os.path.exists(path):
        console.print(f"[red]Recipe '{name}' already exists.[/red]")
        sys.exit(1)
    content = f"""---
name: {name}
description: {description}
libraries_used: []
---

# {name.replace('-', ' ').title()}

## When to Use

## Steps

## Key Details
"""
    with open(path, "w") as f:
        f.write(content)
    console.print(f"[green]Created recipe: {path}[/green]")
    console.print("[yellow]Add an entry to recipes/README.md index.[/yellow]")

def main():
    if len(sys.argv) < 2:
        console.print("Usage: uv run manage_recipes.py <list|create> [name] [description]")
        sys.exit(1)
    action = sys.argv[1]
    if action == "list":
        list_recipes()
    elif action == "create":
        if len(sys.argv) < 4:
            console.print("[red]Usage: uv run manage_recipes.py create <name> <description>[/red]")
            sys.exit(1)
        create_recipe(sys.argv[2], sys.argv[3])
    else:
        console.print(f"[red]Unknown action: {action}[/red]")

if __name__ == "__main__":
    main()
