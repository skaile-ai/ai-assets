# /// script
# requires-python = ">=3.11"
# dependencies = [
#     "elevenlabs",
#     "python-dotenv",
# ]
# ///

import argparse
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

def text_to_speech(text: str, voice_id: str, output_path: Path):
    if not find_and_load_dotenv():
        print("Error: ELEVENLABS_API_KEY not found in environment or .env files.")
        sys.exit(1)
        
    api_key = os.getenv("ELEVENLABS_API_KEY")
    client = ElevenLabs(api_key=api_key)

    print(f"Generating audio for voice ID: {voice_id}...")
    try:
        audio = client.generate(
            text=text,
            voice=voice_id,
            model="eleven_multilingual_v2"
        )
        
        output_path.parent.mkdir(parents=True, exist_ok=True)
        with open(output_path, "wb") as f:
            for chunk in audio:
                if chunk:
                    f.write(chunk)
                    
        print(f"Success! Saved audio to {output_path}")
    except Exception as e:
        print(f"Error during TTS generation: {e}")
        sys.exit(1)

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert text to speech using ElevenLabs API")
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument("--text", type=str, help="Raw text to convert")
    group.add_argument("--input", type=Path, help="Path to text file to convert")
    
    parser.add_argument("--voice-id", required=True, type=str, help="ElevenLabs Voice ID")
    parser.add_argument("--output", required=True, type=Path, help="Path to save the output MP3")
    
    args = parser.parse_args()
    
    text_content = ""
    if args.input:
        if not args.input.exists():
            print(f"Error: Input file {args.input} does not exist.")
            sys.exit(1)
        with open(args.input, "r", encoding="utf-8") as f:
            text_content = f.read().strip()
    else:
        text_content = args.text
        
    if not text_content:
        print("Error: Text content is empty.")
        sys.exit(1)
        
    text_to_speech(text_content, args.voice_id, args.output)
