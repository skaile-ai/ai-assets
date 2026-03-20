# Implementation Plan Template

Appended to PLANS.md after all concept phases are approved.

```markdown
---

## Implementation Plan: <App Name>

### Scope
Build the app defined in _concept/. Phase 1: must-have features only.

### Source Artifacts
- Brief: _concept/1_discovery/1_overview/brief.md
- Features: _concept/2_experience/2_features/ (N features, M must-have)
- Tech stack: _concept/3_blueprint/1_techstack/stack.md (PostXL)
- Schema: _concept/3_blueprint/3_datamodel/postxl-schema.json (N models)
- Screens: _concept/2_experience/3_screens/ (N screens)
- Brand: _concept/1_discovery/3_brand/tokens.json

### Progress
- [ ] Project scaffold (`implement-1-setup-1-scaffold`)
- [ ] Run `pnpm run generate` from postxl-schema.json
- [ ] Database setup + Prisma migrations
- [ ] Apply brand tokens as CSS custom properties
- [ ] Auth setup (Keycloak)
```

For each must-have feature, add:

```markdown
- [ ] Feature: <NN_group_name>
  - [ ] <screen_1> — implements: <feature_path>
  - [ ] <screen_2> — implements: <feature_path>
```

Then add closing tasks:

```markdown
- [ ] Static audit (`app-audit`)
- [ ] E2E testing (`app-e2e`)
- [ ] Responsive testing
- [ ] Deploy

### Implementation Decisions
(to be filled during implementation)

### Known Technical Debt
(to be filled during implementation)

### Verification
- [ ] All must-have features implemented
- [ ] app-audit passes (0 critical, 0 high)
- [ ] app-e2e passes for all ready features
- [ ] Responsive on mobile, tablet, desktop
```
