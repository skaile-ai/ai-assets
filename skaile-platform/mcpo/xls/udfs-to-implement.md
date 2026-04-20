# Excel MCP Server - UDFs to Implement

Custom `FreeRefFunction` implementations to register via `workbook.addToolPack(UDFFinder)`.
These are scalar-returning Excel functions missing from POI's built-in registries (FunctionEval + AnalysisToolPak) that can be implemented without spill-range support.

## Priority 1 - Trivial (10-20 lines each)

| Function | Since | What it does |
|----------|-------|-------------|
| IFNA | 2013 | `=IF(ISNA(x), fallback, x)` |
| IFERROR | 2007 | `=IF(ISERROR(x), fallback, x)` - verify not already in POI |

## Priority 2 - Low effort, high value (30-50 lines each)

| Function | Since | What it does |
|----------|-------|-------------|
| IFS | 2019 | Cascading IF without nesting: `=IFS(cond1, val1, cond2, val2, ...)` |
| SWITCH | 2019 | Multi-value dispatch: `=SWITCH(expr, val1, result1, val2, result2, ..., default)` |
| CONCAT | 2019 | CONCATENATE that accepts ranges, not just individual cells |
| TEXTJOIN | 2019 | Join range with delimiter, optional skip-blanks flag |

## Priority 3 - Medium effort, high value (80-120 lines each)

| Function | Since | What it does |
|----------|-------|-------------|
| MAXIFS | 2019 | Conditional MAX (same multi-criteria pattern as SUMIFS) |
| MINIFS | 2019 | Conditional MIN (same multi-criteria pattern as SUMIFS) |
| TEXTBEFORE | 2022 | Extract text before Nth occurrence of delimiter |
| TEXTAFTER | 2022 | Extract text after Nth occurrence of delimiter |

## Priority 4 - Medium-high effort, increasingly common (100-150 lines)

| Function | Since | What it does |
|----------|-------|-------------|
| LET | 2020 | Named variables in formulas. Needs variable binding in eval context. |

## Priority 5 - Low effort, moderate value (10-30 lines each)

### Math / Rounding

| Function | Since | What it does |
|----------|-------|-------------|
| CEILING.MATH | 2013 | Rounds up with mode/significance |
| FLOOR.MATH | 2013 | Rounds down with mode/significance |
| DECIMAL | 2013 | Text in base N to decimal number |
| BASE | 2013 | Decimal number to text in base N |
| ARABIC | 2013 | Roman numeral string to number |

### Text

| Function | Since | What it does |
|----------|-------|-------------|
| VALUETOTEXT | 2021 | Value to string representation |
| NUMBERVALUE | 2013 | Locale-aware text-to-number parsing |

### Bitwise

| Function | Since | What it does |
|----------|-------|-------------|
| BITAND | 2013 | Bitwise AND |
| BITOR | 2013 | Bitwise OR |
| BITXOR | 2013 | Bitwise XOR |
| BITLSHIFT | 2013 | Left bit shift |
| BITRSHIFT | 2013 | Right bit shift |

### Info / Meta

| Function | Since | What it does |
|----------|-------|-------------|
| FORMULATEXT | 2013 | Returns a cell's formula as a string |
| ISFORMULA | 2013 | True if the referenced cell contains a formula |
| SHEET | 2013 | Sheet number of a reference |
| SHEETS | 2013 | Count of sheets in the workbook |

## Out of scope (require spill-range or lambda architecture)

These cannot be implemented as UDFs within POI's current evaluator:

- FILTER, SORT, SORTBY, UNIQUE, SEQUENCE, RANDARRAY (dynamic array spill)
- LAMBDA, MAP, REDUCE, SCAN, MAKEARRAY, BYCOL, BYROW (lambda/closure)
- TEXTSPLIT (returns array)
- VSTACK, HSTACK, WRAPCOLS, WRAPROWS, TOCOL, TOROW (array reshaping)
- CHOOSECOLS, CHOOSEROWS, DROP, TAKE, EXPAND (array slicing)

Path forward for these: LibreOffice headless sidecar (noted as v1.1 direction in the implementation plan).

## Registration pattern

```java
workbook.addToolPack(new DefaultUDFFinder(
    new String[] { "IFS", "SWITCH", "TEXTJOIN", ... },
    new FreeRefFunction[] { new IfsFn(), new SwitchFn(), new TextJoinFn(), ... }
));
```

## Estimated total effort

Priorities 1-4 (12 functions): ~600-800 lines of Java.
Priority 5 (13 functions): ~400 lines of Java.
Total: ~25 functions in ~1000-1200 lines.
