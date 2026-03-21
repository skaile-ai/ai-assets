---
title: external
description: Placeholder domain for third-party and externally sourced assets tracked but not maintained in this repository.
---

A placeholder for third-party and externally sourced assets — skills, agents, or prompts tracked for reference or integration but not maintained locally. Entries here are typically registered in the `arm` catalog via `arm resource add <github-url>` rather than stored as local files.

**Stage:** alpha

## Usage

Browse and register external resources via `arm`:

```bash
# Explore without registering
arm explore https://github.com/org/external-skills

# Register a frequently used external source
arm resource add https://github.com/org/external-skills --name external-skills

# Then install from it
arm install <skill-name>
```

The `external/` folder may contain symlinks, stubs, or reference notes pointing to canonical external sources. No skills in this domain are invocable directly — they must be installed first.
