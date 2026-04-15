# add-feature CLI

## Trigger

Invoke with: "add a feature", "add X to the concept", "I want a new feature for Y", "modify the login feature", "change the requirements for X", or any request to extend or change an existing concept.

## Output

- New or updated `_concept/2_experience/2_features/<NN_group>/<feature>.md`
- Cascades to any existing downstream artifacts:
  - `_concept/2_experience/1_journeys/stories.json`
  - `_concept/3_blueprint/1_techstack/stack.md`
  - `_concept/3_blueprint/2_architecture/architecture.md`
  - `_concept/3_blueprint/3_datamodel/model.json` + `model.dbml` + `seed.json` + `feature_map.json`
  - `_concept/2_experience/3_screens/<NN_group>/<screen>.md`

## Next Steps

After cascade is approved:
- `screens` — spec new screens if not already generated
- `implement-feature` — implement the feature if the app is already built
- `concept-orchestrator` — continue the full pipeline
