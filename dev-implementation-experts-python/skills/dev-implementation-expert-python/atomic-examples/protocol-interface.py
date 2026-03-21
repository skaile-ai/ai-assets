"""
Atomic Example: Protocol-based interface (structural subtyping).
Extracted from: mai-core/src/mai_core/interface.py pattern.
"""
from typing import Protocol, List, AsyncGenerator, runtime_checkable


# Use @runtime_checkable only if isinstance() checks are needed
@runtime_checkable
class MaiClientProtocol(Protocol):
    """Structural interface — any class with stream_chat() satisfies this."""

    async def stream_chat(
        self,
        message: str,
        history: list,  # use concrete type in real code
    ) -> AsyncGenerator[str, None]:
        """Stream chat response chunks."""
        ...


# Implementation — no explicit inheritance required
class MyClient:
    async def stream_chat(self, message: str, history: list) -> AsyncGenerator[str, None]:
        yield "Hello"
        yield " world"


# Type-safe usage
def get_client() -> MaiClientProtocol:
    return MyClient()  # type: ignore[return-value]  # structurally compatible


# Key difference from ABC:
# - Protocol: duck typing, no inheritance needed, checked statically by mypy
# - ABC: nominal typing, explicit inheritance required
