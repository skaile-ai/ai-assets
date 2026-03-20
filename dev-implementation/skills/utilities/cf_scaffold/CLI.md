# App Bootstrap CLI Usage

The `cf_implement_bootstrap` skill automates the creation of modern, production-ready codebases using extremely fast package managers. This document outlines the core CLI commands executed by the skill when scaffolding new projects.

## Core CLI Commands

### Node / Nuxt Projects
The skill mandates the use of `bun` for JavaScript and TypeScript projects.

- **Initialize a Nuxt Project:**
  ```bash
  bunx nuxi@latest init . --packageManager bun
  ```
  *Executes the official Nuxt bootstrapper, enforcing `bun` as the package manager.*

- **Install Dependencies:**
  ```bash
  bun install
  ```
  *Verifies the environment and ensures dependencies are cleanly installed after scaffolding.*

### Python Projects
The skill mandates the use of `uv` for Python projects (APIs, CLI tools, standard apps).

- **Initialize a Python Project:**
  ```bash
  uv init
  ```
  *Initializes a standard Python application.*

  ```bash
  uv init --app
  # or
  uv init --lib
  ```
  *Initializes specific project types based on the user's requirements.*

- **Synchronize Dependencies:**
  ```bash
  uv sync
  ```
  *Generates a pristine lockfile and ensures the `.venv` is properly set up with all required dependencies.*

- **Execute Python Scripts:**
  ```bash
  uv run <script_name>
  ```
  *Runs the application or scripts securely within the managed virtual environment.*

## Skill Symlinking Protocol

The bootstrapper also bridges the new codebase with global AI capabilities by symlinking agent skills into the local `.claude/skills/` directory.

- **Symlink a Skill:**
  ```bash
  mkdir -p .claude/skills/
  ln -s /home/matthias/workBench/SKILLS/.claude/skills/<skill-name> .claude/skills/<skill-name>
  ```

## Suggested Subcommands

Currently, the bootstrapper relies on the agent executing standard external CLI commands. In the future, a dedicated wrapper CLI (e.g., `pb` or `bootstrap-cli`) could provide these automated workflows in a single command. 

Suggested subcommands for a future custom CLI:

1. **`pb new nuxt <project-name>` / `pb new python <project-name>`**
   A unified command to scaffold the project skeleton, inject highly opinionated defaults from `resources/*_defaults.json`, and automatically run the package manager (`bun install` or `uv sync`).

2. **`pb link-skills [--auto]`**
   A dedicated subcommand to analyze the project type (by reading `package.json` or `pyproject.toml`) and automatically symlink the most relevant global skills (e.g., `cf_concept_mock` for Nuxt) into the local `.claude/skills/` directory.

3. **`pb study <target-file-or-dir>`**
   A command to trigger the "Recipe Generator" locally. It would parse the provided implementation, fetch the latest context/documentation (via Context7 or similar integration), and generate an optimized Markdown recipe in the `knowledge/` folder.
