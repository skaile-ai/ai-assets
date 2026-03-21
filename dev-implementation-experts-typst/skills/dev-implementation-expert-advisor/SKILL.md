---
name: prog-expert-advisor
source: MIGRATED
description: Use when you need to analyze a user request and find the appropriate prog-expert-* skill from the programming-* directories (e.g. programming-js, programming-python), or when you need to store new implementation knowledge and need to know which expert should learn it.
keywords: []
reads_from: []
writes_to: []
metadata:
  stage: alpha
  requires: []
---

# Prog Expert Advisor

## Goal
Acts as the central dispatcher for all `prog-expert-*` skills. It analyzes the user's implementation task or new knowledge to learn, scans all available expert skills in the `programming-*` directories, and recommends the best fit using a term-matching ranking algorithm.

## Core Workflow (Progressive Disclosure)
1. **Skill Discovery & Advice**: 
   Run `uv run scripts/advise.py "user query"` to analyze the user's request against all available expert skills and get a recommendation on which expert to invoke or where to store knowledge.
2. **Central Learning Hub**:
   When new patterns or successful implementations are achieved, run `uv run scripts/learn.py <path/to/file> "context or description"` to find the best expert skill to store the knowledge and receive a detailed prompt on how to format and save the implementation details.
3. **Delegation**: 
   Once the appropriate skill is identified, tell the user the skill name and instruction so they can invoke that specific expert, or directly route the agent workload if requested.

## Instructions
- ALWAYS run `scripts/advise.py` when asked to find an expert for an implementation task.
- ALWAYS run `scripts/learn.py` when asked to store a recipe, pattern, or successful implementation.
- Do not attempt to implement the code yourself; your job is to route to the correct `prog-expert-*` skill.
- If no existing skill fits the implementation/knowledge request, suggest creating a *new* specific `prog-expert` skill using `skill-builder`. Ensure the new skill is placed in the correct `programming-{tech}` directory (e.g., `programming-js`, `programming-python`, `programming-typst`) based on the detected technology.

## How to Use an Expert Skill
When an agent is delegated to or decides to use a specific `prog-expert-*` skill, they MUST follow these steps to utilize its knowledge effectively (Progressive Disclosure):
1. **Explore the Skill Folder**: Use `list_dir` or `ls` on the target skill's directory (e.g. `programming-js/prog-expert-nuxt/`) to see what is available.
2. **Consult Recipes**: Recipes are complete, copy-pasteable implementations located in the `recipes/` folder. Always search here first for pre-built solutions for common tasks.
3. **Consult References**: The `references/` folder contains API documentation, best practices, and smaller code patterns. Read these to understand the "how-to" and constraints of the technology.
4. **Use `view_file`**: DO NOT hallucinate the content of these files. Always use the `view_file` tool to read the specific markdown recipe or reference you need before writing any code.
5. **Run Setup Scripts**: If the skill contains a `scripts/` directory (e.g., for scaffolding or initializing a project), run those scripts if the user needs to bootstrap a new application, rather than doing it manually.

## Auto-Improvement
- Every time this skill is used, analyze the usage chat to find out if further improvement of the skill is advised.
- Ask the user if those changes should be made.
- If approved, store the improvement ideas in `resources/improvement_ideas.md`.

## Script Integration
- **Advice/Routing**: `uv run scripts/advise.py "explain the user's requirement clearly"`
- **Learning/Pattern Extraction**: `uv run scripts/learn.py <path/to/success_file> "optional context"`
