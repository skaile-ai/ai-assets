# daysbug

Ported from xlport `src/test/resources/test-suites/export/daysbug/`. `template.xlsx` and `expected.xlsx` are byte-for-byte copies of xlport's fixtures; `request.json` writes `7` into `Deal!A4` and recalculates.

Known-regression replay kept around as low-cost insurance. Template has `B7 = =A4*3`; after setting `A4=7` and recalculating, B7's cached result refreshes to 21. The xlport-produced expected stores B7 as a plain numeric (the exporter overwrites formulas with cached values), so this suite also doubles as coverage that the comparator's formula-cached-result branch renders identically to a plain numeric cell with the same value.
