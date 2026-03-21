---
name: config-management
description: Config loading with pydantic-settings, pydantic BaseModel, YAML files, and .env
libraries_used: pydantic, pydantic-settings, pyyaml, python-dotenv
---

# Config Management Recipe

## pydantic-settings from .env
```python
from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict

class AppSettings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8")

    openai_api_key: str = Field(..., description="OpenAI API key")
    default_model: str = Field("gpt-4o", description="Default model")
    debug: bool = Field(False)

settings = AppSettings()  # reads .env automatically
```

## YAML Config with Nested Pydantic Models
```python
from pathlib import Path
from typing import Dict, Optional
import yaml
from pydantic import BaseModel, Field

class ProviderSettings(BaseModel):
    api_key: Optional[str] = None
    base_url: Optional[str] = None
    default_model: str = "gpt-4o"

class AppConfig(BaseModel):
    default_provider: str = "openai"
    providers: Dict[str, ProviderSettings] = Field(default_factory=dict)

def load_settings_from_yaml(path: Optional[Path] = None) -> AppConfig:
    config_path = path or Path("config/providers.yaml")
    if not config_path.exists():
        return AppConfig()
    with open(config_path) as f:
        data = yaml.safe_load(f) or {}
    return AppConfig.model_validate(data)
```

## providers.yaml Example
```yaml
default_provider: openai

providers:
  openai:
    api_key: sk-...
    default_model: gpt-4o

  litellm:
    base_url: http://localhost:4000
    api_key: sk-fake
    default_model: gpt-4o

  gemini:
    api_key: AIza...
    default_model: gemini-2.0-flash
```

## Factory Pattern (from_settings)
```python
@classmethod
def from_settings(cls, provider: Optional[str] = None, config_path: Optional[Path] = None) -> "MyClient":
    settings = load_settings_from_yaml(config_path)
    target = provider or settings.default_provider
    if target not in settings.providers:
        raise ValueError(f"Provider '{target}' not in config")
    return cls(config=MyConfig.from_provider_settings(settings.providers[target], target))
```
