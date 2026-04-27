# thedate

Ported from xlport `src/test/resources/test-suites/export/thedate/`. `template.xlsx` and `expected.xlsx` are byte-for-byte copies of xlport's fixtures; `request.json` writes the ISO string `2021-05-09T00:00:00` into the named cell `thedate` (= `Sheet1!C5`).

The template's C5 style is `numFmtId=14` (Excel's built-in short-date format), so rendering the written value through `DataFormatter(Locale.GERMAN)` is the regression signal here — a change in POI's date-serial handling, locale formatting, or the `range.set` ISO-date branch will diverge from the xlport-produced expected rendering of serial 44325.
