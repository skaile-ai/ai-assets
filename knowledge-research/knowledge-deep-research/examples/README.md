# Deep Research - Example Usage

This directory contains example outputs and usage patterns for the deep research skill.

## Quick Start

```bash
# Install dependencies
pip install -r requirements.txt

# Run basic research
python scripts/research.py --query "Kubernetes container orchestration"

# View results
cat research-output/kubernetes-container-orchestration/*/final-report.md
```

## Example Queries

### Technology Research
```bash
python scripts/research.py --query "WebAssembly use cases and performance"
python scripts/research.py --query "Rust vs Go for microservices"
python scripts/research.py --query "GraphQL vs REST API design"
```

### Market Analysis
```bash
python scripts/research.py --query "Cloud computing market trends 2024" --source-types news,blog
python scripts/research.py --query "AI startup landscape Europe" --max-sources 20
```

### Academic Research
```bash
python scripts/research.py --query "Quantum computing algorithms" --source-types academic
python scripts/research.py --query "Neural network architectures survey" --min-credibility 0.8
```

## Output Example

After running a research query, you'll get a structured directory:

```
research-output/
└── kubernetes-container-orchestration/
    └── 20260217-180000/
        ├── metadata.json
        ├── sources/
        │   ├── source-001.json
        │   ├── source-002.json
        │   └── ...
        ├── clusters.json
        └── final-report.md
```

## Sample Metadata

`metadata.json`:
```json
{
  "query": "Kubernetes container orchestration",
  "timestamp": "2026-02-17T18:00:00Z",
  "sources_collected": 12,
  "sources_used": 12,
  "clusters_identified": 3,
  "synthesis_completed": true,
  "duration_seconds": 42.5
}
```

## Sample Final Report Structure

`final-report.md`:
```markdown
# Research Report: Kubernetes container orchestration

*Generated: 2026-02-17T18:00:00Z*

## Executive Summary
This report presents findings from **12 sources** organized into **3 thematic clusters**.

## Findings

### Architecture & Design
Key insights about Kubernetes architecture...

### Deployment & Operations
Best practices for deployment...

### Ecosystem & Tools
Related tools and integrations...

## References
[1] **Kubernetes Documentation** ...
[2] **CNCF Blog Post** ...
```

## Advanced Workflows

### Re-synthesis with Different Parameters
```bash
# Initial research
python scripts/research.py --query "Docker alternatives"

# Re-synthesize with different clustering
python scripts/synthesizer.py research-output/docker-alternatives/20260217-180000
```

### Programmatic Usage
```python
from scripts.research import DeepResearcher
import asyncio

async def my_research():
    researcher = DeepResearcher(
        query="Serverless computing patterns",
        max_sources=10
    )
    
    # Run collection only
    await researcher.collect()
    
    # Custom processing here...
    
    # Then synthesize
    researcher.synthesize()

asyncio.run(my_research())
```
