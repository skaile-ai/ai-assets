---
name: Nuxt + Drizzle ORM + SQLite
description: Full-stack database setup with Drizzle ORM, better-sqlite3, migrations, seeding, and typed API routes in Nuxt.
libraries_used: [drizzle-orm, drizzle-kit, better-sqlite3, tsx, uuid]
---

# Nuxt + Drizzle ORM + SQLite

## 1. Install Dependencies

```bash
pnpm add drizzle-orm better-sqlite3 uuid
pnpm add -D drizzle-kit tsx @types/uuid
```

## 2. Drizzle Config

Create `drizzle.config.ts` in project root:

```typescript
import type { Config } from 'drizzle-kit';

export default {
  schema: './server/db/schema.ts',
  out: './server/db/migrations',
  dialect: 'sqlite',
  dbCredentials: {
    url: './server/db/cards.sqlite',
  },
} satisfies Config;
```

## 3. Database Schema

Create `server/db/schema.ts`:

```typescript
import { integer, sqliteTable, text, primaryKey } from 'drizzle-orm/sqlite-core';
import { relations } from 'drizzle-orm';

// Tables
export const users = sqliteTable('users', {
  id: text('id').primaryKey(), // UUID
  name: text('name'),
  email: text('email'),
});

export const items = sqliteTable('items', {
  id: text('id').primaryKey(),
  text: text('text').notNull(),
  categoryId: integer('category_id').notNull().references(() => categories.id),
  ownerId: text('owner_id').references(() => users.id),
  isPublic: integer('is_public', { mode: 'boolean' }).default(false),
});

export const categories = sqliteTable('categories', {
  id: integer('id').primaryKey({ autoIncrement: true }),
  name: text('name').notNull().unique(),
});

// Many-to-many junction table with composite primary key
export const ratings = sqliteTable('ratings', {
  userId: text('user_id').notNull().references(() => users.id),
  itemId: text('item_id').notNull().references(() => items.id),
  rating: integer('rating').notNull(),
}, (table) => ({
  pk: primaryKey({ columns: [table.userId, table.itemId] }),
}));

// Relations (for query builder)
export const categoriesRelations = relations(categories, ({ many }) => ({
  items: many(items),
}));

export const itemsRelations = relations(items, ({ one, many }) => ({
  category: one(categories, {
    fields: [items.categoryId],
    references: [categories.id],
  }),
  owner: one(users, {
    fields: [items.ownerId],
    references: [users.id],
  }),
  ratings: many(ratings),
}));

export const ratingsRelations = relations(ratings, ({ one }) => ({
  user: one(users, {
    fields: [ratings.userId],
    references: [users.id],
  }),
  item: one(items, {
    fields: [ratings.itemId],
    references: [items.id],
  }),
}));
```

## 4. Database Utility

Create `server/utils/db.ts` (auto-imported by Nitro):

```typescript
import { drizzle } from 'drizzle-orm/better-sqlite3';
import Database from 'better-sqlite3';
import * as schema from '../db/schema';
import path from 'path';

const dbPath = path.resolve(process.cwd(), 'server/db/cards.sqlite');
const sqlite = new Database(dbPath);

export const db = drizzle(sqlite, { schema });
```

**Key:** Placing this in `server/utils/` makes `db` auto-importable in all API routes.

## 5. Migration Script

Create `server/db/migrate.ts`:

```typescript
import { drizzle } from 'drizzle-orm/better-sqlite3';
import { migrate } from 'drizzle-orm/better-sqlite3/migrator';
import Database from 'better-sqlite3';
import path from 'path';

const dbPath = path.resolve(process.cwd(), 'server/db/cards.sqlite');
const sqlite = new Database(dbPath);
const db = drizzle(sqlite);

migrate(db, { migrationsFolder: path.resolve(process.cwd(), 'server/db/migrations') });
sqlite.close();
```

## 6. Seed Script

Create `server/db/seed.ts`:

```typescript
import { drizzle } from 'drizzle-orm/better-sqlite3';
import Database from 'better-sqlite3';
import * as schema from './schema';
import { users, categories, items } from './schema';
import fs from 'fs';
import path from 'path';
import { v4 as uuidv4 } from 'uuid';

const dbPath = path.resolve(process.cwd(), 'server/db/cards.sqlite');
const sqlite = new Database(dbPath);
const db = drizzle(sqlite, { schema });

async function main() {
  try {
    const jsonPath = path.resolve(process.cwd(), 'public/mockup/data.json');
    const data = JSON.parse(fs.readFileSync(jsonPath, 'utf-8'));

    const [defaultUser] = await db.insert(users)
      .values({ id: uuidv4(), name: 'Default User' })
      .returning();

    // Insert categories and items from JSON...
    console.log('Database seeded successfully!');
  } catch (error) {
    console.error('Error seeding database:', error);
    process.exit(1);
  } finally {
    sqlite.close();
  }
}

main();
```

## 7. Package.json Scripts

```json
{
  "scripts": {
    "db:generate": "drizzle-kit generate",
    "db:migrate": "tsx server/db/migrate.ts",
    "db:seed": "tsx server/db/seed.ts",
    "db:setup": "pnpm db:migrate && pnpm db:seed"
  }
}
```

## 8. API Route Patterns

### GET with filters and relational queries

```typescript
// server/api/items/index.get.ts
import { db } from '../../utils/db';
import { like, inArray } from 'drizzle-orm';

export default defineEventHandler(async (event) => {
  const { userId, searchText, categoryIds } = getQuery(event);

  const result = await db.query.items.findMany({
    where: (items, { and }) => {
      const conditions = [];
      if (searchText) conditions.push(like(items.text, `%${searchText}%`));
      if (categoryIds) {
        const ids = (categoryIds as string).split(',').map(id => parseInt(id, 10));
        conditions.push(inArray(items.categoryId, ids));
      }
      return and(...conditions);
    },
    with: {
      category: true,
      ratings: true,
    },
  });

  return result;
});
```

### POST with upsert (onConflictDoUpdate)

```typescript
// server/api/items/rate.post.ts
import { db } from '../../utils/db';
import { ratings, users } from '../../db/schema';

export default defineEventHandler(async (event) => {
  const { userId, itemId, rating } = await readBody(event);

  // Ensure user exists (upsert pattern)
  await db.insert(users).values({ id: userId }).onConflictDoNothing();

  // Upsert rating
  const [newRating] = await db
    .insert(ratings)
    .values({ userId, itemId, rating })
    .onConflictDoUpdate({
      target: [ratings.userId, ratings.itemId],
      set: { rating },
    })
    .returning();

  return newRating;
});
```

## Workflow

1. Define/update schema in `server/db/schema.ts`
2. Run `pnpm db:generate` to create migration SQL
3. Run `pnpm db:migrate` to apply migrations
4. Run `pnpm db:seed` to populate initial data
5. Or use `pnpm db:setup` for migrate + seed in one step
