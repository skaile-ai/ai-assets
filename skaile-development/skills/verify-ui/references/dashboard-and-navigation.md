# Dashboard & Navigation Verification

## SMOKE (Quick Pass)

1. Navigate to {base_url}/dashboard
2. Snapshot -> verify page has a heading or content area
3. Verify sidebar navigation is present (look for nav element with links)
4. Click "Projects" in sidebar -> verify URL changes to /projects
5. DONE

## FULL Verification

### Dashboard Page

1. Navigate to {base_url}/dashboard
2. Wait for networkidle
3. Snapshot -i
4. Verify:
   - Page heading or welcome text is present
   - "Recent Projects" section or empty state message
   - "Deployed Apps" section or empty state
   - Sidebar navigation is visible

### Sidebar Navigation

The sidebar should contain links organized in groups. Verify each link navigates correctly:

**Business Navigation:**

| Link Text | Expected URL | Expected Page Content |
|-----------|-------------|----------------------|
| Dashboard | /dashboard | Dashboard heading |
| Projects | /projects | Projects list or empty state |
| Deployed Apps | /apps | Apps list or empty state |

**IT Admin Navigation (Settings):**

| Link Text | Expected URL | Expected Page Content |
|-----------|-------------|----------------------|
| Organization | /settings/organization | Organization settings form |
| Users | /settings/users | Users table |
| Git Providers | /settings/git-providers | Git provider list |
| SharePoint | /settings/sharepoint-providers | SharePoint config |
| AI Providers | /settings/ai-providers | AI provider config |
| Deployment Targets | /settings/deployment-targets | Targets list |
| Skill Templates | /settings/skill-templates | Templates list |

**Platform Admin Navigation:**

| Link Text | Expected URL | Expected Page Content |
|-----------|-------------|----------------------|
| Platform | /platform | Platform overview |
| Session Manager | /admin/session-manager | Session list |

### Navigation Test Procedure

FOR EACH row in the tables above:
1. Start from dashboard (navigate to {base_url}/dashboard)
2. Snapshot -i
3. Find the link by its text label in the snapshot
4. Click it
5. Wait for networkidle
6. Verify URL contains the expected path
7. Snapshot -> verify some content rendered (heading, table, or form)
8. Report: PASS if page loads with content, WARN if empty state, FAIL if error or blank

### Header

1. From any authenticated page, snapshot
2. Verify: page title/breadcrumb area exists
3. Verify: user menu or avatar is present (top-right area)
