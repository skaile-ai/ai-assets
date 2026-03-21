# Deep Research & Recherche Concepts

This document summarizes the implementation patterns for research and deep research skills identified within the Agent skills ecosystem.

## Implementation Patterns Overview

We have identified six distinct patterns for implementing research capabilities, ranging from autonomous web-scale agents to specialized codebase cartography.

---

### 1. Autonomous Multi-step Research (Gemini Deep Research)
**Source**: `sickn33-awesome-skills/skills/deep-research`

This pattern leverages high-level agents (like Gemini Deep Research) that handle the entire lifecycle of a research task: planning, searching, reading, and synthesizing.

- **Technical Concept**: Asynchronous orchestration of long-running tasks.
- **Key Features**: Multi-turn follow-ups (`--continue`), structured output formats, and real-time progress streaming.
- **Code Example**:
```python
# Conceptual orchestration of a deep research task
def start_research(query, format_instructions):
    interaction = gemini.deep_research.start(
        query=query,
        output_format=format_instructions,
        wait_for_completion=False
    )
    return interaction.id
```

### 2. Swarm/Parallel Research (Claude-Flow)
**Source**: `claude-flow/v2/src/swarm/strategies/research.ts`

Focuses on maximizing throughput and breadth through massive parallelism and intelligent coordination.

- **Technical Concept**: **Decomposition & Parallelism**. A single objective is broken into "Query Planning", "Parallel Web Search", "Data Extraction", and "Synthesis".
- **Optimizations**: 
    - **Caching**: Base64-hashed query results.
    - **Semantic Clustering**: Grouping results by topic similarity using centroids.
    - **Connection Pooling & Rate Limiting**: Managing multiple search engine sessions.
- **Code Example**:
```typescript
// Semantic clustering implementation pattern
private async performSemanticClustering(data: any[]): Promise<ResearchCluster[]> {
  const clusters = await nlp.cluster(data, {
    minCoherence: 0.7,
    maxClusters: 10
  });
  return clusters.map(c => ({
    topic: c.label,
    coherenceScore: c.score,
    results: c.items
  }));
}
```

### 3. Iterative Codebase Research (Wiki Researcher)
**Source**: `sickn33-awesome-skills/skills/wiki-researcher`

Specialized in deep-diving into codebases with an iterative, multi-lens approach.

- **Technical Concept**: **Iterative Refinement**. It enforces a "Depth Before Breadth" rule, tracing actual code paths (A calls B calls C) rather than inferring from structure.
- **Workflow (Lens-based)**:
    1. Structural/Architectural View
    2. Data Flow/State Management
    3. Integration/Dependency
    4. Pattern/Anti-pattern
    5. Synthesis
- **Evidence Standard**: Requires file paths + function names for every claim.

### 4. Targeted Context Fetching (Context7)
**Source**: `sickn33-awesome-skills/skills/context7-auto-research`

A pattern for augmenting LLM context with "just-in-time" documentation.

- **Technical Concept**: **External Knowledge Augmentation**. Instead of general web search, it targets specialized APIs (Context7) to get verified library/framework documentation.
- **Benefit**: High precision, zero hallucination of APIs.

### 5. Cartographic Documentation (SPDD)
**Source**: `sickn33-awesome-skills/skills/SPDD/1-research.md`

A "Cartographer" persona that documents the *status quo* without proposing changes.

- **Technical Concept**: **Bounded Analysis**. Strict separation between observation (Research) and action (Planning/Execution).
- **Goal**: Creating a technical "map" or PRD of the current system.

### 6. Scientific Rigor Persona (Research Engineer)
**Source**: `sickn33-awesome-skills/skills/research-engineer`

An uncompromising approach that combines theoretical computer science with implementation.

- **Technical Concept**: **Formal Verification & Rigorous Tool Selection**. 
- **Pattern**: "Critique First" - rejecting suboptimal user premises (e.g., parsing HTML with regex) before implementing a correct alternative.

---

## Technical Concepts Comparison

| Pattern | Speed | Cost | Depth | Use Case |
| :--- | :--- | :--- | :--- | :--- |
| **Autonomous (Gemini)** | 2-10 min | $2-5 | Extreme | Market Analysis, Literature Review |
| **Swarm (Parallel)** | High | Variable | High | Wide-scale data gathering, Trends |
| **Iterative (Code)** | Moderate | Low | Very High | "How does this code work?" |
| **Targeted (Ctx7)** | Fast | Low | Expert | API Documentation, Frameworks |
| **Cartographic** | Fast | Low | High | Documenting current state |
| **Scientific** | Moderate | Low | Theoretical | Algorithm design, HPC, Safety-critical |

## Core Components for Deep Research Skills

1. **Decomposition Engine**: Breaking a query into sub-queries.
2. **Rate Limiting/Backoff**: Essential for web-scale agents to avoid blocking.
3. **Deduplication Logic**: Removing redundant info from multiple sources.
4. **Evidence Mapping**: Direct linking of synthesis results to source citations or code line numbers.
5. **Clustering/Synthesis**: Transforming raw data into structured insights.
