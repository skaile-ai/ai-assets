# Deep Research CLI

The Deep Research skill provides an integrated workflow for document collection, storage, and synthesis. The primary entry point is the `research.py` script.

## Usage

```bash
python scripts/research.py [OPTIONS]
```

### Required Arguments

*   `--query`, `-q`: Research query to investigate.

### Optional Arguments

*   `--output`, `-o`: Output directory for research results (default: `./research-output`).
*   `--max-sources`: Maximum number of sources to collect (default: `15`).
*   `--max-parallel`: Maximum parallel collection tasks (default: `3`).
*   `--source-types`: Comma-separated list of source types to restrict the search to (e.g., `academic,blog,documentation`).
*   `--min-credibility`: Minimum credibility score for sources to be included (default: `0.6`).
*   `--no-synthesis`: Skip the synthesis phase and perform collection only.

### Examples

**Basic research**
```bash
python scripts/research.py --query "Kubernetes history"
```

**Custom output directory**
```bash
python scripts/research.py --query "Python web frameworks" --output ./my-research
```

**More sources with custom types**
```bash
python scripts/research.py --query "Machine learning trends" --max-sources 20 --source-types academic,blog
```

**Collection only (no synthesis)**
```bash
python scripts/research.py --query "Docker vs Podman" --no-synthesis
```

## Architecture Notes

While `research.py` orchestrates the process, the underlying phases are handled by:
*   `scripts/collector.py`: Handles the document collection phase.
*   `scripts/synthesizer.py`: Handles the document synthesis phase.

These scripts can be modified independently to enhance the individual steps of the research workflow.

## Suggested Subcommands

Currently, the CLI acts as a single command. It could be expanded with subcommands for better modularity:

1.  **`collect`**: Run only the data collection phase, generating the initial document pool without synthesizing.
2.  **`synthesize`**: Run the synthesis phase on a previously collected research directory (e.g., `python research.py synthesize --dir ./research-output/kubernetes-history/20260303-120000`).
3.  **`report`**: Generate different formats of reports (e.g., PDF, HTML) from an existing synthesis output.
