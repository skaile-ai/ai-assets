# Docling CLI Usage

This document explains how to use the `docling` CLI tool within the `use-docling` skill to convert documents, perform OCR, export images, transcribe audio, and extract structured data.

## Core Commands

The `docling` CLI is used directly. No additional wrapper scripts are required for basic usage.

### Basic Document Conversion
Converts a file or URL to Markdown (default output) in the current directory:
```bash
docling /path/to/document.pdf
```

Specify the output format (Markdown, JSON, HTML, Text) and output directory:
```bash
docling --to json --output /path/to/output_dir /path/to/document.pdf
```

### Advanced Image Handling
By default, the skill uses `--image-export-mode placeholder`. If actual images are needed:
- **Embedded**: Embeds the image as a base64 encoded string directly in the output (useful for single-file artifacts, JSON/HTML/Markdown).
  ```bash
  docling --image-export-mode embedded /path/to/document.pdf
  ```
- **Referenced**: Exports images as separate PNG files and references them in the main document.
  ```bash
  docling --image-export-mode referenced --output ./docs /path/to/document.pdf 
  ```

### Table Structure Mode
Uses accurate tables by default:
```bash
docling --table-mode accurate /path/to/document.pdf 
```

### Audio Transcription (ASR)
Transcribes audio files (WAV, MP3, etc.) using Whisper models:
```bash
docling --asr-model whisper_large /path/to/audio/file.mp3
```

### Visual Language Models (VLM)
Use for complex chart or image understanding:
```bash
docling --vlm-model granite_vision /path/to/document.pdf
```

### Content Enrichment
Enables additional specialized models for advanced extraction:
```bash
docling --enrich-code --enrich-formula --enrich-picture-desc /path/to/document.pdf 
```

## Suggested Subcommands

While the raw `docling` CLI is powerful, the following wrapper subcommands could be implemented in the future to simplify complex or repetitive workflows:

1. **`docling-batch`**: A script to process multiple documents in a directory recursively. It could take a folder path, apply standardized defaults (e.g., `--image-export-mode referenced --table-mode accurate`), and output the results into a mirrored directory structure.
2. **`docling-extract`**: A helper subcommand optimized for extracting only specific elements from a document, such as isolating all tables into a single JSON file or extracting all images to a dedicated folder without generating the full markdown text.
3. **`docling-transcribe`**: A simplified wrapper tailored strictly for audio transcription, pre-configured with the optimal Whisper model, automatic noise-reduction, and a clean output text file generation.
