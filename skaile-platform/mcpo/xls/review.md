# Excel MCP Review

## Peter review

Overall: It works, awesome! Manage to get an agent to open workbook, extract data, modify inputs and formulas, recalc, retrieve results and store! So basic loop is working!

- We quickly need to teach the Agent Excel best practices. Not sure if this can be part of the MCP or rather belong into an accompanying skill. Probably the latter.
  Thinking of these "rules":
  - Never mix hardcoded values and formulas: hard coded values are inputs and/or assumptions and should be stated clearly. Formulas are (business) logic and should not include hidden inputs. (Exception: unit conversions, e.g. `/365`, `*100`, ...)
  - Percentages should be represented as 0-1, but formatted in "%" format
  - Do not merge cells
  - Do not hide rows/cols, instead use grouping and collapse

- Reviewed "excel-mcp-server-future-work.md". Let's address this in the following order:
  1. Cell formatting read/writes
  2. ListObjects/Excel tables - including table formula syntax support
  3. Copy operations
  4. Merge/unmerge cells
  5. Sheet protections
  6. Page setup
  7. pivot tables
  8. Charts
  9. Data validation rules
  10. External links
  11. Hyperlinks, comments, ...
  12. Implement missing "simple" functions as UDFs - see ./udfs-to-implement.md
- Probably a prompt/skill thing: right now, the agent loses the excel workbook at the end of each "turn" - instead of keeping it open until further notice. this is fine for small models, but probably not ideal for larger workbooks or longer sessions.

- As discussed briefly in chat, we should add deterministic chat like in xlport that take template + list of changes, produce the outcome and compare it with a expected result. See xlport for the "comparing function"
- Outside of the the above "core functionality", we should build the "extract the gist of the model as LLM text and store it somewhere" - detailed briefing in person (either Johan or Peter)
- Johan to compile list of Excel models from data2impact history and prepare evals (Chris is looking into Langfuse)

## Agent review (using `/review` skill)

Important [!]

1. Sandbox is opt-in - agent can read/write any file when EXCEL_MCP_ROOT unset
   ExcelMcpRoot.java:32, PathValidator.java:71-73  
   isEnabled() returns false when the env var is absent, making assertInsideRoot a no-op. The agent gets full filesystem access  
   by default. Should fail closed: require the env var or refuse to start.  

2. Internal paths leaked in error responses  
   PathValidator.java:78-79, FormatWhitelist.java:36-37, PoiAtomicSaver.java:47-48
   Canonicalized absolute paths (including the EXCEL_MCP_ROOT value) are returned in details maps visible to the agent. Reveals  
   server directory layout.  

3. Silent atomic-save fallback degrades crash safety  
   PoiAtomicSaver.java:33-37  
   Cross-filesystem mounts (common with Docker volumes) silently fall back from ATOMIC_MOVE to REPLACE_EXISTING. A crash  
   mid-rename loses the original file. Should at minimum log a warning.  

4. sheet.delete has no last-sheet guard  
   SheetDeleteTool.java:60-65, XssfInMemoryEngine.java  
   POI silently removes the last sheet, producing an unopenable workbook. No check in either the tool or the engine.  

5. RangeAddress does not parse sheet-qualified references (Sheet1!A1:B2)  
   RangeAddress.java:15-59  
   Standard Excel notation with a sheet prefix throws RANGE_INVALID. Full-column (A:A) and full-row (1:1) references also fail.  

6. requireString reports PATH_INVALID for all missing/non-string fields  
   ToolInputs.java:21-24  
   Fields like sheet, name, old_name report PATH_INVALID on validation failure. Misleads the agent's error recovery.  

7. range.set does not validate that formulas shape matches values shape  
   RangeSetTool.java:121-125, XssfInMemoryEngine.java:408  
   Mismatched dimensions silently truncate or ignore formula rows. Agent gets no error, data is silently lost.  

8. Required integer params silently default to 0  
   SheetInsertRowsTool.java:78, SheetDeleteRowsTool.java:75, SheetInsertColsTool.java:79, SheetDeleteColsTool.java:75  
   intOrDefault falls back to 0 for wrong-type values instead of throwing. The engine then rejects 0 with a confusing "must be >=
   1" error.  

9. Date timezone semantics undocumented  
   PoiCellReader.java:89-93  
   Dates are formatted via ZoneOffset.UTC but Excel date serials are timezone-naive. Callers may incorrectly interpret the ISO  
   string as a UTC instant.  

10. README: EXCEL_MCP_MAX_FILE_BYTES default wrong  
    README.md env table says 100000000 (95.4 MB); ServerConfig.java has 100L _ 1024L _ 1024L = 104857600 (100 MB).
11. No unit tests for HandleRegistry, ServerConfig.fromEnvironment(), PoiSizeGuard, PoiAtomicSaver  
    Security-adjacent code paths with zero test coverage.  


---

Nit [~]

1. RuntimeException messages echoed to agent  
   ToolRegistry.java:84-85 - Unexpected exceptions serialize ex.getMessage() (may contain class names, paths) into the response.
2. poi-scratchpad dependency unused  
   pom.xml:64-67 - Only needed for .doc/.ppt. Adds ~2 MB to the fat jar.  

3. HandleRegistry synchronized but engine HashMap is not  
   HandleRegistry.java:20 vs XssfInMemoryEngine.java - Contradicts the "single-threaded by design" comment.  

4. VBA reads from stale on-disk file, not in-memory state  
   XssfInMemoryEngine.java:643,648 - Documented limitation but caller gets no staleness indicator.  

5. Smoke test helpers copy-pasted across all 10 scripts  
   ~60 lines of send/recv/call duplicated. Phase 3's recv lacks the stderr fallback added in later scripts.  


Plus 6 similar nits not shown.

---

Pre-existing [*]

1. workbook.open swallows FileNotFoundException as INTERNAL_ERROR
   XssfInMemoryEngine.java:88-93 - The FILE_NOT_FOUND error code exists but never fires on open.  

2. log4j-to-slf4j version hardcoded inline  
   pom.xml:96 - All other versions use <properties> block.

### Assessment Agent Review by Peter
