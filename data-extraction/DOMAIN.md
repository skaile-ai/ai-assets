---
name: data-extraction
description: "Extract structured content and source code from proprietary or binary file formats where off-the-shelf tools fall short (e.g. VBA from modern MS Access .accdb)."
type: domain
building_blocks:
  contracts: "TBD - output layout conventions (complete/ vs partial/ split, per-artifact provenance)."
  docs: "File-format reverse-engineering notes captured per skill under references/."
  skills: "One skill: msaccess-vba-extractor (VBA source out of ACE12 .accdb / legacy .mdb)."
  agents: "TBD"
  prompts: "TBD"
  tools: "TBD"
stage: alpha
---

# Data Extraction

Skills that recover data or source from file formats that common tooling cannot
read. The focus is legacy and binary Microsoft/Office container formats where the
usual libraries (olevba, mdbtools) either don't support the format version or
don't reassemble the internal structures.

Each skill is self-contained: a stdlib-only extractor script plus reference notes
documenting the on-disk format, so the knowledge survives even if the code needs
porting.

## Building Blocks

| Folder | Purpose |
|--------|---------|
| `skills/` | `msaccess-vba-extractor` |

## Skills

| Skill | Purpose |
|-------|---------|
| `msaccess-vba-extractor` | Decompress and recover VBA module source (`.bas`/`.cls`) from Access `.accdb` (ACE12) and `.mdb` databases without MS Access installed |

## Conventions

- Extractors are pure Python stdlib (no external deps) so they run anywhere.
- Output is split into `complete/` (fully recovered, verified) and `partial/`
  (best-effort, truncated) - never silently mix uncertain output with clean output.
- Every skill carries a `references/` note documenting the file format it parses.
