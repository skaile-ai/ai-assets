"""
Atomic Example: pydantic-ai message history construction.
Extracted from: mai-client-pydanticai/src/mai_client_pydanticai/client.py
"""
from typing import List
from pydantic_ai.messages import (
    ModelMessage,
    ModelRequest,
    ModelResponse,
    UserPromptPart,
    TextPart,
)
from mai_core.models import MaiResponse  # project-specific type


def convert_mai_history_to_pydantic(
    history: List[MaiResponse],
    current_message: str,
) -> List[ModelMessage]:
    """
    Convert MaiResponse history to pydantic-ai ModelMessage list.

    IMPORTANT: If the last history entry equals current_message and is a user
    message, exclude it — pydantic-ai takes the new message as a separate arg.
    """
    # Trim duplicate of current message from end of history
    to_process = history
    if history and history[-1].content == current_message and history[-1].provider == "user":
        to_process = history[:-1]

    result: List[ModelMessage] = []
    for msg in to_process:
        content = str(msg.content)
        if msg.provider == "user":
            result.append(ModelRequest(parts=[UserPromptPart(content=content)]))
        else:
            result.append(ModelResponse(parts=[TextPart(content=content)]))
    return result


# Usage in stream_chat:
# pydantic_history = convert_mai_history_to_pydantic(history, message)
# async with agent.run_stream(message, message_history=pydantic_history) as result:
#     async for chunk in result.stream_text():
#         yield chunk
