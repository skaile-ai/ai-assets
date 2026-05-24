---
name: "use-docling"
description: "Use when you need to convert documents (PDF, DOCX, etc.), perform OCR, export images, transcribe audio, or extract structured data using the local docling CLI tool."
metadata:
  stage: "alpha"
  source: "MIGRATED"
  requires:
    - "contract:use-contract"
---

# Docling Document Processor

You have access to the local `docling` CLI tool, an advanced document processing CLI that parses diverse formats (PDF, DOCX, PPTX, HTML, WAV, MP3, images, etc.) and provides advanced PDF understanding natively.

**CRITICAL**: You invoke docling directly using the `bash` tool. There are no wrapper scripts.

## Setup & Environment Variables
Ensure the local `docling` CLI tool is installed and accessible in your path. It does not require API keys or external containers.

## Configuration

When invoking `docling`, respect the following user-approved defaults:

- **Image Export**: By default, export images as separate referenced PNG files (`--image-export-mode referenced`).
- **OCR Engine**: Enabled by default, using the 'auto' engine (`--ocr --ocr-engine auto`).
- **Table Mode**: Prefer the `accurate` table extraction mode (`--table-mode accurate`).
- **VLM Model**: Only specify (`--vlm-model <model>`) if the content specifically requires it.

## Instructions
- Consult the [Use Cases](resources/use_cases.md) to see how to approach different workflows (like OCR, Speech-to-Text, and format conversions).
- **CRITICAL**: You invoke docling directly using the `bash` tool. There are no wrapper scripts.

## References
The following sections document the internal structure and verified CLI reference for the `docling` tool.

## Verified CLI Reference (v2.57.0)

```
docling [OPTIONS] source
```

The `source` argument is the path to a local file or a URL. Multiple sources can be provided. The default output format is Markdown and the default output directory is the current working directory.

### Confirmed flags

| Flag | Values | Default | Notes |
|---|---|---|---|
| `--to` | `md`, `json`, `html`, `html_split_page`, `text`, `doctags` | `md` | Output format |
| `--output` | path | `.` | Output directory |
| `--image-export-mode` | `placeholder`, `embedded`, `referenced` | `embedded` | See below |
| `--table-mode` | `fast`, `accurate` | `accurate` | |
| `--ocr` / `--no-ocr` | — | `--ocr` | OCR enabled by default |
| `--ocr-engine` | `auto`, `easyocr`, `rapidocr`, `tesserocr`, `tesseract` | `auto` | |
| `--pipeline` | `standard`, `vlm`, `asr` | `standard` | |
| `--vlm-model` | `smoldocling`, `granite_vision`, `granite_docling`, `got_ocr_2`, and `_vllm`/`_ollama` variants | `granite_docling` | Only set when needed |
| `--asr-model` | `whisper_tiny`, `whisper_small`, `whisper_medium`, `whisper_base`, `whisper_large`, `whisper_turbo` | `whisper_tiny` | Audio only |
| `--enrich-code` | — | off | Classify code blocks |
| `--enrich-formula` | — | off | Render math formulas |
| `--enrich-picture-class` | — | off | Classify pictures |
| `--enrich-picture-desc` | — | off | Generate alt-text for pictures |
| `--num-threads` | int | 4 | |
| `--device` | `auto`, `cpu`, `cuda`, `mps` | `auto` | |
| `--abort-on-error` | — | off | |
| `-v` / `-vv` | — | — | Info / debug logging |

**Flags that do NOT exist** (common mistakes):
- `--extract-images` — use `--image-export-mode referenced` instead
- `--pages` — no page range selection in this version
- `--image-export-referenced` — the correct form is `--image-export-mode referenced`

## Output Structure

Docling writes output files named after the input stem:

```
<output_dir>/
├── <stem>.md                   # Markdown output
└── <stem>_artifacts/           # Created only with --image-export-mode referenced
    ├── image_000000_<hash>.png
    ├── image_000001_<hash>.png
    └── …
```

Image references inside the markdown use relative paths pointing into `<stem>_artifacts/`.
