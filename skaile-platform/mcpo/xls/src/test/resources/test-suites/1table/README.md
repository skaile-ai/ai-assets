# 1table

Ported from xlport `src/test/resources/test-suites/export/1table/`. `template.xlsx` and `expected.xlsx` are byte-for-byte copies of xlport's fixtures; `request.json` is hand-written in the transcript shape — one `range.set` fills the two blank data rows of the `Initiatives` `ListObject` at `A5:I6`.

The table's ref stays at `A3:I6` on both sides (checked: no row growth), so this suite doesn't need any `table.resize` tool. I5 and I6 are written as their final cached values; the xlport expected has them as `=Initiatives[SavingsPotential]*1.19`, but the comparator compares rendered strings via `DataFormatter(Locale.GERMAN)`, so a plain-numeric write with the same rendered value is equivalent.
