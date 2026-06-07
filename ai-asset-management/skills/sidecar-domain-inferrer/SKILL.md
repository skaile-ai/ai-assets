---
name: sidecar-domain-inferrer
description: "Use when curating a store source and you want navigational DOMAIN.md groupings inferred from a foreign upstream repo's folder structure WITHOUT modifying that repo. Reads the upstream tree + already-discovered assets, clusters them into one-level (non-nested) domains, places a single DOMAIN.md per domain at the lowest common ancestor of its members, classifies each as additions | alternatives | sequence, and writes DRAFT DOMAIN.md files into the store-managed sidecar (mirroring upstream paths) for human review before commit. Trigger when the user says 'infer domains for source X', 'group this repo's assets', 'draft DOMAIN.md sidecars', or 'organize a folder-only repo for the store'."
metadata:
  version: 1.0.0
  source: NEW
  tags: [asset-store, domains, sidecar, discovery, navigation, curator, inference]
  stage: alpha
  requires:
    - skill:source-sidecar-curator
  prerequisites:
    files:
      - path: ~/.skaile/sources
        gate: hard
        description: "Sidecar root must exist (created by `skaile source add --sidecar`)"
    inputs_required:
      - id: slug
        label: "Sidecar slug (subdirectory under ~/.skaile/sources/)"
        type: text
    reads:
      - path: workspaces/packages/workspaces/discovery/CLAUDE.md
      - path: skaile-store/docs/proposals/sidecar-domains.md
    writes:
      - path: ~/.skaile/sources/<slug>/**/DOMAIN.md
keywords: [domains, sidecar, navigation, grouping, alternatives, sequence, additions]
---

# Sidecar Domain Inferrer

You infer **navigational domains** for a store source and write them as DRAFT
`DOMAIN.md` files into the **sidecar** — never into the upstream repo. A domain
is display-only: it groups assets so users discover them together. It is **not**
a bundle (members are often alternatives or a pipeline, not install-all) and it
**never** participates in asset identity (`<publisher>/<name>@<version>`) or the
flat install layout.

Hard rules you must honor (they mirror the discovery validator, which rejects
violations at ingest):

- **One level only.** Domains are NOT nested. No `DOMAIN.md` may sit inside the
  subtree claimed by another `DOMAIN.md`.
- **Claims-below.** A `DOMAIN.md` claims its own directory plus everything below
  it (whole-segment path containment). Every asset belongs to at most one domain.
- **Path is layout, not data.** Folder names are never parsed for identity. The
  asset's install name comes from frontmatter (else folder name); the domain
  `slug` is metadata only.
- **Never touch the upstream repo.** All output lands in the sidecar, mirroring
  the upstream relative paths.
- **You never auto-commit.** You produce drafts + a review report; a human
  approves before anything is written to the sidecar's tracked tree.

You drive a six-step workflow: locate → read → cluster → anchor+classify →
draft → review.

## Step 0: Locate the sidecar and the upstream tree

1. Confirm the `slug` is a registered sidecar:
   ```bash
   skaile source sidecar list
   ```
2. Get the upstream repo + ref the sidecar annotates, and list discovered assets:
   ```bash
   skaile source sidecar show <slug>     # upstream url@ref + sidecar path
   skaile source candidates <slug>       # discovered assets: kind, name, relativePath, requires
   ```
   The discovered-asset list (kind, name, `relativePath`, requires edges) is your
   authoritative input. You group these — you never re-derive names or identity.

## Step 1: Read the tree

Build the upstream folder tree from the discovered assets' `relativePath`s. Note
per-folder: asset count, kinds present, name stems, any numeric/stage prefixes
(`01_`, `step-`), and the requires-graph edges between assets.

## Step 2: Cluster into candidate domains

- A **candidate domain** is a folder with **2+ sibling assets** of compatible
  concern. A folder with a single asset, or a purely structural folder
  (`src/`, `docs/`, `references/`, `contracts/`, `shared/`), is **not** its own
  domain — fold it into the nearest meaningful ancestor or leave its asset
  ungrouped.
- Reference-only folders (no invocable assets) are never domains.

## Step 3: Anchor + classify each domain

**Anchor placement (guarantees non-nesting):** place a domain's single
`DOMAIN.md` at the **lowest common ancestor** folder of all its members — the
deepest folder that still contains every member. This keeps the claim tight and
makes it structurally impossible for two anchors to sit on the same path lineage.
After choosing anchors, double-check no anchor folder is an ancestor of another;
if two would nest, either merge them, re-anchor the child deeper as a sibling, or
demote the weaker one to `additions` members of the parent.

**Classify the relation** (requires-graph topology dominates folder-name signals):

| Relation | Signal | Affordance |
|---|---|---|
| `alternatives` | sibling folders share a name stem with a differing tech token (`-js`/`-python`, `-redis`/`-yjs`), near-identical kind/category, and **no** requires-edges between them | pick one |
| `sequence` | numeric/lexical ordering prefixes, stage words (concept→implement→evaluate), or a linear requires-chain (A→B→C) | ordered 1..n |
| `additions` | heterogeneous kinds, independent capabilities, sparse/fan-out requires — the **default** when not clearly the others | pick some |

When confidence is low, choose `additions` and flag it for review rather than
guessing `alternatives`/`sequence`.

**Flat-name collision guard:** if two members derive the same flat install name
in different folders, surface a collision warning — do not silently group them.

## Step 4: Draft the DOMAIN.md files

For each domain, write a draft to the sidecar staging area at the anchor's
mirrored path: `~/.skaile/sources/<slug>/<anchor-path>/DOMAIN.md`.

```yaml
---
slug: <github-shaped: a-z0-9 + single hyphens, ≤39, unique per source>
title: <human title>
description: <one line; rendered in the collapsed info region on every surface>
relation: additions | alternatives | sequence
order: <integer; sort among sibling domains>
members:                      # OPTIONAL ordering/recommend overlay on subtree assets
  - { ref: "@<pub>/<name>", order: 1, recommended: true }
---

# <title>

<human navigation prose; the frontmatter above is the machine-readable manifest>
```

Rules: `slug` unique per source; `members[]` may only reorder/flag assets already
in the claimed subtree (it cannot add cross-subtree assets). `order` is required
on members of a `sequence` domain.

## Step 5: Review report + approval

Produce a report — do NOT commit:

- Per domain: anchor path, slug, title, relation + the evidence for it, member
  list, and confidence.
- Explicitly flag: low-confidence `alternatives`/`sequence` calls, any nesting
  conflict you had to auto-resolve, flat-name collisions, and assets left
  ungrouped.

Then validate the draft set against the discovery rules before handing off:

```bash
skaile source sidecar validate <slug>   # runs the DOMAIN.md validator (nesting, dup slug, slug/relation)
```

Present the report + the validate result. Only on explicit human approval do you
commit the DOMAIN.md files into the sidecar's tracked tree and register them.
Re-runs after upstream changes re-enter this same loop; preserve prior human
edits and only propose changes for the delta.

## Notes

- The store reads these DOMAIN.md from the sidecar at sync time and stamps a
  display-only `domainSlug` on each catalog row; bytes still come from the
  upstream repo (pointer-only). See `skaile-store/docs/proposals/sidecar-domains.md`.
- Companion skills: `source-sidecar-curator` (refines the `assets:` inventory),
  `source-deps-analyzer` (requires-graph enrichment).
