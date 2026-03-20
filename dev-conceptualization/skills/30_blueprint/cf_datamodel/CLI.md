# `cf_concept_datamodel` CLI & Triggers

## Natural Language Triggers

- **"Design data model"** / **"Analyze data schema"**
  - Reads features from `_concept/03_features/`
  - Produces `model.dbml` + `model.json` + `seed.json` in `_concept/06_datamodel/`

- **"Add entity"** / **"Add a users table"**
  - Reads existing `model.json`, adds entity, writes back
  - Updates `model.dbml` to match

- **"Generate Directus schema"** / **"Export to Prisma"**
  - Reads `model.json` + `_concept/05_techstack/stack.md`
  - Uses semantic type translation table from `cf__shared/semantic_types.md`
  - Outputs stack-specific format as a separate file

- **"Validate data model"**
  - Checks `model.json` against TypeBox schema
  - Checks cross-references to features
  - Reports missing or orphaned entities

## Workflow

1. **Concept ingestion** — scan `_concept/03_features/` for data requirements
2. **Entity design** — identify entities, fields, relationships, enums
3. **DBML output** — write human-readable `model.dbml`
4. **JSON output** — write editor-native `model.json` with semantic types
5. **Seed data** — write realistic `seed.json` for mockups
6. **Feedback loop** — update `data_entities` in feature frontmatter
7. **Stack translation** — on request only, output to stack-specific format

## Data Format

The data model uses **semantic types** (not SQL types, not Directus types).
See `cf__shared/semantic_types.md` for the complete type reference and
stack translation table.

The TypeBox schema defining `model.json` lives at:
`concept-forge/shared/schemas/datamodel.ts`
