# add-feature CLI

## Trigger

Invoke with: "add a feature", "add X to the concept", "I want a new feature for Y", "modify the login feature", "change the requirements for X", or any request to extend or change an existing concept.

## Output

- New or updated `_concept/experience/features/<NN_group>/<feature>.md`
- Cascades to any existing downstream artifacts:
  - `_concept/experience/journeys/stories.json`
  - `_concept/blueprint/techstack.md`
  - `_concept/blueprint/architecture.md`
  - `_concept/blueprint/datamodel/model.json` + `model.dbml` + `seed.json` + `feature_map.json`
  - `_concept/experience/screens/<NN_group>/<screen>.md`

## Next Steps

After cascade is approved:
- `screens` — spec new screens if not already generated
- `implement-feature` — implement the feature if the app is already built
- `concept-orchestrator` — continue the full pipeline
