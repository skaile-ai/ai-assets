"""
Atomic Example: Retry/backoff decorator supporting both sync and async functions.
Extracted from: mai-core/src/mai_core/utils/retries.py pattern.
"""
import asyncio
import functools
import time
from typing import Callable, TypeVar, Any

F = TypeVar("F", bound=Callable[..., Any])


def retry_with_backoff(max_retries: int = 3, base_delay: float = 1.0) -> Callable[[F], F]:
    """Decorator factory: retries on exception with exponential backoff.
    Works for both sync and async functions."""
    def decorator(func: F) -> F:
        if asyncio.iscoroutinefunction(func):
            @functools.wraps(func)
            async def async_wrapper(*args: Any, **kwargs: Any) -> Any:
                last_exc: Exception | None = None
                for attempt in range(max_retries):
                    try:
                        return await func(*args, **kwargs)
                    except Exception as e:
                        last_exc = e
                        if attempt < max_retries - 1:
                            await asyncio.sleep(base_delay * (2 ** attempt))
                raise last_exc  # type: ignore[misc]
            return async_wrapper  # type: ignore[return-value]
        else:
            @functools.wraps(func)
            def sync_wrapper(*args: Any, **kwargs: Any) -> Any:
                last_exc: Exception | None = None
                for attempt in range(max_retries):
                    try:
                        return func(*args, **kwargs)
                    except Exception as e:
                        last_exc = e
                        if attempt < max_retries - 1:
                            time.sleep(base_delay * (2 ** attempt))
                raise last_exc  # type: ignore[misc]
            return sync_wrapper  # type: ignore[return-value]
    return decorator  # type: ignore[return-value]


# Usage:
# @retry_with_backoff(max_retries=3)
# def call_api(prompt: str) -> str: ...
#
# @retry_with_backoff(max_retries=3)
# async def async_call_api(prompt: str) -> str: ...
