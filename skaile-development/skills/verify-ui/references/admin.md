# Admin Verification

## SMOKE (Quick Pass)

1. Navigate to {base_url}/admin/session-manager
2. Snapshot -> verify session management UI renders
3. DONE

## FULL Verification

### Session Manager

1. Navigate to {base_url}/admin/session-manager
2. Wait for networkidle, snapshot -i
3. Verify:
   - Session list/table structure
   - Columns: session name/ID, project, status, last activity
   - IF sessions exist: rows with session data
   - IF no sessions: empty state
   - Refresh/reload action available

### Data Management

1. Navigate to {base_url}/admin/data-management
2. Wait for networkidle, snapshot -i
3. Verify:
   - Model/entity list or navigation
   - CRUD interface structure (list view with create/edit/delete actions)
   - IF models listed: verify at least a few model names are visible (Project, Session, User, Organization)

### Admin Visibility

In noAuth dev mode, the mock user typically has PlatformAdmin role, so admin pages should be accessible. Verify:

1. Navigate to dashboard
2. Snapshot -> check sidebar has Platform Admin navigation group
3. IF admin nav group missing: report WARN "Admin nav not visible -- may need PlatformAdmin role"

### Platform Overview

1. Navigate to {base_url}/platform
2. Wait for networkidle, snapshot -i
3. Verify:
   - Platform overview page renders
   - System information or status displayed

### Notes

- Admin pages require PlatformAdmin role in auth mode
- In noAuth mode, all pages should be accessible
- Data management CRUD should be verified as read-only (view only, do NOT create/edit/delete records)
