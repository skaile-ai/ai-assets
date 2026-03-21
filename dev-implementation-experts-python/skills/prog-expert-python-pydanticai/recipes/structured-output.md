---
name: structured-output
description: pydantic-ai structured output with result_type, static and dynamic models
libraries_used: pydantic-ai, pydantic
---

# Structured Output Recipe

## Static Pydantic Model
```python
from pydantic import BaseModel, Field
from pydantic_ai import Agent
from pydantic_ai.models.openai import OpenAIModel

class AnalysisResult(BaseModel):
    sentiment: str = Field(..., description="positive, negative, or neutral")
    score: float = Field(..., ge=-1.0, le=1.0)
    summary: str

agent = Agent(OpenAIModel("gpt-4o"), result_type=AnalysisResult)
result = agent.run_sync("Analyze: 'I love this product!'")
data: AnalysisResult = result.data
print(data.sentiment, data.score)
```

## One-Off Structured Call (temporary agent)
```python
from typing import TypeVar, Type
from pydantic import BaseModel
from pydantic_ai import Agent

T = TypeVar("T", bound=BaseModel)

def generate_structured(base_agent: Agent, prompt: str, output_model: Type[T]) -> T:
    """Create a temporary agent scoped to this output type, reusing the model."""
    temp_agent = Agent(base_agent.model, result_type=output_model)
    return temp_agent.run_sync(prompt).data
```

## Dynamic Model from Schema Dict
```python
from typing import Any, Dict
from pydantic import BaseModel
from pydantic_ai import Agent

def generate_from_schema(
    agent: Agent,
    prompt: str,
    schema: Dict[str, Any],  # e.g. {"name": str, "age": int}
) -> Dict[str, Any]:
    # Build a temporary Pydantic model from the schema
    dynamic_model = type(
        "DynamicResponse",
        (BaseModel,),
        {"__annotations__": schema},
    )
    temp_agent = Agent(agent.model, result_type=dynamic_model)
    result = temp_agent.run_sync(prompt)
    return result.data.model_dump()
```

## Key Rules
1. `result_type` is set at agent construction time, not at call time.
2. For multiple different output types, create multiple agents or use the temporary agent pattern.
3. `result.data` is typed as `result_type` — mypy will infer this correctly with TypeVar.
4. Dynamic models work but lose static type safety — document the schema clearly.
