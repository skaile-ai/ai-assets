# Reference Implementation: Onboarding Wizard

Profile-aware project setup flow. From concept-forge's `OnboardingWizard.vue`.

## Two-Step Wizard

### Step 1: Route Selection

```vue
<div class="grid grid-cols-2 gap-4">
  <button v-for="route in routes" :key="route.id"
    :class="selectedRoute === route.id ? 'border-primary-500' : 'border-gray-200'"
    @click="selectRoute(route)">
    <UIcon :name="route.icon" />
    <span>{{ route.name }}</span>
    <span>{{ route.description }}</span>
  </button>
</div>
```

Routes have default settings:

```typescript
const routes = [
  { id: 'cli_app',    name: 'CLI Application',   complexity: 'moderate', research_depth: 'light',    profile: 'default' },
  { id: 'prototype',  name: 'Quick Prototype',    complexity: 'simple',   research_depth: 'light',    profile: 'rapid_prototype' },
  { id: 'mvp',        name: 'MVP',                complexity: 'moderate', research_depth: 'moderate',  profile: 'default' },
  { id: 'product',    name: 'Production App',     complexity: 'complex',  research_depth: 'deep',      profile: 'enterprise' },
]
```

### Step 2: Details + Settings

Profile selector syncs complexity and research_depth:

```typescript
const profileDefaults = {
  default: { complexity: 'moderate', research_depth: 'moderate' },
  rapid_prototype: { complexity: 'simple', research_depth: 'light' },
  enterprise: { complexity: 'complex', research_depth: 'deep' },
}

watch(profile, (val) => {
  const defaults = profileDefaults[val]
  if (defaults) {
    complexity.value = defaults.complexity
    research_depth.value = defaults.research_depth
  }
})
```

Route-specific input forms:
- **Prototype/CLI**: single textarea for description
- **MVP/Product**: structured fields (name, problem, audience, notes)
- **Reverse Engineer**: repo URL + branch + context

All routes except reverse_engineer show: Profile, Complexity, Research depth selectors.

## On Submit

```typescript
function start() {
  emit('start', {
    route: selectedRoute.value,
    raw_description: buildDescription(),
    complexity: complexity.value,
    research_depth: research_depth.value,
    profile: profile.value,
  })
}
```

The parent page handler:
1. Calls `POST /api/pipeline/start` with all settings
2. Server creates `config.json` (machine state) and `PLANS.md` (agent-readable)
3. Refreshes pipeline status
4. Focuses first available step
5. Opens AI drawer and triggers the first skill

## Key Patterns

1. **Route seeds profile** — selecting a route pre-fills the profile (user can override)
2. **Profile syncs settings** — changing profile updates complexity + research_depth
3. **Individual overrides** — user can manually change complexity/research after profile selection
4. **Two state files** — config.json for UI, PLANS.md for agents
5. **No artifact folders created** — skills create their own output folders
