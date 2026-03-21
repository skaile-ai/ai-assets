---
name: knowledge-writing-contract
description: "Shared contract for all knowledge-writing skills. Describes writing output formats, chapter/episode structure, template conventions, and how writing skills consume research artifacts."
metadata:
  type: system
  do_not_invoke: true
  stage: alpha
  requires: []
---

# Knowledge Writing — Shared Contract

**Do not invoke directly.** This is a dependency contract — all `knowledge-writing` skills read this before operating.

## Scope

Writing skills transform research artifacts and raw material into polished outputs: books, podcast scripts, articles. This contract defines their shared conventions.

## Skills Overview

| Skill | Input | Output |
|-------|-------|--------|
| `writer-book` | research report, outline | `_book/` folder with chapters |
| `writer-podcast` | research or topic | `_podcast/` with speaker scripts |
| `paper-podcast` | extracted paper | podcast episode script from academic paper |

## Output Folder Structure

### Book
```
_book/
├── meta.json               ← title, author, chapters list, status
├── outline.md              ← chapter structure + key points per chapter
├── chapters/
│   ├── 00_introduction.md
│   ├── 01_<chapter-slug>.md
│   └── ...
└── review/
    └── notes.md            ← editorial notes, revision queue
```

### Podcast
```
_podcast/
├── meta.json               ← episode title, speakers, duration estimate
├── outline.md              ← episode beats, key transitions
└── script.md               ← full speaker script with cues
```

## Chapter Format

```markdown
---
chapter: N
title: "<Chapter Title>"
status: draft | review | final
word_count_target: 2000
---

# <Chapter Title>

<body>

---
*Chapter N of N — <Book Title>*
```

## Podcast Script Format

```markdown
---
episode: N
title: "<Episode Title>"
speakers: [HOST, GUEST] | [ALEX, MORGAN]
duration_estimate: "<N> minutes"
source: <research file path>
---

# <Episode Title>

**[INTRO MUSIC FADES]**

**HOST:** <line>

**GUEST:** <line>
...

**[OUTRO]**
```

## Consumption of Research

Writing skills read from `_research/` (produced by `knowledge-research` skills):
- `_research/synthesis/report.md` → primary source material
- `_research/findings/` → supporting details per topic
- `_research/sources/index.md` → citation verification
