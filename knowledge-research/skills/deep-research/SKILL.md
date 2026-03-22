---
name: "deep-research"
description: "Integrated deep research skill that collects documents from multiple sources, stores them in organized folders, and synthesizes findings into a comprehensive research report with citations."
metadata:
  version: "1.0.0"
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "knowledge-research-contract"
---

# Deep Research - Integrated Research Skill

A hybrid research skill combining parallel document collection, structured storage, and evidence-based synthesis.

## When to Use This Skill

Use this skill when:
- You need comprehensive research with traceable sources
- You want to organize research findings systematically
- You need a final report with proper citations
- You're conducting multi-source analysis
- You need reproducible research workflows

## Features

### Three-Phase Workflow

1. **Collection Phase** - Parallel document gathering
   - Multi-source web search
   - Query decomposition
   - Rate-limited execution
   - Deduplication

2. **Storage Phase** - Organized persistence
   - Structured folder hierarchy
   - JSON metadata tracking
   - Raw source preservation
   - Clustering data

3. **Synthesis Phase** - Evidence-based reporting
   - Semantic clustering
   - Citation tracking
   - Structured markdown output
   - Quality scoring

## Requirements

- Python 3.8+
- Dependencies: `pip install -r requirements.txt`

## Installation

```bash
cd deep-research
pip install -r requirements.txt
```

## Usage

> [!IMPORTANT]
> **Run from Workspace Root**: Always replicate the command structure below from your project's root directory. Do not `cd` into the skill folder. This ensures artifacts are saved in your workspace, not hidden in the skill directory.

### Basic Research

```bash
# Run from your project root
python .agent/skills/deep-research/scripts/research.py --query "Research the history of Kubernetes"
```

### With Custom Output Directory

```bash
python .agent/skills/deep-research/scripts/research.py \
  --query "Compare Python web frameworks" \
  --output ./my-research
```

### Specify Number of Sources

```bash
python .agent/skills/deep-research/scripts/research.py \
  --query "Machine learning trends 2024" \
  --max-sources 20
```

### Skip Synthesis (Collection Only)

```bash
python .agent/skills/deep-research/scripts/research.py \
  --query "Docker vs Podman" \
  --no-synthesis
```

## Output Structure

```
research-output/
└── kubernetes-history/
    └── 20260217-180000/
        ├── metadata.json          # Research metadata
        ├── sources/               # Collected documents
        │   ├── source-001.json
        │   ├── source-002.json
        │   └── ...
        ├── clusters.json          # Semantic clustering
        └── final-report.md        # Synthesized report
```

## Metadata Format

```json
{
  "query": "Research the history of Kubernetes",
  "timestamp": "2026-02-17T18:00:00Z",
  "sources_collected": 15,
  "sources_used": 12,
  "clusters_identified": 4,
  "synthesis_completed": true,
  "duration_seconds": 45.3
}
```

## Final Report Structure

The synthesized `final-report.md` includes:

1. **Executive Summary** - Key findings overview
2. **Research Overview** - Query, methodology, sources
3. **Findings by Topic** - Clustered insights with citations
4. **Source Analysis** - Credibility assessment
5. **Conclusions** - Synthesized recommendations
6. **References** - Complete source list

## Advanced Usage

### Custom Source Types

```bash
python .agent/skills/deep-research/scripts/research.py \
  --query "GraphQL best practices" \
  --source-types academic,documentation,blog
```

### Parallel Collection Control

```bash
python .agent/skills/deep-research/scripts/research.py \
  --query "Rust ownership model" \
  --max-parallel 5
```

### Re-synthesize Existing Research

```bash
python .agent/skills/deep-research/scripts/synthesizer.py \
  --input ./research-output/kubernetes-history/20260217-180000
```

## Integration with Other Tools

### Use as a Library

```python
from scripts.research import DeepResearcher

researcher = DeepResearcher(
    query="WebAssembly use cases",
    output_dir="./research-output",
    max_sources=15
)

# Run full workflow
result = researcher.execute()

# Or run phases separately
researcher.collect()
researcher.synthesize()
```

## Best Practices

- **Specific Queries**: More specific queries yield better results
- **Source Diversity**: Use multiple source types for balanced research
- **Iterative Refinement**: Review initial results and re-run with refined queries
- **Citation Verification**: Always verify citations in final report
- **Storage Management**: Archive or clean old research outputs regularly

## Workflow Example

```bash
# 1. Initial research
python .agent/skills/deep-research/scripts/research.py --query "Container orchestration comparison"

# 2. Review sources in research-output/container-orchestration-comparison/*/sources/

# 3. Re-synthesize with different clustering
python .agent/skills/deep-research/scripts/synthesizer.py \
  --input ./research-output/container-orchestration-comparison/20260217-180000 \
  --min-cluster-size 3

# 4. Generate final report
# Output: research-output/container-orchestration-comparison/20260217-180000/final-report.md
```

## Configuration

Create `.research-config.json` in your project:

```json
{
  "default_output": "./research-output",
  "max_sources": 15,
  "max_parallel": 3,
  "source_types": ["academic", "documentation", "blog"],
  "min_credibility": 0.6,
  "cache_enabled": true
}
```

## Troubleshooting

### No sources collected
- Check internet connection
- Verify query is not too specific
- Increase `--max-sources`

### Synthesis fails
- Ensure sources were collected successfully
- Check `metadata.json` for errors
- Try re-running synthesis only

### Low quality results
- Refine query to be more specific
- Increase `--min-credibility` threshold
- Filter by `--source-types`

## Exit Codes

- **0**: Success
- **1**: Collection error
- **2**: Synthesis error  
- **3**: Configuration error
- **130**: Cancelled by user (Ctrl+C)
