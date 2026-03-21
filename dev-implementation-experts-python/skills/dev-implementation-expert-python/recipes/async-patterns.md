---
name: async-patterns
description: Async Python patterns — AsyncGenerator, async context managers, sync wrappers
libraries_used: asyncio
---

# Async Patterns Recipe

## AsyncGenerator for Streaming
```python
from typing import AsyncGenerator

async def stream_text(prompt: str) -> AsyncGenerator[str, None]:
    # Yields chunks as they arrive
    async for chunk in some_api_stream(prompt):
        yield chunk.text

# Consumer
async def main() -> None:
    async for chunk in stream_text("Hello"):
        print(chunk, end="", flush=True)
```

## Async Context Manager
```python
from contextlib import asynccontextmanager
from typing import AsyncGenerator

@asynccontextmanager
async def managed_connection(url: str) -> AsyncGenerator[Connection, None]:
    conn = await connect(url)
    try:
        yield conn
    finally:
        await conn.close()

# Usage
async with managed_connection("ws://...") as conn:
    data = await conn.recv()
```

## Sync Wrapper for Async (in tests or CLI)
```python
import asyncio

def run_async(coro):
    """Run a coroutine synchronously. Use in tests or CLI entry points."""
    loop = asyncio.new_event_loop()
    try:
        return loop.run_until_complete(coro)
    finally:
        loop.close()

# In tests (decorator pattern)
def async_test(coro):
    def wrapper(*args, **kwargs):
        return run_async(coro(*args, **kwargs))
    return wrapper

@async_test
async def test_streaming():
    chunks = []
    async for chunk in stream_text("hi"):
        chunks.append(chunk)
    assert chunks
```

## Consuming Async Generator from Sync Code
```python
# Only use asyncio.run() at top-level entry points
result = asyncio.run(collect_all("prompt"))

async def collect_all(prompt: str) -> list[str]:
    return [chunk async for chunk in stream_text(prompt)]
```

## Common Pitfalls
- Do NOT `await` an `AsyncGenerator` directly — use `async for`.
- Do NOT use `asyncio.run()` inside an already-running event loop (e.g., Jupyter).
- Always `yield` inside `async def` to make it an AsyncGenerator, not a coroutine.
