# Recipes

Implementation recipes for the Typed Action Protocol (omp RPC + Nuxt 4).

## Recipes

| Recipe | Description |
|--------|-------------|
| `recipe-skill-adapter.md` | **Core** — `skill-adapter.ts`: state machine, action routing, consumer presence, input derivation from pipeline.json |
| `recipe-dispatch-endpoint.md` | **API** — Three Nitro endpoints: POST /api/agent/dispatch, POST /api/agent/respond, GET /api/agent/actions (SSE) |
| `recipe-frontend-composable.md` | **Frontend** — `useSkillActions` composable: SSE management, action routing, reactive state |
| `recipe-action-components.md` | **UI** — Vue 3 components: SkillInputDialog, SkillApprovalDialog, SkillStateIndicator, SkillActionRenderer, SkillAiDrawer |
