# Deep Research Skill - Installation & Testing

## Installation with uv (Recommended)

This skill is optimized for `uv` package management.

### 1. Initialize Project & Environment

Navigate to the skill directory:

```bash
cd .agent/skills/deep-research
```

Initialize the environment:

```bash
uv venv
source .venv/bin/activate
```

### 2. Install Dependencies

```bash
uv pip install -r requirements.txt
```

## Quick Test

Run a test query with simulated sources:

```bash
python scripts/research.py --query "Deep learning transformers" --max-sources 5
```

## Production Usage

### Search API Integration

The default `collector.py` uses simulated search. To use real data, integrate a search API:

1. **Get an API Key** (e.g., Google Custom Search, Bing, Serper)
2. **Modify `scripts/collector.py`** to use `httpx` for real requests

### Output Structure

Results are saved to `research-output/{topic-slug}/{timestamp}/`:

- `final-report.md`: The synthesized research paper
- `sources/`: Raw collected JSON documents
- `metadata.json`: Execution stats and query info
- `clusters.json`: Semantic grouping data

## Troubleshooting

### "Externally managed environment" error
Use `uv venv` as shown above to create an isolated environment.

### "Object of type int32 is not JSON serializable"
This issue has been fixed in the latest version by explicitly casting numpy types.
