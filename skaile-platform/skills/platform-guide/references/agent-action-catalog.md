# Agent Action Catalog — `platform.act` / `platform.act_batch`

> Reference for `platform-guide` (see `concepts/agent.md`). Lists every
> state-changing action you can take on the user's behalf via `platform.act` —
> scope, action type, payload parameters, and the role the user must have. Load
> on demand, when constructing an action; do not read it end-to-end for a
> guiding answer.
>
> Generated from the platform data model (`platform/schema/*.model.json`) and
> authorization policy as of the last release; the runtime auth check against the
> approving human is the real gate. If a scope, field, or role here is rejected,
> the model moved — re-check.

## How acting on behalf of the user works

Two approval-gated effect capabilities let you mutate platform state as the user:

- **`platform.act`** — one action: `{ scope, type, payload, rationale }`.
- **`platform.act_batch`** — an ordered list of steps, approved **once** and run
  sequentially. A later step can reuse an earlier step's output with a
  `{ "$ref": [stepIndex, "field"] }` placeholder anywhere in its payload (e.g.
  create a project in step 0, then `{ "$ref": [0, "id"] }` to put a session in
  it in step 1).

**Approval & authorization.** Every call requires approval: the user sees a card
with the action(s) and your `rationale`, and nothing runs until they approve. The
dispatch then executes **under the approving human, with their roles** — you can
only do what that user is allowed to do. An unauthorized action returns an
authorization error, not a silent success. **Always pass a clear, specific
`rationale`** — it is how the human decides.

**Prefer a dedicated capability** when one exists (`platform.invite_user`,
`platform.enable_asset`, `platform.set_voice`, `platform.schedule_action`, ...).
Use `platform.act` only when none fits.

**Long-running actions don't work inline.** A single dispatch is bounded to ~30s.
Actions that provision a container — `session.create` / `session.fork` /
`session.reopen` and `project.create` / `project.setupProject` — exceed that and
return a timeout. Direct the user to start those from the app UI; use `act` for
the lighter create/update/delete operations below.

## Action grammar

An action is `{ scope, type, payload }`. `scope` is an entity (the camelCase
model name, e.g. `project`, `session`, `projectMember`). `type` is one of the
**standard CRUD action types** (available on every scope) or a scope-specific
**custom action**.

### Standard action types (every scope)

| Type | Payload shape | Effect |
|------|---------------|--------|
| `create` | `{ ...fields }` (`id` optional, auto-assigned) | Create one record. |
| `createMany` | `[ { ...fields }, ... ]` | Create several. |
| `update` | `{ id, ...partialFields }` | Patch the listed fields of one record. |
| `updateMany` | `[ { id, ...partialFields }, ... ]` | Patch several. |
| `updateField` | `{ id, field, value }` | Set a single field (value typed per field). |
| `updateFieldMany` | `{ ids: [...], field, value }` | Set one field on many records. |
| `upsert` | `{ ...fields }` | Create or update by key. |
| `upsertMany` | `[ { ...fields }, ... ]` | Upsert several. |
| `delete` | `{ id }` | Delete one record. |
| `deleteMany` | `{ ids: [...] }` | Delete several. |

Field types below: `string`, `int`, `float`, `boolean`, `datetime` (ISO 8601),
`json`, `enum` (one of a fixed set), `id` (a foreign-key reference — pass the
target record's id). "Req = yes" means required on `create`; on `update` all
fields are optional except `id`.

> Not every scope is meant for direct mutation — many Runtime/Config scopes
> (containers, compactions, usage records, oauth flow state) are system-managed
> and a non-admin approver will be denied. They are listed for completeness; the
> runtime auth check is the real gate.

## Authorization — which role the user needs

The action runs as the **approving human**, and their **organization role**
(`Owner`, `User`, `Viewer`, or the cross-org `PlatformAdmin`) is what's checked.
Each scope below lists **Role for create / update / delete** — the roles allowed
to run a standard CRUD action on that entity via `platform.act`. Custom actions
carry their own roles inline (e.g. `session.fork` needs `Owner`).

How it resolves (so the listed role is accurate):

- A **standard CRUD** action type checks the entity's effective **write** role —
  the scope's own override, else its schema-group default (the **Workspace**
  group: User/Owner/PlatformAdmin; the **Config**, **Runtime**, and **Platform**
  groups: Owner/PlatformAdmin).
- A **custom** action type checks that action's specific rule.
- Some scopes add a **row-level** gate on top: a non-Owner may only write their
  **own** row (their dashboard pins, notification prefs, own credentials...).
- A few entities note "app UI is stricter" — the product's own screens restrict,
  say, project `delete` to Owner even though the generic write role is broader;
  row-level ownership enforces the same intent for `platform.act`.

If an action returns an authorization error, the approver lacks the role — tell
them what role is required rather than retrying.

## Scope catalog

### Workspace — projects, sessions, messages, collaboration

#### `a2AExchange`

**Role for create / update / delete:** PlatformAdmin
- Normally written by the platform system user; direct agent CRUD is effectively admin-only.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `originSessionId` | id | yes | references Session. Origin Session |
| `path` | json | yes | Ordered session IDs traversed. |
| `hopCount` | int | no | default 0. Hop Count |
| `mode` | enum | yes | one of: ask | send. Mode |
| `status` | enum | no | one of: open | answered | timed_out | closed; default open. Status |

#### `compaction`

**Role for create / update / delete:** User, Owner, PlatformAdmin
- Normally written by the platform system user; direct agent CRUD is effectively admin-only.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `sessionId` | id | yes | references Session. Session |
| `conversationId` | id | yes | references Conversation. Conversation |
| `parentCompactionId` | id | no | references Compaction. Parent Compaction |
| `status` | enum | yes | one of: Success | Failed | Partial | Dirty | Deleted. Status |
| `errorCode` | string | no | Error Code |
| `trigger` | string | yes | Trigger |
| `summary` | string | yes | Summary |
| `model` | string | no | Model |
| `provider` | string | no | Provider |
| `inputTokens` | int | yes | Input Tokens |
| `outputTokens` | int | yes | Output Tokens |
| `coversFromSeq` | int | yes | Covers From Seq |
| `coversToSeq` | int | yes | Covers To Seq |
| `coversToMessageId` | id | no | references Message. Covers To Message |
| `driverSessionIdAtCompaction` | string | no | Driver Session Id At Compaction |
| `capabilitySignatureAtCompaction` | string | no | Capability Signature At Compaction |
| `modelAtCompaction` | string | no | Model At Compaction |
| `validatedAt` | datetime | no | Validated At |
| `deletedAt` | datetime | no | Deleted At |

#### `conversation`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `title` | string | yes | Title |
| `sessionId` | id | yes | references Session. Session |

#### `excelExtraction`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `sessionId` | id | yes | references Session. Session |
| `fileId` | id | no | references File. Uploaded File |
| `fileName` | string | yes | File Name |
| `status` | enum | yes | one of: Uploading | Extracting | Reviewing | Accepted | Failed. Status |
| `extractedSchema` | json | no | Extracted Schema |
| `acceptedElements` | json | no | Accepted Elements |

#### `featureTask`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `sessionId` | id | yes | references Session. Session |
| `featureName` | string | yes | Feature Name |
| `featureGroup` | string | yes | Feature Group |
| `status` | enum | yes | one of: Pending | InProgress | Testing | Complete | Failed. Status |
| `startedAt` | datetime | no | Started At |
| `completedAt` | datetime | no | Completed At |
| `testResults` | json | no | Test Results |

#### `iteration`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `sessionId` | id | yes | references Session. Session |
| `requestedById` | id | no | references User. Requested By |
| `description` | string | yes | Description |
| `status` | enum | yes | one of: Requested | InProgress | Complete. Status |

#### `message`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `conversationId` | id | yes | references Conversation. The conversation this message belongs to. |
| `seq` | int | yes | Monotonic sequence number within the session. Used for ordering and pagination. |
| `senderId` | id | no | references User. The user that sent this message (present for user commands, absent for agent events). |
| `payload` | json | yes | The message payload — an AgentCommand or AgentEvent from @skaile/agent-types. Check payload.type for the variant. |
| `parentMessageId` | id | no | references Message. If set, this message is a reply to the referenced message. |
| `visibilityMode` | enum | yes | one of: Public | Private | HumansOnly. Routing mode for the message envelope. Public = visible to all session members + agent (default). Private = visible only to sender + suffix-marked @x_ recipients. HumansOnly = visible to all human session members, agent excluded. |
| `a2aExchangeId` | id | no | references A2AExchange. A2A Exchange |
| `peerSessionId` | id | no | references Session. Peer Session |
| `a2aDirection` | string | no | Set only on A2A messages. Values: inbound, outbound. |

#### `messagePrivateRecipient`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `messageId` | id | yes | references Message. The private message this recipient row belongs to. |
| `userId` | string | yes | Recipient user id, or the literal '__agent__' sentinel when the agent is a private recipient (via @agent_). Stored as String, not as a User foreign key, to allow the sentinel. |

#### `messageReaction`

**Role for create / update / delete:** User, Viewer, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `messageId` | id | yes | references Message. The message this reaction is on. |
| `emoji` | string | yes | Unicode emoji character. |
| `userId` | string | yes | ID of the user who reacted. |
| `userName` | string | yes | Display name at reaction time. |

#### `previewShare`

**Role for create / update / delete:** Owner, PlatformAdmin
- Normally written by the platform system user; direct agent CRUD is effectively admin-only.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `tokenHash` | string | yes | Token Hash |
| `shareType` | enum | yes | one of: User | Team | Public. Share Type |
| `accessLevel` | enum | yes | one of: View | Interact. Access Level |
| `sessionId` | id | yes | references Session. Session |
| `projectId` | id | yes | references Project. Project |
| `resourceId` | string | no | Resource |
| `filePath` | string | no | File Path |
| `baseDir` | string | no | Base Dir |
| `label` | string | no | Label |
| `targetUserId` | id | no | references User. Target User |
| `targetTeamId` | id | no | references Team. Target Team |
| `createdById` | id | yes | references User. Created By |
| `expiresAt` | datetime | yes | Expires |
| `revokedAt` | datetime | no | Revoked |
| `revokedById` | id | no | references User. Revoked By |
| `accessCount` | int | yes | Access Count |
| `lastAccessedAt` | datetime | no | Last Accessed |

#### `project`

**Role for create / update / delete:** User, Owner, PlatformAdmin
- Stricter on the app UI: `update` needs Owner, PlatformAdmin; `delete` needs Owner, PlatformAdmin.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `name` | string | yes | Name |
| `slug` | string | yes | Slug |
| `description` | string | no | Description |
| `agentName` | string | no | Agent Name |
| `agentAvatarUrl` | string | no | Agent Avatar URL |
| `voiceId` | string | no | Voice ID |
| `status` | enum | yes | one of: Active | Archived. Status |
| `visibility` | enum | no | one of: Private | Shared; default Private. Visibility |
| `organizationId` | id | yes | references Organization. Organization |
| `ownerId` | id | yes | references User. Owner |
| `mainBranchName` | string | yes | Main Branch Name |
| `mainSessionId` | id | no | references Session. Main Session |
| `sourceType` | enum | yes | one of: Git | SharePoint | GoogleDrive | NextCloud | LocalFolder | Empty | OnSkaile. Source Type |
| `sourceConfig` | json | no | Source Config |
| `skaileConfigId` | id | no | references SkaileConfig. Skaile Config |
| `isAssistant` | boolean | no | default False. True for the per-user personal-assistant project. |

**Custom actions:**

- `markAllSessionsRead` — Clear unread state on all sessions in this project for the calling user. _(roles: User, Owner, Viewer, PlatformAdmin)_
- `archive` — Set project status to Archived and hibernate all Running sessions. _(roles: Owner, PlatformAdmin)_
- `unarchive` — Set project status to Active. Sessions remain hibernated. _(roles: Owner, PlatformAdmin)_

#### `projectMember`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `projectId` | id | yes | references Project. Project |
| `userId` | id | no | references User. User |
| `role` | enum | yes | one of: Owner | User | Viewer. Role |
| `invitedById` | id | no | references User. Invited By |
| `status` | enum | yes | one of: Active | Invited | Expired | Revoked. Status |
| `invitedEmail` | string | no | Invited Email |
| `inviteTokenHash` | string | no | Invite Token Hash |
| `inviteExpiresAt` | datetime | no | Invite Expires |
| `inviteAcceptedAt` | datetime | no | Invite Accepted |
| `inviteDeclinedAt` | datetime | no | Invite Declined |
| `inviteContext` | string | no | Headstart note from the inviter about the invitee (e.g. 'CEO of XYZ'). Stashed on the invite and copied to User.invite_context on accept, then fed to the invitee's assistant onboarding. |
| `cascadeOrgRole` | string | no | Optional org role the inviter chose for the invitee's cascaded OrgMembership on accept (Owner|User|Viewer). Null = default Viewer. |

#### `projectTeamAccess`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `teamId` | id | yes | references Team. Team |
| `projectId` | id | yes | references Project. Project |
| `role` | enum | yes | one of: Owner | User | Viewer. Role |
| `grantedById` | id | no | references User. Granted By |

#### `session`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `projectId` | id | yes | references Project. Project |
| `name` | string | yes | Name |
| `description` | string | no | Description |
| `slug` | string | no | Slug |
| `status` | enum | yes | one of: Provisioning | Running | Hibernating | Hibernated | Waking | Closing | Closed | Error. Status |
| `branchedOfCommit` | string | no | Branched Of Commit |
| `mergeCommit` | string | no | Merge Commit |
| `commitSha` | string | no | Commit SHA |
| `ownerId` | id | yes | references User. Owner |
| `aiConfig` | json | yes | AI Config |
| `runtimeConfig` | json | no | Runtime Config |
| `sessionStoreSnapshot` | json | no | Shared state snapshot (presence, UI state) persisted on hibernate, restored on wake. |
| `entryMethod` | enum | yes | one of: DescribeIdea | ExcelUpload. Entry Method |
| `complexityTier` | enum | yes | one of: Small | Standard | Complex. Complexity Tier |
| `mainConversationId` | id | no | references Conversation. Main Conversation |
| `lastActivityAt` | datetime | no | Last Activity |
| `hibernatedAt` | datetime | no | Hibernated At |
| `contextRestoreLimit` | int | no | Context Restore Limit |
| `idleTimeoutMinutes` | int | no | Per-session override for the idle auto-hibernation threshold, in minutes. Null uses the workspace default (SKAILE_IDLE_TIMEOUT_MINUTES, 120). Clamped to 5..1440. |
| `pushToMain` | boolean | no | Push to Main |
| `followMain` | boolean | no | Follow Main |
| `connectorAccessMode` | string | no | Controls whose credentials are used for external provider access. Values: OwnerDelegation (default), ServiceAccount, SharedDelegation. Null defaults to OwnerDelegation. |
| `scopeRootPath` | string | no | Relative path to the subfolder this session is scoped to. Null means full project (unscoped). Paths are relative to the volume root, forward-slash separated, no leading/trailing slash. |
| `skaileConfigId` | id | no | references SkaileConfig. Skaile Config |
| `visibility` | enum | no | one of: Private | Shared; default Private. Visibility |
| `lastCompactionAttemptAt` | datetime | no | Last Compaction Attempt |
| `lastCompactionStatus` | string | no | Last Compaction Status |
| `resumeStrategyUsed` | string | no | Resume Strategy Used |
| `resumeStrategyDetail` | json | no | Resume Strategy Detail |
| `lastDriverSessionId` | string | no | SDK session id last surfaced by the runner via a runtime_session event. Tier-1 native-resume token persisted independently of compaction. |
| `lastDriverModel` | string | no | Model in effect when lastDriverSessionId was captured. Wake-time tier-1 guard compares against the current model. |
| `lastCapabilitySignature` | string | no | Capability registry signature when lastDriverSessionId was captured. Wake-time tier-1 signature validation. |
| `isOpen` | boolean | no | default False. Open for A2A |
| `externalScope` | string | no | What this session can answer; shown to linking owners and injected into linked agents. |
| `voiceId` | string | no | Voice ID |
| `pendingInitialMessage` | string | no | First-run onboarding seed: the initial user message to flush once the session first reaches Running. Cleared after the seed is sent. |
| `initialMessageSeeded` | boolean | no | default False. Once-only guard: true after the pending initial message has been flushed on the first Running transition. |
| `isArchived` | boolean | no | default False. User-archived sessions are hidden from default sidebar lists but retain all data. Orthogonal to lifecycle status. |
| `assistantOwnerId` | id | no | references User. The user who owns this assistant session (set only for assistant sessions). |

**Custom actions:**

- `fork` — Fork this session into a new session, optionally with a new name. _(roles: Owner, PlatformAdmin)_
- `reopen` — Reopen a closed session. _(roles: Owner, PlatformAdmin)_
- `discard` — Discard a session permanently. _(roles: Owner, PlatformAdmin)_
- `leave` — Leave a session as a member. _(roles: User, Viewer, Owner, PlatformAdmin)_
- `archive` — Archive a session (hide from default sidebar, keep all data). Auto-closes if running. _(roles: Owner, PlatformAdmin)_
- `unarchive` — Unarchive a session (restore to default sidebar visibility). _(roles: Owner, PlatformAdmin)_

#### `sessionLink`

**Role for create / update / delete:** PlatformAdmin
- Normally written by the platform system user; direct agent CRUD is effectively admin-only.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `callerSessionId` | id | yes | references Session. Caller Session |
| `targetSessionId` | id | yes | references Session. Target Session |
| `createdById` | id | yes | references User. Created By |

#### `sessionMember`

**Role for create / update / delete:** User, Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `sessionId` | id | yes | references Session. Session |
| `userId` | id | no | references User. User |
| `role` | enum | yes | one of: Owner | User | Viewer. Role |
| `invitedById` | id | no | references User. Invited By |
| `status` | enum | yes | one of: Active | Invited | Expired | Revoked. Status |
| `invitedEmail` | string | no | Invited Email |
| `inviteTokenHash` | string | no | Invite Token Hash |
| `inviteExpiresAt` | datetime | no | Invite Expires |
| `inviteAcceptedAt` | datetime | no | Invite Accepted |
| `inviteDeclinedAt` | datetime | no | Invite Declined |
| `inviteContext` | string | no | Headstart note from the inviter about the invitee (e.g. 'CEO of XYZ'). Stashed on the invite and copied to User.invite_context on accept, then fed to the invitee's assistant onboarding. |
| `cascadeProjectRole` | string | no | Optional project role the inviter chose for the invitee's cascaded ProjectMember on accept (Owner|User|Viewer). Null = default Viewer. |
| `cascadeOrgRole` | string | no | Optional org role the inviter chose for the invitee's cascaded OrgMembership on accept (Owner|User|Viewer). Null = default Viewer. |
| `lastReadSeq` | int | no | default 0. Last Read Seq |

#### `usageRecord`

**Role for create / update / delete:** User, Owner, PlatformAdmin
- Normally written by the platform system user; direct agent CRUD is effectively admin-only.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `messageId` | string | no | Message Id |
| `sessionId` | string | no | Session Id |
| `projectId` | string | no | Project Id |
| `organizationId` | string | no | Organization Id |
| `userId` | string | no | User Id |
| `provider` | string | no | Provider |
| `model` | string | no | Model |
| `inputTokens` | int | no | Input Tokens |
| `outputTokens` | int | no | Output Tokens |
| `cacheReadTokens` | int | no | Cache Read Tokens |
| `cacheCreationTokens` | int | no | Cache Creation Tokens |
| `totalTokens` | int | no | Total Tokens |
| `costUsd` | float | no | Cost USD |

### Config — organizations, teams, assets, providers, credentials

#### `aIProviderConfig`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `organizationId` | id | no | references Organization. Organization |
| `projectId` | id | no | references Project. Project |
| `name` | string | yes | Name |
| `providerType` | enum | yes | one of: Anthropic | OpenAI | AzureOpenAI | Pi | OhMyPi | CustomHTTP. Provider Type |
| `credentialType` | enum | yes | one of: ApiToken | CredentialsFile. Credential Type |
| `scope` | enum | yes | one of: Global | Organization | Project. Scope |
| `endpoint` | string | yes | API Endpoint |
| `model` | string | yes | Model |
| `encryptedCredentials` | string | yes | Encrypted Credentials |
| `credentialFingerprint` | string | no | Credential Fingerprint |
| `fallbackOrder` | int | yes | Fallback Order |
| `isActive` | boolean | yes | Is Active |
| `lastTestedAt` | datetime | no | Last Tested |
| `tokenExpiresAt` | datetime | no | Token Expires At |
| `lastHealthCheckedAt` | datetime | no | Last Health Checked At |
| `lastHealthStatus` | enum | no | one of: healthy | rate_limited | auth_failed | unknown; default unknown. Last Health Status |
| `lastHealthRequestId` | string | no | Last Health Request Id |
| `recent401Count` | int | no | Recent 401 Count |
| `recent429Count` | int | no | Recent 429 Count |

#### `assetShare`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `organizationId` | id | yes | references Organization. Organization |
| `assetId` | id | yes | references ScopedAsset. Asset |
| `targetScope` | enum | yes | one of: Personal | Session | Project | Team | Org. Target Scope |
| `targetScopeRef` | string | no | Target Scope Ref |
| `status` | enum | yes | one of: Pending | Approved | Rejected. Status |
| `requestedById` | id | yes | references User. Requested By |
| `decidedById` | id | no | references User. Decided By |
| `decidedAt` | datetime | no | Decided At |

#### `dashboardPin`

**Role for create / update / delete:** User, Viewer, Owner, PlatformAdmin
- Row-level: a non-Owner may only write their **own** row.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `userId` | id | yes | references User. User |
| `itemType` | enum | yes | one of: project | session | app. Item Type |
| `itemId` | string | yes | Item Id |
| `label` | string | yes | Label |
| `orgSlug` | string | no | Org Slug |
| `projectSlug` | string | no | Project Slug |
| `sessionSlug` | string | no | Session Slug |
| `liveUrl` | string | no | Live Url |
| `sortOrder` | int | yes | Sort Order |

#### `deploymentTarget`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `organizationId` | id | yes | references Organization. Organization |
| `name` | string | yes | Name |
| `targetType` | enum | yes | one of: Docker | AWS | Azure | GCP | Kubernetes. Target Type |
| `config` | json | yes | Configuration |
| `status` | enum | yes | one of: Active | Inactive | Error. Status |
| `isDefault` | boolean | yes | Is Default |
| `lastTestedAt` | datetime | no | Last Tested |

#### `instanceSecret`

**Role for create / update / delete:** PlatformAdmin
- Normally written by the platform system user; direct agent CRUD is effectively admin-only.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `organizationId` | id | yes | references Organization. Organization |
| `instanceId` | id | yes | references LibraryInstance. Instance |
| `field` | string | yes | Field |
| `secretKind` | id | yes | references InstanceSecretKind. Secret Kind |
| `accessMode` | id | no | references InstanceSecretAccessMode; default Personal. Access Mode |
| `encryptedValue` | string | yes | Encrypted Value |
| `fingerprint` | string | no | Fingerprint |
| `createdById` | id | yes | references User. Created By |
| `rotatedAt` | datetime | no | Rotated At |

#### `libraryAssignment`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `organizationId` | id | yes | references Organization. Organization |
| `scope` | enum | yes | one of: Personal | Session | Project | Team | Org. Scope |
| `scopeRef` | string | no | Scope Ref |
| `instanceId` | id | yes | references LibraryInstance. Instance |
| `pinPolicy` | enum | yes | one of: Exact | MinorTrack | Latest. Pin Policy |
| `excluded` | boolean | no | default False. Suppression discriminator. false = grant; true = a suppression override that hides an inherited grant of the same instance at a narrower scope. |
| `defaultMode` | id | no | references LibraryAssignmentDefaultMode. When set, this org/project assignment is a company default that auto-loads into child sessions. recommended = member may hide; locked = member cannot hide. Null for personal/session assignments and suppression rows. |
| `credentialTrust` | boolean | no | default False. When true, this assignment's sessions may receive a Shared InstanceSecret. Default false: ad-hoc/self-serve sessions never silently inherit an org key. |

#### `libraryInstance`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `organizationId` | id | yes | references Organization. Organization |
| `assetId` | id | yes | references ScopedAsset. Asset |
| `scope` | enum | yes | one of: Personal | Session | Project | Team | Org. Scope |
| `scopeRef` | string | no | Scope Ref |
| `configJson` | json | yes | Config |
| `placeholdersJson` | json | yes | Placeholders |
| `accessMode` | enum | yes | one of: ServiceAccount | ElectedDelegate | OwnerDelegation. Access Mode |
| `createdById` | id | yes | references User. Created By |
| `customInstructions` | string | no | Owner-authored guidance text concatenated (root->child along the extends chain) into the materialized CONNECTOR.md/MCP.md body. Never holds secrets. |
| `extendsInstanceId` | id | no | references LibraryInstance. Parent preset this instance inherits config/credentials from (same asset, broader-or-equal scope). Resolved root->child in the materializer. |

#### `notificationOverride`

**Role for create / update / delete:** User, Owner, PlatformAdmin
- Row-level: a non-Owner may only write their **own** row.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `userId` | id | yes | references User. User |
| `scope` | enum | yes | one of: Project | Session. Scope |
| `projectId` | id | no | references Project. Project |
| `sessionId` | id | no | references Session. Session |
| `notificationMode` | enum | yes | one of: All | Mentions | Direct | Off. Notification Mode |
| `notifySound` | boolean | yes | Notify Sound |
| `notifyBrowser` | boolean | yes | Notify Browser |

#### `oAuthFlowState`

**Role for create / update / delete:** PlatformAdmin
- Normally written by the platform system user; direct agent CRUD is effectively admin-only.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `userId` | id | yes | references User. User |
| `organizationId` | id | yes | references Organization. Organization |
| `providerType` | id | yes | references ProviderType. Provider Type |
| `providerLinkId` | id | yes | references ProviderLink. Provider Link |
| `codeVerifier` | string | yes | Code Verifier |
| `baseUrl` | string | no | Base URL |
| `returnTo` | string | no | Return To |
| `intent` | enum | yes | one of: UserConnect | AdminApproval. Intent |
| `expiresAt` | datetime | yes | Expires At |

#### `orgCatalogConfig`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `organizationId` | id | yes | references Organization. Organization |
| `upstreamUrl` | string | yes | Upstream URL |
| `cacheTtl` | int | yes | Cache TTL |
| `filterMode` | enum | yes | one of: Allowlist | Blocklist. Filter Mode |
| `filterRulesJson` | json | yes | Filter Rules |
| `lastSyncedAt` | datetime | no | Last Synced |
| `cachedCatalogJson` | json | no | Cached Catalog |
| `selfServeCatalog` | boolean | no | default True. Self-Serve Catalog |

#### `orgMembership`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `displayName` | string | yes | Display Name |
| `userId` | id | no | references User. User |
| `organizationId` | id | yes | references Organization. Organization |
| `role` | enum | yes | one of: User | Viewer | Owner. Role |
| `status` | enum | yes | one of: Active | Invited | Expired | Revoked. Status |
| `invitedEmail` | string | no | Invited Email |
| `invitedById` | id | no | references User. Invited By |
| `inviteTokenHash` | string | no | Invite Token Hash |
| `inviteExpiresAt` | datetime | no | Invite Expires |
| `inviteAcceptedAt` | datetime | no | Invite Accepted |
| `inviteDeclinedAt` | datetime | no | Invite Declined |
| `inviteContext` | string | no | Headstart note from the inviter about the invitee (e.g. 'CEO of XYZ'). Stashed on the invite and copied to User.invite_context on accept, then fed to the invitee's assistant onboarding. |
| `lastActiveAt` | datetime | no | Last Active |

#### `organization`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `name` | string | yes | Name |
| `slug` | string | yes | Slug |
| `status` | enum | yes | one of: Active | Inactive. Status |
| `isPersonal` | boolean | no | default False. Personal orgs are single-user sandboxes for individuals. Created lazily when the user first opens 'My Workspace'. Block invites; surface as a 'My Workspace' shortcut in the org picker. |
| `logoUrl` | string | no | Logo URL |
| `iconSvg` | string | no | Icon SVG |
| `voiceId` | string | no | Voice ID |
| `defaultDeploymentTargetId` | id | no | references DeploymentTarget. Default Deployment Target |

#### `providerLink`

**Role for create / update / delete:** Owner, PlatformAdmin
- Stricter on the app UI: `create` needs Owner, PlatformAdmin.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `organizationId` | id | yes | references Organization. Organization |
| `name` | string | yes | Name |
| `category` | enum | yes | one of: Git | Files | Transport. Category |
| `providerType` | id | yes | references ProviderType. Provider Type |
| `credentialMechanism` | enum | yes | one of: UserDelegation | ServiceAccount | AppInstallation. Credential Mechanism |
| `appOwner` | enum | yes | one of: Org | Skaile. App Owner |
| `baseUrl` | string | no | Base URL |
| `config` | json | no | Provider Config |
| `oauthClientId` | string | no | OAuth Client ID |
| `encryptedOauthClientSecret` | string | no | Encrypted OAuth Client Secret |
| `appId` | string | no | App ID |
| `status` | enum | yes | one of: Active | Error | Disconnected. Status |
| `lastValidatedAt` | datetime | no | Last Validated |

#### `scopedAsset`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `organizationId` | id | yes | references Organization. Organization |
| `kind` | string | yes | Kind |
| `name` | string | yes | Name |
| `displayName` | string | yes | Display Name |
| `description` | string | no | Description |
| `scope` | enum | yes | one of: Personal | Session | Project | Team | Org. Scope |
| `scopeRef` | string | no | Scope Ref |
| `ownerId` | id | yes | references User. Owner |
| `originOwnerId` | id | yes | references User. Origin Owner |
| `forkedFromId` | string | no | Forked From |
| `locked` | boolean | yes | Locked |
| `contentKind` | enum | yes | one of: InternalBlob | UpstreamPointer. Content Kind |
| `contentFileId` | string | no | Content File |
| `sourceCommitSha` | string | no | Source Commit SHA |
| `filesJson` | json | no | Files |
| `sha256` | string | no | SHA256 |
| `kindProviderVersion` | string | no | Kind Provider Version |
| `adoptedFrom` | string | no | Adopted From |

#### `serviceAccountCredential`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `providerLinkId` | id | yes | references ProviderLink. Provider Link |
| `name` | string | yes | Name |
| `credentialType` | enum | yes | one of: OAuthToken | PAT | KeyPair | KeyFile | BasicAuth | VaultRef. Credential Type |
| `encryptedCredentials` | string | no | Encrypted Credentials |
| `tokenExpiresAt` | datetime | no | Token Expiry |
| `lastValidatedAt` | datetime | no | Last Validated |

#### `skaileConfig`

**Role for create / update / delete:** User, Owner, PlatformAdmin
- Row-level: a non-Owner may only write their **own** row.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `name` | string | no | Name |
| `config` | json | yes | Config |
| `organizationId` | id | yes | references Organization. Organization |
| `createdById` | id | no | references User. Created By |

#### `team`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `name` | string | yes | Name |
| `slug` | string | yes | Slug |
| `isDefault` | boolean | yes | Is Default |
| `organizationId` | id | yes | references Organization. Organization |

#### `teamMember`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `teamId` | id | yes | references Team. Team |
| `userId` | id | no | references User. User |
| `role` | enum | yes | one of: Owner | Member. Role |
| `status` | enum | yes | one of: Active | Invited | Expired | Revoked. Status |
| `invitedEmail` | string | no | Invited Email |
| `invitedById` | id | no | references User. Invited By |
| `inviteTokenHash` | string | no | Invite Token Hash |
| `inviteExpiresAt` | datetime | no | Invite Expires |
| `inviteAcceptedAt` | datetime | no | Invite Accepted |
| `inviteDeclinedAt` | datetime | no | Invite Declined |

#### `userNotificationPreference`

**Role for create / update / delete:** User, Owner, PlatformAdmin
- Row-level: a non-Owner may only write their **own** row.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `userId` | id | yes | references User. User |
| `notificationMode` | enum | yes | one of: All | Mentions | Direct | Off. Notification Mode |
| `notifySound` | boolean | yes | Notify Sound |
| `notifyBrowser` | boolean | yes | Notify Browser |

#### `userProviderCredential`

**Role for create / update / delete:** User, Owner, PlatformAdmin
- Row-level: a non-Owner may only write their **own** row.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `userId` | id | yes | references User. User |
| `providerLinkId` | id | yes | references ProviderLink. Provider Link |
| `externalUsername` | string | no | External Username |
| `credentialType` | enum | yes | one of: OAuthToken | PAT | KeyPair | KeyFile | BasicAuth. Credential Type |
| `encryptedCredentials` | string | yes | Encrypted Credentials |
| `tokenExpiresAt` | datetime | no | Token Expiry |
| `tokenScopes` | string | no | Token Scopes |
| `status` | enum | yes | one of: Active | Expired | Revoked. Status |

### Runtime — deployments, containers, scheduled actions

#### `deployment`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `label` | string | yes | Label |
| `sessionId` | id | yes | references Session. Session |
| `deploymentTargetId` | id | yes | references DeploymentTarget. Deployment Target |
| `triggeredById` | id | no | references User. Triggered By |
| `status` | enum | yes | one of: Pending | Building | Testing | Deploying | Running | Failed | Stopped. Status |
| `startedAt` | datetime | no | Started At |
| `completedAt` | datetime | no | Completed At |
| `liveUrl` | string | no | Live URL |
| `buildLogs` | json | no | Build Logs |
| `errorDetails` | string | no | Error Details |

#### `scheduledAction`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `sessionId` | id | yes | references Session. Session |
| `projectId` | id | yes | references Project. Project |
| `ownerId` | id | yes | references User. Owner |
| `prompt` | string | yes | The user-visible prompt text delivered at fire time. |
| `label` | string | no | Label |
| `kind` | enum | yes | one of: Once | Recurring. Kind |
| `runAt` | datetime | no | Run At |
| `cron` | string | no | Standard 5-field cron, interpreted in UTC. Set iff kind = Recurring. |
| `nextRunAt` | datetime | yes | Next Run At |
| `lastRunAt` | datetime | no | Last Run At |
| `enabled` | boolean | yes | Enabled |

#### `sessionContainer`

**Role for create / update / delete:** Owner, PlatformAdmin
- Normally written by the platform system user; direct agent CRUD is effectively admin-only.

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `sessionId` | id | yes | references Session. Session |
| `containerId` | string | yes | Docker Container ID |
| `status` | enum | yes | one of: Starting | Running | Stopping | Stopped | Error. Status |
| `startedAt` | datetime | no | Started At |
| `stoppedAt` | datetime | no | Stopped At |
| `lastHeartbeatAt` | datetime | no | Last Heartbeat |
| `aiProviderConfigId` | id | no | references AIProviderConfig. AI Provider Config |

### Platform — users

#### `user`

**Role for create / update / delete:** Owner, PlatformAdmin

| Field | Type | Req | Notes |
|-------|------|-----|-------|
| `sub` | string | no | The OpenID Connect provided subject identifier. |
| `name` | string | yes | The name of the user. |
| `email` | string | yes | The email of the user. |
| `profilePictureUrl` | string | no | The URL of the profile picture of the user. |
| `inviteContext` | string | no | Free-text note from whoever set up this user's account (e.g. 'CEO of XYZ, evaluating Skaile'). Given to the assistant at first-run as a warm starting point to confirm — never asserted as fact. Set from the user-admin dashboard. |
| `onboardingCompleted` | boolean | no | default False. Whether the user has started or skipped first-run onboarding. Persisted server-side so onboarding follows the user across browsers/devices. |
| `platformAdmin` | boolean | no | default False. Whether this user is a platform administrator. Set via DB — not derived from Keycloak JWT. |
| `emailVerified` | boolean | no | default False. Whether the user's email is verified, mirrored from the Keycloak email_verified claim on each login. Gates in-app invite acceptance (which has no magic-link proof of email control). |
| `personalOrgId` | id | no | references Organization. The user's personal organization (single-user sandbox). Created lazily when the user first opens 'My Workspace'. Null until then. |
| `assistantSessionId` | id | no | references Session. Back-pointer to the user's personal assistant session. Null until provisioned. |
| `keepWarmWindowMs` | int | no | default 1800000. How long to keep the user's assistant warm after activity. Default 30 min. |

