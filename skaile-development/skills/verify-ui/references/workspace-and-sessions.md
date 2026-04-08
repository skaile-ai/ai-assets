# Workspace & Sessions Verification

## Prerequisites

This verification requires at least one project to exist. If no projects exist:
1. Navigate to {base_url}/projects
2. Snapshot -> check if any project entries are listed
3. IF empty: report SKIP "No projects exist -- create a project first to verify workspace"
4. IF projects exist: note the first project's link/ID for use below

## SMOKE (Quick Pass)

1. Navigate to {base_url}/projects
2. Find first project link, click it
3. Verify workspace page loads with recognizable layout (chat area, sidebar)
4. DONE

## FULL Verification

### Project Entry Point

1. Navigate to {base_url}/projects
2. Snapshot -i
3. Click first project in list
4. Wait for networkidle
5. Check URL -- should be /projects/{projectId}/main or /projects/{projectId}/{sessionId}
6. Snapshot -i

### Workspace Layout

From the session/workspace page, verify these structural elements:

1. **Chat Panel**:
   - Message input area (textarea or contenteditable)
   - Send button
   - Message list area (may be empty for new session)
   - IF messages exist: verify they render with sender and content

2. **Pipeline Navigator** (structured sessions):
   - Phase list or progress indicator
   - Current phase highlighted/active
   - Phase names visible
   - IF no pipeline: this may be an open session -- verify chat-only layout instead

3. **Resource Explorer / File Browser**:
   - File tree or file list sidebar
   - IF files exist: expandable folders, file names
   - IF empty: empty state message

4. **Artifact Panel**:
   - Code/content viewer area
   - IF artifacts exist: file content displayed
   - IF empty: placeholder or instructions

5. **Session Controls**:
   - Session status indicator
   - Session tabs bar (if multiple sessions)

### Chat Interaction Structure

Verify the chat input accepts text (do NOT send to agent):

1. Find the message input area
2. Type "test" into it (use fill or type command)
3. Verify the send button becomes enabled
4. Clear the input (fill with empty string)
5. Do NOT press send -- this would trigger an agent interaction

### Session Status

1. Look for status indicators in the workspace
2. Verify session status is visible (Running, Waiting, etc.)
3. IF session shows Error state: report as WARN with details

### Iteration Flow (if accessible)

1. Look for iteration-related UI elements (describe change, analyze, execute)
2. Verify the iteration panel/dialog structure exists
3. Do NOT start an actual iteration

### Open Session Mode

1. IF the project has an "Open Session" or free-form option:
   - Verify it shows a simpler chat-only layout
   - Verify message input is present
   - Verify no pipeline navigator

### Notes

- This area is the most complex and dynamic part of the platform
- Many elements depend on session state and backend connectivity
- Report what IS present rather than failing on what's missing
- If backend is not running, workspace will likely show loading or error state -- report this
