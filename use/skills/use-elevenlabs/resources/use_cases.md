# Use Cases for use-elevenlabs

## 1. Single Voice Text-to-Speech

Generate audio for a single section of text or a file using a specific ElevenLabs voice ID.

**From raw text:**
```bash
uv run scripts/text_to_speech.py \
  --text "Hello, welcome to this demonstration." \
  --voice-id "21m00Tcm4llvDq8ikOAX" \
  --output ./output.mp3
```

**From a text file:**
```bash
uv run scripts/text_to_speech.py \
  --input ./script.txt \
  --voice-id "21m00Tcm4llvDq8ikOAX" \
  --output ./output.mp3
```

## 2. Multi-Voice Dialogue Generation

Generate conversational audio from a Markdown script. This is a two-step process utilizing the ElevenLabs v3 Dialogue API.

### Step 2a: Convert Markdown to JSON Manifest

First, parse your Markdown dialogue (with YAML frontmatter) into a structured JSON manifest mapping speaker text to exact Voice IDs.

*Format Reference (Markdown):*
```markdown
---
description: Introduction
characters:
  Clara: host_a_expert
---
Clara: Welcome to the show.
```

*Command:*
```bash
uv run scripts/md_to_json.py \
  --input ./dialogue.md \
  --output ./manifest.json \
  --hosts-dir ./hosts/
```

### Step 2b: Generate Audio from Manifest

Pass the generated JSON manifest to the dialogue API script to produce the final conversational audio track.

*Command:*
```bash
uv run scripts/dialog_to_speech.py \
  --manifest ./manifest.json \
  --output ./final_podcast.mp3
```
