# Recipe: Profile System

Configuration presets with inheritance for pipeline systems.

## profiles.json

```json
{
  "version": "1.0",
  "profiles": {
    "default": {
      "name": "Default",
      "inherits_from": null,
      "settings": {
        "route": "mvp",
        "complexity": "moderate",
        "research_depth": "moderate",
        "standards_injection": true
      }
    },
    "rapid_prototype": {
      "name": "Rapid Prototype",
      "inherits_from": "default",
      "settings": {
        "route": "prototype",
        "complexity": "simple",
        "research_depth": "light"
      }
    },
    "enterprise": {
      "name": "Enterprise",
      "inherits_from": "default",
      "settings": {
        "route": "product",
        "complexity": "enterprise",
        "research_depth": "deep"
      }
    }
  }
}
```

## Resolution Order

1. **Project override** (`_concept/profile.json`) — project-specific settings
2. **Selected profile** — from profiles.json
3. **Default profile** — fallback

```typescript
function resolveProfile(profileName: string, projectOverride?: object): Settings {
  const profiles = JSON.parse(readFileSync('profiles.json', 'utf-8'))
  const profile = profiles.profiles[profileName] || profiles.profiles.default

  // Build inheritance chain
  let settings = {}
  if (profile.inherits_from) {
    settings = { ...resolveProfile(profile.inherits_from) }
  }
  settings = { ...settings, ...profile.settings }

  // Apply project override
  if (projectOverride) {
    settings = { ...settings, ...projectOverride }
  }

  return settings
}
```

## UI Integration

In the onboarding wizard:
1. Show profile selector (Default, Rapid Prototype, Enterprise)
2. When profile changes, sync complexity + research_depth to profile defaults
3. Allow manual override of individual settings
4. Store selected profile name in config.json

```vue
<UFormField label="Profile">
  <USelect v-model="profile" :items="profileOptions" />
</UFormField>

<!-- Complexity + research auto-sync when profile changes -->
watch(profile, (val) => {
  const defaults = profileDefaults[val]
  if (defaults) {
    complexity.value = defaults.complexity
    research_depth.value = defaults.research_depth
  }
})
```

## Bidirectional Standards Sync

For advanced use: push proven project patterns back to profiles.

1. Compare project `_standards/` with profile standards
2. Show diff on conflict
3. User chooses: keep project, keep profile, or merge
4. Update selected target
