---
name: msaccess-vba-extractor
description: "Extract VBA source code (.bas / .cls modules, forms, reports) from a Microsoft Access database (.accdb / .mdb) WITHOUT MS Access installed. Use when asked to extract, dump, recover, or read VBA / macros / module code from an Access database, or when olevba fails with 'not a supported file type' on an .accdb. Handles modern ACE12 'Standard ACE DB' files that olevba and mdbtools cannot decompress."
license: MIT
compatibility: "Python 3.8+ stdlib only. No external dependencies."
metadata:
  author: skaile
  source: ORIGINAL
  tags: [vba, msaccess, accdb, mdb, ace, extract, decompile, ms-ovba, legacy-data]
  stage: alpha
---

# MS Access VBA Extractor

Recover VBA module source from an Access `.accdb` (ACE12) or `.mdb` (Jet4)
database when you cannot open it in Access itself.

## When to reach for this

- "Extract the VBA from this .accdb", "dump the macros", "get the module code".
- `olevba file.accdb` fails with `FileOpenError: ... not a supported file type`.
- `mdb-export` returns `File not found` for the VBA storage.

## Do NOT waste time on tools that don't work here

Confirmed dead ends for modern `.accdb` (ACE12, header `Standard ACE DB`):

| Tool | Result |
|------|--------|
| `olevba` / `oletools` | Refuses the file - only reads OLE2-wrapped VBA |
| `mdbtools` (`mdb-export MSysAccessStorage`) | Cannot reassemble the ACE12 long value; prints `File not found` |
| Wine + Access | Heavy, needs a Windows install; last resort only |

The VBA is MS-OVBA-compressed inside ACE long-value pages. You decompress it
directly. See `references/format-notes.md` for the full on-disk format.

## Workflow

### 1. Run the extractor

```bash
python3 scripts/extract_vba.py "path/to/Database.accdb" -o ./vba
```

Output is split so you never confuse recovered code with best-effort guesses:

```
vba/
  complete/   fully decompressed, verified valid VBA (ends on a real statement)
  partial/    truncated at a desync point - clean prefix only, tail missing
```

The script prints a summary like `complete: 24   partial (truncated): 8` and
lists the partial modules.

### 2. Sanity-check

- Spot-check a couple of `complete/` files - they should read as coherent VBA.
- `python3 scripts/extract_vba.py --selfcheck` round-trips the MS-OVBA
  decompressor against a constructed container (fails loudly if the codec breaks).

### 3. Handle the partial modules honestly

A module lands in `partial/` when its compressed bytes are spread across
physically **non-adjacent, mis-linked** LVAL pages. This is a real limitation:
the raw page pointers are unreliable and any "guess the next page" heuristic
produces **printable garbage** (wrong copy-token references), so the script stops
at the first desync rather than emit corrupt code.

Tell the user plainly which modules are partial and give them the reliable path:

> Open the database in MS Access -> VBA editor (Alt+F11) -> right-click the
> module -> **Export File**.

Do not claim a partial module is complete, and do not hand over garbage-tailed
output as if it were source.

## What "correct" looks like

- German text (ä ö ü ß, bytes 128-255) is **valid source**, not corruption.
- Real corruption = NUL bytes or control chars (except tab/CR/LF) in the output.
- A complete module ends on `End Sub` / `End Function` / `End Property` / ... or,
  for an empty module, on `Option Explicit`.

## Files

| Path | Purpose |
|------|---------|
| `scripts/extract_vba.py` | The extractor + `--selfcheck`. Pure stdlib. |
| `references/format-notes.md` | ACE/Jet + MS-OVBA format, and exactly why the hard cases are hard. |

## Extending to full recovery (optional, non-trivial)

To recover the partial modules programmatically you must parse the
`MSysAccessStorage` table and follow each VBA stream's `Lv` OLE long-value chain
(head page/row + length) instead of guessing page order. That is a genuine ACE
long-value parser and a meaningful piece of work - only build it if full offline
recovery is a hard requirement. Otherwise the Access export above is faster.
