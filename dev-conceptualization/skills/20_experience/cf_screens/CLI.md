# cf_concept_ui_screens CLI

## Trigger

Invoke with: "Specify screens" or "Plan the screens for my app"

## Prerequisites

- `_concept/03_features/**/*.md` must exist (run `cf_concept_functionality_features` first)
- `_concept/04_brand/tokens.json` recommended (run `cf_concept_brand_visual`)
- `_concept/05_techstack/stack.md` recommended (run `cf_concept_techstack`)
- `_concept/06_datamodel/model.json` recommended (run `cf_concept_datamodel`)

## Output

- `_concept/07_screens/00_layout/shell.md` — app structure from user's perspective
- `_concept/07_screens/<NN_group>/<screen>.md` — one per screen (purpose, information, actions, situations, entities)
- Updates `screens:` in feature frontmatter (feedback loop)

## Next Step

After human approval → run `cf_concept_mock` for mockups
