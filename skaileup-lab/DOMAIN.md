---
name: skaileup-lab
description: Skill laboratory — validate, improve, and learn from AI skills
---

## Purpose

Provides AI skill prompts, flow definitions, and agent configurations for the skaile-agent-lab package. Used to drive automated skill validation, improvement loops, and continuous learning from real skill usage.

## Skills

| Skill | Path | What It Does | When to Use |
|---|---|---|---|
| lab-validate | skills/lab-validate/ | Drives validation flow: parse manifest, execute recipes, run metrics | Automated skill testing |
| lab-judge | skills/lab-judge/ | LLM-as-judge quality scoring on four dimensions | Quality evaluation after code generation |
| lab-report | skills/lab-report/ | Generate structured validation/improvement reports | After validation or improvement runs |
| lab-improve | skills/lab-improve/ | Drive mutation and iteration for skill improvement | Automated skill improvement loops |
| lab-learn | skills/lab-learn/ | Analyze observations and extract patterns | Processing real-world usage data |

## Contracts

- `skaileup-lab-contract` — Shared interfaces: test manifest, metric results, observations, quality scores

## Flows

- `validate-skill` — Run test suite in Docker, judge quality, generate report
- `improve-skill` — Iterate: mutate, validate, score, keep/revert, PR
- `learn-skill` — Process observations, extract patterns, write learnings

## Notes

Infrastructure code lives in `agent-framework/lab/`. This domain only contains AI content (prompts, flows, agent configs).
