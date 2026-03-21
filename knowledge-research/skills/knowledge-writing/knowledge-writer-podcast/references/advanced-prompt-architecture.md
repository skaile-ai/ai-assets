# Advanced Architectural Prompt Engineering for Podcasts

This document summarizes the theoretical foundation underlying the `knowledge-writer-podcast` skill.

## 1. The Core Problem
Relying on single-shot text completion prompts for two-speaker dialogue yields outputs that are robotic, overly formal, and unnatural. AI text generators suffer from "emotional mansplaining," where conflicts resolve perfectly and characters lack genuine psychological depth or constraint. 

## 2. Five-Part Persona Formulation
To prevent "model collapse" (where distinct voices merge into a homogeneous AI tone), personas must be rigidly constrained:
1. **Role and Goal**: The specific mandate (e.g., Domain Expert vs. Audience Proxy).
2. **Knowledge Base**: The boundaries of their expertise.
3. **Tone and Style**: Pacing, terminology, and syntax.
4. **Negative Constraints**: What the model MUST NOT do (e.g., no therapy-speak, no marketing jargon).
5. **Example Output**: Few-shot contextual baseline.

## 3. Mathematical Chunking & Narrative Structure
To avoid LLM context-window degradation and TTS memory overflows:
- The script is structured using **3-Acts + Epilogue**.
- The content is mathematically chunked into short, discrete segments (e.g., ~1000-1500 characters) to optimize for TTS stability and prevent memory overflows in TTS models.
- **Contextual Redundancy**: Because chunks are generated in isolation, each chunk forces the AI to open with a brief recap of the previous chunk's conclusion to maintain continuity.

## 4. The Three-Pass Acoustic Refinement
Human conversation is inefficient, bursty, and overlapping. To mimic this, dialogue must be generated through sequential degradation:
- **Pass 1 (Pressure Cooker)**: Socratic tension, forcing active dialectical debate.
- **Pass 2 (Dialogue Assassin)**: Stripping explicit emotion, replacing it with physical beats (`[sigh]`) and behavioral avoidance.
- **Pass 3 (Anchor Pass)**: Engineering linguistic imperfection—burstiness (mixing short and long rambling sentences), backchanneling (`(mhm)`), and hard interruptions (`—`). 

By breaking the generation into these specific orchestration phases, the final script bypasses standard AI detection and reads as genuinely human, ready for advanced Text-to-Speech synthesis.