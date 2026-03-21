"""
Reference Implementation: Full PydanticAIClient from mai-tools.
Source: mai-client-pydanticai/src/mai_client_pydanticai/client.py

This is a real-world implementation showing:
- Config-driven initialization with factory classmethod
- Multi-provider support (OpenAI-compat + Gemini)
- Structured output generation (static and dynamic models)
- Async streaming with message history conversion
- MaiClientProtocol compliance
"""
from typing import Optional, Dict, Any, Type, TypeVar, List, AsyncGenerator
from pathlib import Path
import os
from pydantic import BaseModel, Field
import google.generativeai as genai
from pydantic_ai import Agent
from pydantic_ai.messages import (
    ModelMessage, ModelRequest, ModelResponse, UserPromptPart, TextPart,
)
from pydantic_ai.models.openai import OpenAIModel
from pydantic_ai.models.gemini import GeminiModel
from mai_core.config import ProviderSettings, load_settings_from_yaml
from mai_core.utils import validate_api_key, retry_with_backoff
from mai_core.exceptions import ProviderError
from mai_core.models import MaiResponse, MaiModelInfo

T = TypeVar("T", bound=BaseModel)


class PydanticAIConfig(BaseModel):
    model: str = Field(..., description="Model string")
    api_key: Optional[str] = None
    base_url: Optional[str] = None
    provider: str = Field("openai")
    max_retries: int = Field(3)

    @classmethod
    def from_provider_settings(cls, ps: ProviderSettings, provider_name: str = "litellm") -> "PydanticAIConfig":
        return cls(model=ps.default_model, api_key=ps.api_key, base_url=ps.base_url, provider=provider_name)


class PydanticAIClient:
    def __init__(self, config: PydanticAIConfig):
        self.config = config
        self._validate_config()

        # Set env vars BEFORE constructing model objects
        if config.api_key:
            if config.provider == "gemini":
                os.environ["GEMINI_API_KEY"] = config.api_key
            else:
                os.environ["OPENAI_API_KEY"] = config.api_key
        if config.base_url and config.provider != "gemini":
            os.environ["OPENAI_BASE_URL"] = config.base_url

        # Build model backend
        if config.provider == "gemini":
            if config.api_key:
                genai.configure(api_key=config.api_key)
            model = GeminiModel(config.model)
        else:
            model = OpenAIModel(config.model)

        self.agent = Agent(model)

    @classmethod
    def from_settings(cls, provider: Optional[str] = None, config_path: Optional[Path] = None) -> "PydanticAIClient":
        settings = load_settings_from_yaml(config_path)
        target = provider or settings.default_provider
        if target not in settings.providers:
            raise ValueError(f"Provider '{target}' not configured")
        config = PydanticAIConfig.from_provider_settings(settings.providers[target], target)
        return cls(config)

    def _validate_config(self) -> None:
        if self.config.api_key and not validate_api_key(self.config.api_key):
            raise ProviderError("pydanticai", "Invalid API key - must be at least 16 characters")

    @retry_with_backoff(max_retries=3)
    def generate(self, prompt: str, output_model: Type[T], model: Optional[str] = None, **kwargs) -> T:
        temp_agent = Agent(self.agent.model, result_type=output_model)
        try:
            return temp_agent.run_sync(prompt).data
        except Exception as e:
            raise ProviderError("pydanticai", f"Failed to generate: {e}")

    def generate_with_schema(self, prompt: str, schema: Dict[str, Any], model: Optional[str] = None, **kwargs) -> MaiResponse:
        dynamic_model = type("DynamicResponse", (BaseModel,), {f: (t, ...) for f, t in schema.items()})
        result = self.generate(prompt, dynamic_model, model, **kwargs)
        return MaiResponse(content=result.model_dump(), model=model or self.config.model, provider="pydanticai")

    async def stream_chat(self, message: str, history: List[MaiResponse]) -> AsyncGenerator[str, None]:
        # Convert history, excluding duplicate of current message at tail
        to_process = history
        if history and history[-1].content == message and history[-1].provider == "user":
            to_process = history[:-1]

        pydantic_history: List[ModelMessage] = []
        for msg in to_process:
            content = str(msg.content)
            if msg.provider == "user":
                pydantic_history.append(ModelRequest(parts=[UserPromptPart(content=content)]))
            else:
                pydantic_history.append(ModelResponse(parts=[TextPart(content=content)]))

        async with self.agent.run_stream(message, message_history=pydantic_history) as result:
            async for chunk in result.stream_text():
                yield chunk
