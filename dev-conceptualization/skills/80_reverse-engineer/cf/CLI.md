# cf_concept_reverse_engineer CLI

## Trigger

Invoke with: "reverse engineer this project", "generate concept from existing code",
"I have a codebase, extract the concept", "document this existing app", "build concept from repo"

## Required Input

- `repo_path` — absolute or relative path to the existing project repository

## Output

- `_concept/01_project/brief.md`, `goals.md`, `comparable.md`
- `_concept/05_techstack/stack.md`
- `_concept/03_features/<NN_group>/<feature>.md` (one per discovered feature)
- `_concept/06_datamodel/model.dbml`, `model.json`, `seed.json`
- `_concept/04_brand/identity.md`, `tokens.json`
- `_concept/07_screens/<NN_group>/<screen>.md` (one per discovered route/page)

## Confidence Levels

Every artifact is tagged: `extracted` (from code) | `inferred` (from context) | `needs_review` (unclear)

## Next Steps

After approval:
- `cf_quality_review` — audit extraction quality and find gaps
- `cf_concept_functionality_features` — refine and approve extracted features
- `cf_concept_datamodel` — refine and approve extracted data model
- `cf_concept_brand_visual` — refine brand tokens with user intent
