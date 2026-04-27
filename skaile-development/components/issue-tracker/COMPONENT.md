---
name: issue-tracker
description: Interactive issue board for agent sessions
version: 1.0.0
keywords: [issues, tasks, tracking, collaboration]

provides:
  - type: component
    component: { kind: web-component, url: ./issue-tracker.js, tagName: skaile-issue-tracker }
    schema:
      type: object
      properties:
        issues:
          type: array
          items:
            type: object
            properties:
              id: { type: string }
              title: { type: string }
              status: { type: string, enum: [open, in-progress, done] }
              priority: { type: string, enum: [high, medium, low] }
              assignee: { type: string }
              createdBy: { type: string }
              createdAt: { type: string }
              sourceMessageId: { type: string }
              notes: { type: string }
        sessionId: { type: string }
    interactions: [create, update, select, export]
    targets: [preview]
    fallback: |
      ## Issues
      {{#each issues}}
      - [{{status}}] **{{title}}** ({{priority}}) - assigned to {{assignee}}
      {{/each}}
---

# Issue Tracker

Interactive issue board that lives in the preview pane during agent sessions.
Both users and the agent can create, update, assign, and complete issues.

## Interactions

| Action | Trigger | Effect |
|--------|---------|--------|
| create | User clicks "+" or agent emits render update | New issue added |
| update | User changes status/assignee or agent updates | Issue modified |
| select | User clicks an issue | Scrolls chat to linked message |
| export | User clicks export button | Triggers export-data capability |
