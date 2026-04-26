# datamodel CLI

## Trigger

Invoke with: "data model", "database schema", "what entities do we need", "what tables", or after features are approved.

## Output

- `_concept/blueprint/datamodel/model.dbml`
- `_concept/blueprint/datamodel/model.json`
- `_concept/blueprint/datamodel/seed.json`
- `_concept/blueprint/datamodel/feature_map.json`
- Updates `_concept/experience/features/**/*.md` `data_entities[]` (feedback loop)

## Next Steps

After human approval:
- `screens` — spec UI screens (now entities are defined)
- `concept-orchestrator` — continue the full pipeline
