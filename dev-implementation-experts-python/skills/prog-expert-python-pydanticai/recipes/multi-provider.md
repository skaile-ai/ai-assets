---
name: multi-provider
description: pydantic-ai multi-provider setup — OpenAI, Gemini, LiteLLM-compatible endpoints
libraries_used: pydantic-ai, google-generativeai
---

# Multi-Provider Setup Recipe

## Provider Detection Pattern
```python
import os
from pydantic_ai import Agent
from pydantic_ai.models.openai import OpenAIModel
from pydantic_ai.models.gemini import GeminiModel
import google.generativeai as genai

def build_agent(provider: str, model: str, api_key: str | None, base_url: str | None) -> Agent:
    if provider == "gemini":
        if api_key:
            os.environ["GEMINI_API_KEY"] = api_key
            genai.configure(api_key=api_key)
        return Agent(GeminiModel(model))
    else:
        # OpenAI, LiteLLM, Azure OpenAI, Anthropic via OpenAI-compat, etc.
        if api_key:
            os.environ["OPENAI_API_KEY"] = api_key
        if base_url:
            os.environ["OPENAI_BASE_URL"] = base_url
        return Agent(OpenAIModel(model))
```

## LiteLLM Proxy (OpenAI-Compatible)
```python
# LiteLLM exposes an OpenAI-compatible endpoint
# Just set OPENAI_BASE_URL to the proxy and use OpenAIModel
os.environ["OPENAI_API_KEY"] = "sk-fake"           # required but unused by LiteLLM
os.environ["OPENAI_BASE_URL"] = "http://localhost:4000"
agent = Agent(OpenAIModel("gpt-4o"))               # LiteLLM routes to actual provider
```

## Config-Driven Initialization (from_settings factory)
```python
from pathlib import Path
from typing import Optional
from mai_core.config import load_settings_from_yaml

@classmethod
def from_settings(cls, provider: Optional[str] = None, config_path: Optional[Path] = None):
    settings = load_settings_from_yaml(config_path)
    target = provider or settings.default_provider
    if target not in settings.providers:
        raise ValueError(f"Provider '{target}' not configured")
    ps = settings.providers[target]
    agent = build_agent(
        provider=target,
        model=ps.default_model,
        api_key=ps.api_key,
        base_url=ps.base_url,
    )
    return cls(agent=agent, config=...)
```

## Environment Variable Summary
| Provider | Key env vars |
|---|---|
| OpenAI | `OPENAI_API_KEY`, `OPENAI_BASE_URL` (optional) |
| Gemini | `GEMINI_API_KEY` + `genai.configure(api_key=...)` |
| LiteLLM proxy | `OPENAI_API_KEY=sk-fake`, `OPENAI_BASE_URL=http://proxy:4000` |
| Azure OpenAI | `OPENAI_API_KEY`, `OPENAI_BASE_URL=https://<resource>.openai.azure.com/` |

## Notes
- pydantic-ai reads env vars when constructing `OpenAIModel` / `GeminiModel`.
- Set env vars BEFORE constructing the model object.
- `genai.configure()` is required for Gemini even if `GEMINI_API_KEY` is set.
