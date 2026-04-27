# Test fixtures

Binary workbooks used by smoke scripts and unit tests.

| File | Used by | Provenance |
|---|---|---|
| `vba-hello.xlsm` | `smoke/phase9-smoke.sh` | Built Phase 10 via `jshell`/POI from `test-data/vba-test.xlsm`. Fresh `XSSFWorkbook` with a single `Sheet1`, with `xl/vbaProject.bin` copied verbatim from the source `.xlsm` via `XSSFWorkbook.setVBAProject(InputStream)`. POI cannot author `vbaProject.bin` from scratch — the binary must be lifted from an existing macro-enabled workbook. Regenerate with `BuildVbaFixture` (see commit history). |
