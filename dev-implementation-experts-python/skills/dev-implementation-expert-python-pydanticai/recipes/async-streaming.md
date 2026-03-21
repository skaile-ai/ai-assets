---
name: async-streaming
description: pydantic-ai async streaming with run_stream() as async context manager
libraries_used: pydantic-ai, asyncio
---

# Async Streaming Recipe

## Core Pattern
```python
from typing import AsyncGenerator, List
from pydantic_ai import Agent
from pydantic_ai.messages import ModelMessage

async def stream_chat(
    agent: Agent,
    message: str,
    message_history: List[ModelMessage],
) -> AsyncGenerator[str, None]:
    async with agent.run_stream(message, message_history=message_history) as result:
        async for chunk in result.stream_text():
            yield chunk
```

## Full MaiClientProtocol-Compatible Implementation
```python
from typing import AsyncGenerator, List
from pydantic_ai import Agent
from pydantic_ai.messages import ModelMessage, ModelRequest, ModelResponse, UserPromptPart, TextPart
from mai_core.models import MaiResponse

class MyPydanticAIClient:
    def __init__(self, agent: Agent):
        self.agent = agent

    async def stream_chat(
        self,
        message: str,
        history: List[MaiResponse],
    ) -> AsyncGenerator[str, None]:
        pydantic_history = self._convert_history(history, message)
        async with self.agent.run_stream(message, message_history=pydantic_history) as result:
            async for chunk in result.stream_text():
                yield chunk

    def _convert_history(self, history: List[MaiResponse], current_message: str) -> List[ModelMessage]:
        to_process = history
        if history and history[-1].content == current_message and history[-1].provider == "user":
            to_process = history[:-1]
        result = []
        for msg in to_process:
            content = str(msg.content)
            if msg.provider == "user":
                result.append(ModelRequest(parts=[UserPromptPart(content=content)]))
            else:
                result.append(ModelResponse(parts=[TextPart(content=content)]))
        return result
```

## Key Rules
1. `run_stream()` MUST be used as `async with` — it is an async context manager.
2. `result.stream_text()` is an async iterable — use `async for`.
3. Do NOT call `run_stream()` without `async with` — the stream won't be cleaned up.
4. `message_history` takes `List[ModelMessage]` — convert from your domain types first.
5. The `message` arg is the NEW user message — do not duplicate it in `message_history`.

## Testing Streaming
```python
from unittest.mock import MagicMock, AsyncMock

mock_agent = MagicMock()

async def text_stream():
    yield "Chunk1"
    yield "Chunk2"

mock_result = MagicMock()
mock_result.stream_text.return_value = text_stream()

mock_ctx = MagicMock()
mock_ctx.__aenter__ = AsyncMock(return_value=mock_result)
mock_ctx.__aexit__ = AsyncMock(return_value=None)

mock_agent.run_stream.return_value = mock_ctx
```
