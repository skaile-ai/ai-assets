---
name: stripe-key
description: "Use when a fleet of agents (every hire of one job description) needs
  Stripe access under one shared credential - look up customers, payments,
  subscriptions, invoices, search the Stripe API and docs. Wraps Stripe's official
  hosted MCP server (mcp.stripe.com) over streamable HTTP, authenticated with a
  Stripe agent API key (a restricted key tagged for agent access) sent as a bearer
  token instead of the per-user OAuth sign-in. Every agent that receives this
  credential uses the same key and its permissions. For an individual person working
  with their own Stripe login, use the `stripe` (per-user OAuth) entry instead."
version: 0.1.0
transport: http
url: https://mcp.stripe.com
auth:
  type: bearer
  inject: header
  header: Authorization
  fields:
    - { key: apiKey, label: "Stripe agent API key (restricted)", secret: true }
keywords:
  - stripe
  - payments
  - billing
  - subscriptions
  - invoices
  - api-key
  - fleet
  - mcp
  - remote
---

# Stripe MCP Server, shared agent API key (remote)

Stripe's official, Stripe-hosted MCP server, reached with a static key instead of
OAuth. This is a **remote** asset: the runner opens a streamable-HTTP connection
straight to `https://mcp.stripe.com` and sends the key as
`Authorization: Bearer <key>`, the form Stripe documents for autonomous clients that
cannot use OAuth ([Stripe MCP: Authenticate](https://docs.stripe.com/mcp)). The
toolset is advertised by the server at connect time.

## One key, many agents

The key is a single secret shared by **every recipient**: when a project owner gives
it to a job description, all hires of that job description act on the Stripe account
with the same key and the same permissions. Use this entry for agent fleets. For a
person working with their own Stripe login, use the per-user OAuth entry
**`stripe`** instead: it is the default for individuals.

## Credential: an agent API key with minimal permissions

Stripe MCP accepts only **agent API keys**: restricted keys (`rk_...`) tagged for
agent access by choosing **Authorizing agent access to your account** when creating
the key ([API keys for autonomous agents](https://docs.stripe.com/keys#agent-keys)).
From December 2026 Stripe MCP refuses full-access secret keys (`sk_...`) and
restricted keys without the Agent tag with a `401`.

- Grant only the resources the fleet needs, and **Read** rather than **Write**
  wherever the agents only look things up (e.g. Customers, Charges, PaymentIntents,
  Subscriptions, Invoices: Read; everything else: None).
- Start with a sandbox key (`rk_test_...`) and switch to a live key only once the
  fleet's behaviour is checked.
- Agent-tagged keys fall under Stripe's [approval rules](https://docs.stripe.com/account/approvals):
  sensitive actions (payouts, refunds, account settings) wait for a designated
  reviewer. Keep those rules on.
- To replace a key, test the new one in a sandbox first, then expire the old one.

## Gotchas

- **401 `invalid_api_key`** means the key is wrong, expired or revoked, or is not an
  agent-tagged restricted key. Retrying will not help; replace the key.
- **Permission errors are the key's limits.** A call to a resource the key may not
  touch fails; widen the key only if the fleet really needs it.
- **Connected accounts.** To act on a connected account, Stripe takes a
  `Stripe-Account: acct_...` header; this entry does not set one, so calls run on the
  key's own account.
- **Real money in live mode.** A live key with write permissions can create charges,
  refunds and subscriptions for every hire at once. Prefer read-only keys for fleets.

## Vendor docs

- [Model Context Protocol (MCP)](https://docs.stripe.com/mcp) (setup, "Authenticate Stripe MCP", key migration deadline)
- [API keys: agent keys](https://docs.stripe.com/keys#agent-keys)
- [Restricted API keys](https://docs.stripe.com/keys#create-restricted-api-key)
- [Approval rules](https://docs.stripe.com/account/approvals)
