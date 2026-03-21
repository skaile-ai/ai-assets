# Pipeline Step Definition

A single step in pipeline.json:

```json
{
  "id": "cf_datamodel",
  "name": "Data Model",
  "skill": "cf_datamodel",
  "phase": "conceptualization",
  "sub_phase": "datamodel",
  "folder": "06_datamodel/",
  "depends_on": ["cf_functionality_features", "cf_techstack", "cf_architecture"],
  "optional_reads": ["_grounding/", "_standards/"],
  "optional": false,
  "parallel_group": null,
  "subagent": true,
  "hard_gates": [
    { "type": "file_exists", "path": "03_features/" },
    { "type": "file_exists", "path": "05_techstack/stack.md" }
  ],
  "user_inputs": {
    "dialog": [],
    "files": ["03_features/"]
  },
  "description": "Generates data model from feature specs — DBML + JSON + seed data",
  "outputs": ["model.dbml", "model.json", "seed.json"]
}
```

Key fields:
- `hard_gates`: must ALL pass before skill can run (file existence only)
- `depends_on`: step IDs that must be complete/approved first
- `parallel_group`: steps with same value run concurrently
- `subagent`: true = dispatch in fresh context (prevents token bloat)
- `outputs`: expected files — used to distinguish `in_progress` from `complete`
