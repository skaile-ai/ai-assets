# 1object

Ported from xlport `src/test/resources/test-suites/export/1object/`. `template.xlsx` and `expected.xlsx` are byte-for-byte copies of xlport's fixtures; `request.json` is hand-written in the excel-mcp transcript shape (§5 of the plan): one `range.set` per named scalar cell on sheet `Object`.

`B9` is written as Excel serial 43466 (= 2019-01-01) rather than an ISO date string. The template's B9 already carries a date format at style s=4, so the raw serial is all that's needed to reproduce xlport's expected rendering.
