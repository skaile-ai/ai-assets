# Frontmatter Schema (TypeBox)

Define artifact frontmatter as TypeBox schemas for validation:

```typescript
import { Type, type Static } from '@sinclair/typebox'

// Implementation status — optional tracking for features/screens
export const ImplStatus = Type.Union([
  Type.Literal('pending'),
  Type.Literal('implemented'),
  Type.Literal('tested'),
])

export const Priority = Type.Union([
  Type.Literal('must-have'),
  Type.Literal('nice-to-have'),
])

// Feature file frontmatter
export const FeatureFrontmatter = Type.Object({
  priority: Priority,
  roles: Type.Array(Type.String()),
  screens: Type.Array(Type.Object({
    path: Type.String({ description: 'Relative to _concept/' }),
  })),
  data_entities: Type.Array(Type.String()),
  impl_status: Type.Optional(ImplStatus),
  last_updated: Type.String({ format: 'date' }),
})

// Screen file frontmatter
export const ScreenFrontmatter = Type.Object({
  implements: Type.Array(Type.String()),
  data_entities: Type.Array(Type.String()),
  layout: Type.Optional(Type.String()),
  impl_status: Type.Optional(ImplStatus),
  last_updated: Type.String({ format: 'date' }),
})
```

Key principles:
- **No status field** — file existence is the gate, not a status lifecycle
- **`impl_status` is optional** — only for implementation tracking on features/screens
- **`last_updated` is the only universal field** — used for freshness checks
- **Cross-references as arrays** — `screens[]`, `implements[]`, `data_entities[]`
