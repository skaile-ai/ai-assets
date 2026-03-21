# Improvement Ideas

## 2026-03-12: Learning from concept-forge collaboration implementation

### Scripts are placeholders
All Python scripts (`learn_from_success.py`, `track_versions.py`, `manage_recipes.py`, `manage_examples.py`) are stubs. They should be implemented to:
- `learn_from_success.py`: Parse a Vue/TS file, extract tiptap extension usage, compare against existing recipes, and suggest recipe updates.
- `track_versions.py`: Read `package.json` from the current project, extract tiptap/yjs/hocuspocus versions, and update `versions.json`.
- `manage_recipes.py`: CRUD operations on recipe files (list, create, update, validate frontmatter).

### Collaboration recipe was outdated
The original recipe referenced `@tiptap/extension-collaboration-cursor` and `CollaborationCursor` — both wrong. The correct package is `@tiptap/extension-collaboration-caret` with export `CollaborationCaret`. Recipes should be periodically validated against actual package names.

### Missing pattern: Nitro WebSocket integration
The skill had no knowledge of how to embed WebSocket servers (like Hocuspocus) into Nitro. This is a common need for Nuxt apps. Added to patterns.md.

### Missing pattern: Conditional feature enablement
The "check health, then enable" pattern (start solo, upgrade to collaborative) is broadly useful beyond collaboration. Could be extracted as a general pattern.
