# Skill Self-Testing Convention

Every skill should have testable examples — known input/output pairs that
validate the skill produces correct artifacts.

## Directory Structure

```
<skill-directory>/
├── SKILL.md
├── CLI.md
├── examples/
│   ├── README.md             ← describes each fixture
│   ├── input/                ← simulated _concept/ state before skill runs
│   │   ├── 1_discovery/1_overview/
│   │   │   └── brief.md
│   │   ├── 2_experience/2_features/
│   │   │   └── ...
│   │   └── ...
│   └── expected/             ← what the skill should produce
│       ├── 2_experience/2_features/   ← (or whichever path the skill writes)
│       │   └── ...
│       └── _validation.json  ← machine-checkable assertions
└── ...
```

---

## _validation.json Format

Each fixture includes a `_validation.json` that defines what to check.
The `skill` field uses the canonical kebab-case skill name.

```json
{
  "skill": "features",
  "description": "Given a brief about a task management app, produces 3 feature groups",
  "checks": [
    {
      "type": "file_exists",
      "path": "2_experience/2_features/01_user_auth/login.md"
    },
    {
      "type": "file_exists",
      "path": "2_experience/2_features/01_user_auth/registration.md"
    },
    {
      "type": "file_exists",
      "path": "2_experience/2_features/02_dashboard/overview.md"
    },
    {
      "type": "frontmatter_field",
      "path": "2_experience/2_features/01_user_auth/login.md",
      "field": "priority",
      "one_of": ["must-have", "nice-to-have"]
    },
    {
      "type": "frontmatter_field",
      "path": "2_experience/2_features/01_user_auth/login.md",
      "field": "screens",
      "expected": []
    },
    {
      "type": "frontmatter_field",
      "path": "2_experience/2_features/01_user_auth/login.md",
      "field": "data_entities",
      "expected": []
    },
    {
      "type": "section_exists",
      "path": "2_experience/2_features/01_user_auth/login.md",
      "heading": "## Description"
    },
    {
      "type": "section_exists",
      "path": "2_experience/2_features/01_user_auth/login.md",
      "heading": "## Requirements"
    },
    {
      "type": "contains_checkbox",
      "path": "2_experience/2_features/01_user_auth/login.md",
      "min_count": 1
    },
    {
      "type": "folder_numbered",
      "path": "2_experience/2_features/",
      "pattern": "^\\d{2}_"
    },
    {
      "type": "json_valid",
      "path": "3_blueprint/3_datamodel/model.json"
    },
    {
      "type": "json_field",
      "path": "3_blueprint/3_datamodel/model.json",
      "json_path": "entities",
      "min_length": 1
    },
    {
      "type": "json_field",
      "path": "3_blueprint/3_datamodel/seed.json",
      "json_path": "scenarios.empty",
      "exists": true
    },
    {
      "type": "json_field",
      "path": "3_blueprint/3_datamodel/seed.json",
      "json_path": "scenarios.single_user",
      "exists": true
    },
    {
      "type": "json_field",
      "path": "3_blueprint/3_datamodel/seed.json",
      "json_path": "scenarios.populated",
      "exists": true
    },
    {
      "type": "json_field",
      "path": "3_blueprint/3_datamodel/seed.json",
      "json_path": "scenarios.edge_cases",
      "exists": true
    }
  ]
}
```

---

## Check Types

| Type | What it validates | Parameters |
|---|---|---|
| `file_exists` | File was created | `path` |
| `file_not_exists` | File was NOT created | `path` |
| `frontmatter_field` | YAML field has expected value | `path`, `field`, `expected` or `one_of` |
| `frontmatter_present` | YAML frontmatter exists at all | `path` |
| `section_exists` | Markdown heading exists in body | `path`, `heading` |
| `contains_checkbox` | File has `- [ ]` checkboxes | `path`, `min_count` |
| `folder_numbered` | All subfolders match pattern | `path`, `pattern` |
| `json_valid` | File is parseable JSON | `path` |
| `json_field` | JSON path has expected value | `path`, `json_path`, `expected` or `exists` or `min_length` |
| `cross_reference` | Frontmatter path resolves | `path`, `field` — checks each entry in array resolves to a file |

---

## How to Run

A validation script reads `_validation.json` and checks each assertion
against the `expected/` folder:

```bash
# conceptual — could be a script or agent task
for check in _validation.json.checks:
    verify(check, base_path="examples/expected/")
```

Skills can also be tested by an agent:
1. Copy `examples/input/` to a temp `_concept/` folder
2. Run the skill
3. Compare output against `examples/expected/`
4. Run assertions from `_validation.json`

---

## What to Cover

Each skill should have at least one example fixture covering:

| Skill | Fixture should demonstrate |
|---|---|
| `overview` | Brief from user answers → files in `1_discovery/1_overview/` |
| `features` | Brief → numbered feature groups with valid frontmatter |
| `techstack` | Brief + features → `stack.md` with stack fields in frontmatter |
| `datamodel` | Features + stack → `model.json`, `seed.json` (4 scenarios), `feature_map.json` |
| `screens` | All inputs → screens with `implements[]`, feedback written to features |
| `review` | Mixed-state `_concept/` → health report with correct score |
