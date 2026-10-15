# Docling CLI Usage

`use-docling` is opt-in. Read `SKILL.md` first: it lists the version floors (docling
>= 2.132.0, docling-core >= 2.101.1), the offline setup (`HF_HUB_OFFLINE=1`,
`HF_HUB_DISABLE_TELEMETRY=1`, `--artifacts-path`), the required limits
(`--document-timeout`, and `--page-range` for PDF, XLSX and PPTX), and the forbidden inputs (URL sources,
`--enable-remote-services`, `--allow-external-plugins`).

## Core command

```bash
out=$(mktemp -d); echo "$out"
HF_HUB_OFFLINE=1 HF_HUB_DISABLE_TELEMETRY=1 \
  timeout 600 docling \
    --artifacts-path /path/to/pinned-models \
    --document-timeout 300 \
    --page-range 1-50 \
    --image-export-mode placeholder \
    --to md \
    --output "$out" \
    /path/to/document.pdf
```

Change `--to` (`md`, `json`, `html`, `text`, `doctags`) for another format. Output goes to
the scratch directory, never next to the source file.

More examples (OCR, images, ASR, VLM, enrichment): [`resources/use_cases.md`](resources/use_cases.md).
