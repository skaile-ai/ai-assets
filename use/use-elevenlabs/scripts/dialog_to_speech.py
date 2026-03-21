# /// script
# requires-python = ">=3.11"
# dependencies = [
#     "elevenlabs",
#     "python-dotenv"
# ]
# ///

import argparse
import json
import os
import sys
from pathlib import Path
from elevenlabs.client import ElevenLabs
from dotenv import load_dotenv

def find_and_load_dotenv():
    if os.getenv("ELEVENLABS_API_KEY"):
        return True

    script_path = Path(__file__).resolve()
    search_dirs = [
        script_path.parent,            # scripts/
        script_path.parent.parent,     # skill root
        Path.cwd()                     # workspace root
    ]
    
    for d in search_dirs:
        env_path = d / ".env"
        if env_path.exists():
            load_dotenv(dotenv_path=env_path)
            if os.getenv("ELEVENLABS_API_KEY"):
                print(f"Loaded API key from {env_path}")
                return True
    return False

def dialog_to_speech(input_path: Path, output_path: Path):
    if not find_and_load_dotenv():
        print("Error: ELEVENLABS_API_KEY not found in environment or .env files.")
        sys.exit(1)
        
    api_key = os.getenv("ELEVENLABS_API_KEY")
    client = ElevenLabs(api_key=api_key)

    with open(input_path, 'r', encoding='utf-8') as f:
        data = json.load(f)
        
    dialogue_inputs = data.get("dialogue", [])
    
    if not dialogue_inputs:
        print("Error: No valid dialogue lines found in JSON to convert.")
        sys.exit(1)

    print(f"Converting dialogue ({len(dialogue_inputs)} lines) using ElevenLabs v3 Dialogue API...")
    
    try:
        output_path.parent.mkdir(parents=True, exist_ok=True)
        # Batch conversion
        audio_generator = client.text_to_dialogue.convert(
            inputs=dialogue_inputs,
            output_format="mp3_44100_128",
            model_id="eleven_v3"
        )
        
        with open(output_path, 'wb') as out_f:
            for chunk in audio_generator:
                if chunk:
                    out_f.write(chunk)
                    
        print(f"Success! Saved full dialogue to {output_path}")
                
    except Exception as e:
        print(f"Error during dialogue generation: {e}")
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert JSON dialogue manifest to ElevenLabs audio using v3 Dialogue API")
    parser.add_argument("--manifest", required=True, type=Path, help="Path to input JSON manifest file")
    parser.add_argument("--output", required=True, type=Path, help="Path to save the output MP3")
    
    args = parser.parse_args()
    
    if not args.manifest.exists():
        print(f"Error: Manifest file {args.manifest} does not exist.")
        sys.exit(1)
        
    dialog_to_speech(args.manifest, args.output)
