# Improvement Ideas — Knowledge Paper Research

---

## 2026-03-03 — First run (Viggers et al. 2026, Anorexia Nervosa / ICD-11)

### SearXNG unavailability
- `search.py` exits immediately with "Connection refused" when SearXNG is not
  running. The pipeline should detect this at Phase 3 start and immediately
  fall back to the agent's built-in `web_search` tool rather than failing
  silently or requiring manual intervention.
- **Fix**: add a `--dry-run` mode to `search.py` that tests connectivity and
  prints a clear fallback message; have the skill check connectivity before
  dispatching parallel searches.

### DOI parsing in docling output
- Docling sometimes renders DOIs with space-separated characters (e.g.
  `1 0 . 1 0 0 2 / e a t . 2 3 9 9 4`). The current `extract_structure.py`
  has a space-collapse fallback that handles this, but it should be unit-tested
  against more DOI formats.

### Author filename encoding
- Non-ASCII author names (e.g. `Jørgensen`) are written directly into the
  filename. Some filesystems handle this correctly; others do not. Consider
  offering an ASCII-transliteration option (e.g. `author_Mie_Sedoc_Jorgensen.md`)
  as a fallback.

### Male subgroup underpowered warning
- The AN-BP male subgroup (n=5) was flagged only in `experiment.md`.
  This finding is equally relevant to `results.md` and `concepts.md`. The
  schema should include a cross-file consistency pass to ensure major caveats
  appear in every file where they are relevant.

### Parallel dispatch via Task tool
- The 11-file parallel write attempted via `quick_task` agents failed (agents
  exited without calling `submit_result`). For content-generation tasks, use
  the main agent directly rather than quick_task subagents.
