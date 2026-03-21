# /// script
# requires-python = ">=3.11"
# dependencies = [
#     "pyyaml"
# ]
# ///

import argparse
import json
import sys
from pathlib import Path
import yaml

def convert_md_to_json(input_path: Path, output_path: Path, voice_map: dict):
    characters_map = {}
    
    with open(input_path, 'r', encoding='utf-8') as f:
        content = f.read()
        
    body = content
    if content.startswith('---'):
        parts = content.split('---', 2)
        if len(parts) >= 3:
            try:
                metadata = yaml.safe_load(parts[1])
                if isinstance(metadata, dict):
                    chars = metadata.get("characters")
                    if isinstance(chars, dict):
                        characters_map = chars
            except Exception as e:
                print(f"Warning: Could not parse frontmatter: {e}")
            body = parts[2]
            
    dialogue_inputs = []
    
    for line in body.splitlines():
        line = line.strip()
        if line.startswith('- ') or line.startswith('* '):
            line = line[2:].strip()
        if not line:
            continue
        if ':' in line:
            parts = line.split(':', 1)
            speaker = parts[0].strip().replace('**', '').replace('*', '')
            text = parts[1].strip()
            
            if getattr(speaker, "isascii", lambda: True)() and len(speaker) < 50:
                # Resolve voice ID
                host_id = characters_map.get(speaker, speaker)
                voice_id = voice_map.get(host_id, voice_map.get(speaker, voice_map.get(speaker.upper())))
                
                if not voice_id:
                    print(f"Warning: No voice mapping for speaker '{speaker}'. Skipping line.")
                    continue
                    
                dialogue_inputs.append({
                    "text": text,
                    "voice_id": voice_id
                })

    if not dialogue_inputs:
        print("Error: No valid dialogue lines found to convert.")
        sys.exit(1)
        
    output_path.parent.mkdir(parents=True, exist_ok=True)
    with open(output_path, 'w', encoding='utf-8') as f:
        json.dump({"dialogue": dialogue_inputs}, f, indent=2, ensure_ascii=False)
        
    print(f"Success! Converted {len(dialogue_inputs)} lines to {output_path}")

def load_voice_map(hosts_dir: Path) -> dict:
    voice_map = {}
    try:
        if hosts_dir and hosts_dir.exists():
            for host_file in hosts_dir.glob("*.json"):
                with open(host_file, 'r', encoding='utf-8') as f:
                    host_data = json.load(f)
                    persona_id = host_data.get("persona_id")
                    voice_id = host_data.get("elevenlabs_voice_id")
                    
                    if persona_id and voice_id:
                        voice_map[persona_id] = voice_id
                        
                    name = host_data.get("name")
                    if name and voice_id:
                        voice_map[name.upper()] = voice_id
    except Exception as e:
        print(f"Warning: Error loading hosts from directory: {e}")
    return voice_map

if __name__ == "__main__":
    parser = argparse.ArgumentParser(description="Convert Markdown dialogue to expected JSON format")
    parser.add_argument("--input", required=True, type=Path, help="Path to input MD file")
    parser.add_argument("--output", required=True, type=Path, help="Path to save the output JSON")
    parser.add_argument("--hosts-dir", type=Path, help="Path to directory containing host JSON profiles")
    
    args = parser.parse_args()
    
    if not args.input.exists():
        print(f"Error: Input file {args.input} does not exist.")
        sys.exit(1)
        
    voice_map = {}
    if args.hosts_dir:
        voice_map = load_voice_map(args.hosts_dir)
        
    convert_md_to_json(args.input, args.output, voice_map)
