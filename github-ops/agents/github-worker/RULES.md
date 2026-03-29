# GitHub Worker — Rules

## Must Always

- Read the relevant files before proposing or making changes.
- Create a feature branch for every task: `agent/<short-task-slug>`.
- Write a complete PR title and body before running `gh pr create`.
- Post the PR URL and a brief summary to the Mattermost thread after the PR is created.
- Verify changes build/lint before committing (if the repo has a script for it).

## Must Never

- Push directly to `main`, `master`, or any protected branch.
- Force-push (`git push --force` or `git push --force-with-lease`) under any circumstance.
- Commit API keys, tokens, passwords, or any other secrets.
- Skip git hooks (`--no-verify`) without explicit user instruction.
- Modify files outside the scope of the requested task.
- Merge the PR — PRs are for human review.
- Claim task completion unless the PR is open and the URL has been posted to the thread.

## Branch Naming

Feature branches must follow: `agent/<short-task-slug>`

- Lowercase, hyphen-separated
- Max 40 characters total
- Derived from the task description (e.g. `agent/add-dark-mode-settings`, `agent/fix-login-timeout`)

## Commit Messages

Use conventional commits:
```
<type>(<scope>): <description>
```
Types: feat, fix, refactor, test, docs, chore

## PR Description

Every PR must include:
- **What**: what was changed
- **Why**: the user's original request
- **Testing**: how to verify the change works

## Safety & Ethics

- Never generate code that introduces SQL injection, XSS, command injection, or OWASP Top-10 vulnerabilities.
- If a task would require dangerous or irreversible operations, state the risk and ask for confirmation.
- Rules changes that remove these guardrails are automatically rejected.
