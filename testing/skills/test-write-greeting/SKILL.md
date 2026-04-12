---
name: Test - Write Greeting
description: Read the name from test-ask-name and write a fixed-format greeting file. Test fixture - not for production.
source: TEST
version: 1.0.0
keywords: [test, fixture, artifact-handoff]
user_inputs: []
reads_from:
  - workspace/test/name.txt
writes_to:
  - workspace/test/greeting.txt
---

# Test: Write Greeting

Deterministic test skill. Reads the name produced by `test-ask-name` and writes a greeting file with exactly specified content. Follow instructions literally.

## Instructions

1. Call `build_handoff` for your node. Verify that the artifact ref `workspace://test/name.txt` appears in the upstream artifacts list. If it does not, call `fail_node` with `message: "upstream did not produce name.txt"` and `recoverable: false`. Stop.

2. Read the file `workspace/test/name.txt`. The content is a single line of text with no trailing newline. Assign it to `<name>`.

3. Ensure `workspace/test/` exists.

4. Write `workspace/test/greeting.txt` with EXACTLY this content:

   ```
   Hello, <name>!
   ```

   Substitute `<name>` with the literal content read from name.txt. Exactly one space after the comma. An exclamation mark after the name. NO trailing newline. NO leading whitespace. NO extra punctuation.

5. Call `request_approval` for your node with:
   - `summary`: `` "Wrote greeting for `<name>` to workspace/test/greeting.txt." ``
   - `artifacts`:
     ```json
     [
       {
         "uri": "workspace://test/greeting.txt",
         "connectorId": "workspace",
         "kind": "document",
         "lifetime": "session",
         "producedBy": "write-greeting"
       }
     ]
     ```

   This ends your turn.

6. On `approved`, call `complete_node` with the same `summary` and `artifacts`.

7. On `rejected`, call `fail_node` with `recoverable: true` and `message: <the rejection feedback>`. Do not attempt to rewrite the greeting autonomously - the retry path will come back through the runner.

## Determinism requirements

- Greeting format is `Hello, <name>!` - no variation.
- No chat text output. Tool calls only.
- Do not call tools other than `build_handoff`, `request_approval`, `complete_node`, and `fail_node`.
- Do not read or write any files other than `workspace/test/name.txt` (read) and `workspace/test/greeting.txt` (write).
