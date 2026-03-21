# /// script
# requires-python = ">=3.11"
# dependencies = ["rich", "pyyaml"]
# ///

import os
import sys
import yaml
from rich.console import Console

console = Console()

def list_recipes():
    """Lists all recipes in the recipes/ directory."""
    recipes_dir = os.path.join(os.path.dirname(__file__), "..", "recipes")
    if not os.path.exists(recipes_dir):
        console.print("[yellow]Recipes directory not found.[/yellow]")
        return
    
    files = [f for f in os.listdir(recipes_dir) if f.endswith(".md")]
    if not files:
        console.print("[yellow]No recipes found.[/yellow]")
        return
    
    console.print("[bold green]Available Recipes:[/bold green]")
    for f in files:
        console.print(f"- {f}")

def create_recipe(name, description, libraries):
    """Creates a new recipe boilerplate with frontmatter."""
    filename = name.lower().replace(" ", "-")
    if not filename.endswith(".md"):
        filename += ".md"
    
    recipes_dir = os.path.join(os.path.dirname(__file__), "..", "recipes")
    os.makedirs(recipes_dir, exist_ok=True)
    
    target_path = os.path.join(recipes_dir, filename)
    
    recipe_content = f"""---
name: {name}
description: {description}
libraries_used: {libraries}
---

# {name}

## Objective
{description}

## Prerequisites
- Nuxt 3 project
- {libraries}

## Instructions
1. ...

## Code Example
```vue
<!-- Example -->
```
"""
    with open(target_path, "w") as f:
        f.write(recipe_content)
    
    console.print(f"[green]Scaffolded recipe: {name} at {target_path}[/green]")

def main():
    if len(sys.argv) < 2:
        console.print("[bold red]Usage: uv run manage_recipes.py <action> [args][/bold red]")
        console.print("Actions: list, create <name> <description> <libraries>")
        sys.exit(1)
    
    action = sys.argv[1]
    
    if action == "list":
        list_recipes()
    elif action == "create":
        if len(sys.argv) < 5:
            console.print("[red]Missing arguments for create. Usage: create <name> <description> <libraries>[/red]")
            sys.exit(1)
        create_recipe(sys.argv[2], sys.argv[3], sys.argv[4])
    else:
        console.print(f"[red]Unknown action: {action}[/red]")

if __name__ == "__main__":
    main()
