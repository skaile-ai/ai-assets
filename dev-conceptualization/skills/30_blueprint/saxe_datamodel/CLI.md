# `concept-3-blueprint-3-datamodel` CLI & Triggers

## Natural Language Triggers

- **"Design data model"** / **"Analyze data schema"**
  - Reads features from `_concept/2_experience/2_features/`
  - Produces `postxl-schema.json` + `feature_map.json` + `seed.json` in `_concept/3_blueprint/3_datamodel/`

- **"Add model"** / **"Add a users table"**
  - Reads existing `postxl-schema.json`, adds model, writes back

- **"Export schema"** / **"Generate Prisma schema"**
  - Reads `postxl-schema.json` + `_concept/3_blueprint/1_techstack/stack.md`
  - Outputs Prisma schema or other stack-specific format

- **"Validate data model"**
  - Validates `postxl-schema.json` structure and types
  - Checks cross-references to features via `feature_map.json`
  - Reports missing or orphaned models

## Workflow

1. **Concept ingestion** — scan `_concept/2_experience/2_features/` for data requirements
2. **Model design** — identify models, fields, relationships, enums
3. **PostXL schema output** — write `postxl-schema.json` with PostXL types
4. **Seed data** — write realistic `seed.json` for mockups
5. **Feature map** — write `feature_map.json` linking models to features
6. **Feedback loop** — update `data_entities` in feature frontmatter
7. **Stack translation** — on request only, output to stack-specific format

## Data Format

The data model uses **PostXL Prisma-based types** directly.
See `shared/contracts/semantic_types.md` for the complete type reference.
