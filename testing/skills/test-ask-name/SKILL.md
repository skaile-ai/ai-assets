---
name: Test - Ask Name
description: Request a user name via input gate, then write it verbatim to workspace/test/name.txt. Test fixture - not for production.
source: TEST
version: 1.0.0
keywords: [test, fixture, input-gate]
user_inputs: []
reads_from: []
writes_to:
  - workspace/test/name.txt
---

# Test: Ask Name

This is a deterministic test skill. Its only purpose is to exercise the flow execution engine's input gate, approval gate, and artifact production. Follow these instructions exactly. Do not improvise.

## Instructions

1. Call the flow tool `request_input` for your node with these exact parameters:
   - `prompt`: `"What name should we greet?"`
   - `schema`: `{ "kind": "text", "multiline": false }`

   This ends your turn. Stop generating after the tool call completes. Wait for the next turn.

2. On the next turn, the turn stimulus is `input_received` and the response is a string. Take the string verbatim - do not trim, capitalize, or otherwise modify it.

3. Ensure the directory `workspace/test/` exists. Create it if missing.

4. Write `workspace/test/name.txt` with the response string as the entire file content. No trailing newline. No quoting. No formatting.

5. Call the flow tool `request_approval` for your node with:
   - `summary`: `` "Captured name `<name>`. Wrote workspace/test/name.txt." `` (with the actual name substituted in the backticks)
   - `artifacts`:
     ```json
     [
       {
         "uri": "workspace://test/name.txt",
         "connectorId": "workspace",
         "kind": "data",
         "lifetime": "session",
         "producedBy": "ask-name"
       }
     ]
     ```

   This ends your turn.

6. When the turn stimulus is `approval_received` with `decision: "approved"`, call `complete_node` with the same `summary` and `artifacts` as in step 5. Do not modify them.

7. When the turn stimulus is `approval_received` with `decision: "rejected"`, call `request_input` again with a new prompt: `"Please provide a different name."` Proceed as in steps 2-6 with the new input, overwriting `workspace/test/name.txt`.

## Determinism requirements

- File content is EXACTLY the input string. Nothing else.
- Do not emit any chat text. All output is tool calls only.
- Do not call tools other than `request_input`, `request_approval`, and `complete_node`.
- Do not read other files.
