---
name: strava
description: "Use when an agent needs the connected athlete's own Strava data -
  list recent or historical activities, inspect a single activity's details or
  time-series streams, read athlete profile and season stats, explore segments and
  segment efforts, or export a saved route as GPX/TCX. Runs as a local stdio server
  against Strava's REST API (api/v3) using credentials the user supplies, so every
  call is scoped to that one athlete's account. Reach for this over raw HTTP when
  the task spans several Strava reads or needs typed tools rather than hand-rolled
  API calls. Personal-scope only: Strava's API terms allow an athlete's data to be
  shown to that athlete alone, so this asset must never be assigned at project or
  organization scope."
version: 0.1.0
transport: stdio
command: npx
args:
  - "-y"
  - "@r-huijts/strava-mcp-server@1.2.1"
env:
  ROUTE_EXPORT_PATH: "${workspace}/strava-routes"
# Configure-form fields (platform schema widget). Keys are the env var names;
# values fold into the server env via the instance config / secret path. All four
# come from one Strava OAuth exchange against the user's own API application.
fields:
  - key: STRAVA_CLIENT_ID
    label: Strava API client ID
    type: text
    required: true
  - key: STRAVA_CLIENT_SECRET
    label: Strava API client secret
    type: password
    required: true
    sensitive: true
  - key: STRAVA_ACCESS_TOKEN
    label: Access token (short-lived; refreshed automatically)
    type: password
    required: true
    sensitive: true
  - key: STRAVA_REFRESH_TOKEN
    label: Refresh token (long-lived)
    type: password
    required: true
    sensitive: true
keywords:
  - strava
  - fitness
  - cycling
  - running
  - activities
  - segments
  - training
  - mcp
---

# Strava MCP Server (local)

A stdio MCP server wrapping Strava's REST API (`www.strava.com/api/v3`). It runs
inside the session container and talks to Strava directly as the athlete whose
credentials are configured on the instance.

This is deliberately **not** Strava's hosted MCP endpoint (`mcp.strava.com`). That
endpoint refuses connections from several hosting providers' IP ranges, including
the one this platform deploys on, while the REST API it wraps is reachable
normally. Using the REST API sidesteps the block entirely and needs no proxy.

## When to reach for this

- The user asks about **their own** activities - "what did I ride last week",
  "how far have I run this year", "compare these two efforts".
- A single activity needs **detail or streams** - splits, heart rate, power,
  cadence, altitude series for analysis or charting.
- **Segments**: explore segments in an area, read a segment or a specific effort,
  list or star favourites.
- A saved **route** needs exporting as GPX or TCX.

## Capabilities

25 tools over stdio, all scoped to the connected athlete:

- **Activities** - `get-recent-activities`, `get-all-activities`,
  `get-activity-details`, `get-activity-laps`, `get-activity-streams`,
  `get-activity-photos`.
- **Athlete** - `get-athlete-profile`, `get-athlete-stats`, `get-athlete-zones`,
  `list-athlete-clubs`.
- **Segments** - `explore-segments`, `get-segment`, `get-segment-effort`,
  `list-segment-efforts`, `list-starred-segments`, `star-segment`.
- **Routes** - `list-athlete-routes`, `get-route`, `export-route-gpx`,
  `export-route-tcx`, `format-workout-file`.
- **Session/meta** - `check-strava-connection`, `get-server-version`,
  `connect-strava`, `disconnect-strava` (the last two are not the intended auth
  path here - see Setup).

## Scope: personal only

Strava's API Agreement states that data provided by a specific user may only be
displayed or disclosed to that same user. Assign this asset at **personal/user
scope**. Do not assign it at project or organization scope: a shared session would
expose one athlete's data to everyone in it, which is exactly what that clause
forbids.

## Setup

Each organization uses **its own** Strava API application - rate limits and the
compliance relationship are per-application, so a shared one does not work.

1. Register an application at <https://www.strava.com/settings/api>. Note the
   **client ID** and **client secret**.
2. Complete an OAuth exchange for the athlete's account, requesting at least the
   `activity:read_all` and `profile:read_all` scopes. Keep the resulting **access
   token** and **refresh token**.
3. Configure the installed asset with those four values and restart the session.

The server also ships `connect-strava` / `disconnect-strava` tools that drive a
browser-based OAuth flow. Those are **not** the path here - they assume an
interactive desktop and write credentials to a file that does not survive a
container restart. Supply the tokens as configuration instead; environment values
take priority over any stored file.

## Delivery

This is the domain's first `command: npx` entry. Unlike the release-asset shape
(`sql/`), which pins a `payload.sha256` the platform verifies at materialization,
`npx` re-resolves the package from the npm registry on every session start and
there is **no integrity check**. The version is therefore pinned exactly
(`@r-huijts/strava-mcp-server@1.2.1`) and acts as the closest available
substitute: it stops a new upstream release from silently entering customer
containers, but it does not protect against a republished tag or a compromised
registry account. That is a deliberate, accepted tradeoff for a third-party npm
package with no release-asset bundle - not an oversight. Bump `version` and the
pin in `args` together.

## Known ceiling

The server writes refreshed tokens to `~/.config/strava-mcp/config.json` inside
the container, which is discarded when the container restarts. Configuration wins
on every start, so it simply re-refreshes from the stored refresh token - fine,
because Strava normally returns the same refresh token. If Strava ever rotates it,
the configured value goes stale and has to be re-entered.

Route export (`export-route-gpx`, `export-route-tcx`) writes into
`strava-routes/` in the workspace.
