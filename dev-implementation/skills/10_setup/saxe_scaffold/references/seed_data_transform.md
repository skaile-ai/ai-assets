# Seed Data Transformation

Replace the generated Excel-based seed migration with concept-aligned realistic data.

## Steps

1. Read `_concept/3_blueprint/3_datamodel/seed.json` and extract the `populated` scenario
2. Transform model keys: PascalCase singular to camelCase plural
   - `Organization` -> `organizations`
   - `UserProfile` -> `userProfiles`
3. Transform field names: camelCase to snake_case
   - `cloudProvider` -> `cloud_provider`
   - `createdAt` -> `created_at`
4. Write transformed data to `backend/test-data.json`
5. Update `backend/libs/seedData/src/seed-migrations.ts`:

```typescript
// Replace the Excel seed migration with:
{ type: 'jsonSeed', filename: 'test-data.json', scenario: 'populated' }
```

This gives developers realistic, concept-aligned data from the first `pnpm run dev`.
