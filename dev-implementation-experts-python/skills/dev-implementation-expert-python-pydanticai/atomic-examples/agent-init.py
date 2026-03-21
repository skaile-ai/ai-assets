"""
Atomic Example: pydantic-ai Agent initialization patterns.
Extracted from: mai-client-pydanticai/src/mai_client_pydanticai/client.py
"""
import os
from pydantic import BaseModel, Field
from pydantic_ai import Agent
from pydantic_ai.models.openai import OpenAIModel
from pydantic_ai.models.gemini import GeminiModel
import google.generativeai as genai


# --- Basic chat agent (no structured output) ---
def make_openai_agent(model: str = "gpt-4o", api_key: str | None = None, base_url: str | None = None) -> Agent:
    if api_key:
        os.environ["OPENAI_API_KEY"] = api_key
    if base_url:
        os.environ["OPENAI_BASE_URL"] = base_url  # for LiteLLM proxy or custom endpoints
    return Agent(OpenAIModel(model))


def make_gemini_agent(model: str = "gemini-2.0-flash", api_key: str | None = None) -> Agent:
    if api_key:
        os.environ["GEMINI_API_KEY"] = api_key
        genai.configure(api_key=api_key)
    return Agent(GeminiModel(model))


# --- Structured output agent ---
class MyOutput(BaseModel):
    summary: str = Field(..., description="Summary of the response")
    confidence: float = Field(..., ge=0.0, le=1.0)

structured_agent = Agent(OpenAIModel("gpt-4o"), result_type=MyOutput)

# run_sync returns AgentRunResult — access .data for the typed output
result = structured_agent.run_sync("Summarize quantum computing in one sentence.")
output: MyOutput = result.data
print(output.summary, output.confidence)


# --- Temporary agent for one-off structured calls (reuse existing model) ---
def generate_structured(base_agent: Agent, prompt: str, output_model: type) -> object:
    temp_agent = Agent(base_agent.model, result_type=output_model)
    return temp_agent.run_sync(prompt).data
