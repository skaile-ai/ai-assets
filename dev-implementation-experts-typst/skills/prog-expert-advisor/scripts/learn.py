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
from rich.markdown import Markdown
import yaml

app = typer.Typer(help="Prog Expert Advisor: Central Learning Hub to extract and store patterns.")
console = Console()

def get_root_dir() -> str:
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
            tech = item.replace("programming-", "")
            if item == "programming":
                tech = "general"
                
            for skill_item in os.listdir(tech_dir):
                skill_dir = os.path.join(tech_dir, skill_item)
                if os.path.isdir(skill_dir) and skill_item.startswith("prog-expert-"):
                    skill_md_path = os.path.join(skill_dir, "SKILL.md")
                    if os.path.exists(skill_md_path):
                        with open(skill_md_path, "r", encoding="utf-8") as f:
                            content = f.read()
                            
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
                        skills.append({"name": name, "description": description, "tech": tech, "dir": skill_dir})
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
def learn(
    file_path: str = typer.Argument(..., help="Path to the file to extract knowledge from"),
    context: str = typer.Argument(default="", help="Context or description of what was implemented or why it is important")
):
    """
    Analyzes the file and context to determine the best prog-expert skill to store the knowledge,
    and provides instructions on how to structure the learning.
    """
    if not os.path.exists(file_path):
        console.print(f"[red]Error: File {file_path} not found.[/red]")
        raise typer.Exit(1)
        
    skills = load_skills()
    if not skills:
        console.print("[red]No prog-expert skills found in the directory.[/red]")
        raise typer.Exit(1)
        
    query_text = file_path.lower() + " " + context.lower()
    query_words = set(re.findall(r'\w+', query_text))
    
    best_match = None
    best_score = -1
    
    for skill in skills:
        desc_words = set(re.findall(r'\w+', skill['description'].lower()))
        score = len(query_words.intersection(desc_words))
        
        skill_name_parts = set(skill['name'].replace('prog-expert-', '').split('-'))
        if query_words.intersection(skill_name_parts):
            score += 5
            
        if score > best_score:
            best_score = score
            best_match = skill
            
    if best_score <= 0:
        # Default fallback to an appropriate tech directory
        detected_tech = guess_tech(query_words)
        suggested_folder = "programming" if detected_tech == "general" else f"programming-{detected_tech}"
        console.print(f"[yellow]Could not determine a specific expert skill. Suggested directory: {suggested_folder}[/yellow]")
        console.print("Please review the available skills or create a new one.")
        raise typer.Exit(0)

    target_dir = best_match['dir']
    skill_name = best_match['name']

    console.print(f"\n[bold green]✅ Target Skill Identified: {skill_name}[/bold green]\n")
    
    prompt = f"""
## Instructions for the AI Agent

You have been tasked to learn patterns from **{file_path}**.
Context provided: "{context}"

**Target Expert Skill:** `{skill_name}`
**Target Directory:** `{target_dir}`

Please perform the following steps carefully:

1. **Analyze the Source File**: Review the code in `{file_path}`. Identify the core problem it solves, the exact dependencies it requires, and any novel patterns or elegant solutions it implements.
2. **Determine the Storage Strategy**:
   - If this is a small reusable pattern or snippet, append it to `{target_dir}/references/patterns.md` (create it if it doesn't exist).
   - If this is a larger, complete implementation (like a full component or architecture), create a new markdown file inside `{target_dir}/recipes/` (e.g. `{target_dir}/recipes/my_new_pattern.md`).
3. **Draft the Documentation**: The documentation must include:
   - **Problem Statement:** What does this code solve?
   - **Dependencies:** What libraries/packages are required?
   - **Implementation:** The actual code snippet or pattern.
   - **Gotchas / Edge Cases:** Any things to look out for.
4. **Execute**: Write the documentation to the decided location using the `write_to_file` or `replace_file_content` tools. Maintain the progressive disclosure principle by not overwhelming existing documentation.
"""
    
    md = Markdown(prompt)
    console.print(md)


if __name__ == "__main__":
    app()
