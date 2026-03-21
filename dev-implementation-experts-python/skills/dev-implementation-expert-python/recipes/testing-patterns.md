---
name: testing-patterns
description: pytest + unittest.mock patterns for AI client testing with async support
libraries_used: pytest, unittest.mock, asyncio
---

# Testing Patterns Recipe

## Mock Heavy Imports at Module Level
```python
import sys
import types
from unittest.mock import MagicMock

# Must happen BEFORE importing the module under test
sys.modules["openai"] = MagicMock()
sys.modules["google"] = types.ModuleType("google")       # use ModuleType for packages
sys.modules["google.generativeai"] = MagicMock()
sys.modules["litellm"] = MagicMock()
sys.modules["pydantic_ai"] = MagicMock()
sys.modules["pydantic_ai.messages"] = MagicMock()
sys.modules["pydantic_ai.models.openai"] = MagicMock()
sys.modules["pydantic_ai.models.gemini"] = MagicMock()
```

## Async Test Helper
```python
import asyncio

def async_test(coro):
    """Decorator to run async tests in sync test methods."""
    def wrapper(*args, **kwargs):
        loop = asyncio.new_event_loop()
        try:
            return loop.run_until_complete(coro(*args, **kwargs))
        finally:
            loop.close()
    return wrapper
```

## Mocking Async Generators (streaming)
```python
from unittest.mock import MagicMock, AsyncMock

# For streaming: return an async generator
async def mock_stream():
    yield MagicMock(choices=[MagicMock(delta=MagicMock(content="Chunk1"))])
    yield MagicMock(choices=[MagicMock(delta=MagicMock(content="Chunk2"))])

async def return_stream(*args, **kwargs):
    return mock_stream()

mock_client.chat.completions.create.side_effect = return_stream

@async_test
async def test_stream():
    chunks = []
    async for chunk in client.stream_chat("Hello", []):
        chunks.append(chunk)
    assert chunks == ["Chunk1", "Chunk2"]
```

## patch() Decorator vs Context Manager
```python
from unittest.mock import patch, MagicMock

# Decorator — cleaner for whole test
@patch("my_module.openai.OpenAI")
def test_init(self, mock_openai_cls):
    mock_openai_cls.return_value = MagicMock()
    client = MyClient()
    mock_openai_cls.assert_called_once()

# Context manager — useful for scoped patches
def test_generate(self):
    with patch("my_module.some_func") as mock_func:
        mock_func.return_value = "result"
        assert my_module.do_thing() == "result"
```

## setUp/tearDown with patcher
```python
import unittest

class TestMyClient(unittest.TestCase):
    def setUp(self):
        self.patcher = patch("mai_core.config.get_provider_settings")
        self.mock_settings = self.patcher.start()
        self.mock_settings.return_value = MagicMock(
            api_key="test-key-long-enough-16chars",
            default_model="test-model",
            base_url="http://test.url",
        )

    def tearDown(self):
        self.patcher.stop()
```

## pytest.ini_options for Multi-Package Workspace
```toml
[tool.pytest.ini_options]
testpaths = ["tests", "package-a/tests", "package-b/tests"]
pythonpath = ["package-a/src", "package-b/src"]
addopts = "-v --tb=short"
```
