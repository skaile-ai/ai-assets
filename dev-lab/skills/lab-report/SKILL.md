---
name: lab-report
description: Generate structured validation and improvement reports with trend analysis
source: MERGED
version: 1.0.0
keywords: [lab, report, analysis, trends]
user_inputs: []
reads_from: [data/results/]
writes_to: []
---

# Lab Report

Generate a structured report from validation and improvement results.

Include:
- **Summary**: skill name, timestamp, overall pass rate, average quality
- **Per-case results**: case ID, gate status, quality score, duration, errors
- **Regression analysis**: compare against previous runs, flag regressions
- **Trend analysis**: quality over time, pass rate trends
- **Recommendations**: prioritized list of improvement opportunities

Format output as structured YAML for machine consumption, with a human-readable markdown summary.
