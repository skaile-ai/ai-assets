# Semantic Type System

The data model (`06_datamodel/model.json`) uses stack-independent semantic types.
These types carry **intent** — a stack translator maps them to concrete DB types.

## Type Reference

| Type | Meaning | Example |
|------|---------|---------|
| `string` | Short text (< 255 chars) | name, title |
| `text` | Long text (unlimited) | description, bio |
| `richtext` | Formatted content (HTML/markdown) | article body |
| `integer` | Whole number | count, age |
| `float` | Decimal number | price, rating |
| `boolean` | Yes/no | is_active, is_published |
| `date` | Date only (no time) | birth_date |
| `datetime` | Date + time with timezone | created_at |
| `enum` | Fixed set of choices | status, role |
| `json` | Arbitrary structured data | settings, metadata |
| `image` | Image file reference | avatar, cover |
| `file` | Any file reference | attachment, document |
| `uuid` | Unique identifier | id, external_id |
| `slug` | URL-safe string | url_slug |
| `email` | Email address | user email |
| `url` | Web address | website, link |
| `password` | Hashed secret | password_hash |
| `relation` | Link to another entity | assigned_to |

## Stack Translation Table

| Semantic | Directus | Prisma | Supabase | Raw SQL |
|----------|----------|--------|----------|---------|
| `string` | `string` (input) | `String` | `text` | `VARCHAR(255)` |
| `text` | `text` (textarea) | `String` | `text` | `TEXT` |
| `richtext` | `text` (WYSIWYG) | `String` | `text` | `TEXT` |
| `integer` | `integer` | `Int` | `int4` | `INTEGER` |
| `float` | `float` | `Float` | `float8` | `DOUBLE PRECISION` |
| `boolean` | `boolean` | `Boolean` | `bool` | `BOOLEAN` |
| `date` | `date` | `DateTime` | `date` | `DATE` |
| `datetime` | `timestamp` | `DateTime` | `timestamptz` | `TIMESTAMPTZ` |
| `enum` | `string` (dropdown) | `enum` | `text` + check | `VARCHAR` + CHECK |
| `json` | `json` | `Json` | `jsonb` | `JSONB` |
| `image` | M2O → directus_files | `String` (URL) | storage ref | `TEXT` |
| `file` | M2O → directus_files | `String` (URL) | storage ref | `TEXT` |
| `uuid` | `uuid` | `String @id @default(uuid())` | `uuid` | `UUID` |
| `slug` | `string` | `String` | `text` | `VARCHAR(255)` |
| `email` | `string` | `String` | `text` | `VARCHAR(255)` |
| `url` | `string` | `String` | `text` | `TEXT` |
| `password` | `hash` | `String` | `text` | `TEXT` |

## Relationship Types

| Type | Meaning | Example |
|------|---------|---------|
| `m2o` | Many-to-one | task.assigned_to → user |
| `o2m` | One-to-many (inverse of m2o) | user.tasks → task[] |
| `m2m` | Many-to-many (junction table) | task ↔ tag |

## Rules for Skills

- Always use semantic types in `model.json` — never raw SQL types
- When `type` is `enum`, the field must have an `enum_id` referencing a top-level enum
- When `type` is `relation`, the field should be documented in the `relationships[]` array
- Every entity must have a `uuid` primary key field named `id`
- Stack-specific concerns (Directus system collections, Prisma decorators) are handled
  by the stack translator, not by the data model skill
