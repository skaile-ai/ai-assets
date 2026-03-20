# Feedback Loop — Cross-Reference Protocol

When a downstream skill creates or modifies an artifact that relates to an
upstream artifact, it must register the connection in both directions.

## The Rule

**Every link must exist in both files.** If screen X implements feature Y,
then feature Y must list screen X, and screen X must list feature Y.

## Registration Protocol

### When `cf_concept_ui_screens` creates a screen:

1. Write the screen file with `implements:` listing the feature paths
2. For each referenced feature, read its frontmatter
3. Append the screen to the feature's `screens:` array
4. Update `last_updated` on both files

```yaml
# 07_screens/01_user_auth/login.md
---
implements:
  - 03_features/01_user_auth/login.md
  - 03_features/01_user_auth/registration.md
---
```

```yaml
# 03_features/01_user_auth/login.md — updated by screens skill
screens:
  - path: 07_screens/01_user_auth/login.md
```

### When `cf_concept_datamodel` creates the model:

1. Write `model.json` with `from_features` on each entity
2. For each referenced feature, read its frontmatter
3. Set `data_entities:` to the list of entity IDs that serve this feature
4. Update `last_updated` on the feature file

```yaml
# 03_features/01_user_auth/login.md — updated by datamodel skill
data_entities: [user, session]
```

### When a screen is deleted or renamed:

1. Find all features that reference the old screen path
2. Remove or update the entry in their `screens:` array
3. Update `last_updated`

### When a feature is deleted:

1. Find all screens that reference the feature in `implements:`
2. Remove the entry (or flag for human review if the screen depends on it)
3. Find all entities in `model.json` that reference the feature in `from_features`
4. Remove the reference

### When `cf_concept_functionality_behaviors` creates behavioral specs:

1. Write `.allium` files to `_concept/03b_behavior/`
2. Each file corresponds to a feature group (e.g., `user_auth.allium` ↔ `01_user_auth/`)
3. No frontmatter updates needed — downstream skills discover allium files by checking the folder

### When `cf_concept_datamodel` reads behavioral specs (optional):

1. Check if `_concept/03b_behavior/*.allium` exists
2. If present, use entity definitions to pre-populate the data model:
   - Entity status enums → model enums
   - Entity relationships → model relationships
   - Config values → inform field defaults and constraints
3. This is additive — the agent still reads features and applies its own analysis

### When `cf_concept_ui_screens` reads behavioral specs (optional):

1. Check if `_concept/03b_behavior/*.allium` exists
2. If present, use surfaces to inform screen specs:
   - Surface `exposes` → screen data requirements
   - Surface `provides` → screen user actions
   - Surface `when` guards → state-dependent UI elements

## Validation

The `cf_quality_review` skill checks for broken cross-references:
- Screen references a feature that doesn't exist → ERROR
- Feature lists a screen that doesn't exist → ERROR
- Entity references a feature that doesn't exist → WARNING
- Feature has `screens: []` but matching screens exist → WARNING (missing link)

## Event Emission

When modifying an upstream file, emit a structured event:

```
[feedback_loop] updated 03_features/01_user_auth/login.md
  added screen: 07_screens/01_user_auth/login.md
  source_skill: cf_concept_ui_screens
```

See `docs/OBSERVABILITY.md` for full event format.
