# formulasAndLookups

Ported from xlport `src/test/resources/test-suites/export/formulasAndLookups/`. `template.xlsx` and `expected.xlsx` are byte-for-byte copies of xlport's fixtures; `request.json` writes the four input values into `SheetA!C5:C8` and calls `workbook.recalculate`.

Exercises POI's evaluator on `VLOOKUP(inputA, rangeA, 2, FALSE)` and `VLOOKUP(inputB, rangeB, 2, FALSE)` — both already present as formulas in the template — driven through workbook-scoped defined names that cross sheets (`rangeB` points at `SheetB!B5:C7`). Catches regressions in the recalc path's handling of named ranges.
