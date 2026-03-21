---
name: type-patterns
description: Python 3.12+ type system patterns — Protocol, TypeVar, Generic, AsyncGenerator
libraries_used: typing
---

# Type Patterns Recipe

## Protocol (Structural Subtyping)
```python
from typing import Protocol, runtime_checkable

@runtime_checkable  # enables isinstance() checks
class Readable(Protocol):
    def read(self) -> str: ...

class FileReader:
    def read(self) -> str:
        return open("file.txt").read()

# No inheritance needed — FileReader satisfies Readable structurally
def process(reader: Readable) -> str:
    return reader.read()

process(FileReader())  # mypy: OK
```

## TypeVar + Generic
```python
from typing import TypeVar, Generic
from pydantic import BaseModel

T = TypeVar("T", bound=BaseModel)

def generate(prompt: str, output_model: type[T]) -> T:
    ...  # returns instance of output_model

class MyOutput(BaseModel):
    answer: str

result: MyOutput = generate("What is 2+2?", MyOutput)
# result.answer is typed correctly
```

## AsyncGenerator Type Annotation
```python
from typing import AsyncGenerator

async def stream_chunks(prompt: str) -> AsyncGenerator[str, None]:
    for word in prompt.split():
        yield word

# For protocol methods returning AsyncGenerator:
class StreamProtocol(Protocol):
    async def stream(self, msg: str) -> AsyncGenerator[str, None]: ...
```

## Type Aliases (Python 3.12)
```python
# New syntax (3.12+)
type ProviderMap = dict[str, "ProviderSettings"]
type MaybeStr = str | None

# Old syntax (compatible with 3.10+)
from typing import TypeAlias
ProviderMap: TypeAlias = dict[str, "ProviderSettings"]
```

## Optional vs Union
```python
from typing import Optional

# These are equivalent:
def foo(x: Optional[str]) -> None: ...
def foo(x: str | None) -> None: ...   # preferred in 3.10+
```

## mypy Configuration
```toml
[tool.mypy]
strict = true
ignore_missing_imports = true   # for untyped third-party libs
python_version = "3.12"
```
