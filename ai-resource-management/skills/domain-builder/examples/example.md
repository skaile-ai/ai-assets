# Example: Creating a `data-pipeline` Domain

## User Input

> "I want a new domain for ETL and data pipeline skills — things like ingesting CSVs, transforming data, and loading to databases."

## Skill Collects

- **Domain name:** `data-pipeline`
- **Description:** "Skills for building and running ETL pipelines: ingest, transform, validate, and load structured data."
- **Building blocks (inferred):**
  - contracts: "Shared file structure for pipeline artifacts and data schemas"
  - skills: "Ingest, transform, validate, load skills"
  - agents: "Pipeline orchestrator agent"
  - prompts: "Data schema description prompts"
  - tools: "CSV/JSON CLI tools for schema inspection and validation"

## Script Invocation

```bash
uv run scripts/scaffold_domain.py data-pipeline \
  "Skills for building and running ETL pipelines: ingest, transform, validate, and load structured data." \
  --base-path ./ai-resources
```

## Result

```
data-pipeline/
├── DOMAIN.md
├── contracts/
│   └── data-pipeline-contract/
│       └── SKILL.md
├── skills/
│   └── README.md
├── agents/
│   └── README.md
├── prompts/
│   └── README.md
└── tools/
    └── README.md
```

## Next Steps Shown to User

1. Fill in TODO sections in `data-pipeline/DOMAIN.md`
2. Fill in TODO sections in `contracts/data-pipeline-contract/CONTRACT.md`
3. Use `skill-builder` to scaffold the first skill in `data-pipeline/skills/`
