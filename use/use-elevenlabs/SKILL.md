---
name: use-elevenlabs
source: MIGRATED
description: Use when you need to convert markdown speaker dialogue and text into audio using the ElevenLabs API. Includes support for single voice TTS and multi-voice conversational dialogues.
env_vars:
  ELEVENLABS_API_KEY: "Required. Your ElevenLabs API key for authentication."
keywords: []
reads_from: []
writes_to: []
metadata:
  stage: alpha
  requires:
  - use-contract
---

# Use ElevenLabs

## Setup & Environment Variables

You must provide a valid API key to authenticate with ElevenLabs.
The skill will search for an `ELEVENLABS_API_KEY` defined in the environment, or within a `.env` file either inside the skill directory or at the workspace root.

```bash
# Example .env entry
export ELEVENLABS_API_KEY="sk_..." 
```

### Dependencies
The underlying scripts use the `elevenlabs`, `pydantic`, `python-dotenv`, and `pyyaml` pip packages via the `uv run` mechanism. Required dependencies are fetched automatically.

## Configuration

### Voice Map
When generating dialogues from Markdown, you must map character names to specific ElevenLabs Voice IDs. 
You can provide a directory of host JSON files (each detailing a `persona_id` and `elevenlabs_voice_id`) using the `--hosts-dir` parameter during the markdown-to-json conversion phase.

## Goal
To programmatically generate podcast or conversational audio from structured inputs, bypassing the need for manual text entry into the ElevenLabs Studio.

## Instructions
- Consult the [Use Cases](resources/use_cases.md) to see how to approach different workflows with this tool (Single TTS vs. Dialogue TTS).
- The monolithic TTS process is split into distinct scripts for modularity:
  1. `text_to_speech.py`: Basic single-voice generation.
  2. `md_to_json.py`: Converts formatted Markdown with YAML frontmatter into the JSON manifest expected by the dialogue API.
  3. `dialog_to_speech.py`: Submits a JSON manifest to the ElevenLabs v3 Dialogue API to produce final audio.

## Auto-Improvement
- Every time this skill is used, analyze the usage chat to find out if further improvement of the skill is advised.
- Ask the user if those changes should be made.
- If approved, store the improvement ideas in the `resources/improvement_ideas.md` file.

## Script Integration

```bash
uv run scripts/text_to_speech.py --help
uv run scripts/md_to_json.py --help
uv run scripts/dialog_to_speech.py --help
```
