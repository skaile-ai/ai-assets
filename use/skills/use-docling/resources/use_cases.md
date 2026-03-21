# Use Cases for Docling

## Basic Document Conversion

Convert a local file to Markdown in the current directory:
```bash
docling /path/to/document.pdf
```

Specify output directory:
```bash
docling --output /path/to/output_dir /path/to/document.pdf
```

Specify output format (e.g. JSON):
```bash
docling --to json /path/to/document.pdf
```

## Image Handling

The default is `referenced` — images are extracted as separate PNG files and linked from the markdown.
Use the PDF's own directory as `--output` so the `.md` and `_artifacts/` folder land next to the source file:
```bash
# Default: extract images alongside the PDF
docling --image-export-mode referenced --output /path/to/ /path/to/document.pdf

# Suppress image extraction (placeholder markers only)
docling --image-export-mode placeholder --output /path/to/ /path/to/document.pdf

# Embed images as base64 in the markdown (single-file artifact)
docling --image-export-mode embedded --output /path/to/ /path/to/document.pdf
```

## Audio Transcription (ASR)

```bash
docling --pipeline asr --asr-model whisper_large /path/to/audio.mp3
```

## Advanced Visual Language Models (VLM)

Use only when standard pipeline is insufficient (e.g., complex charts, heavily image-based PDFs):

```bash
docling --pipeline vlm --vlm-model granite_vision /path/to/document.pdf
```

## Content Enrichment

```bash
# Enable code block classification and formula rendering
docling --enrich-code --enrich-formula /path/to/document.pdf
```

## Execution Control

```bash
# Verbose info logging
docling -v /path/to/document.pdf

# Abort on first error, with debug logs
docling --abort-on-error -vv /path/to/document.pdf

# Limit thread count
docling --num-threads 2 /path/to/document.pdf
```

## Full Example: PDF with Referenced Images (output beside the PDF)

Pass the PDF's parent directory as `--output` so the markdown and images land next to the source file:
```bash
# Convert a research paper; extract images as separate files; enrich formulas
docling \
  --to md \
  --image-export-mode referenced \
  --enrich-formula \
  --output /path/to/paper/ \
  /path/to/paper/research_paper.pdf
```

Output will be at `/path/to/paper/research_paper.md` with images under `/path/to/paper/research_paper_artifacts/`, co-located with the original PDF.
