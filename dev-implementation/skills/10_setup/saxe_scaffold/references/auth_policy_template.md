# Authorization Policy Template

The generated `authorization-policy.service.ts` sets `defaultDeny: true` with empty schema rules,
which denies ALL reads and writes for all roles -- making the app appear broken on first run.

## Default Policy

Populate the `schemas` field with sensible defaults:

```typescript
schemas: {
  Data: {
    read: { anyRole: ['admin', 'superadmin', 'editor', 'viewer'] },
    write: { anyRole: ['admin', 'superadmin', 'editor'] }
  },
  PXL: {
    read: { anyRole: ['admin', 'superadmin', 'editor', 'viewer'] },
    write: { anyRole: ['admin', 'superadmin'] }
  }
}
```

## Customization

Adjust roles based on:
- The schema's `auth` configuration in `postxl-schema.json`
- Role requirements from `_concept/2_experience/2_features/` feature specs
- Principle of least privilege: start restrictive, widen as features require

## Common Patterns

- **Public read, authenticated write:** Use `anyRole` for read, specific roles for write
- **Owner-only write:** Requires row-level security (configured per-model in schema)
- **Admin-only models:** Restrict both read and write to `['admin', 'superadmin']`
