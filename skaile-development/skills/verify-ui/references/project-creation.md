# Project Creation Verification

## SMOKE (Quick Pass)

1. Navigate to {base_url}/projects/new
2. Snapshot -> verify source type selection is present (Git, SharePoint, LocalFolder, Empty)
3. Click "Empty" source type
4. Verify project name input appears
5. DONE

## FULL Verification

### Projects List Page

1. Navigate to {base_url}/projects
2. Snapshot -i
3. Verify:
   - Page heading ("Projects" or similar)
   - Create/New project button or link exists
   - IF projects exist: list or grid of project cards
   - IF no projects: empty state with prompt to create

### Project Creation Wizard

1. Navigate to {base_url}/projects/new
2. Wait for networkidle
3. Snapshot -i
4. Verify source type selector shows options:
   - "Git" or "Git Repository"
   - "SharePoint"
   - "Local Folder" or "LocalFolder"
   - "Empty"

### Source Type: Empty

1. Select "Empty" source type (click it)
2. Wait 500ms, re-snapshot
3. Verify form shows:
   - Project name input field
   - Description input field (optional)
   - Create/Submit button
4. Fill project name with "Test Project Verification" (do NOT submit)
5. Verify the create button is enabled/clickable after filling name

### Source Type: Git

1. Navigate to {base_url}/projects/new (fresh start)
2. Snapshot, select "Git" source type
3. Wait 500ms, re-snapshot
4. Verify form shows:
   - Repository mode selector (New / Existing)
   - Git provider dropdown (may be empty if no providers configured)
   - IF "Existing" mode: repository URL input, branch selector
   - IF "New" mode: repository name input
   - Project name input
5. IF no git providers configured: verify helpful message or empty dropdown (this is expected, not a failure)

### Source Type: SharePoint

1. Navigate to {base_url}/projects/new (fresh start)
2. Snapshot, select "SharePoint" source type
3. Wait 500ms, re-snapshot
4. Verify form shows:
   - SharePoint provider dropdown
   - IF provider selected: site/drive/folder browser
   - Project name input
5. IF no SharePoint providers configured: verify helpful message (expected, not a failure)

### Source Type: Local Folder

1. Navigate to {base_url}/projects/new (fresh start)
2. Snapshot, select "Local Folder" / "LocalFolder" source type
3. Wait 500ms, re-snapshot
4. Verify form shows:
   - Folder path input or folder browser
   - Project name input

### Form Validation

1. On any source type form, clear the project name field
2. Try to click the create button
3. Verify: button is disabled OR validation error message appears
4. This confirms client-side validation is working

### Notes

- Do NOT actually create a project during verification (avoid side effects)
- SharePoint and Git forms depend on configured providers -- empty states are valid
- The wizard may use steps/tabs -- navigate through each step
