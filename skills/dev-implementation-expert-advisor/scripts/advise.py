# /// script
# requires-python = ">=3.11"
# dependencies = [
#     "typer",
#     "rich",
#     "pyyaml"
# ]
# ///

import os
import re
import typer
from rich.console import Console
from rich.table import Table
import yaml

app = typer.Typer(help="Prog Expert Advisor: Recommends the best prog-expert skill for a given task.")
console = Console()

def get_root_dir() -> str:
    # This script is in programming/prog-expert-advisor/scripts/
    script_dir = os.path.dirname(os.path.abspath(__file__))
    return os.path.dirname(os.path.dirname(os.path.dirname(script_dir)))

def load_skills():
    root_dir = get_root_dir()
    skills = []
    
    if not os.path.exists(root_dir):
        return skills
        
    for item in os.listdir(root_dir):
        tech_dir = os.path.join(root_dir, item)
        if os.path.isdir(tech_dir) and item.startswith("programming"):
            # Extract the tech part, e.g., 'js' from 'programming-js'
            tech = item.replace("programming-", "")
            if item == "programming":
                tech = "general" # Fallback for just 'programming'
                
            for skill_item in os.listdir(tech_dir):
                skill_dir = os.path.join(tech_dir, skill_item)
                if os.path.isdir(skill_dir) and skill_item.startswith("prog-expert-"):
                    skill_md_path = os.path.join(skill_dir, "SKILL.md")
                    if os.path.exists(skill_md_path):
                        with open(skill_md_path, "r", encoding="utf-8") as f:
                            content = f.read()
                            
                        # Extract yaml frontmatter
                        match = re.match(r'^---\s*\n(.*?)\n---\s*\n', content, re.DOTALL)
                        description = "No description found."
                        name = skill_item
                        if match:
                            try:
                                frontmatter = yaml.safe_load(match.group(1))
                                if isinstance(frontmatter, dict):
                                    description = frontmatter.get("description", description)
                                    name = frontmatter.get("name", name)
                            except Exception:
                                pass
                        skills.append({"name": name, "description": description, "tech": tech})
    return skills

def guess_tech(queryWords: set) -> str:
    if "python" in queryWords or "py" in queryWords or "fastapi" in queryWords or "pandas" in queryWords:
        return "python"
    if "js" in queryWords or "javascript" in queryWords or "ts" in queryWords or "typescript" in queryWords or "vue" in queryWords or "nuxt" in queryWords or "node" in queryWords:
        return "js"
    if "typst" in queryWords or "latex" in queryWords:
        return "typst"
    return "general"

@app.command()
def advise(query: str = typer.Argument(..., help="The task to implement or the knowledge to store")):
    """
    Analyzes the query and recommends the best prog-expert skill to handle it.
    """
    skills = load_skills()
    if not skills:
        console.print("[red]No prog-expert skills found in the directory.[/red]")
        raise typer.Exit(1)
        
    # We use simple keyword matching
    query_words = set(re.findall(r'\w+', query.lower()))
    detected_tech = guess_tech(query_words)
    
    best_match = None
    best_score = -1
    
    results = []
    for skill in skills:
        desc_words = set(re.findall(r'\w+', skill['description'].lower()))
        # simple score: number of overlapping words
        score = len(query_words.intersection(desc_words))
        
        # Boost score if skill name is in the query (e.g. "nuxt", "tiptap")
        skill_name_parts = set(skill['name'].replace('prog-expert-', '').split('-'))
        if query_words.intersection(skill_name_parts):
            score += 5
            
        results.append((score, skill))
        
        if score > best_score:
            best_score = score
            best_match = skill
            
    # Create a table of available skills for the agent to read
    table = Table(title="Available Prog-Expert Skills", show_lines=True)
    table.add_column("Skill Name", style="cyan")
    table.add_column("Tech", style="magenta")
    table.add_column("Description", style="white")
    table.add_column("Match Score", justify="right", style="green")
    
    results.sort(key=lambda x: x[0], reverse=True)
    
    for score, skill in results:
        desc = skill['description']
        if len(desc) > 60:
            desc = desc[:57] + "..."
        # ensure we pass 4 strings to match the 4 columns
        table.add_row(str(skill['name']), str(skill['tech']), str(desc), str(score))
        
    console.print(table)
    
    console.print(f"\n[bold blue]Detected Technology:[/bold blue] {detected_tech}")
    
    console.print("\n[bold yellow]Analysis:[/bold yellow]")
    if best_score > 0:
        console.print(f"The recommended skill is [bold green]{best_match['name']}[/bold green] in `programming-{best_match['tech']}` (Score: {best_score}).")
        console.print(f"Description: {best_match['description']}")
    else:
        console.print("[yellow]Could not find a strong match based on simple keyword analysis.[/yellow]")
        console.print("Please review the table above manually to pick the best expert.")
        
        suggested_folder = "programming" if detected_tech == "general" else f"programming-{detected_tech}"
        console.print(f"If no expert fits the requirement, suggest creating a new one using the [bold]skill-builder[/bold], inside the [bold blue]{suggested_folder}[/bold blue] directory.")

if __name__ == "__main__":
    app()
