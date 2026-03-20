# cf_orchestrator CLI

## Trigger

Invoke with: "Conceptualize an app" or "Start a new concept" or `/concept [description]`

## Modes

- **Default:** pauses for human approval at each checkpoint
- **Auto-review:** (`--auto-review`) runs lint + gardening between phases, auto-approves when score >= 70

## Workflow

```
Phase 0: Initialize PLANS.md (or resume from existing)
Phase 1: Project brief         → _concept/01_project/     (cf_concept_overview)
Phase 2: Research (optional)   → _concept/_research/      (cf_research)
Phase 3: Features              → _concept/03_features/    (cf_concept_functionality_features)
Phase 4: Parallel tracks:
  4a: Brand                    → _concept/04_brand/        (cf_concept_brand_visual)
  4b: Tech stack               → _concept/05_techstack/   (cf_concept_techstack)
  4c: Data model               → _concept/06_datamodel/   (cf_concept_datamodel)
Phase 5: Screens               → _concept/07_screens/     (cf_concept_ui_screens)
Phase 6: Generate implementation plan in PLANS.md
```

## Output

- `PLANS.md` — concept progress + implementation plan
- `_concept/.snapshots/` — approval snapshots at each checkpoint
- All `_concept/01_project/` through `_concept/07_screens/` artifacts

## Next Steps

After concept complete → `cf_concept_mock`, `cf_implement_bootstrap`, `cf_quality_audit`, `cf_test_e2e`
