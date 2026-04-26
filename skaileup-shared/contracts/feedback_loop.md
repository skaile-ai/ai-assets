# Feedback Loop — Cross-Reference Protocol

When a downstream skill creates or modifies an artifact that relates to an
upstream artifact, it must register the connection in both directions.

## The Rule

**Every link must exist in both files.** If screen X implements feature Y,
then feature Y must list screen X, and screen X must list feature Y.

## Registration Protocol

### When the journeys skill creates stories:

1. Write `stories.json` to `_concept/experience/journeys/`
2. Each journey includes `candidate_features` (slugs) and `candidate_entities` (PascalCase names)
3. These are consumed by the features skill to seed its feature list
4. No frontmatter updates needed — features discover journeys by reading the JSON file directly

```json
{
  "journeys": [
    {
      "id": "onboarding_flow",
      "persona": "new_user",
      "candidate_features": ["registration", "email_verification", "profile_setup"],
      "candidate_entities": ["User", "Profile"]
    }
  ]
}
```

### When the features skill reads journeys:

1. Read `_concept/experience/journeys/stories.json`
2. Use `candidate_features` from each journey to seed the feature list
3. For each created feature, set `story_refs` in frontmatter to the journey IDs that motivated it
4. Features may be added, merged, or split beyond what journeys suggest — `story_refs` traces the origin

```yaml
# experience/features/01_user_auth/registration.md
---
story_refs: [onboarding_flow, invite_flow]
---
```

### When the screens skill creates a screen:

1. Write the screen file with `implements:` listing the feature paths
2. For each referenced feature, read its frontmatter
3. Append the screen path to the feature's `screens:` array
4. Update `last_updated` on both files

```yaml
# experience/screens/01_user_auth/login.md
---
implements:
  - experience/features/01_user_auth/login.md
  - experience/features/01_user_auth/registration.md
---
```

```yaml
# experience/features/01_user_auth/login.md — updated by screens skill
screens:
  - path: experience/screens/01_user_auth/login.md
```

### When the datamodel skill creates the model:

1. Write the schema file (format determined by stack)
2. Write `feature_map.json` mapping each model to the features it serves
3. For each referenced feature, read its frontmatter
4. Set `data_entities:` to the list of model names (PascalCase) that serve this feature
5. Update `last_updated` on the feature file

```yaml
# experience/features/01_user_auth/login.md — updated by datamodel skill
data_entities: [User, Session]
```

### When the behaviors skill creates behavioral specs (optional):

1. Write `.allium` files to `_concept/experience/behaviors/`
2. Each file corresponds to a feature group (e.g. `user_auth.allium` ↔ `01_user_auth/`)
3. No frontmatter updates needed — downstream skills discover allium files by checking the folder

### When the datamodel skill reads behavioral specs (optional):

1. Check if `_concept/experience/behaviors/*.allium` exists
2. If present, use entity definitions to pre-populate the data model:
   - Entity status enums → model enums
   - Entity relationships → model relationships
   - Config values → inform field defaults and constraints
3. This is additive — the agent still reads features and applies its own analysis

### When the screens skill reads behavioral specs (optional):

1. Check if `_concept/experience/behaviors/*.allium` exists
2. If present, use surfaces to inform screen specs:
   - Surface `exposes` → screen data requirements
   - Surface `provides` → screen user actions
   - Surface `when` guards → state-dependent UI elements

### When a screen is deleted or renamed:

1. Find all features that reference the old screen path
2. Remove or update the entry in their `screens:` array
3. Update `last_updated`

### When a feature is deleted:

1. Find all screens that reference the feature in `implements:`
2. Remove the entry (or flag for human review if the screen depends on it)
3. Find all models in `feature_map.json` that reference the feature
4. Remove the reference

## Validation

The quality review skill checks for broken cross-references:

- Screen references a feature that doesn't exist → ERROR
- Feature lists a screen that doesn't exist → ERROR
- Model in `feature_map.json` references a feature that doesn't exist → WARNING
- Feature has `screens: []` but matching screens exist → WARNING (missing link)
- Feature has `story_refs` pointing to a journey ID not in `stories.json` → WARNING

## Event Emission

When modifying an upstream file, emit a structured event:

```
[feedback_loop] updated experience/features/01_user_auth/login.md
  added screen: experience/screens/01_user_auth/login.md
  source_skill: concept/experience/screens
```

See `docs/OBSERVABILITY.md` for full event format.
