# Settings Verification

## SMOKE (Quick Pass)

1. Navigate to {base_url}/settings/organization
2. Snapshot -> verify a settings form renders
3. Navigate to {base_url}/settings/ai-providers
4. Snapshot -> verify provider configuration area renders
5. DONE

## FULL Verification

### Organization Settings

1. Navigate to {base_url}/settings/organization
2. Wait for networkidle, snapshot -i
3. Verify:
   - Organization name field
   - Save/Update button
   - Settings form structure

### User Management

1. Navigate to {base_url}/settings/users
2. Wait for networkidle, snapshot -i
3. Verify:
   - Users table or list
   - Add/Invite user button
   - IF users exist: table rows with name, email, role columns
   - IF empty: empty state message

### AI Provider Configuration

1. Navigate to {base_url}/settings/ai-providers
2. Wait for networkidle, snapshot -i
3. Verify:
   - Provider list or configuration area
   - Add provider button
   - IF providers configured: provider entries with name, type, status
   - IF empty: setup prompt or empty state

### Deployment Targets

1. Navigate to {base_url}/settings/deployment-targets
2. Wait for networkidle, snapshot -i
3. Verify:
   - Targets list or table
   - Add target button
   - IF targets exist: entries with name, type columns
   - IF empty: empty state

### Git Providers

1. Navigate to {base_url}/settings/git-providers
2. Wait for networkidle, snapshot -i
3. Verify:
   - Provider list (GitHub, GitLab, Bitbucket options)
   - Connect/Add button
   - IF providers connected: entries with provider type, status
   - IF empty: setup instructions or connect prompt

### SharePoint Providers

1. Navigate to {base_url}/settings/sharepoint-providers
2. Wait for networkidle, snapshot -i
3. Verify:
   - Provider configuration area
   - Connect/Add button
   - IF connected: tenant info, status
   - IF empty: setup instructions

### Skill Templates

1. Navigate to {base_url}/settings/skill-templates
2. Wait for networkidle, snapshot -i
3. Verify:
   - Template list or grid
   - Create/Add template button
   - IF templates exist: entries with name, description
   - IF empty: empty state

### Cross-Page Consistency

After verifying each settings page individually:
1. Verify navigation between settings pages works (sidebar links under Settings group)
2. Verify each page has consistent header/breadcrumb structure
3. Verify back navigation returns to previous settings page or dashboard
