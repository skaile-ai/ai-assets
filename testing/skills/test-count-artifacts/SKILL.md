---
name: test-count-artifacts
description: Count files in workspace/test/ and write the count to a file. Optional node, used to exercise the skip path. Test fixture - not for production.
source: TEST
version: 1.0.0
keywords: [test, fixture, optional-skip]
user_inputs: []
reads_from:
  - workspace/test/
writes_to:
  - workspace/test/count.txt
---

# Test: Count Artifacts

Deterministic test skill. Counts files in `workspace/test/` and writes the count. This node is marked optional in `test-echo.flow.yaml` so tests can exercise the skip path.

## Instructions

1. List the files directly under `workspace/test/`. Rules:
   - Do NOT recurse into subdirectories.
   - Do NOT include directories in the count.
   - Do NOT include hidden files (names starting with `.`).
   - Include regular files only.

2. Count the files. Call the result `N`. `N` is a non-negative integer.

3. Write `workspace/test/count.txt` with EXACTLY this content:

   ```
   <N>
   ```

   Where `<N>` is the decimal representation of N with no leading zeros. Example: if there are 3 files (including name.txt and greeting.txt but before count.txt itself is written), write exactly `3`. No trailing newline. No whitespace. No formatting.

   NOTE: count.txt itself must NOT be counted, because you count before writing it.

4. Call `request_approval` for your node with:
   - `summary`: `"Counted N=<value> files in workspace/test/ and wrote workspace/test/count.txt."`
   - `artifacts`:
     ```json
     [
       {
         "uri": "workspace://test/count.txt",
         "connectorId": "workspace",
         "kind": "data",
         "lifetime": "session",
         "producedBy": "count-artifacts"
       }
     ]
     ```

   This ends your turn.

5. On `approved`, call `complete_node` with the same `summary` and `artifacts`.

6. On `rejected`, call `fail_node` with `recoverable: false`. A file count is not a subjective artifact - if it is wrong, the fault is in execution, not refinement. Message: "count cannot be refined".

## Determinism requirements

- Count reflects the file list at the moment of execution, excluding count.txt itself.
- Output is the integer as ASCII digits - nothing else in the file.
- No chat text output. Tool calls only.
- Do not call tools other than `request_approval`, `complete_node`, and `fail_node`.
