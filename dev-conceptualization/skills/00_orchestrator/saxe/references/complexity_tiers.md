# Complexity Tiers

Complexity tiers control pipeline depth, checkpoint frequency, and testing
intensity. The tier is determined during `concept-1-discovery-1-overview` (Step 1)
and stored in `_concept/1_discovery/1_overview/brief.md` frontmatter as `complexity_tier`.

## Tier Definitions

### small

A focused app with a handful of features and straightforward data.

**Thresholds:** ≤5 features, ≤10 screens, no custom backend modules.

**Typical examples:** internal event organizer, team poll app, simple booking tool.

**Signals during overview:**
- User describes 1–3 things users should be able to do
- No external service integrations mentioned
- Single user role or simple admin/member split
- "Quick", "simple", "small", "internal" language

### standard

A moderate app with multiple feature groups and some complexity.

**Thresholds:** 6–15 features, moderate screen count, may have custom backend.

**Typical examples:** event management system, project tracker, internal HR tool.

**Signals during overview:**
- User describes 4–10 distinct capabilities
- Some integrations or external services mentioned
- Multiple user roles with different permissions
- Moderate domain complexity

### complex

A substantial app with many features, multi-tenancy, or SaaS requirements.

**Thresholds:** 16+ features OR SaaS/multi-tenant OR significant custom backend.

**Typical examples:** SaaS platform, marketplace, enterprise collaboration suite.

**Signals during overview:**
- User describes many capabilities or a platform
- Multi-tenant, billing, or subscription mentioned
- Complex role hierarchies or organizational structures
- Multiple external integrations
- Real-time, AI, or advanced data processing needs

## Default

If `complexity_tier` is absent from `brief.md` (e.g., concept created before
tiers were introduced), default to `standard`.

## Phase Behavior Matrix

The concept pipeline is organized into 3 phases with 9 steps:

| Phase | Step | Folder | small | standard | complex |
|-------|------|--------|-------|----------|---------|
| **Phase 1 — Discovery** | 1. Brief | 1_discovery/1_overview | full | full | full |
| | 2. Research | 1_discovery/2_research | skip | optional (ask user) | optional (ask user) |
| | 3. Brand | 1_discovery/3_brand | full | full | full |
| **Phase 2 — Experience** | 4. Journeys | 2_experience/1_journeys | full | full | full |
| | 5. Features | 2_experience/2_features | full (from journeys) | full (from journeys) | full (from journeys) |
| | 6. Screens | 2_experience/3_screens | full | full + optional storybook | full + optional storybook |
| **Phase 3 — Blueprint** | 7. Tech stack | 3_blueprint/1_techstack | automatic | automatic (offer involvement) | involved (recommend) |
| | 8. Architecture | 3_blueprint/2_architecture | skip | optional (offer involvement) | involved (recommend) |
| | 9. Data model | 3_blueprint/3_datamodel | automatic | automatic (offer involvement) | involved (recommend) |

**automatic** = skill analyzes features, makes best decisions, presents summary for lightweight approval.
**involved** = current behavior with detailed questions.
**full** = always runs with user interaction (questions and/or approval).
**full (from journeys)** = features are derived from 2_experience/1_journeys/stories.json context.

## Checkpoint Consolidation

### Concept Pipeline

| Phase | Checkpoint | small | standard | complex |
|-------|------------|-------|----------|---------|
| **Phase 1 — Discovery** | Brief + Research + Brand | **1 consolidated** (brief+brand, research skipped) | **1 consolidated** (brief + optional research + brand) | **1–3 separate** (brief own, research own if run, brand own) |
| **Phase 2 — Experience** | Journeys + Features + Screens | **1 consolidated** (all three together) | **2** (journeys own, features+screens consolidated) | **3 separate** (journeys own, features own, screens own) |
| **Phase 3 — Blueprint** | Tech + Arch + Data | **1 consolidated** (tech+data, arch skipped) | **1 consolidated** (all three together) | **3 separate** (tech own, arch own, data own) |
| | **Total** | **~3** | **~4–5** | **~7–9** |

### Implementation Pipeline

| Checkpoint | small | standard | complex |
|------------|-------|----------|---------|
| Plan | own | own | own |
| Scaffold + Startup | **consolidated with Foundation** | **consolidated** | separate |
| Foundation | consolidated above | own | own |
| Infrastructure | skip (no custom backend) | own (if applicable) | own (if applicable) |
| Feature groups | own (per group) | own (per group) | own (per group) |
| UAT | own | own | own |
| Final verification | own | own | own |
| **Total** | **~5–6** | **~7–8** | **~9+** |

## Testing Depth

| Aspect | small | standard | complex |
|--------|-------|----------|---------|
| Seed scenarios | `populated` only | all 4 | all 4 |
| Storybook stories | skip | yes | yes |
| E2E tests | yes (simplified) | yes | yes |
| agent-browser checks | yes | yes | yes |
| UAT | yes | yes | yes |

## User-Facing Tier Descriptions

Use these when presenting the tier to the user:

- **small:** "Based on your description, this is a focused app. I'll streamline the process — fewer questions, faster progress. You'll approve about 3 key decisions."
- **standard:** "This is a moderate-sized app. I'll guide you through each step, handling technical details automatically unless you want to be involved. About 5 approval points."
- **complex:** "This is a substantial app. I'll be thorough at each step and involve you in key technical decisions that affect the product. About 8–10 approval points."
